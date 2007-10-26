package uk.ac.starlink.ttools.cone;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.JELUtils;
import uk.ac.starlink.ttools.SequentialJELRowReader;

/**
 * QuerySequenceFactory which uses JEL expressions for RA, Dec and SR.
 *
 * @author   Mark Taylor
 * @since    16 Oct 2007
 */
public class JELQuerySequenceFactory implements QuerySequenceFactory {

    private final String raString_;
    private final String decString_;
    private final String srString_;

    /**
     * Constructor.
     * The JEL expressions will be resolved using the column names of the
     * supplied table when the factory method is called.
     *
     * @param  raExpr  JEL expression for right ascension in degrees
     * @param  decExpr JEL expression for declination in degrees
     * @param  srExpr  JEL expression for search radius in degrees
     */
    public JELQuerySequenceFactory( String raExpr, String decExpr,
                                    String srExpr ) {
        raString_ = raExpr;
        decString_ = decExpr;
        srString_ = srExpr;
    }

    public ConeQueryRowSequence createQuerySequence( StarTable table )
            throws IOException {
        return new JELQuerySequence( table );
    }

    /**
     * ConeQueryRowSequence implementation which does the work for this class.
     */
    private class JELQuerySequence extends SequentialJELRowReader
                                   implements ConeQueryRowSequence {

        private final Library lib_;
        private final CompiledExpression raExpr_;
        private final CompiledExpression decExpr_;
        private final CompiledExpression srExpr_;

        /**
         * Constructor.
         *
         * @param   table providing the context for JEL expression evaluation
         */
        JELQuerySequence( StarTable table ) throws IOException {
            super( table );
            lib_ = JELUtils.getLibrary( this );
            raExpr_ = compileDouble( raString_ );
            decExpr_ = compileDouble( decString_ );
            srExpr_ = compileDouble( srString_ );
        }

        public double getRa() throws IOException {
            return evaluateDouble( raExpr_ );
        }

        public double getDec() throws IOException {
            return evaluateDouble( decExpr_ );
        }

        public double getRadius() throws IOException {
            return evaluateDouble( srExpr_ );
        }

        /**
         * Compiles a JEL expression.
         *
         * @param   sexpr   string expression
         * @return  compiled expression
         */
        private CompiledExpression compileDouble( String sexpr )
                throws IOException {
            try {
                return Evaluator.compile( sexpr, lib_, double.class );
            }
            catch ( CompilationException e ) {
                throw new IOException( "Bad numeric expression \"" + sexpr
                                     + "\"" + " - " + e.getMessage() );
            }
        }

        /**
         * Returns the double value for a compiled expression.
         *
         * @param  expr  expression to evaluate
         * @return  double value
         */
        private double evaluateDouble( CompiledExpression expr )
                throws IOException {
            Object obj;
            try {
                obj = evaluate( expr );
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + expr );
            }
            return obj instanceof Number
                 ? ((Number) obj).doubleValue()
                 : Double.NaN;
        }
    }
}
