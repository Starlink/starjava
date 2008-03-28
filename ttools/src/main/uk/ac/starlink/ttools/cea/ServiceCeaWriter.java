package uk.ac.starlink.ttools.cea;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.xml.sax.SAXException;
import uk.ac.starlink.ttools.Stilts;

/**
 * CeaWriter implementation which writes a registry record document 
 * describing a CEA Service instance.
 * This is what is submitted to the registry to define the service.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class ServiceCeaWriter extends CeaWriter {

    private static final String AG_SCHEMA_BASE =
        "http://www.astrogrid.org/schema/";
    private static final String IVOA_SCHEMA_BASE =
        "http://www.ivoa.net/xml/";
    private static final String VOR_NS =
        IVOA_SCHEMA_BASE + "RegistryInterface/v0.1";
    private static final String VRX_NS =
        IVOA_SCHEMA_BASE + "VOResource/v0.10";
    private static final String CEAS_NS =
        IVOA_SCHEMA_BASE + "CEAService/v0.2";
    private static final String CEAB_NS =
        AG_SCHEMA_BASE + "CommonExecutionArchitectureBase/v1";
    private static final String AGPD_NS =
        AG_SCHEMA_BASE + "AGParameterDefinition/v1";

    public static final String SCHEMA_LOCATION =
        "http://software.astrogrid.org/schema/" +
        "vo-resource-types/CEAService/v0.2/CEAService.xsd";

    private final CeaMetadata meta_;

    /**
     * Constructor.
     *
     * @param  out  output stream for XML
     * @param  tasks  list of tasks to be described by the output
     * @param  meta   application description metadata object
     * @param  redirects  true iff you want stdout/stderr parameters for
     *                    standard output/error redirection
     * @param  cmdline  command line string, used for logging within the
     *                  output only
     */
    public ServiceCeaWriter( PrintStream out, CeaTask[] tasks, CeaMetadata meta,
                             boolean redirects, String cmdline ) {
        super( out, createServiceConfig(), tasks, redirects, cmdline );
        meta_ = meta;
    }

    public static String getUsage() {
        return "";
    }

    public int configure( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );
        if ( ! argList.isEmpty() ) {
            return 1;
        }
        return 0;
    }

    protected void writeContent() throws SAXException {
        startElement( "vor:Resource",
             formatAttribute( "xmlns", VRX_NS )
           + formatAttribute( "xmlns:vor", VOR_NS )
           + formatAttribute( "xmlns:ceas", CEAS_NS )
           + formatAttribute( "xmlns:xsi",
                              "http://www.w3.org/2001/XMLSchema-instance" )
           + formatAttribute( "xmlns:schemaLocation",
                              CEAS_NS + " " + SCHEMA_LOCATION )
           + formatAttribute( "xsi:type", "ceas:CeaApplicationType" ) );
        addElement( "title", "", meta_.getLongName() );
        addElement( "shortName", "", meta_.getShortName() );
        addElement( "identifier", "", meta_.getIvorn() );

        startElement( "curation" );
        addElement( "publisher", "", "Astrogrid" );
        startElement( "creator" );
        addElement( "name", "", "Mark Taylor" );
        endElement( "creator" );
        addElement( "date",
                    formatAttribute( "role", "Resource description generated" ),
                    new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() ) );
        addElement( "version", "", Stilts.getVersion() );
        startElement( "contact" );
        addElement( "name", "", "Mark Taylor" );
        addElement( "address", "",
                    "Astrophysics Group, Physics Department, "
                  + "Bristol University, UK" );
        addElement( "email", "", "m.b.taylor@bristol.ac.uk" );
        endElement( "contact" );
        endElement( "curation" );

        startElement( "content" );
        addElement( "subject", "", "catalogs tables" );
        startElement( "description" );
        print( meta_.getDescription() );
        endElement( "description" );
        addElement( "referenceURL", "", meta_.getRefUrl() );
        addElement( "type", "", "Other" );
        endElement( "content" );

        startElement( "ApplicationDefinition",
                      formatAttribute( "xmlns", CEAS_NS ) );
        writeParameters();
        writeInterfaces();
        endElement( "ApplicationDefinition" );
        endElement( "vor:Resource" );
    }

    public String getSchemaLocation() {
        return SCHEMA_LOCATION;
    }

    /**
     * Constructs a CeaConfig object which encapsulates requirements 
     * specific to this CeaWriter implementation.
     *
     * @return  suitable CeaConfig object
     */
    private static CeaConfig createServiceConfig() {
        ElementDeclaration ifsDecl =
            new ElementDeclaration( "ceas:Interfaces",
                                    formatAttribute( "xmlns", CEAB_NS ) );
        ElementDeclaration paramsDecl =
            new ElementDeclaration( "Parameters" );
        ElementDeclaration paramDecl =
            new ElementDeclaration( "ceas:ParameterDefinition",
                                    formatAttribute( "xmlns", AGPD_NS ) );
            new ElementDeclaration( "ParameterDefinition" );
        paramDecl.setAttributeNames( new String[ 0 ] );
        return new CeaConfig( ifsDecl, paramsDecl, paramDecl );
    }
}
