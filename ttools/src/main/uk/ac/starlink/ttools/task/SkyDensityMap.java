package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
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
import uk.ac.starlink.ttools.cone.SkyTiling;
import uk.ac.starlink.ttools.cone.TilingParameter;
import uk.ac.starlink.ttools.jel.JELQuantity;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.plot2.layer.BinList;
import uk.ac.starlink.ttools.plot2.layer.BinResultColumnData;
import uk.ac.starlink.ttools.plot2.layer.Combiner;

/**
 * Calculates sky density maps and outputs them as tables.
 *
 * <p>The current implementation has the limitation that the same
 * combiner has to be used for all aggregated quantities.
 * If you want to calculate for instance the median of some quantities
 * and the mean of others, you're out of luck.  It's hard to work out
 * what the parameters would look like to do it more generally.
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
    private final BooleanParameter completeParam_;
    private static final int MAX_ARRAY = 1000000;

    /**
     * Constructor.
     */
    public SkyDensityMap() {
        super( "Calculates sky density maps", new ChoiceMode(), true, true );

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
            "<p>Combination mode for aggregating binned quantities.",
            "Possible values are:",
            "<ul>",
            lbuf.toString(),
            "</ul>",
            "</p>",
        } );
        combinerParam_.setDefaultOption( Combiner.MEAN );

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

        quantParam_ = new StringMultiParameter( "cols", ' ' );
        quantParam_.setPrompt( "Quantities to aggregate" );
        quantParam_.setUsage( "<expr> ..." );
        quantParam_.setDescription( new String[] {
            "<p>Selects the columns to be aggregated into bins.",
            "The value is a space-separated list of items,",
            "where each item may be either a column name",
            "or an expression using the",
            "<ref id='jel'>expression language</ref>.",
            "The output table will have one column for each of the",
            "items in this list.",
            "</p>",
        } );
        quantParam_.setNullPermitted( true );

        getParameterList().addAll( Arrays.asList( new Parameter[] {
            lonParam_,
            latParam_,
            tilingParam_,
            countParam_,
            quantParam_,
            combinerParam_,
            completeParam_,
        } ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        String lonString = lonParam_.stringValue( env );
        String latString = latParam_.stringValue( env );
        SkyTiling tiling = tilingParam_.objectValue( env );
        String[] quants = quantParam_.stringsValue( env );
        Combiner combiner = combinerParam_.objectValue( env );
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
            String expr = quantity;
            final String label = quantity.replaceAll( "\\s+", "" )
                                         .replaceAll( "[^0-9A-Za-z]+", "_" );
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
                               countIndex );
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
        private final int countIndex_;

        /**
         * Constructor.
         *
         * @param  lonStr  expression giving longitude coordinate in degrees
         * @param  latStr  expression giving latitude coordinate in degrees
         * @param  tiling  tilling that defines histogram bins on the sky
         * @param  complete  true to write all pixels,
         *                   false for only those represented in the input
         * @param  aqs     list of quantities to aggregate into bins
         * @param  countIndex  index of the <code>aqs</code> element that
         *                     just counts input table rows,
         *                     or -1 if none of the aqs does that
         */
        SkyMapMapping( String lonStr, String latStr, SkyTiling tiling,
                       boolean complete, AggregateQuantity[] aqs,
                       int countIndex ) {
            lonStr_ = lonStr;
            latStr_ = latStr;
            tiling_ = tiling;
            complete_ = complete;
            aqs_ = aqs;
            countIndex_ = countIndex;
        }

        public StarTable map( StarTable inTable )
                throws IOException, TaskException {
            SequentialJELRowReader jelReader =
                new SequentialJELRowReader( inTable );
            Library lib = JELUtils.getLibrary( jelReader );

            /* Acquire accessors for sky position. */
            CompiledExpression lonExpr;
            CompiledExpression latExpr;
            try {
                lonExpr =
                    JELUtils.compile( lib, inTable, lonStr_, double.class );
                latExpr =
                    JELUtils.compile( lib, inTable, latStr_, double.class );
            }
            catch ( CompilationException e ) {
                throw new TaskException( "Bad lon/lat value: " + e.getMessage(),
                                         e );
            }

            /* Acquire accessors for quantities to be aggregated and
             * accumulators to do the work of aggregating them. */
            int nq = aqs_.length;
            long npix = tiling_.getPixelCount();
            Binner[] binners = new Binner[ nq ];
            for ( int iq = 0; iq < nq; iq++ ) {
                AggregateQuantity aq = aqs_[ iq ];
                Combiner combiner = aq.combiner_;
                String expr = aq.expr_;
                BinList binList = createBinList( combiner, npix, complete_ );
                JELQuantity jq;
                try {
                    jq = JELUtils.compileQuantity( lib, jelReader, expr,
                                                   double.class );
                }
                catch ( CompilationException e ) {
                    throw new TaskException( "Bad quantity value " + expr
                                           + ": " + e.getMessage(), e );
                }
                ValueInfo info =
                    aq.adjustInfo( combiner
                                  .createCombinedInfo( jq.getValueInfo() ) );
                CompiledExpression compEx = jq.getCompiledExpression();
                binners[ iq ] = new Binner( info, binList, compEx );
            }

            /* Iterate over input table rows, determining sky pixel index
             * and accumulating the required values for each row. */
            long minIndex = npix;
            long maxIndex = 0;
            try {
                while ( jelReader.next() ) {
                    double lon = doEvaluateDouble( jelReader, lonExpr );
                    double lat = doEvaluateDouble( jelReader, latExpr );
                    long index = tiling_.getPositionTile( lon, lat );
                    minIndex = Math.min( minIndex, index );
                    maxIndex = Math.max( maxIndex, index );
                    for ( Binner binner : binners ) {
                        double datum =
                            doEvaluateDouble( jelReader, binner.compEx_ );
                        if ( ! Double.isNaN( datum ) ) {
                            binner.binList_.submitToBin( index, datum );
                        }
                    }
                }
            }
            finally {
                jelReader.close();
            }

            /* Turn the result into a table. */
            ColumnStarTable binsTable =
                ColumnStarTable.makeTableWithRows( npix );
            binsTable.addColumn( createIndexColumn( tiling_ ) );
            for ( Binner binner : binners ) {
                binsTable.addColumn( BinResultColumnData
                                    .createInstance( binner.info_,
                                                     binner.binList_
                                                           .getResult() ) );
            }

            /* Either output the table as is, with one row for each pixel. */
            if ( complete_ ) {
                return binsTable;
            }

            /* Or output a compressed version, where only those rows with
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
                return new UnsparseTable( binsTable, minIndex, maxIndex,
                                          testIcols );
            }
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
     * Creates an accumulator for a given combination mode.
     *
     * @param  combiner  combiner
     * @param  npix    (maximum) number of bins required
     * @param  isComplete  hint about whether values are likely to be
     *                     caculated for all pixels
     */
    private static BinList createBinList( Combiner combiner, long npix,
                                          boolean isComplete ) {
        if ( isComplete && npix < MAX_ARRAY ) {
            BinList binList = combiner.createArrayBinList( (int) npix );
            if ( binList != null ) {
                return binList;
            }
        }
        return combiner.createHashBinList( npix );
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
     * Aggregates information required for accumulating evaluations of
     * an expression into bins.
     */
    private static class Binner {
        final ValueInfo info_;
        final BinList binList_;
        final CompiledExpression compEx_;

        /**
         * Constructor.
         *
         * @param  info  metadata for the accumulated value
         * @param  binList   accumulator instance
         * @param  compEx   value accessor
         */
        Binner( ValueInfo info, BinList binList, CompiledExpression compEx ) {
            info_ = info;
            binList_ = binList;
            compEx_ = compEx;
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
                    return base_.getCell( irow, icol );
                }
                public Object[] getRow() throws IOException {
                    return base_.getRow( irow );
                }
                public void close() {
                }
            };
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
        public Object getCell( long irow, int icell ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] getRow( long irow ) {
            throw new UnsupportedOperationException();
        }
    }
}
