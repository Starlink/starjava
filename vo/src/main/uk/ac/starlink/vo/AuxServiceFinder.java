package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapServiceFinder implementation that uses the IVOA Registry
 * along with an "auxiliary" labelling of tableset resources.
 * This is a version of the scheme proposed by Markus Demleitner's
 * IVOA Note <em>"Discovering Data Collections Within Services"</em>,
 * version 1.1.
 *
 * @author   Mark Taylor
 * @since    6 Aug 2015
 * @see  <a href="http://www.ivoa.net/documents/Notes/DataCollect/"
 *          >Discovering Data Collections Within Services</a>
 */
public class AuxServiceFinder implements TapServiceFinder {

    private final EndpointSet regtapEndpointSet_;
    private final ContentCoding coding_;
    private final AdqlSyntax syntax_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a default instance.
     */
    public AuxServiceFinder() {
        this( Endpoints.getRegTapEndpointSet(), ContentCoding.GZIP );
    }

    /**
     * Constructs an instance with custom configuration.
     *
     * @param  regtapEndpointSet  TAP endpoints for RegTAP service
     * @param  coding  controls HTTP-level compression during TAP queries
     */
    public AuxServiceFinder( EndpointSet regtapEndpointSet,
                             ContentCoding coding ) {
        regtapEndpointSet_ = regtapEndpointSet;
        coding_ = coding;
        syntax_ = AdqlSyntax.getInstance();
    }

