package uk.ac.starlink.table.gui;

import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.util.MapGroup;

/**
 * Provides a <tt>TableModel</tt> view of a <tt>MapGroup</tt> object.
 * For performance reasons this implementation takes a snapshot of 
 * the MapGroup at construction time rather than treating it as a
 * live object.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MapGroupTableModel extends AbstractTableModel {

    private List maps;
    private List keys;

    /**
     * Constructs a TableModel from a given MapGroup.
     *
     * @param   mapgroup  group to snapshot
     */
    public MapGroupTableModel( MapGroup mapgroup ) {
        maps = mapgroup.getMaps();
        keys = mapgroup.getKnownKeys();
    }

    public int getRowCount() {
        return maps.size();
    }

    public int getColumnCount() {
        return keys.size();
    }

    public Object getValueAt( int irow, int icol ) {
        return ((Map) maps.get( irow )).get( keys.get( icol ) );
    }

    public String getColumnName( int icol ) {
        return keys.get( icol ).toString();
    }

}
