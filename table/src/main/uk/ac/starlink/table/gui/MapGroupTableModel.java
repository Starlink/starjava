package uk.ac.starlink.table.gui;

import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.util.MapGroup;

/**
 * Provides a <code>TableModel</code> view of a <code>MapGroup</code> object.
 * For performance reasons this implementation takes a snapshot of 
 * the MapGroup at construction time rather than treating it as a
 * live object.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MapGroupTableModel<K,V> extends AbstractTableModel {

    private List<Map<K,V>> maps_;
    private List<K> keys_;

    /**
     * Constructs a TableModel from a given MapGroup.
     *
     * @param   mapgroup  group to snapshot
     */
    public MapGroupTableModel( MapGroup<K,V> mapgroup ) {
        maps_ = mapgroup.getMaps();
        keys_ = mapgroup.getKnownKeys();
    }

    public int getRowCount() {
        return maps_.size();
    }

    public int getColumnCount() {
        return keys_.size();
    }

    public V getValueAt( int irow, int icol ) {
        return maps_.get( irow ).get( keys_.get( icol ) );
    }

    public String getColumnName( int icol ) {
        return keys_.get( icol ).toString();
    }

}
