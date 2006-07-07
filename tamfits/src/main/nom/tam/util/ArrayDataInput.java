package nom.tam.util;

import java.io.IOException;

public interface ArrayDataInput extends java.io.DataInput {
    
    /** Read a generic (possibly multidimenionsional) primitive array.
      * An  Object[] array is also a legal argument if each element
      * of the array is a legal.
      * <p>
      * The ArrayDataInput classes do not support String input since
      * it is unclear how one would read in an Array of strings.
      * @param o   A [multidimensional] primitive (or Object) array.
      */
    public int readArray(Object o) throws IOException;

    /* Read a complete primitive array */
    public int read(byte[]    buf) throws IOException;
    public int read(boolean[] buf) throws IOException;
    public int read(short[]   buf) throws IOException;
    public int read(char[]    buf) throws IOException;
    public int read(int[]     buf) throws IOException;
    public int read(long[]    buf) throws IOException;
    public int read(float[]   buf) throws IOException;
    public int read(double[]  buf) throws IOException;
    
    /* Read a segment of a primitive array. */
    public int read(byte[]    buf, int offset, int size) throws IOException;
    public int read(boolean[] buf, int offset, int size) throws IOException;
    public int read(char[]    buf, int offset, int size) throws IOException;
    public int read(short[]   buf, int offset, int size) throws IOException;
    public int read(int[]     buf, int offset, int size) throws IOException;
    public int read(long[]    buf, int offset, int size) throws IOException;
    public int read(float[]   buf, int offset, int size) throws IOException;
    public int read(double[]  buf, int offset, int size) throws IOException;
    
    /* Skip (forward) in a file */
    public long skip(long distance) throws IOException;
    
    /* Close the file. */
    public void close() throws IOException;
    
}
