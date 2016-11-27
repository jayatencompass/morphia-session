package org.mongodb.morphia.session.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("Group")
public class Group
{
    @Id
    private ObjectId id;
    private String name;

    public Group() {}
    public Group(ObjectId id) { setId(id); }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static Group create(ObjectId id, String name)
    {
        Group group = new Group(id);
        group.setName(name);
        return group;
    }

    public static ObjectId BOWLING_ID = new ObjectId("5835dfe9d82d81468e6a7754");
    public static ObjectId LEBOWSKI_ID = new ObjectId("5835dfe9d82d81468e6a7755");
    public static ObjectId PAINTING = new ObjectId("5835dfe9d82d81468e6a7756");

    public static Group createBowling()
    {
        return create(BOWLING_ID, "Bowling");
    }

    public static Group createLebowski()
    {
        return create(LEBOWSKI_ID, "Is a Lebowski");
    }

    public static Group createPainting()
    {
        return create(PAINTING, "Painting");
    }
}
