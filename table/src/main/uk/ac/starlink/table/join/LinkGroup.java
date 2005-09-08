package uk.ac.starlink.table.join;

/**
 * Defines a group of RowLink objects.
 * This object is just a label, containing a unique ID and a count of 
 * how many links there are in the group. 
 * It does not contain references to the constituent RowLinks themselves.
 * 
 * @author   Mark Taylor
 * @since    7 Sep 2005
 * @see    MatchStarTables#findGroups
 */
public class LinkGroup implements Comparable {

    private final int id_;
    private final int size_;

    /**
     * Constructor.
     *
     * @param   id  unique identifier
     * @param   size  number of objects in this group
     */
    public LinkGroup( int id, int size ) {
        id_ = id;
        size_ = size;
    }

    /**
     * Returns this group's ID.
     *
     * @return id
     */
    public int getID() {
        return id_;
    }

    /**
     * Returns this group's size.
     *
     * @return  number of items in this group
     */
    public int getSize() {
        return size_;
    }

    public boolean equals( Object o ) {
        if ( o instanceof LinkGroup ) {
            LinkGroup other = (LinkGroup) o;
            return other.id_ == this.id_
                && other.size_ == this.size_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return size_ * 7777 + id_;
    }

    public int compareTo( Object o ) {
        LinkGroup other = (LinkGroup) o;
        if ( this.id_ == other.id_ ) {
            if ( this.size_ == other.size_ ) {
                return 0;
            }
            else {
                return this.size_ > other.size_ ? +1 : -1;
            }
        }
        else {
            return this.id_ > other.id_ ? +1 : -1;
        }
    }

    public String toString() {
        return "group" + id_ + "[" + size_ + "]";
    }
}
