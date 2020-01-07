package uk.ac.starlink.ttools.plot2.task;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.data.AbstractDataSpec;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.UserDataReader;

/**
 * DataSpec implementation that simply selects columns by index from
 * an input table.  The mask is taken to be always true.
 *
 * @author   Mark Taylor
 * @since    25 Sep 2013
 */
public class ColumnDataSpec extends AbstractDataSpec {

    private final StarTable table_;
    private final int nCoord_;
    private final Coord[] coords_;
    private final int[][] userCoordColIndices_;
    private static final String ALL_MASK = "ALL";

    /**
     * Constructor.
     *
     * @param   table  input table
     * @param  coords  coordinate definitions for which columns are required
     * @param  userCoordColIndices  nCoord-element array, each element an
     *                              array of column indices for the
     *                              table columns containing user values
     *                              for the corresponding Coord
     */
    public ColumnDataSpec( StarTable table, Coord[] coords,
                           int[][] userCoordColIndices ) {
        nCoord_ = coords.length;
        if ( userCoordColIndices.length != nCoord_ ) {
            throw new IllegalArgumentException( "coord count mismatch" );
        }
        table_ = table;
        coords_ = coords;
        userCoordColIndices_ = userCoordColIndices;
    }

    public StarTable getSourceTable() {
        return table_;
    }

    public int getCoordCount() {
        return coords_.length;
    }

    public String getCoordId( int ic ) {
        StringBuffer sbuf = new StringBuffer();
        int[] icols = userCoordColIndices_[ ic ];
        for ( int iu = 0; iu < icols.length; iu++ ) {
            if ( iu > 0 ) {
                sbuf.append( "," );
            }
            sbuf.append( Integer.toString( icols[ iu ] ) );
        }
        return sbuf.toString();
    }

    public Coord getCoord( int ic ) {
        return coords_[ ic ];
    }

    public String getMaskId() {
        return ALL_MASK;
    }

    public ValueInfo[] getUserCoordInfos( int ic ) {
        int[] icols = userCoordColIndices_[ ic ];
        ValueInfo[] infos = new ValueInfo[ icols.length ];
        for ( int iu = 0; iu < icols.length; iu++ ) {
            infos[ iu ] = table_.getColumnInfo( icols[ iu ] );
        }
        return infos;
    }

    public UserDataReader createUserDataReader() {
        final Object[][] userRows = new Object[ nCoord_ ][];
        for ( int ic = 0; ic < nCoord_; ic++ ) {
            int[] icols = userCoordColIndices_[ ic ];
            userRows[ ic ] = new Object[ icols.length ];
        }
        return new UserDataReader() {
            public boolean getMaskFlag( RowSequence rseq, long irow ) {
                return true;
            }
            public Object[] getUserCoordValues( RowSequence rseq, long irow,
                                                int icoord )
                    throws IOException {
                Object[] userRow = userRows[ icoord ];
                int[] icols = userCoordColIndices_[ icoord ];
                for ( int iu = 0; iu < userRow.length; iu++ ) {
                    userRow[ iu ] = rseq.getCell( icols[ iu ] );
                }
                return userRow;
            }
        };
    }

    /**
     * Returns true.
     */
    public boolean isMaskTrue() {
        return true;
    }

    public boolean isCoordBlank( int icoord ) {
        return false;
    }
}
