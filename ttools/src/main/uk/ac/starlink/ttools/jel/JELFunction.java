package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private final XConstant xconst_;
    private final CompiledExpression fCompex_;
    private final Constant<?>[] usedConsts_;
    private final Object[] args_;

    /**
     * Constructs a function with no external constants.
     *
     * @param   xvarname  name of the independent variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     */
    public JELFunction( String xvarname, String fexpr )
            throws CompilationException {
        this( xvarname, fexpr, null );
    }

    /**
     * Constructs a function that can reference provided constants.
     *
     * @param   xvarname  name of the independent variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     * @param   constMap  map by name of constant values that can be
     *                    referenced in the expression, may be null
     */
    public JELFunction( String xvarname, String fexpr,
                        Map<String,? extends Constant<?>> constMap )
            throws CompilationException {
        xvarname_ = xvarname;
        fexpr_ = fexpr;
        xconst_ = new XConstant();
        Map<String,Constant<?>> fconstMap = new HashMap<>();
        if ( constMap != null ) {
            fconstMap.putAll( constMap );
        }
        fconstMap.put( xvarname, xconst_ );
        ConstantResolver resolver = new ConstantResolver( fconstMap );
        Class<?>[] staticLib =
            JELUtils.getStaticClasses().toArray( new Class<?>[ 0 ] );
        Class<?>[] dynamicLib = new Class<?>[] { resolver.getClass() };
        Library lib =
            JELUtils.createLibrary( staticLib, dynamicLib, resolver );
        fCompex_ = Evaluator.compile( fexpr, lib, double.class );
        args_ = new Object[] { resolver };
        Set<Constant<?>> usedConstSet =
            new HashSet<>( resolver.getTranslatedConstants() );
        usedConstSet.remove( xconst_ );
        usedConsts_ = usedConstSet.toArray( new Constant<?>[ 0 ] );
    }

    /**
     * Returns an array of any constants that were referenced in this
     * function's expression.  This list does not include an entry
     * for the X value expression.
     *
     * @return  constants from the map supplied at construction time
     *          that are referenced in this function's expression
     */
    public Constant<?>[] getReferencedConstants() {
        return usedConsts_.clone();
    }

    /**
     * Evaluates this function at a given value of the independent variable.
     * In case of an evaluation error of some kind, NaN is returned.
     *
     * @param   x  variable value
     * @return  function value
     */
    public double evaluate( double x ) {
        xconst_.value_ = x;
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
     * Constant implementation for the independent variable of this function.
     */
    private static class XConstant implements Constant<Double> {
        volatile double value_;
        public Class<Double> getContentClass() {
            return Double.class;
        }
        public Double getValue() {
            return Double.valueOf( value_ );
        }
        public boolean requiresRowIndex() {
            return false;
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
