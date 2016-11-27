package org.mongodb.morphia.session.entities;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("LongIdEntity")
public class LongIdEntity
{
    @Id
    private long id;
    private String name;

    public LongIdEntity() {}
    public LongIdEntity(long id) { setId(id); }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
