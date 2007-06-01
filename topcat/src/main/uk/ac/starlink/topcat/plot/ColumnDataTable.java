package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Table class built up from ColumnData objects.  Two instances of this
 * class will be <code>equal</code> if they have the same TopcatModel and
 * columns which are <code>equal</code>.
 *
 * @author   Mark Taylor
 * @since    1 Jun 2007
 */
public class ColumnDataTable extends ColumnStarTable {

    private final TopcatModel tcModel_;
    private final ColumnData[] cols_;

    /**
     * Constructor.
     *
     * @param   tcModel  topcat model that the columns come from
     * @param   cols     column data objects
     */
    public ColumnDataTable( TopcatModel tcModel, ColumnData[] cols ) {
        tcModel_ = tcModel;
        cols_ = cols;
        for ( int i = 0; i < cols.length; i++ ) {
            addColumn( cols[ i ] );
        }
    }

    /**
     * Returns this table's TopcatModel.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        return tcModel_;
    }

    public long getRowCount() {
        return tcModel_.getDataModel().getRowCount();
    }

    public boolean equals( Object o ) {
        if ( o instanceof ColumnDataTable ) {
            ColumnDataTable other = (ColumnDataTable) o;
            return this.tcModel_ == other.tcModel_
                && Arrays.equals( this.cols_, other.cols_ );
        }
        else {
            return false;
        }
    }
    
    public int hashCode() {
        int code = tcModel_.hashCode();
        for ( int i = 0; i < cols_.length; i++ ) {
            code = 23 * code + cols_[ i ].hashCode();
        }
        return code;
    }
}
