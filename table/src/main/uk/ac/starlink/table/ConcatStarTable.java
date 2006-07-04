package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * StarTable implementation which concatenates several tables together 
 * top-to-bottom, making some effort to check that the concatenated
 * columns are compatible with each other.
 *
 * <p>Column contents from each table are only added if their contents
 * are compatible with the column in question.  Some attempt is made to
 * guess which column can match up with which - this isn't very sophisticated,
 * so if the columns are not compatible it's likely you'll end up with
 * a lot of null values.  However, the result should at least be a legal
 * <code>StarTable</code> instance.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class ConcatStarTable extends AbstractStarTable {

    private final StarTable[] tables_;
    private final int[][] colMaps_;
    private final ColumnInfo[] colInfos_;
    private final boolean isRandom_;

    private static final RowSequence EMPTY_SEQUENCE = new EmptyRowSequence();

    /**
     * Constructs a new concatenated table.
     *
     * @param   tables   array of constituent tables
     */
    public ConcatStarTable( StarTable[] tables ) {

        /* Get array of column metadata objects from the first table in the
         * input array. */
        tables_ = tables;
        StarTable table0 = tables[ 0 ];
        ColumnInfo[] colInfos = new ColumnInfo[ table0.getColumnCount() ];
        int ncol = colInfos.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            colInfos[ icol ] = new ColumnInfo( table0.getColumnInfo( icol ) );
        }

        /* Permute the columns of all the following tables if necessary
         * to match those in the first table.  The algorithm is rather
         * unsophisticated - if subsequent tables don't have columns in
         * an order which matches those of the first one, you'll probably
         * get a lot of empty columns.  However, the table should at least
         * obey the StarTable contract correctly. */
        int nTable = tables.length;
        colMaps_ = new int[ nTable ][];
        for ( int itab = 0; itab < nTable; itab++ ) {
            colMaps_[ itab ] = new int[ ncol ];
            Arrays.fill( colMaps_[ itab ], -1 );
            StarTable table = tables[ itab ];
            int jcol = 0;
            for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
                ColumnInfo info = table.getColumnInfo( icol );
                while ( jcol < ncol && ! matches( colInfos[ jcol ], info ) ) {
                    jcol++;
                }
                if ( jcol < ncol ) {
                    colMaps_[ itab ][ jcol ] = icol;
                }
            }
        }

        /* Adjust each column metadata object to cover all the objects which
         * might appear in that column (may need to be generalised since
         * it's holding items from tables with non-identical per-column
         * metadata). */
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info0 = colInfos[ icol ];
            Class clazz0 = info0.getContentClass();
            int[] shape0 = info0.getShape();
            int esize0 = info0.getElementSize();
            for ( int itab = 0; itab < nTable; itab++ ) {
                int jcol = colMaps_[ itab ][ icol ];
                if ( jcol >= 0 ) {
                    ColumnInfo info1 = tables_[ itab ].getColumnInfo( jcol );
                    Class clazz1 = info1.getContentClass();
                    int[] shape1 = info1.getShape();
                    int esize1 = info1.getElementSize();
                    while ( ! clazz0.isAssignableFrom( clazz1 ) ) {
                        clazz0 = clazz0.getSuperclass();
                        if ( clazz0 == null ) {
                            clazz0 = Object.class;
                        }
                    }
                    if ( info0.isArray() ) {
                        int ndim = shape0.length;
                        if ( shape1.length != ndim ) {
                            shape0 = new int[] { -1 };
                        }
                        else if ( shape1[ ndim - 1 ] != shape0[ ndim - 1 ] ) {
                            shape0[ ndim - 1 ] = -1;
                        }
                    }
                    if ( esize1 != esize0 ) {
                        esize0 = -1;
                    }
                    if ( info1.isNullable() ) {
                        info0.setNullable( true );
                    }
                }
                else {
                    info0.setNullable( true );
                }
            }
            if ( clazz0 != info0.getContentClass() ) {
                info0.setContentClass( clazz0 );
            }
            if ( info0.isArray() &&
                 ! Arrays.equals( shape0, info0.getShape() ) ) {
                info0.setShape( shape0 );
            }
            if ( esize0 != info0.getElementSize() ) {
                info0.setElementSize( esize0 );
            }
        }
        colInfos_ = colInfos;

        /* Determine whether we have random access. */
        boolean rand = true;
        for ( int itab = 0; rand && itab < nTable; itab++ ) {
            rand = rand && tables_[ itab ].isRandom();
        }
        isRandom_ = rand;
    }

    /**
     * Used to determine whether two column metadata objects are sufficiently
     * similar to count as a match.
     * The <code>ConcatStarTable</code> implementation of this method
     * tests compatible content class and array-ness.  
     * It may be overridden to for instance to check also for UCD or
     * column name.
     *
     * @param  info0  base metadata object
     * @param  info1  metadata object for comparison
     * @return  true iff objects in the column described by <code>info1</code>
     *          should be allowed in the column described by <code>info0</code>
     */
    public boolean matches( ColumnInfo info0, ColumnInfo info1 ) {
        return info0.getContentClass()
                    .isAssignableFrom( info1.getContentClass() )
            && info0.isArray() == info1.isArray();
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public long getRowCount() {
        long nrow = 0;
        for ( int itab = 0; itab < tables_.length; itab++ ) {
            long nr = tables_[ itab ].getRowCount();
            if ( nr < 0 ) {
                return -1L;
            }
            else {
                nrow += nr;
            }
        }
        return nrow;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( isRandom_ ) {
            for ( int itab = 0; itab < tables_.length; itab++ ) {
                StarTable t = tables_[ itab ];
                long nr = t.getRowCount();
                if ( irow < nr ) {
                    int jcol = colMaps_[ itab ][ icol ];
                    return jcol >= 0 ? t.getCell( irow, jcol )
                                     : null;
                }
                irow -= nr;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        else {
            throw new UnsupportedOperationException( "No random access" );
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( isRandom_ ) {
            for ( int itab = 0; itab < tables_.length; itab++ ) {
                StarTable t = tables_[ itab ];
                long nr = t.getRowCount();
                if ( irow < nr ) {
                    Object[] inRow = t.getRow( irow );
                    int ncol = colInfos_.length;
                    Object[] outRow = new Object[ ncol ];
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        int jcol = colMaps_[ itab ][ icol ];
                        outRow[ icol ] = jcol >= 0 ? inRow[ jcol ]
                                                   : null;
                    }
                    return outRow;
                }
                irow -= nr;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        else {
            throw new UnsupportedOperationException( "No random access" );
        }
    }

    public boolean isRandom() {
        return isRandom_;
    }

    public RowSequence getRowSequence() throws IOException {
        return new ConcatRowSequence();
    }

    private class ConcatRowSequence implements RowSequence {
        final Iterator tabIt_ = Arrays.asList( tables_ ).iterator();
        final Iterator mapIt_ = Arrays.asList( colMaps_ ).iterator();
        RowSequence rseq_ = EMPTY_SEQUENCE;
        int[] colMap_ = new int[ colInfos_.length ];

        public boolean next() throws IOException {
            while ( ! rseq_.next() ) {
                if ( rseq_ != EMPTY_SEQUENCE ) {
                    rseq_.close();
                }
                if ( tabIt_.hasNext() ) {
                    assert mapIt_.hasNext();
                    rseq_ = ((StarTable) tabIt_.next()).getRowSequence();
                    colMap_ = (int[]) mapIt_.next();
                }
                else {
                    return false;
                }
            }
            return true;
        }

        public void close() throws IOException {
            rseq_.close();
        }

        public Object getCell( int icol ) throws IOException {
            int jcol = colMap_[ icol ];
            return jcol >= 0 ? rseq_.getCell( jcol )
                             : null;
        }

        public Object[] getRow() throws IOException {
            Object[] inRow = rseq_.getRow();
            int ncol = colInfos_.length;
            Object[] outRow = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                int jcol = colMap_[ icol ];
                outRow[ icol ] = jcol >= 0 ? inRow[ jcol ]
                                           : null;
            }
            return outRow;
        }
    }
}
