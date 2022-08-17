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
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that draws shaped markers of a size
 * given by an additional data coordinate.
 * Auto-scaling is provided.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class SizeForm implements ShapeForm {

    private static final FloatingCoord SIZE_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "size", "Size" )
           .setShortDescription( "Marker size (pixels or auto)" )
           .setXmlDescription( new String[] {
                "<p>Size to draw each sized marker.",
                "Units are pixels unless auto-scaling is in effect,",
                "in which case units are arbitrary.",
                "The plotted size is also affected by the",
                "<code>" + StyleKeys.SCALE_PIX.getMeta().getShortName()
                         + "</code>",
                "value.",
                "</p>",
            } )
        , false );
    private static final AuxScale SIZE_SCALE = new AuxScale( "globalsize" );
    private static final SizeForm instance_ = new SizeForm();

    /**
     * Private constructor prevents instantiation.
     */
    private SizeForm() {
    }

    public int getBasicPositionCount() {
        return 1;
    }

    public String getFormName() {
        return "Size";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_SIZE;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a marker of fixed shape but variable size",
            "at each position.",
            "The size is determined by an additional input data value.",
            "</p>",
            "<p>The actual size of the markers depends on the setting of the",
            "<code>" + StyleKeys.AUTOSCALE_PIX.getMeta().getShortName()
                     + "</code>",
            "parameter.",
            "If autoscaling is off, then the basic size of each marker",
            "is the input data value in units of pixels.",
            "If autoscaling is on, then the data values are gathered",
            "for all the currently visible points, and a scaling factor",
            "is applied so that the largest ones will be a sensible size",
            "(a few tens of pixels).",
            "This basic size can be further adjusted with the",
            "<code>" + StyleKeys.SCALE_PIX.getMeta().getShortName()
                     + "</code> factor.",
            "</p>",
            "<p>Currently data values of zero always correspond to",
            "marker size of zero, negative data values are not represented,",
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
            SIZE_COORD,
        };
    }

    public int getExtraPositionCount() {
        return 0;
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            StyleKeys.MARKER_SHAPE,
            StyleKeys.SCALE_PIX,
            StyleKeys.AUTOSCALE_PIX
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
        boolean isAutoscale = config.get( StyleKeys.AUTOSCALE_PIX );
        double scale = config.get( StyleKeys.SCALE_PIX )
                     * ( isAutoscale ? PlotUtil.DEFAULT_MAX_PIXELS : 1 );
        final AuxScale autoscale;
        boolean isGlobal = true;
        if ( isAutoscale ) {
            autoscale = isGlobal ? SIZE_SCALE : new AuxScale( "size1" );
        }
        else {
            autoscale = null;
        }
        return new SizeOutliner( shape, scale, autoscale,
                                 PlotUtil.MAX_MARKSIZE );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static SizeForm getInstance() {
        return instance_;
    }

    /**
     * Returns the column index in a tuple sequence at which the size
     * coordinate will be found.
     *
     * @param  geom  position geometry
     * @return  size column index
     */
    private static int getSizeCoordIndex( DataGeom geom ) {
        return geom.getPosCoords().length;
    }

    /**
     * Outliner implementation for use with SizeForm.
     */
    public static class SizeOutliner extends PixOutliner {
        private final MarkerShape shape_;
        private final AuxScale autoscale_;
        private final double scale_;
        private final int sizeLimit_;
        private final Icon icon_;
        private final Map<Integer,Glyph> glyphMap_;

        /**
         * Constructor.
         *
         * @param  shape  basic marker shape
         * @param  scale  size scaling factor
         * @param  autoscale   key used for autoscaling;
         *                     may be shared with other layers,
         *                     private to this layer, or null for no autoscale
         * @param  sizeLimit  maximum size in pixels of markers;
         *                    if it's too large, plots may be slow or
         *                    run out of memory
         */
        public SizeOutliner( MarkerShape shape, double scale,
                             AuxScale autoscale, int sizeLimit ) {
            shape_ = shape;
            scale_ = scale;
            autoscale_ = autoscale;
            sizeLimit_ = sizeLimit;
            icon_ = MarkForm.createLegendIcon( shape, 4 );
            glyphMap_ = new HashMap<Integer,Glyph>();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            if ( autoscale_ != null ) {
                AuxReader sizeReader =
                    new FloatingCoordAuxReader( SIZE_COORD,
                                                getSizeCoordIndex( geom ),
                                                geom, true, (Scaling) null );
                map.put( autoscale_, sizeReader );
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
            final int icSize = getSizeCoordIndex( geom );
            final double scale = scale_ * getBaseScale( surface, auxSpans );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gpos ) ) {
                        double size =
                            SIZE_COORD.readDoubleCoord( tuple, icSize );
                        if ( PlotUtil.isFinite( size ) ) {
                            int isize = (int) Math.round( size * scale );
                            Glyph glyph = getGlyph( isize );
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
            final int icSize = getSizeCoordIndex( geom );
            final double scale = scale_ * getBaseScale( surface, auxSpans );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gpos ) ) {
                        double size =
                            SIZE_COORD.readDoubleCoord( tuple, icSize );
                        if ( PlotUtil.isFinite( size ) ) {
                            int isize = (int) Math.round( size * scale );
                            Glyph glyph = getGlyph( isize );
                            paperType.placeGlyph( paper, gpos.x, gpos.y, gpos.z,
                                                  glyph, color );
                        }
                    }
                }
            };
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SizeOutliner ) {
                SizeOutliner other = (SizeOutliner) o;
                return this.shape_.equals( other.shape_ )
                    && this.scale_ == other.scale_
                    && PlotUtil.equals( this.autoscale_,  other.autoscale_ )
                    && this.sizeLimit_ == other.sizeLimit_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 4451;
            code = 23 * code + shape_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) scale_ );
            code = 23 * code + PlotUtil.hashCode( autoscale_ );
            code = 23 * code + sizeLimit_;
            return code;
        }

        /**
         * Returns a glyph of a given size.
         * A map is kept so that if one of the right size has been
         * created before, it's returned this time rather than generating
         * a new one.  This is important, because the resulting glyphs
         * may end up stored in a big collection (for instance for 3D
         * rendering), and repeated references to a single instance 
         * are much cheaper than references to a new instance each time.
         * 
         * @param  isize  glyph size in pixels
         * @return  new or re-used glyph
         */
        private Glyph getGlyph( int isize ) {
            isize = Math.min( sizeLimit_, Math.max( 0, isize ) );
            Glyph glyph = glyphMap_.get( isize );
            if ( glyph == null ) {
                glyph = MarkForm.createMarkGlyph( shape_, isize, true );
                glyphMap_.put( isize, glyph );
            }
            return glyph;
        }

        /**
         * Returns the basic scale for sizing markers.
         * For autoscale it's determined from the data, otherwise it's 1.  
         * It may be adjusted by the user-supplied scale adjustment.
         *
         * @param  surface  plot surface
         * @param  spanMap  map of ranges calculated as part of
         *                  plot preparation by request
         * @return  basic size scale
         */
        private double getBaseScale( Surface surface,
                                     Map<AuxScale,Span> spanMap ) {
            if ( autoscale_ != null) {
                Span span = spanMap.get( autoscale_ );
                double[] bounds = span.getFiniteBounds( true );
                return 1. / bounds[ 1 ];
            }
            else {
                return 1;
            }
        }
    }
}
