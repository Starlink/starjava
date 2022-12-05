package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * Provides a double precision function of one double precision
 * variable which can be evaluated using JEL.
 * The variable name and an expression giving the result in terms of that
 * variable name are supplied.
 *
 * <p>Instances of this class are not threadsafe, but could be made so
 * by putting a lock on the {@link #evaluate} method.
 *
 * @author    Mark Taylor
 * @since     14 Jun 2012
 */
public class JELFunction implements DoubleUnaryOperator {

    private final String xvarname_;
    private final String fexpr_;
    private final XResolver xResolver_;
    private final CompiledExpression fCompex_;
    private final Object[] args_;

    /**
     * Constructor.
     *
     * @param   xvarname  name of the independent variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     */
    public JELFunction( String xvarname, String fexpr )
            throws CompilationException {
        xvarname_ = xvarname;
        fexpr_ = fexpr;
        Class<?>[] staticLib =
            JELUtils.getStaticClasses().toArray( new Class<?>[ 0 ] );
        xResolver_ = new XResolver( xvarname );
        Class<?>[] dynamicLib = new Class<?>[] { xResolver_.getClass() };
        Library lib =
            JELUtils.createLibrary( staticLib, dynamicLib, xResolver_ );
        fCompex_ = Evaluator.compile( fexpr, lib, double.class );
        args_ = new Object[] { xResolver_ };
    }

    /**
     * Evaluates this function at a given value of the independent variable.
     * In case of an evaluation error of some kind, NaN is returned.
     *
     * @param   x  variable value
     * @return  function value
     */
    public double evaluate( double x ) {
        xResolver_.setXValue( x );
        try {
            return fCompex_.evaluate_double( args_ );
        }
        catch ( Throwable e ) {
            return Double.NaN;
        }
    }

    /**
     * Does exactly the same as {@link #evaluate}.
     */
    public double applyAsDouble( double x ) {
        return evaluate( x );
    }

    /**
     * Returns the name of the independent variable.
     *
     * @return   x variable name
     */
    public String getXVarName() {
        return xvarname_;
    }

    /**
     * Returns the text of the function expression.
     *
     * @return  function expression
     */
    public String getExpression() {
        return fexpr_;
    }

    /**
     * This public class is an implementation detail,
     * not intended for external use.
     */
    public static class XResolver extends DVMap {
        private final String xvarname_;
        private double dValue_;

        private XResolver( String xvarname ) {
            xvarname_ = xvarname;
        }

        public String getTypeName( String name ) {
            return name.equals( xvarname_ ) ? "Double" : null;
        }

        public double getDoubleProperty( String name ) {
            return name.equals( xvarname_ ) ? dValue_ : Double.NaN;
        }

        private void setXValue( double dval ) {
            dValue_ = dval;
        }
    }

    /**
     * Main method tests this class.
     * Args are varname, expr, varvalue, for instance "x", "3x+1", "29"
     */
    public static void main( String[] args ) {
        String usage = "Usage: " + JELFunction.class.getName() + " "
                     + "<varname> <expr> <varvalue>\n";
        if ( args.length != 3 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        String xvarname = args[ 0 ];
        String fexpr = args[ 1 ];
        String value = args[ 2 ];
        try {
            JELFunction f = new JELFunction( xvarname, fexpr );
            double x = Double.parseDouble( value );
            double y = f.evaluate( x );
            System.out.println( "f(" + xvarname + ")=" + fexpr + "\n"
                              + "f(" + value + ")=" + y );
        }
        catch ( Throwable e ) {
            e.printStackTrace();
            System.err.println( "\n" + usage );
            System.exit( 1 );
        }
    }
}
