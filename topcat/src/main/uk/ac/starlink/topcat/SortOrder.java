package uk.ac.starlink.topcat;

import javax.swing.table.TableColumn;

/**
 * Defines a sorting order for a table.
 * An instance of this class defines the algorithm by which a sort is done,
 * not rather than a given row sequence.
 *
 * <p>Currently, the sort order is defined only by the column that the
 * table is sorted on, but this may get extended one day.
 * Note that the sense (up or down) of the sort is selected separately than by
 * this object.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Feb 2004
 */
public class SortOrder {
    private final TableColumn tcol;

    /** SortOrder instance indicating the natural order of the data. */
    public static final SortOrder NONE = 
        new SortOrder( ColumnComboBoxModel.NO_COLUMN );

    /**
     * Constructs a new sort order based on a table column.
     * 
     * @param  tcol  table colunmn
     */
    public SortOrder( TableColumn tcol ) {
        this.tcol = tcol;
    }

    /**
     * Gives the column on which this table is based.
     *
     * @return  table column
     */
    public TableColumn getColumn() {
        return tcol;
    }

    public String toString() {
        Object id = tcol.getIdentifier();
        return id == null ? "(none)" : id.toString();
    }

    public boolean equals( Object o ) {
        if ( o instanceof SortOrder ) {
            SortOrder other = (SortOrder) o;
            return this.tcol.equals( other.tcol );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return tcol.hashCode();
    }
}
