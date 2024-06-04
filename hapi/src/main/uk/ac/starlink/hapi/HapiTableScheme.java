package uk.ac.starlink.hapi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableScheme;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.IOConsumer;
import uk.ac.starlink.util.IOSupplier;

/**
 * TableScheme implementation for interacting with HAPI services.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class HapiTableScheme implements TableScheme, Documented {

    private final boolean supplyDocExample_;

    private static final String DOC_EXAMPLE =
        "https://vires.services/hapi;GRACE_A_MAG" +
        ";start=2009-01-01T00:00:00;stop=2009-01-01T00:00:10" +
        ";parameters=Latitude,Longitude";
    private static final String CHUNKLIMIT_PARAM = "maxChunk";
    private static final String FAILONLIMIT_PARAM = "failOnLimit";
    private static final int CHUNKLIMIT_DFLT = 1;
    private static final boolean FAILONLIMIT_DFLT = false;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    public HapiTableScheme() {

        /* If true, the example output will be generated automatically at
         * build time.  The trouble is that would require the build process
         * to contact a HAPI server, and build should not be a
         * network-dependent stage, so set it false and use the hard-coded
         * example output. */
        supplyDocExample_ = false;
    }

    public String getSchemeName() {
        return "hapi";
    }

    public String getSchemeUsage() {
        return "<server-url>;<dataset>;start=<start>;stop=<stop>"
             + "[;" + CHUNKLIMIT_PARAM + "=<n>]"
             + "[;" + FAILONLIMIT_PARAM + "=<true|false>]"
             + "[;<key>=<value>...]";
    }

    public String getExampleSpecification() {
        return supplyDocExample_ ? DOC_EXAMPLE : null;
    }

    public String getXmlDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( String.join( "\n",
            "<p>Generates a table by interacting with a HAPI service.",
            "HAPI, the",
            "<a href='http://hapi-server.org/'",
            "   >Heliophysics Data Application Programmer’s Interface</a>",
            "is a protocol for serving streamed time series data.",
            "</p>",
            "<p>In most cases it is not essential to use this scheme,",
            "since pointing the HAPI table input handler",
            "at a URL with suitable parameters will be able to read the data,",
            "but this scheme provides some added value",
            "by negotiating with the server to make sure that",
            "the correct version-sensitive request parameter names",
            "and the most efficient data stream format are used,",
            "and can split the request into multiple chunks",
            "if the service rejects the whole query as too large.",
            "</p>",
            "<p>The first token in the specification is",
            "the base URL of the HAPI service,",
            "the second is the dataset identifier,",
            "and others, as defined by the HAPI protocol, are supplied as",
            "<code>&lt;name&gt;=&lt;value&gt;</code> pairs,",
            "separated by a semicolon (\"<code>;</code>\")",
            "or an ampersand (\"<code>&amp;</code>\").",
            "The <code>start</code> and <code>stop</code> parameters,",
            "giving ISO-8601-like bounds for the interval requested,",
            "are required.",
            "</p>",
            "<p>Additionally, some parameters may be supplied which",
            "affect load behaviour but are not transmitted to the HAPI",
            "service.  These are:",
            "<dl>",
            "<dt><code>" + CHUNKLIMIT_PARAM + "=&lt;n&gt;</code></dt>",
            "<dd><p>divides the request up into at most <code>&lt;n&gt;</code>",
            "    smaller chunks",
            "    if the server refuses to supply the whole range at once.",
            "    </p></dd>",
            "<dt><code>" + FAILONLIMIT_PARAM
                         + "=&lt;true|false&gt;</code></dt>",
            "<dd><p>determines what happens if the service does refuse",
            "    to serve the whole range (in chunks or otherwise);",
            "    if true, the table load will fail,",
            "    but if false as many rows as are available will be loaded.",
            "    </p></dd>",
            "</dl>",
            "</p>",
            "<p>Some variant syntax is permitted;",
            "an ampersand (\"<code>&amp;</code>\") may be used instead of",
            "a semicolon to separate tokens,",
            "and the names \"<code>time.min</code>\" and",
            "\"<code>time.max</code>\" may be used in place of",
            "\"<code>start</code>\" and \"<code>stop</code>\".",
            "</p>",
            "<p>Note that since semicolons and/or ampersands form part of",
            "the syntax, and these characters have special meaning",
            "in some contexts,",
            "it may be necessary to quote the scheme specification",
            "on the command line.",
            "</p>"
        ) );
        if ( ! supplyDocExample_ ) {
            sbuf.append( String.join( "\n",
                "<p>Example:",
                "<verbatim><![CDATA[",
                ":" + getSchemeName() + ":" + DOC_EXAMPLE,
                "+--------------------------+---------------+---------------+",
                "| Timestamp                | Latitude      | Longitude     |",
                "+--------------------------+---------------+---------------+",
                "| 2009-01-01T00:00:03.607Z | -74.136357526 | -78.905620222 |",
                "| 2009-01-01T00:00:05.607Z | -74.009378676 | -78.884853931 |",
                "| 2009-01-01T00:00:06.607Z | -73.945887793 | -78.874590667 |",
                "| 2009-01-01T00:00:07.607Z | -73.882397005 | -78.864406236 |",
                "| 2009-01-01T00:00:08.607Z | -73.818903534 | -78.854396448 |",
                "+--------------------------+---------------+---------------+",
                "]]></verbatim>",
                "</p>",
            "" ) );
        }
        return sbuf.toString();
    }

    public StarTable createTable( String specification ) throws IOException {

        /* Parse arguments. */
        String[] args = specification.split( "[;&]", -1 );
        if ( args.length < 2 ) {
            throw new TableFormatException( "Must specify server and dataset" );
        }
        String server = args[ 0 ];
        String dataset = args[ 1 ];
        HapiService service = new HapiService( server );
        Map<String,String> extrasMap = new LinkedHashMap<>();
        int chunkLimit = CHUNKLIMIT_DFLT;
        boolean failOnLimit = FAILONLIMIT_DFLT;
        for ( int i = 2; i < args.length; i++ ) {
            String arg = args[ i ];
            int ieq = arg.indexOf( "=" );
            if ( ieq > 0 ) {
                String key = arg.substring( 0, ieq );
                String value = arg.substring( ieq + 1, arg.length() );
                if ( CHUNKLIMIT_PARAM.equalsIgnoreCase( key ) ) {
                    try {
                        chunkLimit = Integer.parseInt( value );
                    }
                    catch ( NumberFormatException e ) {
                        String msg = "Bad " + CHUNKLIMIT_PARAM + " value";
                        throw new TableFormatException( msg );
                    }
                }
                else if ( FAILONLIMIT_PARAM.equalsIgnoreCase( key ) ) {
                    failOnLimit = Boolean.parseBoolean( value );
                }
                else {
                    extrasMap.put( arg.substring( 0, ieq ),
                                   arg.substring( ieq + 1, arg.length() ) );
                }
            }
            else {
                String msg = "Optional parameter \"" + arg + "\" "
                           + "is not of form <key>=<value>";
                throw new TableFormatException( msg );
            }
        }
        return createHapiTable( service, dataset, extrasMap,
                                chunkLimit, failOnLimit );
    }

    /**
     * Returns a table given a parsed specification.
     *
     * @param  service  HAPI service
     * @param  dataset   dataset identifier
     * @param  requestParams   request parameters supplied explicitly
     * @param  chunkLimit  chunk limit
     * @param  failOnLimit  chunk overflow policy
     */
    private static StarTable createHapiTable( HapiService service,
                                              String dataset,
                                              Map<String,String> requestParams,
                                              int chunkLimit,
                                              boolean failOnLimit )
            throws IOException {
        String paramlist = requestParams.get( "parameters" );

        /* Determine HAPI version. */
        HapiCapabilities caps =
            HapiCapabilities
           .fromJson( service.readJson( HapiEndpoint.CAPABILITIES ) );
        HapiVersion version = caps.getHapiVersion();
        String datasetParamName = version.getDatasetRequestParam();
        String startParamName = version.getStartRequestParam();
        String stopParamName = version.getStopRequestParam();
        boolean supportsBinary = Arrays.stream( caps.getOutputFormats() )
                                       .anyMatch( s -> "binary".equals( s ) );

        /* Make a metadata request and turn the result into a HAPI header. */
        Map<String,String> infoParams = new LinkedHashMap<>();
        infoParams.put( datasetParamName, dataset );
        if ( paramlist != null ) {
            infoParams.put( "parameters", paramlist );
        }
        HapiInfo infoHdr =
            HapiInfo
           .fromJson( service.readJson( HapiEndpoint.INFO, infoParams ) );

        /* Make sure we have start and stop times,
         * since the request will fail without them. */
        String start = null;
        String stop = null;
        for ( HapiVersion v : HapiVersion.getStandardVersions() ) {
            if ( start == null ) {
                start = requestParams.get( v.getStartRequestParam() );
            }
            if ( stop == null ) {
                stop = requestParams.get( v.getStopRequestParam() );
            }
        }
        if ( start == null ) {
            start = infoHdr.getMetadata( "sampleStartDate" );
        }
        if ( stop == null ) {
            stop = infoHdr.getMetadata( "sampleStopDate" );
        }
        if ( start == null || stop == null ) {
            StringBuffer sbuf = new StringBuffer()
               .append( "Must supply start/stop; " )
               .append( "range is " )
               .append( infoHdr.getMetadata( "startDate" ) )
               .append( " to " )
               .append( infoHdr.getMetadata( "stopDate" ) );
            String cadence = infoHdr.getMetadata( "cadence" );
            if ( cadence != null ) {
                sbuf.append( ", cadence is " )
                    .append( cadence );
            }
            sbuf.append( "." );
            throw new TableFormatException( sbuf.toString() );
        }

        /* Prepare a query for the actual data. */
        Map<String,String> dataParams = new LinkedHashMap<>();
        dataParams.put( datasetParamName, dataset );
        dataParams.putAll( requestParams );
        for ( HapiVersion v : HapiVersion.getStandardVersions() ) {
            dataParams.remove( v.getStartRequestParam() );
            dataParams.remove( v.getStopRequestParam() );
        }
        dataParams.put( startParamName, start );
        dataParams.put( stopParamName, stop );

        /* See if a serialization format has been explicitly requested. */
        boolean includeHeader = "header".equals( dataParams.get( "include" ) );
        final String format;
        if ( dataParams.containsKey( "format" ) ) {
            format = dataParams.get( "format" );
        }
        else {
            format = supportsBinary ? "binary" : "csv";
            dataParams.put( "format", format );
        }
        URL dataUrl = service.createQuery( HapiEndpoint.DATA, dataParams );

        /* Prepare a URL that will recreate the table without an earlier
         * metadata request. */
        Map<String,String> standaloneDataParams =
            new LinkedHashMap<>( dataParams );
        standaloneDataParams.put( "include", "header" );
        URL standaloneUrl =
            service.createQuery( HapiEndpoint.DATA, standaloneDataParams );

        /* Prepare a row sequence supplier based on the data query. */
        HapiTableReader rdr = new HapiTableReader( infoHdr.getParameters() );
        final boolean[] overflowFlag = new boolean[ 1 ];
        IOConsumer<String> limitCallback = msg -> {
            overflowFlag[ 0 ] = true;
            if ( failOnLimit ) {
                throw new IOException( msg );
            }
            else {
                logger_.warning( msg + " - table truncated" );
            }
        };
        final IOSupplier<RowSequence> rseqSupplier = () -> {
            InputStream in =
                new BufferedInputStream( service
                                        .openChunkedStream( dataUrl, chunkLimit,
                                                            limitCallback ) );
            return includeHeader
                ? rdr.createRowSequenceUsingHeader( in )
                : rdr.createRowSequence( in, (Byte) null, format );
        };

        /* Create and return the table. */
        StarTable table = rdr.createStarTable( rseqSupplier );
        if ( overflowFlag[ 0 ] ) {
            table.setParameter( new DescribedValue( Tables.QUERY_STATUS_INFO,
                                                    "OVERFLOW" ) );
        }
        table.setURL( standaloneUrl );
        table.setName( dataset + "-" + start.replaceFirst( "T.*", "" ) );
        return table;
    }
}
