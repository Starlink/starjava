package nom.tam.util;

 /*
  * Copyright: Thomas McGlynn 1997-1998.
  * This code may be used for any purpose, non-commercial
  * or commercial so long as this copyright notice is retained
  * in the source code or included in or referred to in any
  * derived software.
  */
import  java.io.*;
import  java.lang.reflect.Array;

/** A data table is conventionally considered to consist of rows and
  * columns, where the structure within each column is constant, but
  * different columns may have different structures.  I.e., structurally
  * columns may differ but rows are identical.
  * Typically tabular data is usually stored in row order which can
  * make it extremely difficult to access efficiently using Java.
  * This class provides efficient
  * access to data which is stored in row order and allows users to
  * get and set the elements of the table.
  * The table can consist only of arrays of primitive types.
  * Data stored in column order can
  * be efficiently read and written using the
  * BufferedDataXputStream classes.
  *
  * The table is represented entirely as a set of one-dimensional primitive
  * arrays.  For a given column, a row consists of some number of
  * contiguous elements of the array.  Each column is required to have
  * the same number of rows.
  */

public class ColumnTable implements DataTable {


    /** The columns to be read/written */
    private Object[] arrays;

    /** The number of elements in a row for each column */
    private int[] sizes;

    /** The number of rows */
    private int nrow;

    /** The number or rows to read/write in one I/O. */
    private int chunk;

    /** The size of a row in bytes */
    private int rowSize;

    /** The base type of each row (using the second character
      * of the [x class names of the arrays.
      */
    private char[] types;
    private Class[] bases;

    // The following arrays are used to avoid having to check
    // casts during the I/O loops.
    // They point to elements of arrays.
    private byte[][]      bytePointers;
    private short[][]     shortPointers;
    private int[][]       intPointers;
    private long[][]      longPointers;
    private float[][]     floatPointers;
    private double[][]    doublePointers;
    private char[][]      charPointers;
    private boolean[][]   booleanPointers;


    /** Create the object after checking consistency.
      * @param arrays  An array of one-d primitive arrays.
      * @param sizes   The number of elements in each row
      *                for the corresponding column
      */
    public ColumnTable(Object[] arrays, int[] sizes) throws TableException {
        setup(arrays, sizes);
    }

    /** Actually perform the initialization.
      */
    protected void setup(Object[] arrays, int[] sizes) throws TableException {

        checkArrayConsistency(arrays, sizes);
        getNumberOfRows();
        initializePointers();

    }

    /** Get the number of rows in the table.
      */
    public int getNRows() {
        return nrow;
    }

    /** Get the number of columns in the table.
      */
    public int getNCols() {
        return arrays.length;
    }


    /** Get a particular column.
      * @param col The column desired.
      * @return an object containing the column data desired.
      *         This will be an instance of a 1-d primitive array.
      */
    public Object getColumn(int col) {
        return arrays[col];
    }

    /** Set the values in a particular column.
      * The new values must match the old in length but not necessarily in type.
      * @param col The column to modify.
      * @param newColumn The new column data.  This should be a primitive array.
      * @exception TableException Thrown when the new data is not commenserable with
      *                           informaiton in the table.
      */
    public void setColumn(int col, Object newColumn) throws TableException {
	
	boolean reset = newColumn.getClass()       != arrays[col].getClass() ||
	                Array.getLength(newColumn) != Array.getLength(arrays[col]);
        arrays[col] = newColumn;
        if (reset) {
	    setup(arrays, sizes);
	}
    }
    
    /** Add a column */
    public void addColumn(Object newColumn, int size) throws TableException {
	
	String classname = newColumn.getClass().getName();
	nrow = checkColumnConsistency(newColumn, classname, nrow, size);
	
	rowSize += nrow*ArrayFuncs.getBaseLength(newColumn);
	
	getNumberOfRows();
	
	int ncol = arrays.length;
	
	Object[] newArrays  = new Object[ncol+1];
        int[] newSizes   = new int[ncol+1];
	Class[] newBases = new Class[ncol+1];
	char[]  newTypes    = new char[ncol+1];
	
	System.arraycopy(arrays, 0, newArrays, 0, ncol);
	System.arraycopy(sizes, 0, newSizes, 0, ncol);
	System.arraycopy(bases, 0, newBases, 0, ncol);
	System.arraycopy(types, 0, newTypes, 0, ncol);
	
	arrays = newArrays;
	sizes  = newSizes;
	bases  = newBases;
	types  = newTypes;
	
	arrays[ncol] = newColumn;
	sizes[ncol] = size;
	bases[ncol] = ArrayFuncs.getBaseClass(newColumn);
	types[ncol] = classname.charAt(1);
	addPointer(newColumn);
    }
    
