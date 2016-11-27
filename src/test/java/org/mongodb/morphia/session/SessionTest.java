package org.mongodb.morphia.session;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.session.entities.User;
import org.mongodb.morphia.session.events.SessionListener;
import org.mongodb.morphia.session.events.SessionListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mongodb.morphia.session.SessionMode.UPDATE;
import static org.mongodb.morphia.session.SessionState.*;

/**
 * Tests basic session functionality that is not directly tied to morphia/mongo (like state management).
 */
public class SessionTest
{
    private Morphia morphia;
    private Datastore datastore;
    private Session session;

    @Before
    public void beforeTest()
    {
        morphia = mock(Morphia.class);
        datastore = mock(Datastore.class);
        session = new MorphiaSession(morphia, datastore);
    }

    @After
    public void afterTest()
    {
        SessionUtils.close(session);
        session = null;
        morphia = null;
        datastore = null;
    }

    @Test
    public void testBeginState()
    {
        assertEquals(SessionState.PENDING, session.getState());

        session.begin(SessionMode.READ_ONLY);
        assertEquals(ACTIVE, session.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void testBeginTwice()
    {
        session.begin(SessionMode.READ_ONLY).begin(SessionMode.READ_ONLY);
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveActive()
    {
        session.save(User.createJackie());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetActive()
    {
        session.get(User.class, User.DUDE_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetBatchActive()
    {
        session.get(User.class, Arrays.asList(User.DUDE_ID, User.WALTER_ID));
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteActive()
    {
        session.delete(User.createDude());
    }

    @Test
    public void testClosedStateCommit()
    {
        session.begin(UPDATE).commit();
        assertEquals(SessionState.COMPLETE, session.getState());
    }

    @Test
    public void testClosedStateRollback()
    {
        session.begin(UPDATE).rollback();
        assertEquals(SessionState.COMPLETE, session.getState());
    }

    @Test
    public void testClosedStateClose() throws Exception
    {
        session.begin(UPDATE).close();
        assertEquals(SessionState.COMPLETE, session.getState());
    }

    @Test
    public void testSessionListenerReadOnly()
    {
        List<SessionState> states = new ArrayList<>();
        session
            .listen(createStateTrackingListener(states))
            .begin(SessionMode.READ_ONLY);

        session.commit();
        assertThat(states, CoreMatchers.is(Arrays.asList(ACTIVE, COMPLETE)));
    }

    @Test
    public void testSessionListenerUpdate()
    {
        List<SessionState> states = new ArrayList<>();
        session
            .listen(createStateTrackingListener(states))
            .begin(SessionMode.UPDATE);

        session.commit();
        assertThat(states, CoreMatchers.is(Arrays.asList(ACTIVE, COMMITTING, COMPLETE, COMPLETE)));
    }

    @Test
    public void testSessionListenerMultipleUpdate()
    {
        List<SessionState> states = new ArrayList<>();
        session
            .listen(createStateTrackingListener(states), createStateTrackingListener(states))
            .begin(SessionMode.UPDATE);

        session.commit();
        assertThat(states, CoreMatchers.is(Arrays.asList(
            ACTIVE, ACTIVE, COMMITTING, COMMITTING, COMPLETE, COMPLETE, COMPLETE, COMPLETE)));
    }

    /**
     * Creates a session that adds the current state to the given list so we can audit the sequence of events
     * @param states The states list to update
     * @return The new listener
     */
    protected SessionListener createStateTrackingListener(List<SessionState> states)
    {
        return new SessionListenerAdapter() {
            @Override public void beginning(Session session) { states.add(ACTIVE); }
            @Override public void committing(Session session) { states.add(SessionState.COMMITTING); }
            @Override public void committed(Session session) { states.add(SessionState.COMPLETE); }
            @Override public void closed(Session session) { states.add(SessionState.COMPLETE); }
        };
    }
}
