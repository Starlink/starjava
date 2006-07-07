package nom.tam.util;

import java.io.DataInput;
import java.io.DataOutput;

/** This interface combines the DataInput, DataOutput and
 *  RandomAccess interfaces to provide a reference type
 *  which can be used to build BufferedFile in a fashion
 *  that accommodates both the RandomAccessFile and ByteBuffers
 */
public interface DataIO extends DataInput, DataOutput, RandomAccess {
}
