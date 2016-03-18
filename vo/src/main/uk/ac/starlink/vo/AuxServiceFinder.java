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
 * TapServiceFinder implementation that uses the IVOA registry
 * along with "auxiliary" labelling of tableset resources.
 * This is (an early version of) the scheme proposed by
 * Markus Demleitner's "Discovering Data Collections within Services"
 * IVOA Note.
 * 
 * @author   Mark Taylor
 * @since    6 Aug 2015
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
        String adql = new StringBuffer()
            .append( "SELECT" )
            .append( GlotsServiceFinder.commaJoin( colNames ) )
            .append( " FROM" )
            .append( " (SELECT" )
            .append( GlotsServiceFinder
                    .commaJoin( new String[] { IVOID, NAME, TITLE,
                                               DESCRIP, URL } ) )
            .append( " FROM rr.resource" )
            .append( " NATURAL JOIN rr.capability" )
            .append( " NATURAL JOIN rr.interface" )
            .append( " WHERE standard_id = 'ivo://ivoa.net/std/tap'" )
            .append( " AND intf_type = 'vs:paramhttp'" )
            .append( ") AS serv" )
            .append( " JOIN" )
            .append( " (SELECT service_id, COUNT(*) AS ntable" )
            .append( " FROM" )
            .append( " (SELECT DISTINCT" )
            .append( " related_id AS service_id, ivoid, table_index" )
            .append( " FROM rr.relationship" )
            .append( " NATURAL JOIN rr.capability" )
            .append( " NATURAL JOIN rr.interface" )
            .append( " NATURAL JOIN rr.res_table" )
            .append( " WHERE relationship_type = 'served-by'" )
            .append( " AND standard_id = 'ivo://ivoa.net/std/tap#aux'" )
            .append( " AND intf_type = 'vs:paramhttp'" )
            .append( ") AS s" )
            .append( " GROUP BY service_id" )
            .append( ") AS aux" )
            .append( " ON aux.service_id = serv.ivoid" )
            .toString();
        logger_.info( "TAP Query: " + adql );
        TapQuery tq = new TapQuery( regtapEndpointSet_, adql, null );
        final List<Service> serviceList = new ArrayList<Service>();
        try {
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    Map<String,String> valueMap =
                        GlotsServiceFinder.toValueMap( colNames, row );
                    final String id = valueMap.get( IVOID );
                    final String name = valueMap.get( NAME );
                    final String title = valueMap.get( TITLE );
                    final String descrip = valueMap.get( DESCRIP );
                    final String url = valueMap.get( URL );
                    String ntableStr = valueMap.get( NTABLE );
                    int nt = -1;
                    if ( ntableStr != null ) {
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
        final String SERVICE_ID; 
        final String NAME;
        final String DESCRIP;
        final String[] colNames = {
            SERVICE_ID = "related_id",
            NAME = "table_name",
            DESCRIP = "table_description",
        };

        /* This query doesn't bother checking that the related_id resource
         * is in fact a TAP service (standard_id='ivo://ivoa.net/std/tap').
         * It could do, but it's likely that most/all services it recovers
         * will be, and if any are recoved that are not, they will be
         * ignored by later processing anyway, since the results of this
         * query get matched up with the results of the readAllServices
         * query, which are so constrained, anyway. */
        StringBuffer sbuf = new StringBuffer()
            .append( "SELECT" )
            .append( " DISTINCT" )
            .append( GlotsServiceFinder.commaJoin( colNames ) )
            .append( " FROM rr.relationship" )
            .append( " NATURAL JOIN rr.capability" )
            .append( " NATURAL JOIN rr.interface" )
            .append( " NATURAL JOIN rr.res_table" )
            .append( " WHERE relationship_type = 'served-by'" )
            .append( " AND standard_id = 'ivo://ivoa.net/std/tap#aux'" )
            .append( " AND intf_type = 'vs:paramhttp'" )
            .append( " AND" )
            .append( " (" );
        for ( int iw = 0; iw < words.length; iw++ ) {
            String word = words[ iw ];
            if ( iw > 0 ) {
                sbuf.append( isAnd ? " AND" : " OR" );
            }
            sbuf.append( " (" );
            for ( int it = 0; it < tTargets.size(); it++ ) {
                if ( it > 0 ) {
                    sbuf.append( " OR " );
                }
                sbuf.append( getAdqlTest( word, tTargets.get( it ) ) );
            }
            sbuf.append( ")" );
        }
        sbuf.append( " )" );
        logger_.info( "TAP Query: " + sbuf );
        TapQuery tq = new TapQuery( regtapEndpointSet_, sbuf.toString(), null );
        final List<Table> tableList = new ArrayList<Table>();
        try {
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
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
}
