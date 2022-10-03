package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELTable;
import uk.ac.starlink.ttools.mode.CubeMode;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.layer.BinMapper;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.plot2.layer.Rounding;
import uk.ac.starlink.ttools.plot2.layer.Unit;

/**
 * Calculates optionally weighted density maps/histograms on an N-dimensional
 * grid and outputs them in table form.
 *
 * @author   Mark Taylor
 * @since    7 Jan 2022
 */
public class GridDensityMap extends SingleMapperTask {

    private final WordsParameter<String> coordsParam_;
    private final WordsParameter<double[]> boundsParam_;
    private final WordsParameter<Boolean> logsParam_;
    private final WordsParameter<Double> binsizeParam_;
    private final WordsParameter<Integer> nbinParam_;
    private final StringMultiParameter quantsParam_;
    private final ChoiceParameter<Combiner> combinerParam_;
    private final BooleanParameter sparseParam_;
    private final RowRunnerParameter runnerParam_;

    /**
     * Constructor.
     */
    public GridDensityMap() {
        super( "Calculates N-dimensional density maps", new ChoiceMode(),
               true, true );
        final String quantsName = "cols";

        coordsParam_ = WordsParameter.createStringWordsParameter( "coords" );
        coordsParam_.setWordUsage( "<expr>" );
        coordsParam_.setPrompt( "Coordinate for each dimension" );
        coordsParam_.setDescription( new String[] {
            "<p>Defines the dimensions of the grid over which",
            "accumulation will take place.",
            "The form of this value is a space-separated list of words",
            "each giving a column name or",
            "<ref id='jel'>algebraic expression</ref>",
            "defining one of the dimensions of the output grid.",
            "For a 1-dimensional histogram, only one value is required.",
            "</p>",
        } );

        String requireDimCount = String.join( "\n",
            "<p>If supplied, this parameter must have the same number of words",
            "as the <code>" + coordsParam_.getName() + "</code> parameter.",
            "</p>",
        "" );

        logsParam_ = WordsParameter.createBooleanWordsParameter( "logs" );
        logsParam_.setWordUsage( "true|false" );
        logsParam_.setPrompt( "Log flag for each dimension" );
        logsParam_.setNullPermitted( true );
        logsParam_.setDescription( new String[] {
            "<p>Determines whether each coordinate axis is linear or",
            "logarithmic.",
            "By default the grid axes are linear, but if this parameter",
            "is supplied with one or more true values,",
            "the bins on the corresponding axes are assigned logarithmically",
            "instead.",
            "</p>",
            requireDimCount,
        } );
 
        boundsParam_ = CubeMode.createBoundsParameter( "bounds" );
        boundsParam_.setNullPermitted( true );
        boundsParam_.setStringDefault( null );
        boundsParam_.setWordUsage( "[<lo>]:[<hi>]" );
        boundsParam_.setPrompt( "Data bounds for each dimension" );
        boundsParam_.setDescription( new String[] {
            "<p>Gives the bounds for each dimension of the cube in data",
            "coordinates.  The form of the value is a space-separated list",
            "of words, each giving an optional lower bound, then a colon,",
            "then an optional upper bound, for instance",
            "\"1:100 0:20\" to represent a range for two-dimensional output",
            "between 1 and 100 of the first coordinate (table column)",
            "and between 0 and 20 for the second.",
            "Either or both numbers may be omitted to indicate that the",
            "bounds should be determined automatically by assessing the",
            "range of the data in the table.",
            "A null value for the parameter indicates that all bounds should",
            "be determined automatically for all the dimensions.",
            "</p>",
            "<p>If any of the bounds need to be determined automatically",
            "in this way, two passes through the data will be required,",
            "the first to determine bounds and the second",
            "to calculate the map.",
            "</p>",
            requireDimCount,
        } );

        String binsizeName = "binsizes";
        String nbinName = "nbins";

        binsizeParam_ = WordsParameter.createDoubleWordsParameter( binsizeName);
        binsizeParam_.setWordUsage( "<size>" );
        binsizeParam_.setNullPermitted( true );
        binsizeParam_.setPrompt( "Extent of bins in each dimension" );
        binsizeParam_.setDescription( new String[] {
            "<p>Gives the extent of of the data bins in each",
            "dimension in data coordinates.",
            "The form of the value is a space-separated list of values,",
            "giving a list of extents for the first, second, ... dimension.",
            "Either this parameter or the <code>" + nbinName + "</code>",
            "parameter must be supplied.",
            "</p>",
            requireDimCount,
        } );
        
        nbinParam_ = WordsParameter.createIntegerWordsParameter( nbinName );
        nbinParam_.setWordUsage( "<num>" );
        nbinParam_.setNullPermitted( true );
        nbinParam_.setPrompt( "Number of bins in each dimension" );
        nbinParam_.setDescription( new String[] {
            "<p>Gives the approximate number of bins in each dimension.",
            "The form of the value is a space-separated list of integers,",
            "giving the number of bins for the output histogram in the",
            "first, second, ... dimension.",
            "An attempt is made to use round numbers for bin sizes",
            "so the bin counts may not be exactly as specified.",
            "Either this parameter or the <code>" + binsizeName + "</code>",
            "parameter must be supplied.",
            "</p>",
            requireDimCount,
        } );

        Combiner[] combiners = Combiner.getKnownCombiners();
        combinerParam_ = new ChoiceParameter<Combiner>( "combine", combiners );
        combinerParam_.setPrompt( "Combination method" );
        StringBuffer lbuf = new StringBuffer();
        for ( Combiner combiner : combiners ) {
            lbuf.append( "<li>" )
                .append( "<code>" )
                .append( combiner.getName() )
                .append( "</code>: " )
                .append( combiner.getDescription() )
                .append( "</li>\n" );
        }
        String combinersDescrip = lbuf.toString();
        combinerParam_.setDescription( new String[] {
            "<p>Defines the default way that values contributing",
            "to the same density map bin",
            "are combined together to produce the value assigned to that bin.",
            "Possible values are:",
            "<ul>",
            lbuf.toString(),
            "</ul>",
            "</p>",
            "<p>Note this value may be overridden on a per-column basis",
            "by the <code>" + quantsName + "</code> parameter.",
            "</p>",
        } );
        combinerParam_.setDefaultOption( Combiner.MEAN );

        sparseParam_ = new BooleanParameter( "sparse" );
        sparseParam_.setPrompt( "Omit rows for empty cells?" );
        sparseParam_.setDescription( new String[] {
            "<p>Determines whether a row is written for every cell in the",
            "defined grid, or only for those cells in which data appears",
            "in the input.",
            "The result will usually be more compact if this is set false,",
            "but if you want to compare results from different runs",
            "it may be convenient to set it true.",
            "</p>",
        } );
        sparseParam_.setBooleanDefault( true );

        quantsParam_ =
            CombinedColumn
           .createCombinedColumnsParameter( quantsName, combinerParam_ );
        String quantsDflt = "1;count;COUNT";
        quantsParam_.setStringDefault( quantsDflt );
        quantsParam_.setDescription( quantsParam_.getDescription()
                                   + String.join( "\n",
            "<p>The default value is \"<code>" + quantsDflt + "</code>\"",
            "which simply provides an unweighted histogram,",
            "i.e. a count of the rows in each bin",
            "(aggregation of the value \"<code>1</code>\" using the",
            "combination method \"<code>count</code>\",",
            "yielding an output column named \"<code>COUNT</code>\").",
            "</p>",
        "" ) );

        runnerParam_ = RowRunnerParameter.createScanRunnerParameter( "runner" );

        getParameterList().addAll( Arrays.asList( new Parameter<?>[] {
            coordsParam_,
            logsParam_,
            boundsParam_,
            binsizeParam_,
            nbinParam_,
            quantsParam_,
            combinerParam_,
            sparseParam_,
            runnerParam_,
        } ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {

        /* Define the dimensionality of the accumulation grid,
         * which is the number of supplied grid coordinates,
         * and configure the multiplicity of some of the other
         * parameters accordingly. */
        String[] coordExprs = coordsParam_.wordsValue( env );
        final int ndim = coordExprs.length;
        boundsParam_.setRequiredWordCount( ndim );
        logsParam_.setRequiredWordCount( ndim );
        binsizeParam_.setRequiredWordCount( ndim );
        nbinParam_.setRequiredWordCount( ndim );

        /* Get linear/log flags. */
        Boolean[] logFlags = logsParam_.wordsValue( env );
        final boolean[] isLogs = new boolean[ ndim ];
        if ( logFlags != null ) {
            for ( int idim = 0; idim < ndim; idim++ ) {
                isLogs[ idim ] = Boolean.TRUE.equals( logFlags[ idim ] );
            }
        }

        /* Get the explicitly specified bounds for the output grid. */
        double[][] boundsWords = boundsParam_.wordsValue( env );
        final double[] loBounds = new double[ ndim ];
        final double[] hiBounds = new double[ ndim ];
        if ( boundsWords != null ) {
            for ( int i = 0; i < ndim; i++ ) {
                double[] bounds = boundsWords[ i ];
                loBounds[ i ] = bounds[ 0 ];
                hiBounds[ i ] = bounds[ 1 ];
            }
        }
        else {
            Arrays.fill( loBounds, Double.NaN );
            Arrays.fill( hiBounds, Double.NaN );
        }

        /* Get either the number of bins or the extent of each bin in
         * each dimension.  The bin extent is required, but it can be
         * determined from the bounds and the bin count if necessary. */
        Double[] binsizeWords = binsizeParam_.wordsValue( env );
        final double[] binsizes;
        final int[] nbins;
        if ( binsizeWords != null ) {
            binsizes = new double[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                binsizes[ i ] = binsizeWords[ i ].doubleValue();
                if ( ! ( binsizes[ i ] > 0 ) ) {
                    throw new ParameterValueException( binsizeParam_,
                                                       "Non-positive value" );
                }
            }
            nbins = null;
        }
        else {
            nbinParam_.setNullPermitted( false );
            Integer[] nbinWords = nbinParam_.wordsValue( env );
            nbins = new int[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                nbins[ i ] = nbinWords[ i ].intValue();
                if ( nbins[ i ] <= 0 ) {
                    throw new ParameterValueException( nbinParam_,
                                                       "Non-positive value" );
                }
            }
            binsizes = null;
        }

        /* Prepare specification of quantities to be accumulated. */
        String[] quants = quantsParam_.stringsValue( env );
        Combiner dfltCombiner = combinerParam_.objectValue( env );
        List<CombinedColumn> qcList = new ArrayList<>();
        for ( String quantity : quants ) {
            CombinedColumn parsedCol =
                CombinedColumn.parseSpecification( quantity, quantsParam_,
                                                   combinerParam_ );
            String expr = parsedCol.getExpression();
            Combiner qCombiner = parsedCol.getCombiner();
            Combiner combiner = qCombiner == null ? dfltCombiner : qCombiner;
            String qName = parsedCol.getName();
            String label = qName == null
                         ? expr.replaceAll( "\\s+", "" )
                               .replaceAll( "[^0-9A-Za-z]+", "_" )
                         : qName;
            qcList.add( new CombinedColumn( expr, combiner, label ) );
        }

        /* Put it together to form a TableProducer. */
        RowRunner runner = runnerParam_.objectValue( env );
        boolean isSparse = sparseParam_.booleanValue( env );
        final SingleTableMapping mapping =
            new GridMapMapping( coordExprs, isLogs, loBounds, hiBounds,
                                nbins, binsizes,
                                qcList.toArray( new CombinedColumn[ 0 ] ),
                                isSparse, runner );
        final TableProducer inProd = createInputProducer( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return mapping.map( inProd.getTable() );
            }
        };
    }

    /**
     * Table mapping that does the work for this task.
     */
    private static class GridMapMapping implements SingleTableMapping {

        final String[] coordExprs_;
        final boolean[] isLogs_;
        final double[] loBounds_;
        final double[] hiBounds_;
        final int[] nbins_;
        final double[] binSizes_;
        final CombinedColumn[] qcols_;
        final boolean isSparse_;
        final RowRunner rowRunner_;
        final int ndim_;
        final int nq_;

        /**
         * Constructor.
         *
         * @param  coordExprs  JEL expressions defining grid coordinate values
         *                     (ndim-element array)
         * @param  isLogs    log/linear coordinate flags (ndim-element array)
         * @param  loBounds  grid coordinate fixed lower bounds
         *                   (ndim-element array, some elements may be NaN)
         * @param  hiBounds  grid coordinate fixed upper bounds
         *                   (ndim-element array, some elements may be NaN)
         * @param  nbins     bin counts in each grid dimension
         *                   (ndim-element array, null if binSizes is supplied)
         * @param  binSizes  bin extents in each grid dimension
         *                   (ndim-element array, null if nbins is supplied)
         * @param  qCols   array of column specifications for quantities to
         *                 be accumulated
         * @param  isSparse   true for sparse table output, false for dense
         * @param  rowRunner  control for parallel execution
         */
        GridMapMapping( String[] coordExprs, boolean[] isLogs,
                        double[] loBounds, double[] hiBounds, int[] nbins,
                        double[] binSizes, CombinedColumn[] qcols,
                        boolean isSparse, RowRunner rowRunner ) {
            coordExprs_ = coordExprs;
            isLogs_ = isLogs;
            loBounds_ = loBounds;
            hiBounds_ = hiBounds;
            nbins_ = nbins;
            binSizes_ = binSizes;
            qcols_ = qcols;
            isSparse_ = isSparse;
            rowRunner_ = rowRunner;
            ndim_ = coordExprs.length;
            nq_ = qcols.length;
        }

        public StarTable map( StarTable inTable )
                throws IOException, TaskException {
            String[] qExprs = new String[ nq_ ];
            Combiner[] qCombiners = new Combiner[ nq_ ];
            for ( int iq = 0; iq < nq_; iq++ ) {
                CombinedColumn qcol = qcols_[ iq ];
                qExprs[ iq ] = qcol.getExpression();
                qCombiners[ iq ] = qcol.getCombiner();
            }

            /* Prepare a table containing columns for the coordinates,
             * followed by columns for the accumulated quantities.
             * This table containns all the values we need for the
             * calculations. */
            String[] exprs = new String[ ndim_ + nq_ ];
            System.arraycopy( coordExprs_, 0, exprs, 0, ndim_ );
            System.arraycopy( qExprs, 0, exprs, ndim_, nq_ );
            StarTable cqTable;
            try {
                cqTable = JELTable.createJELTable( inTable, exprs );
            }
            catch ( CompilationException e ) {
                throw new TaskException( "Bad expression: " + e.getMessage(),
                                         e );
            }
            for ( int ic = 0; ic < ndim_ + nq_; ic++ ) {
                ValueInfo info = cqTable.getColumnInfo( ic );
                if ( !Number.class.isAssignableFrom( info.getContentClass() ) ){
                    String msg = ( ic < ndim_ ? "Coordinate " : "Quantity " )
                               + info.getName() + " is not numeric";
                    throw new TaskException( msg );
                }
            }

            /* Determine bin grid geometry. */
            BinMapper[] binMappers = createBinMappers( cqTable );

            /* Scan the table to accumulate statistics, possibly in parallel. */
            List<Map<BinKey,Combiner.Container>> qMaps =
                rowRunner_.collect( new BinsCollector( binMappers, isLogs_,
                                                       qCombiners,
                                                       loBounds_, hiBounds_ ),
                                    cqTable );

            /* Construct and return output table. */
            RowControl rowControl = createRowControl( binMappers, qMaps );
            GridTable outTable =
                new GridTable( binMappers, isLogs_, qcols_,
                               Tables.getColumnInfos( cqTable ),
                               qMaps, rowControl );
            for ( int idim = 0; idim < ndim_; idim++ ) {
                outTable.addCoordColumn( idim, 0.5, null, ", central position",
                                         null );
            }
            for ( int iq = 0; iq < nq_; iq++ ) {
                outTable.addQuantityColumn( iq );
            }
            for ( int idim = 0; idim < ndim_; idim++ ) {
                outTable.addCoordColumn( idim, 0.0, "_lo", ", lower bound",
                                         "stat.min" );
                outTable.addCoordColumn( idim, 1.0, "_hi", ", upper bound",
                                         "stat.max" );
            }
            return outTable;
        }

        /**
         * Constructs an array of BinMappers for the table, which define the
         * bin grid for quantity accumulation.
         * This step may or may not require a scan of the table to
         * determine actual data ranges.
         *
         * @param  cqTable  table of which the first ndim columns
         *                  provide the grid coordinates
         * @retur  ndim-element array of BinMappers
         */
        private BinMapper[] createBinMappers( StarTable cqTable )
                throws IOException {
            BinMapper[] mappers = new BinMapper[ ndim_ ];

            /* If we have bin extents, we can proceed directly. */
            if ( binSizes_ != null ) {
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    boolean isLog = isLogs_[ idim ];
                    double binPoint = getExampleCoordinate( cqTable, idim );
                    double binSize = binSizes_[ idim ];
                    double binPhase = getBinPhase( binSize, idim );
                    mappers[ idim ] =
                        BinMapper.createMapper( isLog, binSize,
                                                binPhase, binPoint );
                }
                return mappers;
            }

            /* Otherwise, work it out from data ranges and the number of
             * bins required on each axis. */
            else {

                /* If all bounds have been specified explicitly, use those
                 * values.  Otherwise we need to calculate the ranges by
                 * scanning the data.  In that case calculate ranges for all
                 * dimensions, though not all of them may be required. */
                Range[] ranges =
                      hasAllBounds()
                    ? null
                    : rowRunner_.collect( new RangeCollector( ndim_ ), cqTable);

                /* For each dimension use supplied bounds where available,
                 * otherwise the ones we just calculated from the data. */
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    boolean isLog = isLogs_[ idim ];
                    double lo = Double.isNaN( loBounds_[ idim ] )
                              ? ranges[ idim ].getFiniteBounds( isLog )[ 0 ]
                              : loBounds_[ idim ];
                    double hi = Double.isNaN( hiBounds_[ idim ] )
                              ? ranges[ idim ].getFiniteBounds( isLog )[ 1 ]
                              : hiBounds_[ idim ];
                    double binSize =
                        BinSizer.createCountBinSizer( nbins_[ idim ] )
                                .getWidth( isLog, lo, hi, Rounding.DECIMAL );
                    double binPhase = getBinPhase( binSize, idim );
                    mappers[ idim ] =
                        BinMapper.createMapper( isLog, binSize, binPhase, lo );
                }
                return mappers;
            }
        }

        /**
         * Returns an example data value for a given grid coordinate.
         * This is required as input to the BinMapper creation.
         *
         * @param  cqTable  table of which the first ndim columns
         *                  provide the grid coordinates
         * @param  idim     index of dimension (also table column)
         *                  for which a value is required
         * @return  representative value
         */
        private double getExampleCoordinate( StarTable cqTable, int idim )
                throws IOException {

            /* If one of the limits has been specified explicitly, use that. */
            if ( !Double.isNaN( loBounds_[ idim ] ) ) {
                return loBounds_[ idim ];
            }
            else if ( !Double.isNaN( hiBounds_[ idim ] ) ) {
                return hiBounds_[ idim ];
            }

            /* Otherwise just pick the first value in the input table. */
            else {
                try ( RowSequence rseq = cqTable.getRowSequence() ) {
                    while ( rseq.next() ) {
                        Object obj = rseq.getCell( idim );
                        if ( obj instanceof Number ) {
                            Double dval = ((Number) obj).doubleValue();
                            if ( !Double.isNaN( dval ) &&
                                 ( !isLogs_[ idim ] || dval > 0 ) ) {
                                return dval;
                            }
                        }
                    }
                }
                throw new IOException( "No data for dimension " + idim );
            }
        }

        /**
         * Provides an appropriate bin phase value for a given dimension.
         * A supplied lower or upper bound is used to fix the value if
         * available; otherwise a default value is returned.
         *
         * @param  binSize  bin size in dimension
         * @param  idim  coordinate index
         * @return  phase value in range 0..1
         */
        private double getBinPhase( double binSize, int idim ) {
            final double point;
            if ( !Double.isNaN( loBounds_[ idim ] ) ) {
                point = loBounds_[ idim ];
            }
            else if ( !Double.isNaN( hiBounds_[ idim ] ) ) {
                point = hiBounds_[ idim ];
            }
            else {
                return 0;
            }
            double phase = isLogs_[ idim ]
                         ? ( BinMapper.log( point ) % BinMapper.log( binSize ) )
                           / BinMapper.log( binSize )
                         : ( point % binSize ) / binSize;
            if ( phase < 0 ) {
                phase += 1;
            }
            assert phase >= 0 && phase <= 1;
            return phase;
        }

        /**
         * Determines whether all the grid coordinate bounds have been
         * supplied explicitly by the user.
         *
         * @return   true iff all lower and upper bounds are available
         */
        private boolean hasAllBounds() {
            for ( int idim = 0; idim < ndim_; idim++ ) {
                if ( Double.isNaN( loBounds_[ idim ] ) ||
                     Double.isNaN( hiBounds_[ idim ] ) ) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns an object defining the rows that will be present in the
         * output table.  Each row corresponds to one grid element (bin).
         * The result iterates in first-coordinate-slowest order.
         *
         * @param  qMaps  result of accumulation
         * @return  row control
         */
        private RowControl
                createRowControl( BinMapper[] binMappers,
                                  List<Map<BinKey,Combiner.Container>> qMaps ) {

            /* In the case of sparse output, a row is output for each cell
             * which has at least one non-blank entry. */
            if ( isSparse_ ) {

                /* Get the keys for all the cells with non-blank entries and 
                 * sort them into a well-defined sequence. */
                final BinKey[] keys =
                    getAllKeys( qMaps ).toArray( new BinKey[ 0 ] );
                Arrays.sort( keys, new Comparator<BinKey>() {
                    public int compare( BinKey k1, BinKey k2 ) {
                        int[] ibins1 = k1.ibins_;
                        int[] ibins2 = k2.ibins_;
                        for ( int idim = 0; idim < ndim_; idim++ ) {
                            int ibin1 = ibins1[ idim ];
                            int ibin2 = ibins2[ idim ];
                            if ( ibin1 < ibin2 ) {
                                return -1;
                            }
                            else if ( ibin1 > ibin2 ) {
                                return +1;
                            }
                            assert ibin1 == ibin2;
                        }
                        return 0;
                    }
                } );

                /* Construct and return a RowControl based on this list. */
                return new RowControl() {
                    public long getRowCount() {
                        return keys.length;
                    }
                    public BinKey getBinKey( long lrow ) {
                        return keys[ (int) lrow ];
                    }
                };
            }

            /* For non-sparse output, provide an iterator over all the
             * rows in the N-cube of interest. */ 
            else {

                /* Work out the (inclusive) lower and upper bin index bounds
                 * in each dimension. */
                final int[] loIbins = new int[ ndim_ ];
                final int[] hiIbins = new int[ ndim_ ];

                /* If the grid bounds have not all been explicitly provided
                 * by the user, determine the actual bin index bounds from the
                 * accumulated results. */
                if ( ! hasAllBounds() ) {
                    Arrays.fill( loIbins, Integer.MAX_VALUE );
                    Arrays.fill( hiIbins, Integer.MIN_VALUE );
                    for ( BinKey key : getAllKeys( qMaps ) ) {
                        for ( int idim = 0; idim < ndim_; idim++ ) {
                            int ibin = key.ibins_[ idim ];
                            loIbins[ idim ] = Math.min( loIbins[ idim ], ibin );
                            hiIbins[ idim ] = Math.max( hiIbins[ idim ], ibin );
                        }
                    }
                }

                /* Use supplied bounds where available, fill in with
                 * calculated ones for the remainder. */
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    BinMapper binMapper = binMappers[ idim ];
                    double loBound = loBounds_[ idim ];
                    double hiBound = hiBounds_[ idim ];
                    if ( !Double.isNaN( loBound ) ) {
                        loIbins[ idim ] = binMapper.getBinIndex( loBound );
                    }
                    if ( !Double.isNaN( hiBound ) ) {
                        hiIbins[ idim ] = binMapper.getBinIndex( hiBound );
                    }
                }

                /* Construct and return a RowControl that will iterate over
                 * all the cells in the thus defined hypercube. */
                long nr = 1;
                final int[] nbins = new int[ ndim_ ];
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    nbins[ idim ] = hiIbins[ idim ] - loIbins[ idim ] + 1;
                    nr *= nbins[ idim ];
                }
                final long nrow = nr;
                return new RowControl() {
                    public long getRowCount() {
                        return nrow;
                    }
                    public BinKey getBinKey( long lrow ) {
                        int[] ibins = new int[ ndim_ ];
                        long lr = lrow;
                        for ( int idim = ndim_ - 1; idim >= 0; idim-- ) {
                            int nb = nbins[ idim ];
                            int ib = (int) ( lr % nb );
                            if ( ib < 0 ) {
                                ib += nb;
                            }
                            assert ib >= 0 && ib < nb;
                            ibins[ idim ] = ib + loIbins[ idim ];
                            assert ibins[ idim ] >= loIbins[ idim ]
                                && ibins[ idim ] <= hiIbins[ idim ];
                            lr = lr / nb;
                        }
                        return new BinKey( ibins );
                    }
                };
            }
        }

        /**
         * Returns a set of all the keys that are used
         * (for which non-blank accumulated values exist) in the
         * accumulated output map list.
         *
         * @param  qMaps  result of accumulation
         */
        private Set<BinKey>
                getAllKeys( List<Map<BinKey,Combiner.Container>> qMaps ) {
            if ( qMaps.size() == 1 ) {
                return Collections.unmodifiableSet( qMaps.get( 0 ).keySet() );
            }
            else {
                Set<BinKey> keySet = new HashSet<>();
                for ( Map<BinKey,Combiner.Container> qMap : qMaps ) {
                    keySet.addAll( qMap.keySet() );
                }
                return keySet;
            }
        }
    }

