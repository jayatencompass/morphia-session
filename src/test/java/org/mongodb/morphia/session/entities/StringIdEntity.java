package org.mongodb.morphia.session.entities;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("StringIdEntity")
public class StringIdEntity
{
    @Id
    private String id;
    private String name;

    public StringIdEntity() {}
    public StringIdEntity(String id) { setId(id); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
