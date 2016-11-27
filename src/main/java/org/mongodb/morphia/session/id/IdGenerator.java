package org.mongodb.morphia.session.id;

/**
 * Id generators are workers that create unique identifiers for different types. When saving new records to sessions
 * we need to be able to generate a new, random id for the entity and these implementations take care of that.
 * @
 */
public interface IdGenerator<T>
{
    /**
     * Generates a brand new id for this type using whatever scheme the implementation deems meaningful.
     * @return The newly created id
     */
    T createId();
}
