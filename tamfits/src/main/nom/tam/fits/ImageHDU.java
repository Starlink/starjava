package nom.tam.fits;
/* Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.image.ImageTiler;

/** FITS image header/data unit */
public class ImageHDU
	extends BasicHDU
{
    
  /** Build an image HDU using the supplied data.
    * @param obj the data used to build the image.
    * @exception FitsException if there was a problem with the data.
    */
  public ImageHDU(Header h, Data d)
	throws FitsException
  {
    myData   = d;
    myHeader = h;

  }
   
  /** Indicate that Images can appear at the beginning of a FITS dataset */
  protected boolean canBePrimary() {
      return true;
  }
  
  /** Change the Image from/to primary */
  protected void setPrimaryHDU(boolean status) {
      
      try {
          super.setPrimaryHDU(status);
      } catch (FitsException e) {
	  System.err.println("Impossible exception in ImageData");
      }
      
      if (status) {
	  myHeader.setSimple(true);
      } else {
	  myHeader.setXtension("IMAGE");
      }
  }
      
    /** Check that this HDU has a valid header for this type.
      * @return <CODE>true</CODE> if this HDU has a valid header.
      */
    public static boolean isHeader(Header hdr)
    {
      boolean found = false;
      found= hdr.getBooleanValue("SIMPLE");
      if (!found) {
	  String s = hdr.getStringValue("XTENSION");
	  if (s != null) {
	      if (s.trim().equals("IMAGE") || s.trim().equals("IUEIMAGE")) {
		  found = true;
	      }
	  }
      }
      if (!found) {
	  return false;
      }
      return !hdr.getBooleanValue("GROUPS");
    }
    
    /** Check if this object can be described as a FITS image.
     *  @param o    The Object being tested.
     */
    public static boolean isData(Object o) {
	String s = o.getClass().getName();
	
	int i;
	for (i=0; i<s.length(); i += 1) {
	    if (s.charAt(i) != '[') {
		break;
	    }
	}
	
	// Allow all non-boolean/Object arrays.
	// This does not check the rectangularity of the array though.
	if (i <= 0 || s.charAt(i) == 'L' || s.charAt(i) == 'Z') {
	    return false;
	} else {
	    return true;
	}
    }
		
	  
    /** Create a Data object to correspond to the header description.
     * @return An unfilled Data object which can be used to read
     *         in the data for this HDU.
     * @exception FitsException if the image extension could not be created.
     */
    public Data manufactureData()
	throws FitsException
    {
        return manufactureData(myHeader);
    }
   
    public static Data manufactureData(Header hdr)
        throws FitsException {
        return new ImageData(hdr);
    }
    
   /** Create a  header that describes the given
     * image data.
      * @param o The image to be described.
      * @exception FitsException if the object does not contain
      *		valid image data.
      */
    public static Header manufactureHeader(Data d) 
      throws FitsException {
	
        if (d == null) {
            return null;
        }
	
	Header h = new Header();
	d.fillHeader(h);
	
	return h;
    }
    
    /** Encapsulate an object as an ImageHDU. */
    public static Data encapsulate(Object o) throws FitsException {
	return new ImageData(o);
    }
	

    public ImageTiler getTiler() {
	return( (ImageData) myData).getTiler();
    }
    
  /** Print out some information about this HDU.
    */
  public void info() {
    if (isHeader(myHeader)) {
      System.out.println("  Image");
    } else {
      System.out.println("  Image (bad header)");
    }

    System.out.println("      Header Information:");
    System.out.println("         BITPIX="+myHeader.getIntValue("BITPIX",-1));
    int naxis = myHeader.getIntValue("NAXIS", -1);
    System.out.println("         NAXIS="+naxis);
    for (int i=1; i<=naxis; i += 1) {
      System.out.println("         NAXIS"+i+"="+
			 myHeader.getIntValue("NAXIS"+i,-1));
    }

    System.out.println("      Data information:");
    try {
        if (myData.getData() == null) {
          System.out.println("        No Data");
        } else {
          System.out.println("         "+
			 ArrayFuncs.arrayDescription(myData.getData()));
        }
    } catch (Exception e) {
	System.out.println("      Unable to get data");
    }
  }
    
    
}
