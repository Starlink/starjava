package uk.ac.starlink.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * DataSource implementation that uses an internal byte buffer to store
 * the data.
 *
 * @author   Mark Taylor
 * @since    1 Feb 2007
 */
public class ByteArrayDataSource extends DataSource {

    private final String name_;
    private final byte[] buffer_;

    /**
     * Constructor.
     *
     * @param   name  data source name
     * @param   buffer containing byte content of the source
     */
    public ByteArrayDataSource( String name, byte[] buffer ) {
        name_ = name;
        buffer_ = buffer;
    }

    public String getName() {
        return name_;
    }

    public InputStream getRawInputStream() {
        return new ByteArrayInputStream( buffer_ );
    }

    public long getRawLength() {
        return buffer_.length;
    }
}
