package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filters out rows which are identical in some or all columns.
 *
 * @author   Mark Taylor
 * @since    27 Apr 2006
 */
public class UniqueFilter extends BasicFilter {

    private static final ValueInfo COUNT_INFO =
        new DefaultValueInfo( "DupCount", Integer.class,
                              "Number of duplicate rows" );

    public UniqueFilter() {
        super( "uniq", "[-count] [<colid-list>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Eliminates adjacent rows which have the same values.",
            "If used with no arguments, then any row which has identical",
            "values to its predecessor is removed.",
            "</p>",
            "<p>If the <code>&lt;colid-list&gt;</code> parameter is given",
            "then only the values in the specified columns must be equal",
            "in order for the row to be removed.",
            "</p>",
            "<p>If the <code>-count</code> flag is given, then an additional",
            "column with the name " + COUNT_INFO.getName() + " will be",
            "prepended to the table giving a count of the number of duplicated",
            "input rows represented by each output row.  A unique row",
            "has a " + COUNT_INFO.getName() + " value of 1.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt ) {
        String testIds = null;
        boolean count = false;
        while ( argIt.hasNext() && testIds == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-count" ) ) {
                argIt.remove();
                count = true;
            }
            else {
                argIt.remove();
                testIds = arg;
            }
        }
        final String tids = testIds;
        final boolean doCount = count;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                return new UniqueTable( base, tids, doCount );
            }
        };
    }

    private static class UniqueTable extends WrapperStarTable {

        final boolean[] testFlags_;
        final boolean doCount_;

        UniqueTable( StarTable base, String testIds, boolean doCount )
                throws IOException {
            super( base );
            if ( testIds == null ) {
                testFlags_ = new boolean[ base.getColumnCount() ];
                Arrays.fill( testFlags_, true );
            }
            else {
                testFlags_ = new ColumnIdentifier( base )
                            .getColumnFlags( testIds );
            }
            doCount_ = doCount;
        }

        public boolean isRandom() {
            return false;
        }

        public int getColumnCount() {
            return super.getColumnCount() + ( doCount_ ? 1 : 0 );
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return ( icol == 0 && doCount_ )
                 ? new ColumnInfo( COUNT_INFO )
                 : super.getColumnInfo( icol - ( doCount_ ? 1 : 0 ) );
        }

        public long getRowCount() {
            return -1L;
        }

        public RowSequence getRowSequence() throws IOException {
            final RowSequence rseq = super.getRowSequence();
            final int ncol = super.getColumnCount();
            return new RowSequence() {
                Object[] lastRow_;
                Object[] nextRow_;

                /* Constructor. */ {
                    if ( rseq.next() ) {
                        nextRow_ = rseq.getRow().clone();
                    }
                }

                public boolean next() throws IOException {
                    if ( nextRow_ == null ) {
                        return false;
                    }
                    int dupCount = 1;
                    boolean same = true;
                    Object[] row = null;
                    while ( same && rseq.next() ) {
                        row = rseq.getRow();
                        for ( int icol = 0; same && icol < ncol; icol++ ) {
                            same = same &&
                                ( ( ! testFlags_[ icol ] ) ||
                                  equalValues( row[ icol ],
                                               nextRow_[ icol ] ) );
                        }
                        if ( same ) {
                            dupCount++;
                        }
                    }
                    lastRow_ = new Object[ ncol + ( doCount_ ? 1 : 0 ) ];
                    System.arraycopy( nextRow_, 0,
                                      lastRow_, ( doCount_ ? 1 : 0 ), ncol );
                    if ( doCount_ ) {
                        lastRow_[ 0 ] = Integer.valueOf( dupCount );
                    }
                    nextRow_ = same ? null : row.clone();
                    return true;
                }

                public Object[] getRow() {
                    if ( lastRow_ != null ) {
                        return lastRow_;
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }

                public Object getCell( int icol ) {
                    if ( lastRow_ != null ) {
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

        public RowAccess getRowAccess() {
            throw new UnsupportedOperationException( "not random" );
        }

        public RowSplittable getRowSplittable() throws IOException {
            return Tables.getDefaultRowSplittable( this );
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
