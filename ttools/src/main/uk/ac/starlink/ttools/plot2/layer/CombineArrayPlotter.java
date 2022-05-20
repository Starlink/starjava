package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Partial Plotter implementation for plot types that combine multiple
 * all the array-valued X and Y coordinates in a data set, and make
 * some plot from the resulting combination.  Combination is typically
 * a mean, but can be something else.
 *
 * <p>This plotter is written to cope with blank values for one or other
 * of the X/Y coordinates; if one is blank, it taken to indicate
 * a sequence of values 0, 1, 2, ..., which is probably reasonable for a plot.
 * At present, it will never be fed null values, since FloatingArrayCoord.X/Y
 * are marked required, but if the input coordinates are changed to be
 * optional at some point, it ought to work with that.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2022
 */
public abstract class CombineArrayPlotter
                      <S extends CombineArrayPlotter.CombineArrayStyle>
        extends AbstractPlotter<S> {

    private static final FloatingArrayCoord XS_COORD = FloatingArrayCoord.X;
    private static final FloatingArrayCoord YS_COORD = FloatingArrayCoord.Y;
    private static final int IC_XS = 0;
    private static final int IC_YS = 1;

    /** Configuration key for X coordinate combination mode. */
    public static final ConfigKey<Combiner> XCOMBINER_KEY =
        createCombinerKey( false );

    /** Configuration key for Y coordinate combination mode. */
    public static final ConfigKey<Combiner> YCOMBINER_KEY =
        createCombinerKey( true );

    /** Report key for combined X array values. */
    public static final ReportKey<double[]> XS_REPKEY =
        ReportKey.createUnprintableKey( new ReportMeta( "xs", "X Values" ),
                                        double[].class );

    /** Report key for combined Y array values. */
    public static final ReportKey<double[]> YS_REPKEY =
        ReportKey.createUnprintableKey( new ReportMeta( "ys", "Y Values" ),
                                        double[].class );

    /**
     * Constructor.
     *
     * @param  name  plotter name
     * @param  icon  plotter icon
     */
    protected CombineArrayPlotter( String name, Icon icon ) {
        super( name, icon,
               CoordGroup
              .createPartialCoordGroup( new Coord[] { XS_COORD, YS_COORD },
                                        new boolean[] { true, true } ),
               true );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final CombineArrayStyle style ) {

        /* One or other coordinate can be taken to be a sequence 0, 1, 2,... */
        final boolean hasX = ! dataSpec.isCoordBlank( IC_XS );
        final boolean hasY = ! dataSpec.isCoordBlank( IC_YS );
        if ( ! hasX && ! hasY ) {
            return null;
        }
        return new AbstractPlotLayer( this, geom, dataSpec, style,
                                      style.getLayerOpt() ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          PaperType paperType ) {
                return new CombineArrayDrawing( (PlanarSurface) surface, geom,
                                                dataSpec, style, paperType );
            }
            @Override
            public void extendCoordinateRanges( Range[] ranges,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {
                super.extendCoordinateRanges( ranges, logFlags, dataStore );
                XYData xyData = collectXYData( style, dataSpec, dataStore );
                int np = xyData.nbin_;
                if ( np > 0 ) {
                    Range xRange = ranges[ 0 ];
                    Range yRange = ranges[ 1 ];
                    BinList.Result xResult = xyData.xBins_.getResult();
                    BinList.Result yResult = xyData.yBins_.getResult();
                    for ( int ip = 0; ip < np; ip++ ) {
                        xRange.submit( xResult.getBinValue( ip ) );
                        yRange.submit( yResult.getBinValue( ip ) );
                    }
                }
            }
        };
    }

    @Override
    public Object getRangeStyleKey( S style ) {
        return Arrays.asList( style.getCombinerX(), style.getCombinerY() );
    }

    /**
     * Creates a config key for the combiner to use on one axis.
     *
     * @param  isY  false for X axis, true for Y axis
     * @return  config key
     */
    private static ConfigKey<Combiner> createCombinerKey( boolean isY ) {
        char axischar = isY ? 'y' : 'x';
        char axisChar = Character.toUpperCase( axischar );
        ConfigMeta meta = new ConfigMeta( axischar + "combine",
                                          axisChar + " Combine" );
        meta.setShortDescription( axisChar + " axis combination mode" );
        meta.setXmlDescription( PlotUtil.concatLines( new String[] {
            "<p>Defines how corresponding array elements on the " + axisChar,
            "axis are combined together to produce the plotted value.",
            "</p>",
        } ) );
        Combiner[] options = new Combiner[] {
            Combiner.MEAN,
            Combiner.MEDIAN,
            Combiner.MIN,
            Combiner.MAX,
            Combiner.Q01,
            Combiner.Q1,
            Combiner.Q3,
            Combiner.Q99,
            Combiner.SAMPLE_STDEV,
            Combiner.SUM,
            Combiner.COUNT,
        };
        return new OptionConfigKey<Combiner>( meta, Combiner.class, options,
                                              Combiner.MEAN ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        }.setOptionUsage()
         .addOptionsXml();
    }

    /**
     * Returns a bin list whose bins will contain simple sequence values
     * 0, 1, 2, ...
     *
     * @param  nbin   bin count
     * @return   new bin list
     */
    private static BinList createSequenceBinList( int nbin ) {
        BinList binlist =
            BinListCollector.createDefaultBinList( Combiner.MAX, nbin );
        for ( int i = 0; i < nbin; i++ ) {
            binlist.submitToBin( i, i );
        }
        return binlist;
    }

    /**
     * Acquires the XYData object that contains the positions to be plotted.
     *
     * @param  style  style
     * @param  dataSpec   data spec
     * @param  dataStore  data store
     * @return  xydata
     */
    private static XYData collectXYData( CombineArrayStyle style,
                                         DataSpec dataSpec,
                                         DataStore dataStore ) {

        /* Work out which array coordinates to actually accumulate. */
        boolean xBlank = dataSpec.isCoordBlank( IC_XS );
        boolean yBlank = dataSpec.isCoordBlank( IC_YS );
        if ( xBlank && yBlank ) {
            assert false;   // shouldn't have got this far
            XYData xyData = new XYData();
            xyData.nbin_ = -1;
            return xyData;
        }

        /* Do the accumulation. */
        Combiner combinerX = xBlank ? null : style.getCombinerX();
        Combiner combinerY = yBlank ? null : style.getCombinerY();
        XYData xyData =
            PlotUtil.tupleCollect( new XYCollector( combinerX, combinerY ),
                                   dataSpec, dataStore );

        /* Fill in sequence values if either of the input coords was blank. */
        int nbin = xyData.nbin_;
        if ( nbin > 0 ) {
            if ( xBlank ) {
                xyData.xBins_ = createSequenceBinList( nbin );
            }
            if ( yBlank ) {
                xyData.yBins_ = createSequenceBinList( nbin );
            }
        }
        return xyData;
    }

    /**
     * Partial Style implementation for use with this class.
     */
    public static abstract class CombineArrayStyle implements Style {

        private final Combiner xCombiner_;
        private final Combiner yCombiner_;
        private final LayerOpt layerOpt_;

        /**
         * Constructor.
         *
         * @param  xCombiner  combiner for elements of X array values
         * @param  yCombiner  combiner for elements of Y array values
         * @param  layerOpt   layerOpt
         */
        protected CombineArrayStyle( Combiner xCombiner, Combiner yCombiner,
                                     LayerOpt layerOpt ) {
            xCombiner_ = xCombiner;
            yCombiner_ = yCombiner;
            layerOpt_ = layerOpt;
        }

        /**
         * Returns the combination mode for elements of X array values.
         *
         * @return  X combiner
         */
        public Combiner getCombinerX() {
            return xCombiner_;
        }

        /**
         * Returns the combination mode for elements of Y array values.
         *
         * @return  Y combiner
         */
        public Combiner getCombinerY() {
            return yCombiner_;
        }

        /**
         * Returns the LayerOpt for this style.
         *
         * @return layer opt
         */
        public LayerOpt getLayerOpt() {
            return layerOpt_;
        }

        /**
         * Do the actual plotting given an array of graphics points
         * corresponding to the combined array elements.
         *
         * @param  surface  plot surface
         * @param  paperType  paper type
         * @param  paper     paper
         * @param  points   points in graphics coordinates corresponding to
         *                  the aggregated per-element values of the input
         *                  X and Y array coordinates
         */
        public abstract void paintPoints( PlanarSurface surface,
                                          PaperType paperType, Paper paper,
                                          Point2D.Double[] points );

        @Override
        public int hashCode() {
            int code = 232357;
            code = 23 * code + xCombiner_.hashCode();
            code = 23 * code + yCombiner_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CombineArrayStyle ) {
                CombineArrayStyle other = (CombineArrayStyle) o;
                return this.xCombiner_.equals( other.xCombiner_ )
                    && this.yCombiner_.equals( other.yCombiner_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Drawing implementation for use with this class.
     */
    private static class CombineArrayDrawing implements Drawing {

        final PlanarSurface surface_;
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final CombineArrayStyle style_;
        final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data geom
         * @param  dataSpec   data spec
         * @param  style   style
         * @param  paperType  paper type
         */
        CombineArrayDrawing( PlanarSurface surface, DataGeom geom,
                             DataSpec dataSpec, CombineArrayStyle style,
                             PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            paperType_ = paperType;
        }

        public CombineArrayPlan calculatePlan( Object[] knownPlans,
                                               DataStore dataStore ) {
            Combiner xCombiner = style_.getCombinerX();
            Combiner yCombiner = style_.getCombinerY();
            for ( Object knownPlan : knownPlans ) {
                if ( knownPlan instanceof CombineArrayPlan &&
                     ((CombineArrayPlan) knownPlan)
                    .matches( dataSpec_, xCombiner, yCombiner ) ) {
                    return (CombineArrayPlan) knownPlan;
                }
            }
            XYData xyData = collectXYData( style_, dataSpec_, dataStore );
            int nbin = xyData.nbin_;
            final double[] dxs;
            final double[] dys;
            if ( nbin > 0 ) {
                BinList.Result xResult = xyData.xBins_.getResult();
                BinList.Result yResult = xyData.yBins_.getResult();
                dxs = new double[ nbin ];
                dys = new double[ nbin ];
                for ( int ib = 0; ib < nbin; ib++ ) {
                    dxs[ ib ] = xResult.getBinValue( ib );
                    dys[ ib ] = yResult.getBinValue( ib );
                }
            }
            else {
                dxs = null;
                dys = null;
            }
            return new CombineArrayPlan( dataSpec_, xCombiner, yCombiner,
                                         dxs, dys );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            Point2D.Double[] points =
                ((CombineArrayPlan) plan).getPoints( surface_ );
            if ( points != null ) {
                style_.paintPoints( surface_, paperType_, paper, points );
            }
        }

        public ReportMap getReport( Object plan ) {
            ReportMap report = new ReportMap();
            if ( plan instanceof CombineArrayPlan ) {
                CombineArrayPlan cplan = (CombineArrayPlan) plan;
                if ( cplan.dxs_ != null ) {

                    /* These reports are not currently used anywhere,
                     * but they seem like reasonable aggregations to make
                     * available. */
                    report.put( XS_REPKEY, cplan.dxs_ );
                    report.put( YS_REPKEY, cplan.dys_ );
                }
            }
            return report;
        }
    }

    /**
     * Collector that aggregates per-element statistics from the input
     * array-valued coordinates.
     */
    private static class XYCollector
            implements SplitCollector<TupleSequence,XYData> {

        final Combiner xCombiner_;
        final Combiner yCombiner_;
        final boolean hasX_;
        final boolean hasY_;

        /**
         * Constructor.  One or other combiner may be null in which case
         * a sequence result array of the right length is returned,
         * but there's no point in supplying them both null.
         *
         * @param  xCombiner  combiner for X input array coordinate,
         *                    or null for no X aggregation
         * @param  yCombiner  combiner for Y input array coordinate,
         *                    or null for no Y aggregation
         */
        XYCollector( Combiner xCombiner, Combiner yCombiner ) {
            xCombiner_ = xCombiner;
            yCombiner_ = yCombiner;
            hasX_ = xCombiner != null;
            hasY_ = yCombiner != null;
        }
        public XYData createAccumulator() {
            return new XYData();
        }
        public void accumulate( TupleSequence tseq, XYData xyData ) {

            /* If we have inconsistent array lengths, bail out. */
            if ( xyData.nbin_ < 0 ) {
                return;
            }

            /* Otherwise accumulate. */
            while ( tseq.next() ) {
                int npx = XS_COORD.getArrayCoordLength( tseq, IC_XS );
                int npy = YS_COORD.getArrayCoordLength( tseq, IC_YS );
                if ( ( npx > 0 || !hasX_ ) && ( npy > 0 || !hasY_ ) ) {
                    if ( npx == npy || !hasX_ || !hasY_ ) {
                        int np = hasX_ ? npx : npy;
                        if ( xyData.nbin_ == 0 ) {
                            initBinCount( xyData, np );
                        }
                        if ( xyData.nbin_ != np ) {
                            initBinCount( xyData, -1 );
                            return;
                        }
                        if ( hasX_ ) {
                            submitArray( XS_COORD.readArrayCoord( tseq, IC_XS ),
                                         npx, xyData.xBins_ );
                        }
                        if ( hasY_ ) {
                            submitArray( YS_COORD.readArrayCoord( tseq, IC_YS ),
                                         npy, xyData.yBins_ );
                        }
                    }
                    else {
                        initBinCount( xyData, -1 );
                        return;
                    }
                }
            }
        }

        public XYData combine( XYData xydata1, XYData xydata2 ) {
            int nbin1 = xydata1.nbin_;
            int nbin2 = xydata2.nbin_;

            /* If either or both input accumulator is broken, just return it. */
            if ( nbin1 < 0 ) {
                return xydata1;
            }
            else if ( nbin2 < 0 ) {
                return xydata2;
            }

            /* If one input accumulator has no data, return the other one. */
            else if ( nbin1 == 0 ) {
                return xydata2;
            }
            else if ( nbin2 == 0 ) {
                return xydata1;
            }

            /* If the input accumulators are inconsistent, return broken one. */
            else if ( nbin1 != nbin2 ) {
                initBinCount( xydata1, -1 );
                return xydata1;
            }

            /* Otherwise, merge and return. */
            else {
                if ( hasX_ ) {
                    xydata1.xBins_ = BinListCollector
                                    .mergeBinLists( xydata1.xBins_,
                                                    xydata2.xBins_ );
                }
                if ( hasY_ ) {
                    xydata1.yBins_ = BinListCollector
                                    .mergeBinLists( xydata1.yBins_,
                                                    xydata2.yBins_ );
                }
                return xydata1;
            }
        }

        /**
         * Configure an XYData object to cope with a given bin count.
         * Accumulation only works if all the encountered arrays are
         * the same fixed length.
         *
         * @param  xyData  accumulation object
         * @param  nbin    new bin count; &gt;0 for actual value,
         *                 or -1 to signal failed accumulation
         */
        private void initBinCount( XYData xyData, int nbin ) {
            xyData.nbin_ = nbin;
            if ( nbin > 0 ) {
                if ( hasX_ ) {
                    xyData.xBins_ = BinListCollector
                                   .createDefaultBinList( xCombiner_, nbin );
                }
                if ( hasY_ ) {
                    xyData.yBins_ = BinListCollector
                                   .createDefaultBinList( yCombiner_, nbin );
                }
            }
            else {
                xyData.xBins_ = null;
                xyData.yBins_ = null;
            }
        }

        /**
         * Adds an array of samples to the corresponding bins of a given
         * bin list.
         *
         * @param  samples  data array
         * @param  np    size of data array
         * @param  binList  list of bins for data accumulation
         */
        private static void submitArray( double[] samples, int np,
                                         BinList binList ) {
            for ( int ip = 0; ip < np; ip++ ) {
                double d = samples[ ip ];
                if ( ! Double.isNaN( d ) ) {
                    binList.submitToBin( ip, d );
                }
            }
        }
    }

    /**
     * Bin accumulation object.
     */
    private static class XYData {
        int nbin_;
        BinList xBins_;
        BinList yBins_;
    }

    /**
     * Plot plan class for use with this plotter.
     */
    private static class CombineArrayPlan {

        final DataSpec dataSpec_;
        final Combiner xCombiner_;
        final Combiner yCombiner_;
        final double[] dxs_;
        final double[] dys_;

        /**
         * Constructor.
         *
         * @param  dataSpec  dataspec
         * @param  xCombiner   combination mode for X array coordinate
         * @param  yCombiner   combination mode for Y array coordinate
         * @param  dxs     aggregated X plot coordinates
         * @param  dys     aggregated Y plot coordinates
         */
        CombineArrayPlan( DataSpec dataSpec,
                          Combiner xCombiner, Combiner yCombiner,
                          double[] dxs, double[] dys ) {
            dataSpec_ = dataSpec;
            xCombiner_ = xCombiner;
            yCombiner_ = yCombiner;
            dxs_ = dxs;
            dys_ = dys;
        }

        /**
         * Indicates whether a given set of characteristics will produce
         * an equivalent plan to this one.
         *
         * @param  dataSpec  data spec
         * @param  xCombiner   combination mode for X array coordinate
         * @param  yCombiner   combination mode for Y array coordinate
         * @return   true iff results will be the same
         */
        boolean matches( DataSpec dataSpec,
                         Combiner xCombiner, Combiner yCombiner ) {
            return dataSpec_.equals( dataSpec )
                && xCombiner_.equals( xCombiner )
                && yCombiner_.equals( yCombiner );
        }

        /**
         * Returns the graphics coordinates corresponding to the aggregated
         * point values.
         *
         * @param  surface  plot surface
         * @return  point array; may include points off the surface
         */
        public Point2D.Double[] getPoints( Surface surface ) {
            int np = dxs_ == null ? 0 : dxs_.length;
            if ( np > 0 ) {
                List<Point2D.Double> list = new ArrayList<>( np );
                for ( int ip = 0; ip < np; ip++ ) {
                    double[] dpos = new double[] { dxs_[ ip ], dys_[ ip ] };
                    Point2D.Double gp = new Point2D.Double();
                    if ( surface.dataToGraphics( dpos, false, gp ) &&
                         PlotUtil.isPointReal( gp ) ) {
                        list.add( gp );
                    }
                }
                return list.toArray( new Point2D.Double[ 0 ] );
            }
            else {
                return null;
            }
        }
    }
}
