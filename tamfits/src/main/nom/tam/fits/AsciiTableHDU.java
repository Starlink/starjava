package nom.tam.fits;

import java.io.IOException;
import nom.tam.util.*;
import java.util.Iterator;


/*
 * Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * 
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

/**
 * FITS ASCII table header/data unit
 */
public class AsciiTableHDU extends TableHDU {

    /** Just a copy of myData with the correct type */
    AsciiTable data;
    
    /**
     * Create an ascii table header/data unit.
     * @param header the template specifying the ascii table.
     * @exception FitsException if there was a problem with the header.
     */
    
    public AsciiTableHDU(Header h, Data d) {
	super((TableData)d);
	myHeader =              h;
	data     = (AsciiTable) d;
	myData   =              d;
    }


    /**
     * Check that this is a valid ascii table header.
     * @param header to validate.
     * @return <CODE>true</CODE> if this is an ascii table header.
     */
    public static boolean isHeader(Header header) {
        return header.getStringValue("XTENSION").trim().equals("TABLE");
    } 

    /**
     * Check that this HDU has a valid header.
     * @return <CODE>true</CODE> if this HDU has a valid header.
     */
    public boolean isHeader() {
        return isHeader(myHeader);
    } 

    /** Check if this data is usable as an ASCII table.
     */
    public static boolean isData(Object o) {
	
	if (o instanceof Object[]) {
	    Object[] oo = (Object[]) o;
	    for (int i=0; i<oo.length; i += 1) {
	        if (oo[i] instanceof String[] ||
		    oo[i] instanceof int[]    ||
		    oo[i] instanceof long[]   ||
		    oo[i] instanceof float[]  ||
		    oo[i] instanceof double[]) {
		    continue;
		}
	        return false;
	    }
	    return true;
	} else {
	    return false;
	}
    }
		
    /**
     * Create a Data object to correspond to the header description.
     * @return An unfilled Data object which can be used to read
     * in the data for this HDU.
     * @exception FitsException if the Data object could not be created
     * from this HDU's Header
     */
    public static Data manufactureData(Header hdr) throws FitsException {
	return new AsciiTable(hdr);
    }
    
    public Data manufactureData() throws FitsException {
	return manufactureData(myHeader);
    }
    
    public static Header manufactureHeader(Data d) throws FitsException {
	Header hdr = new Header();
	d.fillHeader(hdr);
	Iterator iter = hdr.iterator();
	return hdr;
    }
    
    public static Data encapsulate(Object o) throws FitsException {
	
	Object[] oo = (Object[]) o;
	AsciiTable d =  new AsciiTable();
	for (int i=0; i<oo.length; i += 1) {
	    d.addColumn(oo[i]);
	}
	return d;
    }
	

    /**
     * Skip the ASCII table and throw an exception.
     * @param stream the stream from which the data is read.
     */
    public void readData(ArrayDataInput stream) 
            throws FitsException {
	myData.read(stream);
    } 
    
    /** Mark an entry as null.
     */
    public void setNull(int row, int col, boolean flag) {
	
	if (flag) {
	    String nullStr = myHeader.getStringValue("TNULL"+(col+1));
	    if (nullStr == null) {
		setNullString(col, "NULL");
	    }
	}
	data.setNull(row, col, flag);
    }
    
    /** See if an element is null */
    public boolean isNull(int row, int col) {
	return data.isNull(row, col);
    }
    
    /** Set the null string for a column */
    public void setNullString(int col, String newNull) {
	myHeader.positionAfterIndex("TBCOL", col+1);
	try {
	    myHeader.addValue("TNULL"+(col+1), newNull, null);
	} catch (HeaderCardException e) {
	    System.err.println("Impossible exception in setNullString"+e);
	}
	data.setNullString(col, newNull);
    }
    
    /** Add a column */
    public int addColumn(Object newCol) throws FitsException {
	
	data.addColumn(newCol);
	
	// Move the iterator to point after all the data describing
	// the previous column.

	Cursor iter=
	  myHeader.positionAfterIndex("TBCOL", data.getNCols());
	
	int rowlen    = data.addColInfo(getNCols(), iter);
	int oldRowlen = myHeader.getIntValue("NAXIS1");
	myHeader.setNaxis(1, rowlen+oldRowlen);
	
	int oldTfields = myHeader.getIntValue("TFIELDS");
	try {
	    myHeader.addValue("TFIELDS", oldTfields+1, null);
	} catch (Exception e) {
	    System.err.println("Impossible exception at addColumn:"+e);
	}
	return getNCols();
    }
    
    /**
     * Print a little information about the data set.
     */
    public void info() {
	System.out.println("ASCII Table:");
	System.out.println("  Header:");
	System.out.println("    Number of fields:"+myHeader.getIntValue("TFIELDS"));
	System.out.println("    Number of rows:  "+myHeader.getIntValue("NAXIS2"));
	System.out.println("    Length of row:   "+myHeader.getIntValue("NAXIS1"));
	System.out.println("  Data:");
	Object[] data = (Object[]) getKernel();
	for (int i=0; i<getNCols(); i += 1) {
	    System.out.println("      "+i+":"+ArrayFuncs.arrayDescription(data[i]));
	}
    } 
    
    public Data getData() {
	return data;
    }

}

