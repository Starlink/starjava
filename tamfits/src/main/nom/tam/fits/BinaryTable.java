package nom.tam.fits;

/* Copyright: Thomas McGlynn 1997-2000.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 *
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import java.io.*;
import nom.tam.util.*;
import java.lang.reflect.Array;
import java.util.Vector;


/** This class defines the methods for accessing FITS binary table data.
  */

public class BinaryTable extends Data implements TableData {
    
    /** This is the area in which variable length column data lives.
      */
    FitsHeap heap;
    
    /** The number of bytes between the end of the data and the heap */
    int      heapOffset;

    /** The sizes of each column (in number of entries per row)
      */
    int[] sizes;
    
    /** The dimensions of each column.
      */
    int[][] dimens;
    
    /** Info about column */
    int[] flags;
    final static int COL_CONSTANT = 0;
    final static int COL_VARYING  = 1;
    final static int COL_COMPLEX  = 2;
    final static int COL_STRING   = 4;
    final static int COL_BOOLEAN  = 8;
    final static int COL_BIT      = 16;
    
    /** The number of rows in the table.
      */
    int nRow;
    
    /** The number of columns in the table.
      */
    int nCol;
    
    /** The length in bytes of each row.
      */
    int rowLen;

    /** The start offset in bytes of each column from the start of the row.
     */
    int[] colOffsets;
    
    /** The base classes for the arrays in the table.
      */
    Class[] bases;
    
    /** An example of the structure of a row
      */
    Object[] modelRow;
    
    /** A pointer to the data in the columns.  This
     *  variable is only used to assist in the
     *  construction of BinaryTable's that are defined
     *  to point to an array of columns.  It is
     *  not generally filled.  The ColumnTable is used
     *  to store the actual data of the BinaryTable.
     */
    Object[] columns;

    /** Where the data is actually stored.
      */
    ColumnTable table;
    
    /** The stream used to input the image
     */
    ArrayDataInput currInput;

    /** Create a null binary table data segment.
      */
    public BinaryTable() throws FitsException {
	try { 
	    table    = new ColumnTable(new Object[0], new int[0]);
	} catch (TableException e) {
	    System.err.println("Impossible exception in BinaryTable() constructor"+e);
	}
	heap     = new FitsHeap(0);
	extendArrays(0);
	nRow     = 0;
	nCol     = 0;
	rowLen   = 0;
    }

    /** Create a binary table from given header information.
      *
      * @param header	A header describing what the binary
      *                 table should look like.
      */
    public BinaryTable(Header myHeader) throws FitsException {
	
      int heapSize = myHeader.getIntValue("PCOUNT");
      heapOffset   = myHeader.getIntValue("THEAP");
      nRow         = myHeader.getIntValue("NAXIS2");
      int rwsz     = myHeader.getIntValue("NAXIS1");
      
      // Subtract out the size of the regular table from
      // the heap offset.

      if (heapOffset > 0) {
	  heapOffset -= nRow*rwsz;
      }
	
      if (heapOffset > heapSize) {
	  throw new FitsException("Inconsistent THEAP and PCOUNT");
      }
	
      heap         = new FitsHeap(heapSize - heapOffset);
      nCol         = myHeader.getIntValue("TFIELDS");
      rowLen       = 0;
	
      extendArrays(nCol);
      for (int col=0; col<nCol; col += 1) {
          colOffsets[col] = rowLen;
	  rowLen += processCol(myHeader, col);
      }
	
    }
    
