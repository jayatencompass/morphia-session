package org.mongodb.morphia.session.id;

import org.bson.types.ObjectId;

/**
 * An id generator that creates mongo ObjectId instances.
 */
public class ObjectIdGenerator implements IdGenerator<ObjectId>
{
    /**
     * Creates a new, unique ObjectId
     * @return The newly created id
     */
    @Override
    public ObjectId createId()
    {
        // The ObjectId class already does all the heavy lifting for us.
        return new ObjectId();
    }
}
