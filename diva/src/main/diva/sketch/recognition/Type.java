/*
 * $Id: Type.java,v 1.6 2001/07/22 22:01:55 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import java.util.HashMap;

/**
 * A unique identifier for the type of a piece of data that results
 * from a recognition.  For example, different drawings of squares
 * have different parameters (size, etc.) but they all have the same
 * type.
 *
 * <p> The type system implemented in Diva is semi-dynamic.  There is
 * a class SimpleData that is dynamically typed; the only data that it
 * contains is the type that is associated with a particular
 * recognized object.  For all other objects with semantic information
 * (e.g.  TextType, which contains the recognized text), the type
 * system uses Java's runtime type system.  This is an implementation
 * detail, and is transparent to the user.
 *
 * <p> There are two ways to get a handle to a type object.  The
 * static method Type.getType with a String name argument will return
 * a static type if the name has been registered as static.  Otherwise
 * it will return a dynamic type.  There is also a getType method
 * with a Class object argument that can be used to create a static
 * type.
 *
 * @see TypedData
 * @see SimpleData
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.6 $
 * @rating      Red
 */
public final class Type {
    /**
     * A mapping of type abbreviation strings to their fully-qualified
     * (i.e. with package) Java class names.
     */
    private static HashMap _staticTypes = new HashMap();
    
    /**
     * Store the Java class associated with the typed data.
     */
    private Class _class;

    /**
     * For SimpleData, also store its associated dynamic type ID.
     */
    private String _id;

    /**
     * The type to be associated with an unrecognized
     * piece of data.
     */
    public static final Type NO_TYPE = new Type(null, null);
    
    /**
     * Construct a new type object that uses the given
     * class object to implement its type identity.
     */
    private Type(Class c) {
        this(c, c.getName());
    }

    /**
     * Construct a new type object that uses the given
     * dynamic type identifier to implement its type identity.
     */
    private Type(String dynamicType) {
        this(SimpleData.class, dynamicType);
    }

    /**
     * Private utility constructor: use the given class
     * and dynamic (or static) type identifier.
     */
    private Type(Class c, String id) {
        _class = c;
        _id = id;
    }

    /**
     * Add a static type to the Type system.  The input should be
     * fully-qualified with the package name, e.g. foo.bar.Baz, and
     * this will establish the alias Baz in the type system.  This
     * method tries to load the class with the given name, and throws
     * an exception if there is a loading error.
     */
    public static void addStaticType(String staticType) throws ClassNotFoundException {
        Class c = Class.forName(staticType);
        _staticTypes.put(strip(staticType), staticType);
        _staticTypes.put(staticType, staticType);
    }

    /**
     * Return whether the given type object is equivalent to
     * this one.
     */
    public boolean equals(Object o) {
        if(o instanceof Type) {
            Type t = (Type)o;
            boolean sameClass = _class.equals(t._class);
            if(sameClass && _class.equals(SimpleData.class)) {
                return _id.equals(t.getID());
            }
            return sameClass;
        }
        return false;
    }

    /**
     * Return the type ID of this object.  This is useful
     * when the type is dynamic.
     */
    public String getID() {
        return _id;
    }

    /**
     * Return the parent type of this one.  Dynamically
     * typed objects have no parent type; statically typed
     * objects' parent type mirrors the parent type in the
     * Java class system.
     */
    public Type getParent() {
        if(this == NO_TYPE || _class.equals(SimpleData.class)) {
            return NO_TYPE;
        }
		
        Class c = _class.getSuperclass();
        return new Type(c);
    }

    /**
     * Override the hashCode() method so that
     * objects of the same type hash to the same
     * index.
     */
    public int hashCode() {
        if(_class.equals(SimpleData.class)) {
            return _id.hashCode();
        }
        else {
            return _class.hashCode();
        }
    }

    /**
     * Return whether the given type name has been
     * registered as a native type in this type
     * system.
     */
    public static boolean isStaticType(String typeID) {
        return _staticTypes.containsKey(typeID);
    }

    /**
     * Return the type object associated with the given
     * type name.  If the type has been registered as a
     * static type, return a static type, otherwise
     * return a dynamic type.
     */
    public static Type makeType(String typeName) {
        if(isStaticType(typeName)) {
            String qualType = (String)_staticTypes.get(typeName);
            try {
                Class c = Class.forName(qualType);
                return new Type(c);
            }
            catch(ClassNotFoundException e) {
                // this should never happen since we check
                // this when the static type is registered.
                throw new RuntimeException(e.toString());
            }
        }
        else {
            return new Type(typeName);
        }
    }

    /**
     * Return the type object associated with the given
     * class and register it as static.
     */
    public static Type makeType(Class c) {
        try {
            addStaticType(c.getName());
        }
        catch(ClassNotFoundException e) {
            //this should never happen
            throw new RuntimeException(e.toString());
        }
        return new Type(c);
    }

    /**
     * Strip the package name off the front of a fully-qualified class
     * name.
     */
    private static String strip(String classWithPackage) {
        int i = classWithPackage.lastIndexOf(".");
        return classWithPackage.substring(i+1);
    }

    /**
     * Return a string representation of the type object.
     */
    public String toString() {
        if(this == NO_TYPE) {
            return "NO_TYPE";
        }
        if(_class == SimpleData.class) {
            return _id;
        }
        return "<" + strip(_class.getName()) + ">";
    }    
}

