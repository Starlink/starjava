package uk.ac.starlink.datanode.nodes;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Blank node.
 */
public class EmptyDataNode extends DefaultDataNode {

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            public boolean hasNext() {
                return false;
            }
            public Object next() {
                throw new NoSuchElementException();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public String getName() {
        return "Empty";
    }

    public String toString() {
        return getName();
    }
}
