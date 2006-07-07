package nom.tam.util;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */

// What do we use in here?

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/** This class is intended for high performance I/O in scientific applications.
  * It combines the functionality of the BufferedOutputStream and the
  * DataOutputStream as well as more efficient handling of arrays.
  * This minimizes the number of method calls that are required to
  * write data.  Informal tests of this method show that it can
  * be as much as 10 times faster than using a DataOutputStream layered
  * on a BufferedOutputStream for writing large arrays.  The performance
  * gain on scalars or small arrays will be less but there should probably
  * never be substantial degradation of performance.
  * <p>
  * Note that there is substantial duplication of code to minimize method
  * invocations.  However simple output methods were used where empirical
  * tests seemed to indicate that the simpler method did not cost any time.
  * It seems likely that most of these variations will be
  * washed out across different compilers and users who wish to tune
  * the method for their particular system may wish to compare the
  * the implementation of write(int[], int, int) with write(float[], int, int).
  * <p>
  * Testing and timing for this class is
  * peformed in the nom.tam.util.test.BufferedFileTester class.
  */

public class BufferedDataOutputStream
               extends BufferedOutputStream
               implements ArrayDataOutput {

/** Use the BufferedOutputStream constructor
  * @param o An open output stream.
  */
public BufferedDataOutputStream(OutputStream o) {
    super(o, 32768);
}
/** Use the BufferedOutputStream constructor
  * @param o           An open output stream.
  * @param bufLength   The buffer size.
  */
public BufferedDataOutputStream(OutputStream o, int bufLength) {
    super(o, bufLength);
}


/** Write a boolean value
  * @param b  The value to be written.  Externally true is represented as
  *           a byte of 1 and false as a byte value of 0.
  */
public void writeBoolean(boolean b) throws IOException {

    checkBuf(1);
    if (b) {
	buf[count++] = 1;
    } else {
        buf[count++] = 0;
    }
}

/** Write a byte value.
  */
public void writeByte(int b) throws IOException {
    checkBuf(1);
    buf[count++] = (byte) b;
}

/** Write an integer value.
  */
public void writeInt(int i) throws IOException {

    checkBuf(4);
    buf[count++] = (byte) (i >>> 24);
    buf[count++] = (byte) (i >>> 16);
    buf[count++] = (byte) (i >>>  8);
    buf[count++] = (byte)  i;
}

/** Write a short value.
  */
public void writeShort(int s) throws IOException {

    checkBuf(2);
    buf[count++] = (byte) (s >>> 8);
    buf[count++] = (byte)  s;
    
}

/** Write a char value.
  */
public void writeChar(int c) throws IOException {
    
    checkBuf(2);
    buf[count++] = (byte) (c >>> 8);
    buf[count++] = (byte)  c;
}

/** Write a long value.
  */
public void writeLong(long l) throws IOException {
    
    checkBuf(8);

    buf[count++] = (byte) (l >>> 56);
    buf[count++] = (byte) (l >>> 48);
    buf[count++] = (byte) (l >>> 40);
    buf[count++] = (byte) (l >>> 32);
    buf[count++] = (byte) (l >>> 24);
    buf[count++] = (byte) (l >>> 16);
    buf[count++] = (byte) (l >>>  8);
    buf[count++] = (byte)  l;
}

/** Write a float value.
  */
public void writeFloat(float f) throws IOException {

    checkBuf(4);
    
    int i = Float.floatToIntBits(f);

    buf[count++] = (byte)   (i >>> 24);
    buf[count++] = (byte) (i >>> 16);
    buf[count++] = (byte) (i >>>  8);
    buf[count++] = (byte)  i;
    
    
}

/** Write a double value.
  */
public void writeDouble(double d) throws IOException {

    checkBuf(8);
    long l = Double.doubleToLongBits(d);

    buf[count++] = (byte) (l >>> 56);
    buf[count++] = (byte) (l >>> 48);
    buf[count++] = (byte) (l >>> 40);
    buf[count++] = (byte) (l >>> 32);
    buf[count++] = (byte) (l >>> 24);
    buf[count++] = (byte) (l >>> 16);
    buf[count++] = (byte) (l >>>  8);
    buf[count++] = (byte)  l;
    
}

/** Write a string using the local protocol to convert char's to bytes.
  *
  * @param s   The string to be written.
  */
public void writeBytes(String s) throws IOException {
    
    write(s.getBytes(), 0, s.length());
}

/** Write a string as an array of chars.
  */
public void writeChars(String s) throws IOException {

    for (int i=0; i < s.length(); i += 1) {
        writeChar(s.charAt(i));
    }
}

/** Write a string as a UTF.  Note that this class does not
  * handle this situation efficiently since it creates
  * new DataOutputStream to handle each call.
  */
public void writeUTF(String s) throws IOException{

    // Punt on this one and use standard routines.
    DataOutputStream d = new DataOutputStream(this);
    d.writeUTF(s);
    d.flush();
    d.close();
}

/** This routine provides efficient writing of arrays of any primitive type.
  * The String class is also handled but it is an error to invoke this
  * method with an object that is not an array of these types.  If the
  * array is multidimensional, then it calls itself recursively to write
  * the entire array.  Strings are written using the standard
  * 1 byte format (i.e., as in writeBytes).
  *
  * If the array is an array of objects, then writePrimitiveArray will
  * be called for each element of the array.
  *
  * @param o  The object to be written.  It must be an array of a primitive
  *           type, Object, or String.
  */
public void writePrimitiveArray(Object o) throws IOException {
    writeArray(o);
}
		   
/** This routine provides efficient writing of arrays of any primitive type.
  * The String class is also handled but it is an error to invoke this
  * method with an object that is not an array of these types.  If the
  * array is multidimensional, then it calls itself recursively to write
  * the entire array.  Strings are written using the standard
  * 1 byte format (i.e., as in writeBytes).
  *
  * If the array is an array of objects, then writePrimitiveArray will
  * be called for each element of the array.
  *
  * @param o  The object to be written.  It must be an array of a primitive
  *           type, Object, or String.
  */
public void writeArray(Object o)  throws IOException {
    String className = o.getClass().getName();

    if (className.charAt(0) != '[') {
        throw new IOException("Invalid object passed to BufferedDataOutputStream.write"+className);
    }

    // Is this a multidimensional array?  If so process recursively.
    if (className.charAt(1) == '[') {
        for (int i=0; i < ((Object[])o).length; i += 1) {
            writeArray(((Object[])o)[i]);
        }
    } else {

        // This is a one-d array.  Process it using our special functions.
        switch (className.charAt(1)) {
        case 'Z': write((boolean[])o, 0, ((boolean[])o).length);
             break;
        case 'B': write((byte[])o, 0, ((byte[])o).length);
             break;
        case 'C': write((char[])o, 0, ((char[])o).length );
             break;
        case 'S': write((short[])o, 0, ((short[])o).length);
             break;
        case 'I': write((int[])o, 0, ((int[])o).length);
             break;
        case 'J': write((long[])o, 0, ((long[])o).length);
             break;
        case 'F': write((float[])o, 0, ((float[])o).length);
             break;
        case 'D': write((double[])o, 0, ((double[])o).length);
             break;
        case 'L':

             // Handle two exceptions: an array of strings, or an
             // array of objects. .
             if (className.equals("[Ljava.lang.String;") ) {
                 write((String[])o, 0, ((String[])o).length);
             } else if (className.equals("[Ljava.lang.Object;")) {
                 for (int i=0; i< ((Object[])o).length; i += 1) {
                     writeArray(((Object[])o)[i]);
                 }
             } else {
                 throw new IOException("Invalid object passed to BufferedDataOutputStream.writeArray: "+className);
             }
             break;
        default:
             throw new IOException("Invalid object passed to BufferedDataOutputStream.writeArray: "+className);
        }
    }

}

/** Write an array of booleans.
  */
public void write(boolean[] b) throws IOException {
    write(b, 0, b.length);
}
		   
/** Write a segment of an array of booleans.
  */
public void write(boolean[] b, int start, int len) throws IOException {
    
    for (int i=start; i<start+len; i += 1) {
	
	if (count+1 > buf.length) {
	    checkBuf(1);
	}
        if (b[i]) {
             buf[count++] = 1;
        } else {
             buf[count++] = 0;
        }
    }
}

/** Write an array of shorts.
  */
public void write(short[] s) throws IOException {
    write(s, 0, s.length);
}
		   
/** Write a segment of an array of shorts.
  */
public void write(short[] s, int start, int len) throws IOException {

    for(int i=start; i<start+len; i += 1) {
	if (count+2 > buf.length) {
	    checkBuf(2);
	}
	buf[count++] = (byte)(s[i]>>8);
	buf[count++] = (byte)(s[i]);
    }
}

/** Write an array of char's.
  */
public void write(char[] c) throws IOException {
    write(c, 0, c.length);
}
		   
/** Write a segment of an array of char's.
  */
public void write(char[] c, int start, int len) throws IOException {

    for(int i=start; i<start+len; i += 1) {
	if (count+2 > buf.length) {
	    checkBuf(2);
	}
	buf[count++] = (byte)(c[i]>>8);
	buf[count++] = (byte)(c[i]);
    }
}

/** Write an array of int's.
  */
public void write(int[] i) throws IOException {
    write(i, 0, i.length);
}
		   
/** Write a segment of an array of int's.
  */
public void write(int[] i, int start, int len) throws IOException {

    for (int ii=start; ii<start+len; ii += 1) {
	if (count + 4 > buf.length) {
	    checkBuf(4);
	}

	buf[count++] = (byte) (i[ii]>>>24);
	buf[count++] = (byte) (i[ii]>>>16);
	buf[count++] = (byte) (i[ii]>>> 8);
	buf[count++] = (byte) (i[ii]);
	
    }

}

/** Write an array of longs.
  */
public void write(long[] l) throws IOException {
    write(l, 0, l.length);
}

/** Write a segement of an array of longs.
  */
public void write(long[] l, int start, int len) throws IOException {

    for (int i=start; i<start+len; i += 1) {
	if (count + 8 > buf.length) {
	    checkBuf(8);
	}
	int t = (int)(l[i] >>> 32);
	
	buf[count++] = (byte)(t >>> 24);
	buf[count++] = (byte)(t >>> 16);
	buf[count++] = (byte)(t >>>  8);
	buf[count++] = (byte)(t);
	
	t = (int)(l[i]);
	
	buf[count++] = (byte)(t >>> 24);
	buf[count++] = (byte)(t >>> 16);
	buf[count++] = (byte)(t >>>  8);
	buf[count++] = (byte)(t);
    }
}

/** Write an array of floats.
  */
public void write(float[] f) throws IOException {
    write(f, 0, f.length);
}
public void write(float[] f, int start, int len) throws IOException {

    for (int i=start; i<start+len; i += 1) {
	
	if (count+4 > buf.length) {
	    checkBuf(4);
	}
        int t = Float.floatToIntBits(f[i]);
        buf[count++] = (byte)(t >>> 24);
        buf[count++] = (byte)(t >>> 16);
        buf[count++] = (byte)(t >>>  8);
        buf[count++] = (byte) t;
    }
}

/** Write an array of doubles.
  */
public void write(double[] d) throws IOException {
    write(d, 0, d.length);
}
public void write(double[] d, int start, int len) throws IOException {
    
    for (int i=start; i<start+len; i += 1) {
	if (count+8 > buf.length) {
	    checkBuf(8);
	}
        long t = Double.doubleToLongBits(d[i]);
	
	int ix = (int) (t >>> 32);
	
        buf[count++] = (byte)(ix >>> 24);
        buf[count++] = (byte)(ix >>> 16);
        buf[count++] = (byte)(ix >>> 8);
        buf[count++] = (byte)(ix);
	
	ix = (int) t;
	
        buf[count++] = (byte)(ix >>> 24);
        buf[count++] = (byte)(ix >>> 16);
        buf[count++] = (byte)(ix >>>  8);
        buf[count++] = (byte) ix;
    }

}

/** Write an array of Strings -- equivalent to calling writeBytes for each string.
  */
public void write(String[] s) throws IOException {
    write(s, 0, s.length);
}

/** Write a segment of an array of Strings. 
  * Equivalent to calling writeBytes for the selected elements.
  */
		   
public void write(String[] s, int start, int len) throws IOException {

    // Do not worry about buffering this specially since the
    // strings may be of differing lengths.

    for (int i=0; i<s.length; i += 1) {
        writeBytes(s[i]);
    }
}

/* See if there is enough space to add
 * something to the buffer.
 */
protected void checkBuf(int need) throws IOException {
    
    if (count+need > buf.length) {
	out.write(buf, 0, count);
	count = 0;
    }
}
		   
}
