package nom.tam.fits;

/*
 * Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import nom.tam.util.*;


/** This class provides access to routines to allow users
  * to read and write FITS files.
  * <p>
  *
  * <p>
  * <b> Description of the Package </b>
  * <p>
  * This FITS package attempts to make using FITS files easy,
  * but does not do exhaustive error checking.  Users should
  * not assume that just because a FITS file can be read
  * and written that it is necessarily legal FITS.
  *
  *
  * <ul>
  * <li> The Fits class provides capabilities to
  *      read and write data at the HDU level, and to
  *      add and delete HDU's from the current Fits object.
  *      A large number of constructors are provided which
  *      allow users to associate the Fits object with
  *      some form of external data.  This external
  *      data may be in a compressed format.
  * <li> The HDU class is a factory class which is used to
  *      create HDUs.  HDU's can be of a number of types
  *      derived from the abstract class BasicHDU.
  *      The hierarchy of HDUs is:
  *      <ul>
  *      <li>BasicHDU
  *           <ul>
  *           <li> ImageHDU
  *           <li> RandomGroupsHDU
  *           <li> TableHDU
  *                <ul>
  *                <li> BinaryTableHDU
  *                <li> AsciiTableHDU
  *                </ul>
  *           </ul>
  *       </ul>
  *
  * <li> The Header class provides many functions to
  *      add, delete and read header keywords in a variety
  *      of formats.
  * <li> The HeaderCard class provides access to the structure
  *      of a FITS header card.
  * <li> The Data class is an abstract class which provides
  *      the basic methods for reading and writing FITS data.
  *      Users will likely only be interested in the getData
  *      method which returns that actual FITS data.
  * <li> The TableHDU class provides a large number of
  *      methods to access and modify information in
  *      tables.
  * <li> The Column class
  *      combines the Header information and Data corresponding to
  *      a given column.
  * </ul>
  *
  * Copyright: Thomas McGlynn 1997-1999.
  * This code may be used for any purpose, non-commercial
  * or commercial so long as this copyright notice is retained
  * in the source code or included in or referred to in any
  * derived software.
  *
  * @version 0.96a  October 10, 2000
  */
public class Fits {

    /** The input stream associated with this Fits object.
      */
    private ArrayDataInput dataStr;
    
    /** A vector of HDUs that have been added to this
      * Fits object.
      */
    private Vector hduList = new Vector();

    /** Has the input stream reached the EOF?
      */
    private boolean atEOF;

    /** Indicate the version of these classes */
    public static String version() {

         // Version 0.1: Original test FITS classes -- 9/96
         // Version 0.2: Pre-alpha release 10/97
         //              Complete rewrite using BufferedData*** and
         //              ArrayFuncs utilities.
         // Version 0.3: Pre-alpha release  1/98
         //              Incorporation of HDU hierarchy developed
         //              by Dave Glowacki and various bug fixes.
         // Version 0.4: Alpha-release 2/98
         //              BinaryTable classes revised to use
         //              ColumnTable classes.
         // Version 0.5: Random Groups Data 3/98
         // Version 0.6: Handling of bad/skipped FITS, FitsDate (D. Glowacki) 3/98
	 // Version 0.9: ASCII tables, Tiled images, Faux, Bad and SkippedHDU class
         //              deleted. 12/99
	 // Version 0.91: Changed visibility of some methods.
	 //               Minor fixes.
	 // Version 0.92: Fix bug in BinaryTable when reading from stream.
	 // Version 0.93: Supports HIERARCH header cards.  Added FitsElement interface.
	 //               Several bug fixes especially for null HDUs.
	 // Version 0.96: Address issues with mandatory keywords.
	 //               Fix problem where some keywords were not properly keyed.
	 // Version 0.96a: Update version in FITS

         return "0.96a";
    }
        
    /** Create an empty Fits object which is not
      * associated with an input stream.
      */
    public Fits() {
    }
    

    /** Create a Fits object associated with
      * the given uncompressed data stream.
      * @param str The data stream.
      */
    public Fits(InputStream str) throws FitsException {
        this(str, false);
    }

    /** Create a Fits object associated with a possibly
      * compressed data stream.
      * @param str The data stream.
      * @param compressed Is the stream compressed?
      */
    public Fits(InputStream str, boolean compressed)
                throws FitsException {
        streamInit(str, compressed, false);
    }

