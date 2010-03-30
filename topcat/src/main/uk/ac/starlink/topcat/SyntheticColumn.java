package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import gnu.jel.Parser;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

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

    private CompiledExpression compEx_;
    private RandomJELRowReader rowReader_;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );


    /**
     * Constructs a new synthetic column from an algebraic expression 
     * applied to a table.
     *
     * @param  vinfo  template for the new column
     * @param  expression  algebraic expression for the value of this
     *         column
     * @param  resultType  a Class for the result, presumably one of the
     *         primitive wrapper types or String.class.  If <tt>null</tt>
     *         a suitable class is chosen automatically.
     * @param  rowReader  context for JEL expression evaluation
     */
    public SyntheticColumn( ValueInfo vinfo, String expression,
                            Class resultType, RandomJELRowReader rowReader )
            throws CompilationException {
        super( vinfo );
        setExpression( expression, resultType, rowReader );
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
     * @param  rowReader  context for JEL expression evaluation
     */
    public void setExpression( String expression, Class resultType,
                               RandomJELRowReader rowReader ) 
            throws CompilationException {

        /* Compile the expression. */
        Library lib = TopcatJELUtils.getLibrary( rowReader, false );
        compEx_ = Evaluator.compile( expression, lib, resultType );
        rowReader_ = rowReader;

        /* Work out the type of the compiled expression. */
        Class actualType =
            new Parser( expression, lib ).parse( resultType ).resType;
        if ( actualType.isPrimitive() ) {
            actualType = TopcatJELUtils.wrapPrimitiveClass( actualType );
        }

        /* Configure the column data type correctly for this expression. */
        ColumnInfo colinfo = getColumnInfo();
        colinfo.setContentClass( actualType );

        /* Store the value of the expression in the column metadata. */
        ValueInfo exprInfo = TopcatUtils.EXPR_INFO;
        colinfo.setAuxDatum( new DescribedValue( exprInfo, expression ) );
        
        /* We also want to store the information in the column's
         * Description, since this gives a better chance of it being
         * serialized when the table is saved.  To do this we stash 
         * away the original value of the description in a new value 
         * (BASE_DESCRIPTION) and append the expression to the base
         * description.  We have to do it like this to prevent the
         * expressions getting repeatedly added onto the end if they
         * are changed. */
        ValueInfo basedescInfo = TopcatUtils.BASE_DESCRIPTION_INFO;
        DescribedValue basedescValue = colinfo.getAuxDatum( basedescInfo );
        if ( basedescValue == null ) {
            basedescValue = new DescribedValue( basedescInfo, 
                                                colinfo.getDescription() );
            colinfo.setAuxDatum( basedescValue );
        }
        colinfo.setDescription( basedescValue.getValue() + 
                                " (" + expression + ")" );
    }

    public Object readValue( long lrow ) throws IOException {
        try {
            return rowReader_.evaluateAtRow( compEx_, lrow );
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
