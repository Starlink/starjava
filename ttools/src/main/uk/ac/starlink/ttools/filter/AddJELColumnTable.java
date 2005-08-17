package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.JELUtils;
import uk.ac.starlink.ttools.RandomJELRowReader;
import uk.ac.starlink.ttools.SequentialJELRowReader;

/**
 * Wrapper table which adds a single column, defined by a JEL expression.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class AddJELColumnTable extends WrapperStarTable {

    private final StarTable baseTable_;
    private final String expr_;
    private final int[] colMap_;
    private final RandomJELRowReader randomReader_;
    private final CompiledExpression randomCompex_;
    private final ColumnInfo addInfo_;

    /**
     * Constructor.
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
        super( baseTable );
        baseTable_ = baseTable;
        expr_ = expr;

        /* Store a map of which column in the base table is used for each
         * column in this table.  One of the elements is -1, which means
         * it's the newly added column. */
        colMap_ = new int[ baseTable.getColumnCount() + 1 ];
        int j = 0;
        for ( int i = 0; i < colMap_.length; i++ ) {
            colMap_[ i ] = i == ipos ? -1
                                     : j++;
        }
        assert j == baseTable.getColumnCount();

        /* Compile the expression ready for random evaluation. */
        randomReader_ = new RandomJELRowReader( baseTable );
        Library lib = JELUtils.getLibrary( randomReader_ );
        randomCompex_ = Evaluator.compile( expr_, lib );

        /* Set the content class for the new column to be that returned by
         * the expression. */
        Class clazz =
            JELUtils.getWrapperType( JELUtils.getExpressionType( lib, expr ) ); 
        addInfo_ = cinfo;
        addInfo_.setContentClass( clazz );
    }

    public int getColumnCount() {
        return colMap_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        int ibase = colMap_[ icol ];
        return ibase >= 0 ? super.getColumnInfo( ibase )
                          : addInfo_;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        int ibase = colMap_[ icol ];
        return ibase >= 0 ? super.getCell( irow, ibase )
                          : evaluateAtRow( irow );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] baseRow = super.getRow( irow );
        int ncol = colMap_.length;
        assert ncol == baseRow.length + 1;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int ibase = colMap_[ icol ];
            row[ icol ] = ibase >= 0 ? baseRow[ ibase ]
                                     : evaluateAtRow( irow );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final SequentialJELRowReader seqReader =
            new SequentialJELRowReader( baseTable_ ); 
        final CompiledExpression seqCompex;
        try {
            seqCompex =
                Evaluator.compile( expr_, JELUtils.getLibrary( seqReader ) );
        }
        catch ( CompilationException e ) {
            // This shouldn't really happen since we already tried to
            // compile it in the constructor to test it.  However, just
            // rethrow it if it does.
            throw (IOException) new IOException( "Bad expression: " + expr_ )
                               .initCause( e );
        }
        return new WrapperRowSequence( seqReader ) {

            public Object getCell( int icol ) throws IOException {
                int ibase = colMap_[ icol ];
                return ibase >= 0 ? super.getCell( ibase )
                                  : evaluate();
            }

            public Object[] getRow() throws IOException {
                Object[] baseRow = super.getRow();
                int ncol = colMap_.length;
                assert ncol == baseRow.length + 1;
                Object[] row = new Object[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    int ibase = colMap_[ icol ];
                    row[ icol ] = ibase >= 0 ? baseRow[ ibase ]
                                             : evaluate();
                }
                return row;
            }

            /**
             * Evaluates the JEL expression at the current row.
             */
            private Object evaluate() throws IOException {
                try {
                    return seqReader.evaluate( seqCompex );
                }
                catch ( IOException e ) {
                    throw e;
                }
                catch ( RuntimeException e ) {
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
     * Evaluates the JEL expression at a given row.
     *
     * @param  irow  row index
     */
    private Object evaluateAtRow( long irow ) throws IOException {
        try {
            return randomReader_.evaluateAtRow( randomCompex_, irow );
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Error e ) {
            throw e;
        }
        catch ( RuntimeException e ) {
            throw e;
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
