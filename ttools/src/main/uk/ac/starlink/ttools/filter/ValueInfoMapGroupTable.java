package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
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
     * The <code>itemNames</code> parameter may optionally contain a list
     * of strings which correspond to the names of mapgroup keys.
     * Names which don't appear as a key are given a new string-typed
     * ValueInfo (the contents of the column are all nulls).
     * The columns of the table will be the same in number and name as
     * the supplied <code>itemNames</code> array.
     * If <code>itemNames</code> is null then the columns will be determined
     * by the (non-empty) contents of the mapgroup.
     *
     * @param   group   map group containing table data and metadata; 
     *          elements of group.getKnownKeys() must all be 
     *          {@link uk.ac.starlink.table.ValueInfo} objects
     * @param   itemNames   column names of the resulting table, or null
     */
    ValueInfoMapGroupTable( MapGroup group, String[] itemNames ) {
        maps_ = (Map[]) group.getMaps().toArray( new Map[ 0 ] );

        /* For a null list of names, ascertain the columns from the non-empty
         * known keys of the map group. */
        if ( itemNames == null ) {
            List keyList = new ArrayList();
            for ( Iterator it = group.getKnownKeys().iterator();
                  it.hasNext(); ) {
                ValueInfo info = (ValueInfo) it.next();
                boolean hasSome = false;
                for ( int imap = 0; ! hasSome && imap < maps_.length;
                      imap++ ) {
                    hasSome =
                        hasSome ||
                        ( ! Tables.isBlank( maps_[ imap ].get( info ) ) );
                }
                if ( hasSome ) {
                    keyList.add( info );
                }
            }
            keys_ = (ValueInfo[]) keyList.toArray( new ValueInfo[ 0 ] );
        }

        /* For a non-null list of names, construct the list of columns
         * accordingly. */
        else {
            keys_ = new ValueInfo[ itemNames.length ];
            for ( int i = 0; i < itemNames.length; i++ ) {
                String item = itemNames[ i ];
                ValueInfo itemInfo = null;

                /* Try to find a ValueInfo in the group keys which corresponds
                 * to the given item name. */
                for ( Iterator it = group.getKnownKeys().iterator();
                      it.hasNext() && itemInfo == null; ) {
                    ValueInfo info = (ValueInfo) it.next();
                    if ( info.getName().equalsIgnoreCase( item ) ) {
                        itemInfo = info;
                    }
                }

                /* If there isn't one, fake an empty column with the right
                 * name. */
                if ( itemInfo == null ) {
                    itemInfo = new DefaultValueInfo( item, String.class );
                }
                keys_[ i ] = itemInfo;
            }
        }
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
