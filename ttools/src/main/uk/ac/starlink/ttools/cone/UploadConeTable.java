package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.UnrepeatableSequenceException;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table suitable for uploading based on a sequence of positional queries
 * and an RowMapper.
 * The resulting table contains just three columns: ID, RA, Dec.
 *
 * <p>This is a one-shot sequential table - only one row sequence
 * may be taken out from it.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2014
 */
public class UploadConeTable extends AbstractStarTable {

    private ConeQueryRowSequence coneSeq_;
    private final RowMapper<?> rowMapper_;
    private final ColumnInfo[] colInfos_;

    /**
     * Constructor.
     *
     * @param  coneSeq  sequence of positional queries
     * @param  rowMapper  maps index of query to an identifier object
     * @param  idName  column name for uploaded ID column
     * @param  raName  column name for uploaded RA column
     * @param  decName  column name for uploaded Dec column
     */
    public UploadConeTable( ConeQueryRowSequence coneSeq,
                            RowMapper<?> rowMapper,
                            String idName, String raName, String decName ) {
        coneSeq_ = coneSeq;
        rowMapper_ = rowMapper;
        colInfos_ = new ColumnInfo[] {
            new ColumnInfo( idName, rowMapper_.getIdClass(), "Row identifier" ),
            new ColumnInfo( raName, Double.class, "ICRS Right Ascension" ),
            new ColumnInfo( decName, Double.class, "ICRS Declination" ),
        };
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public long getRowCount() {
        return -1;
    }

    public synchronized RowSequence getRowSequence() throws IOException {
        if ( coneSeq_ == null ) {
            throw new UnrepeatableSequenceException();
        }
        final ConeQueryRowSequence coneSeq = coneSeq_;
        coneSeq_ = null;
        return new RowSequence() {
            private Object[] row_;
            public boolean next() throws IOException {
                boolean hasNext = coneSeq.next();
                row_ = hasNext
                     ? new Object[] {
                           rowMapper_.rowIndexToId( coneSeq.getIndex() ),
                           Double.valueOf( coneSeq.getRa() ),
                           Double.valueOf( coneSeq.getDec() ),
                       }
                     : null;
                assert ! hasNext || isRowCompatible( row_, colInfos_ );
                return hasNext;
            }
            public Object[] getRow() {
                return row_;
            }
            public Object getCell( int icol ) {
                return row_[ icol ];
            }
            public void close() throws IOException {
                coneSeq.close();
            }
        };
    }

    @Override
    public void close() throws IOException {
        coneSeq_.close();
    }

    /**
     * Determines whether the contents of a given row are
     * compatible with a given list of column metadata objects.
     * Used for assertions.
     *
     * @param  row  tuple of values
     * @param  infos  matching tuple of value metadata objects
     * @return  true iff compatible
     */
    private boolean isRowCompatible( Object[] row, ValueInfo[] infos ) {
        int n = row.length;
        if ( infos.length != n ) {
            return false;
        }
        for ( int i = 0; i < n; i++ ) {
            Object cell = row[ i ];
            if ( cell != null &&
                 ! infos[ i ].getContentClass()
                  .isAssignableFrom( cell.getClass() ) ) {
                return false;
            }
        }
        return true;
    }
}
