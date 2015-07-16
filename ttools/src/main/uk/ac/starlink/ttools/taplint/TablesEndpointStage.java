package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableSetSaxHandler;

/**
 * Validation stage for checking table metadata from the /tables endpoint
 * (as defined by the VODataService schema).
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TablesEndpointStage extends TableMetadataStage {

    public TablesEndpointStage() {
        super( "/tables",
               new String[] { "indexed", "primary", "nullable" }, true );
    }

    protected SchemaMeta[] readTableMetadata( Reporter reporter,
                                              URL serviceUrl ) {
        String turl = serviceUrl + "/tables";
        reporter.report( FixedCode.I_TURL,
                         "Reading table metadata from " + turl );
        try {
            return TableSetSaxHandler.readTableSet( new URL( turl ),
                                                    ContentCoding.NONE );
        }
        catch ( SAXException e ) {
            reporter.report( FixedCode.E_FLSX,
                             "Can't parse table metadata well enough "
                            + "to check it", e );
            return null;
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_FLIO,
                             "Error reading table metadata", e );
            return null;
        }
    }
}