    /**
     * Identifies a grid cell.  Equality semantics are implemented.
     */
    private static class BinKey {

        final int[] ibins_;
 
        /**
         * Constructor.
         *
         * @param  ibins  N-d array of bin indices defnining position
         *                in N-d grid
         */
        BinKey( int[] ibins ) {
            ibins_ = ibins;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode( ibins_ );
        }

        @Override
        public boolean equals( Object other ) {
            return other instanceof BinKey
                && Arrays.equals( this.ibins_, ((BinKey) other).ibins_ );
        }
    }

    /**
     * Defines a closed, ordered list of BinKey objects.
     */
    private interface RowControl {

        /**
         * Returns the number of rows in this list.
         *
         * @return  row count
         */
        long getRowCount();

        /**
         * Returns a BinKey for a given row.
         *
         * @param   lrow  row index, in the range 0..getRowCount()
         * @return   bin identifier
         */
        BinKey getBinKey( long lrow );
    }

    /**
     * Table presented as output containing results of grid accumulation.
     * As initially constructed it has no columns, but methods are provided
     * to add columns as required.
     */
    private static class GridTable extends ColumnStarTable {

        private final BinMapper[] binMappers_;
        private final boolean[] isLogs_;
        private final CombinedColumn[] qcols_;
        private final ColumnInfo[] cqInfos_;
        private final List<Map<BinKey,Combiner.Container>> qMaps_;
        private final RowControl rowControl_;

