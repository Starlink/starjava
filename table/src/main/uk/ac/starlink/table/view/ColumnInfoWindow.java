package uk.ac.starlink.table.view;

import java.awt.Component;
import java.util.Map;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfoMapGroup;
import uk.ac.starlink.table.gui.MapGroupTableModel;
import uk.ac.starlink.table.gui.MultilineJTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.MapGroup;

/**
 * Top-level window which displays per-column metadata for a table.
 */
public class ColumnInfoWindow extends AuxWindow {

    private JTable jtab;
    private PlasticStarTable dataModel;
    private TableColumnModel columnModel;

    private static final String COLID_KEY = "$ID";

    public ColumnInfoWindow( PlasticStarTable dataModel, 
                             TableColumnModel columnModel, Component parent ) {
        super( "Table Columns", dataModel, parent );
        this.dataModel = dataModel;
        this.columnModel = columnModel;

        /* Construct a JTable and place it in this component. */
        jtab = new MultilineJTable();
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( false );

        /* Place the table into a scrollpane in this frame. */
        getContentPane().add( new SizingScrollPane( jtab ) );

        /* Set up the table to display current state. */
        reconfigure();

        /* Ensure that subsequent changes to the column model are reflected
         * in this window.  A more efficient implementation would be 
         * possible if each separate event (column add/move/remove) were
         * dealt with specially, but it's not worth the additional effort. */
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                reconfigure();
            }
            public void columnMoved( TableColumnModelEvent evt ) {
                reconfigure();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                reconfigure();
            }
        } );

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Configure the JTable displayed in this window to reflect the current
     * state of the column and data models that it is reporting on.
     */
    private void reconfigure() {

        /* Set up a MapGroup to handle the column metadata in this table. */
        ValueInfoMapGroup mg = new ValueInfoMapGroup();
        mg.getKeyOrder().add( 1, COLID_KEY );
        mg.addColumnAuxDataKeys( dataModel );

        /* Add the metadata for each of the columns. */
        int ncol = columnModel.getColumnCount();
        for ( int i = 0; i < ncol; i++ ) {
             int index = i + 1;
             int modelIndex = columnModel.getColumn( i ).getModelIndex();
             Map map = ValueInfoMapGroup
                      .makeMap( dataModel.getColumnInfo( modelIndex ) );
             map.put( ValueInfoMapGroup.INDEX_KEY, new Integer( index ) );
             map.put( COLID_KEY, "$" + ( modelIndex + 1 ) );
             mg.addMap( map );
        }
           
        /* Set our JTable to display column data based on this. */
        jtab.setModel( new MapGroupTableModel( mg ) );

        /* Configure the column widths. */
        StarJTable.configureColumnWidths( jtab, 20000, 100 );
    }

}
