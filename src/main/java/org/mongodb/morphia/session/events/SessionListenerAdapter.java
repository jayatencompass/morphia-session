package org.mongodb.morphia.session.events;

import org.mongodb.morphia.session.Session;

/**
 * A handy base class that provides "do-nothing" implementations of all SessionListener operations so that you only
 * need to implement the one or two that you need to accomplish your task. For instance if you only log something when
 * a session finishes committing, you only need to implement <code>committed()</code> rather than stub out the other
 * eight listener methods.
 */
public abstract class SessionListenerAdapter implements SessionListener
{
    @Override
    public void beginning(Session session) { }

    @Override
    public void committing(Session session) { }

    @Override
    public void committed(Session session) { }

    @Override
    public void closed(Session session) { }

    @Override
    public void creating(Session session, Object entity) { }

    @Override
    public void created(Session session, Object entity) { }

    @Override
    public void updating(Session session, Object entity) { }

    @Override
    public void updated(Session session, Object entity) { }

    @Override
    public void deleting(Session session, Object entity) { }

    @Override
    public void deleted(Session session, Object entity) { }
}
