package uk.ac.starlink.table.view;

import java.awt.Component;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfoMapGroup;
import uk.ac.starlink.table.gui.MapGroupTableModel;
import uk.ac.starlink.table.gui.MultilineJTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.MapGroup;

/**
 * Top-level window which displays the parameters of a table.
 * Other per-table metadata may be displayed as well.
 */
public class ParameterWindow extends AuxWindow {

    public ParameterWindow( StarTable startab, Component parent ) {
        super( "Table Parameters", startab, parent );

        /* Construct a MapGroup to hold per-table metadata. */
        ValueInfoMapGroup mg = new ValueInfoMapGroup();

        /* Add table name if applicable. */
        String name = startab.getName();
        if ( name != null ) {
            Map nameMap = new HashMap();
            nameMap.put( ValueInfoMapGroup.NAME_KEY, "Table name" );
            nameMap.put( ValueInfoMapGroup.VALUE_KEY, name );
            mg.addMap( nameMap );
        }

        /* Add table URL if applicable. */
        URL url = startab.getURL();
        if ( url != null ) {
            Map urlMap = new HashMap();
            urlMap.put( ValueInfoMapGroup.NAME_KEY, "URL" );
            urlMap.put( ValueInfoMapGroup.VALUE_KEY, url );
            mg.addMap( urlMap );
        }

        /* Add table shape. */
        int ncol = startab.getColumnCount();
        Map colsMap = new HashMap();
        colsMap.put( ValueInfoMapGroup.NAME_KEY, "Column count" );
        colsMap.put( ValueInfoMapGroup.VALUE_KEY, new Integer( ncol ) );
        mg.addMap( colsMap );
        long nrow = startab.getRowCount();
        if ( nrow >= 0 ) {
            Map rowsMap = new HashMap();
            rowsMap.put( ValueInfoMapGroup.NAME_KEY, "Row count" );
            rowsMap.put( ValueInfoMapGroup.VALUE_KEY, new Long( nrow ) );
            mg.addMap( rowsMap );
        }

        /* Add the actual table parameters as such. */
        for ( Iterator it = startab.getParameters().iterator(); 
              it.hasNext(); ) {
            DescribedValue param = (DescribedValue) it.next();
            mg.addDescribedValue( param );
        }

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
