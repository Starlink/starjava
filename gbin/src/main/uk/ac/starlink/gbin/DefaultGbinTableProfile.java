package uk.ac.starlink.gbin;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of GbinTableProfile.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2015
 */
public class DefaultGbinTableProfile implements GbinTableProfile {

    private final Map<Class<?>,Representation<?>> reprMap_;

    private static final Map<Class<?>,Class<?>> primMap_ =
        Collections.unmodifiableMap( createPrimitiveMap() );

    /**
     * Constructs a profile with default configuration.
     */
    public DefaultGbinTableProfile() {
        this( new Class<?>[] { String.class, },
              new Class<?>[] { Class.class, },
              new Class<?>[] {} );
    }

    /**
     * Constructs a profile with explicit configuration.
     * Any classes not referred to in the supplied lists,
     * apart from primitive classes which are always used,
     * will be treated as elements in the object hierarchy
     * and descended into recursively.
     *
     * @param   simpleClasses  list of scalar Object subclasses that
     *                         are suitable contents of a column as they stand
     * @param   stringClasses  list of scalar Object subclasses that
     *                         can be used as string contents of a column
     *                         by invoking their <code>toString</code> method
     * @param   ignoreClasses  classes which should be ignored completely
     *                         when converting to a table
     */
    public DefaultGbinTableProfile( Class<?>[] simpleClasses,
                                    Class<?>[] stringClasses,
                                    Class<?>[] ignoreClasses ) {
        reprMap_ = new LinkedHashMap<Class<?>,Representation<?>>();
        for ( Class<?> primClazz : primMap_.keySet() ) {
            Class<?> wrapperClazz = primMap_.get( primClazz );
            Class<?> arrayClazz = Array.newInstance( primClazz, 0 ).getClass();
            Representation<?> wrapperRepr =
                createSimpleColumnRepresentation( wrapperClazz );
            reprMap_.put( primClazz, wrapperRepr );
            reprMap_.put( wrapperClazz, wrapperRepr );
            reprMap_.put( arrayClazz,
                          createSimpleColumnRepresentation( arrayClazz ) );
        }
        for ( Class<?> clazz : simpleClasses ) {
            reprMap_.put( clazz, createSimpleColumnRepresentation( clazz ) );
        }
        for ( Class<?> clazz : stringClasses ) {
            reprMap_.put( clazz, createStringColumnRepresentation( clazz ) );
        }
        for ( Class<?> clazz : ignoreClasses ) {
            reprMap_.put( clazz, null );
        }
    }

    public boolean isReadMeta() {
        return true;
    }

    public boolean isTestMagic() {
        return true;
    }

    public boolean isHierarchicalNames() {
        return false;
    }

    public String getNameSeparator() {
        return "_";
    }

    public boolean isSortedMethods() {
        return true;
    }

    public String[] getIgnoreMethodNames() {
        return new String[] {
            "getClass",
            "getField",
            "getStringValue",
            "getGTDescription",
            "getParamMaxValues",
            "getParamMinValues",
            "getParamOutOfRangeValues",
            "getFieldNames",
        };
    }

    public Representation<?> createRepresentation( Class<?> clazz ) {

        /* If we have a specific representation for this type, use it. */
        if ( reprMap_.containsKey( clazz ) ) {
            return reprMap_.get( clazz );
        }

        /* Arrays need treating specially. */
        else if ( clazz.isArray() ) {
            Class<?> scalarClass = getScalarClass( clazz );
            if ( reprMap_.containsKey( scalarClass ) ) {
                Representation<?> scalarRepr = reprMap_.get( scalarClass );

                /* If the scalar type is a structured object
                 * (either appears in the map of known types with
                 * isColumn=false, or implicitly structured by being absent
                 * from that map),
                 * there's not much we can do with it, since you
                 * can't map that structure to a sequence of columns
                 * (you don't know how many there would be).
                 * So we have to discard it. */
                if ( scalarRepr == null || ! scalarRepr.isColumn() ) {
                    return null;
                }

                /* If the scalar type is a column type, assume that it's
                 * OK for an array (or array of arrays, or ...) to be
                 * a column too.  Note however that this code is not
                 * quite right at the moment; it will work for identity
                 * representations, but ones that convert input objects
                 * to a different representation (e.g. toString) will
                 * go wrong here. */
                else {
                    return createSimpleColumnRepresentation( clazz );
                }
            }
            else {
                return null;
            }
        }

        /* Non-array types with no special instructions will be treated
         * as structured objects. */
        else {
            return createIdentityRepresentation( clazz, false );
        }
    }

    /**
     * Returns a scalar, or scalar-like, class based on a class which
     * may be an array, or array of arrays, or ... of some class.
     *
     * @param  clazz  class to test
     * @return   a class obtained by stripping "[]"s from the given type
     *           which either has a known representation or doesn't
     *           have any more "[]"s to strip
     */
    private Class<?> getScalarClass( Class<?> clazz ) {
        if ( reprMap_.containsKey( clazz ) ) {
            return clazz;
        }
        else if ( clazz.isArray() ) {
            return getScalarClass( clazz.getComponentType() );
        }
        else {
            return clazz;
        }
    }

    /**
     * Returns a representation that uses the input values unchanged.
     *
     * @param   clazz  input type
     * @param   isColumn   whether this represents a column or not
     * @return   representation of column or structured object
     */
    public static <T> Representation<T>
            createIdentityRepresentation( final Class<T> clazz,
                                          final boolean isColumn ) {
        return new Representation<T>() {
            public Class<T> getContentClass() {
                return clazz;
            }
            public T representValue( Object value ) {
                return clazz.isInstance( value )
                     ? clazz.cast( value )
                     : null;
            }
            public boolean isColumn() {
                return isColumn;
            }
        };
    }

    /**
     * Returns a column-like representation that uses the
     * input values unchanged.
     *
     * @param   clazz  input type
     * @return   representation of column
     */
    private static <T> Representation<T>
            createSimpleColumnRepresentation( Class<T> clazz ) {
        return createIdentityRepresentation( clazz, true );
    }

    /**
     * Returns a representation that maps its input values to strings
     * using their toString method, and uses the result like a column.
     *
     * @param   clazz  input type
     * @return   representation of column
     */
    public static Representation<String>
            createStringColumnRepresentation( Class<?> clazz ) {
        return new Representation<String>() {
            public Class<String> getContentClass() {
                return String.class;
            }
            public String representValue( Object value ) {
                return value == null ? null : value.toString();
            }
            public boolean isColumn() {
                return true;
            }
        };
    }

    /**
     * Constructs a map of all existing primtive classes to their
     * corresponding wrapper classes.
     *
     * @return  primitive->wrapper class map
     */
    private static Map<Class<?>,Class<?>> createPrimitiveMap() {
        Map<Class<?>,Class<?>> map = new LinkedHashMap<Class<?>,Class<?>>();
        map.put( boolean.class, Boolean.class );
        map.put( char.class, Character.class );
        map.put( byte.class, Byte.class );
        map.put( short.class, Short.class );
        map.put( int.class, Integer.class );
        map.put( long.class, Long.class );
        map.put( float.class, Float.class );
        map.put( double.class, Double.class );
        return map;
    }
}