    private int processCol(Header header, int col) throws FitsException {
	
	String tform = header.getStringValue("TFORM"+(col+1)).trim();
	String tdims = header.getStringValue("TDIM"+(col+1));
	
	if (tform == null) {
	    throw new FitsException("No TFORM for column:"+col);
	}
	if (tdims != null) {
	    tdims = tdims.trim();
	}
	
	char  type = getTformType(tform);
	if (type == 'P') {
	    flags[col] |= COL_VARYING;
	    type = getTformVarType(tform);
	}
	
	    
	int   size = getTformLength(tform);
	
	// Get number of bytes for a bit array.
	if (type == 'X') {
	    size = (size+7)/8;
	    flags[col] |= COL_BIT;
	    
	} else if ( (flags[col] & COL_VARYING) != 0) {
	    size = 2;
	}
	int   bSize= size;
	
	int[] dims = null;
	
	// Cannot really handle arbitrary arrays of bits.
	
	if (tdims != null  && 
	    type != 'X'    && 
	   (flags[col] & COL_VARYING) == 0) {
	    dims = getTDims(tdims);
	}
	
	if (dims == null) {
	    dims = new int[]{size};
	}
	
	if (type == 'C' || type == 'M') {
	    flags[col] |= COL_COMPLEX;
	}
    
	Class colBase = null;
	
        switch (type) {
	 case 'A':
	           colBase     = byte.class;
	           flags[col] |= COL_STRING;
	           bases[col]  = String.class;
	           break;
	 case 'L':
	           colBase     = byte.class;
	           bases[col]  = boolean.class;
	           flags[col] |= COL_BOOLEAN;
	           break;
	 case 'X':
	 case 'B': colBase    = byte.class; 
	           bases[col] = byte.class;
	           break;
	     
	 case 'I': colBase    = short.class;
	           bases[col] = short.class;
	           bSize     *= 2;
	           break;
	     
	 case 'J': colBase    = int.class;
	           bases[col] = int.class;
	           bSize     *= 4;
	           break;
	     
	 case 'K': colBase    = long.class;
	           bases[col] = long.class;
	           bSize     *= 8;
	           break;
	 case 'E':
	 case 'C': colBase    = float.class;
	           bases[col] = float.class;
	           bSize     *= 4;
	           break;
	 case 'D':
	 case 'M': colBase    = double.class;
	           bases[col] = double.class;
	           bSize     *= 8;
	           break;
	     
	 default: throw new FitsException("Invalid type in column:"+col);
	}
	
        if ((flags[col] & COL_VARYING) != 0) {
	    dims    = new int[]{nRow, 2};
	    colBase = int.class;
	    bSize   = 8;
	    
	} else if ((flags[col] & COL_COMPLEX) != 0) {
	    int[] xdims = new int[dims.length+1];
	    System.arraycopy(dims, 0, xdims, 0, dims.length);
	    xdims[dims.length] = 2;
	    dims = xdims;
	}
	
	modelRow[col] = ArrayFuncs.newInstance(colBase, dims);
	dimens[col]  = dims;
	sizes[col]   = size;
	
	return bSize;
    }
    
    /** Get the type in the TFORM field */
    private char getTformType(String tform) {
	
	for (int i=0; i<tform.length(); i+= 1) {
	    if (!Character.isDigit(tform.charAt(i))) {
		return tform.charAt(i);
	    }
	}
	return 0;
    }
    
    /** Get the type in a varying length column TFORM */
    private char getTformVarType(String tform) {
	int ind = tform.indexOf("P");
	if (tform.length() > ind+1) {
	    return tform.charAt(ind+1);
	} else {
	    return 0;
	}
    }
    
    /** Get the explicit or implied length of the TFORM field */
    private int getTformLength(String tform) {
	
	if (Character.isDigit(tform.charAt(0))) {
	    return initialNumber(tform);
	    
	} else {
	    String xform = tform.substring(1).trim();
	    if (xform.length() == 0) {
		return 1;
	    }
	    if (Character.isDigit(xform.charAt(0))) {
		return initialNumber(xform);
	    }
	}
	return 1;
    }
			 
    /** Get an unsigned number at the beginning of a string */
    private int initialNumber (String tform){
	
        int i;
	for (i=0; i<tform.length(); i += 1) {
	    if (!Character.isDigit(tform.charAt(i))) {
	        break;
	    }
	}
	
	return Integer.parseInt(tform.substring(0,i));
    }
	
    
    /** Parse the TDIMS value.
      *
      * If the TDIMS value cannot be deciphered a one-d
      * array with the size given in arrsiz is returned.
      *
      * @param tdims   The value of the TDIMSn card.
      * @param arraySize  The size field found on the TFORMn card.
      * @return        An int array of the desired dimensions.
      *                Note that the order of the tdims is the inverse
      *                of the order in the TDIMS key.
      */
    public static int[] getTDims(String tdims) {

        // The TDIMs value should be of the form: "(iiii,jjjj,kkk,...)"

	int[] dims= null;

	int first = tdims.indexOf('(');
	int last = tdims.lastIndexOf(')');
	if (first >= 0 && last > 0 && first < last) {
	
	    tdims = tdims.substring(first+1,last-first);
	
	    java.util.StringTokenizer st = new java.util.StringTokenizer(tdims, ",");
	    int dim = st.countTokens();
	    if (dim > 0 ) {
	
	        dims = new int[dim];
		
	        for (int i = dim-1; i>=0; i -= 1) {
	            dims[i] = Integer.parseInt(st.nextToken().trim());
	        }
	    }
	}
	return dims;
    }

    
    public void fillHeader(Header h) throws FitsException {
      try {
	h.setXtension("BINTABLE");
	h.setBitpix(8);
	h.setNaxes(2);
	h.setNaxis(1, rowLen);
	h.setNaxis(2, nRow);
	h.addValue("PCOUNT", heap.size(), null);
	h.addValue("GCOUNT", 1, null);
	Cursor iter = h.iterator();
	iter.setKey("GCOUNT");
	iter.next();
	iter.add("TFIELDS", new HeaderCard("TFIELDS", modelRow.length, null));
	iter.add("THEAP", new HeaderCard("THEAP", 0, null));
	for (int i=0; i< modelRow.length; i += 1) {
	    if (i > 0) {
		h.positionAfterIndex("TFORM", i);
	    }
	    fillForColumn(h, i, iter);
	}
      } catch (HeaderCardException e) {
	  System.err.println("Impossible exception");
      }
    }
    
