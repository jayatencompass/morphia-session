package org.mongodb.morphia.session.entities;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("DoubleIdEntity")
public class DoubleIdEntity
{
    @Id
    private Double id;
    private String name;

    public DoubleIdEntity() {}
    public DoubleIdEntity(Double id) { setId(id); }

    public Double getId() { return id; }
    public void setId(Double id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
