package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a set of table rows which are linked (usually this means 
 * that they are considered to reference the same object).
 * No external order may be imposed on the row references, so that any 
 * <tt>RowLink</tt> object linking the same set of rows is considered 
 * equal to any other.  This makes RowLink instances suitable for use
 * as keys in hashes that should not contain duplicate entries for 
 * duplicate links.
 *
 * <p>You could argue this class should implement the
 * {@link java.util.SortedSet} interface (it's a set of RowRefs), but since
 * it's immutable you couldn't do much extra with it anyway.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowLink {

    private final RowRef[] rowRefs;
    private final SortedSet rowRefSet;
    private Integer hashCode;

    /**
     * Constructs a new RowLink from a Collection of {@link RowRef} objects.
     * An unchecked exception will be thrown if <tt>rows</tt> contains some
     * elements which are not instances of <tt>RowRef</tt>.
     *
     * @param  rows  collection of <tt>RowRef</tt> objects
     */
    public RowLink( Collection rows ) {
        this( (RowRef[]) rows.toArray( new RowRef[ 0 ] ) );
    }

    /**
     * Constructs a new RowLink from an array of <tt>RowRef</tt> objects.
     *
     * @param  rows  array of row references
     */
    public RowLink( RowRef[] rows ) {
        this.rowRefs = rows;
        Arrays.sort( rowRefs );
        rowRefSet =
            Collections
           .unmodifiableSortedSet( new TreeSet( Arrays.asList( rowRefs ) ) );
    }

    /**
     * Returns an unmodifiable sorted set of the {@link RowRef} objects linked
     * by this object.  The sort order is the natural order of the
     * <tt>RowRef</tt>s it contains. 
     *
     * @return   unmodifiable sorted set of <tt>RowRef</tt> objects
     */
    public SortedSet getRowRefs() {
        return rowRefSet;
    }

    /**
     * Returns the number of rows linked by this object.
     *
     * @return  number of RowRefs
     */
    public int size() {
        return rowRefs.length;
    }

    /**
     * Assesses equality.  Two <tt>RowLink</tt> objects are equal if they 
     * contain equivalent sets of <tt>RowRef</tt>s.  Note they don't 
     * necessarily have to be the same class, though they will both have
     * to be instances of <tt>RowLink</tt> or one of its subclasses.
     */
    public boolean equals( Object o ) {
        return ( o instanceof RowLink ) 
               ? getRowRefs().equals( ((RowLink) o).getRowRefs() )
               : false;
    }

    /**
     * Returns a hash code which is consistent with the 
     * <tt>equals</tt> method.  Since <tt>RowLink</tt>s are immutable,
     * this is only calculated once, for efficiency.
     */
    public int hashCode() {
        if ( hashCode == null ) {
            int result = 37;
            int n = size();
            for ( int i = 0; i < n; i++ ) {
                result = 23 * result + rowRefs[ i ].hashCode();
            }
            hashCode = new Integer( result );
        }
        return hashCode.intValue();
    }
}
