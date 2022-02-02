package uk.ac.starlink.tfcat;

/**
 * Characterises the type of data held by a field.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public abstract class Datatype<T> {

    private final String name_;
    private final Class<T> clazz_;

    /** Integer type. */
    public static final Datatype<Long> INT;

    /** Floating point type. */
    public static final Datatype<Double> FLOAT;

    /** Boolean type. */
    public static final Datatype<Boolean> BOOL;

    /** String type. */
    public static final Datatype<String> STRING;

    private static final Datatype<?>[] ALL_TYPES = {
        INT = new Datatype<Long>( "int", Long.class ) {
            public boolean isType( String txt ) {
                return txt.matches( "[+-]?[0-9]+" );
            }
            public Long decode( String txt ) {
                return Long.valueOf( txt );
            }
        },
        FLOAT = new Datatype<Double>( "float", Double.class ) {
            public boolean isType( String txt ) {
                return txt.matches( "-?[0-9]+([.][0-9]+)?([eE][+-]?[0-9]+)?" );
            }
            public Double decode( String txt ) {
                return Double.parseDouble( txt );
            }
        },
        BOOL = new Datatype<Boolean>( "bool", Boolean.class ) {
            public boolean isType( String txt ) {
                return "true".equals( txt )
                    || "false".equals( txt );
            }
            public Boolean decode( String txt ) {
                if ( "true".equals( txt ) ) {
                    return Boolean.TRUE;
                }
                else if ( "false".equals( txt ) ) {
                    return Boolean.FALSE;
                }
                else {
                    return null;
                }
            }
        },
        STRING = new Datatype<String>( "str", String.class ) {
            public boolean isType( String txt ) {
                return true;
            }
            public String decode( String txt ) { 
                return txt;
            }
        },
    };

    /**
     * Constructor.
     *
     * @param   type name
     * @return   type class
     */
    private Datatype( String name, Class<T> clazz ) {
        name_ = name;
        clazz_ = clazz;
    }

    /**
     * Returns the name of this datatype, as used in the datatype member
     * of a field.
     *
     * @return   type name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the object class represented by this datatype.
     *
     * @return  class
     */
    public Class<T> getTypeClass() {
        return clazz_;
    }

    /**
     * Decodes a string value that has this type.
     *
     * @param  txt   JSON string representation of a typed value
     * @return   typed value
     * @throws  RuntimeException  if the value cannot be decoded
     */
    public abstract T decode( String txt );

    /**
     * Indicates whether a string value appears to have this type.
     * If this returns true, then {@link #decode} should return a value
     * without error.
     *
     * @param  txt   JSON string representation of a typed value
     * @return   true if the string is of the right type
     */
    public abstract boolean isType( String txt );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the datatype instance for a given type name.
     *
     * @param  name  datatype name
     * @return  Datatype instance, or null if none matches name
     */
    public static Datatype<?> forName( String name ) {
        for ( Datatype<?> t : ALL_TYPES ) {
            if ( t.name_.equals( name ) ) {
                return t;
            }
        }
        return null;
    }
}
