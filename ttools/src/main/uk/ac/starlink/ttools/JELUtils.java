package uk.ac.starlink.ttools;

import gnu.jel.CompilationException;
import gnu.jel.DVMap;
import gnu.jel.Library;
import gnu.jel.Parser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.func.Arithmetic;
import uk.ac.starlink.ttools.func.Conversions;
import uk.ac.starlink.ttools.func.Coords;
import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.func.Strings;
import uk.ac.starlink.util.Loader;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class JELUtils {

    private static List staticClasses_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.ttools" );

    /** 
     * System property name for adding colon-separated list of 
     * additional classnames containing static methods.
     */
    public static final String CLASSES_PROPERTY = "jel.classes";

    /** 
     * Returns a JEL Library suitable for expression evaluation.
     * 
     * @param    rowReader  object which can read rows from the table to
     *           be used for expression evaluation
     * @return   a JEL library
     */
    public static Library getLibrary( JELRowReader reader ) {
        Class[] staticLib =
            (Class[]) getStaticClasses().toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { reader.getClass() };
        Class[] dotClasses = new Class[ 0 ];
        DVMap resolver = reader;
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
        if ( staticClasses_ == null ) {

            /* Basic classes always present. */
            List classList = new ArrayList( Arrays.asList( new Class[] {
                Arithmetic.class,
                Conversions.class,
                Coords.class,
                Maths.class,
                Strings.class,
            } ) );

            /* Add classes specified by a system property. */
            Loader.loadProperties();
            try {
                String auxClasses =
                    System.getProperty( CLASSES_PROPERTY );
                if ( auxClasses != null && auxClasses.trim().length() > 0 ) {
                    String[] cs = auxClasses.split( ":" );
                    for ( int i = 0; i < cs.length; i++ ) {
                        String className = cs[ i ].trim();
                        try {
                            Class clazz =
                                JELRowReader.class.forName( className );
                            if ( ! classList.contains( clazz ) ) {
                                classList.add( clazz );
                            }
                        }
                        catch ( ClassNotFoundException e ) {
                            logger_.warning( "Class not found: " + className );
                        }
                    }
                }
            }

            catch ( SecurityException e ) {
                logger_.info( "Security manager prevents loading "
                            + "auxiliary JEL classes" );
            }

            /* Produce the final list. */
            staticClasses_ = classList;
        }
        return staticClasses_;
    }

    /**
     * Gives the return type of an expression.
     * This also has the effect of testing that an expression is legal.
     *
     * @param   lib   JEL library
     * @param   expr  string representation of the expression
     * @return  class which evaluation of <tt>expr</tt> using <tt>lib</tt>
     *          will return
     * @throws  CompilationExpression  if <tt>expr</tt> cannot be compiled
     */
    public static Class getExpressionType( Library lib, String expr )
             throws CompilationException {
         return new Parser( expr, lib ).parse( null ).resType;
    }

    /**
     * Checks that an expression is legal and returns a particular class.
     *
     * @param  lib    JEL library
     * @param  expr   string representation of the expression
     * @param  clazz  return type required from <tt>expr</tt>
     * @throws  CompilationException  if <tt>expr</tt> cannot be compiled
     *          or will return a type other than <tt>clazz</tt> 
     *          (or one of its subtypes)
     */
    public static void checkExpressionType( Library lib, String expr,
                                            Class clazz )
             throws CompilationException {
         new Parser( expr, lib ).parse( clazz );
    }

    /**
     * Returns a non-primitive version of a given class.
     * If <tt>clazz</tt> is a non-primitive type, it will be returned,
     * otherwise the wrapper class corresponding to the primitive type
     * of <tt>clazz</tt> will be returned 
     * (e.g. <tt>Integer</tt> for <tt>int</tt>).
     *
     * @param   clazz   input class
     * @return  non-primitive class matching <tt>clazz</tt>
     */
    public static Class getWrapperType( Class clazz ) {
        if ( clazz.equals( boolean.class ) ) {
            return Boolean.class;
        }
        else if ( clazz.equals( char.class ) ) {
            return Character.class;
        }
        else if ( clazz.equals( byte.class ) ) {
            return Byte.class;
        }
        else if ( clazz.equals( short.class ) ) {
            return Short.class;
        }
        else if ( clazz.equals( int.class ) ) {
            return Integer.class;
        }
        else if ( clazz.equals( long.class ) ) {
            return Long.class;
        }
        else if ( clazz.equals( float.class ) ) {
            return Float.class;
        }
        else if ( clazz.equals( double.class ) ) {
            return Double.class;
        }
        else {
            return clazz;
        }
    }
}
