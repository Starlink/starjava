package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import gnu.jel.Library;
import gnu.jel.DVMap;
import java.util.Date;
import java.util.Hashtable;
import uk.ac.starlink.topcat.func.Activation;
import uk.ac.starlink.topcat.func.Angles;
import uk.ac.starlink.topcat.func.Miscellaneous;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 */
public class JELUtils {

    private static List staticClasses;
    public static final String AUX_CLASSES_PROPERTY = "gnu.jel.static.classes";
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Returns a JEL Library suitable for expression evaluation.
     *
     * @param    rowReader  object which can read rows from the table to
     *           be used for expression evaluation
     * @return   a library
     */
    public static Library getLibrary( JELRowReader rowReader ) {
        Class[] staticLib = 
            (Class[]) getStaticClasses().toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[] { String.class, Date.class };
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

    /**
     * Returns a JEL library suitable for code activation.
     * This includes access to methods which do stuff (e.g. write to 
     * System.out, pop up viewers) rather than just ones which 
     * calculate things.
     *
     * @param  rowReader  object which can read rows from the table to
     *         be used for expression execution
     * @return a library
     */
    static Library getActivationLibrary( JELRowReader rowReader ) {
        List statix = new ArrayList( getStaticClasses() );
        statix.add( Activation.class );
        Class[] staticLib = (Class[]) statix.toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[] { String.class, Date.class };
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

    /**
     * Returns the list of classes whose static methods will be mapped
     * into the JEL evaluation namespace.  This may be modified.
     *
     * @return   list of classes with static methods
     */
    public static List getStaticClasses() {
        if ( staticClasses == null ) {

            /* Basic classes always present. */
            List classList = new ArrayList( Arrays.asList( new Class[] {
                Math.class, Integer.class, Float.class, Double.class,
                Angles.class, Miscellaneous.class,
            } ) );
            try {

                /* Add classes specified by a system property. */
                String auxClasses = System.getProperty( AUX_CLASSES_PROPERTY );
                if ( auxClasses != null && auxClasses.trim().length() > 0 ) {
                    String[] cs = auxClasses.split( ":" );
                    for ( int i = 0; i < cs.length; i++ ) {
                        String className = cs[ i ].trim();
                        try {
                            classList.add( Class.forName( className,
                                                          true,
                            Thread.currentThread().getContextClassLoader()));
                        }
                        catch ( ClassNotFoundException e ) {
                            logger.warning( "Class not found: " + className );
                        }
                    }
                }
            }
            catch ( SecurityException e ) {
                logger.info( "Security manager prevents loading "
                           + "auxiliary JEL classes" );
            }

            /* Combine to produce the final list. */
            staticClasses = classList;
        }
        return staticClasses;
    }

     /**
     * Turns a primitive class into the corresponding wrapper class.
     *
     * @param   prim  primitive class
     * @return  the corresponding non-primitive wrapper class
     */
    public static Class wrapPrimitiveClass( Class prim ) {
        if ( prim == boolean.class ) {
            return Boolean.class;
        }
        else if ( prim == char.class ) {
            return Character.class;
        }
        else if ( prim == byte.class ) {
            return Byte.class;
        }
        else if ( prim == short.class ) {
            return Short.class;
        }
        else if ( prim == int.class ) {
            return Integer.class;
        }
        else if ( prim == long.class ) {
            return Long.class;
        }
        else if ( prim == float.class ) {
            return Float.class;
        }
        else if ( prim == double.class ) {
            return Double.class;
        }
        else {
            throw new IllegalArgumentException( prim + " is not primitive" );
        }
    }

}
