package nom.tam.util;

/** These packages define the methods which indicate that
 *  an i/o stream may be accessed in arbitrary order.
 *  The method signatures are taken from RandomAccessFile
 *  though that class does not implement this interface.
 */
public interface RandomAccess extends ArrayDataInput {
    
    /** Move to a specified location in the stream. */
    public void seek(long offsetFromStart) throws java.io.IOException;
    
    /** Get the current position in the stream */
    public long getFilePointer();
}
