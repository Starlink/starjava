package uk.ac.starlink.pds4;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.IOUtils;

/**
 * Abstract superclass for PDS4 StarTable implementations.
 * This handles table metadata that is not dependent on the exact
 * storage format, but concrete subclasses have to implement
 * data access (<code>getRowSequence</code>).
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public abstract class Pds4StarTable extends AbstractStarTable {

    private final URL dataUrl_;
    private final long dataOffset_;
    private final long nrow_;
    public static final ValueInfo DESCRIPTION_INFO =
        new DefaultValueInfo( "Description", String.class, "table description");

    /**
     * Constructor.
     *
     * @param  table  table object on which this table is based
     * @param  contextUrl   parent URL for the PDS4 label
     */
    protected Pds4StarTable( Table table, URL contextUrl ) throws IOException {
        dataUrl_ = new URL( contextUrl, table.getFileName() );
        dataOffset_ = table.getOffset();
        nrow_ = table.getRecordCount();
        String name = table.getName();
        if ( name != null ) {
            setName( name );
        }
        else {
            String localIdentifier = table.getLocalIdentifier();
            if ( localIdentifier != null ) {
                setName( localIdentifier );
            }
        }
        String description = table.getDescription();
        if ( description != null ) {
            getParameters().add( new DescribedValue( DESCRIPTION_INFO,
                                                     description ) );
        }
    }

    public long getRowCount() {
        return nrow_;
    }

    /**
     * Returns an input stream for retrieving table data,
     * positioned at the start of the data for this table.
     *
     * @return   correctly-positioned input stream bearing table data
     */
    InputStream getDataStream() throws IOException {
        InputStream in = new BufferedInputStream( dataUrl_.openStream() );
        IOUtils.skip( in, dataOffset_ );
        return in;
    }

    /**
     * Returns the URL of the data file for this table.
     *
     * @return  data file URL
     */
    URL getDataUrl() {
        return dataUrl_;
    }

    /**
     * Returns the byte offset into the data file at which this table's
     * data begins.
     *
     * @return  data file offset
     */
    long getDataOffset() {
        return dataOffset_;
    }
}
