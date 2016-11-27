package org.mongodb.morphia.query;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.cache.EntityCacheStatistics;
import org.mongodb.morphia.session.Session;

/**
 * Standard Morphia EntityCache instances wrangle/cache mapped entities that are resolved during a query so that if you
 * encounter the same entity multiple times you only map it once. This cache decorator adds another layer of caching such
 * that if the entity you're describing is already in a session's front-side cache then we'll simply use that, ensuring
 * that we share previously-resolved instances.
 *
 * Additionally, any entities that are put into the EntityCache as a query optimization are also added to the session
 * cache so other queries in the session can utilize those previously-mapped objects as well.
 */
public class SessionQueryEntityCache implements EntityCache
{
    private static final Logger logger = MorphiaLoggerFactory.get(SessionQueryEntityCache.class);

    /** The decorated cache passed w/ the mapper that only lives for the life of the query. It's our fallback for session misses. */
    private EntityCache queryCache;
    /** The session whose front-side cache we'll check for already-resolved instances */
    private Session session;

    /**
     * Decorate the given morphia entity cache with checks for resolved entities in the specified session
     * @param session The session whose front-side cache we'll check
     * @param queryCache The standard entity cache that we'll fall back on to perform standard query cache operations
     */
    public SessionQueryEntityCache(Session session, EntityCache queryCache)
    {
        this.session = session;
        this.queryCache = queryCache;
    }

    /**
     * Looks for a Key in the cache
     * @param key the Key to search for
     * @return true if the Key is found
     */
    @Override
    public Boolean exists(Key<?> key)
    {
        return session.getCache().contains(key) || queryCache.exists(key);
    }

    /**
     * Clears the cache
     */
    @Override
    public void flush()
    {
        // No sir, do not flush the session's front side cache. Just pass along to the decorated cache.
        queryCache.flush();
    }

    /**
     * Returns the entity for a Key
     * @param key the Key to search for
     * @return the entity
     */
    @Override
    public <T> T getEntity(Key<T> key)
    {
        // Check the session first in case we've loaded the entity in another query or 'get' then fall back to the query cache
        return session.getCache().get(key)
            .map(entity -> {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Session cache hit {0}[{1}]", key.getType().getSimpleName(), key.getId());
                }

                session.getStats().cacheHit();
                stats().incHits();
                return entity;
            })
            .orElseGet(() -> queryCache.getEntity(key));
    }

    /**
     * Returns a proxy for the entity for a Key
     * @param key the Key to search for
     * @return the proxy
     */
    @Override
    public <T> T getProxy(Key<T> key)
    {
        // We have no equivalent for this in our sessions, so just pass along directly to the decorated cache
        return queryCache.getProxy(key);
    }

    /**
     * Notifies the cache of the existence of a Key
     * @param key The entity reference key
     * @param exists true if the Key represents an existing entity
     */
    @Override
    public void notifyExists(Key<?> key, boolean exists)
    {
        // We have no equivalent for this in our sessions, so just pass along directly to the decorated cache
        queryCache.notifyExists(key, exists);
    }

    /**
     * Adds an entity to the cache
     * @param key the Key of the entity
     * @param entity the entity to put in the cache
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void putEntity(Key<T> key, T entity)
    {
        // Add to both caches
        session.getCache().put((Class<T>)key.getType(), entity, key.getId());
        queryCache.putEntity(key, entity);
    }

    /**
     * Adds a proxy to the cache
     * @param key the Key of the entity
     * @param entityProxy the proxy to the target entity
     */
    @Override
    public <T> void putProxy(Key<T> key, T entityProxy)
    {
        queryCache.putProxy(key, entityProxy);
    }

    /**
     * @return the hit/miss stats for this cache
     */
    @Override
    public EntityCacheStatistics stats()
    {
        // We'll update it as we hit the front-side cache but the decorated cache maintains it.
        return queryCache.stats();
    }
}
