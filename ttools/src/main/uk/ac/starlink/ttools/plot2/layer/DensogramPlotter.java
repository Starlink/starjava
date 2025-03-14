package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Scalings;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;

/**
 * Plots a histogram-like density map - a one-dimensional colour bar
 * indicating density on the horizontal axis.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2015
 */
public class DensogramPlotter
        extends Pixel1dPlotter<DensogramPlotter.DensoStyle> {

    /** Config keyset for the colour ramp. */
    public static final RampKeySet RAMP_KEYSET =
        new RampKeySet( "dense", "Density", StyleKeys.createAuxShaders(),
                        Scaling.LINEAR, true );

    /** Config key for the height of the density bar. */
    public static final ConfigKey<Integer> EXTENT_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "size", "Size" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Height in pixels of the density bar" )
           .setXmlDescription( new String[] {
                "<p>Height of the density bar in pixels.",
                "</p>",
            } )
        , 12, 1, 100, false );

    /** Config key for the position of the density bar. */
    public static final ConfigKey<Double> POSITION_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "pos", "Position" )
           .setStringUsage( "<fraction>" )
           .setShortDescription( "Location on plot of density bar,"
                               + " in range 0..1" )
           .setXmlDescription( new String[] {
                "<p>Determines where on the plot region the density bar",
                "appears.",
                "The value should be in the range 0..1;",
                "zero corresponds to the bottom of the plot",
                "and one to the top.",
                "</p>",
            } )
        , 0.05, 0, 1, false );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     */
    public DensogramPlotter( FloatingCoord xCoord, boolean hasWeight ) {
        super( xCoord, hasWeight, (ConfigKey<Unit>) null,
               "Densogram", ResourceIcon.FORM_DENSOGRAM );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Represents smoothed density of data values",
            "along the horizontal axis using a colourmap.",
            "This is like a",
            "<ref id='layer-kde'>Kernel Density Estimate</ref>",
            "(smoothed histogram with bins 1 pixel wide),",
            "but instead of representing the data extent vertically",
            "as bars or a line,",
            "values are represented by a fixed-size pixel-width column",
            "of a colour from a colour map.",
            "A smoothing kernel, whose width and shape may be varied,",
            "is applied to each data point.",
            "</p>",
            getWeightingDescription(),
            "<p>This is a rather unconventional way to represent density data,",
            "and this plotting mode is probably not very useful.",
            "But hey, nobody's forcing you to use it.",
            "</p>",
        } );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.add( StyleKeys.COLOR );
        list.add( SMOOTHSIZER_KEY );
        list.add( StyleKeys.SIDEWAYS );
        list.add( KERNEL_KEY );
        list.addAll( Arrays.asList( RAMP_KEYSET.getKeys() ) );
        list.add( StyleKeys.CUMULATIVE );
        list.add( EXTENT_KEY );
        list.add( POSITION_KEY );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public DensoStyle createStyle( ConfigMap config ) {
        Color baseColor = config.get( StyleKeys.COLOR );
        RampKeySet.Ramp ramp = RAMP_KEYSET.createValue( config );
        BinSizer sizer = config.get( SMOOTHSIZER_KEY );
        boolean isY = config.get( StyleKeys.SIDEWAYS ).booleanValue();
        Kernel1dShape kernelShape = config.get( KERNEL_KEY );
        Combiner combiner = Combiner.SUM;
        Cumulation cumul = config.get( StyleKeys.CUMULATIVE );
        int extent = config.get( EXTENT_KEY );
        double position = config.get( POSITION_KEY );
        return new DensoStyle( baseColor, ramp.getShader(), ramp.getScaling(),
                               ramp.getDataClip(), isY, kernelShape, combiner,
                               sizer, cumul, extent, position );
    }

    public Object getRangeStyleKey( DensoStyle style ) {
        return null;
    }

    protected void paintBins( PlanarSurface surface, BinArray binArray,
                              DensoStyle style, Graphics2D g ) {

        /* Get the data values for each pixel position. */
        boolean isY = style.isY_;
        Axis xAxis = surface.getAxes()[ isY ? 1 : 0 ];
        Combiner combiner = style.combiner_;
        Kernel1d kernel = createKernel( style.kernelShape_, style.sizer_,
                                        xAxis,
                                        ! combiner.getType().isExtensive() );
        double[] bins = getDataBins( binArray, xAxis, kernel,
                                     Normalisation.NONE,
                                     style.combiner_.getType(), Unit.UNIT,
                                     style.cumul_ );

        /* Work out the Y axis bounds. */
        Rectangle bounds = surface.getPlotBounds();
        int gy0 = isY ? ( bounds.x
                          + (int) ( ( bounds.width - style.extent_ ) *
                                    ( style.position_ ) ) )
                      : ( bounds.y
                          + (int) ( ( bounds.height - style.extent_ ) * 
                                    ( 1.0 - style.position_ ) ) );

        /* Work out the range of bin indices that need to be painted. */
        int ixlo = binArray.getBinIndex( isY ? bounds.y : bounds.x );
        int ixhi = binArray.getBinIndex( isY ? bounds.y + bounds.height
                                             : bounds.x + bounds.width );
        int np = ixhi - ixlo;

        /* Get range. */
        Scaling scaling = style.scaling_;
        Ranger ranger = Scalings.createRanger( new Scaling[] { scaling } );
        for ( int ip = 0; ip < np; ip++ ) {
            ranger.submitDatum( bins[ ixlo + ip ] );
        }
        Span span = ranger.createSpan();

        /* Do the painting. */
        if ( span.getHigh() > span.getLow() ) {
            Scaler scaler = span.createScaler( scaling, style.dataclip_ );
            float[] baseRgba = style.baseColor_.getRGBComponents( null );
            float[] rgba = new float[ 4 ];
            Color color0 = g.getColor();
            boolean isLog = scaler.isLogLike();
            for ( int ip = 0; ip < np; ip++ ) {
                int ix = ixlo + ip;
                int gx = binArray.getGraphicsCoord( ix );
                double dy = bins[ ix ];
                if ( ! Double.isNaN( dy ) ) {
                    double sy = scaler.scaleValue( dy );
                    if ( isLog && sy < 0 ) {
                        sy = 0;
                    }
                    System.arraycopy( baseRgba, 0, rgba, 0, 4 );
                    style.shader_.adjustRgba( rgba, (float) sy );
                    Color color =
                        new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
                    g.setColor( color );
                    if ( isY ) {
                        g.fillRect( gy0, gx, style.extent_, 1 );
                    }
                    else {
                        g.fillRect( gx, gy0, 1, style.extent_ );
                    }
                }
            }
            g.setColor( color0 );
        }
    }

    protected boolean isY( DensoStyle style ) {
        return style.isY_;
    }

    protected LayerOpt getLayerOpt( DensoStyle style ) {
        return LayerOpt.OPAQUE;
    }

    protected int getPixelPadding( DensoStyle style, PlanarSurface surf ) {
        boolean isY = style.isY_;
        Kernel1d kernel =
            createKernel( style.kernelShape_, style.sizer_,
                          surf.getAxes()[ isY ? 1 : 0 ],
                          ! style.combiner_.getType().isExtensive() );
        return getEffectiveExtent( kernel );
    }

    protected Combiner getCombiner( DensoStyle style ) {
        return style.combiner_;
    }

    protected void extendPixel1dCoordinateRanges( Range[] ranges,
                                                  Scale[] scales,
                                                  DensoStyle style,
                                                  DataSpec dataSpec,
                                                  DataStore dataStore ) {
        // no-op
    }

    protected ReportMap getPixel1dReport( Pixel1dPlan plan, DensoStyle style,
                                          boolean xLog ) {
        Axis xAxis = plan.xAxis_;
        BinSizer sizer = style.sizer_;
        double[] dlimits = xAxis.getDataLimits();
        double sSmoothWidth =
            sizer.getScaleWidth( xAxis.getScale(), dlimits[ 0 ], dlimits[ 1 ],
                                 false );
        ReportMap report = new ReportMap();
        report.put( SMOOTHWIDTH_KEY, sSmoothWidth );
        return report;
    }

    /**
     * Plotting style for this class.
     */
    public static class DensoStyle implements Style {

        final Color baseColor_;
        final Shader shader_;
        final Scaling scaling_;
        final Subrange dataclip_;
        final boolean isY_;
        final Kernel1dShape kernelShape_;
        final Combiner combiner_;
        final BinSizer sizer_;
        final Cumulation cumul_;
        final int extent_;
        final double position_;

        /**
         * Constructor.
         *
         * @param  baseColor   base colour
         * @param  shader    colour ramp shader
         * @param  scaling   colour ramp scaling function
         * @param  dataclip  input value subrange
         * @param  isY     if true, plotted sideways
         * @param  kernelShape   smoothing kernel shape
         * @param  combiner   pixel bin aggregation mode
         * @param  sizer    smoothing width specification
         * @param  cumul  are bins painted cumulatively
         * @param  extent   height in pixels of density bar
         * @param  position   fractional location of density bar (0..1)
         */
        public DensoStyle( Color baseColor, Shader shader, Scaling scaling,
                           Subrange dataclip, boolean isY,
                           Kernel1dShape kernelShape, Combiner combiner,
                           BinSizer sizer, Cumulation cumul,
                           int extent, double position ) {
            baseColor_ = baseColor;
            shader_ = shader;
            scaling_ = scaling;
            dataclip_ = dataclip;
            isY_ = isY;
            kernelShape_ = kernelShape;
            combiner_ = combiner;
            sizer_ = sizer;
            cumul_ = cumul;
            extent_ = extent;
            position_ = position;
        }

        public Icon getLegendIcon() {
            return Shaders.createShaderIcon( shader_, true, 10, 8, 1, 2 );
        }

        @Override
        public int hashCode() {
            int code = 3455;
            code = 23 * code + baseColor_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + ( isY_ ? 19 : 101 );
            code = 23 * code + kernelShape_.hashCode();
            code = 23 * code + combiner_.hashCode();
            code = 23 * code + sizer_.hashCode();
            code = 23 * code + cumul_.hashCode();
            code = 23 * code + extent_;
            code = 23 * code + Float.floatToIntBits( (float) position_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DensoStyle ) {
                DensoStyle other = (DensoStyle) o;
                return this.baseColor_.equals( other.baseColor_ )
                    && this.shader_.equals( other.shader_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && this.isY_ == other.isY_
                    && this.kernelShape_.equals( other.kernelShape_ )
                    && this.combiner_.equals( other.combiner_ )
                    && this.sizer_.equals( other.sizer_ )
                    && this.cumul_ == other.cumul_
                    && this.extent_ == other.extent_
                    && this.position_ == other.position_;
            }
            else {
                return false;
            }
        }
    }
}