    /** Add a row to the table.  This method is very inefficient
     *  for adding multiple rows and should be avoided if possible.
     */
    public void addRow(Object[] row) throws TableException {
	
	if (arrays.length == 0) {
	    
	    for (int i=0; i<row.length; i += 1) {
		addColumn(row[i], Array.getLength(row[i]));
	    }
	    
	} else {
	    
	    if (row.length != arrays.length) {
		throw new TableException("Row length mismatch");
	    }
	    
	    for (int i=0; i<row.length; i += 1) {
		if (row[i].getClass() != arrays[i].getClass() ||
		    Array.getLength(row[i]) != sizes[i]) {
		    throw new TableException("Row column mismatch at column:"+i);
		}
		Object xarray = ArrayFuncs.newInstance(bases[i], (nrow+1)*sizes[i]);
		System.arraycopy(arrays[i], 0, xarray, 0, nrow*sizes[i]);
		System.arraycopy(row[i], 0, xarray, nrow*sizes[i], sizes[i]);
		arrays[i] = xarray;
	    }
	    initializePointers();
	    nrow += 1;
	}
    }

    /** Get a element of the table.
      * @param row The row desired.
      * @param col The column desired.
      * @return A primitive array containing the information.  Note
      *         that an array will be returned even if the element
      *         is a scalar.
      */
    public Object getElement(int row, int col) {

        Object x = ArrayFuncs.newInstance(bases[col], sizes[col]);
        System.arraycopy(arrays[col], sizes[col]*row, x, 0, sizes[col]);
        return x;
    }

    /** Modify an element of the table.
      * @param row The row containing the element.
      * @param col The column containing the element.
      * @param x   The new datum.  This should be 1-d primitive
      *            array.
      * @exception TableException Thrown when the new data
      *                           is not of the same type as
      *                           the data it replaces.
      */
    public void setElement(int row, int col, Object x)
                           throws TableException {

        String classname = x.getClass().getName();

        if (!classname.equals("["+types[col])) {
            throw new TableException("setElement: Incompatible element type");
        }

        if (Array.getLength(x) != sizes[col]) {
            throw new TableException("setElement: Incompatible element size");
        }

        System.arraycopy(x, 0, arrays[col], sizes[col]*row, sizes[col]);
    }

    /** Get a row of data.
      * @param The row desired.
      * @return An array of objects each containing a primitive array.
      */
    public Object getRow(int row) {

        Object[] x = new Object[arrays.length];
        for (int col=0; col<arrays.length; col += 1) {
             x[col] = getElement(row, col);
        }
        return x;
    }

    /** Modify a row of data.
      * @param row The row to be modified.
      * @param x   The data to be modified.  This should be an
      *            array of objects.  It is described as an Object
      *            here since other table implementations may
      *            use other methods to store the data (e.g.,
      *            @see ColumnTable.getColumn.
      */
    public void setRow(int row, Object x) throws TableException {

        if (! (x instanceof Object[])) {
            throw new TableException("setRow: Incompatible row");
        }

        for (int col=0; col<arrays.length; col += 1) {
            setElement(row, col, ((Object[]) x)[col]);
        }
    }

