package uk.ac.starlink.ttools.filter;

import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.MapGroup;

/**
 * StarTable implementation built on a MapGroup whose keys are 
 * ValueInfo objects representing columns of the table.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class ValueInfoMapGroupTable extends RandomStarTable {

    private final ValueInfo[] keys_;
    private final Map[] maps_;

    /**
     * Constructor.
     *
     * @param   group   map group containing table data and metadata; 
     *          elements of group.getKnownKeys() must all be 
     *          {@link uk.ac.starlink.table.ValueInfo} objects
     */
    ValueInfoMapGroupTable( MapGroup group ) {
        maps_ = (Map[]) group.getMaps().toArray( new Map[ 0 ] );
        keys_ = (ValueInfo[]) group.getKnownKeys()
                                   .toArray( new ValueInfo[ 0 ] );
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return new ColumnInfo( keys_[ icol ] );
    }

    public long getRowCount() {
        return maps_.length;
    }

    public int getColumnCount() {
        return keys_.length;
    }

    public Object getCell( long irow, int icol ) {
        return maps_[ checkedLongToInt( irow ) ].get( keys_[ icol ] );
    }

    public Object[] getRow( long irow ) {
        Map map = maps_[ checkedLongToInt( irow ) ];
        int ncol = keys_.length;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = map.get( keys_[ icol ] );
        }
        return row;
    }
}
