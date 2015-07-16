package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableSetSaxHandler;

/**
 * MetadataHolder implementation which supplies metadata based on the
 * assumed (mandated) form of the TAP_SCHEMA tables which a TAP service
 * is required to supply.
 *
 * @author   Mark Taylor
 * @since    28 Jun 2011
 */
public class TapSchemaMetadataHolder implements MetadataHolder {

    private Reporter reporter_;
    private SchemaMeta[] metadata_;

    /**
     * Constructor.
     */
    public TapSchemaMetadataHolder() {
    }

    /**
     * Sets the reporter for this object.
     *
     * @param  reporter  destination for validation messages
     */
    public void setReporter( Reporter reporter ) {
        reporter_ = reporter;
    }

    public SchemaMeta[] getTableMetadata() {
        if ( metadata_ == null ) {
            metadata_ = readSchemaMetadata();
        }
        return metadata_;
    }

    /**
     * Does the work for reading the metadata from a file which should be
     * present on the classpath.
     */
    private SchemaMeta[] readSchemaMetadata() {
        URL schemaTablesUrl = TapSchemaMetadataHolder.class
                             .getResource( "TAP_SCHEMA_tables.xml" );
        if ( schemaTablesUrl == null ) {
            return null;
        }
        else {
            try {
                SchemaMeta[] smetas =
                    TableSetSaxHandler.readTableSet( schemaTablesUrl,
                                                     ContentCoding.NONE );
                if ( reporter_ != null ) {
                    reporter_.report( FixedCode.I_SCHM,
                                      "Using standard TAP_SCHEMA tables for "
                                    + "metadata" );
                }
                return smetas;
            }
            catch ( IOException e ) {
                return null;
            }
            catch ( SAXException e ) {
                return null;
            }
        }
    }
}
