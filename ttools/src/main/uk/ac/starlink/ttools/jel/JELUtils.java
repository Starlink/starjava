package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import gnu.jel.Parser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.func.Arithmetic;
import uk.ac.starlink.ttools.func.Conversions;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.func.Coverage;
import uk.ac.starlink.ttools.func.Distances;
import uk.ac.starlink.ttools.func.Fluxes;
import uk.ac.starlink.ttools.func.Formats;
import uk.ac.starlink.ttools.func.Gaia;
import uk.ac.starlink.ttools.func.KCorrections;
import uk.ac.starlink.ttools.func.Lists;
import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.func.Randoms;
import uk.ac.starlink.ttools.func.Shapes;
import uk.ac.starlink.ttools.func.Sky;
import uk.ac.starlink.ttools.func.Strings;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.func.TrigDegrees;
import uk.ac.starlink.ttools.func.URLs;
import uk.ac.starlink.ttools.func.VO;
import uk.ac.starlink.util.Loader;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class JELUtils {

    private static List<Class<?>> staticClasses_;
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
     *           be used for expression evaluation; may be null if
     *           there are no references to table-related expressions
     * @return   a JEL library
     */
    public static Library getLibrary( JELRowReader reader ) {
        Class<?>[] staticLib = getStaticClasses().toArray( new Class<?>[ 0 ] );
        Class<?>[] dynamicLib = reader == null
                           ? new Class<?>[ 0 ]
                           : new Class<?>[] { reader.getClass() };
        Class<?>[] dotClasses = new Class<?>[ 0 ];
        DVMap resolver = reader;
        HashMap<String,Class<?>> cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

    /**
     * Returns a row reader that can be used for expression evaluation,
     * optionally in the context of the non-data parts of a given context
     * table.  If the table is non-null, things like its parameters and
     * row counts are available for reference.  If the table is null,
     * those things won't be available.  In any case, references to table
     * columns will not be recognised.
     *
     * @param  table  context table, or null
     * @return  row reader
     */
    public static JELRowReader createDatalessRowReader( StarTable table ) {
        if ( table == null ) {
            return new TablelessJELRowReader();
        }
        else {
            return new DummyJELRowReader( new WrapperStarTable( table ) {
                public ColumnInfo getColumnInfo( int icol ) {
                    ColumnInfo baseInfo = super.getColumnInfo( icol );
                    return new ColumnInfo( "", baseInfo.getContentClass(),
                                           null );
                }
                public Object[] getRow( long irow ) {
                    throw new UnsupportedOperationException();
                }
                public Object getCell( long irow, int icol ) {
                    throw new UnsupportedOperationException();
                }
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
                public RowAccess getRowAccess() {
                    throw new UnsupportedOperationException();
                }
                public RowSplittable getRowSplittable() {
                    throw new UnsupportedOperationException();
                }
            } );
        }
    }

    /**
     * Returns the list of classes whose static methods will be mapped
     * into the JEL evaluation namespace.  This may be modified.
     *
     * @return   list of classes with static methods
     */
    public static List<Class<?>> getStaticClasses() {
        if ( staticClasses_ == null ) {

            /* Basic classes always present. */
            List<Class<?>> classList =
                    new ArrayList<Class<?>>( Arrays.asList( new Class<?>[] {
                Arithmetic.class,
                uk.ac.starlink.ttools.func.Arrays.class,
                Conversions.class,
                CoordsDegrees.class,
                CoordsRadians.class,
                Coverage.class,
                Distances.class,
                Fluxes.class,
                Formats.class,
                Gaia.class,
                KCorrections.class,
                Lists.class,
                Maths.class,
                Randoms.class,
                Shapes.class,
                Sky.class,
                Strings.class,
                Tilings.class,
                Times.class,
                TrigDegrees.class,
                URLs.class,
                VO.class,
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
                            Class<?> clazz = Class.forName( className );
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
     * @throws  CompilationException  if <tt>expr</tt> cannot be compiled
     */
    public static Class<?> getExpressionType( Library lib, StarTable table,
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
                                            String expr, Class<?> clazz )
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
                                              String expr, Class<?> clazz )
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
     * Compiles an expression in the context of a table reader to give
     * a JELQuantity.  This does the same job as the <code>compile</code>
     * methods, but it provides additional metadata if it can be retrieved
     * from the table context.
     *
     * @param  lib   JEL library
     * @param  jelRdr   context table reader
     * @param  expr  expression string
     * @param  clazz   required return type of compiled expression,
     *                 or null if no requirement
     * @return  compiled quantity
     */
    public static JELQuantity compileQuantity( Library lib,
                                               StarTableJELRowReader jelRdr,
                                               final String expr,
                                               Class<?> clazz )
            throws CompilationException {
        StarTable table = jelRdr.getTable();
        final String calcExpr = tweakExpression( table, expr );
        final CompiledExpression compEx =
            clazz == null ? compile( lib, table, calcExpr )
                          : compile( lib, table, calcExpr, clazz );
        int icol = jelRdr.getColumnIndex( calcExpr );
        final ValueInfo info;
        if ( icol >= 0 ) {
            info = table.getColumnInfo( icol );
        }
        else {
            String name = expr.replaceAll( "\\s", "" )
                              .replaceAll( "[^A-Za-z0-9]+", "_" );
            Class<?> exprClazz = getWrapperType( compEx.getTypeC() );
            info = new DefaultValueInfo( name, exprClazz );
            if ( ! name.trim().equals( expr.trim() ) ) {
                ((DefaultValueInfo) info).setDescription( expr );
            }
        }
        return new JELQuantity() {
            public String getExpression() {
                return expr;
            }
            public CompiledExpression getCompiledExpression() {
                return compEx;
            }
            public ValueInfo getValueInfo() {
                return info;
            }
        };
    }

    /**
     * Returns a function that can compile a fixed expression from a Library.
     * This method does a test compilation before it returns,
     * so that if there's something wrong with the expression this method
     * will throw a CompilationException, but invocations of the returned
     * Function on Libraries compatible with the supplied <code>table</code>
     * ought not to.  The returned function therefore does not need to
     * declare throwing a CompilerException.  If for some reason the
     * deferred compilations do fail, a RuntimeException is returned.
     *
     * @param  table  table from which libraries will be derived
     * @param  expr   expression to compile
     * @param  clazz  required result type of expression, or null for automatic
     */
    public static Function<Library,CompiledExpression>
            compiler( StarTable table, String expr, Class<?> clazz )
            throws CompilationException {
        Library dummyLib = getLibrary( new DummyJELRowReader( table ) );
        compile( dummyLib, table, expr, clazz );
        return lib -> {
            try {
                return compile( lib, table, expr, clazz );
            }
            catch ( CompilationException e ) {
                throw new RuntimeException( "Unexpected compilation error; "
                                          + "it worked last time", e );
            }
        };
    }

    /**
     * Compiles a set of expressions relating to a table.
     * <p>Any CompilationExceptions are rethrown as IOExceptions;
     * this method should therefore generally be used only
     * if the expressions are expected to be free from errors
     * (have been compiled before).
     *
     * @param  reader  table reader
     * @param  exprs   strings giving JEL expressions to be compiled
     * @return  array with one compiled expression for each input string
     * @throws   IOException  in case of any CompilationException
     */
    public static CompiledExpression[]
                  compileExpressions( StarTableJELRowReader reader,
                                      String[] exprs )
            throws IOException {
        StarTable table = reader.getTable();
        int nexpr = exprs.length;
        Library lib = JELUtils.getLibrary( reader );
        CompiledExpression[] compexs = new CompiledExpression[ nexpr ];
        for ( int icol = 0; icol < nexpr; icol++ ) {
            String expr = exprs[ icol ];
            try {
                compexs[ icol ] = JELUtils.compile( lib, table, expr );
            }
            catch ( CompilationException e ) {
                throw (IOException) new IOException( "Bad expression: " + expr )
                                   .initCause( e );
            }
        }
        return compexs;
    }

    /**
     * Utility method to convert a CompilationException into an IOException.
     *
     * @param   e   compilation exception
     * @param  expr  expression for which compilation failed,
     *               to be reported in error message
     */
    public static IOException toIOException( CompilationException e,
                                             String expr ) {
        StringBuffer sbuf = new StringBuffer()
            .append( "Bad expression \"" )
            .append( expr )
            .append( "\"" );
        String msg = e.getMessage();
        if ( msg != null && msg.trim().length() > 0 ) {
            sbuf.append( " (" )
                .append( msg )
                .append( ")" );
        }
        return (IOException) new IOException( sbuf.toString() ).initCause( e );
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
    public static Class<?> getWrapperType( Class<?> clazz ) {
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
