package uk.ac.starlink.topcat;

import gnu.jel.CompiledExpression;
import gnu.jel.CompilationException;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import gnu.jel.Parser;

/**
 * Activator which evaluates a given JEL expression per row on activation.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Aug 2004
 */
public class JELActivator implements Activator {

    private final TopcatModel tcModel_;
    private final String expression_;
    private final JELRowReader rowReader_;
    private final Object[] args_;
    private final CompiledExpression compEx_;
    private final Class resultType;

    /**
     * Constructs a new activator given an algebraic expression.
     *
     * @param  tcModel  expression evaluation context
     * @param  expression  string for JEL evaluation at each row
     * @throws  CompilationException  if <tt>expression</tt> is bad
     */
    public JELActivator( TopcatModel tcModel, String expression ) 
            throws CompilationException {
        tcModel_ = tcModel;
        expression_ = expression;

        /* Get a RowReader. */
        rowReader_ = new JELRowReader( tcModel_.getDataModel(), 
                                       tcModel_.getSubsets() );

        /* Compile the expression. */
        Library lib = JELUtils.getActivationLibrary( rowReader_ );
        compEx_ = Evaluator.compile( expression, lib, null );
        args_ = new Object[] { rowReader_ };

        /* Determine the result type. */
        Class clazz = new Parser( expression, lib ).parse( null ).resType;
        if ( clazz.isPrimitive() ) {
            clazz = JELUtils.wrapPrimitiveClass( clazz );
        }
        resultType = clazz;
    }

    public String activateRow( long lrow ) {
        try {
            Object result = rowReader_.evaluateAtRow( compEx_, args_, lrow );
            if ( result != null && ! ( result instanceof Boolean ) ) {
                return result.toString();
            }
            else {
                return null;
            }
        }
        catch ( Throwable th ) {
            String msg = th.getMessage();
            if ( msg == null || msg.trim().length() == 0 ) {
                msg = th.getClass().getName();
            }
            return "Error: " + msg;
        }
    }

    public String toString() {
        return expression_;
    }
}
