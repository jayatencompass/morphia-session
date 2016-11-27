package org.mongodb.morphia.session.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Entity("User")
@Indexes(
    @Index(fields = @Field("email"))
)
public class User
{
    @Id
    private ObjectId id;
    private String name;
    private String email;
    private boolean male;
    @Reference
    private Collection<Group> groups;

    public User() {}
    public User(ObjectId id) { setId(id); }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isMale() { return male; }
    public void setMale(boolean male) { this.male = male; }


    // -------- Some Test Data For Our Unit/Integration Tests ---------------------------------

    // The 4 records that are in the testing DB to state w/
    public static ObjectId DUDE_ID = new ObjectId("5835dfe9d82d81468e6a7759");
    public static ObjectId WALTER_ID = new ObjectId("5835dfe9d82d81468e6a775a");
    public static ObjectId DONNIE_ID = new ObjectId("5835dfe9d82d81468e6a775b");
    public static ObjectId MAUDE_ID = new ObjectId("5835dfe9d82d81468e6a775c");

    // Records we attempt to add as "creation" tests
    public static ObjectId JACKIE_ID = new ObjectId("5835dfe9d82d81468e6a775d");

    public Collection<Group> getGroups()
    {
        if (groups == null)
            groups = new ArrayList<>();

        return groups;
    }

    public static User create(ObjectId id, String name, String email, boolean male, Group... groups)
    {
        User user = new User(id);
        user.setName(name);
        user.setEmail(email);
        user.setMale(male);
        Collections.addAll(user.getGroups(), groups);
        return user;
    }

    public static User createDude(Group... groups)
    {
        return User.create(DUDE_ID, "Jeffrey Lebowski", "dude@example.com", true, groups);
    }

    public static User createWalter(Group... groups)
    {
        return User.create(WALTER_ID, "Walter Sobchack", "walter@example.com", true, groups);
    }

    public static User createDonnie(Group... groups)
    {
        return User.create(DONNIE_ID, "Donald Kerabatsos", "donnie@example.com", true, groups);
    }

    public static User createMaude(Group... groups)
    {
        return User.create(MAUDE_ID, "Maude Lebowski", "maude@example.com", false, groups);
    }

    public static User createJackie(Group... groups)
    {
        return User.create(JACKIE_ID, "Jackie Treehorn", "treehorn@example.com", true, groups);
    }
}
