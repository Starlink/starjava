package uk.ac.starlink.plastic;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Defines the type of a value which is passed through the PLASTIC 
 * messaging system.  These types define what is legal for the
 * arguments and return values of each particular message.
 * The types defined are ones which can be transmitted and have the
 * same meaning using both the Java RMI and the XML-RPC transport modes.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2006
 * @see      MessageDefinition
 */
public class ValueType {

    private final String name_;
    private final Class jClazz_;
    private final Object blank_;

    /**
     * Constructs a basic value type with a given symbolic name and a
     * java class which all legal values of this type must instantiate.
     *
     * @param  name   label for this type (for documentation purposes only)
     * @param  jClazz  required assignable java class
     * @param  blank   suitable blank value (see {@link #getBlankValue})
     */
    public ValueType( String name, Class jClazz, Object blank ) {
        name_ = name;
        jClazz_ = jClazz;
        blank_ = blank;
    }

    /**
     * Checks a value sent or received using Java-RMI.
     * If the value has a type which is inconsistent with the constraints
     * defined by this object, a <code>ValueTypeException</code> will be
     * thrown.
     *
     * @param   jValue   value to check
     */
    public void checkJavaValue( Object jValue ) throws ValueTypeException {
        if ( jValue == null ) {
            throw new ValueTypeException( "Null value illegal" );
        }
        else if ( ! jClazz_.isAssignableFrom( jValue.getClass() ) ) {
            throw new ValueTypeException( jValue
                                        + " (" + objClassName( jValue ) + ")"
                                        + " is not a " + className( jClazz_ ) );
        }
    }

    /**
     * Returns a neutral sort of value which is legal for this type.
     * This is not guaranteed to be suitable for any given use, but
     * if you have to come up with a value for this type and don't have
     * any other information, it's as good a guess as any.
     *
     * @return   blank value compatible with this type
     */
    public Object getBlankValue() {
        return blank_;
    }

    public String toString() {
        return name_;
    }

