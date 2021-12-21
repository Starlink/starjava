package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.cone.HealpixTiling;
import uk.ac.starlink.ttools.cone.SkyTiling;
import uk.ac.starlink.ttools.cone.TilingParameter;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.JELQuantity;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;
import uk.ac.starlink.ttools.plot2.layer.BinList;
import uk.ac.starlink.ttools.plot2.layer.BinListCollector;
import uk.ac.starlink.ttools.plot2.layer.BinResultColumnData;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.plot2.layer.SolidAngleUnit;

/**
 * Calculates sky density maps and outputs them as tables.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2016
 */
public class SkyDensityMap extends SingleMapperTask {

    private final StringParameter lonParam_;
    private final StringParameter latParam_;
    private final TilingParameter tilingParam_;
    private final BooleanParameter countParam_;
    private final StringMultiParameter quantParam_;
    private final ChoiceParameter<Combiner> combinerParam_;
    private final ChoiceParameter<SolidAngleUnit> unitParam_;
    private final BooleanParameter completeParam_;

    /**
     * Constructor.
     */
    public SkyDensityMap() {
        super( "Calculates sky density maps", new ChoiceMode(), true, true );
        final String quantName = "cols";

        lonParam_ = new StringParameter( "lon" );
        lonParam_.setUsage( "<expr/deg>" );
        lonParam_.setPrompt( "Longitude coordinate in input table" );
        lonParam_.setDescription( new String[] {
            "<p>Longitude in degrees for the position of each row",
            "in the input table.",
            "This may simply be a column name, or it may be",
            "an algebraic expression as explained in <ref id='jel'/>.",
            "The sky system used here will determine the",
            "grid on which the output map is built.",
            "</p>",
        } );

        latParam_ = new StringParameter( "lat" );
        latParam_.setUsage( "<expr/deg>" );
        latParam_.setPrompt( "Latitude coordinate in input table" );
        latParam_.setDescription( new String[] {
            "<p>Latitude in degrees for the position of each row",
            "in the input table.",
            "This may simply be a column name, or it may be",
            "an algebraic expression as explained in <ref id='jel'/>.",
            "The sky system used here will determine the",
            "grid on which the output map is built.",
            "</p>",
        } );

        tilingParam_ = new TilingParameter( "tiling" );
        tilingParam_.setHealpixNestDefault( 5 );

        countParam_ = new BooleanParameter( "count" );
        countParam_.setPrompt( "Include count column?" );
        countParam_.setDescription( new String[] {
            "<p>Controls whether a COUNT column is added to the output table",
            "along with any other columns that may have been requested.",
            "If included, this reports the number of rows from the input table",
            "that fell within the corresponding bin.",
            "</p>",
        } );
        countParam_.setBooleanDefault( true );

        Combiner[] combiners = Combiner.getKnownCombiners();
        combinerParam_ = new ChoiceParameter<Combiner>( "combine", combiners );

        SolidAngleUnit[] units = SolidAngleUnit.getKnownUnits();
        unitParam_ = new ChoiceParameter<SolidAngleUnit>( "perunit", units );

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
            "<p>For density-like values",
            "(<code>" + Combiner.DENSITY + "</code>,",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code>)",
            "the scaling is additionally influenced by the",
            "<code>" + unitParam_.getName() + "</code> parameter.",
            "</p>",
            "<p>Note this value may be overridden on a per-column basis",
            "by the <code>" + quantName + "</code> parameter.",
            "</p>",
        } );
        combinerParam_.setDefaultOption( Combiner.MEAN );

        unitParam_.setPrompt( "Solid angle unit for densities" );
        StringBuffer ubuf = new StringBuffer();
        for ( SolidAngleUnit unit : units ) {
            ubuf.append( "<li>" )
                .append( "<code>" )
                .append( unit.getLabel() )
                .append( "</code>: " )
                .append( unit.getTextName() )
                .append( "</li>\n" );
        }
        unitParam_.setDescription( new String[] {
            "<p>Defines the unit of sky area used for scaling density-like",
            "combinations",
            "(e.g. <code>" + combinerParam_.getName() + "</code>=" +
            "<code>" + Combiner.DENSITY + "</code> or",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code>).",
            "If the combination mode is calculating values per unit area",
            "this configures the area scale in question.",
            "For non-density-like combination modes",
            "(e.g. <code>" + combinerParam_.getName() + "</code>=" +
            "<code>" + Combiner.SUM + "</code> or ",
            "<code>" + Combiner.MEAN + "</code>)",
            "it has no effect.",
            "</p>",
            "<p>Possible values are:",
            "<ul>",
            ubuf.toString(),
            "</ul>",
            "</p>",
        } );
        unitParam_.setDefaultOption( SolidAngleUnit.DEGREE2 );

        completeParam_ = new BooleanParameter( "complete" );
        completeParam_.setPrompt( "Write row for every pixel?" );
        completeParam_.setDescription( new String[] {
            "<p>Determines whether the output table contains a row",
            "for every pixel in the tiling, or only the rows for",
            "pixels in which some of the input data fell.",
            "</p>",
            "<p>The value of this parameter may affect performance as well",
            "as output.  If you know that most pixels on the sky will",
            "be covered, it's probably a good idea to set this true,",
            "and if you know that only a small patch of sky will be",
            "covered, it's better to set it false.",
            "</p>",
        } );
        completeParam_.setBooleanDefault( false );

        quantParam_ =
            CombinedColumn
           .createCombinedColumnsParameter( quantName, combinerParam_ );

        getParameterList().addAll( Arrays.asList( new Parameter<?>[] {
            lonParam_,
            latParam_,
            tilingParam_,
            countParam_,
            quantParam_,
            combinerParam_,
            unitParam_,
            completeParam_,
        } ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        String lonString = lonParam_.stringValue( env );
        String latString = latParam_.stringValue( env );
        SkyTiling tiling = tilingParam_.objectValue( env );
        String[] quants = quantParam_.stringsValue( env );
        Combiner dfltCombiner = combinerParam_.objectValue( env );
        SolidAngleUnit unit = unitParam_.objectValue( env );
        boolean complete = completeParam_.booleanValue( env );
        List<AggregateQuantity> aqList = new ArrayList<AggregateQuantity>();
        boolean hasCount = countParam_.booleanValue( env );
        final int countIndex;
        if ( hasCount ) {
            countIndex = aqList.size();
            aqList.add( new AggregateQuantity( Combiner.COUNT, "1" ) {
                public ValueInfo adjustInfo( ValueInfo combInfo ) {
                    DefaultValueInfo info = new DefaultValueInfo( combInfo );
                    info.setName( "count" );
                    info.setDescription( "number of input table rows in bin" );
                    info.setUnitString( null );
                    return info;
                }
            } );
        }
        else {
            countIndex = -1;
        }
        for ( String quantity : quants ) {
            CombinedColumn parsedCol =
                CombinedColumn
               .parseSpecification( quantity, quantParam_, combinerParam_ );
            String expr = parsedCol.getExpression();
            Combiner qCombiner = parsedCol.getCombiner();
            Combiner combiner = qCombiner == null ? dfltCombiner : qCombiner;
            String qName = parsedCol.getName();
            String label = qName == null
                         ? expr.replaceAll( "\\s+", "" )
                               .replaceAll( "[^0-9A-Za-z]+", "_" )
                         : qName;
            aqList.add( new AggregateQuantity( combiner, expr ) {
                public ValueInfo adjustInfo( ValueInfo combInfo ) {
                    DefaultValueInfo info = new DefaultValueInfo( combInfo );
                    info.setName( label );
                    return info;
                }
            } );
        }
        AggregateQuantity[] aqs = aqList.toArray( new AggregateQuantity[ 0 ] );
        if ( aqs.length == 0 ) {
            String msg = "No aggregate quantities to calculate; use parameters "
                       + quantParam_.getName().toUpperCase() + " or " 
                       + countParam_.getName().toUpperCase();
            throw new TaskException( msg );
        }
        final SingleTableMapping mapping =
            new SkyMapMapping( lonString, latString, tiling, complete, aqs,
                               unit, countIndex );
        final DescribedValue[] params;
        if ( tiling instanceof HealpixTiling ) {
            HealpixTiling hpx = (HealpixTiling) tiling;
            HealpixTableInfo.HpxCoordSys csys = null;
            HealpixTableInfo hpxInfo =
                new HealpixTableInfo( hpx.getHealpixK(), hpx.isNest(),
                                      hpx.getIndexInfo().getName(), csys );
            params = hpxInfo.toParams();
        }
        else {
            params = new DescribedValue[ 0 ];
        }
        final TableProducer inProd = createInputProducer( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return mapping.map( inProd.getTable() );
            }
        };
    }

    /**
     * Table mapping that bins quantities from a table into sky pixels,
     * resulting in a histogram table.
     */
    private static class SkyMapMapping implements SingleTableMapping {
        private final String lonStr_;
        private final String latStr_;
        private final SkyTiling tiling_;
        private final boolean complete_;
        private final AggregateQuantity[] aqs_;
        private final SolidAngleUnit unit_;
        private final int countIndex_;

        /**
         * Constructor.
         *
         * @param  lonStr  expression giving longitude coordinate in degrees
         * @param  latStr  expression giving latitude coordinate in degrees
         * @param  tiling  tiling that defines histogram bins on the sky
         * @param  complete  true to write all pixels,
         *                   false for only those represented in the input
         * @param  aqs     list of quantities to aggregate into bins
         * @param  unit    solid angle unit affecting density-like combiners
         * @param  countIndex  index of the <code>aqs</code> element that
         *                     just counts input table rows,
         *                     or -1 if none of the aqs does that
         */
        SkyMapMapping( String lonStr, String latStr, SkyTiling tiling,
                       boolean complete, AggregateQuantity[] aqs,
                       SolidAngleUnit unit, int countIndex ) {
            lonStr_ = lonStr;
            latStr_ = latStr;
            tiling_ = tiling;
            complete_ = complete;
            aqs_ = aqs;
            unit_ = unit;
            countIndex_ = countIndex;
        }

        public StarTable map( StarTable inTable )
                throws IOException, TaskException {

            /* Prepare to read coordinates. */
            Function<Library,CompiledExpression> lonCompiler;
            Function<Library,CompiledExpression> latCompiler;
            try {
                lonCompiler =
                    JELUtils.compiler( inTable, lonStr_, double.class );
                latCompiler =
                    JELUtils.compiler( inTable, latStr_, double.class );
            }
            catch ( CompilationException e ) {
                throw new TaskException( "Bad lon/lat value: " + e.getMessage(),
                                         e );
            }

            /* Prepare to read accumulated quantities and get the associated
             * output metadata. */
            List<Function<Library,CompiledExpression>> qCompilers =
                new ArrayList<>();
            List<ValueInfo> qInfos = new ArrayList<>();
            StarTableJELRowReader dummyRdr = new DummyJELRowReader( inTable );
            Library lib = JELUtils.getLibrary( dummyRdr );
            for ( AggregateQuantity aq : aqs_ ) {
                String qexpr = aq.expr_;
                JELQuantity jq;
                try {
                    qCompilers.add( JELUtils
                                   .compiler( inTable, qexpr, double.class ) );
                    jq = JELUtils
                        .compileQuantity( lib, dummyRdr, qexpr, double.class );
                }
                catch ( CompilationException e ) {
                    throw new TaskException( "Bad quantity value " + qexpr
                                           + ": " + e.getMessage(), e );
                }
                ValueInfo qInfo =
                    aq.adjustInfo( aq.combiner_
                       .createCombinedInfo( jq.getValueInfo(), unit_ ) );
                qInfos.add( qInfo );
            }

            /* Perform the aggregation in parallel. */
            BinDataCollector collector =
                new BinDataCollector( inTable, tiling_, aqs_,
                                      lonCompiler, latCompiler, qCompilers );
            RowRunner rowRunner = RowRunner.DEFAULT;
            BinData binData = rowRunner.collect( collector, inTable );

            /* Turn the result into a table. */
            long npix = tiling_.getPixelCount();
            ColumnStarTable binsTable =
                ColumnStarTable.makeTableWithRows( npix );
            binsTable.addColumn( createIndexColumn( tiling_ ) );
            long minIndex = npix;
            long maxIndex = 0;
            for ( int iq = 0; iq < aqs_.length; iq++ ) {
                AggregateQuantity aq = aqs_[ iq ];
                double binExtent = 4.0 * Math.PI / npix
                                 * ( 180 * 180 ) / ( Math.PI * Math.PI )
                                 / unit_.getExtentInSquareDegrees();
                double binFactor =
                    aq.combiner_.getType().getBinFactor( binExtent );
                ColumnData qData =
                    BinResultColumnData
                   .createInstance( qInfos.get( iq ),
                                    binData.binLists_[ iq ].getResult(),
                                    binFactor );
                binsTable.addColumn( qData );
            }

            /* Either use the table as is, with one row for each pixel. */
            final StarTable outTable;
            if ( complete_ ) {
                outTable = binsTable;
            }

            /* Or prepare a compressed version, where only those rows with
             * some data in the non-pixel-index columns are included. */
            else {
                int icolOffset = 1; // pixel index column
                final int[] testIcols;
                if ( countIndex_ >= 0 ) {
                    testIcols = new int[] { icolOffset + countIndex_ };
                }
                else {
                    int ncol = binsTable.getColumnCount();
                    testIcols = new int[ ncol - icolOffset ];
                    for ( int i = 0; i < testIcols.length; i++ ) {
                        testIcols[ i ] = icolOffset + i;
                    }
                }
                outTable =
                    new UnsparseTable( binsTable,
                                       binData.minIndex_, binData.maxIndex_,
                                       testIcols );
            }

            /* Add HEALPix-specific metadata if applicable. */
            if ( tiling_ instanceof HealpixTiling ) {
                HealpixTiling hpxTiling = (HealpixTiling) tiling_;
                HealpixTableInfo hpxInfo =
                    new HealpixTableInfo( hpxTiling.getHealpixK(),
                                          hpxTiling.isNest(),
                                          hpxTiling.getIndexInfo().getName(),
                                          (HealpixTableInfo.HpxCoordSys) null );
                outTable.getParameters()
                        .addAll( Arrays.asList( hpxInfo.toParams() ) );
            }
            return outTable;
        }
    }

    /**
     * Utility method to evaluate a double-precision compiled expression
     * rethrowing exceptions for convenience.
     * 
     * @param  jelReader  row reader
     * @param  compEx  expression known to be of type double
     * @return   expression value
     */
    private static double doEvaluateDouble( JELRowReader jelReader,
                                            CompiledExpression compEx )
            throws IOException {
        try {
            return jelReader.evaluateDouble( compEx );
        }
        catch ( Throwable e ) {
            throw new IOException( "Evaluation error", e );
        }
    }

    /**
     * Creates a table column that reports pixel index for a given tiling.
     * The data content is just equivalent to the (zero-based) row index,
     * but the column metadata should be appropriately informative.
     * The content class will be Integer if possible, otherwise Long.
     *
     * @param  tiling   tiling scheme
     * @return   pixel index column
     */
    private static ColumnData createIndexColumn( SkyTiling tiling ) {
        DefaultValueInfo info = new DefaultValueInfo( tiling.getIndexInfo() );
        if ( tiling.getPixelCount() <= Integer.MAX_VALUE ) {
            info.setContentClass( Integer.class );
            return new ColumnData( info ) {
                public Object readValue( long irow ) {
                    return new Integer( (int) irow );
                }
            };
        }
        else {
            info.setContentClass( Long.class );
            return new ColumnData( info ) {
                public Object readValue( long irow ) {
                    return new Long( irow );
                }
            };
        }
    }

    /**
     * Aggregates a combiner and a quantity to evaluate.
     * This defines the requirements for a given binned output column.
     */
    private static abstract class AggregateQuantity {
        final Combiner combiner_;
        final String expr_;

        /**
         * Constructor.
         *
         * @param  combiner  combination mode
         * @param  expr     expression to evaluate giving aggregated quantity
         */
        AggregateQuantity( Combiner combiner, String expr ) {
            combiner_ = combiner;
            expr_ = expr;
        }

        /**
         * Returns a metadata item describing the accumulated result column,
         * given a basic metadata item.
         * This method gives this object the chance to tweak column names
         * and descriptions etc.
         *
         *
         * @param   combinedInfo  metadata for basic combined result
         * @return  (possibly) adjusted metadat for combined result
         */
        abstract ValueInfo adjustInfo( ValueInfo combinedInfo );
    }

    /**
     * Accumulator class for binning.
     */
    private static class BinData {
        BinList[] binLists_;
        long minIndex_;
        long maxIndex_;

        /**
         * Constructor.
         *
         * @param  binLists  array of BinList objects for accumulation
         * @param  npix   number of bins in each binlist
         */
        BinData( BinList[] binLists, long npix ) {
            binLists_ = binLists;
            minIndex_ = npix;
            maxIndex_ = -1;
        }
    }

    /**
     * Collector that performs sky map aggregation.
     */
    private static class BinDataCollector extends RowCollector<BinData> {
        final StarTable table_;
        final SkyTiling tiling_;
        final AggregateQuantity[] aqs_;
        final Function<Library,CompiledExpression> lonCompiler_;
        final Function<Library,CompiledExpression> latCompiler_;
        final List<Function<Library,CompiledExpression>> quantCompilers_;
        final int nq_;
        final long npix_;

        /**
         * Constructor.
         *
         * @param  table   table contaning data
         * @param  tiling  sky tiling
         * @param  aqs    array of quantities to aggregate
         * @param  lonCompiler   compiler for longitude expression in degrees
         * @param  latCompiler   compiler for latitude expression in degrees
         * @param  qComps   compilers for aggraged quanties
         *
         */
        BinDataCollector( StarTable table, SkyTiling tiling,
                          AggregateQuantity[] aqs,
                          Function<Library,CompiledExpression> lonCompiler,
                          Function<Library,CompiledExpression> latCompiler,
                          List<Function<Library,CompiledExpression>> qComps ) {
            table_ = table;
            tiling_ = tiling;
            aqs_ = aqs;
            lonCompiler_ = lonCompiler;
            latCompiler_ = latCompiler;
            quantCompilers_ = qComps;
            nq_ = aqs_.length;
            npix_ = tiling_.getPixelCount();
        }

        public BinData createAccumulator() {
            BinList[] binLists =
                Arrays
               .stream( aqs_ )
               .map( aq -> BinListCollector
                          .createDefaultBinList( aq.combiner_, npix_ ) )
               .collect( Collectors.toList() )
               .toArray( new BinList[ nq_ ] );
            return new BinData( binLists, npix_ );
        }

        public BinData combine( BinData binData1, BinData binData2 ) {
            BinList[] bls1 = binData1.binLists_;
            BinList[] bls2 = binData2.binLists_;
            for ( int iq = 0; iq < nq_; iq++ ) {
                bls1[ iq ] = BinListCollector
                            .mergeBinLists( bls1[ iq ], bls2[ iq ] );
            }
            binData1.minIndex_ =
                Math.min( binData1.minIndex_, binData2.minIndex_ );
            binData1.maxIndex_ =
                Math.max( binData1.maxIndex_, binData2.maxIndex_ );
            return binData1;
        }

        public void accumulateRows( RowSplittable rseq, BinData binData )
                throws IOException {
            SequentialJELRowReader jelRdr =
                new SequentialJELRowReader( table_, rseq );
            Library lib = JELUtils.getLibrary( jelRdr );
            CompiledExpression lonExpr = lonCompiler_.apply( lib );
            CompiledExpression latExpr = latCompiler_.apply( lib );
            int nq = aqs_.length;
            CompiledExpression[] quantExprs = new CompiledExpression[ nq ];
            for ( int iq = 0; iq < nq; iq++ ) {
                quantExprs[ iq ] = quantCompilers_.get( iq ).apply( lib );
            }
            long minIndex = binData.minIndex_;
            long maxIndex = binData.maxIndex_;
            BinList[] binLists = binData.binLists_;
            while ( rseq.next() ) {
                double lon = doEvaluateDouble( jelRdr, lonExpr );
                double lat = doEvaluateDouble( jelRdr, latExpr );
                if ( ! Double.isNaN( lon ) && ! Double.isNaN( lat ) ) {
                    long index = tiling_.getPositionTile( lon, lat );
                    minIndex = Math.min( minIndex, index );
                    maxIndex = Math.max( maxIndex, index );
                    for ( int iq = 0; iq < nq; iq++ ) {
                        double datum =
                            doEvaluateDouble( jelRdr, quantExprs[ iq ] );
                        if ( ! Double.isNaN( datum ) ) {
                            binLists[ iq ].submitToBin( index, datum );
                        }
                    }
                }
            }
            binData.minIndex_ = minIndex;
            binData.maxIndex_ = maxIndex;
        }
    }

    /**
     * Wrapper table that includes only interesting rows from a base table.
     */
    private static class UnsparseTable extends WrapperStarTable {

        private final StarTable base_;
        private final long minIrow_;
        private final long maxIrow_;
        private final int[] testIcols_;

        /**
         * Constructor.
         *
         * @param  base  base table
         * @param  minIrow  minimum row index (inclusive) of base table to
         *                  be included
         * @param  maxIrow  maximum row index (inclusive) of base table to
         *                  be included
         * @param  testIcols  array of column indices from the base table;
         *                    only rows with a non-blank value for at least
         *                    one of these columns will be included
         */
        UnsparseTable( StarTable base, long minIrow, long maxIrow,
                       int[] testIcols ) {
            super( base );
            base_ = base;
            minIrow_ = minIrow;
            maxIrow_ = maxIrow;
            testIcols_ = testIcols;
        }

        @Override
        public RowSequence getRowSequence() throws IOException {
            return new RowSequence() {
                long irow = minIrow_ - 1;
                public boolean next() throws IOException {
                    while ( ! hasData( ++irow ) ) {
                        if ( irow > maxIrow_ ) {
                            return false;
                        }
                    }
                    return true;
                }
                public Object getCell( int icol ) throws IOException {
                    checkStarted();
                    return base_.getCell( irow, icol );
                }
                public Object[] getRow() throws IOException {
                    checkStarted();
                    return base_.getRow( irow );
                }
                public void close() {
                }
                private void checkStarted() {
                    if ( irow < minIrow_ ) {
                        throw new IllegalStateException( "next not called" );
                    }
                }
            };
        }

        public RowSplittable getRowSplittable() throws IOException {
            return Tables.getDefaultRowSplittable( this );
        }

        /**
         * Indicates whether a given row from the base table has content
         * that's interesting enough to be included in the output table.
         *
         * @param  irow  base table row index
         * @return  true iff at least one of the test columns is non-blank
         *               for the specified row
         */
        private boolean hasData( long irow ) throws IOException {
            for ( int ic : testIcols_ ) {
                if ( ! Tables.isBlank( base_.getCell( irow, ic ) ) ) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isRandom() {
            return false;
        }

        @Override
        public long getRowCount() {
            return -1;
        }

        @Override
        public RowAccess getRowAccess() throws IOException {
            throw new UnsupportedOperationException( "not random" );
        }

        @Override
        public Object getCell( long irow, int icell ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] getRow( long irow ) {
            throw new UnsupportedOperationException();
        }
    }
}
