package nom.tam.fits;

import nom.tam.util.*;
import java.io.*;


/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * 
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */


    
/** This class provides a simple holder for data which is
  * not handled by other classes.
  */
public class UndefinedData extends Data {
    
    /** The size of the data */
    long byteSize;
    
    byte[] data;
   
    
    public UndefinedData(Header h) throws FitsException {
	
	/** Just get a byte buffer to hold the data.
	 */
	int size=1;
	for (int i=0; i< h.getIntValue("NAXIS"); i += 1) {
	    size *= h.getIntValue("NAXIS"+(i+1));
	}
	size += h.getIntValue("PCOUNT");
	if (h.getIntValue("GCOUNT") > 1) {
	    size *= h.getIntValue("GCOUNT");
	}
	size *= Math.abs(h.getIntValue("BITPIX")/8);
	
	data      = new byte[size];
	byteSize = size;
    }
       
	
    /** Create an UndefinedData object using the specified object.
      */
    public UndefinedData(Object x) {
	
	byteSize = ArrayFuncs.computeSize(x);
	data = new byte[(int)byteSize];
    }
    
    /** Fill header with keywords that describe data.
      * @param head The FITS header
      */
    protected void fillHeader(Header head) {

	try {
            head.setXtension("UNKNOWN");
            head.setBitpix(8);
            head.setNaxes(1);
	    head.addValue("NAXIS1", byteSize, " Number of Bytes ");
	    head.addValue("PCOUNT", 0, null);
	    head.addValue("GCOUNT", 1, null);
            head.addValue("EXTEND", true, "Extensions are permitted");  // Just in case!
	} catch (HeaderCardException e) {
	    System.err.println("Unable to create unknown header:"+e);
	}
	
    }
    
    public void read(ArrayDataInput i) throws FitsException {
	setFileOffset(i);

	if (i instanceof RandomAccess) {
	    try {
	        i.skipBytes( (int) byteSize);
	    } catch (IOException e) {
		throw new FitsException("Unable to skip over data:"+e);
	    }
	    
	} else {
	    try {
	        i.readFully(data);
	    } catch (IOException e) {
		throw new FitsException("Unable to read unknown data:"+e);
	    }
	    
	}
	
        int pad = FitsUtil.padding(getTrueSize());
	try {
	    if (i.skipBytes(pad) != pad) {
                throw new FitsException("Error skipping padding");
            }
	} catch (IOException e) {
	    throw new FitsException("Error reading unknown padding:"+e);
	}
    }
    
    public void write(ArrayDataOutput o) throws FitsException {

        if (data == null) {
	    getData();
	}
	
	if (data == null) {
	    throw new FitsException("Null unknown data");
	}
	
	try {
	     o.write(data);
	} catch (IOException e) {
	    throw new FitsException("IO Error on unknown data write"+e);
	}
	
        byte[] padding = new byte[FitsUtil.padding(getTrueSize())];
        try {
            o.write(padding);
        } catch (IOException e) {
            throw new FitsException ("Error writing padding: "+e);
        }
	
    }

    /** Get the size in bytes of the data */
    protected int getTrueSize() {
	return (int) byteSize;
    }
    
    /** Return the actual data.
     *  Note that this may return a null when
     *  the data is not readable.  It might be better
     *  to throw a FitsException, but this is
     *  a very commonly called method and we prefered
     *  not to change how users must invoke it.
     */
    public Object getData()  {
	
	if (data == null) {
	    
	    try {
		FitsUtil.reposition(input, fileOffset);
		input.read(data);
	    } catch (Exception e) {
		return null;
	    }
	}
	
	return data;
    }
    
}
