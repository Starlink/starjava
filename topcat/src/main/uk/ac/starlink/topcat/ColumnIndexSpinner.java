package uk.ac.starlink.topcat;

import java.awt.Dimension;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;

/**
 * A JSpinner which can select the index of a new column.
 * Note this presents values to the user which displays 1-based column indices
 * while its getColumnIndex and setColumnIndex values are 0-based.
 * Note this means that the legal spinner values are those between 
 * 1 (the lowest non-special column) and columnCount+1.
 * Thus this is not as it stands a class of very general usefulness.
 */
public class ColumnIndexSpinner extends JSpinner {

    private TableColumnModel columnModel;
    private ColumnSpinnerModel spinnerModel;

    /**
     * Constructs a new spinner based on a given model.
     *
     * @param  columnModel  the column model which defines the range of
     *         legal values
     */
    public ColumnIndexSpinner( TableColumnModel columnModel ) {
        super( new ColumnSpinnerModel( columnModel ) );
        this.columnModel = columnModel;
        this.spinnerModel = (ColumnSpinnerModel) getModel();
        Dimension spinSize = getPreferredSize();
        setPreferredSize( new Dimension( spinSize.width + 32,
                                         spinSize.height ) );
    }

    /**
     * Returns the 0-based selected column index.
     *
     * @return  selected index
     */
    public int getColumnIndex() {
        Object value = getValue();
        if ( value instanceof Number ) {
            int index1 = ((Number) getValue()).intValue();
            return index1 - 1;
        }
        else {
            return 0;
        }
    }

    /**
     * Sets the 0-based selected column index.
     *
     * @param  index new index - can be -1 to indicate after the last one
     */
    public void setColumnIndex( int index ) {
        int index0;
        if ( index < 0 ) {
            index0 = ((Number) spinnerModel.getMaximum()).intValue();
        }
        else {
            index0 = index + 1;
        }
        setValue( new Integer( index0 ) );
    }

    private static class ColumnSpinnerModel extends SpinnerNumberModel 
                                         implements TableColumnModelListener {
        TableColumnModel columnModel;
        ColumnSpinnerModel( TableColumnModel columnModel ) {
            this.columnModel = columnModel;
            columnModel.addColumnModelListener( this );
            reconfigure();
        }
        public void columnAdded( TableColumnModelEvent evt ) {
            reconfigure();
        }
        public void columnRemoved( TableColumnModelEvent evt ) {
            reconfigure();
        }
        public void columnMoved( TableColumnModelEvent evt ) {}
        public void columnMarginChanged( ChangeEvent evt ) {}
        public void columnSelectionChanged( ListSelectionEvent evt ) {}
        private void reconfigure() {
            setMinimum( new Integer( 1 ) );
            setMaximum( new Integer( columnModel.getColumnCount() + 1) );
            Object v = getValue();
            if ( v instanceof Comparable ) {
                Comparable val = (Comparable) v;
                if ( val.compareTo( getMaximum() ) > 0 ) {
                    setValue( getMaximum() );
                    fireStateChanged();
                }
                else if ( val.compareTo( getMinimum() ) < 0 ) {
                    setValue( getMinimum() );
                    fireStateChanged();
                }
            }
        }
    }
}
