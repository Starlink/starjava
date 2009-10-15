package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Handles GUI aspects of allowing the user to sort columns in a table
 * by clicking on the column header.
 * After construction you have to install it on a suitable table header.
 * The table must be represented by a {@link ArrayTableModel}.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2009
 */
public class ArrayTableSorter {

    private final ArrayTableModel model_;
    private final MouseListener mouseListener_;
    private int iSortcol_;
    private boolean descending_;

    /**
     * Constructor.
     *
     * @param  model  table model
     */
    public ArrayTableSorter( ArrayTableModel model ) {
        model_ = model;
        mouseListener_ = new SortMouseListener();
        model.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                if ( iSortcol_ >= 0 ) {
                    final int iSortcol = iSortcol_;
                    final boolean descending = descending_;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            setSorting( iSortcol, descending );
                        }
                    } );
                }
            }
        } );
        iSortcol_ = -1;
    }

    /**
     * Sets this object up to allow sorting its model by clicking on a
     * given JTable header.
     *
     * @param  header   table header component
     */
    public void install( JTableHeader header ) {
        TableCellRenderer rend0 = header.getDefaultRenderer();
        header.setDefaultRenderer( new SortingHeaderRenderer( rend0 ) );
        header.addMouseListener( mouseListener_ );
    }

    /**
     * Reverses the action of a previous {@link #install} action.
     *
     * @param  header   table header component
     */
    public void uninstall( JTableHeader header ) {
        TableCellRenderer rend1 = header.getDefaultRenderer();
        if ( rend1 instanceof SortingHeaderRenderer ) {
            TableCellRenderer rend0 =
                ((SortingHeaderRenderer) rend1).baseRenderer_;
            header.setDefaultRenderer( rend0 );
        }
        header.removeMouseListener( mouseListener_ );
    }

    /**
     * Performs a sort on a given column.
     * Note, the header should be repainted following a call to this method.
     *
     * @param   icol  column index
     * @param   descending  true sort down, false sort up
     */
    public void setSorting( int icol, boolean descending ) {
        iSortcol_ = icol;
        descending_ = descending;
        model_.sortByColumn( icol, descending );
    }

    /**
     * Mouse listener to be installed on the table header which arranges
     * to sort by columns according to clicks.
     */
    private class SortMouseListener extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            JTableHeader header = (JTableHeader) evt.getSource();
            TableColumnModel colModel = header.getColumnModel();
            int iViewcol = header.columnAtPoint( evt.getPoint() );
            int icol = colModel.getColumn( iViewcol ).getModelIndex();
            if ( icol > -1 ) {
                if ( iSortcol_ == icol ) {
                    setSorting( icol, ! descending_ );
                }
                else {
                    setSorting( icol, false );
                }
                header.repaint();
            }
        }
    }

    /**
     * Renderer for table header which paints an indication of which
     * column is controlling the table data sort.
     */
    private class SortingHeaderRenderer implements TableCellRenderer {

        private final TableCellRenderer baseRenderer_;

        /**
         * Constructor.
         *
         * @param  baseRenderer   renderer doing basic header display
         */
        public SortingHeaderRenderer( TableCellRenderer baseRenderer ) {
            baseRenderer_ = baseRenderer;
        }

        public Component getTableCellRendererComponent( JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int irow, int icol ) {
            Component comp = baseRenderer_
                .getTableCellRendererComponent( table, value, isSelected,
                                                hasFocus, irow, icol );
            if ( comp instanceof JLabel ) {
                JLabel label = (JLabel) comp;
                label. setHorizontalTextPosition( JLabel.RIGHT );
                int iModelcol = table.convertColumnIndexToModel( icol );
                Icon icon = iModelcol == iSortcol_
                          ? new ArrowIcon( descending_,
                                           label.getFont().getSize() )
                          : null;
                label.setIcon( icon );
            }
            return comp;
        }
    }

    /**
     * Paints a little up or down arrow.
     * Code largely pinched from
     * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TableSorterDemoProject/src/components/TableSorter.java.
     */
    private static class ArrowIcon implements Icon {
        private final boolean descending_;
        private final int size_;

        /**
         * Constructor.
         *
         * @param   descending  false for up, true for down
         * @param   size  font size in pixels
         */
        ArrowIcon( boolean descending, int size ) {
            descending_ = descending;
            size_ = size;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            int dx = ( size_ + 1 ) / 2;
            int dy = descending_ ? dx : -dx;
            y += 5 * size_ / 6 + ( descending_ ? -dy : 0 );
            int shift = descending_ ? 1 : -1;
            g.translate( x, y );
            g.drawLine( dx / 2, dy, 0, 0 );
            g.drawLine( dx / 2, dy + shift, 0, shift );
            g.drawLine( dx / 2, dy, dx, 0 );
            g.drawLine( dx / 2, dy + shift, dx, shift );
            g.drawLine( dx, 0, 0, 0 );
            g.translate( -x, -y );
        }

        public int getIconWidth() {
            return size_;
        }

        public int getIconHeight() {
            return size_;
        }
    }
}
