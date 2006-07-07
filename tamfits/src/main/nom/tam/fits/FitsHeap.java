package nom.tam.fits;


import nom.tam.util.*;
import java.io.*;

/** This class supports the FITS heap.  This
 *  is currently used for variable length columns
 *  in binary tables.
 */

public class FitsHeap implements FitsElement {
    
    /** The storage buffer */
    private byte[] heap;
    
    /** The current used size of the buffer <= heap.length */
    private int heapSize;
    
    /** The offset within a file where the heap begins */
    private long fileOffset = -1;
    
    /** The stream the last read used */
    private ArrayDataInput input;
    
    /** Our current offset into the heap */
    private int heapOffset = 0;
    
    /** A stream used to read the heap data */
    private BufferedDataInputStream bstr;
    
    /** Create a heap of a given size. */
    FitsHeap(int size) {
	heap     = new byte[size];
	heapSize = size;
    }    
    
    /** Read the heap */
    public void read(ArrayDataInput str) throws FitsException {
	
	if (str instanceof RandomAccess) {
	    fileOffset = FitsUtil.findOffset(str);
	    input = str;
	}
	
	if (heap != null) {
	    try {
	        str.read(heap, 0, heapSize);
	    } catch (IOException e) {
		throw new FitsException("Error reading heap:"+e);
	    }
	}
    }
    
    /** Write the heap */
    public void write(ArrayDataOutput str) throws FitsException {
	try {
	    str.write(heap, 0, heapSize);
	} catch (IOException e) {
	    throw new FitsException("Error writing heap:"+e);
	}
    }
    
    public boolean rewriteable() {
	return !(fileOffset < 0 || !(input instanceof ArrayDataOutput));
    }
    
    /** Attempt to rewrite the heap with the current contents.
     *  Note that no checking is done to make sure that the
     *  heap does not extend past its prior boundaries.
     */
    public void rewrite() throws IOException, FitsException {
	if (rewriteable() ) {
	    ArrayDataOutput str = (ArrayDataOutput) input;
	    FitsUtil.reposition(str, fileOffset);
	    write(str);
	} else {
	    throw new FitsException("Invalid attempt to rewrite FitsHeap");
	}
	
    }
	
    /** Get data from the heap.
     *  @param offset The offset at which the data begins.
     *  @param array  The array to be extracted.
     */
    public void getData(int offset, Object array) throws FitsException {
	
	try {
            if (bstr == null || heapOffset > offset) {
	        heapOffset = 0;
                bstr = new BufferedDataInputStream(
                                          new ByteArrayInputStream(heap));
	    }

            bstr.skipBytes(offset-heapOffset);
	    heapOffset = offset;
            heapOffset += bstr.readArray(array);

        } catch (IOException e) {
            throw new FitsException("Error decoding heap area at offset="+offset+
                    ".  Exception: Exception "+e);
        }
    }

    /** Check if the Heap can accommodate a given requirement.
     *  If not expand the heap.
     */
    void expandHeap(int need) {
	
        if (heapSize+need > heap.length) {
            int newlen = (heapSize + need)*2;
            if (newlen < 16384) {
                newlen = 16384;
            }
            byte[] newHeap = new byte[newlen];
            System.arraycopy(heap, 0, newHeap, 0, heapSize);
            heap = newHeap;
        }
    }
    
    /** Add some data to the heap. */
    int putData(Object data) throws FitsException {

	int size = ArrayFuncs.computeSize(data);
        expandHeap(size);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(size);

        try {
             BufferedDataOutputStream o = new BufferedDataOutputStream(bo);
             o.writeArray(data);
             o.flush();
             o.close();
        } catch (IOException e) {
             throw new FitsException("Unable to write variable column length data");
        }

        System.arraycopy(bo.toByteArray(), 0, heap, heapSize, size);
	int oldOffset = heapSize;
	heapSize     += size;
	heapOffset    = heapSize;
	
	return oldOffset;
    }
    
    /** Get the current offset within the heap. */
    public int getOffset() {
	return heapOffset;
    }

    public int size() {
	return heapSize;
    }
    
    public long getSize() {
	return size();
    }
    
    public long getFileOffset() {
	return fileOffset;
    }
}
