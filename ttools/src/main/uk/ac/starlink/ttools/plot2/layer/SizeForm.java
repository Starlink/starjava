package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
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
                "</p>",
            } )
        , false );

    private static ConfigKey<Double> SCALE_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "maxsize", "Max Marker Size" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Maximum marker size in pixels" )
           .setXmlDescription( new String[] {
                "<p>Sets the maximum marker size in pixels.",
                "This scales the sizes of all the plotted markers.",
                "</p>",
            } )
        , 16, 2, 64, false );
    private static final AuxScale SIZE_SCALE = new AuxScale( "globalsize" );

    private static final SizeForm instance_ = new SizeForm();

    /**
     * Private constructor prevents instantiation.
     */
    private SizeForm() {
    }

    public int getPositionCount() {
        return 1;
    }

    public String getFormName() {
        return "Size";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_SIZE;
    }

    public Coord[] getExtraCoords() {
        return new Coord[] {
            SIZE_COORD,
        };
    }

    public ConfigKey[] getConfigKeys() {
        return new ConfigKey[] {
            StyleKeys.MARK_SHAPE,
            SCALE_KEY,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        MarkShape shape = config.get( StyleKeys.MARK_SHAPE );
        double scale = config.get( SCALE_KEY );
        final AuxScale autoscale;
        boolean isGlobal = true;
        boolean isAutoscale = true;
        if ( isAutoscale ) {
            autoscale = isGlobal ? SIZE_SCALE : new AuxScale( "size1" );
        }
        else {
            autoscale = null;
        }
        return new SizeOutliner( shape, scale, autoscale );
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
     * Returns the column index in a tuple sequenc at which the size
     * coordinate will be found.
     *
     * @param  geom  position geometry
     * @return  size column index
     */
    private static int getSizeCoordIndex( DataGeom geom ) {
        return geom.getPosCoords().length;
    }

    /**
     * Indicates whether a value is a usable number.
     *
     * @param  value  value to test
     * @return  true iff <code>value</code> is non-NaN and non-infinite
     */
    private static boolean isFinite( double value ) {
        return ! Double.isNaN( value ) && ! Double.isInfinite( value );
    }

    /**
     * Outliner implementation for use with SizeForm.
     */
    public static class SizeOutliner extends PixOutliner {
        private final MarkShape shape_;
        private final AuxScale autoscale_;
        private final double scale_;
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
         */
        public SizeOutliner( MarkShape shape, double scale,
                             AuxScale autoscale ) {
            shape_ = shape;
            scale_ = scale;
            autoscale_ = autoscale;
            icon_ = MarkForm.createLegendIcon( shape, 4 );
            glyphMap_ = new HashMap<Integer,Glyph>();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            if ( autoscale_ != null ) {
                map.put( autoscale_, new SizeAuxReader( geom ) );
            }
            return map;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gpos = new Point();
            final int icSize = getSizeCoordIndex( geom );
            final double scale = scale_ * getBaseScale( surface, auxRanges );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gpos ) ) {
                        double size =
                            SIZE_COORD.readDoubleCoord( tseq, icSize );
                        if ( isFinite( size ) ) {
                            int isize = (int) Math.round( size * scale );
                            Glyph glyph = getGlyph( isize );
                            paperType.placeGlyph( paper, gpos.x, gpos.y,
                                                  getGlyph( isize ), color );
                        }
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gpos = new Point();
            final double[] zloc = new double[ 1 ];
            final int icSize = getSizeCoordIndex( geom );
            final double scale = scale_ * getBaseScale( surface, auxRanges );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gpos, zloc ) ) {
                        double size =
                            SIZE_COORD.readDoubleCoord( tseq, icSize );
                        if ( isFinite( size ) ) {
                            int isize = (int) Math.round( size * scale );
                            double dz = zloc[ 0 ];
                            Glyph glyph = getGlyph( isize );
                            paperType.placeGlyph( paper, gpos.x, gpos.y, dz,
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
                    && PlotUtil.equals( this.autoscale_,  other.autoscale_ );
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
            isize = Math.max( 0, isize );
            Glyph glyph = glyphMap_.get( isize );
            if ( glyph == null ) {
                glyph = MarkForm.createMarkGlyph( shape_, isize );
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
         * @param  rangeMap  map of ranges calculated as part of
         *                   plot preparation by request
         * @return  basic size scale
         */
        private double getBaseScale( Surface surface,
                                     Map<AuxScale,Range> rangeMap ) {
            if ( autoscale_ != null) {
                Range range = rangeMap.get( autoscale_ );
                double[] bounds = range.getFiniteBounds( true );
                return 1. / bounds[ 1 ];
            }
            else {
                return 1;
            }
        }
    }

    /**
     * Reads coordinate data to determine the range of size coordinate
     * values encountered.
     * This is used during plot preparation if autoscaling is in effect
     * so that the range of sizes in the data can be determined, and
     * a suitable scaling factor can be applied. 
     */
    private static class SizeAuxReader implements AuxReader {

        final DataGeom geom_;
        final int icSize_;
        final double[] dpos_;
        final Point gpos_;

        /**
         * Constructor.
         *
         * @param  geom  position coordinate geometry
         */
        SizeAuxReader( DataGeom geom ) {
            geom_ = geom;
            icSize_ = getSizeCoordIndex( geom );
            dpos_ = new double[ geom.getDataDimCount() ];
            gpos_ = new Point();
        }

        public void updateAuxRange( Surface surface, TupleSequence tseq,
                                    Range range ) {
            if ( geom_.readDataPos( tseq, 0, dpos_ ) &&
                 surface.dataToGraphics( dpos_, true, gpos_ ) ) {
                double size = SIZE_COORD.readDoubleCoord( tseq, icSize_ );
                if ( isFinite( size ) ) {
                    range.submit( size );
                }
            }
        }
    }
}
