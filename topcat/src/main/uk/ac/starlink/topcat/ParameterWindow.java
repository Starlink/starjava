package uk.ac.starlink.topcat;

import java.awt.Component;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
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

    private StarTable stable;
    private TableColumnModel tcmodel;
    private AbstractTableModel pmodel;
    private int colsIndex;
    private int rowsIndex;
    private ValueInfoMapGroup mg;
    private Map colsMap;
    private Map rowsMap;

    public ParameterWindow( StarTable stable, TableColumnModel tcmodel,
                            Component parent ) {
        super( "Table Parameters", stable, parent );
        this.stable = stable;
        this.tcmodel = tcmodel;

        /* Construct a MapGroup to hold per-table metadata. */
        mg = new ValueInfoMapGroup();

        /* Add table name if applicable. */
        String name = stable.getName();
        if ( name != null ) {
            Map nameMap = new HashMap();
            nameMap.put( ValueInfoMapGroup.NAME_KEY, "Table name" );
            nameMap.put( ValueInfoMapGroup.VALUE_KEY, name );
            mg.addMap( nameMap );
        }

        /* Add table URL if applicable. */
        URL url = stable.getURL();
        if ( url != null ) {
            Map urlMap = new HashMap();
            urlMap.put( ValueInfoMapGroup.NAME_KEY, "URL" );
            urlMap.put( ValueInfoMapGroup.VALUE_KEY, url );
            mg.addMap( urlMap );
        }

        /* Add table shape. */
        int ncol = stable.getColumnCount();
        colsMap = new HashMap();
        colsMap.put( ValueInfoMapGroup.NAME_KEY, "Column count" );
        colsIndex = mg.getMaps().size();
        mg.addMap( colsMap );
        long nrow = stable.getRowCount();
        rowsMap = new HashMap();
        rowsMap.put( ValueInfoMapGroup.NAME_KEY, "Row count" );
        rowsIndex = mg.getMaps().size();
        mg.addMap( rowsMap );

        /* Add the actual table parameters as such. */
        for ( Iterator it = stable.getParameters().iterator(); 
              it.hasNext(); ) {
            DescribedValue param = (DescribedValue) it.next();
            mg.addDescribedValue( param );
        }

        /* Turn the MapGroup into a JTable. */
        pmodel = new MapGroupTableModel( mg );
        JTable jtab = new JTable( pmodel );
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( false );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

        /* Place the table into a scrollpane in this frame. */
        getMainArea().add( new SizingScrollPane( jtab ) );
        setMainHeading( "Table parameters" );

        /* Ensure that subsequent changes to the table shape are reflected
         * in this window. */
 //     stmodel.addTableModelListener( new TableModelListener() {
 //         public void tableChanged( TableModelEvent evt ) {
 //             configureRowCount();
 //         }
 //     } );

        tcmodel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                configureColumnCount();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                configureColumnCount();
            }
        } );
        configureColumnCount();
        configureRowCount();

        /* Add standard help actions. */
        addHelp( "ParameterWindow" );

        /* Display. */
        pack();
        setVisible( true );
    }

    private void configureColumnCount() {
        int ncol = tcmodel.getColumnCount();
        assert colsMap == mg.getMaps().get( colsIndex );
        assert colsMap.get( ValueInfoMapGroup.NAME_KEY )
                      .equals( "Column count" );
        colsMap.put( ValueInfoMapGroup.VALUE_KEY, new Integer( ncol ) );
        pmodel.fireTableRowsUpdated( colsIndex, colsIndex );
    }

    private void configureRowCount() {
        assert rowsMap == mg.getMaps().get( rowsIndex );
        assert rowsMap.get( ValueInfoMapGroup.NAME_KEY )
                      .equals( "Row count" );
        long nrow = stable.getRowCount();
        rowsMap.put( ValueInfoMapGroup.VALUE_KEY, 
                     ( nrow >= 0 ) ? (Object) new Long( nrow )
                                   : (Object) "?" );
        pmodel.fireTableRowsUpdated( rowsIndex, rowsIndex );
    }

}
