package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
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
    private static final ValueInfo SKEW_INFO;
    private static final ValueInfo KURT_INFO;
    private static final ValueInfo MIN_INFO;
    private static final ValueInfo MAX_INFO;
    private static final ValueInfo SUM_INFO;
    private static final ValueInfo MINPOS_INFO;
    private static final ValueInfo MAXPOS_INFO;
    private static final ValueInfo CARDINALITY_INFO;

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
        SKEW_INFO = new DefaultValueInfo( "Skew", Double.class,
                                          "Gamma 1 skewness measure" ),
        KURT_INFO = new DefaultValueInfo( "Kurtosis", Double.class,
                                          "Gamma 2 peakedness measure" ),
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
        CARDINALITY_INFO = new DefaultValueInfo( "Cardinality", Integer.class,
                                    "Number of distinct values in column" ),
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

    /** Maximum value for cardinality counters. */
    private static final int MAX_CARDINALITY = 100;

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

        /* Work out if we need to calculate cardinalities. */
        boolean card = Arrays.asList( infos ).contains( CARDINALITY_INFO );

        /* Prepare statistical accumulators for each column of the table. */
        int ncol = table.getColumnCount();
        UnivariateStats[] colStats = new UnivariateStats[ ncol ];
        CardinalityChecker[] cardCheckers =
            card ? new CardinalityChecker[ ncol ] : null;
        for ( int icol = 0; icol < ncol; icol++ ) {
            Class clazz = table.getColumnInfo( icol ).getContentClass();
            colStats[ icol ] = UnivariateStats.createStats( clazz );
            if ( card ) {
                cardCheckers[ icol ] =
                    new CardinalityChecker( MAX_CARDINALITY );
            }
        }

        /* Populate them with the the data read from the table. */
        RowSequence rseq = table.getRowSequence();
        long nrow = 0L;
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    colStats[ icol ].acceptDatum( row[ icol ] );
                    if ( card ) {
                        cardCheckers[ icol ].acceptDatum( row[ icol ] );
                    }
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
                double sum0 = dcount;
                double sum1 = stats.getSum();
                double sum2 = stats.getSum2();
                double sum3 = stats.getSum3();
                double sum4 = stats.getSum4();
                double mean = sum1 / dcount;
                double nvar = ( sum2 - sum1 * sum1 / dcount );
                double variance = nvar / dcount;
              
                double skew = Math.sqrt( dcount ) / Math.pow( nvar, 1.5 )
                            * ( + 1 * sum3
                                - 3 * mean * sum2
                                + 3 * mean * mean * sum1
                                - 1 * mean * mean * mean * sum0 );
                double kurtosis = ( dcount / ( nvar * nvar ) )
                                * ( + 1 * sum4 
                                    - 4 * mean * sum3 
                                    + 6 * mean * mean * sum2
                                    - 4 * mean * mean * mean * sum1
                                    + 1 * mean * mean * mean * mean * sum0 )
                                - 3.0;
                Number min = stats.getMinimum();
                Number max = stats.getMaximum();

                /* Add statistical quantities to the column's
                 * info->values map. */
                Map map = (Map) group.getMaps().get( icol );
                map.put( NGOOD_INFO, new Long( count ) );
                map.put( NBAD_INFO, new Long( nrow - count ) );
                map.put( SUM_INFO, new Double( sum1 ) );
                if ( isFinite( mean ) ) {
                    map.put( MEAN_INFO, new Double( mean ) );
                }
                if ( isFinite( variance ) ) {
                    map.put( STDEV_INFO, new Double( Math.sqrt( variance ) ) );
                    map.put( VARIANCE_INFO, new Double( variance ) );
                    map.put( SKEW_INFO, new Double( skew ) );
                    map.put( KURT_INFO, new Double( kurtosis ) );
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
                if ( card ) {
                    int ncard = cardCheckers[ icol ].getCardinality();
                    if ( ncard > 0 ) {
                        map.put( CARDINALITY_INFO, new Integer( ncard ) );
                    }
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

    /**
     * Counts distinct values which appear in a column.
     * The cardinality is the number of distinct values. 
     * Null isn't counted.
     */
    private static class CardinalityChecker {

        final int maxCard_;
        Set items_ = new HashSet();

        /**
         * Constructor.
         *
         * @param   maxCard  maximum cardinality that this object will count;
         *          for reasons of performance (memory use) it will give up
         *          counting if there appear to be more values than this
         */
        CardinalityChecker( int maxCard ) {
            maxCard_ = maxCard;
        }

        /**
         * Submits a value for counting.
         *
         * @param  obj  value
         */
        void acceptDatum( Object obj ) {
            if ( ! Tables.isBlank( obj ) ) {
                if ( items_ != null ) {
                    items_.add( obj );
                }
                if ( items_.size() > maxCard_ ) {
                    items_ = null;
                }
            }
        }

        /**
         * Returns the cardinality of the data items submitted.
         * If the cardinality is greater than <code>maxCard</code>,
         * -1 will be returned.
         *
         * @return   cardinality, or -1
         */
        int getCardinality() {
            return items_ == null ? -1
                                  : items_.size();
        }
    }
}
