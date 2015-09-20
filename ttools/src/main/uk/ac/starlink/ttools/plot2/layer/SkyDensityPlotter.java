package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that plots a genuine density map on a SkySurface.
 * It paints a single Decal, no Glyphs.
 *
 * <p>Note it only works with a SkySurface.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public class SkyDensityPlotter
             implements Plotter<SkyDensityPlotter.SkyDenseStyle> {

    private final boolean transparent_;
    private final CoordGroup coordGrp_;
    private final FloatingCoord weightCoord_;

    private static FloatingCoord WEIGHT_COORD = FloatingCoord.WEIGHT_COORD;
    private static final RampKeySet RAMP_KEYS = StyleKeys.DENSEMAP_RAMP;
    private static final ConfigKey<Integer> LEVEL_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "level", "HEALPix Level" )
           .setStringUsage( "<level>" )
           .setShortDescription( "HEALPix level (0-29)" )
           .setXmlDescription( new String[] {
                "<p>Gives the HEALPix level of pixels which are averaged",
                "over to calculate density.",
                "At level 0 there are 12 pixels on the sky,",
                "the count multiplies by 4 for each increment.",
                "</p>",
            } )
        , 4, 0, 29, false );
    private static final ConfigKey<Double> OPAQUE_KEY = StyleKeys.AUX_OPAQUE;

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     * @param  hasWeight    if true, an optional weight coordinate will
     *                      be solicited alongside the positional coordinates
     */
    public SkyDensityPlotter( boolean transparent, boolean hasWeight ) {
        transparent_ = transparent;
        weightCoord_ = hasWeight ? FloatingCoord.WEIGHT_COORD : null;
        Coord[] extraCoords = weightCoord_ == null
                            ? new Coord[ 0 ]
                            : new Coord[] { weightCoord_ };
        coordGrp_ = CoordGroup.createCoordGroup( 1, extraCoords );
    }

    public String getPlotterName() {
        return "Density";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_SKYDENSITY;
    }

    public CoordGroup getCoordGroup() {
        return coordGrp_;
    }

    public boolean hasReports() {
        return false;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a density map on the sky.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.add( LEVEL_KEY );
        keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        if ( transparent_ ) {
            keyList.add( OPAQUE_KEY );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    public SkyDenseStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int level = config.get( LEVEL_KEY );
        Scaling scaling = ramp.getScaling();
        float scaleAlpha = (float) ( 1.0 / config.get( OPAQUE_KEY ) );
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new SkyDenseStyle( level, scaling, shader );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final SkyDenseStyle style ) {
        LayerOpt opt = style.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL;
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                return new SkyDensityDrawing( (SkySurface) surface,
                                              (SkyDataGeom) geom,
                                              dataSpec, style, paperType );
            }
        };
    }

    /**
     * Calculates the HEALPix level whose pixels are of approximately
     * the same size as the screen pixels for a given SkySurface.
     * There is not an exact correspondance here.
     * An attempt is made to return the result for the "largest" screen pixel
     * (the one covering more of the sky than any other).
     *
     * @param  surface
     * @return  approximately corresponding HEALPix level
     */
    private static int getPixelLevel( SkySurface surface ) {

        /* Identify the graphics pixel at the center of the sky projection.
         * It may be off the currently visible part of the screen;
         * that doesn't matter.  This is likely to be the largest
         * screen pixel. */
        Point p = surface.getSkyCenter();
        double[] p1 =
            surface.graphicsToData( new Point( p.x - 1, p.y - 1 ), null );
        double[] p2 =
            surface.graphicsToData( new Point( p.x + 1, p.y + 1 ), null );
        double pixTheta = vectorSeparation( p1, p2 ) / Math.sqrt( 4 + 4 );
        return Tilings.healpixK( Math.toDegrees( pixTheta * 2 ) );
    }

    /**
     * Angle in radians between two (not necessarily unit) vectors.
     * The code follows that of SLA_SEPV from SLALIB.
     * The straightforward thing to do would just be to use the cosine rule,
     * but that may suffer numeric instabilities for small angles,
     * so this more complicated approach is more robust.
     *
     * @param  p1  first input vector
     * @param  p2  second input vector
     * @return   angle between p1 and p2 in radians
     */
    private static double vectorSeparation( double[] p1, double[] p2 ) {
        double modCross = Matrices.mod( Matrices.cross( p1, p2 ) );
        double dot = Matrices.dot( p1, p2 );
        return modCross == 0 && dot == 0 ? 0 : Math.atan2( modCross, dot );
    }

    /**
     * Style for configuring with the sky density plot.
     */
    public static class SkyDenseStyle implements Style {

        private final int level_;
        private final Scaling scaling_;
        private final Shader shader_;

        /**
         * Constructor.
         *
         * @param   level   HEALPix level defining the requested map resolution;
         *                  note the actual resolution at which the densities
         *                  are calculated may be different from this,
         *                  in particular if the screen pixel grid is coarser
         *                  than that defined by this level
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   shader   colour map
         */
        public SkyDenseStyle( int level, Scaling scaling, Shader shader ) {
            level_ = level;
            scaling_ = scaling;
            shader_ = shader;
        }

        /**
         * Indicates whether this style has any transparency.
         *
         * @return   if true, the colours painted by this shader within
         *           the plot's geometric region of validity (that is,
         *           on the sky) are guaranteed always to have an alpha
         *           value of 1
         */
        boolean isOpaque() {
            return ! Shaders.isTransparent( shader_ );
        }

        public Icon getLegendIcon() {
            return Shaders.createShaderIcon( shader_, null, true, 16, 8, 2, 2 );
        }

        @Override
        public int hashCode() {
            int code = 23443;
            code = 23 * code + level_;
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + shader_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SkyDenseStyle ) {
                SkyDenseStyle other = (SkyDenseStyle) o;
                return this.level_ == other.level_
                    && this.scaling_.equals( other.scaling_ )
                    && this.shader_.equals( other.shader_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Drawing implementation for the sky density map.
     */
    private class SkyDensityDrawing implements Drawing {

        private final SkySurface surface_;
        private final SkyDataGeom geom_;
        private final DataSpec dataSpec_;
        private final SkyDenseStyle style_;
        private final PaperType paperType_;
        private final int level_;

        /**
         * Constructor.
         *
         * @param  surface  plotting surface
         * @param  geom    coordinate geometry
         * @param  dataSpec   specifies data coordinates
         * @param  style  density map style
         * @param  paperType  paper type
         */
        SkyDensityDrawing( SkySurface surface, SkyDataGeom geom,
                           DataSpec dataSpec, SkyDenseStyle style,
                           PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            paperType_ = paperType;
            level_ = Math.min( style_.level_, getPixelLevel( surface ) );
        }

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            for ( Object plan : knownPlans ) {
                if ( plan instanceof SkyDensityPlan ) {
                    SkyDensityPlan skyPlan = (SkyDensityPlan) plan;
                    if ( skyPlan.matches( level_, dataSpec_, geom_ ) ) {
                        return skyPlan;
                    }
                }
            }
            IntegerBinBag binBag = readBins( dataStore );
            return new SkyDensityPlan( binBag, dataSpec_, geom_ );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            final SkyDensityPlan dplan = (SkyDensityPlan) plan;
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    paintBins( g, dplan.binBag_ );
                }
                public boolean isOpaque() {
                    return style_.isOpaque();
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return null;
        }

        /**
         * Constructs and populates a bin bag (weighted histogram) 
         * suitable for the plot from the data specified for this drawing.
         *
         * @param   dataStore   contains data required for plot
         * @return   populated bin bag
         * @slow
         */
        private IntegerBinBag readBins( DataStore dataStore ) {
            SkyPixer skyPixer = createSkyPixer();
            IntegerBinBag binBag =
                IntegerBinBag.createBinBag( skyPixer.getPixelCount() );
            int icPos = coordGrp_.getPosCoordIndex( 0, geom_ );
            int icWeight = weightCoord_ == null
                         ? -1
                         : coordGrp_.getExtraCoordIndex( 0, geom_ );
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            double[] v3 = new double[ 3 ];

            /* Unweighted. */
            if ( icWeight < 0 || dataSpec_.isCoordBlank( icWeight ) ) {
                while ( tseq.next() ) {
                    if ( geom_.readDataPos( tseq, icPos, v3 ) ) {
                        binBag.addToBin( skyPixer.getIndex( v3 ), 1 );
                    }
                }
            }

            /* Weighted. */
            else {
                while ( tseq.next() ) {
                    if ( geom_.readDataPos( tseq, icPos, v3 ) ) {
                        double w = weightCoord_
                                  .readDoubleCoord( tseq, icWeight );
                        if ( ! Double.isNaN( w ) ) {
                            binBag.addToBin( skyPixer.getIndex( v3 ), w );
                        }
                    }
                }
            }
            return binBag;
        }

        /**
         * Given a prepared data structure, paints the results it represents
         * onto a graphics context appropriate for this layer drawing.
         *
         * @param  g  graphics context
         * @param  binBag   histogram containing sky pixel values
         */
        private void paintBins( Graphics g, IntegerBinBag binBag ) {
            Rectangle bounds = surface_.getPlotBounds();

            /* Work out how to scale binbag values to turn into
             * entries in a colour map.  The first entry in the colour map
             * (index zero) corresponds to transparency. */
            Range densRange = new Range( binBag.getBounds() );
            Scaler scaler =
                Scaling.createRangeScaler( style_.scaling_, densRange );
            IndexColorModel colorModel =
                PixelImage.createColorModel( style_.shader_, true );
            int ncolor = colorModel.getMapSize() - 1;

            /* Prepare a screen pixel grid. */
            int nx = bounds.width;
            int ny = bounds.height;
            Gridder gridder = new Gridder( nx, ny );
            int npix = gridder.getLength();
            int[] pixels = new int[ npix ];

            /* Iterate over screen pixel grid pulling samples from the
             * sky pixel grid for each screen pixel.  Note this is only
             * a good strategy if the screen oversamples the sky grid
             * (i.e. if the screen pixels are smaller than the sky pixels). */
            Point2D.Double point = new Point2D.Double();
            double x0 = bounds.x + 0.5;
            double y0 = bounds.y + 0.5;
            SkyPixer skyPixer = createSkyPixer();
            for ( int ip = 0; ip < npix; ip++ ) {
                point.x = x0 + gridder.getX( ip );
                point.y = y0 + gridder.getY( ip );
                double[] dpos = surface_.graphicsToData( point, null );

                /* Positions on the sky always have a value >= 1.
                 * Positions outside the sky coord range are untouched,
                 * so have a value of zero (transparent). */
                if ( dpos != null ) {
                    double dval = binBag.getValue( skyPixer.getIndex( dpos ) );
                    pixels[ ip ] =
                        Math.min( 1 +
                                  (int) ( scaler.scaleValue( dval ) * ncolor ),
                                  ncolor - 1 );
                }
            }

            /* Copy the pixel grid to the graphics context using the
             * requested colour map. */
            new PixelImage( bounds.getSize(), pixels, colorModel )
               .paintPixels( g, bounds.getLocation() );
        }

        /**
         * Constructs an object which can map sky positions to a pixel
         * index in a HEALPix grid.
         *
         * @return   sky pixer for this drawing
         */
        private SkyPixer createSkyPixer() {
            return new SkyPixer( level_ );
        }
    }

    /**
     * Plot layer plan for the sky density map.
     * Note the basic data cached in the plan is currently the sky pixel
     * grid, not the screen pixel grid.  That means that drawing the
     * plot will take a little bit of time (though it will scale only
     * with plot pixel count, not with dataset size).
     */
    private static class SkyDensityPlan {
        final IntegerBinBag binBag_;
        final DataSpec dataSpec_;
        final SkyDataGeom geom_;

        /**
         * Constructor.
         *
         * @param   binBag  data structure containing sky pixel values
         * @param   dataSpec  data specification used to generate binBag
         * @param   geom   sky geometry used to generate binBag
         */
        SkyDensityPlan( IntegerBinBag binBag, DataSpec dataSpec,
                        SkyDataGeom geom ) {
            binBag_ = binBag;
            dataSpec_ = dataSpec;
            geom_ = geom;
        }

        /**
         * Indicates whether this plan can be used for a given plot
         * specification.
         *
         * @param   level  HEALPix level giving sky pixel resolution
         * @param   dataSpec  input data specification
         * @param   geom    sky geometry
         */
        public boolean matches( int level, DataSpec dataSpec,
                                SkyDataGeom geom ) {
             return binBag_.getSize() == new SkyPixer( level ).getPixelCount()
                 && dataSpec_.equals( dataSpec )
                 && geom_.equals( geom );
        }
    }
}
