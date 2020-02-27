package uk.ac.starlink.feather;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.bristol.star.feather.BufUtils;
import uk.ac.bristol.star.feather.ColStat;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StoragePolicy;

/**
 * Partial ItemAccumulator implementation that handles
 * generic aspects including writing the optional validity mask.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2020
 */
public abstract class AbstractItemAccumulator implements ItemAccumulator {

    private final StoragePolicy storage_;
    private final boolean isNullable_;
    private final ByteStore maskStore_;
    private final OutputStream maskOut_;
    private long nRow_;
    private long nNull_;
    private int ibit_;
    private int mask_;

    /**
     * Constructor.
     *
     * @param  storage  storage policy for buffering output data
     * @param  isNullable   if true, preparations will be made to write
     *                      the validity mask; it will only be actually
     *                      output if there are some null values
     */
    protected AbstractItemAccumulator( StoragePolicy storage,
                                       boolean isNullable ) {
        storage_ = storage;
        isNullable_ = isNullable;
        maskStore_ = isNullable ? storage.makeByteStore() : null;
        maskOut_ = isNullable
                 ? new BufferedOutputStream( maskStore_.getOutputStream() )
                 : null;
    }

    /**
     * Adds the given item to the list to be output.
     * This method is invoked from this AbstractItemAccumulator's
     * {@link #addItem} method, and is just the hook for the
     * subclass-specific behaviour.
     *
     * @param  item  cell value
     */
    protected abstract void addDataItem( Object item ) throws IOException;

    /**
     * Releases resources.
     * This method is invoked from this AbstractItemAccumulator's
     * {@link #close} method, and is just the hook for the
     * subclass-specific behaviour.
     */
    protected abstract void closeData() throws IOException;

    /**
     * Tests whether a given added data item is null
     * (whether it needs to be flagged as null in the validity mask).
     * This method is only ever called for nullable columns
     * (if the <code>isNullable</code> flag was set true at construction time).
     *
     * <p>The default implementation tests whether the supplied item
     * is in fact <code>null</code>, but it may be overridden.
     *
     * @param  item  value to test
     * @return  true iff item is to be flagged null
     */
    protected boolean isNull( Object item ) {
        return item == null;
    }

    /**
     * Writes the bytes constituting the data stream for this column,
     * excluding any optional validity mask.
     * Note the output does not need to be aligned on an 8-byte boundary.
     *
     * @param   out  destination stream
     * @return   number of bytes written
     */
    public abstract long writeDataBytes( OutputStream out ) throws IOException;

    public void close() throws IOException {
        if ( isNullable_ ) {
            maskOut_.close();
            maskStore_.close();
        }
        closeData();
    }

    public void addItem( Object item ) throws IOException {
        nRow_++;
        if ( isNullable_ ) {
            if ( isNull( item ) ) {
                nNull_++;
            }
            else {
                mask_ |= 1 << ibit_;
            }
            if ( ++ibit_ == 8 ) {
                maskOut_.write( mask_ );
                ibit_ = 0;
                mask_ = 0;
            }
        }
        addDataItem( item );
    }

    public ColStat writeColumnBytes( OutputStream out ) throws IOException {
        final long nNullByte;
        if ( isNullable_ ) {
            if ( ibit_ > 0 ) {
                maskOut_.write( mask_ );
            }
            long nbMask = ( nRow_ + 7 ) / 8;
            nbMask += BufUtils.align8( maskOut_, nbMask );
            maskOut_.close();
            if ( nNull_ > 0 ) {
                maskStore_.copy( out );
                nNullByte = nbMask;
            }
            else {
                nNullByte = 0;
            }
            maskStore_.close();
        }
        else {
            nNullByte = 0;
        }
        long nbData = writeDataBytes( out );
        long nDataByte = nbData + BufUtils.align8( out, nbData );

        final long rowCount = nRow_;
        final long nullCount = nNull_;
        final long byteCount;
        final long dataOffset;
        if ( nNull_ > 0 ) {
            byteCount = nNullByte + nDataByte;
            dataOffset = nNullByte;
        }
        else {
            byteCount = nDataByte;
            dataOffset = 0;
        }
        return new ColStat() {
            public long getByteCount() {
                return byteCount;
            }
            public long getNullCount() {
                return nullCount;
            }
            public long getRowCount() {
                return rowCount;
            }
            public long getDataOffset() {
                return dataOffset;
            }
        };
    }
}
