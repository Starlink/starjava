/* Copyright: Thomas McGlynn 1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */

package nom.tam.util.test;

import nom.tam.util.*;
import java.io.*;

/** This class provides runs tests of the
 *  BufferedI/O classes: BufferedFile, BufferedDataInputStream
 *  and BufferedDataOutputStream.  A limited comparison
 *  to the standard I/O classes can also be made.
 *  <p>
 *  Input and output of all primitive scalar and array types is
 *  tested, however input and output of String data is not.
 *  Users may choose to test the BufferedFile class, the
 *  BufferedDataXPUT classes array methods, the BufferedDataXPUT
 *  classes using the methods of DataXput, the traditional
 *  I/O classes, or any combination thereof.
 */
public class BufferedFileTester {


    /** Usage: java nom.tam.util.test.BufferedFileTester file [dim [iter [flags]]]
     *         where 
     *	       file 	is the file to be read and written.
     *         dim 	is the dimension of the arrays to be written.
     *         iter 	is the number of times each array is written.
     *         flags	a string indicating what I/O to test
     *                  O  -- test old I/O (RandomAccessFile and standard streams)
     *                  R  -- BufferedFile (i.e., random access)
     *                  S  -- BufferedDataXPutStream
     *                  X  -- BufferedDataXPutStream using standard methods
     */
    public static void main(String[] args) throws Exception {
    
        String filename = args[0];
	int dim = 1000;
	if (args.length > 1) {
	     dim = Integer.parseInt(args[1]);
	}
	int iter = 1;
	if (args.length > 2) {
	    iter = Integer.parseInt(args[2]);
	}
	
	System.out.println("Allocating arrays.");
	double[] db = new double[dim];
	float[] fl = new float[dim];
	int[]   in = new int[dim];
	long[]  ln = new long[dim];
	short[] sh = new short[dim];
	byte[]  by = new byte[dim];
	char[]  ch = new char[dim];
	boolean[] bl= new boolean[dim];
	
	System.out.println("Initializing arrays -- may take a while");
	int sign = 1;
	for (int i=0; i < dim; i += 1) {
	
	    
	    double x = sign  * Math.pow(10., 20*Math.random()-10);
	    db[i] = x;
	    fl[i] = (float)x;
	    
	    if (Math.abs(x) < 1) {
		x = 1/x;
	    }
	    
	    in[i] = (int)   x;
	    ln[i] = (long)  x;
	    sh[i] = (short) x;
	    by[i] = (byte)  x;
	    ch[i] = (char)  x;
	    bl[i] = x > 0;
	    
	    sign = - sign;
	}
	
	// Ensure special values are tested.
	
	by[0] = Byte.MIN_VALUE;
	by[1] = Byte.MAX_VALUE;
	by[2] = 0;
	ch[0] = Character.MIN_VALUE;
	ch[1] = Character.MAX_VALUE;
	ch[2] = 0;
	sh[0] = Short.MAX_VALUE;
	sh[1] = Short.MIN_VALUE;
	sh[0] = 0;
	in[0] = Integer.MAX_VALUE;
	in[1] = Integer.MIN_VALUE;
	in[2] = 0;
	ln[0] = Long.MIN_VALUE;
	ln[1] = Long.MAX_VALUE;
	ln[2] = 0;
	fl[0] = Float.MIN_VALUE;
	fl[1] = Float.MAX_VALUE;
	fl[2] = Float.POSITIVE_INFINITY;
	fl[3] = Float.NEGATIVE_INFINITY;
	fl[4] = Float.NaN;
	fl[5] = 0;
	db[0] = Double.MIN_VALUE;
	db[1] = Double.MAX_VALUE;
	db[2] = Double.POSITIVE_INFINITY;
	db[3] = Double.NEGATIVE_INFINITY;
	db[4] = Double.NaN;
	db[5] = 0;
	
	double[] db2 = new double[dim];
	float[] fl2 = new float[dim];
	int[]   in2 = new int[dim];
	long[]  ln2 = new long[dim];
	short[] sh2 = new short[dim];
	byte[]  by2 = new byte[dim];
	char[]  ch2 = new char[dim];
	boolean[] bl2= new boolean[dim];
	
	int[][][][] multi  = new int[10][10][10][10];
	int[][][][] multi2 = new int[10][10][10][10];
	for (int i=0; i<10; i += 1) {
	    multi[i][i][i][i] = i;
	}
	
	if (args.length < 4 || args[3].indexOf('O') >= 0) {
	    standardFileTest(filename,   iter, in, in2);
	    standardStreamTest(filename, iter, in, in2);
	}
	
	if (args.length < 4 || args[3].indexOf('X') >= 0) {
	    buffStreamSimpleTest(filename,   iter, in, in2);
	}
	
	if (args.length < 4 || args[3].indexOf('R') >= 0) {
	    bufferedFileTest(filename,iter,db,db2,fl,fl2,ln,ln2,in,in2,sh,sh2,
			 ch,ch2,by,by2,bl,bl2, multi,multi2);
	}
	
	
	if (args.length < 4 || args[3].indexOf('S') >= 0) {
	    bufferedStreamTest(filename,iter,db,db2,fl,fl2,ln,ln2,in,in2,sh,sh2,
			 ch,ch2,by,by2,bl,bl2, multi,multi2);
	}
    }
    
