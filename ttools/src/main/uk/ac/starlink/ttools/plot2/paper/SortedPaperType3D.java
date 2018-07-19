package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
        return new SortedPaper3D( this, (Graphics2D) g, bounds );
    }

    public void placeGlyph( Paper paper, double dx, double dy, double dz,
                            Glyph glyph, Color color ) {
        ((SortedPaper3D) paper).placeGlyph( dx, dy, dz, glyph, color );
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
        final Graphics2D graphics_;
        final Packer xPacker_;
        final Packer yPacker_;
        List<PlacedGlyph> list_;
        int iseq_;

        /**
         * Constructor.
         *
         * @param   paperType  paper type instance which created this paper
         * @param   graphics  graphics destination
         * @param   bounds   plot bounds
         */
        SortedPaper3D( PaperType paperType, Graphics2D graphics,
                       Rectangle bounds ) {
            paperType_ = paperType;
            graphics_ = graphics;
            xPacker_ = new Packer( bounds.x, bounds.width );
            yPacker_ = new Packer( bounds.y, bounds.height );
            list_ = new ArrayList<PlacedGlyph>();
        }

        public PaperType getPaperType() {
            return paperType_;
        }

        void placeGlyph( double gx, double gy, double dz,
                         Glyph glyph, Color color ) {
            short sx = xPacker_.toShort( gx );
            short sy = yPacker_.toShort( gy );
            list_.add( new PlacedGlyph( glyph, sx, sy, dz, color, iseq_++ ) );
        }

        void placeDecal( Decal decal ) {
            decal.paintDecal( graphics_ );
        }

        void flush() {
            List<PlacedGlyph> points = getSortedPoints();
            Color color0 = graphics_.getColor();
            for ( PlacedGlyph pg : points ) {
                double px = xPacker_.fromShort( pg.sx_ );
                double py = yPacker_.fromShort( pg.sy_ );
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
        final short sx_;
        final short sy_;
        final Glyph glyph_;
        final Color color_;

        /**
         * Constructor.
         *
         * @param  glyph  glyph
         * @param  sx   X position packed into a short
         * @param  sy   Y position packed into a short
         * @param  dz   Z-buffer position
         * @param  color  colour
         * @param  iseq   sequence number,
         *                used as a tie-breaker for plotting order
         */
        PlacedGlyph( Glyph glyph, short sx, short sy, double dz, Color color,
                     int iseq ) {
            super( iseq, dz );
            sx_ = sx;
            sy_ = sy;
            glyph_ = glyph;
            color_ = color;
        }
    }

    /**
     * Maps floating point values in a given range to short values.
     */
    private static class Packer {
        final double s0_;
        final double sFact_;
        final double d0_;
        final double dFact_;

        /**
         * Constructor.
         *
         * @param  dBase  minimum double value
         * @param  dExtent   range of double value
         */
        Packer( double dBase, double dExtent ) {

            /* Don't go right to the edge of the range, in case we need
             * to work with some positions a bit outside of the bounding box,
             * and stay within the positive range of short values since
             * value truncation behaves in a sometimes surprising way
             * either side of zero. */
            double sBase = 0.1 * Short.MAX_VALUE;
            double sExtent = 0.8 * Short.MAX_VALUE;
            s0_ = sBase - ( dBase * sExtent / dExtent );
            d0_ = dBase - ( sBase * dExtent / sExtent );
            sFact_ = sExtent / dExtent;
            dFact_ = dExtent / sExtent;
        }

        /**
         * Maps a double value in the stated range to a short value.
         *
         * @param  d   unpacked
         * @return  packed
         */
        short toShort( double d ) {
            return (short) ( s0_ + sFact_ * d );
        }

        /**
         * Unmaps a short value back to (close to) its double value.
         *
         * @param  s  packed
         * @return  unpacked
         */
        double fromShort( short s ) {
            return d0_ + dFact_ * s;
        }
    }
}
