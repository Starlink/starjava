package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.InputStream;

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
abstract class ProvisionalDataSource extends PathedDataSource {

    private InputStream provisionalStream;
    private long rawLength = -1L;
    private String path;

    /**
     * Constructs a ProvisionalDataSource with a given path and known size.
     *
     * @param  path  the full path of the source if known (may be null)
     * @param  rawLength  the length of the raw source in bytes 
     *         (may be -1 if not known)
     */
    public ProvisionalDataSource( String path, long rawLength ) {
        this.path = path;
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
     * will be given the value specified in this method (<tt>strm</tt>),
     * otherwise {@link #getBackupRawInputStream} will be invoked.
     *
     * @param   strm  the new provisional input stream to use.
     *          May be <tt>null</tt> to indicate that there is none
     */
    public void setProvisionalStream( InputStream strm ) {
        this.provisionalStream = strm;
    }

    public String getPath() {
        return path;
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
