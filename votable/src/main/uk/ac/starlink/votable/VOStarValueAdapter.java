package uk.ac.starlink.votable;

import java.lang.reflect.Array;

/**
 * Class which does the necessary to change values as returned by
 * VOTable Field decoder output into objects which a StarTable wants to return.
 * Decoder objects return primitive arrays, possibly containing just
 * one element.  StarTable wants a primitive wrapper type if it is 
 * a scalar, using java <tt>null</tt> value if the element is blank
 * (matches the Field's null value).  If the element is actually an
 * array with more than one element, we still return a java array.
 * The adapter should also be able to report the class of which all
 * its returned values will be instances of (unless they are <tt>null</tt>).
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class VOStarValueAdapter {

    /**
     * Returns the class which all objects returned by the {@link #adapt}
     * method will be instances of.
     */
    public abstract Class getContentClass();

    /**
     * Turns an object returned by a VOTable Decoder decode* method
     * into an object suitable for returning from a StarTable method.
     *
     * @param  value  the object returned from a decoder
     * @return  the object to return from StarTable
     */
    public abstract Object adapt( Object value );

    /**
     * Returns an adapter suitable for values from a given field in
     * a VOTable.
     *
     * @param  field  the field for which the adapter is required
     * @return  the adapter
     */
    public static VOStarValueAdapter makeAdapter( Field field ) {

        /* Get information about this field. */
        long[] arraysize = field.getArraysize();
        String datatype = field.getDatatype();
        final Decoder decoder = field.getDecoder();
        Class pclass = decoder.getBaseClass();

        /* Check if it is one of the boolean-type ones. */
        boolean isCharBoolean = 
            ( decoder instanceof BooleanDecoder ||
              decoder instanceof BitDecoder ) && pclass == char.class;

        /* See if it is variable in size. */
        boolean isVariable = arraysize.length > 0 
                          && arraysize[ arraysize.length - 1 ] < 0;

        /* Get the number of elements if it is fixed size. */
        long size = 1L;
        if ( ! isVariable ) {
            for ( int i = 0; i < arraysize.length; i++ ) {
                size *= arraysize[ i ];
            }
        }

        /* See if we have a scalar. */
        boolean isScalar = ( ( ! isVariable ) &&
                             ( ! datatype.endsWith( "Complex" ) ) &&
                             ( size == 1L ) )
                        || ( arraysize.length == 1 && pclass == String.class );

        /* Deal with scalar datatypes. */
        if ( isScalar ) {
            if ( isCharBoolean ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Boolean.class;
                    }
                    public Object adapt( Object value ) {
                        switch ( ((Character) value).charValue() ) {
                            case 'T':
                                return Boolean.TRUE;
                            case 'F':
                                return Boolean.FALSE;
                            default:
                                return null;
                        }
                    }
                };
            }
            else if ( pclass == char.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Character.class;
                    }
                    public Object adapt( Object value ) {
                        char[] vals = (char[]) value;
                        return decoder.isNull( vals, 0 )
                                  ? null : new Character( vals[ 0 ] );
                    }
                };
            }
            else if ( pclass == byte.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Byte.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == short.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Short.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == int.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Integer.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == long.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Long.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == float.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Float.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == double.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return Double.class;
                    }
                    public Object adapt( Object value ) {
                        return value;
                    }
                };
            }
            else if ( pclass == String.class ) {
                return new VOStarValueAdapter() {
                    public Class getContentClass() {
                        return String.class;
                    }
                    public Object adapt( Object value ) {
                        return (String) value;
                    }
                };
            }
        }

        /* Deal with anything not caught by the above - should be just
         * vector-type datatypes. */
        final Class cclass = Array.newInstance( pclass, 0 ).getClass();
        return new VOStarValueAdapter() {
            public Class getContentClass() {
                return cclass;
            }
            public Object adapt( Object value ) {
                return value;
            }
        };
    }
}
