package nom.tam.fits;

/** This class contains the code which
 *  associates particular FITS types with header
 *  and data configurations.  It comprises
 *  a set of Factory methods which call
 *  appropriate methods in the HDU classes.
 *  If -- God forbid -- a new FITS HDU type were
 *  created, then the XXHDU, XXData classes would
 *  need to be added and this file modified but
 *  no other changes should be needed in the FITS libraries.
 * 
 */

public class FitsFactory {
    
    private static boolean useAsciiTables = true;
    private static boolean useHierarch = false;
    
    /** Indicate whether ASCII tables should be used
     *  where feasible.
     */
    public static void setUseAsciiTables(boolean flag) {
	useAsciiTables  = flag;
    }
    
    /** Get the current status of ASCII table writing */
    static boolean getUseAsciiTables() {
	return useAsciiTables;
    }
    
    /** Enable/Disable hierarchical keyword processing. */
    public static void setUseHierarch(boolean flag) {
	useHierarch = flag;
    }
    
    /** Are we processing HIERARCH style keywords */
    public static boolean getUseHierarch() {
	return useHierarch;
    }
    
    
    /** Given a Header return an appropriate datum.
     */
    
    static Data dataFactory(Header hdr) throws FitsException {
	
	if (ImageHDU.isHeader(hdr)) {
	    return ImageHDU.manufactureData(hdr);
	} else if (RandomGroupsHDU.isHeader(hdr)) {
	    return RandomGroupsHDU.manufactureData(hdr);
	} else if (useAsciiTables && AsciiTableHDU.isHeader(hdr)) {
	    return AsciiTableHDU.manufactureData(hdr);
	} else if (BinaryTableHDU.isHeader(hdr)) {
	    return BinaryTableHDU.manufactureData(hdr);
	} else if (UndefinedHDU.isHeader(hdr)) {
	    return UndefinedHDU.manufactureData(hdr);
	} else {
	    throw new FitsException("Unrecognizable header in dataFactory");
	}
	
    }
    
    /** Given an object, create the appropriate
     *  FITS header to describe it.
     *  @param	o 	The object to be described.
     */
    static BasicHDU HDUFactory(Object o) throws FitsException {
	Data   d;
	Header h;
	if (ImageHDU.isData(o)) {
	    d = ImageHDU.encapsulate(o);
	    h = ImageHDU.manufactureHeader(d);
	} else if (RandomGroupsHDU.isData(o)) {
	    d = RandomGroupsHDU.encapsulate(o);
	    h = RandomGroupsHDU.manufactureHeader(d);
	} else if (useAsciiTables && AsciiTableHDU.isData(o)) {
	    d = AsciiTableHDU.encapsulate(o);
	    h = AsciiTableHDU.manufactureHeader(d);
	} else if (BinaryTableHDU.isData(o)) {
	    d = BinaryTableHDU.encapsulate(o);
	    h = BinaryTableHDU.manufactureHeader(d);
	} else if (UndefinedHDU.isData(o)) {
	    d = UndefinedHDU.encapsulate(o);
	    h = UndefinedHDU.manufactureHeader(d);
	} else {
	    throw new FitsException("Invalid data presented to HDUFactory");
	}
	
	return HDUFactory(h,d);
	  
    }
    
    /** Given Header and data objects return
     *  the appropriate type of HDU.
     */
    static BasicHDU HDUFactory(Header hdr, Data d) throws
      FitsException {
	  
	if (d instanceof ImageData) {
	    return new ImageHDU(hdr, d);
	} else if (d instanceof RandomGroupsData) {
	    return new RandomGroupsHDU(hdr, d);
	} else if (d instanceof AsciiTable) {
	    return new AsciiTableHDU(hdr, d);
	} else if (d instanceof BinaryTable) {
	    return new BinaryTableHDU(hdr, d);
	} else if (d instanceof UndefinedData) {
	    return new UndefinedHDU(hdr, d);
	}
      
	return null;
    }
}
    
    
