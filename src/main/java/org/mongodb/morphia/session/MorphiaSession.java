package org.mongodb.morphia.session;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.SessionBoundQuery;
import org.mongodb.morphia.session.cache.MapCache;
import org.mongodb.morphia.session.cache.SessionCache;
import org.mongodb.morphia.session.events.SessionListener;
import org.mongodb.morphia.session.id.Ids;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mongodb.morphia.session.SessionMode.READ_ONLY;
import static org.mongodb.morphia.session.SessionState.*;

/**
 * The reference implementation of a database session that tracks modifications to entities, caches them, and handles
 * the lifecycle events for the stages of the session.
 */
public class MorphiaSession implements Session
{
    private static final Logger logger = MorphiaLoggerFactory.get(MorphiaSession.class);

    private Morphia morphia;
    private Datastore datastore;
    private SessionCache cache;
    private SessionMode mode;
    private SessionState state;
    private SessionStats stats;
    private Ids idManager;
    private List<SessionListener> listeners;

    // Separately track our "dirty" objects in these "buckets"
    private Set<Object> creationSet;
    private Set<Object> updateSet;
    private Set<Object> deleteSet;

    public MorphiaSession(Morphia morphia, Datastore datastore)
    {
        this.state = PENDING;
        this.mode = READ_ONLY;
        this.morphia = morphia;
        this.datastore = datastore;
        this.idManager = new Ids(morphia);
        this.cache = new MapCache();
        this.listeners = new ArrayList<>();
        this.stats = new SessionStats();
    }

    /**
     * Starts the session in the given update mode.
     *
     * @param mode Is this an UPDATE or READ_ONLY session?
     * @return This session, now activated
     * @throws IllegalStateException If the session is already active.
     */
    @Override
    public Session begin(SessionMode mode)
    {
        assertState("begin", PENDING);

        logger.debug("Starting session");
        this.mode = (mode == null) ? READ_ONLY : mode;
        if (this.mode.isUpdate())
        {
            creationSet = new HashSet<>();
            updateSet = new HashSet<>();
            deleteSet = new HashSet<>();
        }

        state = ACTIVE;
        fireEvent(l -> l::beginning);
        return this;
    }

    /**
     * Looks up a single entity by its id.
     *
     * @param type The entity type/collection to look up
     * @param id   The record id to retrieve
     * @return The appropriate record, or null if no record exists w/ that id
     */
    @Override
    public <T> T get(Class<T> type, Object id)
    {
        assertState("get", ACTIVE);

        return cache.get(type, id)
            .map(cacheHit -> {
                if (logger.isTraceEnabled())
                    logger.trace("Cache HIT on 'get' {0}[{1}]", type.getSimpleName(), id);

                stats.cacheHit();
                return cacheHit;
            })
            .orElseGet(() -> {
                if (logger.isTraceEnabled())
                    logger.trace("Cache miss on 'get', so querying for {0}[{1}]", type.getSimpleName(), id);

                // Use our session queries rather than 'datastore.get()' to better utilize the front-side cache.
                // Also, SessionBoundQuery increments 'stats.read()' for us, so don't do it here.
                T record = query(type).field("_id").equal(id).get();
                return (record != null) ? cache.put(type, record, id) : null;
            });
    }

