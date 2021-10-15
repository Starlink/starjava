package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
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
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Draws a line between two related positions.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    28 Nov 2013
 */
public class PairLinkForm implements ShapeForm {

    private static final PairLinkForm instance_ = new PairLinkForm();

    /** Line thickness config key. */
    public static final ConfigKey<Integer> THICK_KEY = createThicknessKey();

    /**
     * Private constructor prevents instantiation.
     */
    private PairLinkForm() {
    }

    public String getFormName() {
        return "Link2";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_LINK2;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a line linking two positions from the same",
            "input table row.",
            "</p>",
        } );
    }

    public int getPositionCount() {
        return 2;
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            THICK_KEY,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        int nthick = config.get( THICK_KEY ).intValue();
        return new LinkOutliner( nthick );
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return sole instance
     */
    public static PairLinkForm getInstance() {
        return instance_;
    }

    /**
     * Determines whether any part of a line between two points is contained
     * within a given rectangle.
     *
     * @param   box  boundary box
     * @param   p1   one end of line
     * @param   p2   other end of line
     * @return  false guarantees that no part of the line appears in the box;
     *          true means it might do
     */
    private static boolean lineMightCross( Rectangle box, Point p1, Point p2 ) {
        int xmin = box.x;
        int xmax = box.x + box.width;
        if ( getRegion( p1.x, xmin, xmax ) *
             getRegion( p2.x, xmin, xmax ) == 1 ) {
            return false;
        }
        int ymin = box.y;
        int ymax = box.y + box.height;
        if ( getRegion( p1.y, ymin, ymax ) *
             getRegion( p2.y, ymin, ymax ) == 1 ) {
            return false;
        }
        return true;
    }

    /**
     * Returns the region of a point with respect to an interval.
     * The return value is -1, 0, or 1 according to whether the point
     * is lower than, within, or higher than the interval bounds.
     *
     * @param   point   test value
     * @param   lo    region lower bound
     * @param   hi    region upper bound
     * @return  region code
     */
    private static int getRegion( int point, int lo, int hi ) {
        return point >= lo ? ( point <= hi ? 0
                                           : +1 )
                           : -1;
    }

    /**
     * Constructs a config key for line thickness.
     *
     * @return  thickness key
     */
    private static ConfigKey<Integer> createThicknessKey() {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setShortDescription( "Line thickness" );
        meta.setXmlDescription( new String[] {
            "<p>Controls the line thickness used when drawing",
            "point-to-point links.",
            "Zero, the default value, means a 1-pixel-wide line is used,",
            "and larger values make drawn lines thicker.",
            "</p>",
        } );
        return StyleKeys.createPaintThicknessKey( meta, 3 );
    }

    /**
     * Outliner implementation for this form.
     */
    private static class LinkOutliner extends PixOutliner {

        private final int nthick_;
        private final XYShape xyLine_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  nthick  line thickness index &gt;=0
         */
        LinkOutliner( int nthick ) {
            nthick_ = nthick;
            xyLine_ = FatLineXYShape.getInstance( nthick );
            icon_ = createLegendIcon();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<AuxScale,AuxReader>();
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            int ndim = surface.getDataDimCount();
            final double[] dpos1 = new double[ ndim ];
            final double[] dpos2 = new double[ ndim ];
            final Point2D.Double gp1 = new Point2D.Double();
            final Point2D.Double gp2 = new Point2D.Double();
            final Point gp1i = new Point();
            final Point gp2i = new Point();
            final int npc = geom.getPosCoords().length;
            final Rectangle bounds = surface.getPlotBounds();
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {

                    /* Paint the line if any part of it falls within the
                     * plot bounds. */
                    if ( geom.readDataPos( tuple, 0, dpos1 ) &&
                         surface.dataToGraphics( dpos1, false, gp1 ) &&
                         PlotUtil.isPointFinite( gp1 ) &&
                         geom.readDataPos( tuple, npc, dpos2 ) &&
                         surface.dataToGraphics( dpos2, false, gp2 ) &&
                         PlotUtil.isPointFinite( gp2 ) &&
                         surface.isContinuousLine( dpos1, dpos2 ) ) {
                        PlotUtil.quantisePoint( gp1, gp1i );
                        PlotUtil.quantisePoint( gp2, gp2i );
                        if ( lineMightCross( bounds, gp1i, gp2i ) ) {
                            Glyph glyph = getLineGlyph( gp2i.x - gp1i.x,
                                                        gp2i.y - gp1i.y );
                            paperType.placeGlyph( paper, gp1i.x, gp1i.y,
                                                  glyph, color );
                        }
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            int ndim = surface.getDataDimCount();
            final double[] dpos1 = new double[ ndim ];
            final double[] dpos2 = new double[ ndim ];
            final GPoint3D gp1 = new GPoint3D();
            final GPoint3D gp2 = new GPoint3D();
            final Point gp1i = new Point();
            final Point gp2i = new Point();
            final int npc = geom.getPosCoords().length;
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {

                    /* Paint the line if either end falls within the plot
                     * region.  It would require some additional arithmetic
                     * to cover the case where neither end is in the region
                     * but part of the line would be.  Note this can lead
                     * to part of the line sticking out of the cube.
                     * It's not really the right thing to do, but it's
                     * not too bad.  Additional work would be required
                     * as well to truncate it at the cube face. */
                    if ( geom.readDataPos( tuple, 0, dpos1 ) &&
                         geom.readDataPos( tuple, npc, dpos2 ) &&
                         ( surface.inRange( dpos1 ) ||
                           surface.inRange( dpos2 ) ) &&
                         surface.dataToGraphicZ( dpos1, false, gp1 ) &&
                         surface.dataToGraphicZ( dpos2, false, gp2 ) ) {
                        double z = 0.5 * ( gp1.z + gp2.z );
                        PlotUtil.quantisePoint( gp1, gp1i );
                        PlotUtil.quantisePoint( gp2, gp2i );
                        Glyph glyph = getLineGlyph( gp2i.x - gp1i.x,
                                                    gp2i.y - gp1i.y );
                        paperType.placeGlyph( paper, gp1i.x, gp1i.y, z, glyph,
                                              color );
                    }
                }
            };
        }

        @Override
        public int hashCode() {
            int code = -1045;
            code = 23 * code + nthick_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LinkOutliner ) {
                LinkOutliner other = (LinkOutliner) o;
                return this.nthick_ == other.nthick_;
            }
            else {
                return false;
            }
        }

        /**
         * Returns a glyph to draw a line between the origin and a given point.
         *
         * @param   gx  destination X graphics coordinate
         * @param   gy  destination Y graphics coordinate
         * @return  glyph
         */
        private Glyph getLineGlyph( int gx, int gy ) {
            return xyLine_.getGlyph( (short) gx, (short) gy );
        }

        /**
         * Returns an uncoloured icon suitable for use in a legend.
         *
         * @return  legend icon
         */
        private Icon createLegendIcon() {
            return new MultiPosIcon( 2 ) {
                protected void paintPositions( Graphics g, Point[] positions ) {
                    Point p0 = positions[ 0 ];
                    Point p1 = positions[ 1 ];
                    int xoff = p0.x;
                    int yoff = p0.y;
                    g.translate( p0.x, p0.y );
                    getLineGlyph( p1.x - p0.x, p1.y - p0.y ).paintGlyph( g );
                    g.translate( -p0.x, -p0.y );
                }
            };
        }
    }
}
