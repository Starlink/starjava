package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.util.MapGroup;

/**
 * Filter which can calculate statistical quantities.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class StatsFilter extends BasicFilter {

    /** Maximum value for cardinality counters. */
    private static final int MAX_CARDINALITY = 100;

    /*
     * Metadata for calculated quantities.
     */
    private static final ValueInfo NGOOD_INFO;
    private static final ValueInfo NBAD_INFO;
    private static final ValueInfo MEAN_INFO;
    private static final ValueInfo POPSD_INFO;
    private static final ValueInfo POPVAR_INFO;
    private static final ValueInfo SAMPSD_INFO;
    private static final ValueInfo SAMPVAR_INFO;
    private static final ValueInfo SKEW_INFO;
    private static final ValueInfo KURT_INFO;
    private static final ValueInfo MIN_INFO;
    private static final ValueInfo MAX_INFO;
    private static final ValueInfo SUM_INFO;
    private static final ValueInfo MINPOS_INFO;
    private static final ValueInfo MAXPOS_INFO;
    private static final ValueInfo CARDINALITY_INFO;
    private static final ValueInfo MEDIAN_INFO;
    private static final ValueInfo Q1_INFO;
    private static final ValueInfo Q2_INFO;
    private static final ValueInfo Q3_INFO;

    /** All known statistical quantities. */
    private static final ValueInfo[] KNOWN_INFOS = new ValueInfo[] {
        NGOOD_INFO = new DefaultValueInfo( "NGood", Number.class,
                                           "Number of non-blank cells" ),
        NBAD_INFO = new DefaultValueInfo( "NBad", Number.class,
                                          "Number of blank cells" ),
        MEAN_INFO = new DefaultValueInfo( "Mean", Float.class, 
                                          "Average" ),
        POPSD_INFO = new DefaultValueInfo( "StDev", Float.class,
                                           "Population Standard deviation" ),
        POPVAR_INFO = new DefaultValueInfo( "Variance", Float.class,
                                            "Population Variance" ),
        SAMPSD_INFO = new DefaultValueInfo( "SampStDev", Float.class,
                                            "Sample Standard Deviation" ),
        SAMPVAR_INFO = new DefaultValueInfo( "SampVariance", Float.class,
                                             "Sample Variance" ),
        SKEW_INFO = new DefaultValueInfo( "Skew", Float.class,
                                          "Gamma 1 skewness measure" ),
        KURT_INFO = new DefaultValueInfo( "Kurtosis", Float.class,
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
                           "Number of distinct values in column; " +
                           "values >" + MAX_CARDINALITY + " ignored" ),
        MEDIAN_INFO = new QuantileInfo( 0.5, "Median",
                                        "Middle value in sequence" ),
        Q1_INFO = new QuantileInfo( 0.25, "Quartile1", "First quartile" ),
        Q2_INFO = new QuantileInfo( 0.50, "Quartile2", "Second quartile" ),
        Q3_INFO = new QuantileInfo( 0.75, "Quartile3", "Third quartile" ),
    };

    /** Example Q.* infos for documentation only. */
    private static final ValueInfo[] QEX_INFOS = new ValueInfo[] {
        new DefaultValueInfo( "Q.25", Number.class, "First quartile" ),
        new DefaultValueInfo( "Q.625", Number.class, "Fifth octile" ),
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
        POPSD_INFO,
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
        Collection extras = new ArrayList( Arrays.asList( KNOWN_INFOS ) );
        extras.removeAll( Arrays.asList( DEFAULT_INFOS ) );
        ValueInfo[] extraKnownInfos =
            (ValueInfo[]) extras.toArray( new ValueInfo[ 0 ] );
        return new String[] {
            "<p>Calculates statistics on the data in the table.",
            "This filter turns the table sideways, so that each row",
            "of the output corresponds to a column of the input.",
            "The columns of the output table contain statistical items",
            "such as mean, standard deviation etc corresponding to each",
            "column of the input table.",
            "</p><p>By default the output table contains columns for the",
            "following items:",
            DocUtils.listInfos( DEFAULT_INFOS ),
            "</p>",
            "<p>However, the output may be customised by supplying one or more",
            "<code>&lt;item&gt;</code> headings.  These may be selected",
            "from the above as well as the following:",
            DocUtils.listInfos( extraKnownInfos ),
            "Additionally, the form \"Q.<em>nn</em>\" may be used to",
            "represent the quantile corresponding to the proportion",
            "0.<em>nn</em>, e.g.:",
            DocUtils.listInfos( QEX_INFOS ),
            "</p>",
            "<p>Any parameters of the input table are propagated",
            "to the output one.",
            "</p>",
            "<p>Note that quantile calculations (including median and",
            "quartiles) can be expensive on memory.  If you want to calculate",
            "quantiles for large tables, it may be wise to reduce the",
            "number of columns to only those you need the quantiles for",
            "earlier in the pipeline.",
            "No interpolation is performed when calculating quantiles.",
            "</p>",
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
                else if ( name.matches( "^[qQ]\\.[0-9]+$" ) ) {
                    double quant = Double.parseDouble( name.substring( 1 ) );
                    assert quant >= 0.0 && quant <= 1.0;
                    infoList.add( new QuantileInfo( quant ) );
                }
                else {
                    StringBuffer msg = new StringBuffer()
                       .append( "Unknown quantity " )
                       .append( name );
                    try {
                        String opts =
                            new Formatter()
                           .formatXML( DocUtils.listInfos( ALL_KNOWN_INFOS ),
                                       6 );
                        msg.append( " must be one of: " )
                           .append( opts )
                           .append( "or Q.nn" );
                    }
                    catch ( SAXException e ) {
                        assert false;
                    }
                    throw new ArgException( msg.toString() );
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
        boolean doCard = Arrays.asList( infos ).contains( CARDINALITY_INFO );

        /* Work out if we need to calculate quantiles. */
        List quantInfoList = new ArrayList();
        for ( int i = 0; i < infos.length; i++ ) {
            if ( infos[ i ] instanceof QuantileInfo ) {
                quantInfoList.add( infos[ i ] );
            }
        }
        boolean doQuant = ! quantInfoList.isEmpty();
        QuantileInfo[] quantInfos = doQuant
            ? (QuantileInfo[]) quantInfoList.toArray( new QuantileInfo[ 0 ] )
            : null;

        long nrow = table.getRowCount();

        /* Prepare statistical accumulators for each column of the table. */
        int ncol = table.getColumnCount();
        UnivariateStats[] colStats = new UnivariateStats[ ncol ];
        CardinalityChecker[] cardCheckers =
            doCard ? new CardinalityChecker[ ncol ] : null;
        QuantCalc[] quantCalcs = new QuantCalc[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            Class clazz = table.getColumnInfo( icol ).getContentClass();
            colStats[ icol ] = UnivariateStats.createStats( clazz );
            if ( doCard ) {
                cardCheckers[ icol ] =
                    new CardinalityChecker( MAX_CARDINALITY );
            }
            if ( doQuant && Number.class.isAssignableFrom( clazz ) ) {
                quantCalcs[ icol ] = QuantCalc.createInstance( clazz, nrow );
            }
        }

        /* Populate them with the the data read from the table. */
        RowSequence rseq = table.getRowSequence();
        long irow = 0L;
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    Object datum = row[ icol ];
                    colStats[ icol ].acceptDatum( datum );
                    if ( doCard ) {
                        cardCheckers[ icol ].acceptDatum( datum );
                    }
                    if ( quantCalcs[ icol ] != null ) {
                        quantCalcs[ icol ].acceptDatum( datum );
                    }
                }
                irow++;
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
                double popvar = nvar / dcount;
                double sampvar = nvar / ( dcount - 1 );
              
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
                map.put( NBAD_INFO, new Long( irow - count ) );
                map.put( SUM_INFO, new Double( sum1 ) );
                if ( isFinite( mean ) ) {
                    map.put( MEAN_INFO, new Float( (float) mean ) );
                }
                if ( isFinite( popvar ) ) {
                    map.put( POPSD_INFO,
                             new Float( (float) Math.sqrt( popvar ) ) );
                    map.put( POPVAR_INFO, new Float( (float) popvar ) );
                }
                if ( isFinite( sampvar ) ) {
                    map.put( SAMPSD_INFO,
                             new Float( (float) Math.sqrt( sampvar ) ) );
                    map.put( SAMPVAR_INFO, new Float( (float) sampvar ) );
                }
                if ( isFinite( skew ) ) {
                    map.put( SKEW_INFO, new Float( (float) skew ) );
                }
                if ( isFinite( kurtosis ) ) {
                    map.put( KURT_INFO, new Float( (float) kurtosis ) );
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
                if ( doCard ) {
                    int ncard = cardCheckers[ icol ].getCardinality();
                    if ( ncard > 0 ) {
                        map.put( CARDINALITY_INFO, new Integer( ncard ) );
                    }
                }
                if ( quantCalcs[ icol ] != null ) {
                    quantCalcs[ icol ].ready();
                    for ( int iq = 0; iq < quantInfos.length; iq++ ) {
                        QuantileInfo quantInfo = quantInfos[ iq ];
                        Number quantile = quantCalcs[ icol ]
                                         .getQuantile( quantInfo.getQuant() );
                        map.put( quantInfo, quantile );
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
     * Metadata item corresponding to a quantile.
     */
    private static class QuantileInfo extends DefaultValueInfo {
        private final double quant_;

        /**
         * Constructs a quantile info with automatically generated
         * name and description.
         *
         * @param  quant  the proportion through the data for this quantile
         */
        QuantileInfo( double quant ) {
            super( "Q_" + quant, Number.class );
            if ( quant < 0.0 || quant > 1.0 ) {
                throw new IllegalArgumentException( quant + 
                                                    " not in range 0-1" );
            }
            quant_ = quant;

            /* Adjust the name and description to look as sensible as
             * possible. */
            String qv = Float.toString( (float) quant );
            Matcher matcher = Pattern.compile( "^0?.([0-9]+)$" ).matcher( qv );
            if ( matcher.matches() ) {
                qv = matcher.group( 1 );
                while ( qv.length() < 2 ) {
                    qv += '0';
                }
            }
            setName( "Q_" + qv );
            if ( qv.length() == 2 ) {
                setDescription( "Percentile " + qv );
            }
            else {
                setDescription( "Quantile corresponding to " + quant );
            }
        }

        /**
         * Constructs a quantile info with a given name and description.
         *
         * @param  name  name
         * @param  description  description
         */
        QuantileInfo( double quant, String name, String description ) {
            this( quant );
            setName( name );
            setDescription( description );
        }

        /**
         * Returns the proportion through the data which this quantile
         * describes.
         *
         * @return   quant
         */
        public double getQuant() {
            return quant_;
        }
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
                    if ( items_.size() < maxCard_ ) {
                        items_.add( obj );
                    }
                    else {
                        items_ = null;
                    }
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
