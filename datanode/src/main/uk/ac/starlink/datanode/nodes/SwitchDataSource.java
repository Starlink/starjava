package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.util.DataSource;

/**
 * DataSource which can supply a given 'provisional' stream when it has one,
 * and a different one when it doesn't (or if it's already used its
 * provisional one).
 * As usual with DataSources, the provisional stream should provide
 * the same data as the one implemented in getBackupRawInputStream.
 * <p>
 * This rather specialised class is used by archive nodes like
 * TarStreamDataNode and ZipStreamDataNode.
 */
abstract class SwitchDataSource extends DataSource {

    private InputStream provisionalStream;
    private long rawLength = -1L;

    /**
     * Constructs a SwitchDataSource with a known size.
     *
     * @param  rawLength  the length of the raw source in bytes 
     *         (may be -1 if not known)
     */
    public SwitchDataSource( long rawLength ) {
        this.rawLength = rawLength;
    }

    /**
     * Override this method to arrange provision of a raw input stream
     * when the provisional input stream is set to null.
     *
     * @return  a raw input stream suitable for use as per the general
     *          contract of {uk.ac.starlink.util.DataSource#getRawInputStream}
     */
    abstract protected InputStream getBackupRawInputStream() throws IOException;
            
    /**
     * Set the provisional input stream.  If this is set to a non-null
     * value, then the next getRawInputStream invocation on this DataSource
     * will be given the value specified in this method (<code>strm</code>),
     * otherwise {@link #getBackupRawInputStream} will be invoked.
     *
     * @param   strm  the new provisional input stream to use.
     *          May be <code>null</code> to indicate that there is none
     */
    public void setProvisionalStream( InputStream strm ) {
        this.provisionalStream = strm;
    }

    public long getRawLength() {
        return rawLength;
    }

    protected InputStream getRawInputStream() throws IOException {
        InputStream strm;
        if ( provisionalStream != null ) {
            strm = provisionalStream;
            provisionalStream = null;
        }
        else {
            strm = getBackupRawInputStream();
        }
        return strm;
    }

}
