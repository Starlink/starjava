package nom.tam.util.test;

/** This class tests the ByteFormatter and ByteParser classes.
 */

import nom.tam.util.*;

public class ByteFormatParseTester {
    
    public static void main(String[] args) throws Exception {
	
	byte[] buffer = new byte[100000];
	ByteFormatter bf = new ByteFormatter();
	ByteParser    bp = new ByteParser(buffer);
	
	bf.setAlign(true);
	bf.setTruncationThrow(false);
	
	int[] tint = new int[100];
	
	tint[0] = Integer.MIN_VALUE;
	tint[1] = Integer.MAX_VALUE;
	tint[2] = 0;
	
	for (int i=0; i<tint.length; i += 1) {
	    tint[i] = (int)(Integer.MAX_VALUE*(2*(Math.random()-.5)));
	}
	
	System.out.println("Check formatting options...\n");
	System.out.println("\n\nFilled, right aligned");
	int colSize = 12;
	int cnt = 0;
	int offset = 0;
	while (cnt < tint.length) {
	    offset = bf.format(tint[cnt], buffer, offset, colSize);
	    cnt += 1;
	    if (cnt % 8 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println(new String(buffer, 0, offset));
	
	bp.setOffset(0);
	
	boolean error = false;
	for (int i=0; i<tint.length; i += 1) {
	    
	    int chk = bp.getInt(colSize);
	    if (chk != tint[i]) {
		error = true;
		System.out.println("Mismatch at:"+i+" "+tint[i]+"!="+chk);
	    }
	    if ((i+1) % 8 == 0) {
		bp.skip(1);
	    }
	}
	if (!error) {
	    System.out.println("No errors in ByteParser: getInt");
	}
	
	
	System.out.println("\n\nFilled, left aligned");
	bf.setAlign(false);
	offset = 0;
	colSize = 12;
	cnt = 0;
	offset = 0;
	while (cnt < tint.length) {
	    int oldOffset = offset;
	    offset = bf.format(tint[cnt], buffer, offset, colSize);
	    int nb = colSize- (offset- oldOffset);
	    if (nb > 0) {
		offset = bf.alignFill(buffer, offset, nb);
	    }
	    cnt += 1;
	    if (cnt % 8 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println(new String(buffer, 0, offset));
	
	System.out.println("\n\nUnfilled, left aligned -- no separators (hard to read)");
	offset = 0;
	colSize = 12;
	cnt = 0;
	offset = 0;
	while (cnt < tint.length) {
	    offset = bf.format(tint[cnt], buffer, offset, colSize);
	    cnt += 1;
	    if (cnt % 8 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println(new String(buffer, 0, offset));
	
	System.out.println("\n\nUnfilled, left aligned -- single space separators");
	bf.setAlign(false);
	offset = 0;
	colSize = 12;
	cnt = 0;
	offset = 0;
	while (cnt < tint.length) {
	    
	    offset = bf.format(tint[cnt], buffer, offset, colSize);
	    offset = bf.format(" ", buffer, offset, 1);
	    
	    cnt += 1;
	    if (cnt % 8 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println(new String(buffer, 0, offset));
	    
	
	System.out.println("\n\nTest throwing of trunction exception");
	
	bf.setTruncationThrow(false);
	int val = 1;
	for (int i=0; i<10; i += 1) {
	    offset = bf.format(val, buffer, 0, 6);
	    System.out.println("At power:"+ i +" in six chars we get:"+new String(buffer, 0, offset));
	    val *= 10;
	}
	
	System.out.println("Now enabling TruncationExceptions");
	bf.setTruncationThrow(true);
	val = 1;
	for (int i=0; i<10; i += 1) {
	    try {
	        offset = bf.format(val, buffer, 0, 6);
	    } catch (TruncationException e) {
		System.out.println("Caught TruncationException for power "+i);
	    }
	    System.out.println("At power:"+ i +" in six chars we get:"+new String(buffer, 0, offset));
	    val *= 10;
	}
	
	long[] lng = new long[100];
	for (int i=0; i<lng.length; i += 1) {
	    lng[i] = (long)(Long.MAX_VALUE*(2*(Math.random()-0.5)));
	}
	
	lng[0] = Long.MAX_VALUE;
	lng[1] = Long.MIN_VALUE;
	lng[2] = 0;
	
	bf.setTruncationThrow(false);
	bf.setAlign(true);
	offset = 0;
	cnt = 0;
	while (cnt < lng.length) {
	    offset = bf.format(lng[cnt], buffer, offset, 20);
	    cnt += 1;
	    if (cnt % 4 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println("\n\nLongs:\n"+new String(buffer, 0, offset));
	bp.setOffset(0);
	
	error = false;
	for (int i=0; i<lng.length; i += 1) {
	    long chk = bp.getLong(20);
	    if (chk != lng[i]) {
		System.out.println("Error in getLong:"+i+"  "+lng[i]+" != "+chk);
	        error = true;
	    }
	    if ((i+1)%4 == 0) {
		bp.skip(1);
	    }
	}
	if (!error) {
	    System.out.println("No errors in ByteParser: getLong!");
	}
	
	
	float[] flt = new float[100];
	for (int i=0; i<flt.length; i += 1) {
	    flt[i] = (float)(2*(Math.random()-0.5) * Math.pow(10, 60*(Math.random()-0.5)));
	}
	
	flt[0] = Float.MAX_VALUE;
	flt[1] = Float.MIN_VALUE;
	flt[2] = 0;
	flt[3] = Float.NaN;
	flt[4] = Float.POSITIVE_INFINITY;
	flt[5] = Float.NEGATIVE_INFINITY;
	
	
	bf.setTruncationThrow(false);
	bf.setAlign(true);
	
	offset = 0;
	cnt = 0;
	while (cnt < flt.length) {
	    offset = bf.format(flt[cnt], buffer, offset, 24);
	    cnt += 1;
	    if (cnt % 4 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println("\n\nFloats:\n"+new String(buffer, 0, offset));
	
	bp.setOffset(0);
	double delta = 0;
	for (int i=0; i<flt.length; i += 1) {
	    
	    // Skip NaNs and Infinities.
	    if (i > 2 && i < 6) {
		bp.skip(24);
	    } else {
	    
	        float chk = bp.getFloat(24);
	    
	        float dx = Math.abs(chk-flt[i]);
	        if (dx > delta) {
		    System.out.println("Float  High delta:"+i+" "+flt[i]+" "+chk);
		    System.out.println("       Relative error:"+dx/flt[i]);
	            delta = dx;
	        }
	    }
	    if ((i+1) % 4 == 0) {
		bp.skip(1);
	    }
	}
	
	double[] dbl = new double[100];
	for (int i=0; i<dbl.length; i += 1) {
	    dbl[i] = 2*(Math.random()-0.5) * Math.pow(10, 60*(Math.random()-0.5));
	}
	
	dbl[0] = Double.MAX_VALUE;
	dbl[1] = Double.MIN_VALUE;
	dbl[2] = 0;
	dbl[3] = Double.NaN;
	dbl[4] = Double.POSITIVE_INFINITY;
	dbl[5] = Double.NEGATIVE_INFINITY;
	
	
	bf.setTruncationThrow(false);
	bf.setAlign(true);
	offset = 0;
	cnt = 0;
	while (cnt < lng.length) {
	    offset = bf.format(dbl[cnt], buffer, offset, 25);
	    cnt += 1;
	    if (cnt % 4 == 0) {
		offset = bf.format("\n", buffer, offset, 1);
	    }
	}
	System.out.println("\n\nDoubles:\n"+new String(buffer, 0, offset));
	
	bp.setOffset(0);
	delta = 0;
	for (int i=0; i<dbl.length; i += 1) {
	    
	    // Skip NaNs and Infinities.
	    if (i > 2 && i < 6) {
		bp.skip(25);
	    } else {
	    
	        double chk = bp.getDouble(25);
	    
	        double dx = Math.abs(chk-dbl[i]);
	        if (dx > delta) {
		    System.out.println("Double  High delta:"+i+" "+dbl[i]+" "+chk);
		    System.out.println("       Relative error:"+dx/dbl[i]);
	            delta = dx;
	        }
	    }
	    if ((i+1) % 4 == 0) {
		bp.skip(1);
	    }
	}
	
	bp.setOffset(0);
	bp.skip(4*25 + 1 + 2*25);
	for (int i=0; i<30; i += 1) {
	    System.out.println("Reading doubles..."+bp.getDouble());
	}
	
	boolean[] btst = new boolean[100];
	for (int i=0; i<btst.length; i += 1) {
	    btst[i] = Math.random() > 0.5;
	}
	offset= 0;
	for (int i=0; i<btst.length; i += 1) {
	    offset =bf.format(btst[i], buffer, offset, 1);
	}
	System.out.println("Booleans are:"+new String(buffer, 0, btst.length));
			   
	
	bp.setOffset(0);
	for (int i=0; i<btst.length; i += 1) {
	    boolean bt = bp.getBoolean();
	    if (bt != btst[i]) {
		System.out.println("Mismatch at:"+i+" "+btst[i]+" != "+bt);
	    }
	}
	
	offset = 0;
	String bigStr="abcdefghijklmnopqrstuvwxyz";
	for (int i=0; i<100; i += 1) {
	    offset = bf.format(bigStr.substring(i % 27), buffer, offset, 13);
	    offset = bf.format(" ", buffer, offset, 1);
	}
	
	bp.setOffset(0);
	for (int i=0; i<100; i += 1) {
	    String s = bp.getString(13);
	    System.out.println(i+":"+s);
	    bp.skip(1);
	}
	
    }
}