    /** Check that the columns and sizes are consistent.
      * Inconsistencies include:
      * <ul>
      * <li> arrays and sizes have different lengths.
      * <li> an element of arrays is not a primitive array.
      * <li> the size of an array is not divisible by the sizes entry.
      * <li> the number of rows differs for the columns.
      * </ul>
      * @param arrays The arrays defining the columns.
      * @param sizes  The number of elements in each row for the column.
      */
    protected void checkArrayConsistency(Object[] arrays, int[] sizes)
                                         throws TableException {

        // This routine throws an error if it detects an inconsistency
        // between the arrays being read in.

        // First check that the lengths of the two arrays are the same.
        if (arrays.length != sizes.length) {
          throw new TableException ("readArraysAsColumns: Incompatible arrays and sizes.");
        }

        // Now check that that we fill up all of the arrays exactly.
        int ratio = 0;
        int rowSize = 0;

        this.types = new char[arrays.length];
        this.bases = new Class[arrays.length];

        // Check for a null table.
        boolean nullTable = true;

        for (int i=0; i<arrays.length; i += 1) {
	    
            String classname = arrays[i].getClass().getName();
	    
	    ratio    = checkColumnConsistency(arrays[i], classname, ratio, sizes[i]);
	    
            rowSize += sizes[i]*ArrayFuncs.getBaseLength(arrays[i]);
            types[i] = classname.charAt(1);
            bases[i] = ArrayFuncs.getBaseClass(arrays[i]);
	}

        this.nrow = ratio;
        this.rowSize = rowSize;
        this.arrays = arrays;
        this.sizes = sizes;
    }
					     
	    
    private int checkColumnConsistency(Object data, String classname, int ratio, int size)
      throws TableException {

        if (classname.charAt(0) != '['  || classname.length() != 2) {
            throw new TableException("Non-primitive array for column");
        }

        int thisSize = Array.getLength(data);
        if (thisSize == 0 && size != 0 ||
	    thisSize != 0 && size == 0) {
	    throw new TableException("Size mismatch in column");
        }

        // The row size must evenly divide the size of the array.
        if (thisSize % size != 0) {
            throw new TableException("Row size does not divide array for column");
        }

        // Finally the ratio of sizes must be the same for all columns -- this
        // is the number of rows in the table.
	int thisRatio = 0;
        if (size > 0) {
            thisRatio = thisSize/size;

            if (ratio != 0 && (thisRatio != ratio)) {
                throw new TableException("Different number of rows in different columns");
            }
        }
        if (thisRatio > 0) {
	    return thisRatio;
        } else {
	    return ratio;
        }
    }

    /** Calculate the number of rows to read/write at a time.
      * @param rowSize The size of a row in bytes.
      * @param nrows   The number of rows in the table.
      */
    protected void getNumberOfRows() {

        int bufSize=65536;

        // If a row is larger than bufSize, then read one row at a time.
        if (rowSize == 0) {
            this.chunk = 0;

        } else if (rowSize > bufSize) {
            this.chunk = 1;

        // If the entire set is not too big, just read it all.
        } else if (bufSize/rowSize >= nrow) {
            this.chunk = nrow;
        } else {
            this.chunk = bufSize/rowSize + 1;
        }

    }

