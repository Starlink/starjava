package uk.ac.starlink.fits;

import java.io.IOException;
import nom.tam.util.RandomAccess;

/**
 * RandomAccess subinterface to describe an object which can copy itself.
 * The copy is backed by the same data, but has a separate file pointer,
 * so the two objects can be accessed without mutual interference.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2010
 */
public interface CopyableRandomAccess extends RandomAccess {

    /**
     * Returns a new copy of this object.
     * The copy has the same data but an independent file pointer,
     * facilitating (for instance) data access from multiple threads.
     * The initial value of the copy's file pointer is the same as 
     * the current file pointer of the original.
     *
     * @return   new copy
     */
    CopyableRandomAccess copyAccess() throws IOException;
}