    void pointToColumn(int col, Header hdr) throws FitsException {
	
	Cursor iter = hdr.iterator();
	if (col > 0) {
	    hdr.positionAfterIndex("TFORM", col);
	}
	fillForColumn(hdr,col,iter);
    }
	
    void fillForColumn(Header h, int col, Cursor iter)
      throws FitsException {
	  
	String tform;
	if ((flags[col] & COL_VARYING) != 0) {
	    tform = "1P";
	} else {
	    tform = ""+sizes[col];
	}
	if (bases[col] == int.class) {
	    tform += "J";
        } else if (bases[col] == short.class || bases[col] == char.class) {
	    tform += "I";
	} else if (bases[col] == byte.class) {
	    tform += "B";
	} else if (bases[col] == float.class) {
	    tform += "E";
	} else if (bases[col] == double.class) {
	    tform += "D";
	} else if (bases[col] == long.class) {
	    tform += "K";
	} else if (bases[col] == boolean.class) {
	    tform += "L";
	} else if (bases[col] == String.class) {
	    tform += "A";
	} else {
	    throw new FitsException("Invalid column data class:"+bases[col]);
	}
       
	  
	String key = "TFORM"+(col+1);
	iter.add(key, new HeaderCard(key, tform, null));
		    
	if (dimens[col].length > 0 && ((flags[col]&COL_VARYING) == 0)) {
	    StringBuffer tdim = new StringBuffer();
	    char comma = '(';
	    for (int i=dimens[col].length-1; i >= 0; i -= 1) {
		tdim.append(comma);
		tdim.append(dimens[col][i]);
		comma = ',';
	    }
	    tdim.append(')');
	    key = "TDIM"+(col+1);
	    iter.add(key, new HeaderCard(key, new String(tdim), null));
	}
    }
    
    /** Create a column table given the number of
     *  rows and a model row.  This is used when
     *  we defer instantiation of the ColumnTable until
     *  the user requests data from the table.
     */
    
    private ColumnTable createTable() throws FitsException {

      int nfields = modelRow.length;
	
      Object[] arrCol= new Object[nfields];
	
      for (int i=0; i<nfields; i += 1) {
	   arrCol[i] = ArrayFuncs.newInstance(
			     ArrayFuncs.getBaseClass(modelRow[i]),
		             sizes[i]*nRow);
      }
	
      ColumnTable table;
	
      try {
          table = new ColumnTable(arrCol, sizes);
      } catch (TableException e) {
          throw new FitsException("Unable to create table:"+e);
      }

      return table;
    }

    /** Create a binary table from existing data in row order.
      *
      * @param data The data used to initialize the binary table.
      */
     
    public BinaryTable(Object[][] data) throws FitsException {
	this(convertToColumns(data));
    }
    
    /** Convert a two-d table to a table of columns.  Handle
     *  String specially.  Every other element of data should be
     *  a primitive array of some dimensionality.
     */
    private static Object[] convertToColumns(Object[][] data) {
	
	Object[] row = data[0];
	int nrow = data.length;
	
	Object[] results = new Object[row.length];
	
	for (int col=0; col<row.length; col += 1) {
	    
	    if (row[col] instanceof String) {
		
		String[] sa = new String[nrow];
		for (int irow=0; irow<nrow; irow += 1) {
		    sa[irow] = (String) data[irow][col];
		}
		
		results[col] = sa;
		
	    } else {
		
		Class base = ArrayFuncs.getBaseClass(row[col]);
		int[] dims = ArrayFuncs.getDimensions(row[col]);
		
		if (dims.length > 1 || dims[0] > 1) {
		    int[] xdims = new int[dims.length+1];
		    xdims[0] = nrow;
		
		    Object[] arr = (Object[]) ArrayFuncs.newInstance(base, xdims);
		    for (int irow=0; irow<nrow; irow += 1) {
		        arr[irow] = data[irow][col];
		    }
		    results[col] = arr;
		} else {
		    Object arr = ArrayFuncs.newInstance(base, nrow);
		    for (int irow=0; irow<nrow; irow += 1) {
			System.arraycopy(data[irow][col], 0, arr, irow, 1);
		    }
		    results[col] = arr;
		}
		
	    }
	}
	return results;
    }
    
    /** Create a binary table from existing data in column order.
      */
    public BinaryTable(Object[] o) throws FitsException {
	
	heap = new FitsHeap(0);
	modelRow = new Object[o.length];
	extendArrays(o.length);
	
	
	for (int i=0; i<o.length; i += 1) {
	    addColumn(o[i]);
	}
    }

