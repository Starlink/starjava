package uk.ac.starlink.datanode.nodes;

import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.gui.MultilineJTable;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Views a group of metadata maps.
 * A number of maps (associative arrays) are displayed in a table, 
 * with one column for each of the keys contained by at least one of
 * the maps.
 */
   public class MetaTable extends MultilineJTable {
// public class MetaTable extends javax.swing.JTable {

    /**
     * Constructs a MetaTable component from a MetamapGroup.
     *
     * @param metagroup  the group to be displayed
     */
    public MetaTable( final MetamapGroup metagroup ) {
        super();
        TableModel baseModel = new AbstractTableModel() {
            private Map[] metamaps = metagroup.getMetamaps();
            private List knownKeys = metagroup.getKnownKeys();
            public int getRowCount() {
                return metamaps.length;
            }
            public int getColumnCount() {
                return knownKeys.size();
            }
            public Object getValueAt( int irow, int icol ) {
                return metamaps[ irow ].get( knownKeys.get( icol ) );
            }
            public String getColumnName( int icol ) {
                return (String) knownKeys.get( icol );
            }
        };
        setModel( baseModel );
        setAutoResizeMode( AUTO_RESIZE_OFF );
        StarJTable.configureColumnWidths( this, 20000, 100 );
    }

}
