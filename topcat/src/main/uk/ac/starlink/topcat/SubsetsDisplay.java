package uk.ac.starlink.topcat;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.gui.MapGroupTableModel;
import uk.ac.starlink.util.MapGroup;

/**
 * Little panel for displaying the available subsets.
 */
public class SubsetsDisplay extends JPanel {

    private static final String ID_KEY = "#ID";
    private static final String NAME_KEY = "Name";
    private static final String EXPR_KEY = "Expression";
    private static final List keyList = Arrays.asList( new String[] {
        ID_KEY, NAME_KEY, EXPR_KEY } );

    public SubsetsDisplay( List subsets ) {
        MapGroup mg = new MapGroup();
        mg.setKeyOrder( keyList );
        int i = 0;
        for ( Iterator it = subsets.iterator(); it.hasNext(); ) {
            RowSubset rset = (RowSubset) it.next();
            Map map = new HashMap();
            map.put( ID_KEY, "#" + ++i );
            map.put( NAME_KEY, rset.getName() );
            if ( rset instanceof SyntheticRowSubset ) {
                map.put( EXPR_KEY, 
                         ((SyntheticRowSubset) rset).getExpression() );
            }
            mg.addMap( map );
        }
        TableModel tmodel = new MapGroupTableModel( mg );
        JTable jtab = new JTable( tmodel );
        JScrollPane scroller = new JScrollPane( jtab );
        scroller.setPreferredSize( new Dimension( 400, 150 ) );
        add( scroller );
    }

}