    /** Create a binary table from an existing ColumnTable */
    public BinaryTable(ColumnTable tab) {
	
	nCol = tab.getNCols();
	
	extendArrays(nCol);
	
        bases    = tab.getBases();
	sizes    = tab.getSizes();
	
	modelRow = new Object[nCol];
	
	dimens   = new int[nCol][1];
	
	// Set all flags to 0.
	flags    = new int[nCol];
	
	for (int col=0; col<nCol; col += 1) {
	    dimens[col][0] = sizes[col];
	}
	    
	for (int col=0; col < nCol; col += 1) {
	    modelRow[col] = ArrayFuncs.newInstance(bases[col], sizes[col]);
	}
	
	columns = null;
	table = tab;
    }
	
	
    /** Get a given row
      * @param row The index of the row to be returned.
      * @return A row of data.
      */
    public Object[] getRow(int row) throws FitsException {
	
        if (!validRow(row)) {
            throw new FitsException("Invalid row");
        }

	Object[] res;
	if (table != null) {
	    res = getMemoryRow(row);
	} else {
	    res = getFileRow(row);
	}
	return res;
    }
    
    /** Get a row from memory.
     */
    private Object[] getMemoryRow(int row) throws FitsException {

        Object[] data = new Object[modelRow.length];
        for (int col=0; col<modelRow.length; col += 1) {
	    Object o = table.getElement(row, col);
	    o = columnToArray(col, o);
            data[col] = encurl(o, col , 1);
	    if (data[col] instanceof Object[]) {
		data[col] = ((Object[])data[col])[0];
	    }
        }
	
        return data;
	
    }

    /** Get a row from the file.
     */
    private Object[] getFileRow(int row) throws FitsException {
	
	/** Read the row from memory */
	Object[] data = new Object[nCol];
	for (int col=0; col<data.length; col += 1) {
	    data[col] = ArrayFuncs.newInstance(
			      ArrayFuncs.getBaseClass(modelRow[col]),
					  sizes[col]);
	}
	
	try {
	    FitsUtil.reposition(currInput, fileOffset+row*rowLen);
	    currInput.readArray(data);
	} catch (IOException e) {
	    throw new FitsException ("Error in deferred row read");
	}
	for (int col=0; col<data.length; col += 1) {
	    data[col] = columnToArray(col, data[col]);
	    data[col] = encurl(data[col], col, 1);
	    if (data[col] instanceof Object[]) {
		data[col] = ((Object[])data[col])[0];
	    }
	}
	return data;
    }

    /** Get an element from the file.
     */
    private Object getFileElement(int row, int col) throws FitsException {
        Object cell = ArrayFuncs.newInstance( 
                            ArrayFuncs.getBaseClass(modelRow[col]), 
                                        sizes[col]);
        try {
            FitsUtil.reposition(currInput, 
                                fileOffset+row*rowLen+colOffsets[col]);
            currInput.readArray(cell);
        } catch (IOException e) {
            throw new FitsException ("Error in deferred row read");
        }
        cell = columnToArray(col, cell);
        cell = encurl(cell, col, 1);
        if (cell instanceof Object[]) {
            cell = ((Object[])cell)[0];
        }
        return cell;
    }
	
    /** Replace a row in the table.
      * @param row  The index of the row to be replaced.
      * @param data The new values for the row.
      * @exception FitsException Thrown if the new row cannot
      *                          match the existing data.
      */
    public void setRow(int row, Object data[]) throws FitsException {

	if (table == null) {
	    getData();
	}
	
        if (data.length != getNCols()) {
             throw new FitsException("Updated row size does not agree with table");
         }

         Object[] ydata = new Object[data.length];

         for (int col=0; col<data.length; col += 1) {
	     Object o = ArrayFuncs.flatten(data[col]);
             ydata[col] = arrayToColumn(col, o);
         }
	
         try {
             table.setRow(row, ydata);
         } catch (TableException e) {
             throw new FitsException("Error modifying table: "+e);
         }
     }

     /** Replace a column in the table.
       * @param col The index of the column to be replaced.
       * @param xcol The new data for the column
       * @exception FitsException Thrown if the data does not match
       *                          the current column description.
       */
     public void setColumn(int col, Object xcol) throws FitsException {
	 
	 xcol = ArrayFuncs.flatten(xcol);
	 xcol = arrayToColumn(col, xcol);
	 setFlattenedColumn(col, xcol);
     }


