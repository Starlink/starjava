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
import uk.ac.starlink.topcat.func.Arithmetic;
import uk.ac.starlink.topcat.func.Conversions;
import uk.ac.starlink.topcat.func.Coords;
import uk.ac.starlink.topcat.func.Display;
import uk.ac.starlink.topcat.func.Maths;
import uk.ac.starlink.topcat.func.Output;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 */
public class JELUtils {

    private static List generalStaticClasses;
    private static List activationStaticClasses;
    public static final String AUX_CLASSES_PROPERTY = "gnu.jel.static.classes";
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Returns a JEL Library suitable for expression evaluation.
     *
     * @param    rowReader  object which can read rows from the table to
     *           be used for expression evaluation
     * @param    activation  true iff the result is to include classes 
     *           used only for activation (e.g. write to System.out, 
     *           pop up viewers) as well as classes with methods for
     *           calculations
     * @return   a JEL library
     */
    public static Library getLibrary( JELRowReader rowReader,
                                      boolean activation ) {
        List statix = new ArrayList( getGeneralStaticClasses() );
        if ( activation ) {
            statix.addAll( getActivationStaticClasses() );
        }
        Class[] staticLib = (Class[]) statix.toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[ 0 ];
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
    public static List getGeneralStaticClasses() {
        if ( generalStaticClasses == null ) {

            /* Basic classes always present. */
            List classList = new ArrayList( Arrays.asList( new Class[] {
                Arithmetic.class,
                Conversions.class,
                Coords.class,
                Maths.class, 
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
            generalStaticClasses = classList;
        }
        return generalStaticClasses;
    }

    /**
     * Returns the list of classes whose static methods will be mapped
     * into the JEL evaluation namespace for activation purposes only.
     * This may be modified.
     *
     * @return  list of activation classes with static methods
     */
    public static List getActivationStaticClasses() {
        if ( activationStaticClasses == null ) {
            List classList = new ArrayList( Arrays.asList( new Class[] {
                Display.class,
                Output.class, 
            } ) );
            activationStaticClasses = classList;
        }
        return activationStaticClasses;
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
