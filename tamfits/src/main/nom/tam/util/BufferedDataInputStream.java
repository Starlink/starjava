package nom.tam.util;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */

// What do we use in here?

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;

/** This class is intended for high performance I/O in scientific applications.
  * It combines the functionality of the BufferedInputStream and the
  * DataInputStream as well as more efficient handling of arrays.
  * This minimizes the number of method calls that are required to
  * read data.  Informal tests of this method show that it can
  * be as much as 10 times faster than using a DataInputStream layered
  * on a BufferedInputStream for writing large arrays.  The performance
  * gain on scalars or small arrays will be less but there should probably
  * never be substantial degradation of performance.
  * <p>
  * Many new read calls are added to allow efficient reading
  * off array data.  The read(Object o) call provides
  * for reading a primitive array of arbitrary type or
  * dimensionality.  There are also reads for each type
  * of one dimensional array.
  * <p>
  * Note that there is substantial duplication of code to minimize method
  * invocations.  E.g., the floating point read routines read the data
  * as integer values and then convert to float.  However the integer
  * code is duplicated rather than invoked.  There has been
  * considerable effort expended to ensure that these routines are
  * efficient, but they could easily be superceded if
  * an efficient underlying I/O package were ever delivered
  * as part of the basic Java libraries.
  * <p>
  * Testing and timing routines are provided in the
  * nom.tam.util.test.BufferedFileTester class.
  * 
  * Version 1.1: October 12, 2000: Fixed handling of EOF to return
  * partially read arrays when EOF is detected.
  */
public class BufferedDataInputStream
               extends BufferedInputStream
               implements ArrayDataInput {


private int primitiveArrayCount;
private byte[] bb = new byte[8];
		   
/** Use the BufferedInputStream constructor
  */
public BufferedDataInputStream(InputStream o) {
    super(o, 32768);
}
		   
/** Use the BufferedInputStream constructor
  */
public BufferedDataInputStream(InputStream o, int bufLength) {
    super(o, bufLength);
}


/** Read a byte array.  This is the only method
 *  for reading arrays in the fundamental I/O classes.
 *  @param obuf    The byte array.
 *  @param offset  The starting offset into the array.
 *  @param len     The number of bytes to read.
 *  @return The actual number of bytes read.
 */
public int read(byte[] obuf, int offset, int len) throws IOException {
    
    int total = 0;
    
    while (len > 0) {
	
	// Use just the buffered I/O to get needed info.
	
        int xlen= super.read(obuf, offset, len);
        if (xlen <= 0) {
            if (total == 0) {
                throw new EOFException();
            } else {
                return total;
            }
        } else {
            len   -= xlen;
            total += xlen;
	    offset+= xlen;
        }
    }
    return total;

}

/** Read a boolean value.
  * @return b  The value read. 
  */
public boolean readBoolean() throws IOException {

    int b = read();
    if (b == 1) {
        return true;
    } else {
        return false;
    }
}
/** Read a byte value in the range -128 to 127.
  * @return The byte value as a byte (see read() to return the value
  * as an integer.
  */
public byte readByte() throws IOException {
    return (byte) read();
}

/** Read a byte value in the range 0-255.
 *  @return The byte value as an integer.
 */
public int readUnsignedByte() throws IOException {
    return read() | 0x00ff;
}

/** Read an integer.
 *  @return The integer value.
 */
public int readInt() throws IOException {

    if (read(bb, 0, 4) < 4 ) {
        throw new EOFException();
    }
    int i = bb[0] << 24  | (bb[1]&0xFF) << 16 | (bb[2]&0xFF) << 8 | (bb[3]&0xFF);
    return i;
}

/** Read a 2-byte value as a short (-32788 to 32767)
 *  @return The short value.
 */
public short readShort() throws IOException {

    if (read(bb, 0, 2) < 2) {
        throw new EOFException();
    }

    short s = (short) (bb[0] << 8 | (bb[1]&0xFF));
    return s;
}

/** Read a 2-byte value in the range 0-65536.
 * @return the value as an integer.
 */
public int readUnsignedShort() throws IOException {

    if (read(bb,0,2) < 2) {
        throw new EOFException();
    }

    return (bb[0]&0xFF) << 8  |  (bb[1]&0xFF);
}


/** Read a 2-byte value as a character.
 * @return The character read.
 */
public char readChar() throws IOException {
    byte[] b = new byte[2];

    if (read(b, 0, 2)  <  2) {
        throw new EOFException();
    }

    char c = (char) (b[0] << 8 | (b[1]&0xFF));
    return c;
}

/** Read a long.
 *  @return The value read.
 */
public long readLong() throws IOException {

    // use two ints as intermediarys to
    // avoid casts of bytes to longs...
    if (read(bb, 0, 8) < 8) {
        throw new EOFException();
    }
    int i1 =  bb[0] << 24 | (bb[1]&0xFF) << 16 | (bb[2]&0xFF) << 8 | (bb[3]&0xFF);
    int i2 =  bb[4] << 24 | (bb[5]&0xFF) << 16 | (bb[6]&0xFF) << 8 | (bb[7]&0xFF);
    return  (((long) i1) << 32) | (((long)i2)&0x00000000ffffffffL);
}

/** Read a 4 byte real number.
 *  @return The value as a float.
 */
public float readFloat() throws IOException {

    if (read(bb, 0, 4) < 4) {
        throw new EOFException();
    }

    int i = bb[0] << 24  | (bb[1]&0xFF) << 16 | (bb[2]&0xFF) << 8 | (bb[3]&0xFF);
    return Float.intBitsToFloat(i);

}

/** Read an 8 byte real number.
 *  @return The value as a double.
 */
public double readDouble() throws IOException {

    if (read(bb, 0, 8) < 8) {
        throw new EOFException();
    }

    int i1 =  bb[0] << 24 | (bb[1]&0xFF) << 16 | (bb[2]&0xFF) << 8 | (bb[3]&0xFF);
    int i2 =  bb[4] << 24 | (bb[5]&0xFF) << 16 | (bb[6]&0xFF) << 8 | (bb[7]&0xFF);

    return Double.longBitsToDouble( ((long) i1) << 32 | ((long)i2&0x00000000ffffffffL) );
}

/** Read a buffer and signal an EOF if the buffer
 *  cannot be fully read.
 *  @param b  The buffer to be read.
 */
public void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
}