     /** Set a column with the data aleady flattened.
       *
       * @param col  The index of the column to be replaced.
       * @param data The new data array.  This should be a one-d
       *             primitive array.
       * @exception FitsException Thrown if the type of length of
       *                         the replacement data differs from the
       *                         original.
       */
      public void setFlattenedColumn (int col, Object data) throws FitsException {
	  
	  if (table == null) {
	      getData();
	  }
	  
	  data = arrayToColumn(col, data);
	  
	  Object oldCol = table.getColumn(col);
	  if (data.getClass() != oldCol.getClass() ||
	      Array.getLength(data) != Array.getLength(oldCol)) {
              throw new FitsException("Replacement column mismatch at column:"+col);
          }
	  try {
	      table.setColumn(col, data);
	  } catch(TableException e) {
	      throw new FitsException("Unable to set column:"+col+" error:"+e);
	  }
      }

    /** Get a given column
      * @param col The index of the column.
      */
    public Object getColumn(int col) throws FitsException {
	
	if (table == null) {
	    getData();
	}
	
	
	Object res = getFlattenedColumn(col);
	return encurl(res, col, nRow);
    }
    
    private Object encurl(Object res, int col, int rows) {

	if (bases[col] != String.class ) {
	    if ( ((flags[col]&COL_VARYING) == 0) &&
		 (dimens[col].length > 1 || dimens[col][0] != 1) ) {
	    
                int[] dims = new int[dimens[col].length+1];
                System.arraycopy(dimens[col], 0, dims, 1, dimens[col].length);
                dims[0] = rows;
	        res = ArrayFuncs.curl(res, dims);
	    }
	
	} else {
	    
	    // Handle Strings.  Remember the last element
	    // in dimens is the length of the Strings and
	    // we already used that when we converted from
	    // byte arrays to strings.  So we need to ignore
	    // the last element of dimens, and add the row count
	    // at the beginning to curl.

	    if (dimens[col].length > 2) {
                int[] dims = new int[dimens[col].length];
		
                System.arraycopy(dimens[col], 0, dims, 1, dimens[col].length-1);
                dims[0] = rows;
	        res = ArrayFuncs.curl(res, dims);
	    }
	}
	
        return res;

    }

     /** Get a column in flattened format.
       * For large tables getting a column in standard format can be
       * inefficient because a separate object is needed for
       * each row.  Leaving the data in flattened format means
       * that only a single object is created.
       * @param col
       */

     public Object getFlattenedColumn(int col) throws FitsException {
	 
	 if (table == null) {
	     getData();
	 }
	 
         if (!validColumn(col) ) {
             throw new FitsException("Invalid column");
         }

         return columnToArray(col, table.getColumn(col));
     }

     /** Get a particular element from the table.
       * @param i The row of the element.
       * @param j The column of the element.
       */
     public Object getElement(int i, int j) throws FitsException {
	 
         if (!validRow(i) || !validColumn(j)) {
             throw new FitsException("No such element");
         }
	 
	 Object ele;
	 if (table == null) {
	     // // This is really inefficient.
	     // // Need to either save the row, or just read the one element.
	     // Object[] row = getRow(i);
	     // ele = row[j];
             // Efficiency improved by mbt 10/2003.
             ele = getFileElement(i, j);
	     
	 } else {
             ele = table.getElement(i,j);
	     ele = columnToArray(j, ele);
	     ele = encurl(ele, j, 1);
	     if (ele instanceof Object[]) {
		 ele = ((Object[]) ele)[0];
	     }
	 }
	 
	 return ele;
     }

     /** Add a row at the end of the table.
       * @param o An array of objects instantiating the data.  These
       *          should have the same structure as any existing rows.
       */
    
     public int addRow(Object[] o) throws FitsException {
	 
	 if (table == null) {
	     getData();
	 }
	 
	 if (nCol == 0 && nRow == 0) {
	     for (int i=0; i<o.length; i += 1) {
		 addColumn(o);
	     }
	 } else {

	     Object[] flatRow = new Object[getNCols()];
	     for (int i=0; i<getNCols(); i += 1) {
		 Object x  = ArrayFuncs.flatten(o[i]);
	         flatRow[i] = arrayToColumn(i, x);
	     }
	     try {
	         table.addRow(flatRow);
	     } catch (TableException e) {
	         throw new FitsException("Error add row to table");
	     }
	 
	     nRow += 1;
	 }
	 
	 return nRow;
     }

     /** Add a column to the end of a table.
       * @param o An array of identically structured objects with the
       *          same number of elements as other columns in the table.
       */
     public int addColumn(Object o) throws FitsException {
	 
	 extendArrays(nCol+1);
	 Class base = ArrayFuncs.getBaseClass(o);
	 
	 // A varying length column is a two-d primitive
	 // array where the second index is not constant.
	 
	 if (isVarying(o)) {
	     flags[nCol] |= COL_VARYING;
	     dimens[nCol] = new int[]{2};
	 }
	 
	 
	 // Flatten out everything but 1-D arrays and the
	 // two-D arrays associated with variable length columns.
	 
	 if ((flags[nCol] & COL_VARYING) == 0) {
	     
	     int[] allDim = ArrayFuncs.getDimensions(o);
	     
	     // Add a dimension for the length of Strings.
	     if (base == String.class) {
		 int[] xdim = new int[allDim.length+1];
		 System.arraycopy(allDim, 0, xdim, 0, allDim.length);
		 xdim[allDim.length] = -1;
		 allDim = xdim;
	     }
	     
	     if (allDim.length == 1) {
		 dimens[nCol] = new int[]{1};
	     } else {
		 dimens[nCol] = new int[allDim.length-1];
		 System.arraycopy(allDim, 1, dimens[nCol], 0, allDim.length-1);
		 o = ArrayFuncs.flatten(o);
	     }
	 }
	 
         addFlattenedColumn(o, dimens[nCol]);
	 return getNCols();

     }
    
