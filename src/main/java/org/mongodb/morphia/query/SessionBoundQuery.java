package org.mongodb.morphia.query;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.session.Session;

/**
 * You should NOT instantiate one of these directly. You should use <code>Session.query()</code> to construct one of
 * these bad boys for you. It's easier, and cleaner that way.
 *
 * This is a query implementation that attempts to utilize the cache of a specific session. Normally if you use the session to get
 * a record such as "User[A]" by its id, we'll store that entity in the session's front-side cache so that if something
 * else has a reference to that user and you need to lazy-load it, no DB query is required as it will be fetched from
 * the cache. If you execute some other standalone query that includes User[A] as one of the results, Morphia will create
 * a SECOND COPY of that entity which is returned as port of the search results. This is problematic for a few reasons:
 *
 * <ul>
 *     <li>You've performed the work to map the entity the session already knows about.</li>
 *     <li>Comparing both User[A] instances will fail an == test.</li>
 *     <li>If you update the "firstName" on one and "lastName" on the other and save both, you'll squash the first modification.</li>
 * </ul>
 *
 * This query implementation is bound to a specific session so that it can interact w/ its front-side cache, allowing
 * you to re-use instances of entities that were resolved by previous queries in the same session. Additionally any entities
 * that did need to be loaded/mapped from scratch when this query executes will be put into the session's cache to
 * receive the same benefits.
 */
public class SessionBoundQuery<T> extends QueryImpl<T>
{
    private static final Logger logger = MorphiaLoggerFactory.get(SessionBoundQuery.class);

    private Session session;

    /**
     * Creates a Query for the given type and collection
     * @param session The session that this query is bound to
     * @param clazz The entity class describing the collection to query
     * @param coll The MongoDB collection information to query
     * @param ds The datastore instance that's executing this query
     */
    public SessionBoundQuery(Session session, Class<T> clazz, final DBCollection coll, final Datastore ds)
    {
        super(clazz, coll, ds);
        this.session = session;
    }

    /**
     * Executes the query, constructing the results iterator so that you can pull entities at will.
     * @return The results iterator
     */
    @Override
    public MorphiaIterator<T, T> fetch()
    {
        final DBCursor cursor = prepareCursor();

        if (logger.isTraceEnabled())
            logger.trace("Getting cursor({0}) for query: {1}", getCollection().getName(), cursor.getQuery());

        session.getStats().read();

        // The iterator is the same as a standard Morphia query. The only difference is that we supply an EntityCache
        // that checks the session's front-side cache first before mapping a new instance from scratch.
        return new MorphiaIterator<>(
            getDatastore(),
            cursor,
            getDatastore().getMapper(),
            getEntityClass(),
            getCollection().getName(),
            new SessionQueryEntityCache(session, getDatastore().getMapper().createEntityCache()));
    }
}
