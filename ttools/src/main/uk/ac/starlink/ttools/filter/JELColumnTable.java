package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * Creates a new table whose columns are derived by evaluating JEL expressions
 * against an existing table.
 * Table randomness, row count etc is taken from the input table,
 * but not table parameters etc.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2011
 */
public class JELColumnTable extends AbstractStarTable {

    private final StarTable inTable_;
    private final String[] exprs_;
    private final ColumnInfo[] outColInfos_;
    private final int ncol_;
    private final RandomJELRowReader randomReader_;
    private final CompiledExpression[] randomCompexs_;

    /**
     * Constructs a multiple-column JEL table.
     *
     * @param   inTable  table providing JEL context
     * @param   exprs    JEL expressions for columns
     * @param   colInfos metadata for columns
     *                   (data types may be changed to match expression output)
     */
    public JELColumnTable( StarTable inTable, String[] exprs,
                           ColumnInfo[] colInfos )
            throws CompilationException {
        inTable_ = inTable;
        ncol_ = exprs.length;
        if ( colInfos.length != ncol_ ) {
            throw new IllegalArgumentException( "How many output columns?" );
        }
        exprs_ = (String[]) exprs.clone();

        /* Compile the expressions ready for random evaluation. */
        randomReader_ = new RandomJELRowReader( inTable_ );
        Library lib = JELUtils.getLibrary( randomReader_ );
        randomCompexs_ = new CompiledExpression[ ncol_ ];
        outColInfos_ = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            String expr = exprs[ icol ];
            randomCompexs_[ icol ] = JELUtils.compile( lib, inTable_, expr );

            /* Set the content class for the new column to be that
             * returned by the expression. */
            Class primType = JELUtils.getExpressionType( lib, inTable_, expr );
            Class clazz = JELUtils.getWrapperType( primType );
            outColInfos_[ icol ] = new ColumnInfo( colInfos[ icol ] );
            outColInfos_[ icol ].setContentClass( clazz );
        }
    }

    /**
     * Constructs a single-column JEL table.
     *
     * @param   inTable   table providing JEL context
     * @param   expr      JEL expression for column
     * @param   colInfo   metadata for column
     *                    (data type may be changed to match expression output)
     */
    public JELColumnTable( StarTable inTable, String expr, ColumnInfo colInfo )
            throws CompilationException {
        this( inTable, new String[] { expr }, new ColumnInfo[] { colInfo } );
    }

    public long getRowCount() {
        return inTable_.getRowCount();
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return outColInfos_[ icol ];
    }

    public boolean isRandom() {
        return inTable_.isRandom();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return evaluateAtRow( irow, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = getCell( irow, icol );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final SequentialJELRowReader seqReader =
            new SequentialJELRowReader( inTable_ );
        final CompiledExpression[] seqCompexs =
            new CompiledExpression[ ncol_ ];
        Library lib = JELUtils.getLibrary( seqReader );
        for ( int icol = 0; icol < ncol_; icol++ ) {
            try {
                seqCompexs[ icol ] =
                    JELUtils.compile( lib, inTable_, exprs_[ icol ] );
            }
            catch ( CompilationException e ) {
                throw (AssertionError)
                      new AssertionError( "Well it compiled OK last time" )
                     .initCause( e );
            }
        }
        return new RowSequence() {
            public boolean next() throws IOException {
                return seqReader.next();
            }
            public Object getCell( int icol ) throws IOException {
                return evaluateAtCurrentRow( icol );
            }
            public Object[] getRow() throws IOException {
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = getCell( icol );
                }
                return row;
            }
            public void close() throws IOException {
                seqReader.close();
            }

            /**
             * Evaluates the JEL expression for a given column at the
             * current row of this sequence.
             *
             * @param  icol  column index
             * @return   cell value
             */
            private Object evaluateAtCurrentRow( int icol ) throws IOException {
                if ( seqReader.getCurrentRow() < 0 ) {
                    throw new IllegalStateException( "No current row" );
                }
                CompiledExpression compex = seqCompexs[ icol ];
                try {
                    return seqReader.evaluate( compex );
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
        };
    }

    /**
     * Performs random-access evaluation of the JEL expression
     * for a given cell.
     * 
     * @param   irow  row index
     * @param   icol  column index
     * @return   cell value
     */
    private Object evaluateAtRow( long irow, int icol ) throws IOException {
        CompiledExpression compex = randomCompexs_[ icol ];
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
}