     /** Is this a variable length column?
      *  It is if it's a two-d primitive array and
      *  the second dimension is not constant.
      */
     private boolean isVarying(Object o) {
	 
	 String classname = o.getClass().getName();
	 
	 if (classname.length() != 3 ||
	     classname.charAt(0) != '[' ||
	     classname.charAt(1) != '[') {
	     return false;
	 }
	 
	 Object[] ox = (Object[]) o;
	 if (ox.length < 2) {
	     return false;
	 }
	 
	 int flen = Array.getLength(ox[0]);
	 for (int i=1; i<ox.length; i += 1) {
	     if (Array.getLength(ox[i]) != flen) {
		 return true;
	     }
	 }
	 return false;
     }

     /** Add a column where the data is already flattened.
       * @param o      The new column data.  This should be a one-dimensional
       *               primitive array.
       * @param dimens The dimensions of one row of the column.
       */
     public int addFlattenedColumn(Object o, int[] dims)  throws FitsException {
	 
	 extendArrays(nCol + 1);
	 
	 bases[nCol]       = ArrayFuncs.getBaseClass(o);
	 
	 if (bases[nCol] == boolean.class) {
	     flags[nCol]  |= COL_BOOLEAN;
	 } else if (bases[nCol] == String.class) {
	     flags[nCol]  |= COL_STRING;
	 }
	 
	 // Convert to column first in case
	 // this is a String array.  This sets
	 // the size of the column.

	 o = arrayToColumn(nCol, o);
	 
         int size = 1;
	 
         for (int dim=0; dim < dims.length; dim += 1) {
             size *= dims[dim];
         }
         sizes[nCol]    = size;
	 
	 
	 int xRow = Array.getLength(o)/size;
	 if (getNCols() == 0) {
	     nRow = xRow;
         } else if (xRow > 0) {
	     if (xRow != nRow) {
		 throw new FitsException("Added column does not have correct row count");
	     }
	 }
	 
	 if ( (flags[nCol] & COL_VARYING) == 0) {
             modelRow[nCol] = ArrayFuncs.newInstance(ArrayFuncs.getBaseClass(o), dims);
	     rowLen        += size*ArrayFuncs.getBaseLength(o);
	 } else {
	     modelRow[nCol] = new int[2];
	     rowLen        += 8;
	 }
	 
	 // Only add to table if table already exists or if we
	 // are filling up the last element in columns.
	 // This way if we allocate a bunch of columns at the beginning
	 // we only create the column table after we have all the columns
	 // ready.
	 
	 columns[nCol] = o;
	 
	 try {
	     if (table != null) {
	         table.addColumn(o,sizes[nCol]);
	     } else if (nCol == columns.length-1) {
	         table = new ColumnTable(columns, sizes);
	     }
	 } catch (TableException e) {
	     throw new FitsException("Error in ColumnTable:"+e);
	 }
	 
	 nCol += 1;
	 return nCol;
     }

     /** Get the number of rows in the table
       */
     public int getNRows() {
         return nRow;
     }

     /** Get the number of columns in the table.
       */
     public int getNCols() {
         return nCol;
     }

     /** Check to see if this is a valid row.
       * @param i The Java index (first=0) of the row to check.
       */
     protected boolean validRow(int i) {
         if (getNRows() > 0 && i >= 0 && i <getNRows()) {
             return true;
         } else {
             return false;
         }
     }

     /** Check if the column number is valid.
       *
       * @param j The Java index (first=0) of the column to check.
       */
     protected boolean validColumn(int j) {
         return (j >= 0 && j < getNCols());
     }

     /** Replace a single element within the table.
       *
       * @param i The row of the data.
       * @param j The column of the data.
       * @param o The replacement data.
       */
    public void setElement(int i, int j, Object o) throws FitsException{

        try {
            table.setElement(i, j, ArrayFuncs.flatten(o));
        } catch (TableException e) {
            throw new FitsException("Error modifying table:"+e);
        }
    }

