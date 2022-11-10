package uk.ac.starlink.table;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * RowSequence wrapper class that guarantees a given number of rows.
 * The expectation is that the underlying row sequence has the declared
 * row count, but if it has a different number the output of this sequence
 * is forced to the declared value by adding dummy rows or discarding
 * extra rows as required.  If such adjustments are required, suitable
 * messages are written through the logging system.
 *
 * @author   Mark Taylor
 * @since    10 Nov 2022
 */
public class CountCheckRowSequence implements RowSequence {

    private final RowSequence base_;
    private final int ncol_;
    private final long nrow_;
    private long irow_;
    private RowData rowData_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table" );

    /**
     * Constructor.
     *
     * @param  base   row sequence on which this one is based
     * @param  ncol   number of columns (required for constructing dummy rows)
     * @param  nrow   required number of rows, &gt;=0
     */
    private CountCheckRowSequence( RowSequence base, int ncol, long nrow ) {
        base_ = base;
        ncol_ = ncol;
        nrow_ = nrow;
        rowData_ = base_;
        assert nrow_ >= 0;
    }

    public boolean next() throws IOException {
        boolean hasNext = base_.next();
        if ( irow_ < nrow_ ) {
            if ( ! hasNext && rowData_ == base_ ) {
                String msg = new StringBuffer()
                   .append( "Missing rows at row #" )
                   .append( irow_ )
                   .append( "/" )
                   .append( nrow_ )
                   .append( ": adding " )
                   .append( nrow_ - irow_ )
                   .append( " dummies" )
                   .toString();
                logger_.warning( msg );
                rowData_ = new RowData() {
                    public Object getCell( int icol ) {
                        return null;
                    }
                    public Object[] getRow() {
                        return new Object[ ncol_ ];
                    }
                };
            }
            irow_++;
            return true;
        }
        else {
            if ( hasNext && irow_ >= nrow_ ) {
                long nextra = 1;
                try {
                    while ( base_.next() ) {
                        nextra++;
                    }
                }
                catch ( IOException e ) {
                    nextra = -1;
                }
                String msg = new StringBuffer()
                    .append( "Too many rows at row #" )
                    .append( irow_ )
                    .append( "/" )
                    .append( nrow_ )
                    .append( ": discarding" )
                    .append( nextra >= 0 ? ( " " + nextra ) : "" )
                    .append( " extras" )
                    .toString();
                logger_.warning( msg );
            }
            return false;
        }
    }

    public Object getCell( int icol ) throws IOException {
        return rowData_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return rowData_.getRow();
    }

    public void close() throws IOException {
        base_.close();
    }

    /**
     * Returns a row sequence guaranteed to give the required number of rows.
     *
     * @param  rseq  row sequence on which the returned one is based
     * @param  ncol  number of columns (required for constructing dummy rows)
     * @param  nrow  required number of rows, or -1 for no requirement
     * @return  safe row sequence; if <code>nrow&lt;0</code> the input
     *          sequence will be returned
     */
    public static RowSequence getSafeRowSequence( RowSequence rseq, int ncol,
                                                  long nrow ) {
        return nrow >= 0 ? new CountCheckRowSequence( rseq, ncol, nrow )
                         : rseq;
    }
}
