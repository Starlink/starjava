package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that draws shaped markers with their horizontal
 * and vertical dimensions independently determined by two additional
 * data coordinates.  Autoscaling of points based on data values to
 * ensure sensible marker sizes is optionally available.
 *
 * <p>Singleton class.
 *
 * @author   Mark Taylor
 * @since    14 Jan 2015
 */
public class SizeXyForm implements ShapeForm {

    private static final FloatingCoord XSIZE_COORD = createSizeCoord( false );
    private static final FloatingCoord YSIZE_COORD = createSizeCoord( true );
    private static final ConfigKey<Integer> THICK_KEY = createThicknessKey();
    private static final AuxScale XSIZE_SCALE = new AuxScale( "globalsizex" );
    private static final AuxScale YSIZE_SCALE = new AuxScale( "globalsizey" );
    private static final SizeXyForm instance_ = new SizeXyForm();

    /**
     * Private constructor prevents instantiation.
     */
    private SizeXyForm() {
    }

    public int getBasicPositionCount() {
        return 1;
    }

    public String getFormName() {
        return "SizeXY";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_SIZEXY;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a shaped marker with variable",
            "horizontal and vertical extents at each position.",
            "The X and Y dimensions are determined by two additional",
            "input data values.",
            "</p>",
            "<p>The actual size of the markers depends on the setting of the",
            "<code>" + StyleKeys.AUTOSCALE_PIX.getMeta().getShortName()
                     + "</code>",
            "parameter.",
            "If autoscaling is off, the basic dimensions of each marker",
            "are given by the input data values in units of pixels.",
            "If autoscaling is on, the data values are gathered",
            "for all the currently visible points, and scaling factors",
            "are applied so that the largest ones will be a sensible size",
            "(a few tens of pixels).",
            "This autoscaling happens independently for",
            "the X and Y directions.",
            "The basic sizes can be further adjusted with the",
            "<code>" + StyleKeys.SCALE_PIX.getMeta().getShortName() + "</code>",
            "factor.",
            "</p>",
            "<p>Currently data values of zero always correspond to",
            "marker dimension of zero,",
            "negative data values are not represented,",
            "and the mapping is linear.",
            "An absolute maximum of",
            Integer.toString( PlotUtil.MAX_MARKSIZE ),
            "pixels is also imposed on marker sizes.",
            "Other options may be introduced in future.",
            "</p>",
            "<p>Note: for marker sizes that correspond to data values",
            "in data coordinates,",
            "you may find Error plotting more appropriate.",
            "</p>",
        } );
    }

    public Coord[] getExtraCoords() {
        return new Coord[] {
            XSIZE_COORD,
            YSIZE_COORD,
        };
    }

    public int getExtraPositionCount() {
        return 0;
    }

    public DataGeom adjustGeom( DataGeom geom, DataSpec dataSpec,
                                ShapeStyle style ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            StyleKeys.XYSHAPE,
            THICK_KEY,
            StyleKeys.SCALE_PIX,
            StyleKeys.AUTOSCALE_PIX,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        BasicXYShape bshape = config.get( StyleKeys.XYSHAPE );
        int nthick = config.get( THICK_KEY ).intValue();
        XYShape shape = nthick > 0 ? bshape.toThicker( nthick )
                                   : bshape;
        boolean isAutoscale = config.get( StyleKeys.AUTOSCALE_PIX );
        double scale = config.get( StyleKeys.SCALE_PIX )
                     * ( isAutoscale ? PlotUtil.DEFAULT_MAX_PIXELS : 1 );
        final AuxScale xAutoscale;
        final AuxScale yAutoscale;
        boolean isGlobal = true;
        if ( isAutoscale ) {
            xAutoscale = isGlobal ? XSIZE_SCALE : new AuxScale( "xsize1" );
            yAutoscale = isGlobal ? YSIZE_SCALE : new AuxScale( "ysize1" );
        }
        else {
            xAutoscale = null;
            yAutoscale = null;
        }
        return new SizeXyOutliner( shape, scale, xAutoscale, yAutoscale,
                                   PlotUtil.MAX_MARKSIZE );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static SizeXyForm getInstance() {
        return instance_;
    }

    /**
     * Returns the column index in a tuple sequence at which one of the size
     * coordinates will be found.
     *
     * @param  geom  position geometry
     * @param  isY   false for X coord, true for Y coord
     * @return  size column index
     */
    private static int getSizeCoordIndex( DataGeom geom, boolean isY ) {
        return geom.getPosCoords().length + ( isY ? 1 : 0 );
    }

    /**
     * Returns a coordinates used for acquiring marker dimension in one
     * or other dimension.
     *
     * @param  isY  false for X, true for Y
     * @return   coordinate specification
     */
    private static FloatingCoord createSizeCoord( boolean isY ) {
        InputMeta meta =
            new InputMeta( ( isY ? "y" : "x" ) + "size",
                           ( isY ? "Y" : "X" ) + " Size" );
        meta.setShortDescription( "Marker "
                               + ( isY ? "vertical" : "horizontal" )
                               + " size (pixels or auto)" );
        meta.setXmlDescription( new String[] {
            "<p>",
            isY ? "Vertical" : "Horizontal",
            "extent of each marker.",
            "Units are pixels unless auto-scaling is in effect,",
            "in which case units are arbitrary.",
            "</p>",
        } );
        return FloatingCoord.createCoord( meta, false );
    }

    /**
     * Returns a config key for line thickness.
     *
     * @return  line thickness key
     */
    private static ConfigKey<Integer> createThicknessKey() {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setShortDescription( "Line thickness for open shapes" );
        meta.setXmlDescription( new String[] {
            "<p>Controls the line thickness used when drawing shapes.",
            "Zero, the default value, means a 1-pixel-wide line is used.",
            "Larger values make drawn lines thicker,",
            "but note changing this value will not affect all shapes,",
            "for instance filled rectangles contain no line drawings.",
            "</p>",
        } );
        return StyleKeys.createPaintThicknessKey( meta, 3 );
    }

    /**
     * Outliner implementation for use with SizeXyForm.
     */
    public static class SizeXyOutliner extends PixOutliner {

        private final XYShape shape_;
        private final AuxScale xAutoscale_;
        private final AuxScale yAutoscale_;
        private final short sizeLimit_;
        private final double scale_;
        private final Icon icon_;
        private final Map<ShortPair,Glyph> glyphMap_;

        /**
         * Constructor.
         *
         * @param  shape   shape
         * @param  scale   size scaling factor
         * @param  xAutoscale  key used for autoscaling X extents;
         *                     may be shared with other layers,
         *                     private to this layer, or null for no autoscale
         * @param  yAutoscale  key used for autoscaling Y extents;
         *                     may be shared with other layers,
         *                     private to this layer, or null for no autoscale
         * @param  sizeLimit  maximum X/Y extent in pixels of markers;
         *                    if it's too large, plots may be slow or
         *                    run out of memory
         */
        public SizeXyOutliner( XYShape shape, double scale,
                               AuxScale xAutoscale, AuxScale yAutoscale,
                               short sizeLimit ) {
            shape_ = shape;
            scale_ = scale;
            xAutoscale_ = xAutoscale;
            yAutoscale_ = yAutoscale;
            sizeLimit_ = sizeLimit;
            icon_ = XYShape.createIcon( shape, 14, 10, false );

            /* Consider caching glyphs here.  Small ones are already
             * cached by the XYShape implementation.  However, in cases
             * where the paper stores the glyphs in a collection
             * (SortedPaperType3D), it might be a good idea to ensure
             * that new glyphs with the same content are not painted
             * by caching all (since they may be stored elsewhere anyway).
             * On the other hand, it's possibly significant overhead for
             * those paper types which consume and discard glyphs
             * immediately.  For now, set the cache to null,
             * which disables caching. */
            glyphMap_ = null;
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            Scaling scaling = null;
            if ( xAutoscale_ != null ) {
                int icx = getSizeCoordIndex( geom, false );
                AuxReader xReader =
                    new FloatingCoordAuxReader( XSIZE_COORD, icx, geom, true,
                                                scaling );
                map.put( xAutoscale_, xReader );
            }
            if ( yAutoscale_ != null ) {
                int icy = getSizeCoordIndex( geom, true );
                AuxReader yReader =
                    new FloatingCoordAuxReader( YSIZE_COORD, icy, geom, true,
                                                scaling );
                map.put( yAutoscale_, yReader );
            }
            return map;
        }

        public boolean canPaint( DataSpec dataSpec ) {
            return true;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point2D.Double gpos = new Point2D.Double();
            final int icxSize = getSizeCoordIndex( geom, false );
            final int icySize = getSizeCoordIndex( geom, true );
            final double xscale =
                scale_ * getBaseScale( surface, auxSpans, xAutoscale_ );
            final double yscale =
                scale_ * getBaseScale( surface, auxSpans, yAutoscale_ );
            Rectangle bounds = surface.getPlotBounds();
            final short xmax = strunc( bounds.width * 2 );
            final short ymax = strunc( bounds.height * 2 );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gpos ) ) {
                        double xsize =
                            XSIZE_COORD.readDoubleCoord( tuple, icxSize );
                        double ysize =
                            YSIZE_COORD.readDoubleCoord( tuple, icySize );
                        if ( PlotUtil.isFinite( xsize ) &&
                             PlotUtil.isFinite( ysize ) ) {
                            short ixsize = sround( xsize * xscale, xmax );
                            short iysize = sround( ysize * yscale, ymax );
                            Glyph glyph = getGlyph( ixsize, iysize );
                            paperType.placeGlyph( paper, gpos.x, gpos.y,
                                                  glyph, color );
                        }
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final GPoint3D gpos = new GPoint3D();
            final int icxSize = getSizeCoordIndex( geom, false );
            final int icySize = getSizeCoordIndex( geom, true );
            final double xscale =
                scale_ * getBaseScale( surface, auxSpans, xAutoscale_ );
            final double yscale =
                scale_ * getBaseScale( surface, auxSpans, yAutoscale_ );
            Rectangle bounds = surface.getPlotBounds();
            final short xmax = strunc( bounds.width * 2 );
            final short ymax = strunc( bounds.height * 2 );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gpos ) ) {
                        double xsize =
                            XSIZE_COORD.readDoubleCoord( tuple, icxSize );
                        double ysize =
                            YSIZE_COORD.readDoubleCoord( tuple, icySize );
                        if ( PlotUtil.isFinite( xsize ) &&
                             PlotUtil.isFinite( ysize ) ) {
                            short ixsize = sround( xsize * xscale, xmax );
                            short iysize = sround( ysize * yscale, ymax );
                            Glyph glyph = getGlyph( ixsize, iysize );
                            paperType.placeGlyph( paper, gpos.x, gpos.y, gpos.z,
                                                  glyph, color );
                        }
                    }
                }
            };
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SizeXyOutliner ) {
                SizeXyOutliner other = (SizeXyOutliner) o;
                return this.shape_.equals( other.shape_ )
                    && PlotUtil.equals( this.xAutoscale_, other.xAutoscale_ )
                    && PlotUtil.equals( this.yAutoscale_, other.yAutoscale_ )
                    && this.scale_ == other.scale_
                    && this.sizeLimit_ == other.sizeLimit_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 77641;
            code = 23 * code + shape_.hashCode();
            code = 23 * code + PlotUtil.hashCode( xAutoscale_ );
            code = 23 * code + PlotUtil.hashCode( yAutoscale_ );
            code = 23 * code + Float.floatToIntBits( (float) scale_ );
            code = 23 * code + sizeLimit_;
            return code;
        }

        /**
         * Returns a glyph for given X and Y pixel extents.
         *
         * @param  xsize  pixels in X direction
         * @param  ysize  pixels in Y direction
         * @return  glyph
         */
        private Glyph getGlyph( short xsize, short ysize ) {
            if ( xsize > sizeLimit_ ) {
                xsize = sizeLimit_;
            }
            if ( ysize > sizeLimit_ ) {
                ysize = sizeLimit_;
            }
            if ( glyphMap_ != null ) {
                ShortPair size = new ShortPair( xsize, ysize );
                Glyph glyph = glyphMap_.get( size );
                if ( glyph == null ) {
                    glyph = shape_.getGlyph( xsize, ysize );
                    glyphMap_.put( size, glyph );
                }
                return glyph;
            }
            else {
                return shape_.getGlyph( xsize, ysize );
            }
        }

        /**
         * Returns a basic scale in one dimension for sizing markers.
         * For autoscale it's determined from the data, otherwise it's 1.
         * It may be subsequently adjusted by the user-supplied
         * scale adjustment.
         *
         * @param  surface  plot surface
         * @param  spanMap  map of ranges calculated as part of
         *                  plot preparation by request
         * @param  autoscale  key for relevant aux scaling
         * @return  basic size scale
         */
        private static double getBaseScale( Surface surface,
                                            Map<AuxScale,Span> spanMap,
                                            AuxScale autoscale ) {
            if ( autoscale != null) {
                Span span = spanMap.get( autoscale );
                double[] bounds = span.getFiniteBounds( true );
                return 1. / bounds[ 1 ];
            }
            else {
                return 1;
            }
        }

        /**
         * Converts an integer value to a short, clipping it to a given
         * maximum.
         *
         * @param  value  input value
         * @return  max  maximum allowed result
         * @return   clipped value
         */
        private static short strunc( int value ) {
            return (short) Math.min( value, Short.MAX_VALUE );
        }

        /**
         * Rounds a double value to a short, clipping it to fall between
         * zero and a given maximum.
         *
         * @param  value  input value, not NaN
         * @param  max  maximum allowed result
         * @return  rounded and clipped value
         */
        private static short sround( double value, short max ) {
            if ( value < 0 ) {
                return (short) 0;
            }
            else if ( value >= max ) {
                return max;
            }
            else {
                return (short) ( value + 0.5 );
            }
        }
    }
}
