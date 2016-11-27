package org.mongodb.morphia.session;

/**
 * Describes the "phase" of a session's lifecycle.
 */
public enum SessionState
{
    /** Created but not started (still needs to call 'begin') */
    PENDING,
    /** Able to execute queries, perform updates, etc. */
    ACTIVE,
    /** In the process of flushing all session changes to the database */
    COMMITTING,
    /** In the process of cancelling all session changes */
    ROLLING_BACK,
    /** Allows the session to track dirty and deleted records and write them to the datastore when the session commits */
    COMPLETE;

    public boolean isPending() { return this == PENDING; }
    public boolean isActive() { return this == ACTIVE; }
    public boolean isCommitting() { return this == COMMITTING; }
    public boolean isRollingBack() { return this == ROLLING_BACK; }
    public boolean isComplete() { return this == COMPLETE; }
}