        /**
         * Constructor.
         *
         * @param  binMappers   ndim-element array defining grid
         * @param  isLogs    ndim-element array for log/linear grid coordinates
         * @param  qcols     nq-element array defining quantities accumulated
         * @param  cqInfos  column metadata for table contaning
         *                  ndim coordinate columns followed by
         *                  nq accumulation quantities
         * @param  qMaps   accumulation results
         * @param  rowControl   defines output row organisation
         */
        GridTable( BinMapper[] binMappers, boolean[] isLogs,
                   CombinedColumn[] qcols, ColumnInfo[] cqInfos,
                   List<Map<BinKey,Combiner.Container>> qMaps,
                   RowControl rowControl ) {
            binMappers_ = binMappers;
            isLogs_ = isLogs;
            qcols_ = qcols;
            cqInfos_ = cqInfos;
            qMaps_ = qMaps;
            rowControl_ = rowControl;
        }

        public long getRowCount() {
            return rowControl_.getRowCount();
        }

        /**
         * Adds a numeric column giving the value of a grid coordinate.
         *
         * @param  icoord  coordinate index
         * @param  frac   position of reported value in bin;
         *                0 is lower bound, 1 is upper bound
         * @param  nameSuffix   suffix for column name, or null
         * @param  descripSuffix   trailing text for column description, or null
         * @param  secondaryUcd   S-type UCD that may be applied to column UCD,
         *                        or null
         */
        void addCoordColumn( int icoord, double frac,
                             String nameSuffix, String descripSuffix,
                             String secondaryUcd ) {
            ColumnInfo info = new ColumnInfo( cqInfos_[ icoord ] );
            info.setContentClass( Double.class );
            if ( nameSuffix != null ) {
                info.setName( info.getName() + nameSuffix );
            }
            if ( descripSuffix != null ) {
                String descrip = info.getDescription();
                if ( descrip == null || descrip.trim().length() == 0 ) {
                    descrip = "Coordinate";
                }
                info.setDescription( descrip + descripSuffix );
            }
            if ( secondaryUcd != null ) {
                String ucd = info.getUCD();
                if ( ucd != null ) {
                    info.setUCD( ucd + ";" + secondaryUcd );
                }
            }
            final BinMapper binMapper = binMappers_[ icoord ];
            addColumn( new ColumnData( info ) {
                public Double readValue( long lrow ) {
                    int ibin = rowControl_.getBinKey( lrow ).ibins_[ icoord ];
                    double[] limits = binMapper.getBinLimits( ibin );
                    double dval =
                        PlotUtil.scaleValue( limits[ 0 ], limits[ 1 ],
                                             frac, isLogs_[ icoord ] );
                    return new Double( dval );
                }
            } );
        }

