package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.StarTable;

/**
 * Filter which sets a parameter on the table.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2006
 */
public class SetParameterFilter extends BasicFilter {

    public SetParameterFilter() {
        super( "setparam",
               "[-type byte|short|int|long|float|double|boolean|string]\n" +
               "[-desc <descrip>] [-unit <units>] [-ucd <ucd>]\n" +
               "<pname> <pval>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Sets a named parameter in the table to a given value.",
            "The parameter named <code>&lt;pname&gt;</code> is set",
            "to the value <code>&lt;pval&gt;</code>.",
            "By default the type of the parameter is determined automatically",
            "(if it looks like an integer it's an integer etc)", 
            "but this can be overridden using the <code>-type</code> flag.",
            "The parameter description may be set using the",
            "<code>-desc</code> flag.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String type = null;
        String pname = null;
        String pval = null;
        String pdesc = null;
        String pucd = null;
        String punits = null;
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            if ( "-type".equals( arg ) && type == null && argIt.hasNext() ) {
                argIt.remove();
                type = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-desc" ) && pdesc == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                pdesc = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && pucd == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                pucd = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-unit" ) && punits == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                punits = (String) argIt.next();
                argIt.remove();
            }
            else if ( pname == null ) {
                pname = arg;
                argIt.remove();
            }
            else if ( pval == null ) {
                pval = arg;
                argIt.remove();
            }
        }
        if ( pname == null ) {
            throw new ArgException( "No parameter name specified" );
        }
        else if ( pval == null ) {
            throw new ArgException( "No parameter value specified" );
        }
        final String name = pname;
        final String value = pval;
        final String descrip = pdesc;
        final String ucd = pucd;
        final String units = punits;
        final Class clazz = type == null ? null : getClass( type );
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                base.setParameter( createDescribedValue( name, value, descrip,
                                                         ucd, units, clazz ) );
                return base;
            }
        };
    }

    /**
     * Turns a string such as "int" or "float" into a (non-primitive) class.  
     *
     * @param  type  data type
     * @return  class matching <code>type</code>
     */
    private static Class getClass( String type ) throws ArgException {
        type = type.toLowerCase().trim();
        if ( "byte".equals( type ) ) {
            return Byte.class;
        }
        else if ( "short".equals( type ) ) {
            return Short.class;
        }
        else if ( "int".equals( type ) ) {
            return Integer.class;
        }
        else if ( "long".equals( type ) ) {
            return Long.class;
        }
        else if ( "float".equals( type ) ) {
            return Float.class;
        }
        else if ( "double".equals( type ) ) {
            return Double.class;
        }
        else if ( "string".equals( type ) ) {
            return String.class;
        }
        else {
            throw new ArgException( "Unknown parameter type \"" + type + "\"" );
        }
    }

    /**
     * Turns a (name, value, type) triplet into a DescribedValue.
     *
     * @param   name  parameter name
     * @param   sval  string representation of parameter value
     * @param   descrip  parameter description, or null
     * @param   ucd   parameter UCD, or null
     * @param   units  parameter units, or null
     * @param   clazz  class of parameter type, or null for automatic
     *          determination
     */
    private static DescribedValue createDescribedValue( String name,
                                                        String sval,
                                                        String descrip,
                                                        String ucd,
                                                        String units,
                                                        Class clazz )
            throws IOException {
        Object value = null;

        /* If a class has been set, try to force the value to be of the
         * correct type, throwing an exception if it doesn't work. */
        if ( clazz != null ) {
            try {
                if ( clazz == Boolean.class ) {
                    value = Boolean.valueOf( sval );
                }
                else if ( clazz == Byte.class ) {
                    value = Byte.valueOf( sval );
                }
                else if ( clazz == Short.class ) {
                    value = Short.valueOf( sval );
                }
                else if ( clazz == Integer.class ) {
                    value = Integer.valueOf( sval );
                }
                else if ( clazz == Long.class ) {
                    value = Long.valueOf( sval );
                }
                else if ( clazz == Float.class ) {
                    value = Float.valueOf( sval );
                }
                else if ( clazz == Double.class ) {
                    value = Double.valueOf( sval );
                }
                else if ( clazz == String.class ) {
                    value = sval;
                }
                else {
                    assert false;
                    value = sval;
                    clazz = String.class;
                }
            }
            catch ( NumberFormatException e ) {
                throw (IOException)
                      new IOException( "Value \"" + sval + "\" is not of type "
                                      + clazz.getName() )
                     .initCause( e );
            }
        }

        /* Otherwise, just see what we can get to work. */
        else {
            if ( clazz == null ) {
                try {
                    value = Integer.valueOf( sval );
                    clazz = Integer.class;
                }
                catch ( NumberFormatException e ) {
                }
            }
            if ( clazz == null ) {
                try {
                    value = Double.valueOf( sval );
                    clazz = Double.class;
                }
                catch ( NumberFormatException e ) {
                }
            }
            if ( clazz == null ) {
                if ( sval.equalsIgnoreCase( "true" ) ) {
                    value = Boolean.TRUE;
                    clazz = Boolean.class;
                }
                else if ( sval.equalsIgnoreCase( "false" ) ) {
                    value = Boolean.FALSE;
                    clazz = Boolean.class;
                }
            }
            if ( clazz == null ) {
                value = sval;
                clazz = String.class;
            }
        }
        assert clazz != null;

        /* Construct and return the described value. */
        DefaultValueInfo info = new DefaultValueInfo( name, clazz, descrip );
        if ( ucd != null && ucd.trim().length() > 0 ) {
            info.setUCD( ucd );
        }
        if ( units != null && units.trim().length() > 0 ) {
            info.setUnitString( units );
        }
        return new DescribedValue( info, value );
    }
}
