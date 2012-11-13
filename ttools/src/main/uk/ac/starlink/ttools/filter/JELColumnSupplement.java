package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;

/**
 * ColumnSupplement that generates new columns based on JEL expressions.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2012
 */
public class JELColumnSupplement implements ColumnSupplement {

    private final StarTable inTable_;
    private final String[] exprs_;
    private final int ncol_;
    private final ColumnInfo[] outColInfos_;
    private final RandomJELRowReader randomReader_;
    private final CompiledExpression[] randomCompexs_;

    /**
     * Constructs a multiple-column JEL column supplement.
     *
     * @param   inTable  table providing JEL context
     * @param   exprs    JEL expressions for columns
     * @param   colInfos metadata for columns
     *                   (data types may be changed to match expression output);
     *                   if null, names are generated automatically
     * @throws  IOException  with a helpful message if one of the expressions
     *                       cannot be compiled
     */
    public JELColumnSupplement( StarTable inTable, String[] exprs,
                                ColumnInfo[] colInfos ) throws IOException {
        inTable_ = inTable;
        ncol_ = exprs.length;
        if ( colInfos == null ) {
            colInfos = new ColumnInfo[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                colInfos[ ic ] =
                    new ColumnInfo( "col" + ( ic + 1 ), Object.class, null );
            }
        }
        else if ( colInfos.length != ncol_ ) {
            throw new IllegalArgumentException( "How many output columns?" );
        }
        exprs_ = (String[]) exprs.clone();

        /* Compile the expressions ready for random evaluation. */
        randomReader_ = new RandomJELRowReader( inTable_ );
        Library randomLib = JELUtils.getLibrary( randomReader_ );
        randomCompexs_ = new CompiledExpression[ ncol_ ];
        outColInfos_ = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            String expr = exprs[ icol ];
            try {
                randomCompexs_[ icol ] =
                    JELUtils.compile( randomLib, inTable_, expr );

                /* Set the content class for the new column to be that
                 * returned by the expression. */
                Class primType =
                    JELUtils.getExpressionType( randomLib, inTable_, expr );
                Class clazz = JELUtils.getWrapperType( primType );
                outColInfos_[ icol ] = new ColumnInfo( colInfos[ icol ] );
                outColInfos_[ icol ].setContentClass( clazz );
            }
            catch ( CompilationException e ) {
                throw JELUtils.toIOException( e, expr );
            }
        }
    }

    /**
     * Constructs a single-column JEL column supplement.
     *
     * @param   inTable   table providing JEL context
     * @param   expr      JEL expression for column
     * @param   colInfo   metadata for column
     *                    (data type may be changed to match expression output);
     *                    if null, name is generated automatically
     */
    public JELColumnSupplement( StarTable inTable, String expr,
                                ColumnInfo colInfo )
            throws IOException {
        this( inTable, new String[] { expr },
              colInfo == null ? null : new ColumnInfo[] { colInfo } );
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return outColInfos_[ icol ];
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return evaluateRandom( irow, randomCompexs_[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = getCell( irow, icol );
        }
        return row;
    }

    public SupplementSequence createSequence( RowSequence rseq ) {
        return new JELSupplementSequence( inTable_, exprs_, rseq );
    }

    /**
     * Performs random-access evaluation of the JEL expression
     * for a given cell.
     *
     * @param   irow  row index
     * @param   compex  compiled expression
     * @return   cell value
     */
    private Object evaluateRandom( long irow, CompiledExpression compex )
            throws IOException {
        try {
            return randomReader_.evaluateAtRow( compex, irow );
        }
        catch ( RuntimeException e ) {
            return null;
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Error e ) {
            throw e;
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * JEL row reader which also functions as a SupplementSequence.
     */
    private static class JELSupplementSequence extends StarTableJELRowReader
                                               implements SupplementSequence {
        private final RowSequence rseq_;
        private final CompiledExpression[] seqCompexs_;
        private final int ncol_;
        private long lrow_;

        /**
         * Constructor.
         *
         * @param   table  table providing JEL context
         * @param   exprs  JEL expressions for columns
         * @param   rseq   row sequence from <code>table</code> providing
         *                 the base values for this sequence
         */
        JELSupplementSequence( StarTable table, String[] exprs,
                               RowSequence rseq ) {
            super( table );
            lrow_ = -1;
            rseq_ = rseq;
            ncol_ = exprs.length;
            seqCompexs_ = new CompiledExpression[ ncol_ ];
            Library lib = JELUtils.getLibrary( this );
            for ( int icol = 0; icol < ncol_; icol++ ) {
                try {
                    seqCompexs_[ icol ] =
                        JELUtils.compile( lib, table, exprs[ icol ] );
                }
                catch ( CompilationException e ) {
                    throw (AssertionError)
                          new AssertionError( "Well it compiled OK last time" )
                         .initCause( e );
                }
            }
        }

        // JELRowReader method
        public long getCurrentRow() {
            return lrow_;
        }

        // JELRowReader method
        public Object getCell( int icol ) throws IOException {
            return rseq_.getCell( icol );
        }

        // SupplementSequence method
        public Object getCell( long irow, int icol ) throws IOException {
            lrow_ = irow;
            return evaluateAtCurrentRow( seqCompexs_[ icol ] );
        }

        // SupplementSequence method.
        public Object[] getRow( long irow ) throws IOException {
            lrow_ = irow;
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                row[ icol ] = evaluateAtCurrentRow( seqCompexs_[ icol ] );
            }
            return row;
        }

        /**
         * Evaluates the given expression throwing an IOException if there is
         * an error.
         *
         * @param  expr  compiled expression
         */
        private Object evaluateAtCurrentRow( CompiledExpression compex )
                throws IOException {
            try {
                return evaluate( compex );
            }
            catch ( RuntimeException e ) {
                return null;
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Error e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
    }
}
