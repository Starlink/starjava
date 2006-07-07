package nom.tam.fits;

import java.lang.reflect.Array;
import nom.tam.image.ImageTiler;
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


    
/** This class instantiates FITS primary HDU and IMAGE extension data.
  * Essentially these data are a primitive multi-dimensional array.
  * <p>
  * Starting in version 0.9 of the FITS library, this routine
  * allows users to defer the reading of images if the FITS
  * data is being read from a file.  An ImageTiler object is
  * supplied which can return an arbitrary subset of the image
  * as a one dimensional array -- suitable for manipulation by
  * standard Java libraries.  A call to the getData() method
  * will still return a multi-dimensional array, but the
  * image data will not be read until the user explicitly requests.
  * it.
  */
public class ImageData extends Data {
    
    /** The size of the data */
    long byteSize;
    
    /** The actual array of data.  This
     *  is normally a multi-dimensional primitive array.
     *  It may be null until the getData() routine is
     *  invoked, or it may be filled by during the read
     *  call when a non-random access device is used.
     */
    Object dataArray;
    
    /** This class describes an array */
    protected class ArrayDesc {
	
	int[] dims;
	Class type;
	
	ArrayDesc(int[] dims, Class type) {
	    this.dims = dims;
	    this.type = type;
	}
	
    }
    
    /** A description of what the data should look like */
    ArrayDesc dataDescription;
    
    /** This inner class allows the ImageTiler
     *  to see if the user has read in the data.
     */
    protected class ImageDataTiler extends nom.tam.image.ImageTiler {
	
	ImageDataTiler(RandomAccess o, long offset, ArrayDesc d) {
	    super(o, offset, d.dims, d.type);
	}
	
	public Object getMemoryImage() {
	    return dataArray;
	}
    }
    
    /** The image tiler associated with this image. */
    private ImageTiler tiler;

    
    /** Create an array from a header description.
      * This is typically how data will be created when reading
      * FITS data from a file where the header is read first.
      * This creates an empty array.
      * @param h header to be used as a template.
      * @exception FitsException if there was a problem with the header description.
      */
    public ImageData(Header h) throws FitsException {
	
	dataDescription = parseHeader(h);
       
    }
    
    protected ArrayDesc parseHeader(Header h) throws FitsException {

 	int bitpix;
    	int type;
	int ndim;
	int[] dims;

	int i;
	
	Object dataArray;

        Class baseClass;
	

	int gCount = h.getIntValue("GCOUNT",1);
	int pCount = h.getIntValue("PCOUNT",0);
	if (gCount > 1  || pCount != 0) {
              throw new FitsException("Group data treated as images");
        }

	bitpix = (int) h.getIntValue("BITPIX", 0);
	
	if (bitpix == 8) {
	    baseClass = Byte.TYPE;
	} else if (bitpix == 16) {
	    baseClass = Short.TYPE;
	} else if (bitpix == 32) {
	    baseClass = Integer.TYPE;
	} else if (bitpix == 64) {  /* This isn't a standard for FITS yet...*/
	    baseClass = Long.TYPE;
	} else if (bitpix == -32) {
	    baseClass = Float.TYPE;
	} else if (bitpix == -64) {
	    baseClass = Double.TYPE;
	} else {
	    throw new FitsException("Invalid BITPIX:"+bitpix);
	}

	ndim = h.getIntValue("NAXIS", 0) ;
	dims = new int[ndim];


     	// Note that we have to invert the order of the axes
	// for the FITS file to get the order in the array we
        // are generating.

	byteSize = 1;
	for (i=0; i<ndim; i += 1) {
	    int cdim = h.getIntValue("NAXIS"+(i+1), 0);
            if (cdim < 0) {
                throw new FitsException("Invalid array dimension:"+cdim);
            }
	    byteSize *= cdim;
	    dims[ndim-i-1] = (int) cdim;
	}
	byteSize *= Math.abs(bitpix)/8;
	if (ndim == 0) {
	    byteSize = 0;
	}
	return new ArrayDesc(dims, baseClass);
    }

    /** Create the equivalent of a null data element.
      */
    public ImageData() {
        dataArray = new byte[0];
	byteSize = 0;
    }