    /**
     * Looks up a set of entities by their ids.
     *
     * @param type The entity type/collection to look up
     * @param ids  The ids of the records to retrieve
     * @return The matching records in the original order of the input ids
     */
    @Override
    public <T, U> Collection<T> get(Class<T> type, Collection<U> ids)
    {
        assertState("get", ACTIVE);

        if (ids == null || ids.isEmpty())
            return Collections.emptyList();

        /*
         * NOTE: One naive approach is to query for things not in the front side cache and then just iterate the ids
         * again, performing cache lookups. While it does work with standard, simple, map-based caches, it can fail
         * miserably if using some sort of LRU caching system. For instance, say you have ids [A, B, C, D] where
         * A and B are in the cache already while C and D are not. You query for C and D then put both into the cache,
         * but the configured cache implementation now pushes A out because it was least-recently-used (or whatever
         * the scheme dictates). Now when you iterate the ids and look in the cache, you'll only get 3 of the 4 desired
         * records. To avoid this, we build our own temporary map of [id -> entity] so no matter what happens with the
         * cache after we initially check it, we've got copies of the entities.
         */

        // Pull values from the front-side cache that we've already realized
        Map<Object, T> realizedRecords = new HashMap<>();
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(id -> realizedRecords.put(id, cache.get(type, id).orElse(null)));

        // Find the ids of the things that were not in the cache and query for them
        Collection<Object> missingIds = realizedRecords.entrySet().stream()
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Count each record as a separate cache hit
        IntStream
            .range(0,  realizedRecords.size() - missingIds.size())
            .forEach(i -> stats.cacheHit());

        // Query for the ids not already in the front-side cache
        if (missingIds.size() > 0)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Bulk 'get' [Cached={0}][Queried={1}]",
                    realizedRecords.size() - missingIds.size(),
                    missingIds.size());
            }

            // Use our session queries rather than 'datastore.get()' to better utilize the front-side cache.
            // Also, SessionBoundQuery increments 'stats.read()' for us, so don't do it here.
            for (T entity : query(type).field("_id").in(missingIds).asList())
            {
                Object id = idManager.getId(entity);
                cache.put(type, entity, id);
                realizedRecords.put(id, entity);
            }
        }
        else if (logger.isTraceEnabled())
        {
            logger.trace("Bulk 'get' of {0} items completely in cache", realizedRecords.size());
        }

        // The realizedRecords map should now contain everything so iterate the ids again to preserve the original order
        return ids.stream()
            .map(realizedRecords::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Saves the given entity to the session. This does NOT save the record to the database yet. It simply puts the
     * record into the "dirty set" of the session for records to write when the session commits. If this is a new record
     * then this will automatically assign a new id to the record so you can add references to it later on in the session.
     *
     * @param entity The new record to save or an existing record to modify.
     * @return The input entity. If 'record' was a new record, the return value will have the _id we're going to assign it.
     */
    @Override
    public <T> T save(T entity)
    {
        assertState("save", ACTIVE);

        // Only waste the dirty-set space during update sessions
        if (entity == null || getMode().isReadOnly())
            return entity;

        Class<T> entityType = SessionUtils.getEntityClass(morphia, entity);
        Object id = idManager.getId(entity);

        if (idManager.isEmptyId(id))
        {
            // The record appears to be a "new" record, so generate and assign a new id in addition to putting it in the session.
            id = idManager.createId(entity);
            idManager.setId(entity, id);

            // In case you save the same entity multiple times only track it once
            if (creationSet.add(entity))
                stats.write();

            if (logger.isTraceEnabled())
                logger.trace("Saving NEW {0}[{1}]", entityType.getSimpleName(), id);
        }
        else if (!creationSet.contains(entity))   // in case you save a new entity then save it again, don't mark it as modified, too.
        {
            // In case you save the same entity multiple times only track it once
            if (creationSet.add(entity))
                stats.write();

            if (logger.isTraceEnabled())
                logger.trace("Saving MODIFIED {0}[{1}]", entityType.getSimpleName(), id);
        }

        // Store the entity, new or existing, into the front-side cache now that we've dirtied it
        return cache.put(entityType, entity, id);
    }

    /**
     * Marks the given record for deletion. This does NOT delete the record right now. It simply marks it for deletion and
     * the delete won't occur until the session commits.
     *
     * @param entity The record to delete
     * @return The input 'record' now marked for deletion
     */
    @Override
    public <T> T delete(T entity)
    {
        assertState("delete", ACTIVE);

        if (entity == null || getMode().isReadOnly())
            return null;

        if (logger.isTraceEnabled())
        {
            Class<T> entityType = SessionUtils.getEntityClass(morphia, entity);
            logger.trace("Marking as deleted {0}[{1}]", entityType.getSimpleName(), idManager.getId(entity));
        }

        // Mark it for deletion but LEAVE IT IN THE CACHE! It ain't gone yet. Also, in case you delete the same
        // entity multiple times only track it once
        if (deleteSet.add(entity))
            stats.delete();

        return entity;
    }

    /**
     * Constructs a query that integrates with this session's front-side cache.
     * @param entityType The entity that maps to a collection
     * @return The query you can continue to build and run
     */
    @Override
    public <T> Query<T> query(Class<T> entityType)
    {
        Query<T> query = datastore.createQuery(entityType);
        return new SessionBoundQuery<>(this, query.getEntityClass(), query.getCollection(), datastore);
    }

    /**
     * @return The mode defining if this is a READ_ONLY or UPDATE session.
     */
    @Override
    public SessionMode getMode()
    {
        return mode;
    }

    /**
     * @return The lifecycle state of the session
     */
    @Override
    public SessionState getState()
    {
        return state;
    }

    /**
     * @return The session's front-side cache
     */
    @Override
    public SessionCache getCache()
    {
        return cache;
    }

    /**
     * @return The accumulated activity stats for this session
     */
    @Override
    public SessionStats getStats()
    {
        return stats;
    }

    /**
     * This flushes all of the pending writes and deletes to the datastore that have built up over the course of this
     * session. If this is a 'read-only' session then no actual writes will occur and this will behave exactly like
     * the 'rollback()' method.
     * <p>
     * Once this operation completes, the session is closed and all other attempts to access/modify the session will
     * result in "exceptional" failure.
     */
    @Override
    public void commit()
    {
        assertState("commit", ACTIVE);

        if (getMode().isReadOnly())
        {
            rollback();
            return;
        }

        try
        {
            state = COMMITTING;
            logger.debug("Committing session data");
            fireEvent(l -> l::committing);

            // Because each listener can make modifications to the other dirty sets, perform all of the "*ing" events
            // before doing any writes/deletes so that the sets have every possible entity.
            fireEvent(l -> l::creating, creationSet);
            fireEvent(l -> l::updating, updateSet);
            fireEvent(l -> l::deleting, deleteSet);

            logger.debug("[Commit] Writing {0} new entities", creationSet.size());
            datastore.save(creationSet);

            logger.debug("[Commit] Writing {0} modified entities", updateSet.size());
            datastore.save(updateSet);

            logger.debug("[Commit] Deleting {0} entities", deleteSet.size());
            deleteSet.forEach(datastore::delete);

            fireEvent(l -> l::created, creationSet);
            fireEvent(l -> l::updated, updateSet);
            fireEvent(l -> l::deleted, deleteSet);
            fireEvent(l -> l::committed);
        }
        finally
        {
            close();
        }
    }

    /**
     * Closes the session without writing anything to the datastore. Once this operation completes the session is closed
     * and all other attempts to access/modify the session will result in failure.
     */
    @Override
    public void rollback()
    {
        assertState("rollback", ACTIVE);
        try
        {
            state = ROLLING_BACK;
            logger.debug("Rolling back session data");
        }
        finally
        {
            close();
        }
    }

    /**
     * Cleans up all entity references that were built up in this session and marks the session as COMPLETE.
     */
    @Override
    public void close()
    {
        try
        {
            SessionUtils.clear(creationSet);
            SessionUtils.clear(updateSet);
            SessionUtils.clear(deleteSet);
            cache.clear();
        }
        finally
        {
            // In case you call close multiple times, only fire the event once
            if (!state.isComplete())
            {
                fireEvent(l -> l::closed);
                state = COMPLETE;
            }
        }
    }

    /**
     * Adds the listener(s) to the session IN THE GIVEN ORDER. You will NOT receive any events that have already
     * occurred on this session if you call this after they happen (e.g. won't get 'beginning()' if added after 'begin()').
     * @param listeners The listeners to attach to this session
     * @return This session for handy chaining
     */
    @Override
    public Session listen(SessionListener... listeners)
    {
        Collections.addAll(this.listeners, listeners);
        return this;
    }

    /**
     * A simple way to validate that you are in one of the given 'valid' states when beginning some operation. For
     * instance, you can only 'begin' a session while it's in the PENDING state, you can invoke
     * <code>assertState(SessionState.PENDING)</code> to enforce that.
     * @param validStates The set of states that will NOT trigger an exception
     * @throws IllegalStateException If the current state is NOT one of the 'validStates'
     */
    private void assertState(String operation, SessionState... validStates)
    {
        if (Stream.of(validStates).noneMatch(s -> s == state))
        {
            throw new IllegalStateException(String.format("Unable to execute operation '%s' while session is in '%s' state.",
                operation,
                state));
        }
    }

    /**
     * Fires the given session-level event (e.g. committing, committed, etc)
     * @param evt The event to fire
     */
    private void fireEvent(Function<SessionListener, Consumer<Session>> evt)
    {
        listeners.forEach(listener -> evt.apply(listener).accept(this));
    }

    /**
     * Fires the given record-level event (e.g. updating, updated, deleting, etc)
     * @param evt The event to fire
     * @param entity The record/entity that the event is occurring with
     */
    private void fireEvent(Function<SessionListener, BiConsumer<Session, Object>> evt, Object entity)
    {
        listeners.forEach(listener -> evt.apply(listener).accept(this, entity));
    }

    /**
     * Fires the given record-level event (e.g. updating, updated, deleting, etc) on each of the given entities.
     * @param evt The event to fire
     * @param entities All of the records to apply the event to
     */
    private void fireEvent(Function<SessionListener, BiConsumer<Session, Object>> evt, Collection<Object> entities)
    {
        if (SessionUtils.hasValue(entities))
        {
            // IMPORTANT! We do not prevent you from having a listener that modifies ANY of the dirty sets before they
            // are actually flushed. In fact it's quite handy. As such we want to make a copy of the original
            // creation/update/delete set to avoid concurrent modification exceptions.
            entities.stream()
                .collect(Collectors.toList())
                .forEach(entity -> fireEvent(evt, entity));
        }
    }
}
