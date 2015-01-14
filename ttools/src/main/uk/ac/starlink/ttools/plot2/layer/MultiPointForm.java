package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
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
 * ShapeForm implementation that draws shapes based on a single main
 * position, and a number of additional positions supplied as
 * {@link ShapeForm#getExtraCoords extra} coordinates.
 * The extra coordinates required (defining one or more non-central
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
    private final String description_;
    private final MultiPointCoordSet extraCoordSet_;
    private final boolean canScale_;
    private final MultiPointConfigKey rendererKey_;

    /**
     * Constructor.
     *
     * @param  name   shapeform name
     * @param  icon   shapeform icon
     * @param  description  XML description
     * @param  extraCoordSet  defines the extra positional coordinates 
     *                        used to plot multipoint shapes
     * @param  canScale  true if a configuration option to scale the shapes
     *                   should be supplied
     * @param  rendererKey  config key for the renderer; provides option to
     *                      vary the shape, but any renderer specified by it
     *                      must be expecting data corresponding to the
     *                      <code>extraCoordSet</code> parameter
     */
    public MultiPointForm( String name, Icon icon, String description,
                           MultiPointCoordSet extraCoordSet, boolean canScale,
                           MultiPointConfigKey rendererKey ) {
        name_ = name;
        icon_ = icon;
        description_ = description;
        extraCoordSet_ = extraCoordSet;
        canScale_ = canScale;
        rendererKey_ = rendererKey;
    }

    public int getPositionCount() {
        return 1;
    }

    public String getFormName() {
        return name_;
    }

    public Icon getFormIcon() {
        return icon_;
    }

    public String getFormDescription() {
        return description_;
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
     * @param  name  form name
     * @param  extraCoordSet  nDataDim-element coord set that defines one
     *                        extra data position, the (unscaled) endpoint
     *                        of the vector
     * @param  canScale  whether to offer vector size scaling
     * @return  new vector form instance
     */
    public static MultiPointForm
                  createVectorForm( String name,
                                    MultiPointCoordSet extraCoordSet,
                                    boolean canScale ) {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots directed lines from the data position",
            "given delta values for the coordinates.",
            "The plotted markers are typically little arrows,",
            "but there are other options.",
            "</p>",
            "<p>In some cases such delta values may be",
            "the actual magnitude required for the plot,",
            "but often the vector data represents a value",
            "which has a different magnitude or is in different units",
            "to the positional data.",
            "As a convenience for this case, the plotter can optionally",
            "scale the magnitudes of all the vectors",
            "to make them a sensible size,",
            "so by default the largest ones are a few tens of pixels long.",
            "This auto-scaling is in operation by default,",
            "but it can be turned off or adjusted with the scaling and",
            "auto-scaling options.",
            "</p>",
        } );
        return new MultiPointForm( name, ResourceIcon.FORM_VECTOR,
                                   descrip, extraCoordSet, canScale,
                                   StyleKeys.VECTOR_SHAPE );
    }

    /**
     * Returns a MultiPointForm instance for drawing ellipses around the
     * central position.
     *
     * @param  name  form name
     * @param  extraCoordSet  3-element coord set containing major/minor
     *                        radius (order not significant) and
     *                        position angle in degrees
     * @param  canScale  whether to offer vector size scaling
     * @return  new vector form instance
     */
    public static MultiPointForm
                  createEllipseForm( String name,
                                     MultiPointCoordSet extraCoordSet,
                                     boolean canScale ) {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots an ellipse (or rectangle, triangle,",
            "or other similar figure)",
            "defined by two principal radii and",
            "an optional rotation angle.",
            "</p>",
        } );
        return new MultiPointForm( name, ResourceIcon.FORM_ELLIPSE,
                                   descrip, extraCoordSet, canScale,
                                   StyleKeys.ELLIPSE_SHAPE );
    }

    /**
     * Returns a MultiPointForm for drawing error bars.
     *
     * @param  name  form name
     * @param  extraCoordSet  coord set specifying error bar position endpoints
     * @param  rendererKey   config key for specifying error renderers
     * @return  new error form instance
     */
    public static MultiPointForm
                  createErrorForm( String name,
                                   MultiPointCoordSet extraCoordSet,
                                   MultiPointConfigKey rendererKey ) {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots symmetric or asymmetric error bars in some or",
            "all of the plot dimensions.",
            "The shape of the error \"bars\" is quite configurable,",
            "including (for 2-d and 3-d errors)",
            "ellipses, rectangles etc aligned with the axes.",
            "</p>",
        } );
        return new MultiPointForm( name, ResourceIcon.FORM_ERROR, descrip,
                                   extraCoordSet, false, rendererKey );
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
     * Outliner implementation for use with MultiPointForms.
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
        public MultiPointOutliner( ErrorRenderer renderer, ErrorMode[] modes,
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
            final Point2D.Double gpos0 = new Point2D.Double();
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
            final Point2D.Double gpos0 = new Point2D.Double();
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
            int nextra = extraCoordSet_.getPointCount();
            return new Offsetter( surface, nextra, scale );
        }
    }

    /**
     * Calculates the actual graphics positions at each row
     * for a given multipoint shape.
     */
    private static class Offsetter {
        final Surface surface_;
        final int nextra_;
        final double scale_;
        final Point2D.Double gp_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  nextra  number of non-central data coordinates
         */
        Offsetter( Surface surface, int nextra, double scale ) {
            surface_ = surface;
            nextra_ = nextra;
            scale_ = scale;
            gp_ = new Point2D.Double();
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
        void calculateOffsets( double[] dpos0, Point2D.Double gpos0,
                               double[][] dposExtras,
                               int[] xoffs, int[] yoffs ) {
            double gx0 = gpos0.x;
            double gy0 = gpos0.y;
            for ( int ie = 0; ie < nextra_; ie++ ) {
                final int gx;
                final int gy;
                if ( surface_.dataToGraphicsOffset( dpos0, gpos0,
                                                    dposExtras[ ie ], false,
                                                    gp_ ) ) {
                    gx = (int) Math.round( ( gp_.x - gx0 ) * scale_ );
                    gy = (int) Math.round( ( gp_.y - gy0 ) * scale_ );
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

        public Pixer createPixer( Rectangle clip ) {
            final Pixellator pixellator =
                renderer_.getPixels( clip, 0, 0, xoffs_, yoffs_ );
            pixellator.start();
            return new Pixer() {
                public boolean next() {
                    return pixellator.next();
                }
                public int getX() {
                    return pixellator.getX();
                }
                public int getY() {
                    return pixellator.getY();
                }
            };
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
            final Point2D.Double gpos0 = new Point2D.Double();
            final Point2D.Double gpos1 = new Point2D.Double();
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
