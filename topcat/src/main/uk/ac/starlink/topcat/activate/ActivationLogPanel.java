package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Displays the results of activation actions.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class ActivationLogPanel extends JPanel {

    private final int maxItems_;
    private final LogTableModel tableModel_;
    private final JTable jtable_;
    private final JScrollPane scroller_;
    private int iseq_;

    /**
     * Constructor.
     *
     * @param  maxItems  largest number of results permitted;
     *                   if more are submitted, the oldest ones will be
     *                   removed
     */
    @SuppressWarnings("this-escape")
    public ActivationLogPanel( int maxItems ) {
        super( new BorderLayout() );
        maxItems_ = maxItems;
        tableModel_ = new LogTableModel();
        jtable_ = new JTable( tableModel_ );
        jtable_.setShowHorizontalLines( false );
        jtable_.setShowVerticalLines( false );
        jtable_.setCellSelectionEnabled( true );
        Dimension interCell = jtable_.getIntercellSpacing();
        jtable_.setIntercellSpacing( new Dimension( 5, interCell.height ) );
        StarJTable.alignHeadersLeft( jtable_ );
        scroller_ = new JScrollPane( jtable_ );

        /* Arrange to do an initial column width configuration one time
         * on first display of the table. */
        jtable_.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                jtable_.removeAncestorListener( this );
                tableModel_.configureColumnWidths( null );
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );

        /* Arrange to reconfigure the column widths if the component
         * is resized. */
        jtable_.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                tableModel_.configureResizeMode();
            }
        } );

        /* Grab cell content to the clipboard on cell highlight.
         * This is useful, but may be slightly unexpected in some
         * windowing environments? */
        jtable_.getSelectionModel()
               .addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                int[] icols = jtable_.getSelectedColumns();
                int[] irows = jtable_.getSelectedRows();
                if ( icols.length == 1 && irows.length == 1 ) {
                    Object value = jtable_.getValueAt( irows[ 0 ], icols[ 0 ] );
                    String txt = value == null ? null : value.toString();
                    if ( txt != null ) {
                        TopcatUtils.setClipboardText( txt );
                    }
                }
            }
        } );
        add( scroller_ );
    }

    /**
     * Adds a completed item to the display log.
     *
     * @param  irow   row index
     * @param  status  completed execution status
     * @param  text   user-directed one-line message
     */
    public void addItem( long irow, Status status, String text ) {
        Item item = new Item( iseq_++, irow );
        item.status_ = status;
        item.text_ = text;
        tableModel_.addItem( item );
        scrollToBottom();
    }

    /**
     * Adds an item whose details are to be filled in at a later date
     * to the display log.  The caller should make a subsequent call to
     * {@link #updateItem updateItem}.
     *
     * @param   irow  row index
     * @return   item that can be updated later
     */
    public Item addItem( long irow ) {
        Item item = new Item( iseq_++, irow );
        tableModel_.addItem( item );
        scrollToBottom();
        return item;
    }

    /**
     * Fills in the details for a previously added item.
     *
     * @param  item  item acquired from a previous call to <code>addItem</code>
     * @param  status  completed execution status
     * @param  msg   user-directed one-line message
     */
    public void updateItem( Item item, Status status, String msg ) {
        item.status_ = status;
        item.text_ = msg;
        tableModel_.updateItem( item );
        scrollToBottom();
    }

    /**
     * Ensures that the bottom (most recently added) item is visible.
     */
    private void scrollToBottom() {
        final JScrollBar vbar = scroller_.getVerticalScrollBar();

        /* Desperation; it doesn't seem to invoke it at the right time
         * if it's done in-thread. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                vbar.setValue( vbar.getMaximum() );
            }
        } );
    }

    /**
     * Represents a complete or incomplete activation result.
     */
    public static class Item {
        private final long iseq_;
        private final long irow_;
        private Status status_;
        private String text_;

        /**
         * Constructor.
         *
         * @param  iseq   counter for items used by this panel
         * @param  irow   activated row index
         */
        private Item( long iseq, long irow ) {
            iseq_ = iseq;
            irow_ = irow;
        }
    }

    /**
     * Status of completed activation action.
     */
    public enum Status {

        /** Successful completion. */
        OK,

        /** Failed completion. */
        FAIL,

        /** Action aborted before it had time to complete. */
        CANCELLED;
    }

    /**
     * TableModel supplying the content of the displayed JTable.
     * It has a row corresponding to each Item.
     */
    private class LogTableModel extends AbstractTableModel {
        final List<Item> items_;
        final ItemColumn<?>[] cols_;
        final int[] maxWidths_;
        long iseq0_;
        long iseq_;

        /**
         * Constructor.
         */
        LogTableModel() {
            items_ = new ArrayList<Item>();
            cols_ = new ItemColumn<?>[] {
                new ItemColumn<Long>( "Seq", Long.class ) {
                    public Long getValue( Item item ) {
                        return Long.valueOf( item.iseq_ + 1 );
                    }
                },
                new ItemColumn<Long>( "Row", Long.class ) { 
                    public Long getValue( Item item ) {
                        return Long.valueOf( item.irow_ + 1 );
                    }
                },
                new ItemColumn<String>( "Status", String.class ) {
                    public String getValue( Item item ) {
                        Status status = item.status_;
                        return status == null ? null
                                              : status.toString();
                    }
                },
                new ItemColumn<String>( "Message", String.class ) {
                    public String getValue( Item item ) {
                        return item.text_;
                    }
                },
            };
            int ncol = cols_.length;
            maxWidths_ = new int[ ncol ];
        }
        public int getColumnCount() {
            return cols_.length;
        }
        public int getRowCount() {
            return items_.size();
        }
        public Object getValueAt( int ir, int ic ) {
            return cols_[ ic ].getValue( items_.get( ir ) );
        }
        @Override
        public String getColumnName( int ic ) {
            return cols_[ ic ].name_;
        }
        @Override
        public Class<?> getColumnClass( int icol ) {
            return cols_[ icol ].clazz_;
        }

        /**
         * Flag cells as editable as a quick hack to allow the cell contents
         * to be selected for copy/paste.  Actual editing of the values
         * doesn't do anything.
         */
        @Override
        public boolean isCellEditable( int irow, int icol ) {
            return true;
        }

        /**
         * Updates column widths based on the presented cell values
         * alongside any previous column width determinations.
         *
         * @param  row  cell array with one value per column,
         *              or null to take account of headers
         */
        private void configureColumnWidths( Object[] row ) {
            TableColumnModel colModel = jtable_.getColumnModel();
            int ncol = colModel.getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {
                TableColumn tcol = colModel.getColumn( icol );
                TableCellRenderer rend =
                      row == null
                    ? jtable_.getTableHeader().getDefaultRenderer()
                    : jtable_.getDefaultRenderer( cols_[ icol ].clazz_ );
                Object value = row == null
                             ? tcol.getHeaderValue()
                             : row[ icol ];
                Component comp =
                    rend.getTableCellRendererComponent( jtable_, value, false,
                                                        false, 0, icol );
                int w = comp.getPreferredSize().width + 10;
                maxWidths_[ icol ] = Math.max( w, maxWidths_[ icol ] );
                int wpref = maxWidths_[ icol ];
                if ( icol == ncol - 1 ) {
                    tcol.setMinWidth( wpref );
                    tcol.setMaxWidth( Integer.MAX_VALUE );
                }
                else {
                    tcol.setMinWidth( wpref );
                    tcol.setMaxWidth( wpref );
                }
            }
            configureResizeMode();
        }

        /**
         * Updates the resize mode of this model's JTable based on the
         * current column width requirements and the actual size of
         * the JTable's parent component.
         */
        void configureResizeMode() {
            int wtot = 0;
            for ( int i = 0; i < maxWidths_.length; i++ ) {
                wtot += maxWidths_[ i ];
            }
            Component holder = jtable_.getParent();
            boolean isWide = holder == null || wtot > holder.getSize().width;
            jtable_.setAutoResizeMode( isWide
                                     ? JTable.AUTO_RESIZE_OFF
                                     : JTable.AUTO_RESIZE_LAST_COLUMN );
        }

        /**
         * Adds a complete or incomplete entry to the end of this table.
         * If that means too many rows, entries are lost from the top.
         *
         * @param  item  item to add
         */
        private void addItem( Item item ) {
            if ( items_.size() < maxItems_ ) {
                int ir = items_.size();
                items_.add( item );
                fireTableRowsInserted( ir, ir );
            }
            else {
                items_.remove( 0 );
                iseq0_++;
                items_.add( item );
                fireTableDataChanged();
            }
            configureColumnWidths( getRow( items_.size() - 1 ) );
        }

        /**
         * Signals that an existing entry's content may have changed.
         *
         * @param  item  item that may have been updated
         */
        private void updateItem( Item item ) {
            int ir = (int) ( item.iseq_ - iseq0_ );
            if ( ir >= 0 && ir < getRowCount() ) {
                configureColumnWidths( getRow( ir ) );
                fireTableRowsUpdated( ir, ir );
            }
        }

        /**
         * Returns the cell values for a given row index.
         *
         * @param  irow  row index
         * @return  array of cell values, one per column
         */
        private Object[] getRow( int irow ) {
            int ncol = getColumnCount();
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = getValueAt( irow, icol );
            }
            return row;
        }
    }

    /**
     * Defines data and metadata for the column of a LogTable.
     * A column represents one attribute of an Item
     */
    private static abstract class ItemColumn<T> {
        final String name_;
        final Class<T> clazz_;

        /**
         * Constructor.
         *
         * @param  name  column display name
         * @param  clazz  content type for column
         */
        ItemColumn( String name, Class<T> clazz ) {
            name_ = name;
            clazz_ = clazz;
        }

        /**
         * Returns the value of this column corresponding to a given Item value.
         *
         * @param  item   object containing data
         * @return   column value
         */
        public abstract T getValue( Item item );
    }
}
