package uk.ac.starlink.table.gui;

import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Provides a component suitable for use as a rowHeader component in
 * the same <tt>JScrollPane</tt> as is being used to house a 
 * <tt>JTable</tt>.  It displays the row indices starting at 1 and increasing.
 * If you want some other number to be displayed, give this object a 
 * new model using {@link javax.swing.JTable#setModel}.  Supplying a 
 * model with more than one column, or which does not supply a 
 * number-like value, is not guaranteed to work properly.
 * <p>
 * You would normally use this class as follows:
 * <pre>
 *     JTable jtab = ...
 *     JScrollPane scrollpane = new JScrollPane( jtab );
 *     scrollpane.setRowHeaderView( new TableRowHeader( jtab ) );
 * </pre>
 *
 * @author   Mark Taylor (Starlink)
 * @see      javax.swing.JScrollPane
 */
public class TableRowHeader extends JTable {

    private JTable table;

    /**
     * Construct a new 
     */
    public TableRowHeader( JTable tabl ) {
        this.table = tabl;

        /* Set the model. */
        setModel( new AbstractTableModel() {
            public int getRowCount() {
                return table.getRowCount();
            }
            public int getColumnCount() {
                return 1;
            }
            public Object getValueAt( int irow, int icol ) {
                return new Integer( irow + 1 ) + "  ";
            }
        } );

        /* Configure to be uninteresting as a JTable. */
        setTableHeader( null );
        setAutoResizeMode( AUTO_RESIZE_OFF );
        setPreferredScrollableViewportSize( table.getPreferredSize() );
        setColumnSelectionAllowed( false );
        setRowSelectionAllowed( false );

        /* Create a suitable renderer. */
        DefaultTableCellRenderer rend = 
            (DefaultTableCellRenderer) new JTableHeader().getDefaultRenderer();
        rend.setFont( UIManager.getFont( "TableHeader.font" ) );
        rend.setBackground( UIManager.getColor( "TableHeader.background" ) );
        rend.setForeground( UIManager.getColor( "TableHeader.foreground" ) );
        rend.setHorizontalAlignment( SwingConstants.RIGHT );

        /* Set up the sole column. */
        TableColumn col = new TableColumn( 0, 64, rend, null ) {
            public int getPreferredWidth() {
                JTable tab = TableRowHeader.this;
                return 8 + 
                    Math.max( StarJTable
                             .getCellWidth( tab, table.getRowCount() - 1, 0 ),
                              StarJTable
                             .getCellWidth( tab, 0, 0 ) );
            }
        };

        /* Configure it into the column model. */
        TableColumnModel tcm = new DefaultTableColumnModel();
        tcm.addColumn( col );
        setColumnModel( tcm );
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

}
