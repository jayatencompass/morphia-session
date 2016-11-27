package org.mongodb.morphia.session;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.session.cache.SessionCache;
import org.mongodb.morphia.session.events.SessionListener;

import java.util.Collection;

/**
 * Defines a "session" that implements the "Unit of Work" pattern for interacting with Morphia-mapped objects. Rather
 * than treating each read/write as a completely atomic operation that is independent of everything around it, a
 * DatastoreSession treats them as a cohesive set of operations to efficiently and effectively complete a greater
 * unit of work.
 *
 * Sessions provide the following benefits over standard "wild west" Morphia:
 * <ul>
 *   <li>
 *       Deferred Writes. Nothing is written to the database until everything in the "unit of work" is complete. This
 *       helps to avoid leaving your database in only a partially-correct state should some exception occur 3/4 through
 *       the process.
 *   </li>
 *   <li>Caching. Multiple accesses to the same entity by ObjectId results in only 1 DB query.</li>
 *   <li>Double Equals Equality. Accessing the same entity from multiple places returns the exact same instance in memory.</li>
 *   <li>Life-Cycle Events. Attach database listeners to operate on entities at any stage of the session's or entity's lifecycle.</li>
 *   <li>Read-Only Sessions. Avoid accidental updates by marking a session as "read-only".</li>
 * </ul>
 *
 * Unlike traditional relational databases these sessions are NOT ACID compliant. While these sessions will get you
 * really close to ACID-style transactions, they are simply an abstraction to defer writes and maximize caching as
 * per the unit of work pattern.
 */
public interface Session extends AutoCloseable
{
    /**
     * Starts the session in the given update mode.
     * @param mode Is this an UPDATE or READ_ONLY session?
     * @return This session, now activated
     * @throws IllegalStateException If the session is already active.
     */
    Session begin(SessionMode mode);

    /**
     * Looks up a single entity given the Morphia Key descriptor for the record
     * @param key The key w/ the type and id information
     * @param <T> The type of the target entity
     * @return The appropriate record, or null if no record exists w/ that id
     */
    default <T> T get(Key<T> key)
    {
        return get(key.getType(), key.getId());
    }

    /**
     * Looks up a single entity by its id.
     * @param type The entity type/collection to look up
     * @param id The record id to retrieve
     * @return The appropriate record, or null if no record exists w/ that id
     */
    <T> T get(Class<T> type, Object id);

    /**
     * Looks up a set of entities by their ids.
     * @param type The entity type/collection to look up
     * @param ids The ids of the records to retrieve
     * @param <T> The type of the entity/collection
     * @param <U> The type of the id (e.g. ObjectId)
     * @return The matching records in the original order of the input ids
     */
    <T, U> Collection<T> get(Class<T> type, Collection<U> ids);

    /**
     * Saves the given entity to the session. This does NOT save the record to the database yet. It simply puts the
     * record into the "dirty set" of the session for records to write when the session commits. If this is a new record
     * then this will automatically assign a new id to the record so you can add references to it later on in the session.
     * @param entity The new record to save or an existing record to modify.
     * @return The input entity. If 'record' was a new record, the return value will have the _id we're going to assign it.
     */
    <T> T save(T entity);

    /**
     * Marks the given record for deletion. This does NOT delete the record right now. It simply marks it for deletion and
     * the delete won't occur until the session commits.
     * @param entity The record to delete
     * @return The input 'record' now marked for deletion
     */
    <T> T delete(T entity);

    /**
     * @return The mode defining if this is a READ_ONLY or UPDATE session.
     */
    SessionMode getMode();

    /**
     * @return The lifecycle state of the session
     */
    SessionState getState();

    /**
     * @return The session's front-side cache
     */
    SessionCache getCache();

    /**
     * @return The accumulated activity stats for this session
     */
    SessionStats getStats();

    /**
     * Constructs a query that integrates with this session's front-side cache. If a query result has an entity in the
     * session's front-side cache, it will not map it from scratch again - instead using the existing copy from the session.
     * Additionally, any query results that are mapped from scratch are added to the session's cache so that any other
     * queries or "gets" in this session can avoid additional DB round trips and remaps as well.
     * @see org.mongodb.morphia.query.SessionBoundQuery For additional information about why this kicks ass.
     * @param entityType The entity that maps to a collection
     * @return The query you can continue to build and run
     */
    <T> Query<T> query(Class<T> entityType);

    /**
     * This flushes all of the pending writes and deletes to the datastore that have built up over the course of this
     * session. If this is a 'read-only' session then no actual writes will occur and this will behave exactly like
     * the 'rollback()' method.
     *
     * Once this operation completes, the session is closed and all other attempts to access/modify the session will
     * result in "exceptional" failure.
     */
    void commit();

    /**
     * Closes the session without writing anything to the datastore. Once this operation completes the session is closed
     * and all other attempts to access/modify the session will result in failure.
     */
    void rollback();

    /**
     * Adds the listener(s) to the session IN THE GIVEN ORDER. You will NOT receive any events that have already
     * occurred on this session if you call this after they happen (e.g. won't get 'beginning()' if added after 'begin()').
     * @param listeners The listeners to attach to this session
     * @return This session for handy chaining
     */
    Session listen(SessionListener... listeners);
}
