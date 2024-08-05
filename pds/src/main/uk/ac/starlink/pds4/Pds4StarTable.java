package uk.ac.starlink.pds4;

import gov.nasa.pds.label.object.FieldType;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;
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
     * @param  contextUri   parent URI for the PDS4 label
     */
    @SuppressWarnings("this-escape")
    protected Pds4StarTable( Table table, URI contextUri ) throws IOException {
        dataUrl_ = contextUri.resolve( table.getFileName() ).toURL();
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

    /**
     * Creates column metadata description for a given field.
     *
     * @param  field  field to describe
     * @param  clazz  content class of field (may be array or scalar)
     * @return  configured column info
     */
    static ColumnInfo createColumnInfo( Field field, Class<?> clazz ) {
        ColumnInfo info =
            new ColumnInfo( field.getName(), clazz, field.getDescription() );
        info.setUnitString( field.getUnit() );
        DomainMapper mapper = getDomainMapper( field.getFieldType() );
        if ( mapper != null &&
             clazz.isAssignableFrom( mapper.getSourceClass() ) ) {
            info.setDomainMappers( new DomainMapper[] { mapper } );
        }
        return info;
    }

    /**
     * Returns a suitable domain mapper for a given PDS4 field type.
     *
     * @param  ftype  field type
     * @return  potentially applicable domain mapper, may be null
     */
    private static DomainMapper getDomainMapper( FieldType ftype ) {
        switch ( ftype ) {

            /* These ASCII_DATE* types are defined in the PDS4 Standards
             * Reference 1.16.0; they all return ISO-8601 dates, using
             * either Calendar (YYYY-MM-DD) or Ordinal (YYYY-DOY) format.
             * This ignores any distinction between UTC and local time zone. */
            case ASCII_DATE_DOY:
            case ASCII_DATE_TIME_DOY:
            case ASCII_DATE_TIME_DOY_UTC:
            case ASCII_DATE_TIME_YMD:
            case ASCII_DATE_TIME_YMD_UTC:
            case ASCII_DATE_YMD:

            /* These ASCII_DATE* types don't seem to be referenced in the
             * PDS4 standards, but they exist in the pds4-jparser enum.
             * If they exist, presume that they are treated the same
             * as the others. */
            case ASCII_DATE:   // nope
            case ASCII_DATE_TIME: // nope
            case ASCII_DATE_TIME_UTC: // nope
                return TimeMapper.ISO_8601;

            /* No other domain mappers are applicable. */
            default:
                return null;
        }
    }
}