    public Service[] readAllServices() throws IOException {

        /* Identify columns to be extracted from RegTAP query. */
        final String IVOID;
        final String NAME;
        final String TITLE;
        final String DESCRIP;
        final String URL;
        final String NTABLE;
        final String[] colNames = {
            IVOID = "ivoid",
            NAME = "short_name",
            TITLE = "res_title",
            DESCRIP = "res_description",
            URL = "access_url",
            NTABLE = "ntable",
        };

        /* Assemble a query that returns the ivoid of all known TAP services
         * (easy) along with the number of tables in each one (harder).
         * The first subquery identifies all the registered TAP services
         * along with their service metadata.
         * The second subquery counts the tables associated with each
         * TAP access_url.  The two are then joined on access_url.
         * access_url is not defined or documented as a key in this context,
         * but it is expected to be in practice unique per TAP service.
         * A LEFT OUTER JOIN is used so that TAP services for which
         * no auxiliary tables are registered still show up in the results;
         * they end up with a null value for table count.
         * The second subquery has to use a further nested subquery so
         * we can use a SELECT DISTINCT to avoid getting multiple rows
         * per table (which may be associated with multiple
         * capabilities/interfaces). */
        String adql = new StringBuffer()
            .append( "SELECT" )
            .append( "\n  " )
            .append( GlotsServiceFinder.commaJoin( colNames ) )
            .append( "\nFROM (" )
            .append( "\n   SELECT DISTINCT" )
            .append( "\n     " )
            .append( GlotsServiceFinder.commaJoin( new String[] {
                         IVOID, NAME, TITLE, DESCRIP, URL,
                     } ) )
            .append( "\n   FROM rr.resource" )
            .append( "\n   NATURAL JOIN rr.capability" )
            .append( "\n   NATURAL JOIN rr.interface" )
            .append( "\n   WHERE" )
            .append( "\n      standard_id = 'ivo://ivoa.net/std/tap'" )
            .append( "\n) AS serv" )
            .append( "\nLEFT OUTER JOIN (" )
            .append( "\n   SELECT" )
            .append( "\n      access_url, COUNT(*) AS ntable" )
            .append( "\n   FROM (" )
            .append( "\n      SELECT DISTINCT" )
            .append( "\n         table_name, access_url" )
            .append( "\n      FROM rr.res_table" )
            .append( "\n      NATURAL JOIN rr.capability" )
            .append( "\n      NATURAL JOIN rr.interface" )
            .append( "\n      WHERE" )
            .append( "\n        standard_id LIKE 'ivo://ivoa.net/std/tap%'" )
            .append( "\n   ) AS caps" )
            .append( "\n   GROUP BY access_url" )
            .append( "\n) AS tcount" )
            .append( "\nUSING (access_url)" )
            .append( "\nORDER BY ntable DESC" )
            .toString();

        /* Execute this query and turn the result into a list of Service
         * objects. */
        logAdql( adql );
        TapQuery tq = new TapQuery( regtapEndpointSet_, adql, null );
        final List<Service> serviceList = new ArrayList<Service>();
        try {
            final int[] nrow = new int[ 1 ];
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    nrow[ 0 ]++;
                    Map<String,String> valueMap =
                        GlotsServiceFinder.toValueMap( colNames, row );
                    final String id = valueMap.get( IVOID );
                    final String name = valueMap.get( NAME );
                    final String title = valueMap.get( TITLE );
                    final String descrip = valueMap.get( DESCRIP );
                    final String url = valueMap.get( URL );
                    String ntableStr = valueMap.get( NTABLE );
                    int nt = -1;
                    if ( ntableStr != null && ntableStr.matches( "[0-9]+" ) ) {
                        try {
                            nt = Integer.parseInt( ntableStr );
                        }
                        catch ( RuntimeException e ) {
                            // never mind
                        }
                    }
                    final int ntable = nt;
                    serviceList.add( new Service() {
                        public String getId() {
                            return id;
                        }
                        public String getName() {
                            return name;
                        }
                        public String getTitle() {
                            return title;
                        }
                        public String getDescription() {
                            return descrip;
                        }
                        public String getServiceUrl() {
                            return url;
                        }
                        public int getTableCount() {
                            return ntable;
                        }
                    } );
                }
                public void acceptMetadata( StarTable meta ) {
                }
                public void endRows() {
                }
            }, coding_ );
            logRows( isTrunc, nrow[ 0 ] );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Error parsing VOTable result: " + e )
                 .initCause( e );
        }
        return serviceList.toArray( new Service[ 0 ] );
    }

    public Table[] readSelectedTables( Constraint constraint )
            throws IOException {

        /* Make sense of submitted constraints. */
        String[] words = constraint.getKeywords();
        boolean isAnd = constraint.isAndKeywords();
        List<Target> tTargets = new ArrayList<Target>();
        for ( Target targ : constraint.getTargets() ) {
            if ( ! targ.isServiceMeta() ) {
                assert targ.getRrTablesCol() != null;
                tTargets.add( targ );
            }
        }
        if ( tTargets.size() == 0 ) {
            return new Table[ 0 ];
        }

        /* Identify columns to be extracted from RegTAP query. */
        final String SERVICE_ID;
        final String NAME;
        final String DESCRIP;
        final String[] colNames = {
            SERVICE_ID = "ivoid",
            NAME = "table_name",
            DESCRIP = "table_description",
        };

        /* Assemble the ADQL query that returns tables of interest
         * along with the ivoids of their corresponding TAP services.
         * The main part of this query (the first subquery) selects the
         * tables from rr.res_table that match the stated metadata
         * constraints, and that also correspond to a TAP service.
         * The standardId matching will match either
         * "ivo://ivoa.net/std/tap" (resource is a TAP service) or
         * "ivo://ivoa.net/std/tap#aux" (resource points to a TAP service).
         * The second subquery acquires the ivoid for these table resources
         * by joining on access_url.
         * access_url is not defined or documented as a key in this context,
         * but it is expected to be in practice unique per TAP service. */
        StringBuffer sbuf = new StringBuffer()
            .append( "SELECT" )
            .append( "\n  " ) 
            .append( GlotsServiceFinder.commaJoin( colNames ) )
            .append( "\nFROM (" )
            .append( "\n   SELECT DISTINCT" )
            .append( "\n      table_name, table_description, access_url" )
            .append( "\n   FROM rr.res_table" )
            .append( "\n   NATURAL JOIN rr.capability" )
            .append( "\n   NATURAL JOIN rr.interface" )
            .append( "\n   WHERE standard_id LIKE 'ivo://ivoa.net/std/tap%'" )
            .append( "\n     AND (" );
        for ( int iw = 0; iw < words.length; iw++ ) {
            sbuf.append( "\n      " )
                .append( iw == 0 ? "    "
                                 : ( isAnd ? "AND " : " OR " ) )
                .append( "(" );
            String word = words[ iw ];
            for ( int it = 0; it < tTargets.size(); it++ ) {
                if ( it > 0 ) {
                    sbuf.append( " OR\n           " );
                }
                sbuf.append( getAdqlTest( word, tTargets.get( it ) ) );
            }
            sbuf.append( ")" );
        }
        sbuf.append( "\n     )" )
            .append( "\n) AS tbl" )
            .append( "\nJOIN (" )
            .append( "\n   SELECT DISTINCT" )
            .append( "\n      ivoid, access_url" )
            .append( "\n   FROM rr.resource" )
            .append( "\n   NATURAL JOIN rr.capability" )
            .append( "\n   NATURAL JOIN rr.interface" )
            .append( "\n   WHERE standard_id = 'ivo://ivoa.net/std/tap'" )
            .append( "\n) AS serv" )
            .append( "\nUSING (access_url)" );
        String adql = sbuf.toString();

        /* Execute the TAP query and turn it into a list of Table objects. */ 
        logAdql( adql );
        TapQuery tq = new TapQuery( regtapEndpointSet_, adql, null );
        final List<Table> tableList = new ArrayList<Table>();
        try {
            final int[] nrow = new int[ 1 ];
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    nrow[ 0 ]++;
                    Map<String,String> valueMap =
                        GlotsServiceFinder.toValueMap( colNames, row );
                    final String serviceId = valueMap.get( SERVICE_ID );
                    final String name = valueMap.get( NAME );
                    final String descrip = valueMap.get( DESCRIP );
                    tableList.add( new Table() {
                        public String getServiceId() {
                            return serviceId;
                        }
                        public String getName() {
                            return name;
                        }
                        public String getDescription() {
                            return descrip;
                        }
                    } );
                }
                public void acceptMetadata( StarTable meta ) {
                }
                public void endRows() {
                }
            }, coding_ );
            logRows( isTrunc, nrow[ 0 ] );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Error parsing VOTable result: " + e )
                 .initCause( e );
        }
        return tableList.toArray( new Table[ 0 ] );
    }

    /**
     * Returns an ADQL snippet that matches a search term against a
     * given search target.
     *
     * @param  keyword   search term
     * @param  target   metadata item that must match keyword
     * @return   ADQL text
     */
    private String getAdqlTest( String keyword, Target target ) {
        String auxName = target.getRrTablesCol();
        if ( target.isWords() ) {
            return new StringBuffer()
                .append( "1=ivo_hasWord(" )
                .append( auxName )
                .append( ", " )
                .append( syntax_.characterLiteral( keyword ) )
                .append( ")" )
                .toString();
        }
        else {
            return new StringBuffer()
                .append( "1=ivo_nocasematch(" )
                .append( auxName )
                .append( ", " )
                .append( syntax_.characterLiteral( "%" + keyword + "%" ) )
                .append( ")" )
                .toString();
        }
    }

    /**
     * Reports a multi-line ADQL string through the logging system.
     *
     * @param   adql  aux query
     */
    private void logAdql( String adql ) {
        logger_.info( "Aux RegTAP query: " + adql.replaceAll( "\\s+", " " ) );
        logger_.config( "Aux RegTAP query:\n" + adql );
    }

    /**
     * Reports the result of a TAP query through the logging system.
     *
     * @param  isTrunc  true if result was truncated, faise if complete
     * @param  nrow   number of rows received
     */
    private void logRows( boolean isTrunc, int nrow ) {
        if ( isTrunc ) {
            logger_.warning( "Result truncated at " + nrow + " rows" );
        }
        else {
            logger_.info( "Received " + nrow + " rows" );
        }
    }
}
