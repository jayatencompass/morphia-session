package org.mongodb.morphia.session.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A cache implementation that uses a simple in-memory map to store entities during a session.
 */
public class MapCache implements SessionCache
{
    private Map<String, Object> cache;

    public MapCache()
    {
        cache = new HashMap<>();
    }

    /**
     * Retrieves the entry from the cache if it exists
     *
     * @param type The type of the target entity
     * @param id   The entity's id
     * @return The matching cached item
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type, Object id)
    {
        return Optional.ofNullable((T) cache.get(buildKey(type, id)));
    }

    /**
     * Puts the record into the cache
     *
     * @param type   The entity type
     * @param entity The record to store in the cache
     * @param id     The id of the record we can use to look it up later
     * @return The input record (merely for convenience)
     */
    @Override
    public <T> T put(Class<T> type, T entity, Object id)
    {
        cache.put(buildKey(type, id), entity);
        return entity;
    }

    /**
     * Checks whether or not the given record exists the in the cache.
     *
     * @param type The entity type
     * @param id   The record id
     * @return Does the record exist in the cache?
     */
    @Override
    public <T> boolean contains(Class<T> type, Object id)
    {
        return cache.containsKey(buildKey(type, id));
    }

    /**
     * Wipes out all entries in the cache.
     */
    @Override
    public void clear()
    {
        cache.clear();
    }

    /**
     * Constructs the standard lookup key for the given entry
     * @param type The entity type
     * @param id The id of the target entity
     * @return The lookup key for the target entity
     */
    private <T> String buildKey(Class<T> type, Object id)
    {
        return type.getSimpleName() + ":" + id;
    }
}
