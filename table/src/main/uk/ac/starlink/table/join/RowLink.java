package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collection;

/**
 * Represents an ordered set of {@link RowRef}s 
 * which are considered in some way linked to each other.
 * Although it doesn't implement the {@link java.util.SortedSet} interface
 * (being immutable this wouldn't gain you much) its spirit is that of
 * a sorted set - its <tt>equals</tt> and <tt>hashCode</tt> methods 
 * are implemented such that two <tt>RowLink</tt>s which contain
 * equivalent groups of <tt>RowRef</tt> objects are considered the same.
 * This makes RowLink instances suitable for use
 * as keys in hashes that should not contain duplicate entries for
 * duplicate links.
 * The <tt>getRef</tt> method returns <tt>RowRef</tt>s
 * in their natural order.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowLink implements Comparable {

    private final RowRef[] rowRefs;
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
        this.rowRefs = (RowRef[]) rows.clone();
        Arrays.sort( rowRefs );
    }

    /**
     * Convenience constructor to construct a singleton RowLink.
     *
     * @param  row  sole row
     */
    public RowLink( RowRef row ) {
        this.rowRefs = new RowRef[] { row };
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
     * Returns the <tt>i</tt><sup>th</sup> row ref in this link.
     *
     * @param  i  index
     * @return  RowRef at <tt>i</tt>
     */
    public RowRef getRef( int i ) {
        return rowRefs[ i ];
    }

    /**
     * Assesses equality.  Two <tt>RowLink</tt> objects are equal if they
     * contain equivalent sets of <tt>RowRef</tt>s.
     */
    public boolean equals( Object o ) {
        if ( o instanceof RowLink ) {
            RowLink other = (RowLink) o;
            if ( this.size() == other.size() ) {
                int nref = size();
                for ( int i = 0; i < nref; i++ ) {
                    if ( ! this.getRef( i ).equals( other.getRef( i ) ) ) {
                        return false;
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    /**
     * Returns a hash code which is consistent with the
     * <tt>equals</tt> method.  Since <tt>RowLink</tt>s are immutable,
     * this is only calculated once, for efficiency.
     */
    public int hashCode() {
        if ( hashCode == null ) {
            int result = 37;
            for ( int i = 0; i < size(); i++ ) {
                result = 23 * result + getRef( i ).hashCode();
            }
            hashCode = new Integer( result );
        }
        return hashCode.intValue();
    }

    /**
     * Comparison order compares first table (if present in both objects)
     * first, etc.
     */
    public int compareTo( Object o ) {
        RowLink other = (RowLink) o;
        int nTable = 
            Math.max( other.getRef( other.size() - 1 ).getTableIndex() + 1,
                      this.getRef( this.size() - 1 ).getTableIndex() + 1 );
        long[] thisRowIndices = getRowIndices( this, nTable );
        long[] otherRowIndices = getRowIndices( other, nTable );
        for ( int i = 0; i < nTable; i++ ) {
            if ( thisRowIndices[ i ] < otherRowIndices[ i ] ) {
                return -1;
            }
            else if ( thisRowIndices[ i ] > otherRowIndices[ i ] ) {
                return +1;
            }
        }
        return new Integer( this.hashCode() )
              .compareTo( new Integer( other.hashCode() ) );
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer( "(" );
        for ( int i = 0; i < rowRefs.length; i++ ) {
            if ( i > 0 ) {
               sbuf.append( ", " );
            }
            sbuf.append( rowRefs[ i ].toString() );
        }
        sbuf.append( ')' );
        return sbuf.toString();
    }

    /**
     * Utility method used by compareTo.
     */
    private static long[] getRowIndices( RowLink link, int nTable ) {
        long[] rowIndices = new long[ nTable ];
        Arrays.fill( rowIndices, Long.MAX_VALUE );
        int nref = link.size();
        for ( int i = 0; i < nref; i++ ) {
            RowRef ref = link.getRef( i );
            int iTable = ref.getTableIndex();
            long iRow = ref.getRowIndex();
            if ( iRow < rowIndices[ iTable ] ) {
                rowIndices[ iTable ] = iRow;
            }
        }
        return rowIndices;
    }
}
