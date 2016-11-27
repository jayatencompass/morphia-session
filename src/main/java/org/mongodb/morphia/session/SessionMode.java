package org.mongodb.morphia.session;

/**
 * Specifies the persistence mode for a session. Essentially it distinguishes a "read only" session which only allows
 * you to fetch data from an "update" session where you can write/delete records.
 */
public enum SessionMode
{
    /** Prevents any writes/deletes from being flushed to the database */
    READ_ONLY,
    /** Allows the session to track dirty and deleted records and write them to the datastore when the session commits */
    UPDATE;

    /** @return Is this the READ_ONLY instance? */
    public boolean isReadOnly() { return this == READ_ONLY; }
    /** @return Is this the UPDATE instance? */
    public boolean isUpdate() { return this == UPDATE; }
}
