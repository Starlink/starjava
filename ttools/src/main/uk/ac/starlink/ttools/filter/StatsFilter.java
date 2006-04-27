package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.MapGroup;

/**
 * Filter which can calculate statistical quantities.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class StatsFilter extends BasicFilter {

    /*
     * Metadata for calculated quantities.
     */
    private static final ValueInfo NGOOD_INFO;
    private static final ValueInfo NBAD_INFO;
    private static final ValueInfo MEAN_INFO;
    private static final ValueInfo STDEV_INFO;
    private static final ValueInfo VARIANCE_INFO;
    private static final ValueInfo MIN_INFO;
    private static final ValueInfo MAX_INFO;
    private static final ValueInfo SUM_INFO;
    private static final ValueInfo MINPOS_INFO;
    private static final ValueInfo MAXPOS_INFO;

    /** All known statistical quantities. */
    private static final ValueInfo[] KNOWN_INFOS = new ValueInfo[] {
        NGOOD_INFO = new DefaultValueInfo( "NGood", Number.class,
                                           "Number of non-blank cells" ),
        NBAD_INFO = new DefaultValueInfo( "NBad", Number.class,
                                          "Number of blank cells" ),
        MEAN_INFO = new DefaultValueInfo( "Mean", Double.class, 
                                          "Average" ),
        STDEV_INFO = new DefaultValueInfo( "StDev", Double.class,
                                           "Standard deviation" ),
        VARIANCE_INFO = new DefaultValueInfo( "Variance", Double.class,
                                              "Variance" ),
        MIN_INFO = new DefaultValueInfo( "Minimum", Number.class,
                                         "Numeric minimum" ),
        MAX_INFO = new DefaultValueInfo( "Maximum", Number.class,
                                         "Numeric maximum" ),
        SUM_INFO = new DefaultValueInfo( "Sum", Double.class,
                                         "Sum of values" ),
        MINPOS_INFO = new DefaultValueInfo( "MinPos", Long.class, 
                                            "Row index of numeric minimum" ),
        MAXPOS_INFO = new DefaultValueInfo( "MaxPos", Long.class,
                                            "Row index of numeric maximum" ),
    };

    /** All known per-column quantities (statistical and metadata). */
    private static final ValueInfo[] ALL_KNOWN_INFOS;
    static {
        List known = new ArrayList();
        known.addAll( Arrays.asList( MetadataFilter.KNOWN_INFOS ) );
        known.addAll( Arrays.asList( KNOWN_INFOS ) );
        ALL_KNOWN_INFOS = (ValueInfo[]) known.toArray( new ValueInfo[ 0 ] );
    }

    /** Quantities calculated by default. */
    private static final ValueInfo[] DEFAULT_INFOS = new ValueInfo[] {
        MetadataFilter.NAME_INFO,
        MEAN_INFO,
        STDEV_INFO,
        MIN_INFO,
        MAX_INFO,
        NGOOD_INFO,
    };

    /**
     * Constructor.
     */
    public StatsFilter() {
        super( "stats", "[<item> ...]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "Calculates statistics on the data in the table.",
            "This filter turns the table sideways, so that each row",
            "of the output corresponds to a column of the input.",
            "The columns of the output table contain statistical items",
            "such as mean, standard deviation etc corresponding to each",
            "column of the input table.",
            "</p><p>By default the output table contains columns for the",
            "items " + MetadataFilter.listInfos( DEFAULT_INFOS ) + ".",
            "The output may be customised however by supplying one or more",
            "<code>&lt;item&gt;</code> headings.  These may be selected",
            "from the list " + MetadataFilter.listInfos( KNOWN_INFOS ) + ".",
            "</p><p>Any parameters of the input table are propagated",
            "to the output one.",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        final ValueInfo[] colInfos;
        if ( argIt.hasNext() ) {
            Map infoMap = new HashMap();
            for ( int i = 0; i < ALL_KNOWN_INFOS.length; i++ ) {
                ValueInfo info = ALL_KNOWN_INFOS[ i ];
                infoMap.put( info.getName().toLowerCase(), info );
            }
            List infoList = new ArrayList();
            while ( argIt.hasNext() ) {
                String name = (String) argIt.next();
                argIt.remove();
                String lname = name.toLowerCase();
                if ( infoMap.containsKey( lname ) ) {
                    infoList.add( (ValueInfo) infoMap.get( lname ) );
                }
                else {
                    throw new ArgException( "Unknown quantity " + name + "; " 
                        + "must be one of " 
                        + MetadataFilter.listInfos( ALL_KNOWN_INFOS ) );
                }
            }
            colInfos = (ValueInfo[]) infoList.toArray( new ValueInfo[ 0 ] );
        }
        else {
            colInfos = DEFAULT_INFOS;
        }
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                MapGroup group = statsMapGroup( base, colInfos );
                group.setKnownKeys( Arrays.asList( colInfos ) );
                AbstractStarTable table = new ValueInfoMapGroupTable( group );
                table.setParameters( base.getParameters() );
                return table;
            }
        };
    }

    /**
     * Constructs a MapGroup containing statistical information about 
     * a given table.
     *
     * @param   table   input table
     * @return  mapgroup containing column statistics
     */
    private static MapGroup statsMapGroup( StarTable table, ValueInfo[] infos )
            throws IOException {

        /* Prepare statistical accumulators for each column of the table. */
        int ncol = table.getColumnCount();
        UnivariateStats[] colStats = new UnivariateStats[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            Class clazz = table.getColumnInfo( icol ).getContentClass();
            colStats[ icol ] = UnivariateStats.createStats( clazz );
        }

        /* Populate them with the the data read from the table. */
        RowSequence rseq = table.getRowSequence();
        long nrow = 0L;
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    colStats[ icol ].acceptDatum( row[ icol ] );
                }
                nrow++;
            }

            /* Get a MapGroup representing column metadata (the option is 
             * provided to output this alongside the statistical results). */
            MapGroup group = MetadataFilter.metadataMapGroup( table );

            /* Augment the metadata with the relevant statistical results for
             * each column. */
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Gather statistical results. */
                UnivariateStats stats = colStats[ icol ];
                long count = stats.getCount();
                double dcount = (double) count;
                double sum = stats.getSum();
                double sum2 = stats.getSum2();
                double mean = sum / dcount;
                double variance = ( sum2 - sum * sum / dcount ) / dcount;
                Number min = stats.getMinimum();
                Number max = stats.getMaximum();

                /* Add the the column's info->values map. */
                Map map = (Map) group.getMaps().get( icol );
                map.put( NGOOD_INFO, new Long( count ) );
                map.put( NBAD_INFO, new Long( nrow - count ) );
                if ( isFinite( mean ) ) {
                    map.put( MEAN_INFO, new Double( mean ) );
                }
                if ( isFinite( variance ) ) {
                    map.put( VARIANCE_INFO, new Double( variance ) );
                    map.put( STDEV_INFO, new Double( Math.sqrt( variance ) ) );
                }
                if ( min instanceof Number &&
                     isFinite( ((Number) min).doubleValue() ) ) {
                    map.put( MIN_INFO, min );
                    map.put( MINPOS_INFO, new Long( stats.getMinPos() + 1 ) );
                }
                if ( max instanceof Number &&
                     isFinite( ((Number) max).doubleValue() ) ) {
                    map.put( MAX_INFO, max );
                    map.put( MAXPOS_INFO, new Long( stats.getMaxPos() + 1 ) );
                }
            }
            return group;
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Tests whether a value is finite (not infinite or NaN).
     *
     * @param   val  number to test
     * @return  true iff <tt>val</tt> is finite
     */
    private final static boolean isFinite( double val ) {
        return val > - Double.MAX_VALUE
            && val < + Double.MAX_VALUE;
    }
}
