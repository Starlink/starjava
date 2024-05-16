package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowRunner;
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
    private static final ValueInfo MAD_INFO;
    private static final ValueInfo SMAD_INFO;
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
    private static final ValueInfo ARRAY_NGOOD_INFO;
    private static final ValueInfo ARRAY_SUM_INFO;
    private static final ValueInfo ARRAY_MEAN_INFO;
    private static final ValueInfo ARRAY_POPSD_INFO;

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
        MAD_INFO = new DefaultValueInfo( "MedAbsDev", Float.class,
                                         "Median Absolute Deviation" ),
        SMAD_INFO = new DefaultValueInfo( "ScMedAbsDev", Float.class,
                                          "Median Absolute Deviation * "
                                        + TableStats.MAD_SCALE ),
        SKEW_INFO = new DefaultValueInfo( "Skew", Float.class,
                                          "Gamma 1 skewness measure" ),
        KURT_INFO = new DefaultValueInfo( "Kurtosis", Float.class,
                                          "Gamma 2 peakedness measure" ),
        MIN_INFO = new DefaultValueInfo( "Minimum", Comparable.class,
                                         "Numeric minimum" ),
        MAX_INFO = new DefaultValueInfo( "Maximum", Comparable.class,
                                         "Numeric maximum" ),
        SUM_INFO = new DefaultValueInfo( "Sum", Double.class,
                                         "Sum of values" ),
        MINPOS_INFO = new DefaultValueInfo( "MinPos", Long.class, 
                                            "Row index of numeric minimum" ),
        MAXPOS_INFO = new DefaultValueInfo( "MaxPos", Long.class,
                                            "Row index of numeric maximum" ),
        CARDINALITY_INFO = new DefaultValueInfo( "Cardinality", Integer.class,
            "Number of distinct values in column; " +
            "values >" + UnivariateStats.MAX_CARDINALITY + " ignored" ),
        MEDIAN_INFO = new QuantileInfo( 0.5, "Median",
                                        "Middle value in sequence" ),
        Q1_INFO = new QuantileInfo( 0.25, "Quartile1", "First quartile" ),
        Q2_INFO = new QuantileInfo( 0.50, "Quartile2", "Second quartile" ),
        Q3_INFO = new QuantileInfo( 0.75, "Quartile3", "Third quartile" ),
        ARRAY_NGOOD_INFO =
            new DefaultValueInfo( "ArrayNGood", long[].class,
                                  "Per-element non-blank counts"
                                + " for fixed-length array columns" ),
        ARRAY_SUM_INFO =
            new DefaultValueInfo( "ArraySum", double[].class,
                                  "Per-element sums"
                                + " for fixed-length array columns" ),
        ARRAY_MEAN_INFO =
            new DefaultValueInfo( "ArrayMean", double[].class,
                                  "Per-element means"
                                + " for fixed-length array columns" ),
        ARRAY_POPSD_INFO =
            new DefaultValueInfo( "ArrayStDev", double[].class,
                                  "Per-element population standard deviation"
                                + " for fixed-length array columns" ),
    };

    /** Example Q.* infos for documentation only. */
    private static final ValueInfo[] QEX_INFOS = new ValueInfo[] {
        new DefaultValueInfo( "Q.25", Number.class, "First quartile" ),
        new DefaultValueInfo( "Q.625", Number.class, "Fifth octile" ),
    };

    /** All known per-column quantities (statistical and metadata). */
    private static final ValueInfo[] ALL_KNOWN_INFOS;
    static {
        List<ValueInfo> known = new ArrayList<ValueInfo>();
        known.addAll( Arrays.asList( MetadataFilter.KNOWN_INFOS ) );
        known.addAll( Arrays.asList( KNOWN_INFOS ) );
        ALL_KNOWN_INFOS = known.toArray( new ValueInfo[ 0 ] );
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
        super( "stats", "[-[no]parallel] [-qapprox|-qexact] [<item> ...]" );
    }

    protected String[] getDescriptionLines() {
        Collection<ValueInfo> extras =
            new ArrayList<ValueInfo>( Arrays.asList( KNOWN_INFOS ) );
        extras.removeAll( Arrays.asList( DEFAULT_INFOS ) );
        ValueInfo[] extraKnownInfos = extras.toArray( new ValueInfo[ 0 ] );
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
            "<p>The <code>-qapprox</code> or <code>-qexact</code>",
            "flag controls how quantiles are calculated.",
            "With <code>-qexact</code> they are calculated exactly,",
            "but this requires memory usage scaling with the number of rows.",
            "If the <code>-qapprox</code> flag is supplied,",
            "an method is used which is typically slower and produces only",
            "approximate values, but which will work in fixed memory",
            "and so can be used for arbitrarily large tables.",
            "By default, exact calculation is used.",
            "These flags are ignored if neither quantiles nor the MAD",
            "are being calculated",
            "</p>",
            "<p>The <code>-noparallel</code> flag may be supplied to inhibit",
            "multi-threaded statistics accumulation.",
            "Calculation is done in parallel by default if multi-threaded",
            "hardware is available, and it's usually faster.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        boolean isParallel = true;
        boolean isQuantileApprox = false;
        final ValueInfo[] colInfos;
        Map<String,ValueInfo> infoMap = new HashMap<>();
        for ( int i = 0; i < ALL_KNOWN_INFOS.length; i++ ) {
            ValueInfo info = ALL_KNOWN_INFOS[ i ];
            infoMap.put( info.getName().toLowerCase(), info );
        }
        List<ValueInfo> infoList = new ArrayList<>();
        while ( argIt.hasNext() ) {
            String name = argIt.next();
            argIt.remove();
            String lname = name.toLowerCase();
            double quantile = parseQuantileSpecifier( name );
            if ( lname.equals( "-parallel" ) ) {
                isParallel = true;
            }
            else if ( lname.equals( "-noparallel" ) ) {
                isParallel = false;
            }
            else if ( lname.equals( "-qapprox" ) ) {
                isQuantileApprox = true;
            }
            else if ( lname.equals( "-qexact" ) ) {
                isQuantileApprox = false;
            }
            else if ( infoMap.containsKey( lname ) ) {
                infoList.add( infoMap.get( lname ) );
            }
            else if ( ! Double.isNaN( quantile ) ) {
                infoList.add( new QuantileInfo( quantile ) );
            }
            else {
                List<ValueInfo> docInfoList = new ArrayList<>();
                docInfoList.addAll( Arrays.asList( ALL_KNOWN_INFOS ) );
                docInfoList
                   .add( new DefaultValueInfo( "Q.nn", Number.class,
                                               "Quantile for 0.nn" ) );
                ValueInfo[] docInfos =
                    docInfoList.toArray( new ValueInfo[ 0 ] );
                StringBuffer msg = new StringBuffer()
                   .append( "Unknown quantity " )
                   .append( name );
                try {
                    String opts =
                        new Formatter()
                       .formatXML( DocUtils.listInfos( docInfos ), 6 );
                    msg.append( " must be one of: " )
                       .append( opts );
                }
                catch ( SAXException e ) {
                    assert false;
                }
                throw new ArgException( msg.toString() );
            }
        }
        colInfos = infoList.isEmpty() ? DEFAULT_INFOS
                                      : infoList.toArray( new ValueInfo[ 0 ] );
        final RowRunner runner = isParallel ? RowRunner.DEFAULT
                                            : RowRunner.SEQUENTIAL;
        final boolean qApprox = isQuantileApprox;
        final Supplier<Quantiler> qSupplier =
            isQuantileApprox ? GKQuantiler::new
                             : SortQuantiler::new;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                MapGroup<ValueInfo,Object> group;
                try {
                    group = statsMapGroup( base, colInfos, runner, qSupplier );
                }
                catch ( OutOfMemoryError e ) {
                    if ( ! qApprox ) {
                        throw new IOException( "Out of memory: Try -qapprox?",
                                               e );
                    }
                    else {
                        throw e;
                    }
                }
                group.setKnownKeys( Arrays.asList( colInfos ) );
                AbstractStarTable table = new ValueInfoMapGroupTable( group );
                table.setParameters( base.getParameters() );
                return table;
            }
        };
    }

    /**
     * Parses a string that represents an arbitrary quantile.
     * This is of the form Q.xxx, where xxx is the quantile fraction,
     * so for instance "Q.01" is the first percentile and "Q.4"
     * is the 40th percentile.
     *
     * @param  txt  text which may represent a quantile specifier
     * @return  value in the range 0..1 giving the quantile level,
     *          or NaN if it's not a quantile specifier
     */
    public static double parseQuantileSpecifier( String txt ) {
        if ( txt.matches( "^[qQ]\\.[0-9]+$" ) ) {
            double quant = Double.parseDouble( txt.substring( 1 ) );
            assert quant >= 0.0 && quant <= 1.0;
            return quant;
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Constructs a MapGroup containing statistical information about 
     * a given table.
     *
     * @param   table   input table
     * @param  infos   metadata for statistical quantities to be calculated
     * @param  runner   object that can perform data access
     * @return  mapgroup containing column statistics
     */
    private static MapGroup<ValueInfo,Object>
            statsMapGroup( StarTable table, ValueInfo[] infos,
                           RowRunner runner, Supplier<Quantiler> qSupplier )
            throws IOException {
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        int ncol = cinfos.length;

        /* Work out if we need to calculate cardinalities. */
        boolean doCard = Arrays.asList( infos ).contains( CARDINALITY_INFO );

        /* Work out if we need to calculate Mean Absolute Deviations. */
        boolean doMad = Arrays.asList( infos ).contains( MAD_INFO )
                     || Arrays.asList( infos ).contains( SMAD_INFO );

        /* Work out if we need to calculate quantiles. */
        List<QuantileInfo> quantInfoList = new ArrayList<QuantileInfo>();
        for ( int i = 0; i < infos.length; i++ ) {
            if ( infos[ i ] instanceof QuantileInfo ) {
                quantInfoList.add( (QuantileInfo) infos[ i ] );
            }
        }
        boolean doQuant = ! quantInfoList.isEmpty() || doMad;
        QuantileInfo[] quantInfos =
              doQuant ? quantInfoList.toArray( new QuantileInfo[ 0 ] )
                      : null;

        /* Calculate the statistics in sequential or parallel mode. */
        TableStats tstats =
            TableStats.calculateStats( table, runner,
                                       doQuant ? qSupplier : null, doCard );
        UnivariateStats[] colStats = tstats.getColumnStats();

        /* Get a MapGroup representing column metadata (the option is 
         * provided to output this alongside the statistical results). */
        MapGroup<ValueInfo,Object> group =
            MetadataFilter.metadataMapGroup( table );

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
            Comparable<?> min = stats.getMinimum();
            Comparable<?> max = stats.getMaximum();
            UnivariateStats.ArrayStats arrayStats = stats.getArrayStats();

            /* Add statistical quantities to the column's
             * info->values map. */
            Map<ValueInfo,Object> map = group.getMaps().get( icol );
            map.put( NGOOD_INFO, new Long( count ) );
            map.put( NBAD_INFO, new Long( tstats.getRowCount() - count ) );
            map.put( SUM_INFO, new Double( sum1 ) );
            if ( isFinite( mean ) ) {
                map.put( MEAN_INFO, new Float( (float) mean ) );
            }
            if ( isFinite( popvar ) ) {
                map.put( POPSD_INFO, new Float( (float) Math.sqrt( popvar ) ) );
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
            if ( min != null ) {
                map.put( MIN_INFO, min );
                map.put( MINPOS_INFO, new Long( stats.getMinPos() + 1 ) );
            }
            if ( max != null ) {
                map.put( MAX_INFO, max );
                map.put( MAXPOS_INFO, new Long( stats.getMaxPos() + 1 ) );
            }
            if ( doCard ) {
                int ncard = stats.getCardinality();
                if ( ncard > 0 ) {
                    map.put( CARDINALITY_INFO, new Integer( ncard ) );
                }
            }
            Quantiler quantiler = stats.getQuantiler();
            if ( quantiler != null ) {
                for ( int iq = 0; iq < quantInfos.length; iq++ ) {
                    QuantileInfo quantInfo = quantInfos[ iq ];
                    double quantile =
                        quantiler.getValueAtQuantile( quantInfo.getQuant() );
                    map.put( quantInfo, Float.valueOf( (float) quantile ) );
                }
            }
            if ( arrayStats != null ) {
                long[] sum0s = arrayStats.getCounts();
                double[] sum1s = arrayStats.getSum1s();
                double[] sum2s = arrayStats.getSum2s();
                int leng = arrayStats.getLength();
                double[] means = new double[ leng ];
                double[] popsds = new double[ leng ];
                for ( int i = 0; i < leng; i++ ) {
                    double acount = sum0s[ i ];
                    double asum1 = sum1s[ i ];
                    double asum2 = sum2s[ i ];
                    double amean = asum1 / acount;
                    double anvar = ( asum2 - asum1 * asum1 / acount );
                    double apopvar = anvar / acount;
                    means[ i ] = amean;
                    popsds[ i ] = Math.sqrt( apopvar );
                }
                map.put( ARRAY_NGOOD_INFO, sum0s );
                map.put( ARRAY_SUM_INFO, sum1s );
                map.put( ARRAY_MEAN_INFO, means );
                map.put( ARRAY_POPSD_INFO, popsds );
            }
        }

        /* For the MAD, we need an extra pass. */
        if ( doMad ) {
            double[] medians = new double[ ncol ];
            int nmad = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                Quantiler quantiler = colStats[ icol ].getQuantiler();
                medians[ icol ] = Double.NaN;
                if ( quantiler != null ) {
                    medians[ icol ] = quantiler.getValueAtQuantile( 0.5 );
                    nmad++;
                }
            }
            if ( nmad > 0 ) {
                double[] mads = TableStats.calculateMads( table, runner,
                                                          qSupplier, medians );
                for ( int icol = 0; icol < ncol; icol++ ) {
                    double mad = mads[ icol ];
                    if ( ! Double.isNaN( mad ) ) {
                        double smad = mad * TableStats.MAD_SCALE;
                        Map<ValueInfo,Object> map = group.getMaps().get( icol );
                        map.put( MAD_INFO, Float.valueOf( (float) mad ) );
                        map.put( SMAD_INFO, Float.valueOf( (float) smad ) );
                    }
                }
            }
        }
        return group;
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
}
