package uk.ac.starlink.treeview;

import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Currently used only to house utility methods generic to 
 * StarTable presentation.
 * May one day come to implement DataNode.
 */
public class StarTableDataNode {

    public static void addDataViews( DetailViewer dv, 
                                     final StarTable startable ) {

        /* Get and display info about the table. */
        int ncols = startable.getColumnCount();
        long nrows = startable.getRowCount();
        dv.addKeyedItem( "Columns", ncols );
        if ( nrows >= 0 ) {
            dv.addKeyedItem( "Rows", nrows );
        }

        int npar = 0;
        final List params = startable.getParameters();
        if ( startable.getParameters().size() > 0 ) {
            dv.addSubHead( "Parameters" );
            for ( Iterator it = startable.getParameters().iterator();
                  it.hasNext(); ) {
                Object item = it.next();
                if ( item instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) item;
                    dv.addKeyedItem( dval.getInfo().getName(), 
                                     dval.getValueAsString( 250 ) );
                    npar++;
                }
            }
        }

        dv.addSubHead( "Columns" );
        for ( int i = 0; i < ncols; i++ ) {
            ColumnInfo info = startable.getColumnInfo( i );
            dv.addKeyedItem( "Column " + ( i + 1 ), info.getName() );
        }
        dv.addPane( "Columns", new ComponentMaker() {
            public JComponent getComponent() {
                MetamapGroup metagroup = new ColumnsMetamapGroup( startable );
                return new MetaTable( metagroup );
            }
        } );
        if ( npar > 0 ) {
            dv.addPane( "Parameters", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup = 
                        new ValueInfoMetamapGroup( params );
                    return new MetaTable( metagroup );
                }
            } );
        }
        dv.addPane( "Table data", new ComponentMaker() {
            public JComponent getComponent() {
                StarJTable sjt = new StarJTable( startable, true );
                sjt.configureColumnWidths( 800, 100 );
                return sjt;
            }
        } );
    }
}
