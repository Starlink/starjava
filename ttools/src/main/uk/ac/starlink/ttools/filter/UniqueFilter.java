package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.ColumnIdentifier;

/**
 * Filters out rows which are identical in some or all columns.
 *
 * @author   Mark Taylor
 * @since    27 Apr 2006
 */
public class UniqueFilter extends BasicFilter {

    public UniqueFilter() {
        super( "uniq", "[<colid-list>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "Eliminates adjacent rows which have the same values.",
            "If used with no arguments, then any row which has identical",
            "values to its predecessor is removed.",
            "If the <code>&lt;colid-list&gt;</code> parameter is given",
            "then only the values in the specified columns must be equal",
            "in order for the row to be removed.",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) {
        String testIds = null;
        if ( argIt.hasNext() ) {
            testIds = (String) argIt.next();
            argIt.remove();
        }
        final String tids = testIds;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                return new UniqueTable( base, tids );
            }
        };
    }

    private static class UniqueTable extends WrapperStarTable {

        final boolean[] testFlags_;

        UniqueTable( StarTable base, String testIds ) throws IOException {
            super( base );
            if ( testIds == null ) {
                testFlags_ = new boolean[ base.getColumnCount() ];
                Arrays.fill( testFlags_, true );
            }
            else {
                testFlags_ = new ColumnIdentifier( base )
                            .getColumnFlags( testIds );
            }
        }

        public boolean isRandom() {
            return false;
        }

        public long getRowCount() {
            return -1L;
        }

        public RowSequence getRowSequence() throws IOException {
            final RowSequence rseq = super.getRowSequence();
            final int ncol = getColumnCount();
            return new RowSequence() {
                boolean started_;
                final Object[] lastRow_ = new Object[ ncol ];
                { Arrays.fill( lastRow_, new Object() ); }

                public boolean next() throws IOException {
                    boolean same = true;
                    Object[] row = null;
                    while ( same && rseq.next() ) {
                        row = rseq.getRow();
                        for ( int icol = 0; same && icol < ncol; icol++ ) {
                            same = same &&
                               ( ( ! testFlags_[ icol ] ) ||
                                 equalValues( row[ icol ], lastRow_[ icol ] ) );
                        }
                    }
                    if ( ! same ) {
                        System.arraycopy( row, 0, lastRow_, 0, ncol );
                    }
                    started_ = true;
                    return ! same;
                }

                public Object[] getRow() {
                    if ( started_ ) {
                        return (Object[]) lastRow_.clone();
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }

                public Object getCell( int icol ) {
                    if ( started_ ) {
                        return lastRow_[ icol ];
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }

                public void close() throws IOException { 
                    rseq.close();
                }
            };
        }
    }

    /**
     * Tests whether two values are equal withing the meaning of the act.
     * Nulls and blank values are treated properly.
     *
     * @param  o1  object 1
     * @param  o2  object 2
     * @return  true iff o1 is equivalent to o2
     */
    private static final boolean equalValues( Object o1, Object o2 ) {
        return ( Tables.isBlank( o1 ) && Tables.isBlank( o2 ) )
            || ( o1 != null && o1.equals( o2 ) );
    }
}
