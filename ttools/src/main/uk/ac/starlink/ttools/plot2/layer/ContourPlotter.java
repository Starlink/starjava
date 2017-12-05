package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter implementation that draws contours for a density map of points.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2013
 */
public class ContourPlotter extends AbstractPlotter<ContourStyle> {

    private static final ConfigKey<Integer> NLEVEL_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "nlevel", "Level Count" )
           .setShortDescription( "Maximum number of contours" )
           .setXmlDescription( new String[] {
                "<p>Number of countour lines drawn.",
                "In fact, this is an upper limit;",
                "if there is not enough variation in the plot's density,",
                "then fewer contour lines will be drawn.",
                "</p>",
            } )
        , 5, 0, 999 );
    private static final ConfigKey<Integer> SMOOTH_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "smooth", "Smoothing" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Smoothing kernel size in pixels" )
           .setXmlDescription( new String[] {
                "<p>The linear size of the smoothing kernel applied to the",
                "density before performing the contour determination.",
                "If set too low the contours will be too crinkly,",
                "and if too high they will lose definition.",
                "Smoothing currently uses an approximately Gaussian kernel.",
                "</p>",
            } )
        , 5, 1, 100 );
    private static final ConfigKey<Double> OFFSET_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "zero", "Zero Point" )
           .setShortDescription( "Level of first contour" )
           .setXmlDescription( new String[] {
                "<p>Determines the level at which the first contour",
                "(and hence all the others, which are separated from it",
                "by a fixed amount) are drawn.",
                "</p>",
            } )
        , 0, -2, +2, false );
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    private final FloatingCoord weightCoord_;

    /**
     * Constructor.
     *
     * @param  hasWeight  true if plotter is to allow weighted contour maps
     */
    public ContourPlotter( boolean hasWeight ) {
        this( hasWeight ? WEIGHT_COORD : null );
    }

    /**
     * Constructs a plotter based on an optional weight coordinate.
     *
     * @param   weightCoord   weight coordinate, or null for no weighting
     */
    private ContourPlotter( FloatingCoord weightCoord ) {
        super( "Contour", ResourceIcon.PLOT_CONTOUR, 1,
               weightCoord == null ? new Coord[ 0 ]
                                   : new Coord[] { weightCoord } );
        weightCoord_ = weightCoord;
    }

    public String getPlotterDescription() {
        final String weightPara;
        if ( weightCoord_ == null ) {
            weightPara = "";
        }
        else {
            weightPara = PlotUtil.concatLines( new String[] {
                "<p>A weighting may optionally be applied to the quantity",
                "being contoured.",
                "To do this, provide a non-blank value for the",
                "<code>" + weightCoord_.getInput().getMeta().getShortName()
                         + "</code>",
                "coordinate, and use the",
                "<code>" + StyleKeys.COMBINER.getMeta().getShortName()
                         + "</code>",
                "parameter to define how the weights are combined",
                "(<code>" + Combiner.SUM + "</code>,",
                 "<code>" + Combiner.MEAN + "</code>,",
                 "<code>" + Combiner.MEDIAN + "</code>, etc).",
                "</p>",
            } );
        }
        return PlotUtil.concatLines( new String[] {
            "<p>Plots position density contours.",
            "This provides another way",
            "(alongside the",
            ShapeMode.modeRef( ShapeMode.AUTO ),
            "and",
            ShapeMode.modeRef( ShapeMode.DENSITY ),
            "shading modes)",
            "to visualise the characteristics of overdense regions",
            "in a crowded plot.",
            "It's not very useful if you just have a few points.",
            "</p>",
            weightPara,
            "<p>The contours are currently drawn as pixels rather than lines",
            "so they don't look very beautiful in exported vector",
            "output formats (PDF, PostScript).",
            "This may be improved in the future.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keys = new ArrayList<ConfigKey>();
        keys.add( StyleKeys.COLOR );
        if ( weightCoord_ != null ) {
            keys.add( StyleKeys.COMBINER );
        }
        keys.add( NLEVEL_KEY );
        keys.add( SMOOTH_KEY );
        keys.add( StyleKeys.LEVEL_MODE );
        keys.add( OFFSET_KEY );
        return keys.toArray( new ConfigKey[ 0 ] );
    }

    public ContourStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        int nlevel = config.get( NLEVEL_KEY );

        /* Map offset to the range 0..1. */
        double offset = ( ( config.get( OFFSET_KEY ) % 1 ) + 1 ) % 1;
        int nsmooth = config.get( SMOOTH_KEY );
        LevelMode levMode = config.get( StyleKeys.LEVEL_MODE );
        Combiner combiner = weightCoord_ == null
                          ? Combiner.COUNT
                          : config.get( StyleKeys.COMBINER );
        return new ContourStyle( color, nlevel, offset, nsmooth, levMode,
                                 combiner );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final ContourStyle style ) {
        LayerOpt opt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {

                /* It would be nice to draw vector contours.
                 * The algorithm is quite a bit more complicated though.
                 * Maybe one day. */
                if ( ! paperType.isBitmap() ) {
                    logger_.warning( "Sorry - "
                                   + "contours are ugly in vector plots" );
                }
                CoordGroup cgrp = getCoordGroup();
                int icPos = cgrp.getPosCoordIndex( 0, geom );
                int icWeight = weightCoord_ == null
                             ? -1
                             : cgrp.getExtraCoordIndex( 0, geom );
                return new BitmapContourDrawing( surface, geom, dataSpec, style,
                                                 icPos, icWeight, paperType );
            }
        };
    }

    /**
     * Drawing implementation that plans and paints contours.
     * The plan is a density map (2d histogram).
     * Given this it works out where the level boundaries are and
     * just fills in a pixel anywhere two adjacent pixels are in
     * different levels.
     */
    private static class BitmapContourDrawing implements Drawing {

        private final Surface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final ContourStyle style_;
        private final int icPos_;
        private final int icWeight_;
        private final PaperType paperType_;
        private final boolean hasWeight_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom      coordinate geometry
         * @param  dataSpec  coordinate specification
         * @param  style    plot style object
         * @param  icPos    index of first position coordinate in tuples
         * @param  icWeight  index of weight coordinate in tuples, or -1
         * @param  paperType  paper type
         */
        BitmapContourDrawing( Surface surface, DataGeom geom, DataSpec dataSpec,
                              ContourStyle style, int icPos, int icWeight,
                              PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            icPos_ = icPos;
            icWeight_ = icWeight;
            paperType_ = paperType;
            hasWeight_ = icWeight_ >= 0
                    && ! dataSpec_.isCoordBlank( icWeight_ );
        }

        /**
         * The plan is a density map.
         */
        public ContourPlan calculatePlan( Object[] knownPlans,
                                          DataStore dataStore ) {
            Combiner combiner = style_.getCombiner();
            int smooth = style_.getSmoothing();
            int requiredPad = ( smooth + 1 ) / 2;
            for ( Object plan : knownPlans ) {
                if ( plan instanceof ContourPlan ) {
                    ContourPlan cplan = (ContourPlan) plan;
                    if ( cplan.matches( combiner, surface_, dataSpec_, geom_,
                                        requiredPad ) ) {
                        return cplan.smooth_ == smooth
                             ? cplan
                             : cplan.resmooth( smooth );
                    }
                }
            }

            /* If we are calculating a new plan, request more padding than
             * we need for the current style.  In that way, the same plan
             * can be used if the smoothing value goes up a bit. */
            int requestedPad = Math.max( 16, requiredPad * 2 );
            NumberGrid rawGrid = readBinGrid( dataStore, requestedPad );
            return new ContourPlan( combiner, surface_, dataSpec_, geom_,
                                    requestedPad, rawGrid, 1, rawGrid )
                  .resmooth( smooth );
        }

        /**
         * Populates a 2d pixel grid giving the value of the weighted or
         * unweighted density map at each pixel.  It does this by reading
         * the input table.  The populated pixel grid comprises the
         * visible plot bounds, plus a padding region of a few pixels
         * (as requested) around the outside to accommodate smoothing
         * operations etc.
         *
         * @param  dataStore  input table data object
         * @param  pad   number of pixels around the plot bounds that should
         *               be populated
         * @return   populated pixel grid
         */
        private NumberGrid readBinGrid( DataStore dataStore, int pad ) {
            Rectangle bounds = surface_.getPlotBounds();
            int nx = bounds.width + 2 * pad;
            int ny = bounds.height + 2 * pad;
            Gridder gridder = new Gridder( nx, ny );
            int xoff = bounds.x - pad;
            int yoff = bounds.y - pad;
            int nbin = gridder.getLength();
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            double[] dpos = new double[ surface_.getDataDimCount() ];
            Point2D.Double gp = new Point2D.Double();
            final BinList binList;
            if ( hasWeight_ ) {
                binList = style_.getCombiner().createArrayBinList( nbin );
                while ( tseq.next() ) {
                    double w = WEIGHT_COORD.readDoubleCoord( tseq, icWeight_ );
                    if ( ! Double.isNaN( w ) &&
                         geom_.readDataPos( tseq, icPos_, dpos ) &&
                         surface_.dataToGraphics( dpos, false, gp ) ) {
                        int gx = PlotUtil.ifloor( gp.x ) - xoff;
                        if ( gx >= 0 && gx < nx ) {
                            int gy = PlotUtil.ifloor( gp.y ) - yoff;
                            if ( gy >= 0 && gy < ny ) {
                                int ibin = gridder.getIndex( gx, gy );
                                binList.submitToBin( ibin, w );
                            }
                        }
                    }
                }
            }
            else {
                binList = Combiner.COUNT.createArrayBinList( nbin );
                while ( tseq.next() ) {
                    if ( geom_.readDataPos( tseq, icPos_, dpos ) &&
                         surface_.dataToGraphics( dpos, false, gp ) ) {
                        int gx = PlotUtil.ifloor( gp.x ) - xoff;
                        if ( gx >= 0 && gx < nx ) {
                            int gy = PlotUtil.ifloor( gp.y ) - yoff;
                            if ( gy >= 0 && gy < ny ) {
                                int ibin = gridder.getIndex( gx, gy );
                                binList.submitToBin( ibin, 1 );
                            }
                        }
                    }
                }
            }
            final BinList.Result binResult = binList.getResult();
            return new NumberGrid( gridder ) {
                public double getValue( int index ) {
                    double value = binResult.getBinValue( index );
                    return Double.isNaN( value ) ? 0 : value;
                }
            };
        }

        public void paintData( final Object plan, Paper paper,
                               final DataStore dataStore ) {
            /* For 2D we could fairly easily implement this as a Glyph
             * instead, which would probably be more efficient. */
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    paintContours( g, (ContourPlan) plan );
                }
                public boolean isOpaque() {
                    return true;
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return null;
        }

        /**
         * Does the work for plotting contours given a density map.
         *
         * @param   g  graphics context
         * @param   contourPlan   plan including point density map
         * @param   dataStore   data storage
         */
        private void paintContours( Graphics g, ContourPlan cplan ) {
            NumberGrid grid = cplan.smoothGrid_;
            assert cplan.smooth_ == style_.getSmoothing();
            Color color0 = g.getColor();
            g.setColor( style_.getColor() );
            Rectangle bounds = surface_.getPlotBounds();
            int pad = cplan.pad_;
            int xoff = bounds.x - pad;
            int yoff = bounds.y - pad;
            int nx = bounds.width;
            int ny = bounds.height;
            Gridder gridder = new Gridder( nx + 2 * pad, ny + 2 * pad );
            assert gridder.equals( grid.gridder_ );
            final int leng = gridder.getLength();

            /* Set up a list of contour levels.  Contours are defined as
             * the boundaries of groups of contiguous pixels falling within
             * a single level. */
            Leveller leveller = createLeveller( grid, style_, hasWeight_ );

            /* For each pixel, see whether the next one along (+1 in X/Y
             * direction) is in a different level.  If so, paint a
             * contour pixel.  Note that really there is a systematic
             * positional offset of these contour pixels of +0.5 in
             * both directions.
             * Sweep in both directions (X,Y, then Y,X).  This paints some
             * contour pixels twice, but if you don't then lines that
             * are too steep miss pixels. */
            int lw = 1; // line width
            int ix0 = Math.max( 0, pad - lw );
            int ix1 = Math.min( nx + pad + 2 * lw, gridder.getWidth() );
            int iy0 = Math.max( 0, pad - lw );
            int iy1 = Math.min( ny + pad + 2 * lw, gridder.getHeight() );
            for ( int ix = ix0; ix < ix1; ix++ ) {
                int lev0 = leveller.getLevel( grid.getValue( ix, 0 ) );
                for ( int iy = iy0 + 1; iy < iy1; iy++ ) {
                    int lev1 = leveller.getLevel( grid.getValue( ix, iy ) );
                    if ( lev1 != lev0 ) {
                        g.fillRect( xoff + ix, yoff + iy - 1, 1, 1 );
                    }
                    lev0 = lev1;
                }
            }
            for ( int iy = iy0; iy < iy1; iy++ ) {
                int lev0 = leveller.getLevel( grid.getValue( 0, iy ) );
                for ( int ix = ix0 + 1; ix < ix1; ix++ ) {
                    int lev1 = leveller.getLevel( grid.getValue( ix, iy ) );
                    if ( lev1 != lev0 ) {
                        g.fillRect( xoff + ix - 1, yoff + iy, 1, 1 );
                    }
                    lev0 = lev1;
                }
            }
            g.setColor( color0 );
        }
    }

    /**
     * Smooths a pixel grid.
     * This is a convolution with a square top hat kernel,
     * with full width given by the smoothing parameter.
     *
     * @param   inGrid  original grid data
     * @param   smooth   smoothing parameter
     * @return  smoothed grid data
     */
    private static NumberGrid smooth( NumberGrid inGrid, int smooth ) {
        Gridder gridder = inGrid.gridder_;
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        int npix = gridder.getLength();
        double[] kernel = gaussian( smooth, 1.5 );

        /* Since the kernel is separable, we can do two 1-d convolutions,
         * which scales much better than one 2-d convolution. */
        double[] a1 = new double[ npix ];
        for ( int qx = 0; qx < smooth; qx++ ) {
            double k = kernel[ qx ];
            int px = qx - smooth / 2;
            int ix0 = Math.max( 0, px );
            int ix1 = Math.min( nx, nx + px );
            for ( int iy = 0; iy < ny; iy++ ) {
                for ( int ix = ix0; ix < ix1; ix++ ) {
                    int jx = ix - px;
                    a1[ gridder.getIndex( ix, iy ) ] +=
                        k * inGrid.getValue( gridder.getIndex( jx, iy ) );
                }
            }
        }
        double[] a2 = new double[ npix ];
        for ( int qy = 0; qy < smooth; qy++ ) {
            double k = kernel[ qy ];
            int py = qy - smooth / 2;
            int iy0 = Math.max( 0, py );
            int iy1 = Math.min( ny, ny + py );
            for ( int iy = iy0; iy < iy1; iy++ ) {
                for ( int ix = 0; ix < nx; ix++ ) {
                    int jy = iy - py;
                    a2[ gridder.getIndex( ix, iy ) ] +=
                        k * a1[ gridder.getIndex( ix, jy ) ];
                }
            }
        }
        final double[] out = a2;

        /* Normalise. */
        double sk = 0;
        for ( int i = 0; i < smooth; i++ ) {
            for ( int j = 0; j < smooth; j++ ) {
                sk += kernel[ i ] * kernel[ j ];
            }
        }
        double factor = 1.0 / sk;
        for ( int i = 0; i < out.length; i++ ) {
            out[ i ] *= factor;
        }

        /* Return the result as a NumberGrid. */
        return new NumberGrid( gridder ) {
            public double getValue( int i ) {
                return out[ i ];
            }
        };
    }

    /**
     * Calculates a 1-d smoothing kernel that approximates a Gaussian.
     * It just uses discrete samples of the Gaussian function,
     * it doesn't bother to integrate the function over pixels.
     *
     * <p>Note that the Gaussian kernel is separable, so can be split
     * into two 1-d kernels for convolution (I read somewhere that
     * the Gaussian is the only 2-d kernel that has this property,
     * but I haven't taken the trouble to understand if that's true).
     * I tested as well that separating this truncated discretised
     * kernel for convolution works, at least it gives the same result
     * regardless of which order you do the separated X- and
     * Y-direction 1-d convolutions.
     *
     * @param  nsamp  number of samples required
     * @param  nsigma   the number of standard deviations corresponding
     *                  to the half-width of the kernel
     *                  (is that called the support?)
     * @return   nsamp-element array giving normalised, separable kernel
     */
    private static double[] gaussian( int nsamp, double nsigma ) {
        double[] kernel = new double[ nsamp ];
        double x0 = 0.5 * ( nsamp - 1 );
        double sigma = nsamp * 0.5 / nsigma;
        double sum = 0;
        for ( int i = 0; i < nsamp; i++ ) {
            double p = ( i - x0 ) / sigma;
            double sample = Math.exp( -0.5 * p * p );
            kernel[ i ] = sample;
            sum += sample;
        }
        double norm = 1.0 / sum;
        for ( int i = 0; i < nsamp; i++ ) {
            kernel[ i ] += norm;
        }
        return kernel;
    }

    /**
     * Returns a leveller for a given data grid and contour style.
     *
     * @param   grid  grid data
     * @param  style  contour style
     * @param  hasWeight  true if a weighting column is in use
     * @return  leveller object
     */
    private static Leveller createLeveller( NumberGrid grid,
                                            ContourStyle style,
                                            boolean hasWeight ) {
        Combiner combiner = style.getCombiner();
        boolean isCounts = ! hasWeight
                        || combiner.equals( Combiner.COUNT )
                        || combiner.equals( Combiner.HIT );
        final double[] levels = style.getLevelMode()
                               .calculateLevels( grid, style.getLevelCount(),
                                                 style.getOffset(), isCounts );
        return new Leveller() {
            public int getLevel( double count ) {
                int ipos = Arrays.binarySearch( levels, count );
                return ipos < 0 ? - ( ipos + 1 ) : ipos;
            }
        };
    }

    /**
     * Knows how to turn a pixel value into an integer level.
     */
    private interface Leveller {

        /**
         * Returns the level value for a given pixel value.
         *
         * @param  pixel value
         * @return  contour level
         */
        int getLevel( double count );
    }

    /**
     * Partial NumberArray implementation that knows about grid geometry.
     */
    private static abstract class NumberGrid implements NumberArray {
        final Gridder gridder_;

        /**
         * Constructor.
         *
         * @param  gridder  grid geometry object
         */
        NumberGrid( Gridder gridder ) {
            gridder_ = gridder;
        }
        public double getValue( int ix, int iy ) {
            return getValue( gridder_.getIndex( ix, iy ) );
        }
        public int getLength() {
            return gridder_.getLength();
        }
    }

    /**
     * Aggregates accumulated grid data with information that characterises its
     * scope of applicability.
     * It combines a raw and smoothed grid.
     */
    private static class ContourPlan {
        final Combiner combiner_;
        final Surface surface_;
        final DataSpec dataSpec_;
        final DataGeom geom_;
        final int pad_;
        final NumberGrid rawGrid_;
        final int smooth_;
        final NumberGrid smoothGrid_;

        /**
         * Constructor.
         *
         * @param  combiner  combination method for values
         * @param  surface   plot surface
         * @param  dataSpec   data specification
         * @param  geom     geom
         * @param  pad     number of pixels around the visible plot bounds
         *                 that are populated in the bin grid
         * @param  rawGrid  accumulated weight data
         * @param  smooth   smoothing width
         * @param  smoothGrid   rawGrid data smoothed by the supplied smoothing
         *                      parameter; if smooth==1, this is the same
         *                      as rawGrid
         */
        ContourPlan( Combiner combiner, Surface surface, DataSpec dataSpec,
                     DataGeom geom, int pad, NumberGrid rawGrid,
                     int smooth, NumberGrid smoothGrid ) {
            combiner_ = combiner;
            surface_ = surface;
            dataSpec_ = dataSpec;
            geom_ = geom;
            pad_ = pad;
            rawGrid_ = rawGrid;
            smooth_ = smooth;
            smoothGrid_ = smoothGrid;
        }

        /**
         * Returns a smoother instance with the same raw grid data, but
         * which may have a different smoothed grid.
         *
         * @param  smooth  required smoothing width
         * @return  grid; may be this one or not
         */
        ContourPlan resmooth( int smooth ) {
            if ( smooth == smooth_ ) {
                return this;
            }
            else {
                NumberGrid smoothGrid = smooth == 1
                                      ? rawGrid_
                                      : smooth( rawGrid_, smooth );
                return new ContourPlan( combiner_, surface_, dataSpec_,
                                        geom_, pad_, rawGrid_,
                                        smooth, smoothGrid );
            }
        }

        /**
         * Indicates whether this plan can be used for a given set
         * of drawing requirements.
         * Note this does not test smoothing equality, but the smoothed
         * grid can be recalculated without having to regenerate the
         * raw grid, so this object will still be worth using if it
         * matches according to this method.
         *
         * @param  gridder   grid geometry
         * @param  combiner  combination method for values
         * @param  dataSpec   data specification
         * @param  geom     geom
         * @param  requiredPad   minimum number of padding pixels around
         *                       the grid region that corresponds to the
         *                       visible plot bounds
         */
        public boolean matches( Combiner combiner, Surface surface,
                                DataSpec dataSpec, DataGeom geom,
                                int requiredPad ) {
            return combiner.equals( combiner_ )
                && surface.equals( surface_ )
                && dataSpec.equals( dataSpec_ )
                && geom_.equals( geom )
                && pad_ >= requiredPad;
        }
    }
}
