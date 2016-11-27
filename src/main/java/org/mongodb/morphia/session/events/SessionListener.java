package org.mongodb.morphia.session.events;

import org.mongodb.morphia.session.Session;

/**
 * Defines all of the events you can listen for during the lifecycle of a single session. Some of the events are
 * at the session level so you can detect state changes. Others occur at the record level so you can potentially
 * intercept changes/actions performed on records (e.g. detect when a record is about to be written so you can
 * update a last modified timestamp).
 */
public interface SessionListener
{
    // ---- Session-Level Events --------------------------

    /**
     * Fired as soon as the session is officially activated.
     * @param session The session that was activated
     */
    void beginning(Session session);

    /**
     * Called at the beginning of the "COMMITTING" phase BEFORE any actual datastore writes have occurred.
     * @param session The session that is being committed
     */
    void committing(Session session);

    /**
     * Called at the end of the "COMMITTING" phase AFTER all writes/deletes have occurred.
     * @param session The session that is being committed
     */
    void committed(Session session);

    /**
     * Called at the end of everything when the session is shut down for good.
     * @param session The session that is being closed
     */
    void closed(Session session);


    // ---- Record-Level Events -----------------------------

    /**
     * Called right before we write this entity as a new record to the datastore.
     * @param session The session that this write is occurring in
     * @param entity The entity to be written
     */
    void creating(Session session, Object entity);

    /**
     * Called right after we write this entity as a new record to the datastore.
     * @param session The session that this write is occurring in
     * @param entity The entity that was just written
     */
    void created(Session session, Object entity);

    /**
     * Called right before we write this entity as a modification to an existing record to the datastore.
     * @param session The session that this write is occurring in
     * @param entity The entity to be written
     */
    void updating(Session session, Object entity);

    /**
     * Called right after we write this entity as a modification to an existing record to the datastore.
     * @param session The session that this write is occurring in
     * @param entity The entity that was just written
     */
    void updated(Session session, Object entity);

    /**
     * Called right before we delete this entity from the datastore
     * @param session The session that this delete is occurring in
     * @param entity The entity to be deleted
     */
    void deleting(Session session, Object entity);

    /**
     * Called right after we delete this entity from the datastore
     * @param session The session that this delete is occurring in
     * @param entity The entity that was just deleted
     */
    void deleted(Session session, Object entity);
}