    /** Set the pointer arrays for the eight primitive types
      * to point to the appropriate elements of arrays.
      */
    protected void initializePointers() {

        int nbyte, nshort, nint, nlong, nfloat, ndouble, nchar, nboolean;

        // Count how many of each type we have.
        nbyte=0; nshort=0; nint = 0; nlong = 0;
        nfloat = 0; ndouble=0; nchar = 0; nboolean = 0;

        for (int col=0; col<arrays.length; col += 1) {
            switch (types[col]) {

               case 'B':
                   nbyte += 1;
                   break;
               case 'S':
                   nshort += 1;
                   break;
               case 'I':
                   nint += 1;
                   break;
               case 'J':
                   nlong += 1;
                   break;
               case 'F':
                   nfloat += 1;
                   break;
               case 'D':
                   ndouble += 1;
                   break;
               case 'C':
                   nchar += 1;
                   break;
               case 'Z':
                   nboolean += 1;
                   break;
            }
        }

        // Allocate the pointer arrays.  Note that many will be
        // zero-length.

        bytePointers     = new byte[nbyte][];
        shortPointers    = new short[nshort][];
        intPointers      = new int[nint][];
        longPointers     = new long[nlong][];
        floatPointers    = new float[nfloat][];
        doublePointers   = new double[ndouble][];
        charPointers     = new char[nchar][];
        booleanPointers  = new boolean[nboolean][];

        // Now set the pointers.
        nbyte=0; nshort=0; nint = 0; nlong = 0;
        nfloat = 0; ndouble=0; nchar = 0; nboolean = 0;

        for (int col=0; col<arrays.length; col += 1) {
            switch (types[col]) {

               case 'B':
                   bytePointers[nbyte] = (byte[]) arrays[col];
                   nbyte += 1;
                   break;
               case 'S':
                   shortPointers[nshort] = (short[]) arrays[col];
                   nshort += 1;
                   break;
               case 'I':
                   intPointers[nint] = (int[]) arrays[col];
                   nint += 1;
                   break;
               case 'J':
                   longPointers[nlong] = (long[]) arrays[col];
                   nlong += 1;
                   break;
               case 'F':
                   floatPointers[nfloat] = (float[]) arrays[col];
                   nfloat += 1;
                   break;
               case 'D':
                   doublePointers[ndouble] = (double[]) arrays[col];
                   ndouble += 1;
                   break;
               case 'C':
                   charPointers[nchar] = (char[]) arrays[col];
                   nchar += 1;
                   break;
               case 'Z':
                   booleanPointers[nboolean] = (boolean[]) arrays[col];
                   nboolean += 1;
                   break;
            }
        }
    }

    // Add a pointer in the pointer lists.
    protected void addPointer(Object data) throws TableException {
	String classname = data.getClass().getName();
	char type = classname.charAt(1);
	
	switch (type) {
	 case 'B': {
	    byte[][] xb = new byte[bytePointers.length+1][];
	    System.arraycopy(bytePointers, 0, xb, 0, bytePointers.length);
	    xb[bytePointers.length] = (byte[]) data;
	    bytePointers = xb;
	    break;
	 }
	 case 'Z': {
	    boolean[][] xb = new boolean[booleanPointers.length+1][];
	    System.arraycopy(booleanPointers, 0, xb, 0, booleanPointers.length);
	    xb[booleanPointers.length] = (boolean[]) data;
	    booleanPointers = xb;
	    break;
	 }
	 case 'S':{
	    short[][] xb = new short[shortPointers.length+1][];
	    System.arraycopy(shortPointers, 0, xb, 0, shortPointers.length);
	    xb[shortPointers.length] = (short[]) data;
	    shortPointers = xb;
	    break;
	 }
	 case 'C':{
	    char[][] xb = new char[charPointers.length+1][];
	    System.arraycopy(charPointers, 0, xb, 0, charPointers.length);
	    xb[charPointers.length] = (char[]) data;
	    charPointers = xb;
	    break;
	 }
	 case 'I': {
	    int[][] xb = new int[intPointers.length+1][];
	    System.arraycopy(intPointers, 0, xb, 0, intPointers.length);
	    xb[intPointers.length] = (int[]) data;
	    intPointers = xb;
	    break;
	 }
	 case 'J': {
	    long[][] xb = new long[longPointers.length+1][];
	    System.arraycopy(longPointers, 0, xb, 0, longPointers.length);
	    xb[longPointers.length] = (long[]) data;
	    longPointers = xb;
	    break;
	 }
	 case 'F':{
	    float[][] xb = new float[floatPointers.length+1][];
	    System.arraycopy(floatPointers, 0, xb, 0, floatPointers.length);
	    xb[floatPointers.length] = (float[]) data;
	    floatPointers = xb;
	    break;
	 }
	 case 'D':{
	    double[][] xb = new double[doublePointers.length+1][];
	    System.arraycopy(doublePointers, 0, xb, 0, doublePointers.length);
	    xb[doublePointers.length] = (double[]) data;
	    doublePointers = xb;
	    break;
	 }
	 default:
	    throw new TableException("Invalid type for added column:"+classname);
	}
    }
	    


