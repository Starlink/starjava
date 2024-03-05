package uk.ac.starlink.vo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapServiceFinder implementation that uses an IVOA registry compliant
 * with RegTAP v1.2 to discover services and tables.
 * In particular the new <code>rr.tap_table</code> table introduced at
 * RegTAP v1.2 is used.
 *
 * @author   Mark Taylor
 * @since    28 Feb 2024
 */
public class Regtap12ServiceFinder implements TapServiceFinder {

    private final TapService regtapService_;
    private final ContentCoding coding_;
    private final AdqlSyntax syntax_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a default instance.
     */
    public Regtap12ServiceFinder() {
        this( TapServices.getRegTap12Service(), ContentCoding.GZIP );
    }

    /**
     * Constructs an instance with custom configuration.
     *
     * @param  regtapService   RegTAP v1.2 service description
     * @param  coding  controls HTTP-level compression during TAP queries
     */
    public Regtap12ServiceFinder( TapService regtapService,
                                  ContentCoding coding ) {
        regtapService_ = regtapService;
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
         * along with the number of tables in each one.
         * Note this figure may end up being incorrect if registry bugs
         * mean that rr.tap_table contains duplicate (svcid, table_name)
         * values (forbidden by RegTAP 1.2 sec 8.18). */
        String adql = String.join( "\n", new String[] {
            "SELECT " + String.join( ", ", colNames ),
            "FROM (",
            "   SELECT svcid AS ivoid, COUNT(*) AS ntable",
            "   FROM rr.tap_table",
            "   GROUP BY svcid",
            ") AS tt",
            "NATURAL JOIN rr.resource",
            "NATURAL JOIN rr.capability",
            "NATURAL JOIN rr.interface",
            "WHERE standard_id = 'ivo://ivoa.net/std/tap'",
            "  AND intf_type = 'vs:paramhttp'",
            "ORDER BY ntable DESC",
        } );

        /* Execute this query, turning the result into a list of Service
         * objects. */
        Function<Object[],Service> serviceReader = row -> {
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
            return new Service() {
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
            };
        };
        return readQuery( adql, serviceReader )
              .toArray( new Service[ 0 ] );
    }

    public Table[] readSelectedTables( Constraint constraint )
            throws IOException {

        /* Make sense of submitted constraints. */
        String[] words = constraint.getKeywords();
        boolean isAnd = constraint.isAndKeywords();
        List<Target> tTargets = new ArrayList<Target>();
        for ( Target targ : constraint.getTargets() ) {
            if ( ! targ.isServiceMeta() ) {
                assert targ.getRrTapTablesCol() != null;
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
            SERVICE_ID = "svcid",
            NAME = "table_name",
            DESCRIP = "table_description",
        };

        /* Assemble the ADQL query that returns tables of interest
         * along with the ivoids of their corresponding TAP services. */
        List<String> wheres = new ArrayList<>();
        for ( String word : words ) {
            wheres.add( tTargets.stream()
                       .map( targ -> getAdqlTest( word, targ ) )
                       .collect( Collectors.joining( " OR " ) ) );
        }

        /* RegTAP 1.2 sec 8.18 says of rr.tap_table "there cannot be
         * two rows in the view having the same (svcid, table_name)".
         * However at time of writing, publishing registry bugs
         * can result in violations of this rule.
         * We could work round this by adding a DISTINCT here,
         * but Markus has lobbied that this would be a bad idea,
         * since service providers are less likely to fix things if they
         * look OK in topcat, so turn this fix off for now. */
        boolean fixDuplicates = false;
        StringBuffer sbuf = new StringBuffer()
            .append( "SELECT " );
        if ( fixDuplicates ) {
            sbuf.append( "DISTINCT " );
        }
        sbuf.append( String.join( ", ", colNames ) )
            .append( "\nFROM rr.tap_table" );
        for ( int iw = 0; iw < wheres.size(); iw++ ) {
            sbuf.append( "\n" );
            if ( iw == 0 ) {
                sbuf.append( "WHERE" );
            }
            else {
                sbuf.append( isAnd ? "  AND" : "   OR" );
            }
            sbuf.append( " (" )
                .append( wheres.get( iw ) )
                .append( ")" );
        }
        String adql = sbuf.toString();

        /* Execute the TAP query, turning the result into an array of
         * Table objects. */
        Function<Object[],Table> tableReader = row -> {
            Map<String,String> valueMap =
                GlotsServiceFinder.toValueMap( colNames, row );
            String serviceId = valueMap.get( SERVICE_ID );
            String name = valueMap.get( NAME );
            String descrip = valueMap.get( DESCRIP );
            return new Table() {
                public String getName() {
                    return name;
                }
                public String getDescription() {
                    return descrip;
                }
                public String getServiceId() {
                    return serviceId;
                }
            };
        };
        return readQuery( adql, tableReader )
              .toArray( new Table[ 0 ] );
    }

    /**
     * Executes a given ADQL query and returns the results as a list of
     * objects, one for each row in the result.
     *
     * @param  adql  query text
     * @param  rowReader   converts a row of the query result into an object
     *                     for the output list
     * @return  output list
     */
    private <T> List<T> readQuery( String adql, Function<Object[],T> rowReader )
            throws IOException {
        logger_.info( "RegTAP 1.2 query: " + adql.replaceAll( "\\s+", " " ) );
        logger_.config( "RegTAP 1.2 query:\n" + adql );
        TapQuery tq = new TapQuery( regtapService_, adql, null );
        final List<T> resultList = new ArrayList<>();
        try {
            final int[] nrows = new int[ 1 ];
            boolean isTrunc = tq.executeSync( new TableSink() {
                public void acceptRow( Object[] row ) {
                    nrows[ 0 ]++;
                    T result = rowReader.apply( row );
                    if ( result != null ) {
                        resultList.add( result );
                    }
                }
                public void acceptMetadata( StarTable meta ) {
                }
                public void endRows() {
                }
            }, coding_ );
            int nrow = nrows[ 0 ];
            if ( isTrunc ) {
                logger_.warning( "Result truncated at " + nrow + " rows" );
            }
            else {
                logger_.info( "Received " + nrow + " rows" );
            }
        }
        catch ( SAXException e ) {
            throw new IOException( "Error parsing VOTable result: " + e, e );
        }
        return resultList;
    }

    /**
     * Returns a boolean ADQL expression that matches a search term
     * against a search target.
     *
     * @param  keyword   search term
     * @param  target   metadata item that must match keyword
     * @return   ADQL text
     */
    private String getAdqlTest( String keyword, Target target ) {
        String colName = target.getRrTapTablesCol();
        if ( target.isWords() ) {
            return new StringBuffer()
                  .append( "1=ivo_hasword(" )
                  .append( colName )
                  .append( ", " )
                  .append( syntax_.characterLiteral( keyword ) )
                  .append( ")" )
                  .toString();
        }
        else {
            return new StringBuffer()
                  .append( colName )
                  .append( " ILIKE " )
                  .append( syntax_.characterLiteral( "%" + keyword + "%" ) )
                  .toString();
        }
    }
}