    /** Read the data -- or defer reading on random access
     */
    public void read(ArrayDataInput i) throws FitsException {

	setFileOffset(i);
	currInput = i;
	
	if (i instanceof RandomAccess) {
	    try {
		i.skipBytes(getTrueSize());
		i.skipBytes(FitsUtil.padding(getTrueSize()));
	    } catch (IOException e) {
		throw new FitsException ("Unable to skip binary table HDU:"+e);
	    }
	} else {

           /** Read the data associated with the HDU including the hash area if present.
             * @param i The input stream
             */
	    if (table == null) {
		table = createTable();
	    }
	    
            readTrueData(i);
	}
    }

    /** Read table, heap and padding */
    protected void readTrueData(ArrayDataInput i) throws FitsException {

         int len;

         try {
             len = table.read(i);
	     i.skipBytes(heapOffset);
	     heap.read(i);
	     i.skipBytes(FitsUtil.padding(getTrueSize()));

         } catch (IOException e) {
             throw new FitsException("Error reading binary table data:"+e);
         }
    }

    /** Get the size of the data in the HDU sans padding.
      */
    public int getTrueSize() {
	int len= nRow*rowLen;
	if (heap.size() > 0) {
	    len += heap.size() + heapOffset;
	}
        return len;
    }


    /** Write the table, heap and padding */
    public void write(ArrayDataOutput os) throws FitsException {
	
      if (table == null) {
	  long currentOffset=FitsUtil.findOffset(os);
	  getData();
	  FitsUtil.reposition(os, currentOffset);
      }
	
      int len;
      try {

        // First write the table.
        len = table.write(os);
	if (heapOffset > 0) {
	    os.write(new byte[heapOffset]);
	}
	if (heap.size() > 0) {
	    heap.write(os);
        }
	
	os.write(new byte[FitsUtil.padding(getTrueSize())]);
			  
      } catch (IOException e) {
          throw new FitsException("Unable to write table:"+e);
      }


    }
    
    public Object getData() throws FitsException {
	
        if (table == null) {
	
	    if (currInput == null) {
	        throw new FitsException("Cannot find input for deferred read");
	    }
	    
	    table=createTable();
	
	    long currentOffset = FitsUtil.findOffset(currInput);
	    FitsUtil.reposition(currInput, fileOffset);
	    readTrueData(input);
	    FitsUtil.reposition(currInput, currentOffset);
	}
	
	return table;
    }
	

    public int[][] getDimens() {
        return dimens;
    }


    public Class[] getBases() {
        return table.getBases();
    }

    public char[] getTypes() {
	if (table == null) {
	    try {
	        getData();
	    } catch (FitsException e){
	    }
	}
        return table.getTypes();
    }
    
    public Object[] getFlatColumns() {
	if (table == null) {
	    try {
		getData();
	    } catch (FitsException e) {
	    }
	}
	return table.getColumns();
    }
    

    public int[] getSizes() {
        return sizes;
    }
    
    /** Convert the external representation to the
     *  BinaryTable representation.  Transformation include
     *  boolean -> T/F, Strings -> byte arrays,
     *  variable length arrays -> pointers (after writing data
     *  to heap).
     */
    private Object arrayToColumn(int col, Object o) throws FitsException {
	
	if (flags[col] == 0) {
	    return o;
	}
	
	if ((flags[col]&COL_VARYING) == 0) {
	    
	    if ( (flags[col] &  COL_STRING) != 0) {
		
		// Convert strings to array of bytes.
	        int[] dims = dimens[col];
	    
	        // Set the length of the string if we are just adding the column.
	        if (dims[dims.length-1] < 0) {
	            dims[dims.length-1] = FitsUtil.maxLength((String[])o);
	        }
		if (o instanceof String) {
		    o = new String[]{(String) o};
		}
	        o = FitsUtil.stringsToByteArray((String[])o, dims[dims.length-1]);
	
	    
	    } else if ( (flags[col] & COL_BOOLEAN) != 0) {
		
		// Convert true/false to 'T'/'F'
	        o = FitsUtil.booleanToByte((boolean[])o);
	    }
	    
	} else {
	    
	    if ( (flags[col]&COL_BOOLEAN) != 0) {
		
		// Handle addRow/addElement
		if (o instanceof boolean[]) {
		    o = new boolean[][]{(boolean[])o};
	        }
		
		// Convert boolean to byte arrays
		boolean [][] to = (boolean[][]) o;
		byte[][] xo = new byte[to.length][];
		for (int i=0; i<to.length; i += 1) {
		    xo[i] = FitsUtil.booleanToByte(to[i]);
		}
		o = xo;
	    }

	    // Write all rows of data onto the heap.
	    int offset = heap.putData(o);
	    
	    int blen = ArrayFuncs.getBaseLength(o);
	    
	    // Handle an addRow of a variable length element.
	    // -- We only get a one-d array, but the following
	    //    lets us use the same code as when we add in a column!
	    if (!(o instanceof Object[])) {
		o = new Object[]{o};
	    }
	    
	    // Create the array descriptors
	    int nrow = Array.getLength(o);
	    int[] descrip = new int[2*nrow];
	    
	    Object[] x = (Object[]) o;
	    
	    // Fill the descriptor for each row.
	    for (int i=0; i<nrow; i += 1) {
		descrip[2*i] = Array.getLength(x[i]);
		descrip[2*i+1] = offset;
		offset += descrip[2*i]*blen;
		if ( (flags[col] & COL_COMPLEX) != 0) {
		    // We count each pair in a complex number as
		    // a single element.
		    descrip[2*i] /= 2;
		}
	    }
	    o = descrip;
	}
	
	return o;
    }
    
