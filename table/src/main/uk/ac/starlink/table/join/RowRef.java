package uk.ac.starlink.table.join;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Represents a reference to a table row.
 * This class really just exists to encapsulate the combination of a 
 * {@link StarTable} and a <tt>long</tt> referencing a row of that table.
 * Importantly though, it implements <tt>equals</tt>, <tt>hashCode</tt>
 * and the <tt>Comparable</tt> interface in such a way as to make it
 * suitable for use as keys in a SortedSet.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowRef implements Comparable {
    private final StarTable table;
    private final long lrow;
 
    /**
     * Constructs a new RowRef from a table and a row index.
     *
     * @param  table  StarTable
     * @param  lrow   row index
     */
    public RowRef( StarTable table, long lrow ) {
        this.table = table;
        this.lrow = lrow;
    }

    /**
     * Returns the StarTable referenced by this object.
     *
     * @return  table
     */
    public StarTable getTable() {
        return table;
    }

    /**
     * Returns the row index referenced by this object.
     *
     * @return   row index
     */
    public long getRowIndex() {
        return lrow;
    }

    /**
     * Invokes {@link uk.ac.starlink.table.StarTable#getRow} and returns
     * the result.  Doesn't cache the value.
     *
     * @return   row data referenced by this object
     */
    public Object[] getRow() throws IOException {
        return table.getRow( lrow );
    }

    public boolean equals( Object o ) {
        if ( o instanceof RowRef ) {
            RowRef other = (RowRef) o;
            return other.lrow == lrow
                && other.table == table;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int result = 37;
        result = 23 * result + (int) lrow;
        result = 23 * result + System.identityHashCode( table );
        return result;
    }

    public int compareTo( Object o ) {
        RowRef other = (RowRef) o;
        if ( this.table != other.table ) {
            return this.hashCode() < other.hashCode() ? -1 : 1;
        }
        else if ( this.lrow != other.lrow ) {
            return this.lrow < other.lrow ? -1 : 1;
        }
        else {
            return 0;
        }
    }
}
