package org.mongodb.morphia.session;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.session.entities.DoubleIdEntity;
import org.mongodb.morphia.session.entities.LongIdEntity;
import org.mongodb.morphia.session.entities.StringIdEntity;
import org.mongodb.morphia.session.entities.User;
import org.mongodb.morphia.session.id.Ids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * All of our tests for the id manager class
 */
public class IdsTest
{
    private static Morphia morphia;
    private Ids idManager;

    @BeforeClass
    public static void setupMorphia()
    {
        morphia = new Morphia();
        morphia.mapPackage("org.mongodb.morphia.sample.entities");
    }

    @Before
    public void createIds()
    {
        idManager = new Ids(morphia);
    }

    @Test
    public void testEmptyObjectIdNull()
    {
        assertTrue(idManager.isEmptyId(null));
    }

    @Test
    public void testEmptyObjectIdZero()
    {
        assertTrue(idManager.isEmptyId(new ObjectId(0, 0, (short) 0, 0)));
    }

    @Test
    public void testEmptyObjectIdValid()
    {
        assertFalse(idManager.isEmptyId(new ObjectId()));
    }

    @Test
    public void testEmptyIdLongZero()
    {
        assertTrue(idManager.isEmptyId(0L));
    }

    @Test
    public void testEmptyIdLongValid()
    {
        assertFalse(idManager.isEmptyId(12345L));
    }

    @Test
    public void testEmptyIdStringBlank()
    {
        assertTrue(idManager.isEmptyId(""));
    }

    @Test
    public void testEmptyIdStringValid()
    {
        assertFalse(idManager.isEmptyId("foo"));
    }

    @Test
    public void testGetIdNull()
    {
        assertEquals(null, idManager.getId(new User()));
    }

    @Test
    public void testGetIdObjectId()
    {
        assertEquals(User.DUDE_ID, idManager.getId(User.createDude()));
    }

    @Test
    public void testGetIdString()
    {
        assertEquals("12345", idManager.getId(new StringIdEntity("12345")));
    }

    @Test
    public void testGetIdLong()
    {
        assertEquals(12345L, idManager.getId(new LongIdEntity(12345L)));
    }

    @Test
    public void testSetIdNull()
    {
        User user = User.createDude();
        idManager.setId(user, null);
        assertEquals(null, user.getId());
    }

    @Test
    public void testSetIdObjectId()
    {
        User user = new User();
        idManager.setId(user, User.DUDE_ID);
        assertEquals(User.DUDE_ID, user.getId());
    }

    @Test
    public void testSetIdString()
    {
        StringIdEntity entity = new StringIdEntity();
        idManager.setId(entity, "12345");
        assertEquals("12345", entity.getId());
    }

    @Test
    public void testSetIdLong()
    {
        LongIdEntity entity = new LongIdEntity();
        idManager.setId(entity, 12345L);
        assertEquals(12345L, entity.getId());
    }
    
    @Test
    public void testCreateObjectId()
    {
        Object id = idManager.createId(new User());
        assertTrue(id instanceof ObjectId);
    }

    @Test
    public void testCreateStringId()
    {
        Object id = idManager.createId(new StringIdEntity());
        assertTrue(id instanceof String);
        assertTrue(id.toString().length() > 0);
    }

    @Test
    public void testCreateLongId()
    {
        Object id = idManager.createId(new LongIdEntity());
        assertTrue(id instanceof Long);
        assertTrue((Long)id != 0L);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateDoubleId()
    {
        // We don't have a double id generator so this should fail
        idManager.createId(new DoubleIdEntity());
    }
}