    /** Do the stream initialization.
      *
      * @param str The input stream.
      * @param compressed Is this data compressed?  If so,
      *            then the GZIPInputStream class will be
      *            used to inflate it.
      */
    protected void streamInit(InputStream str, boolean compressed,
			      boolean seekable)
                              throws FitsException {

      if (str == null) {
          throw new FitsException("Null input stream");
      }

      if (compressed) {
          try {
              str = new GZIPInputStream(str);
          } catch (IOException e) {
              throw new FitsException("Cannot inflate input stream"+e);
          }
      }

      if (str instanceof ArrayDataInput) {
          dataStr = (ArrayDataInput) str;
      } else {
	  // Use efficient blocking for input.
          dataStr = new BufferedDataInputStream(str);
      }
    }
    
    /** Initialize using buffered random access */
    protected void randomInit(String filename) throws FitsException {
	
	String permissions = "r";
	File f = new File(filename);
	if (!f.exists()  || !f.canRead()) {
	    throw new FitsException("Non-existent or unreadable file");
	}
	if (f.canWrite()) {
	    permissions += "w";
	}
	try {
	    dataStr = new BufferedFile(filename, permissions);
	    
	    ((BufferedFile)dataStr).seek(0);
	} catch (IOException e) {
	    throw new FitsException("Unable to open file "+filename);
	}
    }
	    
    /** Associate FITS object with an uncompressed File
      * @param myFile The File object.
      */
    public Fits(File myFile) throws FitsException {
        this(myFile, false);
    }

    /** Associate the Fits object with a File
      * @param myFile The File object.
      * @param compressed Is the data compressed?
      */
    public Fits(File myFile, boolean compressed) throws FitsException {
        fileInit(myFile, compressed);
    }

    /** Get a stream from the file and then use the stream initialization.
      * @param myFile  The File to be associated.
      * @param compressed Is the data compressed?
      */
    protected void fileInit(File myFile, boolean compressed) throws FitsException {

        try {
            FileInputStream str = new FileInputStream(myFile);
            streamInit(str, compressed, true);
        } catch (IOException e) {
              throw new FitsException("Unable to create Input Stream from File: "+myFile);
        }
    }

    /** Associate the FITS object with a file or URL.
      *
      * The string is assumed to be a URL if it begins with
      * http:  otherwise it is treated as a file name.
      * If the string ends in .gz it is assumed that
      * the data is in a compressed format.
      * All string comparisons are case insensitive.
      *
      * @param filename  The name of the file or URL to be processed.
      * @exception FitsException Thrown if unable to find or open
      *                          a file or URL from the string given.
      **/
    public Fits(String filename) throws FitsException {

      InputStream inp;

      if (filename == null) {
          throw new FitsException("Null FITS Identifier String");
      }

      boolean compressed = FitsUtil.isCompressed(filename);

      int len = filename.length();
      if (len > 4 && filename.substring(0,5).equalsIgnoreCase("http:") ) {
          // This seems to be a URL.
          URL myURL;
          try {
               myURL = new URL(filename);
          } catch (IOException e) {
               throw new FitsException ("Unable to convert string to URL: "+filename);
          }
          try {
              InputStream is = myURL.openStream();
              streamInit(is, compressed, false);
          } catch (IOException e) {
              throw new FitsException ("Unable to open stream from URL:"+filename+" Exception="+e);
          }
      } else if (compressed) {
          fileInit(new File(filename), true);
      } else {
	  randomInit(filename);
      }

    }

    /** Associate the FITS object with a given uncompressed URL
      * @param myURL  The URL to be associated with the FITS file.
      * @exception FitsException Thrown if unable to use the specified URL.
      */
    public Fits (URL myURL) throws FitsException {
        this(myURL, FitsUtil.isCompressed(myURL.getFile()));
    }

    /** Associate the FITS object with a given URL
      * @param myURL  The URL to be associated with the FITS file.
      * @param compressed Is the data compressed?
      * @exception FitsException Thrown if unable to find or open
      *                          a file or URL from the string given.
      */
    public Fits (URL myURL, boolean compressed) throws FitsException {
	  try {
            streamInit(myURL.openStream(), compressed, false);
        } catch (IOException e) {
            throw new FitsException("Unable to open input from URL:"+myURL);
        }
    }

    /** Return all HDUs for the Fits object.   If the
      * FITS file is associated with an external stream make
      * sure that we have exhausted the stream.
      * @return an array of all HDUs in the Fits object.  Returns
      * null if there are no HDUs associated with this object.
      */

    public BasicHDU[] read() throws FitsException {

      readToEnd();

      int size = getNumberOfHDUs();
	
      if (size == 0) {
          return null;
      }

      BasicHDU[] hdus = new BasicHDU[size];
      hduList.copyInto(hdus);
      return hdus;
    }

