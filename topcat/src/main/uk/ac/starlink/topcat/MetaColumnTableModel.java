package uk.ac.starlink.topcat;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Makes a TableModel out of a list of MetaColumn objects.
 */
public abstract class MetaColumnTableModel extends AbstractTableModel {

    private final List<MetaColumn> metaList_;

    /**
     * Constructs a new MetaColumnTableModel.
     *
     * @param   metaList  a list of {@link MetaColumn} objects
     */
    public MetaColumnTableModel( List<MetaColumn> metaList ) {
        metaList_ = metaList;
    }

    public int getColumnCount() {
        return metaList_.size();
    }

    abstract public int getRowCount();

    public Object getValueAt( int irow, int icol ) {
        return metaList_.get( icol ).getValue( irow );
    }

    public void setValueAt( Object value, int irow, int icol ) {
        metaList_.get( icol ).setValue( irow, value );
        fireTableCellUpdated( irow, icol );
    }

    public Class getColumnClass( int icol ) {
        return metaList_.get( icol ).getContentClass();
    }

    public String getColumnName( int icol ) {
        return metaList_.get( icol ).getName();
    }

    public boolean isCellEditable( int irow, int icol ) {
        return metaList_.get( icol ).isEditable( irow );
    }

    /**
     * Returns the list of columns which provide the data for this model.
     * The list may be altered (but fire appropriate events if you do it
     * on a live instance).
     *
     * @return   column list
     */
    public List<MetaColumn> getColumnList() {
        return metaList_;
    }
}
