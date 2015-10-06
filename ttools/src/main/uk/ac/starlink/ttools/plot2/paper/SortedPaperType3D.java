package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.ttools.plot.Point3D;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * PaintPaperType for 3-dimensional plots.
 * Suitable for output to vector graphics media.
 *
 * <p>It works by accumulating a list of glyphs to be painted,
 * and when they are all in (all layer drawings have been processed)
 * sorts them by Z-coordinate and paints them in order.
 * I think that's the only way you can do it for vector graphics.
 * It will unavoidably have a large memory footprint and be slow
 * for large numbers of points.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class SortedPaperType3D extends PaintPaperType
                               implements PaperType3D {

    /**
     * Constructor.
     */
    public SortedPaperType3D() {
        super( "Sorted", true );
    }

    protected Paper createPaper( Graphics g, Rectangle bounds ) {
        return new SortedPaper3D( this, g );
    }

    public void placeGlyph( Paper paper, double dx, double dy, double dz,
                            Glyph glyph, Color color ) {
        assert dx >= Short.MIN_VALUE && dx <= Short.MAX_VALUE
            && dy >= Short.MIN_VALUE && dy <= Short.MAX_VALUE : "uh-oh";
        short gx = (short) dx;
        short gy = (short) dy;
        ((SortedPaper3D) paper).placeGlyph( gx, gy, dz, glyph, color );
    }

    public void placeDecal( Paper paper, Decal decal ) {
        ((SortedPaper3D) paper).placeDecal( decal );
    }

    protected void flushPaper( Paper paper ) {
        ((SortedPaper3D) paper).flush();
    }

    /**
     * Paper implementation for this paper type.
     */
    private static class SortedPaper3D implements Paper {
        final PaperType paperType_;
        final Graphics graphics_;
        List<PlacedGlyph> list_;
        int iseq_;

        /**
         * Constructor.
         *
         * @param   paperType  paper type instance which created this paper
         * @param   graphics  graphics destination
         */
        SortedPaper3D( PaperType paperType, Graphics graphics ) {
            paperType_ = paperType;
            graphics_ = graphics;
            list_ = new ArrayList<PlacedGlyph>();
        }

        public PaperType getPaperType() {
            return paperType_;
        }

        void placeGlyph( short gx, short gy, double dz,
                         Glyph glyph, Color color ) {
            list_.add( new PlacedGlyph( glyph, gx, gy, dz, color, iseq_++ ) );
        }

        void placeDecal( Decal decal ) {
            decal.paintDecal( graphics_ );
        }

        void flush() {
            List<PlacedGlyph> points = getSortedPoints();
            Color color0 = graphics_.getColor();
            for ( PlacedGlyph pg : points ) {
                int px = pg.gx_;
                int py = pg.gy_;
                graphics_.setColor( pg.color_ );
                graphics_.translate( px, py );
                pg.glyph_.paintGlyph( graphics_ );
                graphics_.translate( -px, -py );
            }
            graphics_.setColor( color0 );
        }

        /**
         * Returns the list of glyphs to draw in the order they have to get
         * drawn in.
         *
         * @return   list of placed glyphs ready to paint in sequence
         */
        List<PlacedGlyph> getSortedPoints() {

            /* If they are all the same colour, there's no point in sorting
             * them because the rendering will end up the same,
             * so don't bother. */
            if ( ! singleColor( list_ ) ) {
                Collections.sort( list_, Point3D.getComparator( false, true ) );
            }
            return list_;
        }

        /**
         * Checks a list of placed glyphs to see if they are all the same
         * colour.
         *
         * @param  list  glyph list
         * @return  true if all are known to have the same colour
         */
        private static boolean singleColor( List<PlacedGlyph> list ) {

             /* Could maybe extend this to check whether they all
              * have the same RGB (regardless of alpha), which would probably
              * be good enough. */
             if ( list.size() > 0 ) {
                 Iterator<PlacedGlyph> it = list.iterator();
                 Color color = it.next().color_;
                 while ( it.hasNext() ) {
                     if ( ! PlotUtil.equals( color, it.next().color_ ) ) {
                         return false;
                     }
                 }
             }
             return true;
        }
    }

    /**
     * Aggregates a glyph, its graphical position, its Z-buffer position,
     * and its colour.
     * This is the information that needs to be known about it in order
     * to plot it when the time comes.
     * The glyph and the colour members should in most cases be shared
     * between many instances of this, reducing memory usage.
     */
    private static class PlacedGlyph extends Point3D {
        final short gx_;
        final short gy_;
        final Glyph glyph_;
        final Color color_;

        /**
         * Constructor.
         *
         * @param  glyph  glyph
         * @param  gx   X position in pixels
         * @param  gy   Y position in pixels
         * @param  dz   Z-buffer position
         * @param  color  colour
         * @param  iseq   sequence number,
         *                used as a tie-breaker for plotting order
         */
        PlacedGlyph( Glyph glyph, short gx, short gy, double dz, Color color,
                     int iseq ) {
            super( iseq, dz );
            gx_ = gx;
            gy_ = gy;
            glyph_ = glyph;
            color_ = color;
        }
    }
}
