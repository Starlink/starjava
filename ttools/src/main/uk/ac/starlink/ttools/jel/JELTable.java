package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.ExecutionException;

/**
 * Wrapper table which is constructed entirely of columns defined by 
 * JEL expressions based on the base table.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2005
 */
public class JELTable extends WrapperStarTable {

    private final StarTable baseTable_;
    private final String[] exprs_;
    private final ColumnInfo[] colInfos_;
    private final CompiledExpression[] randomCompexs_;
    private final RandomJELRowReader randomReader_;
    private final int ncol_;

    /**
     * Constructor.
     * The number of columns is the same as the number of elements of
     * <code>colInfos</code>, which must be the same as the number of
     * elements in <code>exprs</code>.
     * If the <code>contentClass</code> attributes of the <code>colInfos</code>
     * elements are non-null, they must be compatible with the actual
     * types of the evaluated expressions.  If they are not, an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * @param  baseTable  table which provides both behaviour determining
     *         whether random access is available etc, and an evaluation
     *         context for the JEL calculations
     * @param  colInfos   column metadata for each of the columns 
     *         in this table
     * @param  exprs   JEL expressions, evaluated in a context determined
     *         by <code>baseTable</code>, which give the data for this table.
     */
    public JELTable( StarTable baseTable, ColumnInfo[] colInfos,
                     String[] exprs ) throws CompilationException {
        super( baseTable );
        baseTable_ = baseTable;
        exprs_ = exprs;
        colInfos_ = colInfos;
        if ( exprs_.length != colInfos_.length ) {
            throw new IllegalArgumentException( "How many columns??" );
        }
        ncol_ = exprs.length;

        /* Compile the expressions ready for random evaluation. */
        randomReader_ = new RandomJELRowReader( baseTable );
        Library lib = JELUtils.getLibrary( randomReader_ );
        randomCompexs_ = new CompiledExpression[ ncol_ ];
        for ( int i = 0; i < ncol_; i++ ) {
            final String expr = exprs_[ i ];
            ColumnInfo colInfo = colInfos_[ i ];
            try {
                randomCompexs_[ i ] = JELUtils.compile( lib, baseTable, expr );
            }

            /* If there's trouble, rethrow the exception to give more
             * information. */
            catch ( final CompilationException e ) {
                throw new CustomCompilationException( "Bad expression " + expr
                                                    + ": " + e.getMessage(),
                                                      e );
            }

            /* Check that the type of the compiled expression is compatible
             * with that specified in the ColInfos, if any. */
            Class pClazz = JELUtils.getExpressionType( lib, baseTable, expr );
            Class clazz = JELUtils.getWrapperType( pClazz );
            Class reqClazz = colInfos_[ i ].getContentClass();
            if ( reqClazz != null &&
                 ! reqClazz.isAssignableFrom( clazz ) ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( "Column " )
                    .append( colInfo.getName() )
                    .append( ": expression " )
                    .append( expr )
                    .append( " has type " )
                    .append( clazz.getName() )
                    .append( ", incompatible with required type " )
                    .append( reqClazz.getName() );
                throw new IllegalArgumentException( sbuf.toString() );
            }
            colInfos_[ i ].setContentClass( clazz );
        }
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public Object getCell( long irow, int icol ) throws IOException {
        try {
            return randomReader_.evaluateAtRow( randomCompexs_[ icol ], irow );
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

    public Object[] getRow( long irow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = getCell( irow, icol );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final SequentialJELRowReader seqReader =
            new SequentialJELRowReader( baseTable_ );
        Library lib = JELUtils.getLibrary( seqReader );
        final CompiledExpression[] seqCompexs = new CompiledExpression[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            String expr = exprs_[ icol ];
            try {
                seqCompexs[ icol ] = JELUtils.compile( lib, baseTable, expr );
            }
            catch ( CompilationException e ) {
                // This shouldn't really happen since we already tried to
                // compile it in the constructor to test it.  However, just
                // rethrow it if it does.
                throw (IOException) new IOException( "Bad expression: " + expr )
                                   .initCause( e );
            }
        }
        return new WrapperRowSequence( seqReader ) {

            public Object getCell( int icol ) throws IOException {
                try {
                    return seqReader.evaluate( seqCompexs[ icol ] );
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

            public Object[] getRow() throws IOException {
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = getCell( icol );
                }
                return row;
            }
        };
    }

    /**
     * Convenience factory method.  It turns all the supplied <code>infos</code>
     * into ColumnInfos and rethrows any CompilationException as an
     * ExecutionException.
     *
     * @param  baseTable  table which provides both behaviour determining
     *         whether random access is available etc, and an evaluation
     *         context for the JEL calculations
     * @param  infos  metadata used to construct column metadata
     * @param  exprs   JEL expressions, evaluated in a context determined
     *         by <code>baseTable</code>, which give the data for this table.
     */
    public static StarTable createJELTable( StarTable baseTable,
                                            ValueInfo[] infos, String[] exprs )
            throws ExecutionException {
        int ncol = exprs.length;
        if ( infos.length != ncol ) {
            throw new IllegalArgumentException( "Column length mismatch" );
        }
        ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            colInfos[ i ] = new ColumnInfo( infos[ i ] );
        }
        try {
            return new JELTable( baseTable, colInfos, exprs );
        }
        catch ( CompilationException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
    }
}
