package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.SAXException;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;

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

    protected TableMeta[] readTableMetadata( Reporter reporter,
                                             URL serviceUrl ) {
        reporter.report( ReportType.INFO, "TURL",
                         "Reading table metadata from "
                       + serviceUrl + "/tables" );
        try {
            return TapQuery.readTableMetadata( serviceUrl );
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.ERROR, "FLSX",
                             "Can't parse table metadata well enough "
                            + "to check it", e );
            return null;
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "FLIO",
                             "Error reading table metadata", e );
            return null;
        }
    }
}
