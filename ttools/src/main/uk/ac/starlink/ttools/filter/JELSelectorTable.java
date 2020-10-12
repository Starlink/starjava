package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.List;
import java.util.function.LongSupplier;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.SequentialRowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;

/**
 * Sequential table which selects rows on the basis of a JEL-interpreted
 * expression.
 *
 * @see  uk.ac.starlink.ttools.jel.JELRowReader
 */
public class JELSelectorTable extends WrapperStarTable {

    private final String expr_;
    private final boolean requiresRowIndex_;
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
        StarTableJELRowReader rdr = new DummyJELRowReader( baseTable );
        Library lib = JELUtils.getLibrary( rdr );
        JELUtils.checkExpressionType( lib, baseTable, expr, boolean.class );
        requiresRowIndex_ = rdr.requiresRowIndex();
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getRowCount() {
        return -1L;
    }

    @Override
    public Object getCell( long irow, int icol ) {
        throw new UnsupportedOperationException( "Not random" );
    }

    @Override
    public Object[] getRow( long irow ) {
        throw new UnsupportedOperationException( "Not random" );
    }

    @Override
    public RowAccess getRowAccess() {
        throw new UnsupportedOperationException( "Not random" );
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
                    if ( isIncluded( jelSeq, compEx ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        return requiresRowIndex_
             ? new SequentialRowSplittable( this )
             : new JELSelectorRowSplittable( baseTable_.getRowSplittable() );
    }

    /**
     * Evaluates a boolean expression in the context of a row reader.
     *
     * @param  rdr  reader
     * @param  compEx  expression testing for inclusion
     * @return   true iff row is included
     */
    private static boolean isIncluded( StarTableJELRowReader rdr,
                                       CompiledExpression compEx )
            throws IOException {
        try {
            return rdr.evaluateBoolean( compEx );
        }
        catch ( Throwable e ) {
            throw new IOException( "Evaluation error", e );
        }
    }

    /**
     * RowSplittable instance for use with JELSelectorTable.
     */
    private class JELSelectorRowSplittable extends WrapperRowSequence
                                           implements RowSplittable {

        final RowSplittable baseSplittable_;
        final SequentialJELRowReader rdr_;
        final CompiledExpression compEx_;

        /**
         * Constructor.
         *
         * @param  baseSplittable  splittable for base table
         */
        JELSelectorRowSplittable( RowSplittable baseSplittable )
                throws IOException {
            super( baseSplittable );
            baseSplittable_ = baseSplittable;
            rdr_ = new SequentialJELRowReader( JELSelectorTable.this,
                                               baseSplittable );
            try {
                compEx_ = JELUtils.compile( JELUtils.getLibrary( rdr_ ),
                                            baseTable_, expr_, boolean.class );
            }
            catch ( CompilationException e ) {
                // This shouldn't happen since we already tried to
                // compile it in the constructor to test it.  However, just
                // rethrow it if it does.
                throw JELUtils.toIOException( e, expr_ );
            }
        }

        public RowSplittable split() {
            RowSplittable spl = baseSplittable_.split();
            if ( spl == null ) {
                return null;
            }
            else {
                try {
                    return new JELSelectorRowSplittable( spl );
                }
                catch ( IOException e ) {
                    return null;
                }
            }
        }

        public LongSupplier rowIndex() {
            return null;
        }

        public long splittableSize() {
            return baseSplittable_.splittableSize(); // best guess
        }

        @Override
        public boolean next() throws IOException {
            while ( super.next() ) {
                if ( isIncluded( rdr_, compEx_ ) ) {
                    return true;
                }
            }
            return false;
        }
    }
}
