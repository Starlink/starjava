package nom.tam.fits;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import java.io.*;
import nom.tam.util.*;

/** This class provides methods to access the data segment of an
  * HDU.
  */

public abstract class Data implements FitsElement {

    /** This is the object which contains the actual data for the HDU.
      * <ul>
      *  <li> For images and primary data this is a simple (but possibly
      *       multi-dimensional) primitive array.  When group data is
      *       supported it will be a possibly multidimensional array
      *       of group objects.
      *  <li> For ASCII data it is a two dimensional Object array where
      *       each of the constituent objects is a primitive array of length 1.
      *  <li> For Binary data it is a two dimensional Object array where
      *       each of the constituent objects is a primitive array of arbitrary
      *       (more or less) dimensionality.
      *  </ul>
      */

    /** The starting location of the data when last read */
    protected long fileOffset = -1;
    /** The size of the data when last read */
    protected int dataSize;
    /** The inputstream used. */
    protected RandomAccess input;
    
    
    
    /** Get the file offset */
    public long getFileOffset()  {
	return fileOffset;
    }
    
    /** Set the fields needed for a re-read */
    protected void setFileOffset(Object o) {
	if (o instanceof RandomAccess) {
	    fileOffset = FitsUtil.findOffset(o);
	    dataSize   = getTrueSize();
	    input      = (RandomAccess) o;
	}
    }
    
    
    /** Write the data -- including any buffering needed
      * @param o  The output stream on which to write the data.
      */
    public abstract void write(ArrayDataOutput o) throws FitsException;

    /** Read a data array into the current object and if needed position
      * to the beginning of the next FITS block.
      * @param i The input data stream
      */
    public abstract void read(ArrayDataInput i) throws FitsException;
    
    public void rewrite() throws FitsException {
	
	if (!rewriteable()) {
	    throw new FitsException("Illegal attempt to rewrite data");
	}
	
	FitsUtil.reposition(input, fileOffset);
	write((ArrayDataOutput)input);
	try {
	    ((ArrayDataOutput)input).flush();
	} catch (IOException e) {
	    throw new FitsException("Error in rewrite flush: "+e);
	}
    }
    
    public boolean rewriteable() {
	if (input == null  ||
	    fileOffset < 0 ||
	    (getTrueSize() + 2879)/2880 != (dataSize + 2879)/2880)
	{
	    return false;
	} else {
	    return true;
	}
    }

    abstract int getTrueSize();
    
    /** Get the size of the data element in bytes */
    public long getSize() {
	return FitsUtil.addPadding(getTrueSize());
    }
    
    /** Return the data array object.
      */
    public abstract Object getData() throws FitsException ;
    
    /** Return the non-FITS data object */
    public Object getKernel() throws FitsException {
	return getData();
    }
    
    /** Modify a header to point to this data
     */
    abstract void fillHeader(Header head) throws FitsException;
    
}
