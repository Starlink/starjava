package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;

/**
 * A <tt>RowSubset</tt> which uses an algebraic expression based on the
 * values of other columns in the same row to decide whether a row
 * is included or not.
 * <p>
 * The engine used for expression evaluation is the GNU 
 * Java Expressions Library (JEL).
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://galaxy.fzu.cz/JEL/">JEL</a>
 */
public class SyntheticRowSubset implements RowSubset {

    private StarTable stable;
    private List subsets;
    private String name;
    private String expression;
    private RandomJELRowReader rowReader;
    private CompiledExpression compEx;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new synthetic subset given a table and an algebraic
     * expression.
     *
     * @param  stable the StarTable which supplies column values which can
     *         appear in the expression
     * @param  subsets  a List of other <tt>RowSubset</tt> objects which
     *         may be referenced by name or number in the expression
     * @param  name  the name to use for the new RowSubset
     * @param  expression   the algebraic expression
     */
    public SyntheticRowSubset( StarTable stable, List subsets, String name, 
                               String expression ) 
            throws CompilationException {
        this.stable = stable;
        this.subsets = subsets;
        this.name = name;
        setExpression( expression );
    }

    public void setExpression( String expression ) throws CompilationException {

        /* Get an up-to-date RowReader (an old one may not be aware of recent
         * changes to the StarTable or subset list). */
        RowSubset[] subsetArray = subsets == null
                                ? new RowSubset[ 0 ]
                                : (RowSubset[]) 
                                  subsets.toArray( new RowSubset[ 0 ] );
        rowReader = new RandomJELRowReader( stable, subsetArray );

        /* Compile the expression. */
        Library lib = TopcatJELUtils.getLibrary( rowReader, false );
        compEx = Evaluator.compile( expression, lib, boolean.class );
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isIncluded( long lrow ) {
        try {
            Boolean result = (Boolean) 
                             rowReader.evaluateAtRow( compEx, lrow );
            return result == null ? false : result.booleanValue();
        }
        catch ( RuntimeException e ) {
            logger.info( e.toString() );
            return false;
        }
        catch ( Throwable th ) {
            logger.warning( th.toString() );
            return false;
        }
    }

    public String toString() {
        return getName();
    }

}
