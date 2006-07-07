/** This inteface describes allows uses to easily perform
 *  basic I/O operations
 *  on a FITS element.
 */

package nom.tam.fits;

import nom.tam.util.*;
import java.io.IOException;


public interface FitsElement {
    
    /** Read the contents of the element from an input source.
     *  @param in	The input source.
     */
    public void read(ArrayDataInput   in)  throws FitsException, IOException;
    
    /** Write the contents of the element to a data sink.
     *  @param out      The data sink.
     */
    public void write(ArrayDataOutput out) throws FitsException, IOException;
    
    /** Rewrite the contents of the element in place.
     *  The data must have been orignally read from a random
     *  access device, and the size of the element may not have changed.
     */
    public void rewrite() throws FitsException, IOException;
    
    /** Get the byte at which this element begins.
     *  This is only available if the data is originally read from
     *  a random access medium.
     */
    public long getFileOffset();
    
    /** Can this element be rewritten? */
    public boolean rewriteable();
    
    /** The size of this element in bytes */
    public long getSize();
    
}