    public static void standardFileTest(String filename, int iter, int[] in, int[] in2) 
    throws Exception {
	System.out.println("Standard I/O library: java.io.RandomAccessFile");
	
	RandomAccessFile f = new RandomAccessFile(filename, "rw");
	int dim = in.length;
	resetTime();
	f.seek(0);
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		f.writeInt(in[i]);
	    }
	}
	System.out.println("  RAF Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	f.seek(0);
	resetTime();
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		in2[i]= f.readInt();
	    }
	}
	System.out.println("  RAF Int read:  "+  (4*dim*iter)/(1000*deltaTime()));
	
	
	synchronized(f) {
	f.seek(0);
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		f.writeInt(in[i]);
	    }
	}
	System.out.println("  SyncRAF Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	f.seek(0);
	resetTime();
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		in2[i]= f.readInt();
	    }
	}
	}
	System.out.println("  SyncRAF Int read:  "+  (4*dim*iter)/(1000*deltaTime()));
    }
    
    public static void standardStreamTest(String filename, int iter, int[] in, int[] in2) 
    throws Exception {
	System.out.println("Standard I/O library: java.io.DataXXputStream");
	System.out.println("                      layered atop a BufferedXXputStream");
	
	DataOutputStream f = new DataOutputStream(new BufferedOutputStream(
				 new FileOutputStream(filename), 32768));
	resetTime();
	int dim=in.length;
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		f.writeInt(in[i]);
	    }
	}
	f.flush();
	f.close();
	System.out.println("  DIS Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	
	DataInputStream is = new DataInputStream(new BufferedInputStream(
				 new FileInputStream(filename), 32768));
	resetTime();
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		in2[i] = is.readInt();
	    }
	}
	System.out.println("  DIS Int read:  "+  (4*dim*iter)/(1000*deltaTime()));
	
	
	f = new DataOutputStream(new BufferedOutputStream(
				 new FileOutputStream(filename), 32768));
	resetTime();
	dim=in.length;
	synchronized(f) {
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		f.writeInt(in[i]);
	    }
	}
	f.flush();
	f.close();
	System.out.println("  DIS Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	
	is = new DataInputStream(new BufferedInputStream(
				 new FileInputStream(filename), 32768));
	resetTime();
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		in2[i] = is.readInt();
	    }
	}
	}
	System.out.println("  DIS Int read:  "+  (4*dim*iter)/(1000*deltaTime()));
    }
    
    public static void buffStreamSimpleTest(String filename, int iter, int[] in, int[] in2) 
    throws Exception {
	
	System.out.println("New libraries:  nom.tam.BufferedDataXXputStream");
	System.out.println("                Using non-array I/O");
	BufferedDataOutputStream f = new BufferedDataOutputStream(
				 new FileOutputStream(filename), 32768);
	resetTime();
	int dim=in.length;
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		f.writeInt(in[i]);
	    }
	}
	f.flush();
	f.close();
	System.out.println("  BDS Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	
	BufferedDataInputStream is = new BufferedDataInputStream(new BufferedInputStream(
				 new FileInputStream(filename), 32768));
	resetTime();
	for(int j=0; j<iter; j += 1) {
	    for (int i=0; i<dim; i += 1) {
		in2[i] = is.readInt();
	    }
	}
	System.out.println("  BDS Int read:  "+  (4*dim*iter)/(1000*deltaTime()));
    }
    
    
    public static void bufferedStreamTest(String filename, int iter, double[] db, double[] db2,
				 float[] fl, float[] fl2, long[] ln, long[] ln2,
				 int[] in, int[] in2, short[] sh, short[] sh2,
				 char[] ch, char[] ch2, byte[] by, byte[] by2,
				 boolean[] bl, boolean[] bl2,
				 int[][][][] multi, int[][][][] multi2) throws Exception {
	
	int dim = db.length;
	
	double ds = Math.random()-0.5;
	double ds2;
	float  fs = (float) (Math.random()-0.5);
	float  fs2;
	int    is = (int) (1000000*(Math.random()-500000));
	int    is2;
	long   ls = (long) (100000000000L*(Math.random()-50000000000L));
	long   ls2;
	short  ss = (short) (60000*(Math.random()-30000));
	short  ss2;
	char   cs = (char) (60000*Math.random());
	char   cs2;
	byte   bs = (byte)(256*Math.random()-128);
	byte   bs2;
        boolean bls = (Math.random() > 0.5);
	boolean bls2;
	System.out.println("New libraries: nom.tam.util.BufferedDataXXputStream");
	System.out.println("               Using array I/O methods");

	  {
	BufferedDataOutputStream f = new BufferedDataOutputStream(new FileOutputStream(filename));
	
	resetTime();
	for (int i=0; i<iter; i+=1) f.writeArray(db);
	System.out.println("  BDS Dbl write: " + (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1) f.writeArray(fl);
	System.out.println("  BDS Flt write: "+  (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(in);
	System.out.println("  BDS Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(ln);
	System.out.println("  BDS Lng write: "+  (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(sh);
	System.out.println("  BDS Sht write: "+  (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(ch);
	System.out.println("  BDS Chr write: "+  (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray((byte[])by);
	System.out.println("  BDS Byt write: "+  (1*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(bl);
	System.out.println("  BDS Boo write: "+  (1*dim*iter)/(1000*deltaTime()));
	
	f.writeByte(bs);
	f.writeChar(cs);
	f.writeShort(ss);
	f.writeInt(is);
	f.writeLong(ls);
	f.writeFloat(fs);
	f.writeDouble(ds);
	f.writeBoolean(bls);
	
	f.writeArray(multi);
	f.flush();
	f.close();
	  }
	
	  {
	BufferedDataInputStream f = new BufferedDataInputStream(new FileInputStream(filename));
       
	
	resetTime();
	for (int i=0; i<iter; i+=1)f.readArray(db2);
	System.out.println("  BDS Dbl read:  "+ (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(fl2);
	System.out.println("  BDS Flt read:  "+ (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(in2);
	System.out.println("  BDS Int read:  "+ (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(ln2);
	System.out.println("  BDS Lng read:  "+ (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(sh2);
	System.out.println("  BDS Sht read:  "+ (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(ch2);
	System.out.println("  BDS Chr read:  "+ (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray((byte[])by2);
	System.out.println("  BDS Byt read:  "+ (1*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(bl2);
	System.out.println("  BDS Boo read:  "+ (1*dim*iter)/(1000*deltaTime()));
	
	bs2=f.readByte();
	cs2=f.readChar();
	ss2=f.readShort();
	is2=f.readInt();
	ls2=f.readLong();
	fs2=f.readFloat();
	ds2=f.readDouble();
	bls2=f.readBoolean();
	
        for (int i=0; i<10; i += 1) {
	    multi2[i][i][i][i] = 0;
	}
	      
	// Now read only pieces of the multidimensional array.
	for (int i=0; i<5; i += 1) {
	    System.out.println("Multiread:"+i);
	    // Skip the odd initial indices and
	    // read the evens.
	    f.skipBytes(4000);
	    f.readArray(multi2[2*i+1]);
	}
	  f.close();
	  }
	
	System.out.println("Stream Verification:");
	System.out.println("  An error should be reported for double and float NaN's");
	System.out.println("  Arrays:");
	
	for (int i=0; i < dim; i += 1) {
	    	    
	    if (db[i] != db2[i]) {
		System.out.println("     Double error at "+i+" "+db[i]+" "+db2[i]);
	    }
	    if (fl[i] != fl2[i]) {
		System.out.println("     Float error at "+i+" "+fl[i]+" "+fl2[i]);
	    }
	    if (in[i] != in2[i]) {
		System.out.println("     Int error at "+i+" "+in[i]+" "+in2[i]);
	    }
	    if (ln[i] != ln2[i]) {
		System.out.println("     Long error at "+i+" "+ln[i]+" "+ln2[i]);
	    }
	    if (sh[i] != sh2[i]) {
		System.out.println("     Short error at "+i+" "+sh[i]+" "+sh2[i]);
	    }
	    if (ch[i] != ch2[i]) {
		System.out.println("     Char error at "+i+" "+(int)ch[i]+" "+(int)ch2[i]);
	    }
	    if (by[i] != by2[i]) {
		System.out.println("     Byte error at "+i+" "+by[i]+" "+by2[i]);
	    }
	    if (bl[i] != bl2[i]) {
		System.out.println("     Bool error at "+i+" "+bl[i]+" "+bl2[i]);
	    }
	}
	
	System.out.println("  Scalars:");
	// Check the scalars.
	if (bls != bls2) {
	    System.out.println("     Bool Scalar mismatch:"+bls+" "+bls2);
	}
	if (bs != bs2) {
	    System.out.println("     Byte Scalar mismatch:"+bs+" "+bs2);
	}
	if (cs != cs2) {
	    System.out.println("     Char Scalar mismatch:"+(int)cs+" "+(int)cs2);
	}
	if (ss != ss2) {
	    System.out.println("     Short Scalar mismatch:"+ss+" "+ss2);
	}
	if (is != is2) {
	    System.out.println("     Int Scalar mismatch:"+is+" "+is2);
	}
	if (ls != ls2) {
	    System.out.println("     Long Scalar mismatch:"+ls+" "+ls2);
	}
	if (fs != fs2) {
	    System.out.println("     Float Scalar mismatch:"+fs+" "+fs2);
	}
	if (ds != ds2) {
	    System.out.println("     Double Scalar mismatch:"+ds+" "+ds2);
	}
	
	System.out.println("  Multi: odd rows should match");
	for (int i=0; i<10; i += 1) {
	    System.out.println("      "+i+" "+multi[i][i][i][i]+" "+multi2[i][i][i][i]);
	}
	System.out.println("Done BufferedStream Tests");
    }
    
    public static void bufferedFileTest(String filename, int iter, double[] db, double[] db2,
				 float[] fl, float[] fl2, long[] ln, long[] ln2,
				 int[] in, int[] in2, short[] sh, short[] sh2,
				 char[] ch, char[] ch2, byte[] by, byte[] by2,
				 boolean[] bl, boolean[] bl2,
				 int[][][][] multi, int[][][][] multi2)  throws Exception {
	      
	
	int dim = db.length;
	
	double ds = Math.random()-0.5;
	double ds2;
	float  fs = (float) (Math.random()-0.5);
	float  fs2;
	int    is = (int) (1000000*(Math.random()-500000));
	int    is2;
	long   ls = (long) (100000000000L*(Math.random()-50000000000L));
	long   ls2;
	short  ss = (short) (60000*(Math.random()-30000));
	short  ss2;
	char   cs = (char) (60000*Math.random());
	char   cs2;
	byte   bs = (byte)(256*Math.random()-128);
	byte   bs2;
        boolean bls = (Math.random() > 0.5);
	boolean bls2;
				     
	System.out.println("New libraries: nom.tam.util.BufferedFile");
	System.out.println("               Using array I/O methods.");

	BufferedFile f = new BufferedFile(filename, "rw");
	
	resetTime();
	for (int i=0; i<iter; i+=1)f.writeArray(db);
	System.out.println("  BF  Dbl write: " + (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(fl);
	System.out.println("  BF  Flt write: "+  (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(in);
	System.out.println("  BF  Int write: "+  (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(ln);
	System.out.println("  BF  Lng write: "+  (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(sh);
	System.out.println("  BF  Sht write: "+  (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(ch);
	System.out.println("  BF  Chr write: "+  (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(by);
	System.out.println("  BF  Byt write: "+  (1*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.writeArray(bl);
	System.out.println("  BF  Boo write: "+  (1*dim*iter)/(1000*deltaTime()));
	
	f.writeByte(bs);
	f.writeChar(cs);
	f.writeShort(ss);
	f.writeInt(is);
	f.writeLong(ls);
	f.writeFloat(fs);
	f.writeDouble(ds);
	f.writeBoolean(bls);
	
	f.writeArray(multi);
	f.seek(0);
       
	
	resetTime();
	for (int i=0; i<iter; i+=1)f.readArray(db2);
	System.out.println("  BF  Dbl read:  "+ (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(fl2);
	System.out.println("  BF  Flt read:  "+ (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(in2);
	System.out.println("  BF  Int read:  "+ (4*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(ln2);
	System.out.println("  BF  Lng read:  "+ (8*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(sh2);
	System.out.println("  BF  Sht read:  "+ (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(ch2);
	System.out.println("  BF  Chr read:  "+ (2*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(by2);
	System.out.println("  BF  Byt read:  "+ (1*dim*iter)/(1000*deltaTime()));
	for (int i=0; i<iter; i+=1)f.readArray(bl2);
	System.out.println("  BF  Boo read:  "+ (1*dim*iter)/(1000*deltaTime()));
	
	bs2=f.readByte();
	cs2=f.readChar();
	ss2=f.readShort();
	is2=f.readInt();
	ls2=f.readLong();
	fs2=f.readFloat();
	ds2=f.readDouble();
	bls2=f.readBoolean();
	
	// Now read only pieces of the multidimensional array.
	for (int i=0; i<5; i += 1) {
	    // Skip the odd initial indices and
	    // read the evens.
	    f.skipBytes(4000);
	    f.readArray(multi2[2*i+1]);
	}
	
	System.out.println("BufferedFile Verification:");
	System.out.println("  An error should be reported for double and float NaN's");
	System.out.println("  Arrays:");
	
	for (int i=0; i < dim; i += 1) {
	    	    
	    if (db[i] != db2[i]) {
		System.out.println("     Double error at "+i+" "+db[i]+" "+db2[i]);
	    }
	    if (fl[i] != fl2[i]) {
		System.out.println("     Float error at "+i+" "+fl[i]+" "+fl2[i]);
	    }
	    if (in[i] != in2[i]) {
		System.out.println("     Int error at "+i+" "+in[i]+" "+in2[i]);
	    }
	    if (ln[i] != ln2[i]) {
		System.out.println("     Long error at "+i+" "+ln[i]+" "+ln2[i]);
	    }
	    if (sh[i] != sh2[i]) {
		System.out.println("     Short error at "+i+" "+sh[i]+" "+sh2[i]);
	    }
	    if (ch[i] != ch2[i]) {
		System.out.println("     Char error at "+i+" "+(int)ch[i]+" "+(int)ch2[i]);
	    }
	    if (by[i] != by2[i]) {
		System.out.println("     Byte error at "+i+" "+by[i]+" "+by2[i]);
	    }
	    if (bl[i] != bl2[i]) {
		System.out.println("     Bool error at "+i+" "+bl[i]+" "+bl2[i]);
	    }
	}
	
	System.out.println("  Scalars:");
	// Check the scalars.
	if (bls != bls2) {
	    System.out.println("     Bool Scalar mismatch:"+bls+" "+bls2);
	}
	if (bs != bs2) {
	    System.out.println("     Byte Scalar mismatch:"+bs+" "+bs2);
	}
	if (cs != cs2) {
	    System.out.println("     Char Scalar mismatch:"+(int)cs+" "+(int)cs2);
	}
	if (ss != ss2) {
	    System.out.println("     Short Scalar mismatch:"+ss+" "+ss2);
	}
	if (is != is2) {
	    System.out.println("     Int Scalar mismatch:"+is+" "+is2);
	}
	if (ls != ls2) {
	    System.out.println("     Long Scalar mismatch:"+ls+" "+ls2);
	}
	if (fs != fs2) {
	    System.out.println("     Float Scalar mismatch:"+fs+" "+fs2);
	}
	if (ds != ds2) {
	    System.out.println("     Double Scalar mismatch:"+ds+" "+ds2);
	}
	
	System.out.println("  Multi: odd rows should match");
	for (int i=0; i<10; i += 1) {
	    System.out.println("      "+i+" "+multi[i][i][i][i]+" "+multi2[i][i][i][i]);
	}
	System.out.println("Done BufferedFile Tests");
    }
    
    static long lastTime;
    static void resetTime() {
	lastTime = new java.util.Date().getTime();
    }
    static double deltaTime() {
	long time = lastTime;
	lastTime = new java.util.Date().getTime();
	return (lastTime-time)/1000.;
    }
}
	
	
