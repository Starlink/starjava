package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.StorageType;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;
import uk.ac.starlink.util.IconUtils;

/**
 * Plots a pair of related points.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 */
public class PairPlotter extends AbstractPlotter<PairPlotter.PairStyle> {

    /**
     * Constructor.
     */
    public PairPlotter() {
        super( "Pair", ResourceIcon.PLOT_LINK2, 2, new Coord[ 0 ] );
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
        };
    }

    public PairStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        return new PairStyle( color );
    }

    public PlotLayer createLayer( final DataGeom geom,
                                  final DataSpec dataSpec,
                                  final PairStyle style ) {
        LayerOpt opt = new LayerOpt( style.color_, true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                if ( paperType instanceof PaperType2D ) {
                    final PaperType2D ptype = (PaperType2D) paperType;
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  DataStore dataStore ) {
                            paintPairs2D( style, surface, geom, dataSpec,
                                          dataStore, ptype, paper );
                        }
                    };
                }
                else if ( paperType instanceof PaperType3D ) {
                    final PaperType3D ptype = (PaperType3D) paperType;
                    final CubeSurface surf = (CubeSurface) surface;
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  DataStore dataStore ) {
                            paintPairs3D( style, surf, geom, dataSpec,
                                          dataStore, ptype, paper );
                        }
                    };
                }
                else {
                    throw new IllegalArgumentException( "paper type" );
                }
            }
        };
    }

    private void paintPairs2D( PairStyle style, Surface surface,
                               DataGeom geom, DataSpec dataSpec,
                               DataStore dataStore, PaperType2D paperType,
                               Paper paper ) {
        Color color = style.color_;
        int ndim = surface.getDataDimCount();
        double[] dpos1 = new double[ ndim ];
        double[] dpos2 = new double[ ndim ];
        Point gp1 = new Point();
        Point gp2 = new Point();
        int npc = geom.getPosCoords().length;
        Rectangle bounds = surface.getPlotBounds();
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {

            /* Paint the line if any part of it falls in the plot region. */
            if ( geom.readDataPos( tseq, 0, dpos1 ) &&
                 surface.dataToGraphics( dpos1, false, gp1 ) &&
                 geom.readDataPos( tseq, npc, dpos2 ) &&
                 surface.dataToGraphics( dpos2, false, gp2 ) &&
                 lineMightCross( bounds, gp1, gp2 ) ) {
                Glyph glyph =
                    LineGlyph.getLineGlyph( gp2.x - gp1.x, gp2.y - gp1.y );
                paperType.placeGlyph( paper, gp1.x, gp1.y, glyph, color );
            }
        }
    }

    private void paintPairs3D( PairStyle style, CubeSurface surface,
                               DataGeom geom, DataSpec dataSpec,
                               DataStore dataStore, PaperType3D paperType,
                               Paper paper ) {
        Color color = style.color_;
        int ndim = surface.getDataDimCount();
        double[] dpos1 = new double[ ndim ];
        double[] dpos2 = new double[ ndim ];
        Point gp1 = new Point();
        Point gp2 = new Point();
        double[] dz1 = new double[ 1 ];
        double[] dz2 = new double[ 1 ];
        int npc = geom.getPosCoords().length;
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {

            /* Paint the line if either end falls within the plot region.
             * It would require some additional arithmetic to cover the case
             * where neither end is in the region but part of the line
             * would be.  Note this can lead to part of the line sticking
             * out of the cube.  It's not really the right thing to do,
             * but it's not too bad.  Additional work would be required as
             * well to truncate it at the cube face. */
            if ( geom.readDataPos( tseq, 0, dpos1 ) &&
                 geom.readDataPos( tseq, npc, dpos2 ) &&
                 ( surface.inRange( dpos1 ) || surface.inRange( dpos2 ) ) &&
                 surface.dataToGraphicZ( dpos1, false, gp1, dz1 ) &&
                 surface.dataToGraphicZ( dpos2, false, gp2, dz2 ) ) {
                double z = 0.5 * ( dz1[ 0 ] + dz2[ 0 ] );
                Glyph glyph =
                    LineGlyph.getLineGlyph( gp2.x - gp1.x, gp2.y - gp1.y );
                paperType.placeGlyph( paper, gp1.x, gp1.y, z, glyph, color );
            }
        }
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
     * Style class for pair plots.
     */
    public static class PairStyle implements Style {
        private final Color color_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  color  link colour
         */
        public PairStyle( Color color ) {
            color_ = color;
            icon_ = IconUtils.colorIcon( new EdgeIcon( 1 ), color );
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        @Override
        public int hashCode() {
            int code = 54321;
            code = 23 * code + color_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof PairStyle ) {
                PairStyle other = (PairStyle) o;
                return this.color_.equals( other.color_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Legend icon.
     */
    private static class EdgeIcon implements Icon {
        private final int SIZE = 20;
        private final int[] xs_;
        private final int[] ys_;

        /**
         * Constructor.
         *
         * @param   nEdge  edge count
         */
        EdgeIcon( int nEdge ) {
            int size2 = SIZE / 2;
            int nPoint = nEdge + 1;
            xs_ = new int[ nPoint ];
            ys_ = new int[ nPoint ];
            xs_[ 0 ] = size2;
            ys_[ 0 ] = size2;
            for ( int i = 1; i < nPoint; i++ ) {
                double theta = 0.5 * Math.PI * i / nPoint;
                xs_[ i ] = (int) ( size2 + size2 * Math.cos( theta ) );
                ys_[ i ] = (int) ( size2 - size2 * Math.sin( theta ) );
            }
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g.translate( x, y );
            g.drawPolygon( xs_, ys_, xs_.length );
            g.translate( -x, -y );
        }
    }
}