/** Read a buffer and signal an EOF if the requested elements
 *  cannot be read.
 * 
 *  This differs from read(b,off,len) since that call
 *  will not signal and end of file unless no bytes can
 *  be read.  However both of these routines will attempt
 *  to fill their buffers completely.
 *  @param b   The input buffer.
 *  @param off The requested offset into the buffer.
 *  @param len The number of bytes requested.
 */
public void readFully(byte[] b, int off, int len) throws IOException {

    if (off < 0 || len < 0 || off+len > b.length) {
        throw new IOException("Attempt to read outside byte array");
    }

    if (read(b, off, len) < len) {
        throw new EOFException();
    }
}

/** Skip the requested number of bytes.
 *  This differs from the skip call in that
 *  it takes an integer argument and will throw
 *  an end of file if the full number of bytes cannot be skipped.
 *  @param toSkip The number of bytes to skip.
 */
public int skipBytes(int toSkip) throws IOException {

    int need = toSkip;
    
    while (need > 0) {
	
	int got = (int) skip(need);
	
	if (got > 0) {
	    need -= got;
	} else {
	    break;
	}
    }

    if (need > 0) {
        throw new EOFException();
    } else {
        return toSkip;
    }
}


/** Read a String in the UTF format.
 *  The implementation of this is very inefficient and
 *  use of this class is not recommended for applications
 *  which will use this routine heavily.
 *  @return The String that was read.
 */
public String readUTF() throws IOException{

    // Punt on this one and use DataInputStream routines.
    DataInputStream d = new DataInputStream(this);
    return d.readUTF();

}

/**  
  * Emulate the deprecated DataInputStream.readLine() method.
  * Originally we used the method itself, but Alan Brighton
  * suggested using a BufferedReader to eliminate
  * the deprecation warning...
  * Note that the implementation
  * of this routine is very slow and this class should probably
  * not be used if this method is called heavily.
  * 
  * @return The String read.
  * @deprecated Use BufferedReader methods.
  */
public String readLine() throws IOException {
    // Punt on this and use BufferedReader routines.
    BufferedReader d = new BufferedReader(new InputStreamReader(this));
    return d.readLine();
}

		   
/** This routine provides efficient reading of arrays of any primitive type.
  * It is an error to invoke this method with an object that is not an array
  * of some primitive type.  Note that there is no corresponding capability
  * to writePrimitiveArray in BufferedDataOutputStream to read in an
  * array of Strings.
  *
  * @param o  The object to be read.  It must be an array of a primitive type,
  *           or an array of Object's.
  * @deprecated  See readArray(Object o).
  */

