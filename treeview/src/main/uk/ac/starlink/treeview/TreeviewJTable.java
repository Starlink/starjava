package uk.ac.starlink.treeview;

import javax.swing.table.TableCellRenderer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Extends StarJTable to do custom rendering of NDArrays.
 */
public class TreeviewJTable extends StarJTable {

    private static TableCellRenderer tvrend = new TreeviewCellRenderer();

    public TreeviewJTable( StarTable startable ) {
        super( startable );
        setDefaultRenderer( Object.class, tvrend );
        setDefaultRenderer( Number.class, tvrend );
        setDefaultRenderer( Double.class, tvrend );
        configureColumnWidths( 500 );
    }
}
