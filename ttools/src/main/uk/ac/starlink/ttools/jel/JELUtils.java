package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import gnu.jel.Parser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.func.Arithmetic;
import uk.ac.starlink.ttools.func.Conversions;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.func.Distances;
import uk.ac.starlink.ttools.func.Fluxes;
import uk.ac.starlink.ttools.func.Formats;
import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.func.Strings;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.func.TrigDegrees;
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
     * @param    reader  object which can read rows from the table to
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
                uk.ac.starlink.ttools.func.Arrays.class,
                Conversions.class,
                CoordsDegrees.class,
                CoordsRadians.class,
                Distances.class,
                Fluxes.class,
                Formats.class,
                Maths.class,
                Strings.class,
                Tilings.class,
                Times.class,
                TrigDegrees.class,
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
     * @param   table  context table
     * @param   expr  string representation of the expression
     * @return  class which evaluation of <tt>expr</tt> using <tt>lib</tt>
     *          will return
     * @throws  CompilationExpression  if <tt>expr</tt> cannot be compiled
     */
    public static Class getExpressionType( Library lib, StarTable table,
                                           String expr )
             throws CompilationException {
        return new Parser( tweakExpression( table, expr ), lib )
              .parse( null ).resType;
    }

    /**
     * Checks that an expression is legal and returns a particular class.
     *
     * @param  lib    JEL library
     * @param  table  context table
     * @param  expr   string representation of the expression
     * @param  clazz  return type required from <tt>expr</tt>
     * @throws  CompilationException  if <tt>expr</tt> cannot be compiled
     *          or will return a type other than <tt>clazz</tt> 
     *          (or one of its subtypes)
     */
    public static void checkExpressionType( Library lib, StarTable table,
                                            String expr, Class clazz )
             throws CompilationException {
        new Parser( tweakExpression( table, expr ), lib ).parse( clazz );
    }

    /**
     * Compiles an expression in the context of a given table with a 
     * required type for the result.
     * Additional to the behaviour of Evaluator.compile this also checks
     * for expressions which exactly match table column names, even if they 
     * are not syntactically legal identifiers.
     *
     * @param  lib   JEL library
     * @param  table   context table
     * @param  expr  expression string
     * @param  clazz  required class of resulting expression
     * @return  compiled expression
     */
    public static CompiledExpression compile( Library lib, StarTable table,
                                              String expr, Class clazz )
            throws CompilationException {
        try {
            return Evaluator.compile( tweakExpression( table, expr ),
                                                       lib, clazz );
        }
        catch ( CompilationException e ) {
            try {
                Evaluator.compile( tweakExpression( table, expr ), lib );
            }
            catch ( CompilationException e2 ) {
                throw e;
            }
            throw new CustomCompilationException( "Expression " + expr
                                                + " has wrong type" 
                                                + " (not " + clazz.getName()
                                                + ")", e );
        }
    }

    /**
     * Compiles an expression in the context of a given table.
     * Additional to the behaviour of Evaluator.compile this also checks
     * for expressions which exactly match table column names, even if they 
     * are not syntactically legal identifiers.
     *
     * @param  lib   JEL library
     * @param  table   context table
     * @param  expr  expression string
     * @return  compiled expression
     */
    public static CompiledExpression compile( Library lib, StarTable table,
                                              String expr )
            throws CompilationException {
        return Evaluator.compile( tweakExpression( table, expr ), lib );
    }

    /**
     * Modifies an expression so that column names are turned into 
     * numeric column references - works even for syntactically invalid 
     * identifiers.
     *
     * @param   table  context table
     * @param   expr  expression, either a legal JEL expression or an
     *          exact match column name
     * @return  expression which is a legal JEL expression
     */
    private static final String tweakExpression( StarTable table,
                                                 String expr ) {
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( table.getColumnInfo( icol ).getName()
                      .equalsIgnoreCase( expr ) ) {
                return JELRowReader.COLUMN_ID_CHAR
                     + Integer.toString( icol + 1 );
            }
        }
        return expr;
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
