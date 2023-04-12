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

    private TableColumnModel columnModel_;
    private ColumnSpinnerModel spinnerModel_;

    /**
     * Constructs a new spinner based on a given model.
     * Setting <code>add1</code> true makes sense if you're adding a
     * new column, and false if you're editing an existing one.
     *
     * @param  columnModel  the column model which defines the range of
     *         legal values
     * @param  add1  if true, the maximum can be one larger than the
     *               current size of the model
     */
    public ColumnIndexSpinner( TableColumnModel columnModel, boolean add1 ) {
        super( new ColumnSpinnerModel( columnModel, add1 ) );
        columnModel_ = columnModel;
        spinnerModel_ = (ColumnSpinnerModel) getModel();
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
            index0 = ((Number) spinnerModel_.getMaximum()).intValue();
        }
        else {
            index0 = index + 1;
        }
        setValue( Integer.valueOf( index0 ) );
    }

    /**
     * Model implementation for use with ColumnIndexSpinner.
     */
    private static class ColumnSpinnerModel
            extends SpinnerNumberModel
            implements TableColumnModelListener {
        private final TableColumnModel columnModel_;
        private final boolean add1_;

        /**
         * Constructor.
         *
         * @param  columnModel  the column model which defines the range of
         *         legal values
         * @param  add1  if true, the maximum can be one larger than the
         *               current size of the model
         */
        ColumnSpinnerModel( TableColumnModel columnModel, boolean add1 ) {
            columnModel_ = columnModel;
            add1_ = add1;
            columnModel_.addColumnModelListener( this );
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
            setMinimum( Integer.valueOf( 1 ) );
            setMaximum( Integer.valueOf( columnModel_.getColumnCount()
                                         + ( add1_ ? 1 : 0 ) ) );
            Object v = getValue();
            if ( v instanceof Comparable ) {
                @SuppressWarnings("unchecked")
                Comparable<Object> val = (Comparable<Object>) v;
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
