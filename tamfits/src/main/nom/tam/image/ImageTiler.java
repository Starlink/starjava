package nom.tam.image;

import nom.tam.util.*;
import java.lang.reflect.Array;
import java.io.IOException;

/** This class provides a subset of an N-dimensional image.
 *  Modified May 2, 2000 by T. McGlynn to permit
 *  tiles that go off the edge of the image.
 */

public class ImageTiler {
    
    RandomAccess  	f;
    long		fileOffset;
    
    int[] 	  	dims;
    Class		base;
    
    /** Create a tiler.
     * @param f 	The random access device from which image data may be read.
     *          	This may be null if the tile information is available from
     *          	memory.
     * @param fileOffset  The file offset within the RandomAccess device at which
     *          	the data begins.
     * @param dims   	The actual dimensions of the image.
     * @param base	The base class (should be a primitive type) of the image.
     */
    
    public ImageTiler (RandomAccess f, long fileOffset, int[] dims, 
			     Class base) {
	this.f = f;
	this.fileOffset = fileOffset;
	this.dims = dims;
	this.base = base;
    }
    
    /** See if we can get the image data from memory.
     *  This may be overriden by other classes, notably
     *  in nom.tam.fits.ImageData.
     */
    public Object getMemoryImage() {
	return null;
    }
    
    
    /** Get a subset of the image.  An image tile is returned
     *  as a one-dimensional array although the image will
     *  normally be multi-dimensional.
     *  @param The starting corner (using 0 as the start) for the image.
     *  @param The length requested in each dimension.
     */
    public Object getTile(int[] corners, int[] lengths) throws IOException {
    
	if (corners.length != dims.length || lengths.length != dims.length) {
	    throw new IOException("Inconsistent sub-image request");
	}
	
	int arraySize = 1;
	for (int i=0; i<dims.length; i += 1) {
	    
	    if (corners[i] < 0 || lengths[i] < 0 || corners[i]+lengths[i] > dims[i]) {
		throw new IOException("Sub-image not within image");
	    }
	    
	    arraySize *= lengths[i];
	}
	
	Object outArray = ArrayFuncs.newInstance(base, arraySize);
	
	getTile(outArray, corners, lengths);
	return outArray;
    }
    
    /** Get a tile, filling in a prespecified array.
     *  This version does not check that the user hase
     *  entered a valid set of corner and length arrays.
     *  ensure that out matches the
     *  length implied by the lengths array.
     * 
     *  @param	outArray	The output tile array.  A one-dimensional
     *                          array.
     *                          Data not within the valid limits of the image will
     *                          be left unchanged.  The length of this
     *                          array should be the product of lengths.
     *  @param  corners		The corners of the tile.
     *  @param  lengths		The dimensions of the tile.
     * 
     */
    public void getTile(Object outArray, int[] corners, int[] lengths)
      throws IOException {
	
	Object data = getMemoryImage();
	
	if (data == null && f == null) {
	    throw new IOException("No data source for tile subset");
	}
	fillTile(data, outArray, dims, corners, lengths);
    }

    /** Fill the subset.
     *  @param		data	The memory-resident data image.
     *                          This may be null if the image is to
     *                          be read from a file.  This should
     *                          be a multi-dimensional primitive array.
     *  @param		o	The tile to be filled.  This is a
     *                          simple primitive array.
     *  @param		dims	The dimensions of the full image.
     *  @param		corners The indices of the corner of the image.
     *  @param		lengths The dimensions of the subset.
     */
    protected void fillTile(Object data, Object o, int[] dims, int[] corners, int[] lengths)
       throws IOException {
	
	
	int n = dims.length;
	int[] posits = new int[n];
	int baseLength = ArrayFuncs.getBaseLength(o);
	int segment = lengths[n-1];
	
	System.arraycopy(corners, 0, posits, 0, n);
	long currentOffset = 0;
	if (data == null) {
	    currentOffset = f.getFilePointer();
	}
	
	int outputOffset = 0;
	   
	      
	do {
	    
	    // This implies there is some overlap
	    // in the last index (in conjunction
	    // with other tests)
	    
	    int mx               = dims.length - 1;
	    boolean validSegment = 
	              posits[mx] + lengths[mx] >= 0        &&
		      posits[mx]               < dims[mx];
			      
	    
	    // Don't do anything for the current
	    // segment if anything but the
	    // last index is out of range.
	    
	    if (validSegment) {
		for (int i=0; i<mx; i += 1) {
		    if (posits[i] < 0 || posits[i] >= dims[i]) {
		        validSegment = false;
		        break;
		    }
		}
	    }
	    
	    if (validSegment) {
	        if (data != null) {
	            fillMemData(data, posits, segment, o, outputOffset, 0);
	        } else {
	            int offset = getOffset(dims, posits) * baseLength;
		    
		    // Point to offset at real beginning
		    // of segment
		    int actualLen = segment;
		    int actualOffset  = offset;
		    int actualOutput = outputOffset;
		    if (posits[mx] < 0) {
			actualOffset -= posits[mx]*baseLength;
			actualOutput -= posits[mx];
			actualLen    += posits[mx];
		    }
		    if (posits[mx] + segment > dims[mx]) { 
			actualLen -= posits[mx]+segment-dims[mx];
		    }
		    fillFileData(o, actualOffset, actualOutput, actualLen);
	        }
	    }
	    outputOffset += segment;
		
	} while (incrementPosition(corners, posits, lengths));
	if (data == null) {
	    f.seek(currentOffset);
	}
    }


