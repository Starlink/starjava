package uk.ac.starlink.table.view;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.Hashtable;
import java.util.List;
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
    private JELRowReader rowReader;
    private Object[] args;
    private CompiledExpression compEx;

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
        this.expression = expression;
        setExpression( expression );
    }

    public void setExpression( String expression ) throws CompilationException {

        /* Get an up-to-date RowReader (an old one may not be aware of recent
         * changes to the StarTable or subset list). */
        rowReader = new JELRowReader( stable, subsets );
        args = new Object[] { rowReader };

        /* Compile the expression. */
        String exprsub = expression.replace( '#', '£' );
        compEx = Evaluator.compile( exprsub, getLibrary(), boolean.class );
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isIncluded( long lrow ) {
        synchronized ( rowReader ) {
            rowReader.setRow( lrow );
            try {
                return ((Boolean) compEx.evaluate( args )).booleanValue();
            }
            catch ( NullPointerException e ) {
                return false;
            }
            catch ( Throwable th ) {
                th.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Returns a JEL Library suitable for expression evaluation on this
     * column's table.
     *
     * @return   a library
     */
    private Library getLibrary() {
        Class[] staticLib = new Class[] { Math.class, Float.class };
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[] { String.class };
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

    public String toString() {
        return name + " (" + expression + ")";
    }

}
