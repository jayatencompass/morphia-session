package org.mongodb.morphia.session;

import org.mongodb.morphia.Morphia;

import java.util.Collection;

/**
 * Random helper utility methods that span a wide array of uses. Some of these functions are available in other libraries
 * but it's silly to include additional dependencies for some of this rather simple, yet annoying stuff.
 */
public class SessionUtils
{
    /**
     * Does the given string have a "meaningful" value? This means its non-null/empty. Additionally, all-whitespace
     * strings will return false as those are not "meaningful" values.
     * @param text The text to test
     * @return Does the text have a meaningful value?
     */
    public static boolean hasValue(String text)
    {
        return (text != null) && text.trim().length() > 0;
    }

    /**
     * The opposite of <code>hasValue(String)</code>. This returns true when the text has no meaningful value which
     * includes null strings, empty strings, and all-whitespace strings.
     * @param text The text to test
     * @return Is the text a meaningless value?
     */
    public static boolean isBlank(String text)
    {
        return !hasValue(text);
    }

    /**
     * Is this a non-null collection w/ at least 1 item in it?
     * @param collection The collection to test
     * @return Is it a collection with data in it?
     */
    public static boolean hasValue(Collection<?> collection)
    {
        return (collection != null) && collection.size() > 0;
    }

    /**
     * A null-safe way to clear a collection that might not be initialized
     * @param collection The collection to clear
     */
    public static void clear(Collection<?> collection)
    {
        if (collection != null)
            collection.clear();
    }

    /**
     * Grabs the mapped entity class for the given instance. This is useful if you're entity is actually some dynamic
     * proxy (like Guice gives you when using AOP) and you want to get the class of the actual defined entity w/
     * your mapping annotations.
     * @param morphia The morphia mapping object
     * @param entity The entity whose class you want
     * @return The entity class
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getEntityClass(Morphia morphia, T entity)
    {
        return (entity != null)
            ? (Class<T>)morphia.getMapper().getMappedClass(entity).getClazz()
            : null;
    }

    /**
     * Quietly and null-safely close the given resource. This will gobble up any exceptions that are thrown. Basically
     * this exists here to quietly close sessions as needed.
     * @param c The thing to close
     */
    public static void close(AutoCloseable c)
    {
        try
        {
            if (c != null)
                c.close();
        }
        catch (Exception e)
        {
            // The point is to be silent.... so be silent....
        }
    }
}