    /** Fill a single segment from memory.
     *  This routine is called recursively to handle multi-dimensional
     *  arrays.  E.g., if data is three-dimensional, this will
     *  recurse two levels until we get a call with a single dimensional
     *  datum.  At that point the appropriate data will be copied
     *  into the output.
     * 
     *  @param data	The in-memory image data.
     *  @param posits	The current position for which data is requested.
     *  @param length	The size of the segments.
     *  @param output	The output tile.
     *  @param outputOffset The current offset into the output tile.
     *  @param dim	The current dimension being
     */
    protected void fillMemData(Object data, int[] posits, int length, 
			    Object output, int outputOffset, int dim) {
	
	
	if (data instanceof Object[]) {
	
	    Object[] xo = (Object[]) data;
	    fillMemData(xo[posits[dim]], posits, length, output, outputOffset, dim+1);
	    
	} else {
	    
	    // Adjust the spacing for the actual copy.
	    int startFrom = posits[dim];
	    int startTo   = outputOffset;
	    int copyLength= length;
	    
	    if (posits[dim] < 0) {
		startFrom -= posits[dim];
		startTo   -= posits[dim];
		copyLength+= posits[dim];
	    }
	    if (posits[dim] + length > dims[dim]) {
		copyLength -= (posits[dim]+length-dims[dim]);
	    }
	    
	    System.arraycopy(data, startFrom, output, startTo, copyLength);
	}
    }
    
    /** File a tile segment from a file.
     *  @param output		The output tile.
     *  @param delta		The offset from the beginning of the image in bytes.
     *  @param outputOffset 	The index into the output array.
     *  @param segment		The number of elements to be read for this segment.
     */
    protected void fillFileData(Object output, int delta, int outputOffset,
				int segment) throws IOException {
				    

	f.seek(fileOffset+delta);
				    
	if (base == float.class) {
	    f.read((float[]) output, outputOffset, segment);
	} else if (base == int.class) {
	    f.read((int[]) output, outputOffset, segment);
	} else if (base == short.class) {
	    f.read((short[]) output, outputOffset, segment);
	} else if (base == double.class) {
	    f.read((double[]) output, outputOffset, segment);
	} else if (base == byte.class) {
	    f.read((byte[]) output, outputOffset, segment);
	} else if (base == char.class) {
	    f.read((char[]) output, outputOffset, segment);
	} else if (base == long.class) {
	    f.read((long[]) output, outputOffset, segment);
	} else {
	    throw new IOException("Invalid type for tile array");
	}
    }

	


    /** Increment the offset within the position array.
     *  Note that we never look at the last index since
     *  we copy data a block at a time and not byte by byte.
     *  @param	start	The starting corner values.
     *  @param	current	The current offsets.
     *  @param  lengths The desired dimensions of the subset.
     */
    
    protected static boolean incrementPosition(int[] start, 
					       int[] current, 
					       int[] lengths) {
	
	for (int i=start.length-2; i >= 0; i -= 1) {
	    if (current[i]-start[i] < lengths[i]-1) {
		current[i] += 1;
		for (int j=i+1; j<start.length-1; j += 1) {
		    current[j] = start[j];
		}
	        return true;
	    }
	}
	return false;
    }
	

    /** Get the offset of a given position.
     *  @param dims  The dimensions of the array.
     *  @param pos   The index requested.
     */
    public static final int getOffset(int[] dims, int[] pos) {
	    
        int offset =  0;
        for (int i=0; i < dims.length; i += 1) {
     	    if (i > 0) {
		offset *= dims[i];
	    }
	    offset += pos[i];
	}
	return offset;
    }
    
    /** Read the entire image into a multidimensional
      * array.
      */
    public Object getCompleteImage() throws IOException {
	
	if (f == null) {
	    throw new IOException("Attempt to read from null file");
	}
	long currentOffset = f.getFilePointer();
	Object o = ArrayFuncs.newInstance(base, dims);
	f.seek(fileOffset);
	f.readArray(o);
	f.seek(currentOffset);
	return o;
    }
	
}
    
