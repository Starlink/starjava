package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapServiceFinder implementation that uses the GloTS schema
 * maintained (at time of writing) at the GAVO Data Center.
 * GloTS is a non-standard registry containing metadata gathered
 * by hook or by crook from all known registered TAP services.
 *
 * <p>It is not very respectable to use this resource;
 * the correct way to find out this kind of thing is using the
 * standard interfaces of the IVOA Registry.
 * However, at time of writing (June 2015) the Registry does not
 * contain sufficiently detailed metadata (in particular, lists of
 * table names and descriptions for each service) to do the job,
 * while GloTS does.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public class GlotsServiceFinder implements TapServiceFinder {

    private final TapService glotsService_;
    private final ContentCoding coding_;
    private final AdqlSyntax syntax_;
    public static final String GAVO_DC_TAP_URL = "http://reg.g-vo.org/tap";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a default instance.
     */
    public GlotsServiceFinder() {
        this( TapServices.createTapService( GAVO_DC_TAP_URL, TapVersion.V11 ),
              ContentCoding.GZIP );
    }

    /**
     * Constructs an instance with custom configuration.
     *
     * @param  glotsService   TAP service description for a service
     *                        containing GloTS tables
     * @param  coding   controls HTTP-level compression during TAP queries
     */
    public GlotsServiceFinder( TapService glotsService, ContentCoding coding ) {
        glotsService_ = glotsService;
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
            URL = "accessurl",
            NTABLE = "ntable",
        };
        String adql = new StringBuffer()
            .append( "SELECT" )
            .append( commaJoin( colNames ) )
            .append( " FROM" )
            .append( " (SELECT" )
            .append( commaJoin( new String[] { IVOID, URL } ) )
            .append( ", COUNT(" )
            .append( IVOID )
            .append( ") AS " )
            .append( NTABLE )
            .append( " FROM glots.services" )
            .append( " JOIN glots.tables USING (" )
            .append( IVOID )
            .append( ")" )
            .append( " GROUP BY " )
            .append( IVOID )
            .append( ") AS t" )
            .append( " JOIN rr.resource USING (" )
            .append( IVOID )
            .append( ")" )
            .toString();
        logger_.info( "TAP Query: " + adql );
        TapQuery tq = new TapQuery( glotsService_, adql, null );
        final List<Service> serviceList = new ArrayList<Service>();
        try { 
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    Map<String,String> valueMap = toValueMap( colNames, row );
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
                assert targ.getGlotsTablesCol() != null;
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
            SERVICE_ID = "ivoid",
            NAME = "table_name",
            DESCRIP = "table_desc"
        };
        StringBuffer sbuf = new StringBuffer()
            .append( "SELECT" )
            .append( commaJoin( colNames ) )
            .append( " FROM glots.tables" )
            .append( " WHERE" );
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
        logger_.info( "TAP Query: " + sbuf );
        TapQuery tq = new TapQuery( glotsService_, sbuf.toString(), null );
        final List<Table> tableList = new ArrayList<Table>();
        try {
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    Map<String,String> valueMap = toValueMap( colNames, row );
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
        String glotsName = target.getGlotsTablesCol();
        if ( target.isWords() ) {
            return new StringBuffer()
                .append( "1=ivo_hasWord(" )
                .append( glotsName )
                .append( ", " )
                .append( syntax_.characterLiteral( keyword ) )
                .append( ")" )
                .toString();
        }
        else {
            return new StringBuffer()
                .append( "1=ivo_nocasematch(" )
                .append( glotsName )
                .append( ", " )
                .append( syntax_.characterLiteral( "%" + keyword + "%" ) )
                .append( ")" )
                .toString();
        }
    }

    /**
     * Utility method to join an array of words together with commas
     * between them.
     *
     * @param  words  words to join
     * @return   concatenated string
     */
    static String commaJoin( String[] words ) {
        StringBuffer sbuf = new StringBuffer();
        for ( String word : words ) {
            if ( sbuf.length() > 0 ) {
                sbuf.append( "," );
            }
            sbuf.append( " " );
            sbuf.append( word );
        }
        return sbuf.toString();
    }

    /**
     * Turns matched arrays of keys and values in to a key-&gt;value map.
     * The input arrays should have the same length N, and the output map
     * will (in absence of duplicated keys) have N entries.
     *
     * @param   keys   key list
     * @param   values   value list
     * @return  map
     */
    static Map<String,String> toValueMap( String[] keys, Object[] values ) {
        int n = keys.length;
        Map<String,String> map = new HashMap<String,String>( n );
        for ( int i = 0; i < n; i++ ) {
            String key = keys[ i ];
            Object val = values[ i ];
            map.put( key, val == null ? null : val.toString() );
        }
        return map;
    }
}