    /** Read the next HDU on the default input stream.
      * @return The HDU read, or null if an EOF was detected.
      * Note that null is only returned when the EOF is detected immediately
      * at the beginning of reading the HDU.
      */
    public BasicHDU readHDU() throws FitsException, IOException {

        if (dataStr == null || atEOF) {
            return null;
        }

        Header hdr   = Header.readHeader(dataStr);
        if (hdr == null) {
	    atEOF = true;
	    return null;
        }
	
        Data  datum = hdr.makeData();
        datum.read(dataStr);
        BasicHDU nextHDU = FitsFactory.HDUFactory(hdr, datum);
	
        hduList.addElement(nextHDU);
        return nextHDU;
    }

    /** Skip HDUs on the associate input stream.
      * @param n The number of HDUs to be skipped.
      */
    public void skipHDU(int n) throws FitsException, IOException {
        for (int i=0; i<n; i += 1) {
            skipHDU();
        }
    }

    /** Skip the next HDU on the default input stream.
      */
    public void skipHDU() throws FitsException, IOException {

	if (atEOF) {
	    return;
	} else {
            Header hdr = new Header(dataStr);
	    if (hdr == null) {
		atEOF = true;
		return;
	    }
	    int dataSize = (int) hdr.getDataSize();
	    dataStr.skip(dataSize);
        }
    }

   /** Return the n'th HDU.
     * If the HDU is already read simply return a pointer to the
     * cached data.  Otherwise read the associated stream
     * until the n'th HDU is read.
     * @param n The index of the HDU to be read.  The primary HDU is index 0.
     * @return The n'th HDU or null if it could not be found.
     */
    public BasicHDU getHDU(int n) throws FitsException, IOException {

        int size = getNumberOfHDUs();
	
        for (int i=size; i <= n; i += 1) {
	    BasicHDU hdu;
	    hdu = readHDU();
            if (hdu == null) {
                return null;
            }
        }

        try {
            return (BasicHDU) hduList.elementAt(n);
        } catch (NoSuchElementException e) {
            throw new FitsException("Internal Error: hduList build failed");
        }
    }

    /** Read to the end of the associated input stream */
    private void readToEnd() throws FitsException {
	
        while (dataStr != null && !atEOF) {
            try {
	        if (readHDU() == null) {
                    break;
                }
            } catch (IOException e) {
                throw new FitsException("IO error: "+e);
            }
        }
    }


    /** Return the number of HDUs in the Fits object.   If the
      * FITS file is associated with an external stream make
      * sure that we have exhausted the stream.
      * @return number of HDUs.
      * @deprecated The meaning of size of ambiguous.  Use
      */
    public int size() throws FitsException {
        readToEnd();
        return getNumberOfHDUs();
    }
    
    /** Add an HDU to the Fits object.  Users may intermix
      * calls to functions which read HDUs from an associated
      * input stream with the addHDU and insertHDU calls,
      * but should be careful to understand the consequences.
      *
      * @param myHDU  The HDU to be added to the end of the FITS object.
      */
    public void addHDU(BasicHDU myHDU)
	throws FitsException
    {
	insertHDU(myHDU, getNumberOfHDUs());
    }


    /** Insert a FITS object into the list of HDUs.
      *
      * @param myHDU The HDU to be inserted into the list of HDUs.
      * @param n     The location at which the HDU is to be inserted.
      */

    public void insertHDU(BasicHDU myHDU, int n)
	throws FitsException
    {

        if (myHDU == null) {
            return;
        }

        if (n < 0 || n > getNumberOfHDUs()) {
            throw new FitsException("Attempt to insert HDU at invalid location: "+n);
        }
	
	try {
	
	    if (n == 0) {
		
		// Note that the previous initial HDU is no longer the first.
		// If we were to insert tables backwards from last to first,
		// we could get a lot of extraneous DummyHDUs but we currently
		// do not worry about that.

		if (getNumberOfHDUs() > 0) {
		    ((BasicHDU) hduList.elementAt(0)).setPrimaryHDU(false);
		}
	    
	        if (myHDU.canBePrimary() ) {
		    myHDU.setPrimaryHDU(true);
		    hduList.insertElementAt(myHDU, 0);
	        } else {
		    insertHDU(BasicHDU.getDummyHDU(), 0);
		    myHDU.setPrimaryHDU(false);
		    hduList.insertElementAt(myHDU, 1);
		}
	    } else {
		myHDU.setPrimaryHDU(false);
		hduList.insertElementAt(myHDU, n);
	    }
        } catch (NoSuchElementException e) {
	    throw new FitsException("hduList inconsistency in insertHDU");
	}
	
    }

