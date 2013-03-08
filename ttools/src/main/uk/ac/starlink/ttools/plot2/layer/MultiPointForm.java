package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that draws shapes based on the positions of
 * more than one data point.
 * The actual coordinates required (defining one or more non-central
 * data positions) are defined by a supplied {@link MultiPointCoordSet}
 * and those coordinates are then plotted by a corresponding
 * {@link uk.ac.starlink.ttools.plot.ErrorRenderer}.
 * ErrorRenderer may be a slightly misleading name in this context, but you
 * can think of any of these multi-point shapes as a generalisation of
 * error bars.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class MultiPointForm implements ShapeForm {

    private final String name_;
    private final Icon icon_;
    private final MultiPointCoordSet extraCoordSet_;
    private final boolean canScale_;
    private final MultiPointConfigKey rendererKey_;

    /**
     * Constructor.
     *
     * @param  name   shapeform name
     * @param  icon   shapeform icon
     * @param  extraCoordSet  defines the extra positional coordinates 
     *                        used to plot multipoint shapes
     * @param  canScale  true if a configuration option to scale the shapes
     *                   should be supplied
     * @param  rendererKey  config key for the renderer; provides option to
     *                      vary the shape, but any renderer specified by it
     *                      must be expecting data corresponding to the
     *                      <code>extraCoordSet</code> parameter
     */
    public MultiPointForm( String name, Icon icon,
                           MultiPointCoordSet extraCoordSet, boolean canScale,
                           MultiPointConfigKey rendererKey ) {
        name_ = name;
        icon_ = icon;
        extraCoordSet_ = extraCoordSet;
        canScale_ = canScale;
        rendererKey_ = rendererKey;
    }

    public String getFormName() {
        return name_;
    }

    public Icon getFormIcon() {
        return icon_;
    }

    public Coord[] getExtraCoords() {
        return extraCoordSet_.getCoords();
    }

    public ConfigKey[] getConfigKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( rendererKey_ );
        if ( canScale_ ) {
            list.add( StyleKeys.SCALE );
            list.add( StyleKeys.AUTOSCALE );
        }
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public Outliner createOutliner( ConfigMap config ) {
        ErrorRenderer renderer = config.get( rendererKey_ );
        ErrorMode[] errorModes = rendererKey_.getErrorModes();
        double scale = canScale_ ? config.get( StyleKeys.SCALE ) : 1;
        boolean isAutoscale = canScale_ && config.get( StyleKeys.AUTOSCALE );
        return new MultiPointOutliner( renderer, errorModes, scale,
                                       isAutoscale );
    }

    /**
     * Returns a MultiPointForm instance for drawing arrows from the
     * central position to another position.
     *
     * @param  extraCoordSet  nDataDim-element coord set that defines one
     *                        extra data position, the (unscaled) endpoint
     *                        of the vector
     * @param  canScale  whether to offer vector size scaling
     * @return  new vector form instance
     */
    public static MultiPointForm
                  createVectorForm( MultiPointCoordSet extraCoordSet,
                                    boolean canScale ) {
        return new MultiPointForm( "Vector",
                                   PlotUtil.icon( "form-vector.gif" ),
                                   extraCoordSet, canScale,
                                   StyleKeys.VECTOR_SHAPE );
    }

    /**
     * Returns a MultiPointForm instance for drawing ellipses around the
     * central position.
     *
     * @param  extraCoordSet  3-element coord set containing major/minor
     *                        radius (order not significant) and
     *                        position angle in degrees
     * @param  canScale  whether to offer vector size scaling
     * @return  new vector form instance
     */
    public static MultiPointForm
                  createEllipseForm( MultiPointCoordSet extraCoordSet,
                                     boolean canScale ) {
        return new MultiPointForm( "Ellipse",
                                   PlotUtil.icon( "form-ellipse2.gif" ),
                                   extraCoordSet, canScale,
                                   StyleKeys.ELLIPSE_SHAPE );
    }

    /**
     * Returns the column index in a tuple sequence at which the
     * extra (multi-point) coordinates start.
     *
     * @param  geom  data position geometry
     * @return  first non-central position coordinate index
     *          (others follow contiguously)
     */
    private static int getExtrasCoordIndex( DataGeom geom ) {
        return geom.getPosCoords().length;
    }

    /**
     * Outliner implementation for use with this class.
     */
    private class MultiPointOutliner extends PixOutliner {
        private final ErrorRenderer renderer_;
        private final ErrorMode[] modes_;
        private final double scale_;
        private final boolean isAutoscale_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  renderer  multi-point shape drawer
         * @param  modes   used with renderer to define icon shape
         * @param  scale   scaling adjustment factor
         * @param  isAutoscale  true if initial size scaling is done
         *                      from the data
         */
        MultiPointOutliner( ErrorRenderer renderer, ErrorMode[] modes,
                            double scale, boolean isAutoscale ) {
            renderer_ = renderer;
            modes_ = modes;
            scale_ = scale;
            isAutoscale_ = isAutoscale;
            icon_ = renderer.getLegendIcon( modes, 14, 10, 1, 1 );
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            if ( isAutoscale_ ) {
                SizeScale scale = new SizeScale( this );
                map.put( scale, scale.createAuxReader( geom, extraCoordSet_ ) );
            }
            return map;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType2D paperType ) {
            int ndim = surface.getDataDimCount();
            final int nextra = extraCoordSet_.getPointCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nextra ][ ndim ];
            final Point gpos0 = new Point();
            final int icExtra = getExtrasCoordIndex( geom );
            double scale = scale_ * getBaseScale( surface, auxRanges );
            final Offsetter offsetter = createOffsetter( surface, scale );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos0 ) &&
                         surface.dataToGraphics( dpos0, true, gpos0 ) &&
                         extraCoordSet_.readPoints( tseq, icExtra, dpos0,
                                                    dposExtras ) ) {
                        int[] xoffs = new int[ nextra ];
                        int[] yoffs = new int[ nextra ];
                        offsetter.calculateOffsets( dpos0, gpos0, dposExtras,
                                                    xoffs, yoffs );
                        Glyph glyph =
                            new MultiPointGlyph( renderer_, xoffs, yoffs );
                        paperType.placeGlyph( paper, gpos0.x, gpos0.y,
                                              glyph, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            int ndim = surface.getDataDimCount();
            final int nextra = extraCoordSet_.getPointCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nextra ][ ndim ];
            final Point gpos0 = new Point();
            final int icExtra = getExtrasCoordIndex( geom );
            final double[] zloc = new double[ 1 ];
            double scale = scale_ * getBaseScale( surface, auxRanges );
            final Offsetter offsetter = createOffsetter( surface, scale );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos0 ) &&
                         surface.dataToGraphicZ( dpos0, true, gpos0, zloc ) &&
                         extraCoordSet_.readPoints( tseq, icExtra, dpos0,
                                                    dposExtras ) ) {
                        double dz0 = zloc[ 0 ];
                        int[] xoffs = new int[ nextra ];
                        int[] yoffs = new int[ nextra ];
                        offsetter.calculateOffsets( dpos0, gpos0, dposExtras,
                                                    xoffs, yoffs );
                        Glyph glyph =
                            new MultiPointGlyph( renderer_, xoffs, yoffs );
                        paperType.placeGlyph( paper, gpos0.x, gpos0.y, dz0,
                                              glyph, color );
                    }
                }
            };
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MultiPointOutliner ) {
                MultiPointOutliner other = (MultiPointOutliner) o;
                return this.renderer_.equals( other.renderer_ )
                    && Arrays.equals( this.modes_, other.modes_ )
                    && this.isAutoscale_ == other.isAutoscale_
                    && this.scale_ == other.scale_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 3203;
            code = code * 23 + renderer_.hashCode();
            code = code * 23 + Arrays.hashCode( modes_ );
            code = code * 23 + ( isAutoscale_ ? 11 : 13 );
            code = code * 23 + Float.floatToIntBits( (float) scale_ );
            return code;
        }

        /**
         * Returns the base size scaling value.
         * Manual adjustment may be applied on top of this value.
         *
         * @param   surface  plot surface
         * @param   auxRanges  ranges calculated from data by request
         */
        private double getBaseScale( Surface surface,
                                     Map<AuxScale,Range> auxRanges ) {

            /* If no autoscale, just return 1. */
            if ( ! isAutoscale_ ) {
                return 1;
            }

            /* Otherwise, pick a scale so that the largest sized shape
             * painted will be a few tens of pixels long. */
            Range sizeRange = auxRanges.get( new SizeScale( this ) );
            double[] bounds = sizeRange.getFiniteBounds( false );
            double gmax = Math.max( -bounds[ 0 ], +bounds[ 1 ] );
            assert gmax >= 0;
            return gmax == 0 ? 1 : 32 / gmax;
        }

        /**
         * Returns an object that can calculate the actual graphics offset
         * positions for each data point.
         *
         * @param  surface  plot surface
         * @param  scale   scaling factor
         */
        private Offsetter createOffsetter( Surface surface,
                                           final double scale ) {
            final int nextra = extraCoordSet_.getPointCount();
            if ( scale == 1.0 ) {
                return new Offsetter( surface, nextra );
            }
            else {

                // This scaling is not great, because it is working on the
                // integer graphics coordinates, so it will lose precision.
                // To retain the precision, there would need to be a
                // Surface.dataToGraphicsOffset method which outputs
                // floating point rather than integer graphics coordinates
                // (e.g. Point2D.Doubles rather than Points).
                // There's no technical reason why this can't be done, but
                // the Surface interface doesn't currently work that way.
                return new Offsetter( surface, nextra ) {
                    @Override
                    void calculateOffsets( double[] dpos0, Point gpos0,
                                           double[][] dposExtras, 
                                           int[] xoffs, int[] yoffs ) {
                        super.calculateOffsets( dpos0, gpos0, dposExtras,
                                                xoffs, yoffs );
                        for ( int ie = 0; ie < nextra; ie++ ) {
                            xoffs[ ie ] =
                                (int) Math.round( xoffs[ ie ] * scale );
                            yoffs[ ie ] =
                                (int) Math.round( yoffs[ ie ] * scale );
                        } 
                    }
                };
            }
        }
    }

    /**
     * Calculates the actual graphics positions at each row
     * for a given multipoint shape.
     */
    private static class Offsetter {
        final Surface surface_;
        final int nextra_;
        final Point gp_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  nextra  number of non-central data coordinates
         */
        Offsetter( Surface surface, int nextra ) {
            surface_ = surface;
            nextra_ = nextra;
            gp_ = new Point();
        }

        /**
         * Converts data values read from the tuple sequence into a list
         * of graphics coordinates suitable for feeding to the ErrorRenderer.
         * The result is returned by filling a supplied pair of arrays
         * giving X and Y offsets in graphics coordinates from the
         * (supplied) central point, so (0,0) indicates an extra point
         * on top of the central one.
         *
         * @param  dpos0  nDataDim-element array giving coordinates in data
         *                space of the central position
         * @param  gpos0  central position in graphics coordinates 
         * @param  dposExtras   [nExtra][nDataDim]-shaped array containing the
         *                      coordinates in data space of the extra
         *                      (non-central) positions
         * @param  xoffs  nExtra-element array to receive graphics X offsets
         * @param  yoffs  nExtra-element array to receive graphics Y offsets
         */
        void calculateOffsets( double[] dpos0, Point gpos0,
                               double[][] dposExtras,
                               int[] xoffs, int[] yoffs ) {
            int gx0 = gpos0.x;
            int gy0 = gpos0.y;
            for ( int ie = 0; ie < nextra_; ie++ ) {
                final int gx;
                final int gy;
                if ( surface_.dataToGraphicsOffset( dpos0, gpos0,
                                                    dposExtras[ ie ], false,
                                                    gp_ ) ) {
                    gx = gp_.x - gx0;
                    gy = gp_.y - gy0;
                }
                else {
                    gx = 0;
                    gy = 0;
                }
                xoffs[ ie ] = gx;
                yoffs[ ie ] = gy;
            }
        }
    }

    /**
     * Glyph implementation to draw a multipoint shape.
     */
    private static class MultiPointGlyph implements Glyph {
        private final ErrorRenderer renderer_;
        private final int[] xoffs_;
        private final int[] yoffs_;

        /**
         * Constructor.
         *
         * @param  renderer  multipoint shape renderer
         * @param  xoffs  graphics position X-coordinate offsets
         * @param  yoffs  graphics position Y-coordinate offsets.
         */
        MultiPointGlyph( ErrorRenderer renderer, int[] xoffs, int[] yoffs ) {
            renderer_ = renderer;
            xoffs_ = xoffs;
            yoffs_ = yoffs;
        }

        public void paintGlyph( Graphics g ) {
            renderer_.drawErrors( g, 0, 0, xoffs_, yoffs_ );
        }

        public Pixellator getPixelOffsets( Rectangle clip ) {
            return renderer_.getPixels( clip, 0, 0, xoffs_, yoffs_ );
        }
    }

    /**
     * AuxScale key for calculating multipoint shape size ranges.
     * Currently this is non-shared, but it could be shared for
     * global size ranging if required, since it just looks at the
     * graphics offsets and works out the size from them.
     */
    private static class SizeScale extends AuxScale {
        private final MultiPointOutliner outliner_;
        private final boolean scaleFromVisible_;

        /**
         * Constructor.
         *
         * @param  outliner  outline drawing object
         */
        SizeScale( MultiPointOutliner outliner ) {
            super( "autosize" );
            outliner_ = outliner;
            scaleFromVisible_ = true;
        }

        AuxReader createAuxReader( final DataGeom geom,
                                   final MultiPointCoordSet extraCoordSet ) {
            final int ndim = geom.getDataDimCount();
            final int nextra = extraCoordSet.getPointCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nextra ][ ndim ];
            final int icExtra = getExtrasCoordIndex( geom );
            final Point gpos0 = new Point();
            final Point gpos1 = new Point();
            return new AuxReader() {
                public void updateAuxRange( Surface surface,
                                            TupleSequence tseq, Range range ) {
                    if ( geom.readDataPos( tseq, 0, dpos0 ) &&
                         surface.dataToGraphics( dpos0, scaleFromVisible_,
                                                 gpos0 ) &&
                         extraCoordSet.readPoints( tseq, icExtra, dpos0,
                                                   dposExtras ) ) {
                        for ( int ie = 0; ie < nextra; ie++ ) {
                            if ( surface
                                .dataToGraphicsOffset( dpos0, gpos0,
                                                       dposExtras[ ie ], false,
                                                       gpos1 ) ) {
                                range.submit( gpos1.x - gpos0.x );
                                range.submit( gpos1.y - gpos0.y );
                            }
                        }
                    }
                }
            };
        }

        public int hashCode() {
            return outliner_.hashCode();
        }

        public boolean equals( Object other ) {
            return other instanceof SizeScale
                && this.outliner_.equals( ((SizeScale) other).outliner_ );
        }
    }
}
