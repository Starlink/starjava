package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.List;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * Sequential table which selects rows on the basis of a JEL-interpreted
 * expression.
 *
 * @see  uk.ac.starlink.ttools.jel.JELRowReader
 */
public class JELSelectorTable extends WrapperStarTable {

    private final String expr_;
    private final StarTable baseTable_;

    /**
     * Construct a table given a base table and a selection expression.
     *
     * @param  baseTable  base table
     * @param  expr   boolean algebraic expression describing inclusion test
     */
    public JELSelectorTable( StarTable baseTable, String expr ) 
            throws CompilationException {
        super( baseTable );
        baseTable_ = baseTable;
        expr_ = expr;

        /* Check the expression. */
        Library lib = JELUtils.getLibrary( new DummyJELRowReader( baseTable ) );
        JELUtils.checkExpressionType( lib, baseTable, expr, boolean.class );
    }

    public boolean isRandom() {
        return false;
    }

    public long getRowCount() {
        return -1L;
    }

    public RowSequence getRowSequence() throws IOException {
        final SequentialJELRowReader jelSeq = 
            new SequentialJELRowReader( baseTable_ );
        final CompiledExpression compEx;
        try {
            compEx = JELUtils.compile( JELUtils.getLibrary( jelSeq ),
                                       baseTable_, expr_, boolean.class );
        }
        catch ( CompilationException e ) {
            // This shouldn't really happen since we already tried to
            // compile it in the constructor to test it.  However, just
            // rethrow it if it does.
            throw JELUtils.toIOException( e, expr_ );
        }
        assert compEx.getType() == 0; // boolean
        
        return new WrapperRowSequence( jelSeq ) {

            public boolean next() throws IOException {
                while ( jelSeq.next() ) {
                    if ( isIncluded() ) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isIncluded() throws IOException {
                Object val;
                try {
                    val = jelSeq.evaluate( compEx );
                }
                catch ( Throwable e ) {
                    throw (IOException) new IOException( "Evaluation error" )
                                       .initCause( e );
                }
                Boolean bval = (Boolean) val;
                return bval != null && bval.booleanValue();
            }
        };
    }

}
