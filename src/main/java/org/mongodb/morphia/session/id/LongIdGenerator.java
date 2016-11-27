package org.mongodb.morphia.session.id;

/**
 * An id generator that creates globally unique long values (or as close as we can get)
 */
public class LongIdGenerator implements IdGenerator<Long>
{
    /**
     * Creates a new, unique long
     * @return The newly created id
     */
    @Override
    public Long createId()
    {
        // TODO: Use a bit-based approach like ObjectId using timestamp, machine, process, and counter. This works for now.
        return java.util.UUID.randomUUID().getLeastSignificantBits();
    }
}
