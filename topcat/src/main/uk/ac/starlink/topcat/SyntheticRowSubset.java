package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.util.List;
import java.util.logging.Logger;

/**
 * A <code>RowSubset</code> which uses an algebraic expression based on the
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

    private final TopcatModel tcModel_;
    private TopcatJELEvaluator evaluator_;
    private String expression_;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new synthetic subset given a table and an algebraic
     * expression.
     *
     * @param  name  the name to use for the new RowSubset
     * @param  tcModel   context for JEL expression evaluation
     * @param  expression   the algebraic expression
     */
    @SuppressWarnings("this-escape")
    public SyntheticRowSubset( String name, TopcatModel tcModel,
                               String expression )
            throws CompilationException {
        super( name );
        tcModel_ = tcModel;
        setExpression( expression );
    }

    /**
     * Sets the expression to use for this subset.
     *
     * @param  expression  JEL expression
     */
    public void setExpression( String expression )
            throws CompilationException {
        evaluator_ = TopcatJELEvaluator
                    .createEvaluator( tcModel_, expression, false,
                                      boolean.class );
        expression_ = expression;
    }

    /**
     * Returns the text of the expression used by this subset.
     *
     * @return  expression
     */
    public String getExpression() {
        return expression_;
    }

    @Override
    public String getMaskId() {
        return TopcatJELUtils.getContentIdentifier( tcModel_, expression_ );
    }

    public boolean isIncluded( long lrow ) {
        try {
            return evaluator_.evaluateBoolean( lrow );
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