    /** Read a table.
      * @param is The input stream to read from.
      */
    public int read(ArrayDataInput is) throws IOException {

        int currRow = 0;

        // While we have not finished reading the table..
	for (int row=0; row<nrow; row += 1) {
	    
            int ibyte=0;
            int ishort = 0;
            int iint = 0;
            int ilong = 0;
            int ichar = 0;
            int ifloat = 0;
            int idouble = 0;
            int iboolean = 0;

            // Loop over the columns within the row.
            for (int col=0; col < arrays.length; col += 1) {

                int arrOffset = sizes[col]*row;
                int size = sizes[col];
		
                switch(types[col]) {
                  // In anticpated order of use.
                  case 'I':
                    int[] ia = intPointers[iint];
                    iint += 1;
		    is.read(ia, arrOffset, size);
		    break;

                  case 'S':
                    short[] s = shortPointers[ishort];
                    ishort += 1;
		    is.read(s, arrOffset, size);
                    break;

                  case 'B':
                    byte[] b = bytePointers[ibyte];
                    ibyte += 1;
		    is.read(b, arrOffset, size);
                    break;

                  case 'F':
                    float[] f = floatPointers[ifloat];
                    ifloat += 1;
		    is.read(f, arrOffset, size);
                    break;

                  case 'D':
                    double[] d = doublePointers[idouble];
                    idouble += 1;
		    is.read(d, arrOffset, size);
                    break;
		    
                  case 'C':
                    char[] c = charPointers[ichar];
                    ichar += 1;
		    is.read(c, arrOffset, size);
                    break;

                  case 'J':
                    long[] l = longPointers[ilong];
                    ilong += 1;
		    is.read(l, arrOffset, size);
                    break;
		    
                  case 'Z':

                    boolean[] bool = booleanPointers[iboolean];
                    iboolean += 1;
		    is.read(bool, arrOffset, size);
                    break;
		}
	    }
        }

        // All done if we get here...
        return rowSize*nrow;
    }

    /** Write a table.
      * @param os the output stream to write to.
      */
    public int write(ArrayDataOutput os) throws IOException {

        if (rowSize == 0) {
            return 0;
        }
	
	for (int row=0; row<nrow; row += 1) {

            int ibyte    = 0;
            int ishort   = 0;
            int iint     = 0;
            int ilong    = 0;
            int ichar    = 0;
            int ifloat   = 0;
            int idouble  = 0;
            int iboolean = 0;

            // Loop over the columns within the row.
            for (int col=0; col < arrays.length; col += 1) {

                int arrOffset = sizes[col]*row;
                int size = sizes[col];

                switch(types[col]) {
                  // In anticpated order of use.
                  case 'I':
                    int[] ia = intPointers[iint];
                    iint += 1;
		    os.write(ia, arrOffset, size);
                    break;

                  case 'S':
                    short[] s = shortPointers[ishort];
                    ishort += 1;
		    os.write(s, arrOffset, size);
                    break;

                  case 'B':
                    byte[] b = bytePointers[ibyte];
                    ibyte += 1;
		    os.write(b, arrOffset, size);
                    break;

                  case 'F':
                    float[] f = floatPointers[ifloat];
                    ifloat += 1;
		    os.write(f,arrOffset, size);
                    break;

                  case 'D':
                    double[] d = doublePointers[idouble];
                    idouble += 1;
		    os.write(d, arrOffset, size);
                    break;

                  case 'C':
                    char[] c = charPointers[ichar];
                    ichar += 1;
		    os.write(c, arrOffset, size);
                    break;

                  case 'J':
                    long[] l = longPointers[ilong];
                    ilong += 1;
		    os.write(l, arrOffset, size);
                    break;
		    
                  case 'Z':
                    boolean[] bool = booleanPointers[iboolean];
                    iboolean += 1;
		    os.write(bool, arrOffset, size);
                    break;
                }
              
	    }

        }

        // All done if we get here...
        return rowSize*nrow;
    }

    /** Get the base classes of the columns.
      * @return An array of Class objects, one for each column.
      */
    public Class[] getBases() {
        return bases;
    }

    /** Get the characters describing the base classes of the columns.
      * @return An array of char's, one for each column.
      */
    public char[] getTypes() {
        return types;
    }
    
    /** Get the actual data arrays */
    public Object[] getColumns() {
	return arrays;
    }
    
    public int[] getSizes() {
	return sizes;
    }


}


