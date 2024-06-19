package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * A column which produces read-only values based on an algebraic
 * expression.  This expression may include other columns in the table
 * referenced by column name, and can use any of the java language 
 * expression constructs as well as the static methods in the
 * <code>java.lang.Math</code> class.  This list could be extended quite 
 * easily if new arithmetic tricks were required.
 * <p>
 * The engine used for expression evaluation is the GNU 
 * Java Expressions Library (JEL).
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://galaxy.fzu.cz/JEL/">JEL</a>
 */
public class SyntheticColumn extends ColumnData {

    private final TopcatModel tcModel_;
    private TopcatJELEvaluator evaluator_;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new synthetic column from an algebraic expression 
     * applied to a table.
     *
     * @param  tcModel  context for JEL expression evaluation
     * @param  cinfo   metadata for the new column;
     *                 note this object may be modified as required by
     *                 the supplied expression
     * @param  expression  algebraic expression for the value of this
     *         column
     * @param  resultType  a Class for the result, presumably one of the
     *         primitive wrapper types or String.class.  If <code>null</code>
     *         a suitable class is chosen automatically.
     */
    public SyntheticColumn( TopcatModel tcModel, ColumnInfo cinfo,
                            String expression, Class<?> resultType )
            throws CompilationException {
        super( cinfo );
        tcModel_ = tcModel;
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
     *         primitive wrapper types or String.class.  If <code>null</code>
     *         a suitable class is chosen automatically.
     */
    public void setExpression( String expression, Class<?> resultType )
            throws CompilationException {

        /* Compile the expression. */
        evaluator_ = TopcatJELEvaluator
                    .createEvaluator( tcModel_, expression, false, resultType );
        Class<?> actualType = evaluator_.getResultType();

        /* Configure the column data type correctly for this expression. */
        ColumnInfo colinfo = getColumnInfo();
        colinfo.setContentClass( actualType );

        /* Store the value of the expression in the column metadata. */
        ValueInfo exprInfo = TopcatUtils.EXPR_INFO;
        colinfo.setAuxDatum( new DescribedValue( exprInfo, expression ) );
    }

    /**
     * Returns the JEL expression that provides this column's value.
     *
     * @return  expression
     */
    public String getExpression() {
        return evaluator_.getExpression();
    }

    public Object readValue( long lrow ) throws IOException {
        try {
            return evaluator_.evaluateObject( lrow );
        }
        catch ( RuntimeException e ) {
            logger.info( e.toString() );
            return null;
        }
        catch ( Throwable th ) {
            throw (IOException) new IOException( th.getMessage() )
                               .initCause( th );
        }
    }
}