public int readPrimitiveArray(Object o) throws IOException {

    // Note that we assume that only a single thread is
    // doing a primitive Array read at any given time.  Otherwise
    // primitiveArrayCount can be wrong and also the
    // input data can be mixed up.

    primitiveArrayCount = 0;
    return primitiveArrayRecurse(o);
}

/** Read an object.  An EOF will be signaled if the
 *  object cannot be fully read.  The getPrimitiveArrayCount()
 *  method may then be used to get a minimum number of bytes read.
 *  @param o  The object to be read.  This object should
 *            be a primitive (possibly multi-dimensional) array.
 * 
 *  @returns  The number of bytes read.
 */
public int readArray(Object o) throws IOException {
    primitiveArrayCount = 0;
    return primitiveArrayRecurse(o);
}

/** Read recursively over a multi-dimensional array.
 *  @return The number of bytes read.
 */
protected int primitiveArrayRecurse(Object o) throws IOException {

    if (o == null) {
	return primitiveArrayCount;
    }

    String className = o.getClass().getName();

    if (className.charAt(0) != '[') {
        throw new IOException("Invalid object passed to BufferedDataInputStream.readArray:"+className);
    }

    // Is this a multidimensional array?  If so process recursively.
    if (className.charAt(1) == '[') {
        for (int i=0; i < ((Object[])o).length; i += 1) {
            primitiveArrayRecurse(((Object[])o)[i]);
        }
    } else {

        // This is a one-d array.  Process it using our special functions.
        switch (className.charAt(1)) {
        case 'Z':
             primitiveArrayCount += read((boolean[])o, 0, ((boolean[])o).length);
             break;
        case 'B':
             int len = read((byte[])o, 0, ((byte[])o).length);
	     primitiveArrayCount += len;
	    
             if (len < ((byte[])o).length){
		 throw new EOFException();
             }
             break;
        case 'C':
             primitiveArrayCount += read((char[])o, 0, ((char[])o).length);
             break;
        case 'S':
             primitiveArrayCount += read((short[])o, 0, ((short[])o).length);
             break;
        case 'I':
             primitiveArrayCount += read((int[])o,0,((int[])o).length);
             break;
        case 'J':
             primitiveArrayCount += read((long[])o, 0,((long[])o).length);
             break;
        case 'F':
             primitiveArrayCount += read((float[])o,0,((float[])o).length);
             break;
        case 'D':
             primitiveArrayCount += read((double[])o, 0, ((double[])o).length);
             break;
        case 'L':

             // Handle an array of Objects by recursion.  Anything
             // else is an error.
             if (className.equals("[Ljava.lang.Object;") ) {
                 for (int i=0; i < ((Object[])o).length; i += 1) {
                      primitiveArrayRecurse( ((Object[]) o)[i] );
                 }
             } else {
                 throw new IOException("Invalid object passed to BufferedDataInputStream.readArray: "+className);
             }
             break;
        default:
             throw new IOException("Invalid object passed to BufferedDataInputStream.readArray: "+className);
        }
    }
    return primitiveArrayCount;
}

/** Ensure that the requested number of bytes
 *  are available in the buffer or throw an EOF
 *  if they cannot be obtained.  Note that this
 *  routine will try to fill the buffer completely.
 * 
 *  @param The required number of bytes. 
 */
private void fillBuf(int need) throws IOException {
    
    if (count > pos) {
	System.arraycopy(buf, pos, buf, 0, count-pos);
	count -= pos;
	need  -= count;
	pos    = 0;
    } else {
	count = 0;
	pos   = 0;
    }
    
    while (need > 0) {
	

	int len = in.read(buf, count, buf.length-count);
	if (len <= 0) {
	    throw new EOFException();
	}
	count += len;
	need  -= len;
    }
    
}

/** Read a boolean array */
public int read(boolean[] b) throws IOException {
    return read(b, 0, b.length);
}
		   
/** Read a boolean array.
 */
public int read(boolean[] b, int start, int len) throws IOException {
    
    int i=start;
    try {
        for (; i < start+len; i += 1) {
        	
        	if (pos >= count) {
        	    fillBuf(1);
        	}
        	
            if (buf[pos] == 1) {
                 b[i] = true;
            } else {
                 b[i] = false;
            }
            pos += 1;
        }
    } catch (EOFException e) {
	return eofCheck(e, i, start, 1);
    }
    return len;
}

/** Read a short array */
public int read(short[] s) throws IOException {
    return read(s, 0, s.length);
}

/** Read a short array */
public int read(short[] s, int start, int len) throws IOException {

    int i=start;
    try {
        for(; i<start+len; i += 1) {
	    if (count - pos < 2) {
	        fillBuf(2);
	    }
            s[i] = (short) (buf[pos] << 8 | (buf[pos+1]&0xFF));
	    pos += 2;
        }
    } catch (EOFException e) {
	return eofCheck(e, i, start, 2);
    }
    return 2*len;
}

