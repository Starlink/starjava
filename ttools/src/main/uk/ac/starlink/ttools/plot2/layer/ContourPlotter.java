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
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
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

    /** Coordinate used for weighting. */
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;

    /** Config key for the number of contour levels plotted. */
    public static final ConfigKey<Integer> NLEVEL_KEY =
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
        , 5, 1, 999 );

    /** Config key for the width of the smoothing kernel. */
    public static final ConfigKey<Integer> SMOOTH_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "smooth", "Smoothing" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Smoothing kernel size in pixels" )
           .setXmlDescription( new String[] {
                "<p>The linear size of the smoothing kernel applied to the",
                "density before performing the contour determination.",
                "If set too low the contours will be too crinkly,",
                "and if too high they will lose definition.",
                "Smoothing currently uses an approximately Gaussian kernel",
                "for extensive combination modes (count, sum)",
                "or a circular top hat for intensive modes (weighted mean).",
                "</p>",
            } )
        , 5, 1, 100 );

    /** Config key for the contour zero level. */
    public static final ConfigKey<Double> OFFSET_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "zero", "Zero Point" )
           .setShortDescription( "Level of first contour" )
           .setXmlDescription( new String[] {
                "<p>Determines the level at which the first contour",
                "(and hence all the others, which are separated from it",
                "by a fixed amount) are drawn.",
                "</p>",
            } )
        , 1, 0, 2, false, false, SliderSpecifier.TextOption.ENTER_ECHO );

    /** Config key for the smoothing combination mode. */
    public static final ConfigKey<Combiner> COMBINER_KEY =
        new OptionConfigKey<Combiner>(
            new ConfigMeta( "combine", "Combine" )
           .setShortDescription( "Weight combination mode" )
           .setXmlDescription( new String[] {
                "<p>Defines the way that the weight values are combined",
                "when generating the value grid for which the contours",
                "will be plotted.",
                "If a weighting is supplied, the most useful values are",
                "<code>" + Combiner.MEAN + "</code> which traces the",
                "mean values of a quantity and",
                "<code>" + Combiner.SUM + "</code> which traces the",
                "weighted sum.",
                "Other values such as",
                "<code>" + Combiner.MEDIAN + "</code>",
                "are of dubious validity because of the way that the",
                "smoothing is done.",
                "</p>",
                "<p>This value is ignored if the weighting coordinate",
                "<code>" + WEIGHT_COORD.getInput().getMeta().getShortName()
                         + "</code>",
                "is not set.",
                "</p>",
            } )
        , Combiner.class, new Combiner[] {
            Combiner.SUM,
            Combiner.MEAN,
            Combiner.MEDIAN,
            Combiner.SAMPLE_STDEV,
            Combiner.MIN,
            Combiner.MAX,
            Combiner.COUNT,
        }, Combiner.SUM ) {
        public String getXmlDescription( Combiner combiner ) {
            return combiner.getDescription();
        }
    }.setOptionUsage()
     .addOptionsXml();

    /** Report key for the contour levels plotted. */
    public static final ReportKey<double[]> LEVELS_REPKEY =
         new LevelsReportKey();

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
                "<code>" + COMBINER_KEY.getMeta().getShortName() + "</code>",
                "parameter to define how the weights are combined",
                "(<code>" + Combiner.SUM + "</code>,",
                 "<code>" + Combiner.MEAN + "</code>, etc).",
                "</p>",
            } );
        }
        return PlotUtil.concatLines( new String[] {
            "<p>Plots position density contours.",
            "This provides another way",
            "(alongside the",
            ShapeMode.modeRef( ShapeMode.AUTO ) + ",",
            ShapeMode.modeRef( ShapeMode.DENSITY ),
            "and",
            ShapeMode.modeRef( ShapeMode.WEIGHTED ),
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

    @Override
    public boolean hasReports() {
        return true;
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keys = new ArrayList<ConfigKey>();
        keys.add( StyleKeys.COLOR );
        if ( weightCoord_ != null ) {
            keys.add( COMBINER_KEY );
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

        double offset = config.get( OFFSET_KEY );
        int nsmooth = config.get( SMOOTH_KEY );
        LevelMode levMode = config.get( StyleKeys.LEVEL_MODE );
        Combiner combiner = weightCoord_ == null
                          ? Combiner.COUNT
                          : config.get( COMBINER_KEY );
        return new ContourStyle( color, nlevel, offset, nsmooth, levMode,
                                 combiner );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final ContourStyle style ) {
        LayerOpt opt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
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
                    return binResult.getBinValue( index );
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
            ReportMap reports = new ReportMap();
            if ( plan instanceof ContourPlan ) {
                Leveller leveller = createLeveller( (ContourPlan) plan );
                reports.put( LEVELS_REPKEY, leveller.levels_ );
            }
            return reports;
        }

        /**
         * Returns a leveller for a given plan.
         *
         * @param   plan  contour plan
         * @return  leveller object
         */
        private Leveller createLeveller( ContourPlan plan ) {
            Combiner combiner = style_.getCombiner();
            LevelMode lmode = style_.getLevelMode();

            /* Calculate the levels from the data.  We use the smoothed grid
             * here since that's what's going to get plotted.
             * To get the levels right it's important that the grid values
             * are NaN and not just zero where there is no data
             * (no contribution from the smoothed input grid). */
            NumberGrid grid = plan.smoothGrid_;
            boolean isCounts = plan.smooth_ == 1 
                            && ( ! hasWeight_
                                 || combiner.equals( Combiner.COUNT )
                                 || combiner.equals( Combiner.HIT ) );
            double[] levels = lmode
                             .calculateLevels( grid, style_.getLevelCount(),
                                               style_.getOffset(), isCounts );
            return new Leveller( levels );
        }

        /**
         * Does the work for plotting contours given a density map.
         *
         * @param   g  graphics context
         * @param   contourPlan   plan including point density map
         * @param   dataStore   data storage
         */
        private void paintContours( Graphics g, ContourPlan cplan ) {
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
            final int leng = gridder.getLength();

            /* Set up a list of contour levels.  Contours are defined as
             * the boundaries of groups of contiguous pixels falling within
             * a single level. */
            Leveller leveller = createLeveller( cplan );

            /* For the contour generation, set up a grid which treats
             * bad values as simply very low values.  This has the effect
             * of making blanke regions fall below the lowest contour. */
            final NumberGrid smoothGrid = cplan.smoothGrid_;
            assert gridder.equals( smoothGrid.gridder_ );
            NumberGrid plotGrid = new NumberGrid( smoothGrid.gridder_ ) {
                public double getValue( int i ) {
                    double value = smoothGrid.getValue( i );
                    return Double.isNaN( value ) ? -Double.MAX_VALUE
                                                 : value;
                }
            };

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
                int lev0 = leveller.getLevel( plotGrid.getValue( ix, 0 ) );
                for ( int iy = iy0 + 1; iy < iy1; iy++ ) {
                    int lev1 = leveller.getLevel( plotGrid.getValue( ix, iy ) );
                    if ( lev1 != lev0 ) {
                        g.fillRect( xoff + ix, yoff + iy - 1, 1, 1 );
                    }
                    lev0 = lev1;
                }
            }
            for ( int iy = iy0; iy < iy1; iy++ ) {
                int lev0 = leveller.getLevel( plotGrid.getValue( 0, iy ) );
                for ( int ix = ix0 + 1; ix < ix1; ix++ ) {
                    int lev1 = leveller.getLevel( plotGrid.getValue( ix, iy ) );
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
     * Smooths a pixel grid using an algorithm suitable for summing values.
     *
     * @param   inGrid  original grid data
     * @param   smooth   smoothing parameter
     * @return  smoothed grid data, NaNs where no input contribution
     */
    private static NumberGrid smoothSum( NumberGrid inGrid, int smooth ) {
        Gridder gridder = inGrid.gridder_;
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        int npix = gridder.getLength();

        /* Smooth using a convolution with a Gaussian kernel,
         * with full width given by the smoothing parameter. */
        double[] kernel = gaussian( smooth, 1.5 );

        /* Since the Gaussian kernel is separable and we are just summing the
         * results, we can do two 1-d convolutions, which scales much
         * better than one 2-d convolution.
         * When doing it like this, blank values in the input grid,
         * which correspond to values with no input contributions,
         * just don't contribute (equivalent to contributing at zero weight)
         * to output pixels.  It's important (when calculating levels)
         * that zero values are indistinguishable from values with no data,
         * so we need to do extra work to write NaNs where there are no
         * input contributions.  We do it by effectively convolving a mask
         * (blank=0, non-blank=1) with the same kernel.
         * Anywhere the mask convolution ends up zero corresponds to
         * no input data contribution. */
        double[] a1 = new double[ npix ];
        double[] b1 = new double[ npix ];
        for ( int qx = 0; qx < smooth; qx++ ) {
            double k = kernel[ qx ];
            int px = qx - smooth / 2;
            int ix0 = Math.max( 0, px );
            int ix1 = Math.min( nx, nx + px );
            for ( int iy = 0; iy < ny; iy++ ) {
                for ( int ix = ix0; ix < ix1; ix++ ) {
                    int jx = ix - px;
                    double d = inGrid.getValue( gridder.getIndex( jx, iy ) );
                    if ( ! Double.isNaN( d ) ) {
                        int index1 = gridder.getIndex( ix, iy );
                        a1[ index1 ] += k * d;
                        b1[ index1 ] += k;
                    }
                }
            }
        }
        double[] a2 = new double[ npix ];
        double[] b2 = new double[ npix ];
        for ( int qy = 0; qy < smooth; qy++ ) {
            double k = kernel[ qy ];
            int py = qy - smooth / 2;
            int iy0 = Math.max( 0, py );
            int iy1 = Math.min( ny, ny + py );
            for ( int iy = iy0; iy < iy1; iy++ ) {
                for ( int ix = 0; ix < nx; ix++ ) {
                    int jy = iy - py;
                    int index2 = gridder.getIndex( ix, iy );
                    int index1 = gridder.getIndex( ix, jy );
                    a2[ index2 ] += k * a1[ index1 ];
                    b2[ index2 ] += k * b1[ index1 ];
                }
            }
        }
        final double[] out = a2;
        final double[] mask = b2;

        /* Calculate the normalisation factor. */
        double sk = 0;
        for ( int i = 0; i < smooth; i++ ) {
            for ( int j = 0; j < smooth; j++ ) {
                sk += kernel[ i ] * kernel[ j ];
            }
        }
        double factor = 1.0 / sk;

        /* For each value in the convolved grid: apply the normalisation
         * factor if the mask says it had contributions from the input,
         * or set it NaN if the mask says it had no contributions. */
        for ( int i = 0; i < out.length; i++ ) {
            out[ i ] = mask[ i ] > 0 ? out[ i ] * factor
                                     : Double.NaN;
        }

        /* Return the result as a NumberGrid. */
        return new NumberGrid( gridder ) {
            public double getValue( int i ) {
                return out[ i ];
            }
        };
    }

    /**
     * Smooths a pixel grid using an algorithm suitable for mean values.
     *
     * @param  inGrid  input grid
     * @param  smooth  smoothing kernel size
     * @return  smoothed grid data, NaNs where no input contribution
     */
    private static NumberGrid smoothMean( NumberGrid inGrid, int smooth ) {
        Gridder gridder = inGrid.gridder_;
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        int npix = gridder.getLength();

        /* We can't decompose this into two 1-d convolutions, since we
         * need to keep track of the number of values submitted to the
         * combiner as well as the sums of those values. 
         * The effective kernel used for the smoothing is a circular top hat.
         * A Gaussian isn't a good choice here, since its main job is going
         * to be covering up for missing values, and it doesn't need to
         * be separable. */
        BinList outBinList = Combiner.MEAN.createArrayBinList( npix );
        double q0 = 0.5 * ( smooth - 1 );
        double qr = 0.5 * ( smooth - 1 ) + 0.5;
        for ( int qx = 0; qx < smooth; qx++ ) {
            int px = qx - smooth / 2;
            int ix0 = Math.max( 0, px );
            int ix1 = Math.min( nx, nx + px );
            for ( int qy = 0; qy < smooth; qy++ ) {
                int py = qy - smooth / 2;
                int iy0 = Math.max( 0, py );
                int iy1 = Math.min( ny, ny + py );
                double r = Math.hypot( qx - q0, qy - q0 );
                if ( r <= qr ) {
                    for ( int iy = iy0; iy < iy1; iy++ ) {
                        int jy = iy - py;
                        for ( int ix = ix0; ix < ix1; ix++ ) {
                            int jx = ix - px;
                            double d =
                                inGrid.getValue( gridder.getIndex( jx, jy ) );
                            if ( ! Double.isNaN( d ) ) {
                                int ig = gridder.getIndex( ix, iy );
                                outBinList.submitToBin( ig, d );
                            }
                        }
                    }
                }
            }
        }
        final BinList.Result binResult = outBinList.getResult();
        return new NumberGrid( gridder ) {
            public double getValue( int i ) {
                return binResult.getBinValue( i );
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
     * ReportKey implementation for reporting contour levels actually
     * used in the plotted drawing.
     */
    private static class LevelsReportKey extends ReportKey<double[]> {

        /**
         * Constructor.
         */
        LevelsReportKey() {
            super( new ReportMeta( "levels", "Levels" ), double[].class, true );
        }

        public String toText( double[] values ) {
            int nval = values == null ? 0 : values.length;
            if ( nval == 0 ) {
                return null;
            }
            else if ( nval == 1 ) {
                return Double.toString( values[ 0 ] );
            }
            else if ( allInteger( values ) ) {
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < nval; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( Long.toString( (long) values[ i ] ) );
                }
                return sbuf.toString();
            }
            else {
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < nval; i++ ) {
                    double d1 = i - 1 >= 0
                              ? Math.abs( values[ i ] - values[ i - 1 ] )
                              : Double.POSITIVE_INFINITY;
                    double d2 = i + 1 < nval
                              ? Math.abs( values[ i + 1 ] - values[ i ] )
                              : Double.POSITIVE_INFINITY;
                    double diff = Math.min( d1, d2 );
                    double dp = 0.001 * diff;
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( PlotUtil.formatNumber( values[ i ], dp ) );
                }
                return sbuf.toString();
            }
        }

        /**
         * Indicates whether every element of a supplied double array
         * is in fact an integer value.
         *
         * @param  values  array
         * @return  true iff all values elements are integers
         */
        private static boolean allInteger( double[] values ) {
            for ( double d : values ) {
                if ( (long) d != d ) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Knows how to turn a pixel value into an integer level.
     */
    private static class Leveller {
        final double[] levels_;

        /**
         * Constructor.
         *
         * @param  levels  monotonically increasing array of level values
         */
        Leveller( double[] levels ) {
            levels_ = levels;
        }

        /**
         * Returns the level value for a given pixel value.
         *
         * @param  value   pixel value
         * @return  contour level
         */
        public int getLevel( double value ) {
            int ipos = Arrays.binarySearch( levels_, value );
            return ipos < 0 ? - ( ipos + 1 ) : ipos;
        }
    }

    /**
     * Partial NumberArray implementation that knows about grid geometry.
     * Note that the values must be NaN where there is no input data,
     * not zero.
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

                /* Note that the smoothed grid, like the raw grid, must have
                 * NaNs for missing data not zeros, otherwise contour level
                 * calculation won't work correctly. */
                final NumberGrid sgrid;
                if ( smooth == 1 ) {
                    sgrid = rawGrid_;
                }
                else if ( combiner_.getType().isExtensive() ) {
                    sgrid = smoothSum( rawGrid_, smooth );
                }
                else {
                    sgrid = smoothMean( rawGrid_, smooth );
                }
                return new ContourPlan( combiner_, surface_, dataSpec_,
                                        geom_, pad_, rawGrid_,
                                        smooth, sgrid );
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
