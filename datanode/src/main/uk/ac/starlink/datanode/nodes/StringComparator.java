package uk.ac.starlink.datanode.nodes;

import java.util.Comparator;

/**
 * A very simple implementation of the {@link java.util.Comparator} interface.
 * It is suitable for use on any objects for which the <code>toString</code>
 * method provides a suitable basis for object comparison.
 * <p>
 * You might think that calling <code>java.text.Collator.getInstance</code>
 * ought to provide much the same thing; however that gives you a 
 * <code>compare</code> method which casts to <code>String</code> rather
 * than calling the <code>toString</code> method on its arguments,
 * leading to a <code>ClassCastException</code> in most useful cases.
 * I wonder why it does that?
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class StringComparator implements Comparator {
    public int compare( Object o1, Object o2 ) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.compareTo( s2 );
    }
    public boolean equals( Object obj ) {
        return false;
    }
}