    /** Convert data from binary table representation to external
     *  Java representation.
     */
    private Object columnToArray(int col, Object o) throws FitsException {
	
	// Most of the time we need do nothing!
	if (flags[col] == 0) {
	    return o;
	}
	
	// If a varying length column use the descriptors to
	// extract appropriate information from the headers.
	
	if ( (flags[col] & COL_VARYING) != 0) {
	    int[] descrip = (int[]) o;
	    int nrow = descrip.length/2;
	    
	    Object[] res;  // Res will be the result of extracting from the heap.
	    int[] dims;    // Used to create result arrays.
	    
	    
	    if ( (flags[col] & COL_COMPLEX) != 0) {
		
	        // Complex columns have an extra dimension for each row
	        dims = new int[]{nrow,0,0};
		res =  (Object[]) ArrayFuncs.newInstance(bases[col], dims) ;
		// Set up dims for individual rows.
		dims = new int[2];
		dims[1] = 2;
		
	    } else {
		
		// Non-complex data has a simple primitive array for each row
		dims = new int[]{nrow, 0};
		res = (Object[]) ArrayFuncs.newInstance(bases[col], dims);
	    }
	
	    // Now read in each requested row.
	    for (int i=0; i<nrow; i += 1) {
		Object row;
		int offset = descrip[2*i+1];
		int dim = descrip[2*i];
		
		if ( (flags[col] & COL_COMPLEX) != 0) {
		    dims[0] = dim;
		    row = ArrayFuncs.newInstance(bases[col], dims);
		    
		} else if ( (flags[col] & COL_BOOLEAN) != 0) {
		    // For boolean data, we need to read bytes and convert
		    // to booleans.
		    row = ArrayFuncs.newInstance(byte.class, dim);
		    
		} else {
		    
		    row = ArrayFuncs.newInstance(bases[col], dim);
		}
		
		heap.getData(offset, row);
		
		// Now do the boolean conversion.
		if ( (flags[col] & COL_BOOLEAN) != 0) {
		    row = FitsUtil.byteToBoolean((byte[])row);
		}
		
		res[i] = row;
	    }
	    o = res;
	    
	} else {  // Fixed length columns
    
	    // Need to convert String byte arrays to appropriate Strings.
	    if ( (flags[col] & COL_STRING) != 0) {
	        int[] dims = dimens[col];
	        o = FitsUtil.byteArrayToStrings((byte[])o, dims[dims.length-1]);
	    
	    } else if ( (flags[col] & COL_BOOLEAN) != 0) {
	        o =  FitsUtil.byteToBoolean((byte[])o);
	    }
	}
	
	return o;
    }
    
    /** Make sure the arrays which describe the columns are
     *  long enough, and if not extend them.
     */
    private void extendArrays(int need) {
	
	boolean wasNull = false;
	if (sizes == null) {
	    wasNull = true;
	    
	} else if (sizes.length > need) {
	    return;
	}
	
	// Allocate the arrays.
	int[]    newSizes = new int[need];
        int[]    newColOffsets = new int[need];
	int[][]  newDimens= new int[need][];
	int[]    newFlags = new int[need];
	Object[] newModel = new Object[need];
	Object[] newColumns= new Object[need];
	Class[]  newBases = new Class[need];
	
	if (!wasNull) {
	    int len = sizes.length;
	    System.arraycopy(sizes,    0, newSizes,  0, len);
            System.arraycopy(colOffsets, 0, newColOffsets, 0, len);
	    System.arraycopy(dimens,   0, newDimens, 0, len);
	    System.arraycopy(flags,    0, newFlags,  0, len);
	    System.arraycopy(modelRow, 0, newModel,  0, len);
	    System.arraycopy(columns,  0, newColumns,  0, len);
	    System.arraycopy(bases,    0, newBases,  0, len);
	}
	
	sizes    = newSizes;
        colOffsets = newColOffsets;
	dimens   = newDimens;
	flags    = newFlags;
	modelRow = newModel;
	columns  = newColumns;
	bases    = newBases;
    }
    
    public int getHeapSize() {
	return heapOffset + heap.size();
    }
    
    public int getHeapOffset() {
	return heapOffset;
    }
}
