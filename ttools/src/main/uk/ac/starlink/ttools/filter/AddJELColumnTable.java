package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * Wrapper table which adds one or more columns, defined by JEL expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class AddJELColumnTable extends WrapperStarTable {

    private final int nAdded_;
    private final StarTable baseTable_;
    private final String[] exprs_;
    private final int[] colMap_;
    private final RandomJELRowReader randomReader_;
    private final CompiledExpression[] randomCompexs_;
    private final ColumnInfo[] addInfos_;

    /**
     * Constructs a table which adds a single new column at a given
     * column index.
     *
     * @param   baseTable   table on which this one is based
     * @param   cinfo     ColumnInfo describing the column to be added.
     *                    Only the name has to be set; other metadata items
     *                    will be used if available except the contentClass,
     *                    which is determined from the return type of 
     *                    the compiled expression
     * @param   expr    JEL expression defining the value of the new column
     * @param   ipos    position of the new column 
     */
    public AddJELColumnTable( StarTable baseTable, ColumnInfo cinfo, 
                              String expr, int ipos )
            throws CompilationException {
        this( baseTable, new ColumnInfo[] { cinfo }, new String[] { expr },
              ipos );
    }

    /**
     * Constructs a table which adds a list of new columns.
     *
     *
     * @param   baseTable   table on which this one is based
     * @param   cinfos    array of ColumnInfos describing the new columns
     *                    to be added.  Only the names have to be set; 
     *                    other metadata items will be used if available
     *                    apart from the contentClass, which is determined
     *                    from the return type of the compiled expression
     * @param   exprs   JEL expressions defining the value of the new columns
     * @param   ipos   column index of the first new column; the others
     *                 will follow straight after it
     */
    public AddJELColumnTable( StarTable baseTable, ColumnInfo[] cinfos,
                              String[] exprs, int ipos )
            throws CompilationException {
        super( baseTable ); 
        baseTable_ = baseTable;
        exprs_ = (String[]) exprs.clone();
        nAdded_ = exprs_.length;
        if ( cinfos.length != nAdded_ ) {
            throw new IllegalArgumentException( "How many new columns??" );
        }
        if ( ipos < 0 ) {
            ipos = baseTable.getColumnCount();
        }

        /* Store a map of which column in the base table is used for each
         * column in this table.  The first newly-added column is -1, the 
         * next is -2, etc. */
        colMap_ = new int[ baseTable.getColumnCount() + nAdded_ ];
        int j = 0;
        for ( int i = 0; i < colMap_.length; i++ ) {
            int k = i - ipos;
            colMap_[ i ] = ( k >= 0 && k < nAdded_ )
                         ? - ( k + 1 )
                         : j++;
        }
        assert j == baseTable.getColumnCount();

        /* Compile the expressions ready for random evaluation. */
        randomReader_ = new RandomJELRowReader( baseTable );
        Library lib = JELUtils.getLibrary( randomReader_ );
        randomCompexs_ = new CompiledExpression[ nAdded_ ];
        addInfos_ = new ColumnInfo[ nAdded_ ];
        for ( int i = 0; i < nAdded_; i++ ) {
            String expr = exprs_[ i ];
            randomCompexs_[ i ] = JELUtils.compile( lib, baseTable, expr );

            /* Set the content class for the new column to be that
             * returned by the expression. */
            Class primType = JELUtils.getExpressionType( lib, baseTable, expr );
            Class clazz = JELUtils.getWrapperType( primType );
            addInfos_[ i ] = cinfos[ i ];
            addInfos_[ i ].setContentClass( clazz );
        }
    }

    public int getColumnCount() {
        return colMap_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        int ibase = colMap_[ icol ];
        return ibase >= 0 ? super.getColumnInfo( ibase )
                          : addInfos_[ -1 - ibase ];
    }

    public Object getCell( long irow, int icol ) throws IOException {
        int ibase = colMap_[ icol ];
        return ibase >= 0 ? super.getCell( irow, ibase )
                          : evaluateAtRow( irow, -1 - ibase );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] baseRow = super.getRow( irow );
        int ncol = colMap_.length;
        assert ncol == baseRow.length + 1;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int ibase = colMap_[ icol ];
            row[ icol ] = ibase >= 0 ? baseRow[ ibase ]
                                     : evaluateAtRow( irow, -1 - ibase );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final SequentialJELRowReader seqReader =
            new SequentialJELRowReader( baseTable_ ); 
        final CompiledExpression[] seqCompexs = 
            new CompiledExpression[ nAdded_ ];
        Library lib = JELUtils.getLibrary( seqReader );
        for ( int i = 0; i < nAdded_; i++ ) {
            try {
                seqCompexs[ i ] =
                    JELUtils.compile( lib, baseTable_, exprs_[ i ] );
            }
            catch ( CompilationException e ) {
                // This shouldn't really happen since we already tried to
                // compile it in the constructor to test it.  However, just
                // rethrow it if it does.
                throw (IOException)
                      new IOException( "Bad expression: " + exprs_[ i ] )
                     .initCause( e );
            }
        }
        return new WrapperRowSequence( seqReader ) {

            public Object getCell( int icol ) throws IOException {
                if ( seqReader.getCurrentRow() < 0 ) {
                    throw new IllegalStateException( "Before start of table" );
                }
                int ibase = colMap_[ icol ];
                return ibase >= 0 ? super.getCell( ibase )
                                  : evaluate( -1 - ibase );
            }

            public Object[] getRow() throws IOException {
                Object[] baseRow = super.getRow();
                int ncol = colMap_.length;
                assert ncol == baseRow.length + 1;
                Object[] row = new Object[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    int ibase = colMap_[ icol ];
                    row[ icol ] = ibase >= 0 ? baseRow[ ibase ]
                                             : evaluate( -1 - ibase );
                }
                return row;
            }

            /**
             * Evaluates the JEL expression at the current row.
             *
             * @param  iAddcol  index of the added column (first added
             *         column is zero, second is 1, ...)
             */
            private Object evaluate( int iAddcol ) throws IOException {
                try {
                    return seqReader.evaluate( seqCompexs[ iAddcol ] );
                }
                catch ( IOException e ) {
                    throw e;
                }
                catch ( RuntimeException e ) {
                    return null;
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
     * Evaluates the JEL expression at a given row.
     *
     * @param  irow  row index
     * @param  iAddcol  index of the added column (first added
     *         column is zero, second is 1, ...)
     */
    private Object evaluateAtRow( long irow, int iAddcol ) throws IOException {
        if ( irow < 0 ) {
            throw new IllegalStateException( "Illegal row index " + irow );
        }
        try {
            return randomReader_
                  .evaluateAtRow( randomCompexs_[ iAddcol ], irow );
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Error e ) {
            throw e;
        }
        catch ( RuntimeException e ) {
            return null;
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