    /** Create an ImageData object using the specified object to
      * initialize the data array.
      * @param x The initial data array.  This should be a primitive
      *          array but this is not checked currently.
      */
    public ImageData(Object x) {
        dataArray = x;
	byteSize = ArrayFuncs.computeSize(x);
    }
    
    /** Fill header with keywords that describe
      * image data.
      * @param head The FITS header
      * @exception FitsException if the object does not contain
      *		valid image data.
      */
    protected void fillHeader(Header head) throws FitsException {
	

        if (dataArray == null) {
            head.nullImage();
	    return;
        }

        String classname = dataArray.getClass().getName();

        int[] dimens = ArrayFuncs.getDimensions(dataArray);
	
        if (dimens == null || dimens.length == 0) {
            throw new FitsException("Image data object not array");
        }

	
        int bitpix;
        switch (classname.charAt(dimens.length)) {
          case 'B':
            bitpix = 8;
            break;
          case 'S':
            bitpix = 16;
            break;
          case 'I':
            bitpix = 32;
            break;
          case 'J':
            bitpix = 64;
            break;
          case 'F':
            bitpix = -32;
            break;
          case 'D':
            bitpix = -64;
            break;
          default:
            throw new FitsException("Invalid Object Type for FITS data:"+
				    classname.charAt(dimens.length));
        }

	// if this is neither a primary header nor an image extension,
	// make it a primary header
        head.setSimple(true);
        head.setBitpix(bitpix);
        head.setNaxes(dimens.length);

        for (int i=1; i<=dimens.length; i += 1) {
            if (dimens[i-1] == -1) {
                throw new FitsException("Unfilled array for dimension: "+i);
            }
            head.setNaxis(i, dimens[dimens.length-i]);
        }
        head.addValue("PCOUNT", 0, "No extra parameters");
        head.addValue("GCOUNT", 1, "One group");
        head.addValue("EXTEND", true, "Extensions are permitted");  // Just in case!
    }
    
    public void read(ArrayDataInput i) throws FitsException {
	
	// Don't need to read null data (noted by Jens Knudstrup)
	if (byteSize == 0) {
	    return;
	}
	setFileOffset(i);

	if (i instanceof RandomAccess) {
	    tiler = new ImageDataTiler((RandomAccess) i, 
				       ((RandomAccess)i).getFilePointer(),
	    				dataDescription);
	    try {
	        i.skipBytes( (int) byteSize);
	    } catch (IOException e) {
		throw new FitsException("Unable to skip over data:"+e);
	    }
	    
	} else {
            dataArray = ArrayFuncs.newInstance(dataDescription.type,
					  dataDescription.dims);
	    try {
	        i.readArray(dataArray);
	    } catch (IOException e) {
		throw new FitsException("Unable to read image data:"+e);
	    }
	    
	    tiler = new ImageDataTiler(null, 0, dataDescription);
	}
	
        int pad = FitsUtil.padding(getTrueSize());
	try {
	    if (i.skipBytes(pad) != pad) {
                throw new FitsException("Error skipping padding");
            }
	} catch (IOException e) {
	    throw new FitsException("Error reading image padding:"+e);
	}
    }
    
    public void write(ArrayDataOutput o) throws FitsException {
	
	// Don't need to write null data (noted by Jens Knudstrup)
	if (byteSize == 0) {
	    return;
	}

        if (dataArray == null && tiler != null) {
	    try {
	        dataArray = tiler.getCompleteImage();
	    } catch (IOException e) {
		throw new FitsException("Error attempting to fill image");
	    }
	}
	if (dataArray == null) {
	    throw new FitsException("Null image data");
	}
	
	try {
	     o.writeArray(dataArray);
	} catch (IOException e) {
	    throw new FitsException("IO Error on image write"+e);
	}
	
        byte[] padding = new byte[FitsUtil.padding(getTrueSize())];
        try {
            o.write(padding);
	    o.flush();
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
	
	if (dataArray == null  && tiler != null) {
	    try {
		dataArray = tiler.getCompleteImage();
	    } catch (Exception e) {
		return null;
	    }
	}
	
	return dataArray;
    }
    
    public ImageTiler getTiler() {
	return tiler;
    }
    
}
