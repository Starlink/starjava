package uk.ac.starlink.topcat;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Makes a TableModel out of a list of MetaColumn objects.
 */
public abstract class MetaColumnTableModel extends AbstractTableModel {

    private List metas;

    /**
     * Constructs a new MetaColumnTableModel.
     *
     * @param   metas  a list of {@link MetaColumn} objects
     */
    public MetaColumnTableModel( List metas ) {
        this.metas = metas;
    }

    public int getColumnCount() {
        return metas.size();
    }

    abstract public int getRowCount();

    public Object getValueAt( int irow, int icol ) {
        return getMeta( icol ).getValue( irow );
    }

    public void setValueAt( Object value, int irow, int icol ) {
        getMeta( icol ).setValue( irow, value );
    }

    public Class getColumnClass( int icol ) {
        return getMeta( icol ).getContentClass();
    }

    public String getColumnName( int icol ) {
        return getMeta( icol ).getName();
    }

    public boolean isCellEditable( int irow, int icol ) {
        return getMeta( icol ).isEditable();
    }

    private MetaColumn getMeta( int icol ) {
        return (MetaColumn) metas.get( icol );
    }
}
