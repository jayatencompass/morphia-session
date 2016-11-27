package org.mongodb.morphia.session;

import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.junit.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.session.entities.Group;
import org.mongodb.morphia.session.entities.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the general behavior of getting/saving records from a session. This connects to a real MongoDB database to
 * make sure that our integration with the real Morphia/MongoDB API is sound.
 */
public class SessionIntegrationTest
{
    private static final String DATABASE_NAME = "moprhia-session-test-data";

    private static MongoClient mongo;
    private static Morphia morphia;
    private static Datastore datastore;
    private Session session;

    // --------- SETUP AND TEAR DOWN -----------------------------------


    /**
     * Create the connection pool to the testing database and have morphia already build the mapping for our entities
     */
    @BeforeClass
    public static void setupTestDatabase()
    {
        // Start w/ a fresh database each time we run the tests
        mongo = new MongoClient("localhost",  27017);
        mongo.dropDatabase(DATABASE_NAME);

        morphia = new Morphia();
        morphia.mapPackage("org.mongodb.morphia.sample.entities");

        datastore = morphia.createDatastore(mongo, DATABASE_NAME);
        datastore.ensureIndexes();
    }

    /**
     * Tear down all remnants of the testing data/database
     */
    @AfterClass
    public static void tearDownDatabase()
    {
        mongo.dropDatabase(DATABASE_NAME);
        mongo.close();
        mongo = null;
        morphia = null;
        datastore = null;
    }

    /**
     * Reconstruct the handful of testing records we use for each test
     */
    @Before
    public void startSession()
    {
        datastore.delete(datastore.createQuery(User.class));
        datastore.delete(datastore.createQuery(Group.class));

        Group bowling = Group.createBowling();
        Group lebowski = Group.createLebowski();
        Group painting = Group.createPainting();

        datastore.save(bowling);
        datastore.save(lebowski);
        datastore.save(painting);

        datastore.save(User.createDude(bowling, lebowski));
        datastore.save(User.createWalter(bowling));
        datastore.save(User.createDonnie(bowling, painting));
        datastore.save(User.createMaude(lebowski, painting));

        session = new MorphiaSession(morphia, datastore);
    }

    /**
     * Close the session, but leave the connections open. The next test's 'Before' will reset the test data for its own test.
     */
    @After
    public void endSession() throws Exception
    {
        try
        {
            if (session != null)
                session.close();
        }
        finally
        {
            session = null;
        }
    }



    // ---------- TESTS -----------------------------------------------

    /**
     * Looking up an id that isn't in the database should return null
     */
    @Test
    public void testGetMissing()
    {
        session.begin(SessionMode.READ_ONLY);

        assertNull(session.get(User.class, new ObjectId()));
    }

    /**
     * Make sure that looking up a valid record works
     */
    @Test
    public void testGet()
    {
        session.begin(SessionMode.READ_ONLY);

        User dude = session.get(User.class, User.DUDE_ID);
        assertNotNull(dude);
        assertEquals("Jeffrey Lebowski", dude.getName());
    }

    /**
     * Look up a batch of records by id where all ids are in the database
     */
    @Test
    public void testGetBatch()
    {
        session.begin(SessionMode.READ_ONLY);

        Collection<ObjectId> ids = Arrays.asList(User.DUDE_ID, User.DONNIE_ID);
        Collection<User> users = session.get(User.class, ids);
        assertNotNull(users);
        assertEquals(2, users.size());

        // Make sure id order is preserved
        Iterator<User> userIterator = users.iterator();
        assertEquals(User.DUDE_ID, userIterator.next().getId());
        assertEquals(User.DONNIE_ID, userIterator.next().getId());
    }

    /**
     * Adding a new record auto-assigns the id and can be pulled up in a subsequent call
     */
    @Test
    public void testCreate()
    {
        session.begin(SessionMode.UPDATE);

        User jackie = User.create(null, "Jackie Treehorn", "treehorn@example.com", true);

        // Make sure an id was auto-assigned
        assertTrue(jackie.getId() == null);
        session.save(jackie);

        ObjectId jackieId = jackie.getId();
        assertTrue(jackieId != null && jackieId.toString().length() > 0);
        session.commit();

        // Going through the datastore directly should ensure the retrieved record is not a relic of bad caching in a new session
        jackie = datastore.get(User.class, jackieId);
        assertEquals("Jackie Treehorn", jackie.getName());
    }

    /**
     * Can add multiple records of different entity types in the same session
     */
    @Test
    public void testCreateMultiple()
    {
        session.begin(SessionMode.UPDATE);

        User jackie = User.create(null, "Jackie Treehorn", "treehorn@example.com", true);
        Group ralphs = Group.create(null, "Ralph's");

        session.save(jackie);
        session.save(ralphs);

        ObjectId jackieId = jackie.getId();
        ObjectId ralphsId = ralphs.getId();
        session.commit();

        // Going through the datastore directly should ensure the retrieved record is not a relic of bad caching in a new session
        jackie = datastore.get(User.class, jackieId);
        ralphs = datastore.get(Group.class, ralphsId);
        assertEquals("Jackie Treehorn", jackie.getName());
        assertEquals("Ralph's", ralphs.getName());
    }

    /**
     * Updating fields on an entity should be saved to the DB after commit so subsequent retrievals have those new values.
     */
    @Test
    public void testUpdate()
    {
        session.begin(SessionMode.UPDATE);

        User dude = session.get(User.class, User.DUDE_ID);
        assertEquals("Jeffrey Lebowski", dude.getName());
        dude.setName("El Duderino");
        dude.setEmail("duderino@example.com");
        session.save(dude);
        session.commit();

        // Going through the datastore directly should ensure the retrieved record is not a relic of bad caching in a new session
        dude = datastore.get(User.class, User.DUDE_ID);
        assertEquals("El Duderino", dude.getName());
        assertEquals("duderino@example.com", dude.getEmail());
    }

    /**
     * Make sure that deleted records are really gone from the datastore
     */
    @Test
    public void testDelete()
    {
        session.begin(SessionMode.UPDATE);

        // Poor Donnie...
        session.delete(session.get(User.class, User.DONNIE_ID));
        session.commit();

        assertEquals(null, datastore.get(User.class, User.DONNIE_ID));
    }

    /**
     * Make sure a simple query that is created by the session works
     */
    @Test
    public void testQuery()
    {
        session.begin(SessionMode.READ_ONLY);

        List<User> results = session.query(User.class)
            .field("male").equal(false)
            .asList();

        assertEquals(1, results.size());
        assertEquals("Maude Lebowski", results.get(0).getName());
    }

    @Test
    public void testStatsTracking()
    {
        session.begin(SessionMode.UPDATE);

        User dude = session.get(User.class, User.DUDE_ID);
        session.query(User.class)
            .field("male").equal(true)
            .asList();

        User walter = session.get(User.class, User.WALTER_ID);
        User maude = session.get(User.class, User.MAUDE_ID);
        User jackie = User.createJackie();

        session.delete(walter);
        session.delete(walter);   // should not mark as another delete
        session.save(dude);
        session.save(maude);
        session.save(maude);   // should not mark as another write
        session.save(maude);   // should not mark as another write
        session.save(jackie);
        session.save(jackie);  // should not mark as another write
        session.commit();

        // The cache hits also include the @Reference mappings for groups
        assertEquals(6, session.getStats().getCacheHits());
        assertEquals(3, session.getStats().getTotalReads());
        assertEquals(3, session.getStats().getTotalWrites());
        assertEquals(1, session.getStats().getTotalDeletes());
    }
}
