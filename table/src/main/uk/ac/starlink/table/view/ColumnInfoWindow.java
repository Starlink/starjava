package uk.ac.starlink.table.view;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfoMapGroup;
import uk.ac.starlink.table.gui.MapGroupTableModel;
import uk.ac.starlink.table.gui.MultilineJTable;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Top-level window which displays per-column metadata for a table.
 */
public class ColumnInfoWindow extends AuxWindow {

    public ColumnInfoWindow( StarTable startab, Component parent ) {
        super( "Table Columns", startab, parent );

        /* Construct a MapGroup to hold column metadata. */
        ValueInfoMapGroup mg = new ValueInfoMapGroup();
        mg.addTableColumns( startab );

        /* Turn the MapGroup into a JTable. */
        JTable jtab = new MultilineJTable( new MapGroupTableModel( mg ) );
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

        /* Place the table into a scrollpane in this frame. */
        getContentPane().add( new SizingScrollPane( jtab ) );

        /* Display. */
        pack();
        setVisible( true );
    }

}
