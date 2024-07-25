package nom.tam.util;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */

/** This class is intended for high performance I/O in scientific applications.
  * It adds buffering to the RandomAccessFile and also
  * provides efficient handling of arrays.  Primitive arrays
  * may be written using a single method call.  Large buffers
  * are used to minimize synchronization overheads since methods
  * of this class are not synchronized.
  * <p>
  * Note that although this class fully supports the
  * contract of RandomAccessFile it does not (and can not)
  * extend that class since many of the methods of
  * RandomAccessFile are final.  In practice this
  * method works much like the StreamFilter classes.
  * All methods are implemented in this class but
  * some are simply delegated to an underlying RandomAccessFile member.
  * <p>
  * Testing and timing routines are available in
  * the nom.tam.util.test.BufferedFileTester class.
  *
  *  Version 1.1 October 12, 2000: Fixed handling of EOF in array reads
  *  so that a partial array will be returned when an EOF is detected.
  *  Excess bytes that cannot be used to construct array elements will
  *  be discarded (e.g., if there are 2 bytes left and the user is
  *  reading an int array).
  */

import java.io.*;

public class BufferedFile
  implements ArrayDataInput, ArrayDataOutput, RandomAccess {

    /** The current offset into the buffer */
    private int    bufferOffset;

    /** The number of valid characters in the buffer */
    private int    bufferLength;

    /** The declared length of the buffer array */
    private int	   bufferSize;

    /** Counter used in reading arrays */
    private int    primitiveArrayCount;

    /** The data buffer. */
    private byte[] buffer;

    /** The name of the original file */
    private String filename;

    /** The underlying access to the file system */
    private RandomAccessFile raf;

    /** The offset of the beginning of the current buffer */
    private long fileOffset;

    /** Is the buffer being used for input or output */
    private boolean doingInput;

    /** Create a read-only buffered file */
    public BufferedFile(String filename) throws IOException {
        this(filename, "r", 32768);
    }

    /** Create a buffered file with the given mode.
     *  @param filename The file to be accessed.
     *  @param mode     A string composed of "r" and "w" for
     *                  read and write access.
     */
    public BufferedFile(String filename, String mode) throws IOException {
	this(filename, mode, 32768);
    }

    /** Create a buffered file with the given mode and a specified
     *  buffer size.
     *  @param filename The file to be accessed.
     *  @param mode     A string composed of "r" and "w" indicating
     *                  read or write access.
     *  @param buffer   The buffer size to be used. This should be
     *                  substantially larger than 100 bytes and
     *                  defaults to 32768 bytes in the other
     *                  constructors.
     */
    public BufferedFile(String filename, String mode, int bufferSize) throws
      IOException {
	raf = new RandomAccessFile(filename, mode);
	buffer       = new byte[bufferSize];
	bufferOffset = 0;
	bufferLength = 0;
	fileOffset   = 0;
	this.bufferSize = bufferSize;

	this.filename = filename;
    }

    /** Create a buffered file using a mapped


    /** Read an entire byte array.
     *  Note BufferedFile will return a partially filled array
     *  only at an end-of-file.
     *  @param buf The array to be filled.
     */
    public int read(byte[] buf) throws IOException {
	return read(buf, 0, buf.length);
    }

    /** Read into a segment of a byte array.
     *  @param buf    The array to be filled.
     *  @param offset The starting location for input.
     *  @param length The number of bytes to be read.  Fewer bytes
     *                will be read if an EOF is reached.
     */
    public int read(byte[] buf, int offset, int len) throws IOException {

        checkBuffer(-1);
        int total = 0;

        // Ensure that the entire buffer is read.
        while (len > 0) {

	    if (bufferOffset < bufferLength) {

		int get = len;
		if (bufferOffset + get > bufferLength) {
		    get = bufferLength - bufferOffset;
		}
		System.arraycopy(buffer, bufferOffset, buf, offset, get);
		len -= get;
		bufferOffset += get;
		offset += get;
		total  += get;
		continue;

	    } else {

	        // This might be pretty long, but we know that the
		// old buffer is exhausted.
		try {
		    if (len > bufferSize) {
		        checkBuffer(bufferSize);
		    } else {
		        checkBuffer(len);
		    }
		} catch (EOFException e) {
		    if (bufferLength > 0) {
			System.arraycopy(buffer, 0, buf, offset, bufferLength);
			total += bufferLength;
			bufferLength = 0;
		    }
		    if (total == 0) {
			throw e;
		    } else {
			return total;
		    }
		}
	    }
        }

        return total;
    }

    /** This should only be used when a small number of
      * bytes is required (substantially smaller than
      * bufferSize.
      */
    private void checkBuffer(int needBytes) throws IOException {

        // Check if the buffer has some pending output.
        if (!doingInput && bufferOffset > 0) {
            flush();
        }
        doingInput = true;

        if (bufferOffset + needBytes < bufferLength) {
	    return;
        }
        /* Move the last few bytes to the beginning of the buffer
         * and read in enough data to fill the current demand.
         */
        int len = bufferLength-bufferOffset;

        /* Note that new location that the beginning of the buffer
         * corresponds to.
         */
        fileOffset += bufferOffset;
        if (len > 0) {
            System.arraycopy(buffer, bufferOffset, buffer, 0, len);
        }
        needBytes   -= len;
        bufferLength = len;
        bufferOffset = 0;

        while (needBytes > 0) {
	    len = raf.read(buffer, bufferLength, bufferSize-bufferLength);
	    if (len < 0) {
		throw new EOFException();
	    }
	    needBytes    -= len;
	    bufferLength += len;
        }
    }

    /** Read a byte */
    public int read() throws IOException {
        checkBuffer(1);
        bufferOffset += 1;
        return buffer[bufferOffset-1];
    }

    /** Skip from the current position.
     *  @param offset The number of bytes from the
     *                current position.  This may
     *                be negative.
     */
    public long skip(long offset) throws IOException {

	if (offset > 0 && fileOffset+bufferOffset+offset > raf.length()) {
	    offset = raf.length() - fileOffset - bufferOffset;
	    seek(raf.length());
	} else if (fileOffset+bufferOffset+offset < 0) {
	    offset = -(fileOffset+bufferOffset);
	    seek(0);
	} else {
            seek(fileOffset + bufferOffset + offset);
	}
        return offset;
    }

    /** Move to the current offset from the beginning of the file.
     *  A user may move past the end of file but this
     *  does not extend the file unless data is written there.
     */
    public void seek(long offsetFromStart) throws IOException {

        if (!doingInput) {
            // Have to flush before a seek...
	    flush();
        }

        // Are we within the current buffer?
        if (fileOffset <= offsetFromStart && offsetFromStart < fileOffset+bufferLength) {
	    bufferOffset = (int) (offsetFromStart - fileOffset);
        } else {

            // Seek to the desired location.
	    if (offsetFromStart < 0) {
	        offsetFromStart = 0;
	    }

	    fileOffset = offsetFromStart;
	    raf.seek(fileOffset);

            // Invalidate the current buffer.
            bufferLength = 0;
	    bufferOffset = 0;
        }
    }

    /** Read a boolean
     * @return a boolean generated from the next
     *                     byte in the input.
     */
    public boolean readBoolean() throws IOException {
        return convertToBoolean();
    }

    /** Get a boolean from the buffer */
    private boolean convertToBoolean() throws IOException {
        checkBuffer(1);
        bufferOffset += 1;
        return buffer[bufferOffset-1] == 1;
    }

    /** Read a byte
     *  @return the next byte in the input.
     */
    public byte readByte() throws IOException {
        checkBuffer(1);
        bufferOffset += 1;
        return buffer[bufferOffset-1];
    }

    /** Read an unsigned byte.
     *  @return the unsigned value of the next byte as
     *          an integer.
     */
    public int readUnsignedByte() throws IOException {
        checkBuffer(1);
        bufferOffset += 1;
        return buffer[bufferOffset-1] | 0x00ff;
    }

    /** Read an int
     *  @return an integer read from the input.
     */
    public int readInt() throws IOException {
        return convertToInt();
    }

    /** Get an integer value from the buffer */
    private int convertToInt() throws IOException {
        checkBuffer(4);
        int x = bufferOffset;
        int i = buffer[x] << 24  | (buffer[x+1]&0xFF) << 16 | (buffer[x+2]&0xFF) << 8 | (buffer[x+3]&0xFF);
        bufferOffset += 4;
        return i;
    }


    /** Read a short
     *  @return a short read from the input.
     */
    public short readShort() throws IOException {
        return convertToShort();
    }

    /** Get a short from the buffer */
    private short convertToShort() throws IOException {
        checkBuffer(2);
        short s = (short) (buffer[bufferOffset] << 8 | (buffer[bufferOffset+1]&0xFF));
        bufferOffset += 2;
        return s;
    }

    /** Read an unsigned short.
     *  @return an unsigned short value as an integer.
     */
    public int readUnsignedShort() throws IOException {
        return readShort()&0xFFFF;
    }

    /** Read a char
     *  @return a char read from the input.
     */
    public char readChar() throws IOException {
        return convertToChar();
    }

    /** Get a char from the buffer */
    private char convertToChar() throws IOException {
        checkBuffer(2);
        char c = (char) (buffer[bufferOffset] << 8 | (buffer[bufferOffset+1]&0xFF));
        bufferOffset += 2;
        return c;
    }

    /** Read a long.
     *  @return a long value read from the input.
     */
    public long readLong() throws IOException {
        return convertToLong();
    }

    /** Get a long value from the buffer */
    private long convertToLong() throws IOException {
        checkBuffer(8);
        int x = bufferOffset;

        int i1 =  buffer[x] << 24 | (buffer[x+1]&0xFF) << 16 | (buffer[x+2]&0xFF) << 8 | (buffer[x+3]&0xFF);
        int i2 =  buffer[x+4] << 24 | (buffer[x+5]&0xFF) << 16 | (buffer[x+6]&0xFF) << 8 | (buffer[x+7]&0xFF);
        bufferOffset += 8;

        return  (((long) i1) << 32) | (((long)i2)&0x00000000ffffffffL);
    }

    /** Read a float.
     *  @return a float value read from the input.
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(convertToInt());
    }

    /** Read a double.
     *  @return a double value read from the input.
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble( convertToLong());
    }

    /** Read a byte array fully.
     *  Since the read method of this class reads an entire
     *  buffer, the only difference with readFully is that
     *  readFully will signal an EOF if the buffer cannot be filled.
     */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }


    /** Read a byte array fully.
     *  Since the read method of this class reads an entire
     *  buffer, the only difference with readFully is that
     *  readFully will signal an EOF if the buffer cannot be filled.
     */
    public void readFully(byte[] b, int off, int len) throws IOException {

        if (off < 0 || len < 0 || off+len > b.length) {
            throw new IOException("Attempt to read outside byte array");
       }

        if (read(b, off, len) < len) {
            throw new EOFException();
        }
    }

    /** Skip the number of bytes.
     *  This differs from the skip method in that
     *  it will throw an EOF if a forward skip cannot
     *  be fully accomplished...  (However that isn't
     *  supposed to happen with a random access file,
     *  so there is probably no operational difference).
     */
    public int skipBytes(int toSkip) throws IOException {

        // Note that we allow negative skips...
        if (skip(toSkip) < toSkip) {
            throw new EOFException();
        } else {
            return toSkip;
        }
    }


    /** Read a string encoded as a UTF.
     *  @return the string.
     */
    public String readUTF() throws IOException{
        checkBuffer(-1);
        raf.seek(fileOffset + bufferOffset);
        String utf = raf.readUTF();
        fileOffset = raf.getFilePointer();

        // Invalidate the buffer.
        bufferLength = 0;
        bufferOffset = 0;

        return utf;
    }

    /** Read a line of input.
     *  @return the next line.
     */
    public String readLine() throws IOException {

        checkBuffer(-1);
        raf.seek(fileOffset + bufferOffset);
        String line = raf.readLine();
        fileOffset = raf.getFilePointer();

        // Invalidate the buffer.
        bufferLength = 0;
        bufferOffset = 0;

        return line;
    }

    /** This routine provides efficient reading of arrays of any primitive type.
      *
      *  @param o  The object to be read.  It must be an array of a primitive type,
      *           or an array of Object's.
      */

    public int readArray(Object o) throws IOException {

        // Note that we assume that only a single thread is
        // doing a primitive Array read at any given time.  Otherwise
        // primitiveArrayCount can be wrong and also the
        // input data can be mixed up.  If this assumption is not
        // true we need to synchronize this call.

        primitiveArrayCount = 0;
        return primitiveArrayRecurse(o);
    }

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
                 int len=read((byte[])o, 0, ((byte[])o).length);
                 break;
            case 'C':
                 primitiveArrayCount += read((char[])o, 0, ((char[])o).length);
                 break;
            case 'S':
                 primitiveArrayCount += read((short[])o, 0, ((short[])o).length);
                 break;
            case 'I':
                 primitiveArrayCount += read((int[])o, 0, ((int[])o).length);
                 break;
            case 'J':
                 primitiveArrayCount += read((long[])o, 0, ((long[])o).length);
                 break;
            case 'F':
                 primitiveArrayCount += read((float[])o, 0, ((float[])o).length);
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
                     throw new IOException("Invalid object passed to BufferedFile.readPrimitiveArray: "+className);
                 }
                 break;
            default:
                 throw new IOException("Invalid object passed to BufferedDataInputStream.readArray: "+className);
            }
        }
        return primitiveArrayCount;
    }


    public int read(boolean[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public int read(boolean[] b, int start, int length) throws IOException {

	int i = start;
	try {
            for (; i < start+length; i += 1) {
                b[i] = convertToBoolean();
            }
            return length;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,1);
	}
    }

    public int read(short[] s) throws IOException {
	return read(s, 0, s.length);
    }
    public int read(short[] s, int start, int length) throws IOException {

	int i = start;
	try {
            for(; i<start+length; i += 1) {
                s[i] = convertToShort();
            }
            return length*2;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,2);
	}
    }

    public int read(char[] c) throws IOException {
	return read(c, 0, c.length);
    }
    public int read(char[] c, int start, int length) throws IOException {

	int i=start;
	try {
            for(; i < start+length; i += 1) {
                c[i] = convertToChar();
            }
            return length*2;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,2);
	}
    }

    public int read(int[] i) throws IOException {
	return read(i, 0, i.length);
    }

    public int read(int[] i, int start, int length) throws IOException {

	int ii=start;
        try {
	    for (; ii<start+length; ii += 1) {
                i[ii] = convertToInt();
            }
            return length*4;
	} catch (EOFException e) {
	    return eofCheck(e,start,ii,4);
	}
    }

    public int read(long[] l) throws IOException {
	return read(l, 0, l.length);
    }

    public int read(long[] l, int start, int length) throws IOException {

	int i=start;
        try {
	    for (; i<start+length; i += 1) {
                l[i] = convertToLong();
            }
            return length*8;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,8);
	}

    }

    public int read(float[] f) throws IOException {
	return read(f, 0, f.length);
    }

    public int read(float[] f, int start, int length) throws IOException {

	int i=start;
	try {
            for (; i<start+length; i += 1) {
                f[i] = Float.intBitsToFloat(convertToInt());
            }
            return length*4;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,4);
	}
    }

    public int read(double[] d) throws IOException {
	return read(d, 0, d.length);
    }

    public int read(double[] d, int start, int length) throws IOException {

	int i=start;
	try {
            for (; i<start+length; i += 1) {
                d[i] = Double.longBitsToDouble(convertToLong());
            }
            return length*8;
	} catch (EOFException e) {
	    return eofCheck(e,start,i,8);
	}
    }

    /** See if an exception should be thrown during an array read. */
    private int eofCheck(EOFException e, int start, int index, int length)
	throws EOFException {
	if (start == index) {
	    throw e;
	} else {
	    return (index-start)*length;
	}
    }

    /**** Output Routines ****/

    private void needBuffer(int need) throws IOException  {

        if (doingInput) {

            fileOffset += bufferOffset;
	    raf.seek(fileOffset);

	    doingInput = false;

	    bufferOffset = 0;
	    bufferLength = 0;
        }

        if (bufferOffset + need >= bufferSize) {
            raf.write(buffer, 0, bufferOffset);
	    fileOffset  += bufferOffset;
	    bufferOffset = 0;
        }
    }


    public void write(int buf) throws IOException {
        convertFromByte(buf);
    }

    public void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    public void write(byte[] buf, int offset, int length) throws IOException {

        if (length < bufferSize) {
	    /* If we can use the buffer do so... */
	    needBuffer(length);
            System.arraycopy(buf, offset, buffer, bufferOffset, length);
	    bufferOffset += length;
        } else {
	    /* Otherwise flush the buffer and write the data directly.
	     * Make sure that we indicate that the buffer is clean when
	     * we're done.
	     */
            flush();

	    raf.write(buf, offset, length);

	    fileOffset += length;

	    doingInput = false;
	    bufferOffset = 0;
	    bufferLength = 0;
        }
    }

    /** Flush output buffer if necessary.
     *  This method is not present in RandomAccessFile
     *  but users may need to call flush to ensure
     *  that data has been written.
     */
    public void flush() throws IOException {

        if (!doingInput && bufferOffset > 0) {
            raf.write(buffer, 0, bufferOffset);
	    fileOffset += bufferOffset;
	    bufferOffset = 0;
	    bufferLength = 0;
        }
    }

    /** Write a boolean value
      * @param b  The value to be written.  Externally true is represented as
      *           a byte of 1 and false as a byte value of 0.
      */
    public void writeBoolean(boolean b) throws IOException {
        convertFromBoolean(b);
    }

    private void convertFromBoolean(boolean b) throws IOException {
        needBuffer(1);
        if (b) {
            buffer[bufferOffset] = (byte) 1;
        } else {
	    buffer[bufferOffset] = (byte) 0;
        }
        bufferOffset += 1;
    }

    /** Write a byte value.
      */
    public void writeByte(int b) throws IOException {
        convertFromByte(b);
    }

    private void convertFromByte(int b) throws IOException  {
        needBuffer(1);
        buffer[bufferOffset++] = (byte) b;
    }

    /** Write an integer value.
      */
    public void writeInt(int i) throws IOException {
        convertFromInt(i);
    }

    private void convertFromInt(int i) throws IOException {

        needBuffer(4);
        buffer[bufferOffset++] = (byte) (i >>> 24);
        buffer[bufferOffset++] = (byte) (i >>> 16);
        buffer[bufferOffset++] = (byte) (i >>>  8);
        buffer[bufferOffset++] = (byte)  i;
    }

    /** Write a short value.
     */
    public void writeShort(int s) throws IOException {

        convertFromShort(s);
    }

    private void convertFromShort(int s) throws IOException {
        needBuffer(2);

        buffer[bufferOffset++] = (byte) (s >>> 8);
        buffer[bufferOffset++] = (byte)  s;
    }

    /** Write a char value.
      */
    public void writeChar(int c) throws IOException {
        convertFromChar(c);
    }

    private void convertFromChar(int c) throws IOException {
        needBuffer(2);
        buffer[bufferOffset++] = (byte) (c >>> 8);
        buffer[bufferOffset++] = (byte)  c;
    }

    /** Write a long value.
      */
    public void writeLong(long l) throws IOException {
        convertFromLong(l);
    }

    private void convertFromLong(long l) throws IOException {
        needBuffer(8);

        buffer[bufferOffset++] = (byte) (l >>> 56);
        buffer[bufferOffset++] = (byte) (l >>> 48);
        buffer[bufferOffset++] = (byte) (l >>> 40);
        buffer[bufferOffset++] = (byte) (l >>> 32);
        buffer[bufferOffset++] = (byte) (l >>> 24);
        buffer[bufferOffset++] = (byte) (l >>> 16);
        buffer[bufferOffset++] = (byte) (l >>>  8);
        buffer[bufferOffset++] = (byte)  l;
    }

    /** Write a float value.
      */
    public void writeFloat(float f) throws IOException {
        convertFromInt(Float.floatToIntBits(f));
    }

    /** Write a double value.
      */
    public void writeDouble(double d) throws IOException {
        convertFromLong(Double.doubleToLongBits(d));
    }

    /** Write a string using the local protocol to convert char's to bytes.
      *
      * @param s   The string to be written.
      */
    public void writeBytes(String s) throws IOException {
        write(s.getBytes(),0,s.length());
    }

    /** Write a string as an array of chars.
      */
    public void writeChars(String s) throws IOException {

        int len = s.length();
        for (int i=0; i<len; i += 1) {
            convertFromChar(s.charAt(i));
        }
    }

    /** Write a string as a UTF.
      */
    public void writeUTF(String s) throws IOException{
        flush();
        raf.writeUTF(s);
        fileOffset = raf.getFilePointer();
    }

    /** This routine provides efficient writing of arrays of any primitive type.
      * The String class is also handled but it is an error to invoke this
      * method with an object that is not an array of these types.  If the
      * array is multidimensional, then it calls itself recursively to write
      * the entire array.  Strings are written using the standard
      * 1 byte format (i.e., as in writeBytes).
      *
      * If the array is an array of objects, then write will
      * be called for each element of the array.
      *
      * @param o  The object to be written.  It must be an array of a primitive
      *           type, Object, or String.
      */
    public void writeArray(Object o) throws IOException {
        String className = o.getClass().getName();

        if (className.charAt(0) != '[') {
            throw new IOException("Invalid object passed to BufferedFile.writeArray:"+className);
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
            case 'C': write((char[])o, 0, ((char[])o).length);
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
                     throw new IOException("Invalid object passed to BufferedFile.write: "+className);
                 }
                 break;
            default:
                 throw new IOException("Invalid object passed to BufferedFile.write: "+className);
            }
        }

    }

    /** Write an array of booleans.
      */
    public void write(boolean[] b) throws IOException {
	write(b, 0, b.length);
    }
    public void write(boolean[] b, int start, int length) throws IOException {
        for (int i=start; i<start+length; i += 1) {
            convertFromBoolean(b[i]);
        }
    }

    /** Write an array of shorts.
      */
    public void write(short[] s) throws IOException {
	write(s, 0, s.length);
    }
    public void write(short[] s, int start, int length) throws IOException {

        for(int i=start; i<start+length; i += 1) {
            convertFromShort(s[i]);
        }
    }

    /** Write an array of char's.
      */
    public void write(char[] c) throws IOException {
	write(c,0,c.length);
    }
    public void write(char[] c, int start, int length) throws IOException {

        for(int i=start; i<start+length; i += 1) {
            convertFromChar(c[i]);
        }
    }

    /** Write an array of int's.
      */
    public void write(int[] i) throws IOException {
	write(i, 0, i.length);
    }
    public void write(int[] i, int start, int length) throws IOException {
        for (int ii=start; ii<start+length; ii += 1) {
            convertFromInt(i[ii]);
        }
    }

    /** Write an array of longs.
      */
    public void write(long[] l) throws IOException {
	write(l, 0, l.length);
    }

    public void write(long[] l, int start, int length) throws IOException {

        for (int i=start; i<start+length; i += 1) {
            convertFromLong(l[i]);
        }
    }

    /** Write an array of floats.
      */
    public void write(float[] f) throws IOException {
	write(f, 0, f.length);
    }

    public void write(float[] f, int start, int length) throws IOException {
        for (int i=start; i<start+length; i += 1) {
            convertFromInt(Float.floatToIntBits(f[i]));
        }
    }

    /** Write an array of doubles.
      */
    public void write(double[] d) throws IOException {
	write(d, 0, d.length);
    }

    public void write(double[] d, int start, int length) throws IOException {

        for (int i=start; i<start+length; i += 1) {
            convertFromLong(Double.doubleToLongBits(d[i]));
        }
    }

    /** Write an array of Strings -- equivalent to calling writeBytes for each string.
      */
    public void write(String[] s) throws IOException {
	write(s, 0, s.length);
    }

    public void write(String[] s, int start, int length) throws IOException {
        for (int i=start; i<start+length; i += 1) {
            writeBytes(s[i]);
        }
    }


    /** Close the file */
    public void close() throws IOException {
        flush();
        raf.close();
    }

    /** Get the file descriptor associated with
     *  this stream.  Note that this returns the file
     *  descriptor of the associated RandomAccessFile.
     */
    public FileDescriptor getFD() throws IOException {
        return raf.getFD();
    }

    /**
     * Return the FileChannel associated with the file. Used for
     * efficient mapped access in JSky.
     */
    public java.nio.channels.FileChannel getChannel() {
        // PWD: re-engineered this from JSky, hope it's all that's
        // needed!
	return raf.getChannel();
    }

    /** Get the current length of the file.
     */
    public long length() throws IOException {
        flush();
        return raf.length();
    }

    /** Get the current offset into the file.
     */
    public long getFilePointer() {
        return fileOffset + bufferOffset;
    }

    /** Set the length of the file.  This method calls
     *  the method of the same name in RandomAccessFile which
     *  is only available in JDK1.2 and greater.  This method
     *  may be deleted for compilation with earlier versions.
     *
     * @param newLength The number of bytes at which the file
     *                  is set.
     *
     */
    public void setLength(long newLength) throws IOException {

        flush();
        raf.setLength(newLength);
	if (newLength < fileOffset) {
	    fileOffset = newLength;
	}

    }
}
