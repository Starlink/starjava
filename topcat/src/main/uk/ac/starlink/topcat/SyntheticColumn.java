package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * A column which produces read-only values based on an algebraic
 * expression.  This expression may include other columns in the table
 * referenced by column name, and can use any of the java language 
 * expression constructs as well as the static methods in the
 * <tt>java.lang.Math</tt> class.  This list could be extended quite 
 * easily if new arithmetic tricks were required.
 * <p>
 * The engine used for expression evaluation is the GNU 
 * Java Expressions Library (JEL).
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://galaxy.fzu.cz/JEL/">JEL</a>
 */
public class SyntheticColumn extends ColumnData {

    private StarTable stable;
    private List subsets;
    private String expression;
    private CompiledExpression compEx;
    private JELRowReader rowReader;
    private Object[] args;

    public final static ValueInfo EXPR_INFO = 
        new DefaultValueInfo( "Expression", String.class, 
                              "Algebraic expression for column value" );

    /**
     * Constructs a new synthetic column from an algebraic expression 
     * applied to a table.
     *
     * @param  vinfo  template for the new column
     * @param  stable the StarTable which supplies the other columns 
     *         which can appear in the expression
     * @param  subsets  a List of {@link RowSubset} objects which may be
     *         referenced by name or number in the expression
     * @param  expression  algebraic expression for the value of this
     *         column
     * @param  resultType  a Class for the result, presumably one of the
     *         primitive wrapper types or String.class.  If <tt>null</tt>
     *         a suitable class is chosen automatically.
     */
    public SyntheticColumn( ValueInfo vinfo, StarTable stable, List subsets,
                            String expression, Class resultType )
            throws CompilationException {
        super( vinfo );
        this.stable = stable;
        this.subsets = subsets;
        setExpression( expression, resultType );
    }

    /**
     * Sets the algebraic expression which this column uses to calculate
     * its results.  Column names are used to refer to values of cells
     * in other columns of this table in the same row as that for which
     * the result is being generated.
     *
     * @param  expression  the string giving the algebraic exprssion for
     *         this column's value in terms of other columns
     * @param  resultType  a Class for the result, presumably one of the
     *         primitive wrapper types or String.class.  If <tt>null</tt>
     *         a suitable class is chosen automatically.
     */
    public void setExpression( String expression, Class resultType ) 
            throws CompilationException {

        /* Get an up-to-date RowReader (an old one may not be aware of recent
         * changes to the StarTable or subset list). */
        rowReader = new JELRowReader( stable, subsets );
        args = new Object[] { rowReader };

        /* Compile the expression. */
        String exprsub = expression.replace( '#', '£' );
        compEx = Evaluator.compile( exprsub, getLibrary(), resultType );

        /* Configure the column metadata correctly for this expression. */
        ColumnInfo colinfo = getColumnInfo();
        colinfo.setAuxDatum( new DescribedValue( EXPR_INFO, expression ) );
        colinfo.setContentClass( getReturnClass( compEx ) );
    }

    public Object readValue( long lrow ) throws IOException {
        synchronized ( rowReader ) {
            rowReader.setRow( lrow );
            try {
                return compEx.evaluate( args );
            }
            catch ( NullPointerException e ) {
                return null;
            }
            catch ( Throwable th ) {
                throw (IOException) new IOException( th.getMessage() )
                                   .initCause( th );
            }
        }
    }

    /**
     * Returns a JEL Library suitable for expression evaluation on this
     * column's table.
     *
     * @return   a library
     */
    private Library getLibrary() {
        Class[] staticLib = new Class[] { Math.class, Integer.class, 
                                          Float.class, Double.class };
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[] { String.class, Date.class };
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

    /**
     * Gives the class of results rendered by executing the given 
     * compiled expression.
     *
     * @param  compex  the compiled expression
     * @return  the class of values got from <tt>compex.evaluate</tt> 
     *          invocations
     * @see   gnu.jel.CompiledExpression#getType
     */
    private static Class getReturnClass( CompiledExpression compex ) {
        switch ( compex.getType() ) {
            case 0:  return Boolean.class;
            case 1:  return Byte.class;
            case 2:  return Character.class;
            case 3:  return Short.class;
            case 4:  return Integer.class;
            case 5:  return Long.class;
            case 6:  return Float.class;
            case 7:  return Double.class;
            case 8:  return Object.class;
            case 9:  return null;
        }
        throw new AssertionError( 
            "Unknown gnu.jel.CompiledExpression.getType() return" );
    }
}
