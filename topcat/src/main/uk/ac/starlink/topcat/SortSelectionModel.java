package uk.ac.starlink.topcat;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.table.ColumnData;

/**
 * ComboBoxModel used for storing the available and last-invoked
 * sort orders.
 */
public class SortSelectionModel implements ComboBoxModel<SortOrder> {

    private final ColumnDataComboBoxModel sortDataModel_;
    private SortOrder selectedOrder_;

    SortSelectionModel( TopcatModel tcModel ) {
        ColumnDataComboBoxModel.Filter comparableFilter = info ->
            Comparable.class.isAssignableFrom( info.getContentClass() );
        sortDataModel_ = new ColumnDataComboBoxModel( tcModel, comparableFilter,
                                                      true, false );
        selectedOrder_ = SortOrder.NONE;
    }

    /**
     * Turns a column identifier into a sort order definition.
     */
    public SortOrder getElementAt( int index ) {
        ColumnData cdata = sortDataModel_.getColumnDataAt( index );
        String expr = cdata == null ? null
                                    : cdata.getColumnInfo().getName();
        return expr != null && expr.trim().length() > 0
             ? new SortOrder( new String[] { expr } )
             : SortOrder.NONE;
    }

    public int getSize() {
        return sortDataModel_.getSize();
    }

    public void addListDataListener( ListDataListener l ) {
        sortDataModel_.addListDataListener( l );
    }

    public void removeListDataListener( ListDataListener l ) {
        sortDataModel_.removeListDataListener( l );
    }

    public SortOrder getSelectedItem() {
        return selectedOrder_;
    }

    public void setSelectedItem( Object item ) {
        // Note: should update the sortDataModel here.
        selectedOrder_ = (SortOrder) item;
    }
}
