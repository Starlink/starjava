package uk.ac.starlink.ttools.taplint;

import java.net.URL;

/**
 * Validation stage for validating tables metadata against the
 * VODataService XSD schema.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TableMetadataXsdStage extends XsdStage {

    public TableMetadataXsdStage() {
        super( XSDS + "/VODataService/VODataService-v1.1.xsd" );
    }

    public String getDescription() {
        return "Validate table metadata against schema";
    }

    public String getDocumentUrl( URL serviceUrl ) {
        return serviceUrl + "/tables";
    }

    public void run( URL serviceUrl, Reporter reporter ) {
        super.run( serviceUrl, reporter );
        Result result = getResult();
        if ( result == Result.NOT_FOUND ) {
            reporter.report( Reporter.Type.WARNING, "NOTM",
                             "/tables resource not present (not mandatory)" );
        }
    }
}
