package uk.ac.starlink.util.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class ArrayTableSorter<R> {

    private final ArrayTableModel<R> model_;
    private final MouseListener mouseListener_;
    private int iSortcol_;
    private boolean descending_;
    private R[] unsortedItems_;

    /**
     * Constructor.
     *
     * @param  model  table model
     */
    public ArrayTableSorter( ArrayTableModel<R> model ) {
        model_ = model;
        unsortedItems_ = model.getItems().clone();
        mouseListener_ = new SortMouseListener();
        model.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                if ( ! equalElements( model_.getItems(), unsortedItems_ ) ) {
                    unsortedItems_ = model_.getItems().clone();
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
        header.setDefaultRenderer( new SortingHeaderRenderer( rend0 ) {
            public int getSortColumnIndex() {
                return iSortcol_;
            }
            public boolean isSortDescending() {
                return descending_;
            }
        } );
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
                ((SortingHeaderRenderer) rend1).getBaseRenderer();
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
        if ( icol >= 0 && icol < model_.getColumnCount() ) {
            model_.sortByColumn( icol, descending );
        }
        else {
            model_.setItems( unsortedItems_.clone() );
        }
    }

    /**
     * Determines whether two arrays have the same elements,
     * regardless of their sequence.
     *
     * @param  items1  first array
     * @param  items2  second array
     * @return  true iff both arrays contain the same elements
     */
    private boolean equalElements( R[] items1, R[] items2 ) {
        List<R> list1 = new ArrayList<R>( Arrays.asList( items1 ) );
        for ( R item2 : items2 ) {
            if ( ! list1.remove( item2 ) ) {
                return false;
            }
        }
        return list1.isEmpty();
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
                    if ( descending_ ) {
                        setSorting( -1, false );
                    }
                    else {
                        setSorting( icol, true );
                    }
                }
                else {
                    setSorting( icol, false );
                }
                header.repaint();
            }
        }
    }
}
