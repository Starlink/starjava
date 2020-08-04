package uk.ac.starlink.ttools.plot2.task;

import java.io.IOException;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowData;
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
    private final DomainMapper[][] userCoordMappers_;
    private static final String ALL_MASK = "ALL";

    /**
     * Constructs a ColumnDataSpec with default mappers.
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
        this( table, coords, userCoordColIndices, null );
    }

    /**
     * Constructs a ColumnDataSpec with supplied mappers.
     *
     * @param   table  input table
     * @param  coords  coordinate definitions for which columns are required
     * @param  userCoordColIndices  nCoord-element array, each element an
     *                              array of column indices for the
     *                              table columns containing user values
     *                              for the corresponding Coord
     * @param  userCoordMappers   nCoord-element array, each element an
     *                            array of domain mappers for the
     *                            table columns containing domain mappers
     *                            for the corresponding Coord
     */
    public ColumnDataSpec( StarTable table, Coord[] coords,
                           int[][] userCoordColIndices,
                           DomainMapper[][] userCoordMappers ) {
        nCoord_ = coords.length;
        table_ = table;
        coords_ = coords;
        userCoordColIndices_ = userCoordColIndices;

        /* Ensure we have DomainMappers in place. */
        userCoordMappers_ = new DomainMapper[ nCoord_ ][];
        for ( int ic = 0; ic < nCoord_; ic++ ) {
            final DomainMapper[] dms;

            /* If the user has supplied mappers, make a deep copy of
             * the list. */
            if ( userCoordMappers[ ic ] != null ) {
                dms = userCoordMappers[ ic ].clone();
            }

            /* Otherwise, use best guess values for each input. */
            else {
                int[] icols = userCoordColIndices[ ic ];
                int nuc = icols.length;
                dms = new DomainMapper[ nuc ];
                for ( int iuc = 0; iuc < nuc; iuc++ ) {
                    ValueInfo info = table.getColumnInfo( icols[ iuc ] );
                    Domain<?> domain =
                        coords[ ic ].getInputs()[ iuc ].getDomain();
                    dms[ iuc ] = domain.getProbableMapper( info );
                    if ( dms[ iuc ] == null ) {
                        dms[ iuc ] = domain.getPossibleMapper( info );
                    }
                }
            }
            userCoordMappers_[ ic ] = dms;
        }

        /* Check consistency. */
        if ( userCoordColIndices.length != nCoord_ ||
             userCoordMappers.length != nCoord_ ) {
            throw new IllegalArgumentException( "coord count mismatch" );
        }
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
        DomainMapper[] dms = userCoordMappers_[ ic ];
        for ( int iu = 0; iu < icols.length; iu++ ) {
            if ( iu > 0 ) {
                sbuf.append( "," );
            }
            sbuf.append( Integer.toString( icols[ iu ] ) );
            if ( dms != null && dms[ iu ] != null ) {
                sbuf.append( "|" )
                    .append( dms[ iu ].getSourceName() );
            }
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

    public DomainMapper[] getUserCoordMappers( int ic ) {
         return userCoordMappers_[ ic ];
    }

    public UserDataReader createUserDataReader() {
        final Object[][] userRows = new Object[ nCoord_ ][];
        for ( int ic = 0; ic < nCoord_; ic++ ) {
            int[] icols = userCoordColIndices_[ ic ];
            userRows[ ic ] = new Object[ icols.length ];
        }
        return new UserDataReader() {
            public boolean getMaskFlag( RowData rdata, long irow ) {
                return true;
            }
            public Object[] getUserCoordValues( RowData rdata, long irow,
                                                int icoord )
                    throws IOException {
                Object[] userRow = userRows[ icoord ];
                int[] icols = userCoordColIndices_[ icoord ];
                for ( int iu = 0; iu < userRow.length; iu++ ) {
                    userRow[ iu ] = rdata.getCell( icols[ iu ] );
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
