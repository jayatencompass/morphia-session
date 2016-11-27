package org.mongodb.morphia.session.id;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.session.SessionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Central hub for interacting with ids: retrieving them, updating them, and generating them.
 */
public class Ids
{
    private Morphia morphia;
    private Map<Class<?>, IdGenerator<?>> idGenerators;

    public Ids(Morphia morphia)
    {
        this.morphia = morphia;
        this.idGenerators = new HashMap<>();
        register(ObjectId.class, new ObjectIdGenerator());
        register(String.class, new StringIdGenerator());
        register(Long.class, new LongIdGenerator());
        register(Long.TYPE, new LongIdGenerator());
    }

    /**
     * Looks up the id declaration for the entity and creates a brand new id instance of the appropriate type. This
     * does NOT apply the new id to the entity. The entity is purely for type-lookup purposes.
     * @param entity The entity with the class definition so we can look up the id type
     * @return A newly created id that is compatible with the given 'entity' or any other of that type
     * @throws IllegalStateException When the 'id' type of the entity has no IdGenerator registered
     */
    public Object createId(Object entity)
    {
        Field idField = getIdField(entity);
        IdGenerator<?> generator = idGenerators.get(idField.getType());
        if (generator == null)
            throw new IllegalStateException("Unable to create 'id' for entity of type " + idField.getType());

        return generator.createId();
    }

    /**
     * Being an empty or blank 'id' means different things depending on the type. For instance if it's an ObjectId
     * a null check is likely sufficient. If it's a Long then we need to check for 0 as well as null, and so on.
     * @param id The id to test
     * @return True if the 'id' does not have a meaningful value
     */
    public boolean isEmptyId(Object id)
    {
        if (id == null)
            return true;

        // Don't consider an 'all zero' ObjectId valid
        if (id instanceof ObjectId)
        {
            ObjectId oid = (ObjectId)id;
            return (oid.getTimestamp() == 0) &&
                (oid.getMachineIdentifier() == 0) &&
                (oid.getProcessIdentifier() == 0) &&
                (oid.getCounter() == 0);
        }

        // In case it's a primitive long don't just accept the default value
        if (id instanceof Long)
            return (Long) id == 0;

        // Empty strings don't count, sucka'
        if (id instanceof String)
            return SessionUtils.isBlank((String)id);

        return false;
    }

    /**
     * Uses morphia's internal mapping structure to extract the "_id" value of the given entity.
     * @param entity The entity whose id you want
     * @return The current id of the entity
     */
    public Object getId(Object entity)
    {
        try
        {
            return (entity == null)
                ? null
                : getIdField(entity).get(entity);
        }
        catch (IllegalAccessException e)
        {
            // Morphia already unlocks private _id fields so we shouldn't ever hit this.
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses morphia's internal mapping to update/modify the "_id" value of the given entity.
     * @param entity The entity whose id you want to update
     * @param id The id to apply to the entity
     * @return The 'entity' argument, now theoretically with an updated id
     */
    public Object setId(Object entity, Object id)
    {
        try
        {
            if (entity == null)
                return null;

            getIdField(entity).set(entity, id);
            return entity;
        }
        catch (IllegalAccessException e)
        {
            // Morphia already unlocks private _id fields so we shouldn't ever hit this.
            throw new RuntimeException(e);
        }
    }

    /**
     * Tells this id manager to use the given id generator to create new ids for all id fields of the given type. For
     * instance if you have a special way you want to generate String ids, you can specify the generator used to build
     * unique string ids.
     * @param type The id type/class that this will generate ids for
     * @param generator The worker that will generate ids when asked
     * @return This instance for fluent-style chaining.
     */
    public <T> Ids register(Class<T> type, IdGenerator<T> generator)
    {
        idGenerators.put(type, generator);
        return this;
    }

    /**
     * Retrieves the reflection Field handle for this entity's "_id"
     * @param entity The entity whose _id you want
     * @return The field instance
     */
    public Field getIdField(Object entity)
    {
        return (entity != null)
            ? morphia.getMapper().getMappedClass(entity).getIdField()
            : null;
    }
}
