package org.mongodb.morphia.session.id;

import java.util.UUID;

/**
 * An id generator that creates globally unique string values
 */
public class StringIdGenerator implements IdGenerator<String>
{
    /**
     * Creates a new, unique String
     * @return The newly created id
     */
    @Override
    public String createId()
    {
        return UUID.randomUUID().toString();
    }
}