    /** Delete an HDU from the HDU list.
      *
      * @param n  The index of the HDU to be deleted.
      *           If n is 0 and there is more than one HDU present, then
      *           the next HDU will be converted from an image to
      *           primary HDU if possible.  If not a dummy header HDU
      *           will then be inserted.
      */
    public void deleteHDU(int n) throws FitsException  {
      int size = getNumberOfHDUs();
      if (n < 0 || n >= size) {
          throw new FitsException("Attempt to delete non-existent HDU:"+n);
      }
      try {
          hduList.removeElementAt(n);
          if (n == 0 && size > 1) {
	      BasicHDU newFirst = (BasicHDU) hduList.elementAt(0);
	      if (newFirst.canBePrimary()) {
	          newFirst.setPrimaryHDU(true);
	      } else {
		  insertHDU(BasicHDU.getDummyHDU(), 0);
	      }
          }
      } catch (NoSuchElementException e) {
          throw new FitsException("Internal Error: hduList Vector Inconsitency");
      }
    }

    /** Write a Fits Object to an external Stream.
      *
      * @param dos  A DataOutput stream.
      */
    public void write(DataOutput os) throws FitsException {
	
        ArrayDataOutput obs;
        boolean newOS = false;

        if (os instanceof ArrayDataOutput) {
            obs = (ArrayDataOutput) os;
        } else if (os instanceof DataOutputStream) {
	    newOS = true;
            obs = new BufferedDataOutputStream((DataOutputStream)os);
        } else {
	    throw new FitsException("Cannot create ArrayDataOutput from class "+
				      os.getClass().getName());
	}

	BasicHDU  hh;
	for (int i=0; i<getNumberOfHDUs(); i += 1) {
	    try {
		hh = (BasicHDU) hduList.elementAt(i);
	        hh.write(obs);
	    } catch (ArrayIndexOutOfBoundsException e) {
		e.printStackTrace();
		throw new FitsException("Internal Error: Vector Inconsistency"+e);
	    }
	}
	if (newOS) {
	    try {
	        obs.flush();
	        obs.close();
	    } catch (IOException e) {
		System.err.println("Warning: error closing FITS output stream");
	    }
	}

    }

    /** Read a FITS file from an InputStream object.
      *
      * @param is The InputStream stream whence the FITS information
      *            is found.
      */
    public void read(InputStream is) throws FitsException, IOException {
	
	boolean newIS = false;

        if (is instanceof ArrayDataInput) {
	    dataStr = (ArrayDataInput) is;
        } else {
            dataStr = new BufferedDataInputStream(is);
        }
	
        read();
	
	if (newIS) {
	    dataStr.close();
	    dataStr = null;
	}
	
    }

   /** Get the current number of HDUs in the Fits object.
     * @return The number of HDU's in the object.
     * @deprecated See getNumberOfHDUs()
     */
    public int currentSize() {
        return hduList.size();
    }
    
   /** Get the current number of HDUs in the Fits object.
     * @return The number of HDU's in the object.
     */
    public int getNumberOfHDUs() {
	return hduList.size();
    }

    /** Get the data stream used for the Fits Data.
      * @return The associated data stream.  Users may wish to
      *         call this function after opening a Fits object when
      *         they wish detailed control for writing some part of the FITS file.
      */

    public ArrayDataInput getStream() {
        return dataStr;
    }

    /** Set the data stream to be used for future input.
      *
      * @param stream The data stream to be used.
      */
    public void setStream(ArrayDataInput stream) {
        dataStr = stream;
        atEOF = false;
    }
    
    /** Create an HDU from the given header.
     *  @param h  The header which describes the FITS extension
     */
    public static BasicHDU makeHDU(Header h) throws FitsException {
	Data d = FitsFactory.dataFactory(h);
	return FitsFactory.HDUFactory(h, d);
    }
    
    /** Create an HDU from the given data kernel.
     *  @param o The data to be described in this HDU.
     */
    public static BasicHDU makeHDU(Object o) throws FitsException {
        return FitsFactory.HDUFactory(o);
    }

    /** Create an HDU from the given Data.
     *  @param datum The data to be described in this HDU.
     */
    public static BasicHDU makeHDU(Data datum) throws FitsException {
	Header hdr = new Header();
	datum.fillHeader(hdr);
        return FitsFactory.HDUFactory(hdr, datum);
    }

}
