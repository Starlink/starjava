package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Filter for selecting a contiguous range of rows.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2010
 */
public class RangeFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public RangeFilter() {
        super( "rowrange", "<first> <last>|+<count>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Includes only rows in a given range.",
            "The range can either be supplied as",
            "\"<code>&lt;first&gt; &lt;last&gt;</code>\",",
            "where row indices are inclusive, or",
            "\"<code>&lt;first&gt; +&lt;count&gt;</code>\".",
            "In either case, the first row is numbered 1.",
            "</p>",
            "<p>Thus, to get the first hundred rows, use either",
            "\"<code>rowrange 1 100</code>\" or",
            "\"<code>rowrange 1 +100</code>\"",
            "and to get the second hundred, either",
            "\"<code>rowrange 101 200</code>\" or",
            "\"<code>rowrange 101 +100</code>\"",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {

        /* Get first index. */
        long ifirst;
        if ( argIt.hasNext() ) {
            String iStr = (String) argIt.next();
            argIt.remove();
            if ( iStr.matches( "[1-9][0-9]*" ) ) {
                ifirst = Long.parseLong( iStr );
            }
            else {
                throw new ArgException( "Row index " + iStr
                                      + " must be integer >0" );
            }
        }
        else {
            throw new ArgException( "Arg <first> not given" );
        }

        /* Get last index. */
        long ilast;
        if ( argIt.hasNext() ) {
            String iStr = (String) argIt.next();
            argIt.remove();
            if ( iStr.matches( "\\+[0-9]+" ) ) {
                ilast = ifirst + Long.parseLong( iStr.substring( 1 ) ) - 1;
            }
            else if ( iStr.matches( "[0-9]+" ) ) {
                ilast = Long.parseLong( iStr );
                if ( ifirst > ilast ) {
                    throw new ArgException( ifirst + " > " + ilast );
                }
            }
            else {
                throw new ArgException( "Row index " + iStr + " not numeric" );
            }
        }
        else {
            throw new ArgException( "Arg <last>|+<count> not given" );
        }

        /* Return row selected table as appropriate. */
        final long ifrom = ifirst - 1;
        final long ito = ilast;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                return new RangeTable( base, ifrom, ito );
            }
        };
    }

    /**
     * StarTable implementation which selects a contiguous range of rows.
     */
    private static class RangeTable extends WrapperStarTable {
        private final long ifrom_;
        private final long ito_;

        /**
         * @param   base  base table
         * @param   ifrom  first 0-based row index to include
         * @param   ito    first 0-based row index to exclude
         */
        RangeTable( StarTable base, long ifrom, long ito ) {
            super( base );
            ifrom_ = ifrom;
            ito_ = ito;
        }

        public long getRowCount() {
            long nbase = super.getRowCount();
            return nbase > 0L ? Math.min( nbase, ito_ ) - ifrom_
                              : -1L;
        }

        public Object getCell( long irow, int icol ) throws IOException {
            return super.getCell( convertRow( irow ), icol );
        }

        public Object[] getRow( long irow ) throws IOException {
            return super.getRow( convertRow( irow ) );
        }

        public RowSequence getRowSequence() throws IOException {
            if ( getRowCount() == 0 ) {
                return EmptyRowSequence.getInstance(); 
            }
            else if ( isRandom() ) {
                return new RandomRowSequence( this );
            }
            else {
                RowSequence baseSeq = super.getRowSequence();
                for ( long i = 0; i < ifrom_ && baseSeq.next(); i++ );
                return new WrapperRowSequence( baseSeq ) {
                    long nleft = ito_ - ifrom_;
                    public boolean next() throws IOException {
                        return nleft-- > 0 && super.next();
                    }
                };
            }
        }

        /**
         * Converts a row index from the value used for this table to
         * the value in the base table.  If out of range, an exception
         * is thrown.
         *
         * @param   irow   row index in this table
         * @return   row index in base table
         */
        private long convertRow( long irow ) {
            if ( irow >= 0 && irow < ito_ - ifrom_ ) {
                return irow + ifrom_;
            }
            else {
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
