package nom.tam.fits.test;

import nom.tam.util.*;
import nom.tam.fits.*;
/** This class tests the AsciiTableHDU and AsciiTable FITS
 *  classes and implicitly the ByteFormatter and ByteParser
 *  classes in the nam.tam.util library.
 *  Tests include:
 *     Create columns of every type
 *     Read columns of every type
 *     Create a table column by column
 *     Create a table row by row
 *     Use deferred input on rows
 *     Use deferred input on elements
 *     Read rows, columns and elements from in-memory kernel.
 *     Specify width of columns.
 *     Rewrite data/header  in place.
 *     Set and read null elements.
 */
public class AsciiTableTester {
    
    public static void main(String[] args) throws Exception {
	
	float[]   realCol = new float[50];
	for (int i=0; i<realCol.length; i += 1) {
	    realCol[i] = 10000.F*(i)*(i)*(i);
	}
	
	int[] intCol   = (int[]) ArrayFuncs.convertArray(realCol, int.class);
	long[] longCol  = (long[]) ArrayFuncs.convertArray(realCol, long.class);
	double[] doubleCol =(double[])  ArrayFuncs.convertArray(realCol, double.class);
	
	String[] strCol = new String[realCol.length];
	
	for (int i=0; i<realCol.length; i += 1) {
	    strCol[i] = "ABC"+String.valueOf(realCol[i])+"CDE";
	}

	System.out.println("**** Create a table from the data kernel****");
        Fits f = new Fits();
	Object[] obj = new Object[]{realCol, intCol, longCol, doubleCol, strCol};
	
	f.addHDU(Fits.makeHDU(obj));
	
	BufferedFile bf = new BufferedFile("at1.fits", "rw");
        f.write(bf);
	bf.flush();
	bf.close();
	System.out.println("**** Read table **** ");
	f = new Fits("at1.fits");
	AsciiTableHDU hdu = (AsciiTableHDU) f.getHDU(1);
	Object[] info = (Object[]) hdu.getKernel();
	float[]  f1   = (float[]) info[0];
	String[] s1   = (String[]) info[4];
	
	System.out.println("Print selected rows and data ");
	for (int i=0; i<10; i += 1) {
	    System.out.println(i+":"+f1[i]+" "+s1[i]);
	    int j = f1.length - i-1;
	    System.out.println(j+":"+f1[j]+" "+s1[j]);
	}
	
        AsciiTable data = new AsciiTable();

	System.out.println("\n\n**** Create a table column by column ****");
	
	data.addColumn(longCol);
	data.addColumn(realCol);
	data.addColumn(intCol, 20);
	data.addColumn(strCol, 10);
	
	f = new Fits();
	f.addHDU(Fits.makeHDU(data));
	
	System.out.println("**** Create a table row by row ****");
	
	// Create a table row by row .
	data = new AsciiTable();
	Object[] row = new Object[4];
	for (int i=0; i<realCol.length; i += 1) {
	    row[0] = new String[]{strCol[i]};
	    row[3] = new float[]{realCol[i]};
	    row[1] = new int[]{intCol[i]};
	    row[2] = new double[]{doubleCol[i]};
	    data.addRow(row);
	}
	f.addHDU(Fits.makeHDU(data));
	
	System.out.println("**** Write out multiple tables ****");
	bf = new BufferedFile("at2.fits", "rw");
	f.write(bf);
	
	bf.flush();
	bf.close();
	
	System.out.println("**** Read multiple tables ****");
	f = new Fits("at2.fits");
	for (int i=0; i<3; i += 1) {
	    System.out.println("Reading at HDU #"+i);
	    f.readHDU().info();
	}
	System.out.println("**** Check col x col table ****");
	System.out.println("--- use preserved links in FITS object ----");
	info = (Object[]) f.getHDU(1).getKernel();
	f1   = (float[]) info[1];
	s1   = (String[]) info[3];
	
	System.out.println("Print selected rows and data ");
	for (int i=0; i<10; i += 1) {
	    System.out.println(i+":"+f1[i]+" "+s1[i]);
	    int j = f1.length - i-1;
	    System.out.println(j+":"+f1[j]+" "+s1[j]);
	}
	System.out.println("**** Check row x row table ****");
	info = (Object[]) f.getHDU(2).getKernel();
	f1   = (float[]) info[3];
	s1   = (String[]) info[0];
	
	System.out.println("Print selected rows and data ");
	for (int i=0; i<10; i += 1) {
	    System.out.println(i+":"+f1[i]+" "+s1[i]);
	    int j = f1.length - i-1;
	    System.out.println(j+":"+f1[j]+" "+s1[j]);
	}
	
	System.out.println("****Check out row, elem, col input ****");
	f = new Fits("at1.fits");
	
	hdu = (AsciiTableHDU) f.getHDU(1);
	data = (AsciiTable) hdu.getData();
    
	for (int i=0; i<10; i += 1) {
	    row = data.getRow(i);
	    f1 = (float[]) row[0];
	    s1 = (String[]) row[4];
	    System.out.println("Row "+i+":"+f1[0]+"   "+s1[0]);
	    f1 = (float[]) data.getElement(i,0);
	    s1 = (String[]) data.getElement(i,4);
	    System.out.println("Ele "+i+":"+f1[0]+"   "+s1[0]);
        }
	
	f1 = (float[]) data.getColumn(0);
	s1 = (String[]) data.getColumn(4);
	for (int i=0; i<10; i += 1) {
	    System.out.println("Col "+i+":"+f1[i]+"   "+s1[i]);
	}
	
	System.out.println("Check row/elem from memory ");
	for (int i=0; i<10; i += 1) {
	    
	    row = data.getRow(i);
	    f1 = (float[]) row[0];
	    s1 = (String[]) row[4];
	    System.out.println("Row "+i+":"+f1[0]+"   "+s1[0]);
	    f1 = (float[]) data.getElement(i,0);
	    s1 = (String[]) data.getElement(i,4);
	    System.out.println("Ele "+i+":"+f1[0]+"   "+s1[0]);
	    
        }

	System.out.println("**** Check modifying rows, table, cols****");
	f1 = (float[]) data.getColumn(0);
	float[] f2 = (float[]) f1.clone();
	for (int i=0; i<f2.length; i += 1) {
	    f2[i] = 2*f2[i];
	}
	
	data.setColumn(0, f2);
	f1 = new float[]{3.14159f};
	data.setElement(3,0,f1);
	
	hdu.setNullString(0, "**INVALID**");
	data.setNull(5, 0, true);
	data.setNull(6, 0, true);
	
	row = new Object[5];
	row[0] = new float[]{6.28f
	};
	row[1] = new int[]{22
	};
	row[2] = new long[]{0
	};
	row[3] = new double[]{-3
	};
	row[4] = new String[]{"A string"};
	data.setRow(4,row);
	
	System.out.println("**** Rewrite (for grins rewrite header second)  ****");
	System.out.println("Read data from file and note changes.");
//	data.rewrite();
	hdu.getHeader().rewrite();

	f = new Fits("at1.fits");
	data = (AsciiTable) f.getHDU(1).getData();
	for (int i=0; i<10; i += 1) {
	    row = data.getRow(i);
	    
	    f1 = (float[]) row[0];
	    if (f1 != null) {
	        s1 = (String[]) row[4];
	        System.out.println("Row "+i+":"+f1[0]+"   "+s1[0]);
	        f1 = (float[]) data.getElement(i,0);
	        s1 = (String[]) data.getElement(i,4);
	        System.out.println("Ele "+i+":"+f1[0]+"   "+s1[0]);
	    } else {
		System.out.println("Row:"+i+" has null element");
	    }
        }
	f1 = (float[]) data.getColumn(0);
	for (int i=0; i<10; i += 1) {
	    // isNull is set since the kernel was read in!
	    // 
	    if (data.isNull(i,0)) {
		System.out.println("Col:"+i+ " null flag is set");
	    } else {
		System.out.println("Col:"+i+" "+f1[i]);
	    }
	}
	
	
	
    }
}
	
	
