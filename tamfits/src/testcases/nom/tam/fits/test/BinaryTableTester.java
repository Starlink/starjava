package nom.tam.fits.test;

import nom.tam.util.*;
import nom.tam.fits.*;
import java.io.*;

import java.lang.reflect.*;

/** This class tests the binary table classes for
 *  the Java FITS library, notably BinaryTableHDU,
 *  BinaryTable, FitsHeap and the utility class ColumnTable.
 *  Tests include:
 *  <pre>
 *     Reading and writing data of all valid types.
 *     Reading and writing variable length da
 *     Creating binary tables from:
 *        Object[][] array
 *        Object[] array
 *        ColumnTable
 *        Column x Column
 *        Row x Row
 *     Read binary table
 *        Row x row
 *        Element x element
 *     Modify
 *        Row, column, element
 *     Rewrite binary table in place
 * </pre>
 */

public class BinaryTableTester {
    
    public static void main(String[] args) throws Exception {
	
	byte[]	  bytes  = new byte[50];
	byte[][]  bits   = new byte[50][2];
	boolean[]    bools  = new boolean[50];
	
	short[][] shorts = new short[50][3];
	
	int[]     ints   = new int[50];
	float[][][]   floats = new float[50][4][4];
	
	double[]  doubles= new double[50];
	long[]    longs  = new long[50];
	
	String[]  strings= new String[50];
	
	System.out.println("**** Initialize Arrays ***");
	for (int i=0; i<bytes.length; i += 1) {
	    bytes[i]  = (byte) (2*i);
	    bits[i][0] = bytes[i];
	    bits[i][1] = (byte)(~bytes[i]);
	    bools[i]  = (bytes[i] % 8) == 0 ? true : false;
	    
	    shorts[i][0] = (short) (2*i);
	    shorts[i][1] = (short) (3*i);
	    shorts[i][2] = (short) (4*i);
	    
	    ints[i] = i*i;
	    for (int j=0; j<4; j += 1) {
		for (int k=0; k<4; k += 1) {
		    floats[i][j][k] = (float)(i+j*Math.exp(k));
		}
	    }
	    doubles[i] = 3*Math.sin(i);
	    longs[i] = i*i*i*i;
	    strings[i] = "abcdefghijklmnopqrstuvwxzy".substring(0, i%20);
	}
	
	System.out.println("***** Create a table using columns ****");
	FitsFactory.setUseAsciiTables(false);
	
	Fits f = new Fits();
	f.addHDU(Fits.makeHDU(new Object[]{bytes, bits, bools, shorts, ints,
	    floats, doubles, longs, strings}));
	
	BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
	bhdu.setColumnName(0,"bytes", null);
	bhdu.setColumnName(1,"bits", "bits later on");
	bhdu.setColumnName(6,"doubles",null);
	bhdu.setColumnName(5,"floats", "4 x 4 array");
		 
	BufferedFile bf = new BufferedFile("bt1.fits", "rw");
	System.out.println("writing file");
	
	f.write(bf);
	bf.flush();
	bf.close();
	
	System.out.println("New file");
        f = new Fits("bt1.fits");
	BasicHDU hdu;
	
	System.out.println("read...");
	f.read();
	for (int i=0; i<f.getNumberOfHDUs(); i += 1) {
	    hdu = f.getHDU(i);
	    System.out.println("info...");
	    hdu.info();
	}
	
	BinaryTableHDU thdu = (BinaryTableHDU) f.getHDU(1);
	byte[] tb         = (byte[])   thdu.getColumn(0);
	byte[][] tbits    = (byte[][]) thdu.getColumn(1);
	boolean[] tbools  = (boolean[]) thdu.getColumn(2);
	short[][] tsh     = (short[][]) thdu.getColumn(3);
	int[]     tints   = (int[])     thdu.getColumn(4);
	System.out.println("Got tint");
	float[][][] tflt  = (float[][][])thdu.getColumn(5);
	System.out.println("Got tflt");
	double[]  tdoub   = (double[])  thdu.getColumn(6);
	long[]  tlong     = (long[]) thdu.getColumn(7);
	String[] tstr     = (String[]) thdu.getColumn(8);
	
	System.out.println("Checking rows...");
	
	for (int i=0; i < tb.length; i += 1) {
	    if (i > 0 && i%10 == 0) {
		System.out.println("Checking row:"+i+" "+bytes[i]+" "+tb[i]);
	    }
	    if (tb[i] != bytes[i]) {
		System.out.println("Byte Mismatch at:"+i+" "+bytes[i]+" "+tb[i]);
	    }
	    for (int j=0; j<2; j += 1) {
		if (tbits[i][j] != bits[i][j]) {
		    System.out.println("Bit mismatch at:"+i+","+j);
		}
	    }
	    for (int j=0; j<3; j += 1) {
		if (tsh[i][j] != shorts[i][j]) {
		    System.out.println("Short mismatch at:"+i+","+j);
		}
	    }
	    
	    for (int j=0; j<4; j += 1) {
		for (int k=0; k<4; k += 1) {
		    if (tflt[i][j][k] != floats[i][j][k]) {
			System.out.println("Float mismatch at:"+
					   i+","+j+","+k);
		    }
		}
	    }
	    if (tbools[i] != bools[i]) {
		System.out.println("Boolean mismatch at:"+i+" "+
				   bools[i]+" "+tbools[i]);
	    }
	    
	    if (tints[i] != ints[i]) {
		System.out.println("Int mismatch at:"+i+" "+ints[i]+" "+tints[i]);
	    }
	    if (tdoub[i] != doubles[i]) {
		System.out.println("Double mismatch at:"+i);
	    }
	    if (tlong[i] != longs[i]) {
		System.out.println("Long mismatch at:"+i+" "+longs[i]+" "+
				   tlong[i]);
	    }
	    if (!tstr[i].equals(strings[i])) {
		System.out.println("String mismatch at:"+i+" "+
				   tstr[i]+" "+strings[i]);
	    }
	}
	
	
	
        System.out.println("*** Create some variable length columns ***");
	
	float[][]   vf    = new float[50][];
	short[][]   vs    = new short[50][];
	double[][]  vd    = new double[50][];
	boolean[][] vbool = new boolean[50][]; 
	
	for (int i=0; i<50; i += 1) {
	    vf[i] = new float[i+1];
	    vf[i][i/2] = i*3;
	    vs[i] = new short[i/10+1];
	    vs[i][i/10] = (short) -i;
	    vd[i] = new double[i%2 == 0? 1:2];
	    vd[i][0] = 99.99;
	    vbool[i] = new boolean[i/10];
	    if (i >= 10) {
		vbool[i][0] = i%2 == 1;
	    }
	}
	
	FitsFactory.setUseAsciiTables(false);
	hdu = Fits.makeHDU(new Object[]{floats, vf, vs, vd, shorts, vbool});
	f = new Fits();
	f.addHDU(hdu);
	
	BufferedDataOutputStream bdos = new BufferedDataOutputStream(new FileOutputStream("bt2.fits"));
	f.write(bdos);
	
	f = new Fits("bt2.fits");
	f.read();
	for (int i=0; i<f.size(); i += 1) {
	    f.getHDU(i).info();
	}
	
	bhdu = (BinaryTableHDU) f.getHDU(1);
	
	float[][] tvf = (float[][]) bhdu.getColumn(1);
	short[][] tvs = (short[][]) bhdu.getColumn(2);
	double[][] tvd = (double[][]) bhdu.getColumn(3);
	boolean[][] tvbool = (boolean[][]) bhdu.getColumn(5);
	
	for (int i=0; i<50; i += 10) {
	    
	    System.out.println("Row "+i);
	    System.out.println("   float len:"+tvf[i].length);
	    System.out.println("   short len:"+tvs[i].length);
	    System.out.println("   doub  len:"+tvd[i].length);
	    System.out.println("   bool  len:"+tvbool[i].length);
	    
	    if (tvf[i].length != vf[i].length) {
		System.out.println("Flt Length mismatch on floats.");
	    } else {
		for (int j=0; j<tvf[i].length; j += 1) {
		    if (tvf[i][j] != vf[i][j]) {
			System.out.println("Flt Value mismatch at:"+i+","+j);
		    }
		}
	    }
	    
	    if (tvs[i].length != vs[i].length) {
		System.out.println("Short Length mismatch on floats.");
	    } else {
		for (int j=0; j<tvs[i].length; j += 1) {
		    if (tvs[i][j] != vs[i][j]) {
			System.out.println("Short Value mismatch at:"+i+","+j);
		    }
		}
	    }
	    if (tvd[i].length != vd[i].length) {
		System.out.println("DBL Length mismatch on floats.");
	    } else {
		for (int j=0; j<tvd[i].length; j += 1) {
		    if (tvd[i][j] != vd[i][j]) {
			System.out.println("DBL Value mismatch at:"+i+","+j);
		    }
		}
	    }
	    if (tvbool[i].length != vbool[i].length) {
		System.out.println("Bool Length mismatch on floats.");
	    } else {
		for (int j=0; j<tvbool[i].length; j += 1) {
		    if (tvbool[i][j] != vbool[i][j]) {
			System.out.println("Bool Value mismatch at:"+i+","+j);
		    }
		}
	    }
	}
	
	System.out.println("**** Build a table by columns ****");
	
	BinaryTable btab = new BinaryTable();
	
	btab.addColumn(floats);
	btab.addColumn(vf);
	btab.addColumn(strings);
	btab.addColumn(vbool);
	btab.addColumn(ints);
	
        f = new Fits();
	f.addHDU(Fits.makeHDU(btab));
	
	bdos = new BufferedDataOutputStream(new FileOutputStream("bt3.fits"));
	f.write(bdos);
	
	f = new Fits("bt3.fits");
	bhdu = (BinaryTableHDU) f.getHDU(1);
	btab = (BinaryTable) bhdu.getData();
	
	float[] flatfloat = (float[]) btab.getFlattenedColumn(0);
	tvf = (float[][])btab.getColumn(1);
	String[] xstr = (String[])btab.getColumn(2);
	tvbool = (boolean[][])btab.getColumn(3);
	
	for (int i=0; i<50; i += 3) {
	    System.out.println("Row:"+i);
	    System.out.println("   flatFloats:"+flatfloat[16*i]+","+flatfloat[16*i+1]);
	    System.out.print  ("   float.length"+tvf[i].length);
	    if (tvf[i].length > 0) {
		System.out.println(" "+tvf[i][0]);
	    } else {
		System.out.println(" ");
	    }
	    System.out.println("   str:"+xstr[i]);
	    System.out.print  ("   bool.length:"+tvbool[i].length);
	    if (tvbool[i].length > 0) {
		System.out.println(" "+tvbool[i][0]);
	    } else {
		System.out.println(" ");
	    }
	}
	
	System.out.println("*** Test reading and writing by row.  We'll add 50 more rows***");
	for (int i=0; i<50; i += 1) {
	    Object[] row = btab.getRow(i);
	    float[] qx = (float[]) row[1];
	    
	    String p = (String)row[2];
	    row[2] = "new string:"+i;
	    btab.addRow(row);
	}

	f = new Fits();
	f.addHDU(Fits.makeHDU(btab));
	bf = new BufferedFile("bt4.fits", "rw");
	f.write(bf);
	bf.flush();
	bf.close();
	
	f = new Fits("bt4.fits");
	
	btab = (BinaryTable) f.getHDU(1).getData();
	
	// Try getting data before we read in the table.
	
	xstr = (String[]) btab.getColumn(2);

        System.out.println("**** Reading from file ****");
	for (int i=0; i<xstr.length; i += 3) {
	
	    boolean[] ba = (boolean[]) btab.getElement(i, 3);
	    float[] fx = (float[]) btab.getElement(i,1);
	    float[][] tst = (float[][]) btab.getElement(i,0);
	    String s = (String) btab.getElement(i,2);
	    System.out.println("Row "+i+"   String is"+s);

            int trow = i%50;

            for (int j=0; j<ba.length; j += 1) {
	        if (ba[j] != vbool[trow][j]) {
		    System.out.println("Bool Mismatch at:"+i+" "+j);
		}
	    }
            for (int j=0; j<fx.length; j += 1) {
	        if (fx[j] != vf[trow][j]) {
		    System.out.println("Float Mismatch at:"+i+" "+j);
		}
	    }
 
	}
	// Fill the table.
	f.getHDU(1).getData();
        System.out.println("**** Reading from memory ****");

        xstr = (String[]) btab.getColumn(2);
	
	for (int i=0; i<xstr.length; i += 3) {
	
	    boolean[] ba = (boolean[]) btab.getElement(i, 3);
	    float[] fx = (float[]) btab.getElement(i,1);
	    float[][] tst = (float[][]) btab.getElement(i,0);
	    String s = (String) btab.getElement(i,2);
	    System.out.println("Row "+i+"   String is"+s);

            int trow = i%50;

            for (int j=0; j<ba.length; j += 1) {
	        if (ba[j] != vbool[trow][j]) {
		    System.out.println("Bool Mismatch at:"+i+" "+j);
		}
	    }
            for (int j=0; j<fx.length; j += 1) {
	        if (fx[j] != vf[trow][j]) {
		    System.out.println("Float Mismatch at:"+i+" "+j);
		}
	    }
 
	}
	
	/*** Create a binary table from an Object[][] array */
	Object[][] x = new Object[5][3];
	for (int i=0; i<5; i += 1) {
	    x[i][0] = new float[]{i};
	    x[i][1] = new String("AString"+i);
	    x[i][2] = new int[][]{{i,2*i},{3*i,4*i}};
	}
	
	f = new Fits();
	FitsFactory.setUseAsciiTables(false);
	hdu = Fits.makeHDU(x);
        hdu.info();
	f.addHDU(hdu);
	bf = new BufferedFile("bt5.fits", "rw");
	f.write(bf);
	bf.close();
		
    }    
}
