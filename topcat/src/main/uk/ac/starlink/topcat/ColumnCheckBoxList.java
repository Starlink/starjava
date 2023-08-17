package uk.ac.starlink.topcat;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import uk.ac.starlink.table.gui.StarTableColumn;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * CheckBoxList that represents the columns in a TableColumnModel.
 *
 * <p>If columns are added, removed or renamed in the TableColumnModel,
 * that is reflected here.  However, since you can reorder entries in
 * this list, not much attempt is made to reflect order changes in
 * the TableColumnModel: added columns are added at the end,
 * movements are ignored.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2023
 */
public class ColumnCheckBoxList extends BasicCheckBoxList<TableColumn> {

    private final boolean dfltChecked_;
    private final Predicate<TableColumn> filter_;
    private final TableColumnModelListener tcmListener_;
    private TableColumnModel columnModel_;
    private static final boolean CAN_SELECT = false;

    /**
     * Constructor.
     *
     * @param  dfltChecked   whether new columns added to the list are
     *                       by default checked or not
     * @param  filter        filter for columns from the input column model
     *                       to include in this list, or null to include all
     */
    public ColumnCheckBoxList( boolean dfltChecked,
                               Predicate<TableColumn> filter ) {
        super( CAN_SELECT,
               createLabelRendering( tc ->
                     tc instanceof StarTableColumn
                   ? ((StarTableColumn) tc).getColumnInfo().getName()
                   : String.valueOf( tc.getHeaderValue() ) ) );
        dfltChecked_ = dfltChecked;
        filter_ = filter == null ? tc -> true : filter;
        tcmListener_ = new TableColumnModelListener() {
            public void columnAdded( TableColumnModelEvent evt ) {
                Set<TableColumn> existing =
                    new HashSet<TableColumn>( getItems() );
                for ( Enumeration<TableColumn> en = columnModel_.getColumns();
                      en.hasMoreElements(); ) {
                    TableColumn tcol = en.nextElement();
                    if ( filter_.test( tcol ) && ! existing.contains( tcol ) ) {
                        getModel().addElement( tcol );
                        setChecked( tcol, dfltChecked_ );
                    }
                }
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                Set<TableColumn> current = new HashSet<>();
                for ( Enumeration<TableColumn> en = columnModel_.getColumns();
                      en.hasMoreElements(); ) {
                    current.add( en.nextElement() );
                }
                for ( TableColumn tcol : getItems() ) {
                    if ( ! current.contains( tcol ) ) {
                        getModel().removeElement( tcol );
                    }
                }
            }
            public void columnMoved( TableColumnModelEvent evt ) {
            }
            public void columnMarginChanged( ChangeEvent evt ) {
            }
            public void columnSelectionChanged( ListSelectionEvent evt ) {
            }
        };
    }

    /**
     * Sets the TableColumnModel that this list should represent.
     *
     * @param  columnModel  table column model, may be null
     */
    public void setTableColumnModel( TableColumnModel columnModel ) {
        if ( columnModel_ != null ) {
            columnModel_.removeColumnModelListener( tcmListener_ );
        }
        columnModel_ = columnModel;
        if ( columnModel_ != null ) {
            columnModel_.addColumnModelListener( tcmListener_ );
        }
        DefaultListModel<TableColumn> listModel = getModel();
        listModel.clear();
        for ( Enumeration<TableColumn> en = columnModel_.getColumns();
              en.hasMoreElements(); ) {
            TableColumn tcol = en.nextElement();
            if ( filter_.test( tcol ) ) {
                listModel.addElement( tcol );
            }
        }
        repaint();
    }

    /**
     * Returns the TableColumnModel that this list is representing.
     *
     * @return  table column model, may be null
     */
    public TableColumnModel getTableColumnModel() {
        return columnModel_;
    }
}
