package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
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
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that plots a genuine density map (2-d histogram).
 * It presents a single Decal, no glyph.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2015
 */
public class DensityPlotter implements Plotter<DensityPlotter.DenseStyle> {

    private final boolean transparent_;
    private final CoordGroup coordGrp_;
    private final FloatingCoord weightCoord_;
    private final boolean reportAuxKeys_;

    /** Report key for bin X dimension in data coordinates. */
    public static final ReportKey<Double> REPKEY_XBIN =
        new ReportKey<Double>( new ReportMeta( "xbin_size",
                                               "Bin X dimension"
                                             + " in data coords" ),
                               Double.class, true );

    /** Report key for bin Y dimension in data coordinates. */
    public static final ReportKey<Double> REPKEY_YBIN =
        new ReportKey<Double>( new ReportMeta( "ybin_size",
                                               "Bin Y dimension"
                                             + " in data coords" ),
                               Double.class, true );

    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<Integer> BINPIX_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "binpix", "Bin Size" )
           .setShortDescription( "Bin dimension in pixels" )
           .setXmlDescription( new String[] {
                "<p>Determines the dimension of grid bins in pixels.",
                "Bins are square in pixel dimensions, and this parameter",
                "gives the extent in pixels along each side.",
                "Currently, only integer values are allowed.",
                "</p>",
            } )
        , 2, 1, 50 );
    private static final ConfigKey<Double> OPAQUE_KEY = StyleKeys.AUX_OPAQUE;

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     * @param  hasWeight    if true, an optional weight coordinate will
     *                      be solicited alongside the positional coordinates
     */
    public DensityPlotter( boolean transparent, boolean hasWeight ) {
        transparent_ = transparent;
        weightCoord_ = hasWeight ? FloatingCoord.WEIGHT_COORD : null;
        Coord[] extraCoords = weightCoord_ == null
                            ? new Coord[ 0 ]
                            : new Coord[] { weightCoord_ };
        coordGrp_ = CoordGroup.createCoordGroup( 1, extraCoords );

        /* Set reportAuxKeys false, since the colour ramp config will
         * usually be controlled globally at the level of the plot. */
        reportAuxKeys_ = false;
    }

    public String getPlotterName() {
        return "Density";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_DENSITY;
    }

    public CoordGroup getCoordGroup() {
        return coordGrp_;
    }

    public boolean hasReports() {
        return false;
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots a density map on the pixel grid of the " )
            .append( "plot surface,\n" )
            .append( "coarsened by a configurable factor.\n" )
            .append( "You can optionally use a weighting for the points,\n" )
            .append( "and you can configure how the points are combined\n" )
            .append( "to produce the output pixel values.\n" )
            .append( "</p>\n" );
        sbuf.append( "<p>" );
        if ( reportAuxKeys_ ) {
            sbuf.append( "There are additional options to adjust\n" )
                .append( "the way data values are mapped to colours.\n" );
        }
        else {
            sbuf.append( "The way that data values are mapped\n" )
                .append( "to colours is usually controlled by options\n" )
                .append( "at the level of the plot itself,\n" )
                .append( "rather than by per-layer configuration.\n" );
        }
        sbuf.append( "</p>\n" );
        return sbuf.toString();
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.add( BINPIX_KEY );
        if ( weightCoord_ != null ) {
            keyList.add( StyleKeys.COMBINER );
        }
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        if ( transparent_ ) {
            keyList.add( OPAQUE_KEY );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    public DenseStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int binpix = config.get( BINPIX_KEY );
        Scaling scaling = ramp.getScaling();
        float scaleAlpha = (float) ( 1.0 / config.get( OPAQUE_KEY ) );
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        Combiner combiner = weightCoord_ == null
                          ? Combiner.COUNT
                          : config.get( StyleKeys.COMBINER );
        return new DenseStyle( binpix, scaling, shader, combiner );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  DenseStyle style ) {
        return new DensityLayer( geom, dataSpec, style );
    }

    /**
     * Style for configuring the density plot.
     */
    public static class DenseStyle implements Style {

        private final int binpix_;
        private final Scaling scaling_;
        private final Shader shader_;
        private final Combiner combiner_;

        /**
         * Constructor.
         *
         * @param   binpix   linear dimension in pixels of the square
         *                   histogram bins
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   shader   colour map
         * @param   combiner  value combination mode for bin calculation
         */
        public DenseStyle( int binpix, Scaling scaling, Shader shader,
                           Combiner combiner ) {
            binpix_ = binpix;
            scaling_ = scaling;
            shader_ = shader;
            combiner_ = combiner;
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
            code = 23 * code + binpix_;
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + combiner_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DenseStyle ) {
                DenseStyle other = (DenseStyle) o;
                return this.binpix_ == other.binpix_
                    && this.scaling_.equals( other.scaling_ )
                    && this.shader_.equals( other.shader_ )
                    && this.combiner_.equals( other.combiner_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for density plotter.
     */
    private class DensityLayer extends AbstractPlotLayer {

        private final DenseStyle dstyle_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  geom  geom
         * @param  dataSpec   data specification
         * @param  style   layer style
         */
        DensityLayer( DataGeom geom, DataSpec dataSpec, DenseStyle style ) {
            super( DensityPlotter.this, geom, dataSpec, style,
                   style.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL );
            dstyle_ = style;
            icWeight_ = weightCoord_ == null
                      ? -1
                      : coordGrp_.getExtraCoordIndex( 0, geom );
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Range> auxRanges,
                                      PaperType ptype ) {
            return new DensityDrawing( surface, auxRanges.get( SCALE ), ptype );
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icWeight_;
                }
                public void adjustAuxRange( Surface surface, TupleSequence tseq,
                                            Range range ) {
                    double[] bounds = readBins( surface, tseq )
                                     .getResult()
                                     .getValueBounds();
                    range.submit( bounds[ 0 ] );
                    range.submit( bounds[ 1 ] );
                }
            } );
            return map;
        }

        /**
         * Returns a GridPixer for a given surface suitable for this layer.
         *
         * @param  surface  plot surface
         * @return   pixer
         */
        GridPixer createGridPixer( Surface surface ) {
            return new GridPixer( surface.getPlotBounds(), dstyle_.binpix_ );
        }

        /**
         * Returns the calculated histogram for this layer on a given
         * plot surface.
         *
         * @param  surface   plot surface
         * @param  tseq  row iterator
         * @return   populated bin list
         */
        private BinList readBins( Surface surface, TupleSequence tseq ) {
            GridPixer pixer = createGridPixer( surface );
            int nbin = pixer.getBinCount();
            Combiner combiner = dstyle_.combiner_;
            BinList binList = combiner.createArrayBinList( nbin );
            DataSpec dataSpec = getDataSpec();
            DataGeom geom = getDataGeom();
            int icPos = coordGrp_.getPosCoordIndex( 0, geom );
            int icWeight = weightCoord_ == null
                         ? -1
                         : coordGrp_.getExtraCoordIndex( 0, geom );
            assert weightCoord_ == null ||
                   weightCoord_ == dataSpec.getCoord( icWeight );
            Point2D.Double gp = new Point2D.Double();
            double[] dpos = new double[ geom.getDataDimCount() ];

            /* Unweighted. */
            if ( icWeight < 0 || dataSpec.isCoordBlank( icWeight ) ) {
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos, dpos ) &&
                         surface.dataToGraphics( dpos, true, gp ) ) {
                        binList.submitToBin( pixer.getIndex( gp ), 1 );
                    }
                }
            }

            /* Weighted. */
            else {
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos, dpos ) &&
                         surface.dataToGraphics( dpos, true, gp ) ) {
                        double w = weightCoord_
                                  .readDoubleCoord( tseq, icWeight );
                        if ( ! Double.isNaN( w ) ) {
                            binList.submitToBin( pixer.getIndex( gp ), w );
                        }
                    }
                }
            }
            return binList;
        }

        /**
         * Drawing implementation for the density map.
         */
        private class DensityDrawing implements Drawing {

            private final Surface surface_;
            private final Range auxRange_;
            private final PaperType paperType_;
            private final GridPixer gridPixer_;

            /**
             * Constructor.
             *
             * @param   surface   plotting surface
             * @param   auxRange  range defining colour scaling
             * @param   paperType  paper type
             */
            DensityDrawing( Surface surface, Range auxRange,
                            PaperType paperType ) {
                surface_ = surface;
                auxRange_ = auxRange;
                paperType_ = paperType;
                gridPixer_ = createGridPixer( surface );
            }

            public Object calculatePlan( Object[] knownPlans,
                                         DataStore dataStore ) {
                int binpix = dstyle_.binpix_;
                Combiner combiner = dstyle_.combiner_;
                DataSpec dataSpec = getDataSpec();
                DataGeom geom = getDataGeom();
                for ( Object plan : knownPlans ) {
                    if ( plan instanceof DensityPlan ) {
                        DensityPlan dplan = (DensityPlan) plan;
                        if ( dplan.matches( binpix, combiner, surface_,
                                            dataSpec, geom ) ) {
                            return dplan;
                        }
                    }
                }
                BinList.Result binResult =
                    readBins( surface_, dataStore.getTupleSequence( dataSpec ) )
                   .getResult();
                return new DensityPlan( binpix, combiner, surface_, dataSpec,
                                        geom, binResult );
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                final DensityPlan dplan = (DensityPlan) plan;
                paperType_.placeDecal( paper, new Decal() {
                    public void paintDecal( Graphics g ) {
                        paintBins( g, dplan.result_ );
                    }
                    public boolean isOpaque() {
                        return dstyle_.isOpaque();
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                ReportMap report = new ReportMap();
                if ( surface_ instanceof PlaneSurface ) {
                    Axis[] axes = ((PlaneSurface) surface_).getAxes();
                    addBinSize( report, REPKEY_XBIN, axes[ 0 ] );
                    addBinSize( report, REPKEY_YBIN, axes[ 1 ] );
                }
                return report;
            }

            /**
             * Attempts to add a bin dimension entry to a given report map
             * for a certain axis.
             *
             * @param  report  map to augment
             * @param  key   report key for new entry
             * @param  axis   axis along which dimension is to be reported
             */
            private void addBinSize( ReportMap report, ReportKey key,
                                     Axis axis ) {
                if ( axis.isLinear() ) {
                    int g0 = axis.getGraphicsLimits()[ 0 ];
                    double pixSize = Math.abs( axis.graphicsToData( g0 + 1 )
                                             - axis.graphicsToData( g0 ) );
                    double binSize = pixSize * dstyle_.binpix_;
                    report.put( key, new Double( binSize ) );
                }
            }

            /**
             * Paints a given bin list onto a graphics context for this
             * drawing.
             *
             * @param   g  graphics context
             * @param   binResult  bin values
             */
            private void paintBins( Graphics g, BinList.Result binResult ) {

                /* Work out how to scale binlist values to turn into
                 * entries in a colour map.  The first entry in the colour map
                 * (index zero) corresponds to transparency. */
                Scaler scaler =
                    Scaling.createRangeScaler( dstyle_.scaling_, auxRange_ );
                IndexColorModel colorModel =
                    PixelImage.createColorModel( dstyle_.shader_, true );
                int ncolor = colorModel.getMapSize() - 1;

                /* Prepare a bin pixel grid. */
                Gridder gridder = gridPixer_.gridder_;
                int nbin = gridder.getLength();
                int[] bins = new int[ nbin ];
                for ( int ib = 0; ib < nbin; ib++ ) {
                    double dval = binResult.getBinValue( ib );
                    if ( ! Double.isNaN( dval ) ) {
                        bins[ ib ] =
                            Math.min( 1 + (int) ( scaler.scaleValue( dval )
                                                  * ncolor ),
                                      ncolor - 1 );
                    }
                }
                new PixelImage( new Dimension( gridPixer_.nx_, gridPixer_.ny_ ),
                                bins, colorModel )
                   .paintScaledPixels( g,
                                       surface_.getPlotBounds().getLocation(),
                                       dstyle_.binpix_ );
            }
        }
    }

    /**
     * Aggregates a BinList with information that characterises its
     * scope of applicability.
     */
    private static class DensityPlan {
        final int binpix_;
        final Combiner combiner_;
        final Surface surface_;
        final DataSpec dataSpec_;
        final DataGeom geom_;
        final BinList.Result result_;

        /**
         * Constructor.
         *
         * @param  binpix   linear bin dimension in pixels
         * @param  combiner  combination method for values
         * @param  surface   plot surface
         * @param  dataSpec   data specification
         * @param  geom     geom
         * @param  result  contains accumulated weight data
         */
        DensityPlan( int binpix, Combiner combiner, Surface surface,
                     DataSpec dataSpec, DataGeom geom, BinList.Result result ) {
            binpix_ = binpix;
            combiner_ = combiner;
            surface_ = surface;
            dataSpec_ = dataSpec;
            geom_ = geom;
            result_ = result;
        }

        /**
         * Indicates whether this plan can be used for a given set
         * of drawing requirements.
         *
         * @param  binpix   linear bin dimension in pixels
         * @param  combiner  combination method for values
         * @param  surface   plot surface
         * @param  dataSpec   data specification
         * @param  geom     geom
         */
        public boolean matches( int binpix, Combiner combiner, Surface surface,
                                DataSpec dataSpec, DataGeom geom ) {
            return binpix_ == binpix
                && combiner_.equals( combiner )
                && surface_.equals( surface )
                && dataSpec_.equals( dataSpec )
                && geom_.equals( geom );
        }
    }

    /**
     * Defines the mapping of graphics coordinates to bin index.
     */
    private static class GridPixer {
        final int binpix_;
        final int x0_;
        final int y0_;
        final int nx_;
        final int ny_;
        final Gridder gridder_;

        /**
         * Constructor.
         *
         * @param  box   rectangle covering the graphics coordinate bounds
         * @param  binpix   linear bin dimension in pixels
         */
        GridPixer( Rectangle box, int binpix ) {
            x0_ = box.x;
            y0_ = box.y;
            nx_ = 1 + ( box.width - 1 ) / binpix;
            ny_ = 1 + ( box.height - 1 ) / binpix;
            gridder_ = new Gridder( nx_, ny_ );
            binpix_ = binpix;
        }

        /**
         * Returns the number of bins covered by this grid.
         *
         * @return   bin count
         */
        public int getBinCount() {
            return gridder_.getLength();
        }

        /**
         * Calculates the grid index for a given graphics position.
         *
         * @param   gp  graphics position
         * @return   grid index
         */
        public int getIndex( Point2D gp ) {
            int ix = (int) ( ( gp.getX() - x0_ ) / binpix_ );
            int iy = (int) ( ( gp.getY() - y0_ ) / binpix_ );
            return gridder_.getIndex( ix, iy );
        }
    }
}
