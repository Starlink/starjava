package uk.ac.starlink.connect;

import java.text.Collator;
import java.util.Comparator;

/**
 * Comparator for nodes.  It returns all Branch nodes first, followed
 * by all Leaf nodes.  Within each group, items are ordered alphabetically.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
public class NodeComparator implements Comparator {

    private Collator collator_;

    private static final NodeComparator INSTANCE = new NodeComparator();

    /**
     * Returns an instance of this class.
     */
    public static NodeComparator getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the collator used to perform alphabetic ordering.
     * May be overridden to modify behaviour.
     *
     * @return  collator
     */
    public Collator getCollator() {
        if ( collator_ == null ) {
            collator_ = Collator.getInstance();
        }
        return collator_;
    }

    public int compare( Object o1, Object o2 ) {
        Node i1 = (Node) o1;
        Node i2 = (Node) o2;
        if ( ( i1 instanceof Branch ) && ! ( i2 instanceof Branch ) ) {
            return -1;
        }
        else if ( ! ( i1 instanceof Branch ) && ( i2 instanceof Branch ) ) {
            return +1;
        }
        else {
            return getCollator().compare( i1.getName(), i2.getName() );
        }
    }
}
