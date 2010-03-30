package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

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
public class SyntheticRowSubset extends RowSubset {

    private String expression_;
    private RandomJELRowReader rowReader_;
    private CompiledExpression compEx_;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new synthetic subset given a table and an algebraic
     * expression.
     *
     * @param  name  the name to use for the new RowSubset
     * @param  expression   the algebraic expression
     * @param  rowReader   context for JEL expression evaluation
     */
    public SyntheticRowSubset( String name, String expression,
                               RandomJELRowReader rowReader ) 
            throws CompilationException {
        super( name );
        setExpression( expression, rowReader );
    }

    /**
     * Sets the expression to use for this subset.
     *
     * @param  expression  JEL expression
     * @param  rowReader   context for JEL expression evaluation
     */
    public void setExpression( String expression, RandomJELRowReader rowReader )
            throws CompilationException {
        Library lib = TopcatJELUtils.getLibrary( rowReader, false );
        compEx_ = Evaluator.compile( expression, lib, boolean.class );
        expression_ = expression;
        rowReader_ = rowReader;
    }

    /**
     * Returns the text of the expression used by this subset.
     *
     * @return  expression
     */
    public String getExpression() {
        return expression_;
    }

    public boolean isIncluded( long lrow ) {
        try {
            Boolean result =
                (Boolean) rowReader_.evaluateAtRow( compEx_, lrow );
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
}
