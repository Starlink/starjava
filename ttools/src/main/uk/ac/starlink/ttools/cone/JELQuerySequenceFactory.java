package uk.ac.starlink.ttools.cone;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.task.SkyCoordParameter;

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
     * @param  raExpr  JEL expression for right ascension in degrees;
     *                 if null a guess will be attempted
     * @param  decExpr JEL expression for declination in degrees;
     *                 if null a guess will be attempted
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
        return new JELQuerySequence( table, raString_, decString_, srString_ );
    }

    /**
     * ConeQueryRowSequence implementation which does the work for this class.
     */
    private static class JELQuerySequence extends SequentialJELRowReader
                                          implements ConeQueryRowSequence {

        private final Library lib_;
        private final CompiledExpression raExpr_;
        private final CompiledExpression decExpr_;
        private final CompiledExpression srExpr_;

        /**
         * Constructor.
         *
         * @param   table providing the context for JEL expression evaluation
         * @param   raString   supplied string for RA expression, may be null
         * @param   decString  supplied string for Dec expression, may be null
         * @param   srString   supplied string for radius expression
         */
        JELQuerySequence( StarTable table, String raString, String decString,
                          String srString ) throws IOException {
            super( table );
            lib_ = JELUtils.getLibrary( this );
            if ( raString == null || raString.trim().length() == 0 ) {
                raString =
                    SkyCoordParameter.guessRaDegreesExpression( table );
            }
            if ( decString == null || decString.trim().length() == 0 ) {
                decString =
                    SkyCoordParameter.guessDecDegreesExpression( table );
            }
            if ( raString == null || decString == null ) {
                throw new IOException( "Failed to identify "
                                     + "likely RA/Dec columns" );
            }
            raExpr_ = compileDouble( raString );
            decExpr_ = compileDouble( decString );
            srExpr_ = compileDouble( srString );
        }

        public double getRa() throws IOException {
            return doEvaluateDouble( raExpr_ );
        }

        public double getDec() throws IOException {
            return doEvaluateDouble( decExpr_ );
        }

        public double getRadius() throws IOException {
            return doEvaluateDouble( srExpr_ );
        }

        public long getIndex() {
            return getCurrentRow();
        }

        /**
         * Compiles a JEL expression.
         *
         * @param   sexpr   string expression
         * @return  compiled expression
         */
        private CompiledExpression compileDouble( String sexpr )
                throws IOException {
            if ( sexpr == null || "null".equals( sexpr ) ||
                 sexpr.trim().length() == 0 ) {
                sexpr = "NULL";
            }
            try {
                return JELUtils.compile( lib_, getTable(), sexpr,
                                         double.class );
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
        private double doEvaluateDouble( CompiledExpression expr )
                throws IOException {
            try {
                return evaluateDouble( expr );
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + expr );
            }
        }
    }
}
