package uk.ac.starlink.table.join;

/**
 * Represents a reference to a table row.
 * This class really just exists to encapsulate the combination of an
 * <code>int</code> index referencing a table
 * and a <code>long</code> referencing a row of that table.
 * Importantly though, it implements <code>equals</code>, <code>hashCode</code>
 * and the <code>Comparable</code> interface in such a way as to make it
 * suitable for use as keys in a SortedSet.
 * The sort order defined sorts lowest table index first, then lowest
 * row index.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowRef implements Comparable<RowRef> {
    private final int iTable;
    private final long lRow;

    /**
     * Constructs a new RowRef from a table and a row index.
     *
     * @param  iTable table index
     * @param  lRow   row index
     */
    public RowRef( int iTable, long lRow ) {
        this.iTable = iTable;
        this.lRow = lRow;
    }

    /**
     * Returns the table index.
     *
     * @return  table index
     */
    public int getTableIndex() {
        return iTable;
    }

    /**
     * Returns the row index;
     *
     * @return  row index
     */
    public long getRowIndex() {
        return lRow;
    }

    public boolean equals( Object o ) {
        if ( o instanceof RowRef ) {
            RowRef other = (RowRef) o;
            return other.lRow == lRow
                && other.iTable == iTable;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int result = 37;
        result = 23 * result + iTable;
        result = 23 * result + (int) lRow;
        return result;
    }

    public int compareTo( RowRef other ) {
        if ( this.iTable != other.iTable ) {
            return this.iTable < other.iTable ? -1 : +1;
        }
        else if ( this.lRow != other.lRow ) {
            return this.lRow < other.lRow ? -1 : +1;
        }
        else {
            return 0;
        }
    }

    public String toString() {
        return iTable + ":" + lRow;
    }
}