        /**
         * Adds a numeric column giving the value of one of the
         * accumulated quantities.
         *
         * @param  iq  quantity index
         */
        void addQuantityColumn( int iq ) {
            int ndim = binMappers_.length;
            CombinedColumn qcol = qcols_[ iq ];
            ValueInfo cInfo =
                qcol.getCombiner()
                    .createCombinedInfo( cqInfos_[ ndim + iq ], (Unit) null );
            DefaultValueInfo dInfo = new DefaultValueInfo( cInfo );
            dInfo.setName( qcol.getName() );
            Class<?> dataClazz = dInfo.getContentClass();
            final DoubleFunction<Number> dataFunc;
            if ( Integer.class.equals( dataClazz ) ) {
                dataFunc = d -> Integer.valueOf( (int) d );
            }
            else if ( Short.class.equals( dataClazz ) ) {
                dataFunc = d -> Short.valueOf( (short) d );
            }
            else if ( Long.class.equals( dataClazz ) ) {
                dataFunc = d -> Long.valueOf( (long) d );
            }
            else {
                assert Double.class.equals( dataClazz );
                dataFunc = d -> Double.valueOf( d );
            }
            final Map<BinKey,Combiner.Container> qMap = qMaps_.get( iq );
            addColumn( new ColumnData( dInfo ) {
                public Object readValue( long lrow ) {
                    BinKey binKey = rowControl_.getBinKey( lrow );
                    Combiner.Container container = qMap.get( binKey );
                    if ( container == null ) {
                        return null;
                    }
                    else {
                        double dval = container.getCombinedValue();
                        return Double.isNaN( dval ) ? null
                                                    : dataFunc.apply( dval );
                    }
                }
            } );
        }
    }