/** Read a character array */
public int read(char[] c) throws IOException {
    return read(c, 0, c.length);
}
    
/** Read a character array */
public int read(char[] c, int start, int len) throws IOException {
    
    int i=start;
    try {
	for(; i<start+len; i += 1) {
	    if (count - pos < 2) {
	       fillBuf(2);
	    }
            c[i] = (char) (buf[pos] << 8 | (buf[pos+1]&0xFF));
	    pos += 2;
        }
    } catch (EOFException e) {
	return eofCheck(e, i, start, 2);
    }
    return 2*len;
}

/** Read an integer array */
public int read(int[] i) throws IOException {
    return read(i, 0, i.length);
}
		   
/** Read an integer array */
public int read(int[] i, int start, int len) throws IOException {
    
    int ii=start;
    try {
        for (; ii<start+len; ii += 1) {
	  
	    if (count-pos < 4) {
	        fillBuf(4);
	    }
	
            i[ii] = buf[pos] << 24         | 
	           (buf[pos+1]&0xFF) << 16 | 
	           (buf[pos+2]&0xFF) << 8  | 
	           (buf[pos+3]&0xFF);
	    pos += 4;
        }
    } catch (EOFException e) {
	return eofCheck(e, ii, start, 4);
    }
    return i.length*4;
}

/** Read a long array */
public int read(long[] l) throws IOException {
    return read(l, 0, l.length);
}
    
/** Read a long array */
public int read(long[] l, int start, int len) throws IOException {

    int i = start;
    try {
        for (; i<start+len; i += 1) {
	    if (count - pos < 8) {
	        fillBuf(8);
	    }
            int i1  = buf[pos]   << 24 | (buf[pos+1]&0xFF) << 16 | (buf[pos+2]&0xFF) << 8 | (buf[pos+3]&0xFF);
            int i2  = buf[pos+4] << 24 | (buf[pos+5]&0xFF) << 16 | (buf[pos+6]&0xFF) << 8 | (buf[pos+7]&0xFF);
            l[i] = ( (long) i1) << 32 | ((long)i2&0x00000000FFFFFFFFL);
	    pos += 8;
        }
	
    } catch (EOFException e) {
	return eofCheck(e, i, start, 8);
    }
    return 8*len;
}

/** Read a float array */
public int read(float[] f) throws IOException {
    return read(f, 0, f.length);
}
    
/** Read a float array */
public int read(float[] f, int start, int len) throws IOException {
    
    int i=start;
    try {
	for (; i<start+len; i += 1) {
	    if (count - pos < 4) {
	        fillBuf(4);
	    }
            int t = buf[pos] << 24 |
                   (buf[pos+1]&0xFF) << 16 |
                   (buf[pos+2]&0xFF) <<  8 |
                   (buf[pos+3]&0xFF);
            f[i] = Float.intBitsToFloat(t);
	    pos += 4;
	}
    } catch (EOFException e) {
	return eofCheck(e, i, start, 4);
    }
    return 4*len;
}

/** Read a double array */
public int read(double[] d) throws IOException {
    return read(d, 0, d.length);
}
    
/** Read a double array */
public int read(double[] d, int start, int len) throws IOException {

    int i=start;
    try {
        for (; i < start+len; i += 1) {
	
	    if (count - pos < 8) {
	        fillBuf(8);
	    }
            int i1  = buf[pos]   << 24 | (buf[pos+1]&0xFF) << 16 | (buf[pos+2]&0xFF) << 8 | (buf[pos+3]&0xFF);
            int i2  = buf[pos+4] << 24 | (buf[pos+5]&0xFF) << 16 | (buf[pos+6]&0xFF) << 8 | (buf[pos+7]&0xFF);
            d[i] = Double.longBitsToDouble(
                    ((long) i1) << 32 | ((long)i2&0x00000000FFFFFFFFL));
	    pos += 8;
        }
    } catch (EOFException e) {
	return eofCheck(e, i, start, 8);
    }
    return 8*len;
}

/** For array reads return an EOF if unable to
 *  read any data.
 */
private int eofCheck(EOFException e, int i, int start, int length) 
		     throws EOFException {
    
    if (i == start) {
	throw e;
    } else {
	return (i-start)*length;
    }
}
		   
/** Represent the stream as a string */
public String toString() {
    return super.toString()+"[count="+count+",pos="+pos+"]";
}

}
