package org.mongodb.morphia.session.cache;

import org.mongodb.morphia.Key;

import java.util.Optional;

/**
 * Wrangles all of the caching levels/constructs that help us minimize round-trips to the server during a session.
 */
public interface SessionCache
{
    /**
     * Retrieves the entry from the cache if it exists
     * @return The matching cached item
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<T> get(Key<T> key)
    {
        return get((Class<T>)key.getType(), key.getId());
    }

    /**
     * Retrieves the entry from the cache if it exists
     * @param type The type of the target entity
     * @param id The entity's id
     * @return The matching cached item
     */
    <T> Optional<T> get(Class<T> type, Object id);

    /**
     * Puts the record into the cache
     * @param type The entity type
     * @param entity The record to store in the cache
     * @param id The id of the record we can use to look it up later
     * @return The input record (merely for convenience)
     */
    <T> T put(Class<T> type, T entity, Object id);

    /**
     * Checks whether or not the given record exists the in the cache.
     * @param type The entity type
     * @param id The record id
     * @return Does the record exist in the cache?
     */
    <T> boolean contains(Class<T> type, Object id);

    /**
     * Checks whether or not the given record exists in the cache
     * @param key The morphia 'key' which contains the entity type and id value
     * @return Does the record exist in the cache?
     */
    default <T> boolean contains(Key<T> key)
    {
        return contains(key.getType(), key.getId());
    }

    /**
     * Wipes out all entries in the cache.
     */
    void clear();
}
