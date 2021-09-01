package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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
 *
 * <p><strong>Note:</strong> The <tt>getRef</tt> method <strong>must</strong>
 * return <tt>RowRef</tt>s in their natural (<code>Comparable</code>) order.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class RowLink implements Comparable<RowLink> {

    /**
     * Returns the number of rows linked by this object.
     *
     * @return  number of RowRefs
     */
    public abstract int size();

    /**
     * Returns the <tt>i</tt><sup>th</sup> row ref in this ordered
     * sequence of refs.  The sequence must be as defined by
     * <code>RowRef.compareTo</code>.
     *
     * @param  i  index
     * @return  RowRef at <tt>i</tt>
     */
    public abstract RowRef getRef( int i );

    /**
     * Assesses equality.  Two <tt>RowLink</tt> objects are equal if they
     * contain equivalent sets of <tt>RowRef</tt>s.
     */
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof RowLink ) {
            RowLink other = (RowLink) o;
            int nref = this.size();
            if ( nref == other.size() ) {
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
    @Override
    public int hashCode() {
        int nref = size();
        int result = 37;
        for ( int i = 0; i < nref; i++ ) {
            result = 23 * result + getRef( i ).hashCode();
        }
        return result;
    }

    /**
     * Comparison order compares first table (if present in both objects)
     * first, etc.
     */
    public int compareTo( RowLink other ) {
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
        return Integer.compare( this.hashCode(), other.hashCode() );
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer( "(" );
        for ( int i = 0; i < size(); i++ ) {
            if ( i > 0 ) {
               sbuf.append( ", " );
            }
            sbuf.append( getRef( i ).toString() );
        }
        sbuf.append( ')' );
        return sbuf.toString();
    }

    /**
     * Returns a RowLink instance for a given collection of RowRefs.
     * This may be more efficient than using the {@link RowLinkN} constructor.
     *
     * @param  refs  row refs
     * @return  row link containing given refs
     */
    public static RowLink createLink( Collection<RowRef> refs ) {
        switch ( refs.size() ) {
            case 1:
                return new RowLink1( refs.iterator().next() );
            case 2:
                Iterator<RowRef> it2 = refs.iterator();
                RowRef ref1 = it2.next();
                RowRef ref2 = it2.next();
                assert ! it2.hasNext();
                return new RowLink2( ref1, ref2 );
            default:
                return new RowLinkN( refs );
        }
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
