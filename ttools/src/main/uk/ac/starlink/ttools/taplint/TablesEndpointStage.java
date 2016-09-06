package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.EndpointSet;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TableSetSaxHandler;

/**
 * Validation stage for checking table metadata from the /tables endpoint
 * (as defined by the VODataService schema).
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TablesEndpointStage extends TableMetadataStage {

    private final ContentCoding coding_;

    public TablesEndpointStage() {
        super( "/tables",
               new String[] { "indexed", "primary", "nullable" }, true );
        coding_ = ContentCoding.NONE;
    }

    protected SchemaMeta[] readTableMetadata( Reporter reporter,
                                              EndpointSet endpointSet ) {
        URL turl = endpointSet.getTablesEndpoint();
        reporter.report( FixedCode.I_TURL,
                         "Reading table metadata from " + turl );

        /* Open URL connection. */
        final URLConnection conn;
        try {
            conn = turl.openConnection();
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_FLIO,
                             "Can't open tables endpoint", e );
            return null;
        }
        coding_.prepareRequest( conn );
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setInstanceFollowRedirects( true );
            final int code;
            try {
                code = hconn.getResponseCode();
            }
            catch ( IOException e ) {
                reporter.report( FixedCode.E_FLIO,
                                 "Can't open tables endpoint", e );
                return null;
            }

            /* 404 is grudgingly permitted by TAP 1.0 sec 2.2.5. */
            if ( code == 404 ) {
                String msg = new StringBuffer()
                   .append( "/tables resource, recommended but not required," )
                   .append( " is absent - " )
                   .append( code )
                   .append( " at " )
                   .append( turl )
                   .toString();
                reporter.report( FixedCode.W_TBNF, msg );
                return null;
            }
            else if ( code != HttpURLConnection.HTTP_OK ) {
                String msg = new StringBuffer()
                   .append( "HTTP response " )
                   .append( code )
                   .append( " from /tables endpoint" )
                   .append( " - should be 200 or 404" )
                   .toString();
                reporter.report( FixedCode.E_FLIO, msg );
                return null;
            }
        }

        /* Get input stream. */
        final InputStream in;
        try {
            in = new BufferedInputStream( coding_.getInputStream( conn ) );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_FLIO,
                             "Error reading from /tables endpoint", e );
            return null;
        }

        /* Prepare SAX parser. */
        final SAXParser parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setNamespaceAware( false );
            spfact.setValidating( false );
            parser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            reporter.report( FixedCode.F_CAPC,
                             "Trouble setting up XML parse", e );
            return null;
        }
        catch ( SAXException e ) {
            reporter.report( FixedCode.F_CAPC,
                             "Trouble setting up XML parse", e );
            return null;
        }

        /* Read and parse tables document into a handler object. */
        final TableSetSaxHandler populatedHandler;
        try {
            TableSetSaxHandler handler = new TableSetSaxHandler();
            parser.parse( in, handler );
            populatedHandler = handler;
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

        /* Extract metadata items from populated handler. */
        List<SchemaMeta> schemaList = new ArrayList<SchemaMeta>();
        schemaList.addAll( Arrays.asList( populatedHandler
                                         .getSchemas( false ) ) );
        TableMeta[] nakedTables = populatedHandler.getNakedTables();
        int nNaked = nakedTables.length;
        if ( nNaked > 0 ) {
            String msg = new StringBuffer()
               .append( nNaked )
               .append( " tables declared outside of any schema " )
               .toString();
            reporter.report( FixedCode.E_NAKT, msg );
            SchemaMeta dummySchema =
                SchemaMeta.createDummySchema( "<no_schema>" );
            dummySchema.setTables( nakedTables );
            schemaList.add( dummySchema );
        }

        /* Return metadata as array of schemas. */
        return schemaList.toArray( new SchemaMeta[ 0 ] );
    }
}
