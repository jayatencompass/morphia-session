Morphia Session implements the "[Unit Of Work](http://martinfowler.com/eaaCatalog/unitOfWork.html)" 
pattern for data access using Morphia/MongoDB. It caches your reads so you don't query for the
same record multiple times in the same operation. It also tracks and defers writes/deletes so 
that they can be batched in an all-or-nothing fashion.

## Getting Started
 
Here's a simple example that transforms a typical Morphia-based operation into one that 
uses sessions.

**Standard Morphia**
```java
public void sendMessage(ObjectId a, ObjectId b, String text) {
    try {
        Instant timestamp = Instant.now();
    
        User userA = datastore.get(User.class, a);
        userA.setLastMessageTimestamp(timestamp);
        datastore.save(userA);
    
        User userB = datastore.get(User.class, b);
        userA.setLastMessageTimestamp(timestamp);
        datastore.save(userB);
    
        Message message = new Message(userA, userB);
        message.setTimestamp(timestamp);
        message.setText(text);
        datastore.save(message);
    }
    catch (Exception e) {
        e.printStackTrace();
    }
}
```

**With Morphia Sessions**
```java
public void sendMessage(ObjectId a, ObjectId b, String text) {
    try (Session session = new MorphiaSession(morphia, datastore)) {
        Instant timestamp = Instant.now();
    
        User userA = session.get(User.class, a);
        userA.setLastMessageTimestamp(timestamp);
        session.save(userA);
    
        User userB = session.get(User.class, b);
        userA.setLastMessageTimestamp(timestamp);
        session.save(userB);
    
        Message message = new Message(userA, userB);
        message.setTimestamp(timestamp);
        message.setText(text);
        session.save(message);
    
        // If we reach this line, flush all 3 pending writes to the DB in 1 round trip
        session.commit();
    }
    catch (Exception e)
    {
        // Nothing is written to the database if we get here
        e.printStackTrace();
        session.rollback();
    }
}
```

By introducing sessions into the mix we now have the following benefits:

* If ```message.setText()``` throws an exception in the first version, you've already written
both updated users back to the database so it's likely in a goofy state, now. In the session version
```session.save()``` only marks the entity for writing. Nothing is written until you ```commit()```
so even though the exception is thrown nothing is written. It's all-or-nothing.
* If everything goes as expected in the first version, you've made 5 round trips to your
database; the 2 reads for each user, the 2 writes for each user, and the last write for the
new message. In the session version, you only make 3 database trips; the 2 reads but all 3 
updated entities are written in 1 trip to the database even though you called ```save()``` in 
different spots.
* Let's pretend that a user is sending a message to themselves ('a' and 'b' are the same id).
In the first example you will query the exact same record twice and map it two separate times.
While ```userA.equals(userB)``` is true, ```userA == userB``` is not because they are actually
copies of the same record. In the session version, however, retrieving User B actually hits 
the session cache and returns the same instance that was generated when you grabbed User A.
This means that ```userA == userB``` and you don't have to worry about updating two 
separate copies of the same record.