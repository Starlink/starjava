package nom.tam.fits;
/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import nom.tam.util.ArrayFuncs;

/** Holder for unknown data types. */
public class UndefinedHDU
	extends BasicHDU
{
    
  /** Build an image HDU using the supplied data.
    * @param obj the data used to build the image.
    * @exception FitsException if there was a problem with the data.
    */
  public UndefinedHDU(Header h, Data d)
	throws FitsException
  {
    myData   = d;
    myHeader = h;

  }
    
    /* Check if we can find the length of the data for this
     * header.
     * @return <CODE>true</CODE> if this HDU has a valid header.
     */
    public static boolean isHeader(Header hdr)
    {
	if (hdr.getStringValue("XTENSION") != null &&
	    hdr.getIntValue("NAXIS", -1) >= 0) {
	    return true;
	}
	return false;
    }
    
    /** Check if we can use the following object as
     *  in an Undefined FITS block.  We allow this
     *  so long as computeSize can get a size.  Note
     *  that computeSize may be wrong!
     *  @param o    The Object being tested.
     */
    public static boolean isData(Object o) {
	if (ArrayFuncs.computeSize(o) > 0) {
	    return true;
	}
	return false;
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
        return new UndefinedData(hdr);
    }
    
   /** Create a  header that describes the given
     * image data.
      * @param o The image to be described.
      * @exception FitsException if the object does not contain
      *		valid image data.
      */
    public static Header manufactureHeader(Data d) 
      throws FitsException {
	
   	Header h = new Header();
	d.fillHeader(h);
	
	return h;
    }
    
    /** Encapsulate an object as an ImageHDU. */
    public static Data encapsulate(Object o) throws FitsException {
	return new UndefinedData(o);
    }
	

    
  /** Print out some information about this HDU.
    */
  public void info() {
      
      System.out.println("  Unhandled/Undefined/Unknown Type");
      System.out.println("  XTENSION="+myHeader.getStringValue("XTENSION").trim());
      System.out.println("  Apparent size:"+myData.getTrueSize());
  }
    
}
