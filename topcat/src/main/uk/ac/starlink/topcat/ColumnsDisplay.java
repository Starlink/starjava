package uk.ac.starlink.topcat;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfoMapGroup;
import uk.ac.starlink.table.gui.MapGroupTableModel;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Little panel for displaying the columns in a startable.
 */
public class ColumnsDisplay extends JPanel {

    private static List keyList = Arrays.asList( new String[] {
        PlasticStarTable.COLID_INFO.getName(),
        ValueInfoMapGroup.NAME_KEY,
        ValueInfoMapGroup.UNITS_KEY,
        SyntheticColumn.EXPR_INFO.getName(),
    } );

    public ColumnsDisplay( StarTable stable ) {
        ValueInfoMapGroup mg = new ValueInfoMapGroup();

        /* Add metadata for the dummy column zero. */
        Map map0 = ValueInfoMapGroup
                  .makeMap( ColumnInfoWindow.dummyIndexColumn() );
        map0.put( ValueInfoMapGroup.INDEX_KEY, new Integer( 0 ) );
        mg.addMap( map0 );

        mg.addTableColumns( stable );
        mg.setKeyOrder( keyList );
        mg.retainKeys( keyList );
        TableModel tmodel = new MapGroupTableModel( mg );
        JTable jtab = new JTable( tmodel );
        for ( int icol = 0; icol < jtab.getColumnCount() - 1; icol++ ) {
            StarJTable.configureColumnWidth( jtab, 300, mg.size(), icol );
        }
        JScrollPane scroller = new JScrollPane( jtab );
        scroller.setPreferredSize( new Dimension( 400, 150 ) );
        add( scroller );
    }
}