    /** No constraints - any object is permissible. */
    public static ValueType ANY = new ValueType( "ANY", Object.class, "" ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            checkObject( jValue );
        }
    };

    /** Integer type. */
    public static ValueType INT =
        new ValueType( "INT", Integer.class, Integer.valueOf( 0 ) );

    /** Boolean type. */
    public static ValueType BOOLEAN =
        new ValueType( "BOOLEAN", Boolean.class, Boolean.FALSE );

    /** String type. */
    public static ValueType STRING =
        new ValueType( "STRING", String.class, "" );

    /** Double precision type. */
    public static ValueType DOUBLE =
        new ValueType( "DOUBLE", Double.class, Double.valueOf( 0.0 ) );

    /** ISO-8601 date type. */
    public static ValueType DATE =
        new ValueType( "DATE", Date.class, new Date( 0L ) );

    /** Map/&lt;struct&gt; type. */
    public static ValueType MAP = new ValueType( "MAP", Map.class,
                                                 new Hashtable() ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            checkMap( (Map) jValue );
        }
    };

    /** List/&lt;array&gt; type. */
    public static ValueType LIST = new ValueType( "LIST", List.class,
                                                  new Vector() ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            checkList( (List) jValue );
        }
    };

    /** Void type - the return type for methods with no return value. */
    public static ValueType VOID = new ValueType( "VOID", null, null ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {

            /* Because of potential difficulties returning null values via
             * XML-RPC, allow empty strings or lists. */
            if ( jValue == null ||
                 "".equals( jValue ) ||
                 ( ( jValue instanceof Collection ) &&
                   ((Collection) jValue).isEmpty() ) ) {
                // ok
            }
            else {
                throw new ValueTypeException( "Value " + jValue +
                                              " should have been null" );
            }
        }
    };

    /** Type for a string which is required to be a legal URL. */
    public static ValueType STRING_URL = new ValueType( "STRING_URL",
                                                        String.class, "" ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            String str = (String) jValue;
            try {
                new URL( str );
                if ( str.matches( "file:/?[^/].*" ) ) {
                    throw new ValueTypeException( "file:-type URL " + str + " "
                                                + "violates RFC1738 - " 
                                                + "should be "
                                                + "file://localhost/..." );
                }
            }
            catch ( MalformedURLException e ) {
                throw new ValueTypeException( "Badly formed URL: " + str, e );
            }
        }
    };

    /** Type for a string which is required to be a legal IVORN. */
    public static ValueType STRING_IVORN = new ValueType( "STRING_IVORN",
                                                          String.class, "" ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            String str = (String) jValue;
            try {
                new URI( str );
                if ( ! str.startsWith( "ivo:" ) ) {
                    throw new ValueTypeException( "IVORN should start ivo: "
                                                + " not " + str );
                }
            }
            catch ( URISyntaxException e ) {
                throw new ValueTypeException( "Badly formed URI: " + str, e );
            }
        }
    };

    /** Type for a string which is required to be a legal URI. */
    public static ValueType STRING_URI = new ValueType( "STRING_URI",
                                                        String.class, "" ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            String str = (String) jValue;
            try {
                new URI( str );
            }
            catch ( URISyntaxException e ) {
                throw new ValueTypeException( "Badly formed URI: " + str, e );
            }
        }
    };

    /** List/&lt;array&gt; type in which all elements must be integers. */
    public static ValueType LIST_INTS = new ValueType( "LIST_INTS", List.class,
                                                       new Vector() ) {
        public void checkJavaValue( Object jValue ) throws ValueTypeException {
            super.checkJavaValue( jValue );
            List list = (List) jValue;
            for ( Iterator it = list.iterator(); it.hasNext(); ) {
                if ( ! ( it.next() instanceof Integer ) ) {
                    throw new ValueTypeException( "List elements are not all "
                                                + "Integers" );
                }
            }
        }
    };

    /**
     * Guesses from a single value what type it might correspond to.
     * Necessarily unreliable.
     *
     * @param  value  example value
     * @return  ValueType that <code>value</code> might be an instance of
     */
    public static ValueType inferValueType( Object value )
            throws ValueTypeException {
        checkObject( value );
        ValueType[] tries = new ValueType[] {
            INT, BOOLEAN, STRING, DOUBLE, DATE, LIST, MAP,
        };
        for ( int i = 0; i < tries.length; i++ ) {
            try {
                tries[ i ].checkJavaValue( value );
                return tries[ i ];
            }
            catch ( ValueTypeException e ) {
                // nope.
            }
        }
        throw new ValueTypeException( "Value not of legal type: " + value );
    }

    /**
     * Checks whether an arbitrary Java object is of an acceptable type for
     * use in a PLASTIC call.
     *
     * @param   obj  object
     */
    private static void checkObject( Object obj ) throws ValueTypeException {
        if ( obj == null ) {
            throw new ValueTypeException( "Null value illegal" );
        }
        else {
            Class clazz = obj.getClass();
            if ( clazz == Integer.class ||
                 clazz == Boolean.class ||
                 clazz == String.class ||
                 clazz == Double.class ||
                 clazz == Date.class ||
                 clazz == byte[].class ) {
                return;
            }
            else if ( Map.class.isAssignableFrom( clazz ) ) {
                checkMap( (Map) obj );
            }
            else if ( List.class.isAssignableFrom( clazz ) ) {
                checkList( (List) obj );
            }
            else {
                throw new ValueTypeException( "Unsupported type " + clazz
                                            + " of " + obj );
            }
        }
    }

    /**
     * Checks whether a List is suitable for use in a PLASTIC call.
     *
     * @param  list  list
     */
    private static void checkList( List list ) throws ValueTypeException {
        for ( Iterator it = list.iterator(); it.hasNext(); ) {
            checkObject( it.next() );
        }
    }

    /**
     * Checks whether a Map is suitable for use in a PLASTIC call.
     *
     * @param   map  map
     */
    private static void checkMap( Map map ) throws ValueTypeException {
        for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ( key == null ) {
                throw new ValueTypeException( "Illegal null map key value" );
            }
            else if ( key.getClass() != String.class ) {
                throw new ValueTypeException( "Illegal map key value " + key
                                            + " (" + objClassName( key ) + ")"
                                            + " should be a String" );
            }
            else {
                checkObject( value );
            }
        }
    }

    /**
     * Convenience method to return a human-readable version of the classname
     * of an object.
     *
     * @param  obj  object
     * @return  classname or similar
     */
    private static String objClassName( Object obj ) {
        return obj == null ? "null"
                           : className( obj.getClass() );
    }

    /**
     * Convenience method to return a human-readable version of the name of
     * a class.
     *
     * @param  clazz  class
     * @return  classname or similar
     */
    private static String className( Class clazz ) {
        return clazz.getName().replaceFirst( "^java\\.lang\\.", "" );
    }
}