    /**
     * Calculates numeric ranges for the first ndim columns of a given table.
     * The collected result is an array of ranges, one for each column.
     */
    private static class RangeCollector extends RowCollector<Range[]> {
        final int ndim_;

        /**
         * Constructor.
         *
         * @param  ndim  number of columns to calculate ranges for
         */
        RangeCollector( int ndim ) {
            ndim_ = ndim;
        }

        public Range[] createAccumulator() {
            Range[] ranges = new Range[ ndim_ ];
            for ( int i = 0; i < ndim_; i++ ) {
                ranges[ i ] = new Range();
            }
            return ranges;
        }

        public Range[] combine( Range[] ranges0, Range[] ranges1 ) {
            for ( int i = 0; i < ndim_; i++ ) {
                ranges0[ i ].extend( ranges1[ i ] );
            }
            return ranges1;
        }

        public void accumulateRows( RowSplittable rseq, Range[] ranges )
                throws IOException {
            while ( rseq.next() ) {
                for ( int i = 0; i < ndim_; i++ ) {
                    Object obj = rseq.getCell( i );
                    if ( obj instanceof Number ) {
                        ranges[ i ].submit( ((Number) obj).doubleValue() );
                    }
                }
            }
        }
    }

    /**
     * Performs quantity accumulation over a data table.
     * The input table must contain data coordinate values in the
     * first ndim columns, and quantities for accumulation in the
     * following nq columns.
     * The collected result is a list of binkey-&gt;container maps,
     * one for each accumulated quantity.
     */
    private static class BinsCollector
            extends RowCollector<List<Map<BinKey,Combiner.Container>>> {

        private final BinMapper[] binMappers_;
        private final Combiner[] qCombiners_;
        private final DoublePredicate[] coordTests_;
        private final int ndim_;
        private final int nq_;

        /**
         * Constructor.
         *
         * @param  binMappers  ndim-element array defining grid geometry
         * @param  isLogs      ndim-element array indicating linear/log axis
         * @param  qCombiners  nq-element array defining how accumulation
         *                     is done for accumulated colums
         * @param  loBounds    ndim-element array of grid fixed lower bounds,
         *                     elements may be NaN
         * @param  hiBounds    ndim-element array of grid fixed upper bounds,
         *                     elements may be NaN
         */
        BinsCollector( BinMapper[] binMappers, boolean[] isLogs,
                       Combiner[] qCombiners,
                       double[] loBounds, double[] hiBounds ) {
            binMappers_ = binMappers;
            qCombiners_ = qCombiners;
            ndim_ = binMappers.length;
            nq_ = qCombiners.length;

            /* Prepare to ignore input positions with unsuitable coordinates.
             * Exclude blank values, and any values outside the bins defined
             * by supplied bounds.  However, be careful:
             * (1) don't exclude values outside supplied bounds if they
             * are still in an included bin, since that could produce
             * misleading output, and
             * (2) take care not to add a new bin that only includes
             * a supplied upper bound that is exactly on its lower edge. */
            coordTests_ = new DoublePredicate[ ndim_ ];
            for ( int idim = 0; idim < ndim_; idim++ ) {
                BinMapper mapper = binMappers[ idim ];
                boolean isLog = isLogs[ idim ];
                double blo = loBounds[ idim ];
                double bhi = hiBounds[ idim ];
                final double dlo;
                if ( Double.isNaN( blo ) ) {
                    dlo = Double.NaN;
                }
                else {
                    dlo = getPointBinLimits( mapper, blo )[ 0 ];
                }
                final double dhi;
                if ( Double.isNaN( bhi ) ) {
                    dhi = Double.NaN;
                }
                else {
                    double[] hiBin = getPointBinLimits( mapper, bhi );
                    dhi = bhi == hiBin[ 0 ] ? hiBin[ 0 ] : hiBin[ 1 ];
                }
                coordTests_[ idim ] = createCoordTest( dlo, dhi, isLog );
            }
        }

        public List<Map<BinKey,Combiner.Container>> createAccumulator() {
            List<Map<BinKey,Combiner.Container>> qMaps = new ArrayList<>( nq_ );
            for ( int iq = 0; iq < nq_; iq++ ) {
                qMaps.add( new HashMap<BinKey,Combiner.Container>() );
            }
            return qMaps;
        }

        public List<Map<BinKey,Combiner.Container>>
               combine( List<Map<BinKey,Combiner.Container>> qMaps1,
                        List<Map<BinKey,Combiner.Container>> qMaps2 ) {
            List<Map<BinKey,Combiner.Container>> result = new ArrayList<>( nq_);
            for ( int iq = 0; iq < nq_; iq++ ) {
                boolean big1 = qMaps1.get( iq ).size()
                            >= qMaps2.get( iq ).size();
                Map<BinKey,Combiner.Container> mapA =
                    ( big1 ? qMaps1 : qMaps2 ).get( iq );
                Map<BinKey,Combiner.Container> mapB =
                    ( big1 ? qMaps2 : qMaps1 ).get( iq );
                for ( Map.Entry<BinKey,Combiner.Container> entryB :
                      mapB.entrySet() ) {
                    BinKey key = entryB.getKey();
                    Combiner.Container valB = entryB.getValue();
                    Combiner.Container valA = mapA.get( key );
                    if ( valA == null ) {
                        mapA.put( key, valB );
                    }
                    else {
                        valA.add( valB );
                    }
                }
                result.add( mapA );
            }
            return result;
        }

        public void accumulateRows( RowSplittable rseq,
                                    List<Map<BinKey,Combiner.Container>> qMaps )
                throws IOException {
            while ( rseq.next() ) {
                BinKey binKey = getBinKey( rseq );
                if ( binKey != null ) {
                    for ( int iq = 0; iq < nq_; iq++ ) {
                        Object qObj = rseq.getCell( ndim_ + iq );
                        if ( qObj instanceof Number ) {
                            double dq = ((Number) qObj).doubleValue();
                            if ( !Double.isNaN( dq ) ) {
                                Combiner combiner = qCombiners_[ iq ];
                                qMaps.get( iq )
                                     .computeIfAbsent( binKey,
                                                       k -> combiner
                                                           .createContainer() )
                                     .submit( dq );
                            }
                        }
                    }
                }
            }
        }

        /**
         * Returns a bin key identifying a grid cell for a table row.
         *
         * @param  rseq  table row
         * @return   map key, or null if no quantity should be accumulated
         */
        private BinKey getBinKey( RowSequence rseq ) throws IOException {
            int[] ibins = new int[ ndim_ ];
            for ( int idim = 0; idim < ndim_; idim++ ) {
                Object cObj = rseq.getCell( idim );
                if ( cObj instanceof Number ) {
                    double dc = ((Number) cObj).doubleValue();
                    if ( coordTests_[ idim ].test( dc ) ) {
                        int ix = binMappers_[ idim ].getBinIndex( dc );
                        ibins[ idim ] = ix;
                    }
                    else {
                        return null;
                    }
                }
                else {
                    return null;
                }
            }
            return new BinKey( ibins );
        }

        /**
         * Returns the limits of the mapper bin in which a given point appears.
         *
         * @param  mapper  bin mapper
         * @param  dval    test point
         * @return  2-element array giving bin (lower,upper) bounds,
         *          or null for dval NaN
         */
        private static double[] getPointBinLimits( BinMapper mapper,
                                                   double dval ) {
            return Double.isNaN( dval )
                 ? null
                 : mapper.getBinLimits( mapper.getBinIndex( dval ) );
        }

        /**
         * Returns a test indicating whether a coordinate value is
         * within the bounds of interest.
         *
         * @param   loBound  fixed lower bound, may be NaN
         * @param   hiBound  fixed upper bound, may be NaN
         * @param   isLog    true for logarithmic, false for linear
         * @return   function testing whether coordinate is usable
         */
        private static DoublePredicate createCoordTest( double loBound,
                                                        double hiBound,
                                                        boolean isLog ) {
            boolean hasLo = !Double.isNaN( loBound );
            boolean hasHi = !Double.isNaN( hiBound );
            if ( hasLo && hasHi ) {
                return d -> d >= loBound && d < hiBound;
            }
            else if ( hasLo ) {
                return d -> d >= loBound;
            }
            else if ( hasHi ) {
                return isLog ? d -> d < hiBound && d > 0
                             : d -> d < hiBound;
            }
            else {
                return isLog ? d -> d > 0
                             : d -> !Double.isNaN( d );
            }
        }
    }
}
