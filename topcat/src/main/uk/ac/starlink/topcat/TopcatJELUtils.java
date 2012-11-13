package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import gnu.jel.CompilationException;
import gnu.jel.Library;
import gnu.jel.DVMap;
import java.util.Date;
import java.util.Hashtable;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Browsers;
import uk.ac.starlink.topcat.func.Image;
import uk.ac.starlink.topcat.func.Mgc;
import uk.ac.starlink.topcat.func.Output;
import uk.ac.starlink.topcat.func.Sdss;
import uk.ac.starlink.topcat.func.Sog;
import uk.ac.starlink.topcat.func.SuperCosmos;
import uk.ac.starlink.topcat.func.TwoQZ;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TopcatJELUtils extends JELUtils {

    private static List activationStaticClasses;
    public static final String ACTIVATION_CLASSES_PROPERTY =
        "jel.classes.activation";
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
        List<Class> statix = new ArrayList<Class>( getStaticClasses() );
        if ( activation ) {
            statix.addAll( getActivationStaticClasses() );
        }
        Class[] staticLib = statix.toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { rowReader.getClass() };
        Class[] dotClasses = new Class[ 0 ];
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        Library lib = new Library( staticLib, dynamicLib, dotClasses,
                                   resolver, cnmap );

        if ( activation ) {
            /* Mark the System.exec methods as dynamic.  This is a bit obscure;
             * its purpose is to make sure that they get evaluated at run time
             * not compilation time even if their arguments are constant.
             * You can't do very useful things with constant arguments, but
             * you might have them while you're experimenting, and it is
             * surprising to see the expression be evaluated when you type
             * the expression in rather than when the activation takes place.
             * For this reason, all the activation methods should probably
             * be so marked, but JEL doesn't make that very easy. */
            try {
                for ( int i = 1; i <= 4; i++ ) {
                    Class[] argClasses = new Class[ i ];
                    Arrays.fill( argClasses, String.class );
                    lib.markStateDependent( "exec", argClasses );
                }
            }
            catch ( CompilationException e ) {
                throw (AssertionError) new AssertionError().initCause( e );
            }
        }
        return lib;
    }

    /**
     * Returns the list of classes whose static methods will be mapped
     * into the JEL evaluation namespace for activation purposes only.
     * This may be modified.
     *
     * @return  list of activation classes with static methods
     */
    public static List<Class> getActivationStaticClasses() {
        if ( activationStaticClasses == null ) {

            /* Assemble the list of classes which we know we have on hand.
             * Be careful though, since we may not have the classes they
             * rely on. */
            List<Class> classList = new ArrayList<Class>();
            classList.add( Output.class );
            classList.add( uk.ac.starlink.topcat.func.System.class );
            classList.add( Image.class );
            classList.add( BasicImageDisplay.class );
            if ( TopcatUtils.canSog() ) { 
                classList.add( Sog.class );
            }
            classList.add( Browsers.class );
            classList.add( Mgc.class );
            classList.add( Sdss.class );
            classList.add( SuperCosmos.class );
            classList.add( TwoQZ.class );

            /* Add classes specified by a system property. */
            try {
                String auxClasses =
                    System.getProperty( ACTIVATION_CLASSES_PROPERTY );
                if ( auxClasses != null && auxClasses.trim().length() > 0 ) {
                    String[] cs = auxClasses.split( ":" );
                    for ( int i = 0; i < cs.length; i++ ) {
                        String className = cs[ i ].trim();
                        Class clazz = classForName( className );
                        if ( clazz != null ) {
                            if ( ! classList.contains( clazz ) ) {
                                classList.add( clazz );
                            }
                        }
                        else {
                            logger.warning( "Class not found: " + className );
                        }
                    }
                }
            }
            catch ( SecurityException e ) {
                logger.info( "Security manager prevents loading " +
                             "auxiliary JEL classes" );
            }

            /* Produce the final list. */
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

    /**
     * Returns the class with the given name, or null if it's not on the
     * path.
     *
     * @param   cname  class name
     * @return  class or null
     */
    public static Class classForName( String cname ) {

        // Hmm - not sure now why I wanted to make sure I got this classloader.
        // I wonder if there is a good reason??
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName( cname, true, loader );
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }
    }

}
