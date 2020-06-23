package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.StdCapabilityInterface;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TableSetSaxHandler;
import uk.ac.starlink.vo.TapService;

/**
 * Validation stage for checking table metadata from the /tables endpoint
 * (as defined by the VODataService schema).
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TablesEndpointStage extends TableMetadataStage {

    private final CapabilityHolder capHolder_;
    private final ContentCoding coding_;

    public TablesEndpointStage( CapabilityHolder capHolder ) {
        super( "/tables",
               new String[] { "indexed", "primary", "nullable" }, true );
        capHolder_ = capHolder;
        coding_ = ContentCoding.NONE;
    }

    protected MetadataHolder readTableMetadata( Reporter reporter,
                                                TapService tapService ) {

        /* Determine if tables endpoint is declared. */
        StdCapabilityInterface[] intfs = capHolder_.getInterfaces();
        boolean isVosi11 = false;
        Boolean declaresTables;
        if ( intfs == null ) {
            declaresTables = null;
        }
        else {
            declaresTables = Boolean.FALSE; 
            for ( StdCapabilityInterface intf : intfs ) {
                String stdid = intf.getStandardId();
                if ( stdid != null &&
                     stdid.startsWith( "ivo://ivoa.net/std/VOSI#tables" ) ) {
                    declaresTables = Boolean.TRUE;
                    String vers = intf.getVersion();
                    if ( vers != null && vers.matches( "1[.][1-9][0-9]*" ) ) {
                        isVosi11 = true;
                    }
                }
            }
        }

        /* Prepare to read tables document.  Request maximum detail if we
         * are talking to a VOSI 1.1 tables endpoint.  This doesn't mean
         * that we will necessarily get maximum detail, but if it doesn't
         * want to give it to us here, we ought not to subvert that policy
         * by attempting to get it in another way. */
        String query = isVosi11 ? "?detail=max" : "";
        final URL turl;
        try {
            turl = new URL( tapService.getTablesEndpoint() + query );
        }
        catch ( MalformedURLException e ) {
            throw new RuntimeException( "Shouldn't happen: " + e, e );
        }
        reporter.report( FixedCode.I_TURL,
                         "Reading table metadata from " + turl );

        /* Open URL connection. */
        final URLConnection conn;
        try {
            conn = AuthManager.getInstance().connect( turl, coding_ );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_FLIO,
                             "Can't open tables endpoint", e );
            return null;
        }
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
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
                if ( Boolean.TRUE.equals( declaresTables ) ) {
                    reporter.report( FixedCode.E_TADH,
                                     "Tables endpoint declared but absent" );
                }
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

        /* Check if declaration matches presence. */
        if ( Boolean.FALSE.equals( declaresTables ) ) {

            /* TAP 1.1 (PR-20180830) mandates that the examples endpoint
             * must be declared in the capabilities if it is present.
             * TAP 1.0 doesn't (examples wasn't invented then).
             * So adjust the report level in case of discrepancy. */
            boolean is11 = tapService.getTapVersion().is11();
            reporter.report( is11 ? FixedCode.E_TADH : FixedCode.W_TADH,
                             "Tables endpoint present but undeclared" );
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
        final SchemaMeta[] smetas = schemaList.toArray( new SchemaMeta[ 0 ] );
        final boolean hasDetail = hasDetail( reporter, turl, smetas, isVosi11 );
        return new MetadataHolder() {
            public SchemaMeta[] getTableMetadata() {
                return smetas;
            }
            public boolean hasDetail() {
                return hasDetail;
            }
        };
    }

    /**
     * Indicates whether the table metadata includes per-table detail,
     * that is column and foreign key information.
     * At least at VOSI 1.1, it is optional whether the service supplies
     * such detail from the tables endpoint.
     *
     * <p>Anomalies are reported through the logging system.
     *
     * @param  reporter  reporter
     * @param  turl    tables URL, used for reports
     * @param  smetas   populated schema metadata array
     * @param  isVosi11  true if tables endpoint declares itself as
     *                   at least VOSI 1.1
     * @return   true if at least some of the column metadata is supplied
     */
    private boolean hasDetail( Reporter reporter, URL turl,
                               SchemaMeta[] smetas, boolean isVosi11 ) {
        int nTable = 0;
        int nColDetail = 0;
        int nKeyDetail = 0;
        for ( SchemaMeta smeta : smetas ) {
            for ( TableMeta tmeta : smeta.getTables() ) {
                nTable++;
                ColumnMeta[] cmetas = tmeta.getColumns();
                ForeignMeta[] fmetas = tmeta.getForeignKeys();
                if ( cmetas != null && cmetas.length > 0 ) {
                    nColDetail++;
                }
                if ( fmetas != null && fmetas.length > 0 ) {
                    nKeyDetail++;
                }
            }
        }
        if ( nColDetail == 0 ) {
            if ( isVosi11 ) {
                reporter.report( FixedCode.I_CDET,
                                 "No column detail returned from " + turl );
            }
            else {
                reporter.report( FixedCode.W_CDET,
                                 "No column detail returned from VOSI 1.0"
                               + " endpoint " + turl );
            }
            return false;
        }
        if ( nColDetail == nTable ) {
            return true;
        }
        assert nColDetail < nTable;
        String msg = "Column detail returned for " + nColDetail + "/" + nTable
                   + " tables from " + turl;
        reporter.report( FixedCode.W_CDET, msg );
        return false;
    }
}
