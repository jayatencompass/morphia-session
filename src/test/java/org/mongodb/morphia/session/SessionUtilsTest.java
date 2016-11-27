package org.mongodb.morphia.session;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.session.entities.Group;
import org.mongodb.morphia.session.entities.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * All of our tests for the id manager class
 */
public class SessionUtilsTest
{
    private static Morphia morphia;

    @BeforeClass
    public static void setupMorphia()
    {
        morphia = new Morphia();
        morphia.mapPackage("org.mongodb.morphia.sample.entities");
    }

    @Test
    public void testHasValueStringNull()
    {
        assertFalse(SessionUtils.hasValue((String)null));
    }

    @Test
    public void testHasValueStringEmpty()
    {
        assertFalse(SessionUtils.hasValue(""));
    }

    @Test
    public void testHasValueStringSpace()
    {
        assertFalse(SessionUtils.hasValue("  \n \t"));
    }

    @Test
    public void testHasValueStringValid()
    {
        assertTrue(SessionUtils.hasValue("  foo "));
    }

    @Test
    public void testHasValueCollectionNull()
    {
        assertFalse(SessionUtils.hasValue((Collection<?>) null));
    }

    @Test
    public void testHasValueCollectionEmpty()
    {
        assertFalse(SessionUtils.hasValue(Collections.emptyList()));
    }

    @Test
    public void testHasValueCollectionNullSingleton()
    {
        List<String> list = new ArrayList<>();
        list.add(null);
        assertTrue(SessionUtils.hasValue(list));
    }

    @Test
    public void testHasValueCollectionValid()
    {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add("baz");
        assertTrue(SessionUtils.hasValue(list));
    }

    @Test
    public void testClearCollectionNull()
    {
        SessionUtils.clear(null);
        assertTrue(true);
    }

    @Test
    public void testClearCollection()
    {
        List<String> items = new ArrayList<>();
        items.add("foo");
        items.add("bar");
        items.add("baz");
        SessionUtils.clear(items);
        assertTrue(items.size() == 0);
    }

    @Test
    public void testGetEntityClassUser()
    {
        Class<?> type = SessionUtils.getEntityClass(morphia, new User());
        assertEquals(User.class, type);
    }

    @Test
    public void testGetEntityClassGroup()
    {
        Class<?> type = SessionUtils.getEntityClass(morphia, new Group());
        assertEquals(Group.class, type);
    }
}
