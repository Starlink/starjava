package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * MarkerShape implementations based on line drawings (open shapes,
 * crosses etc; not filled shapes) which have lines that are
 * thicker than the single-pixel traditionally provided.
 *
 * <p>A fatness parameter corresponds somewhat to the line thickness;
 * it goes up in ones, and the idea is that the larger the number
 * the fatter the lines, but for a given fatness the lines in
 * all the shapes are about the same.  Fatness zero corresponds nominally
 * to the traditional single-pixel lines.  The static shapes available
 * here currently correspond to fatness 1.
 *
 * <p>Some fiddling is required to make it look OK in a bitmapped context,
 * and in particular to make sure that bitmapped representations
 * are centered on the given point rather than being half a pixel off.
 *
 * @author   Mark Taylor
 * @since    4 Dec 2019
 */
public class FatMarkerShapes {

    /** Standard fatness measure; 0 corresponds to single-pixel lines. */
    public static final int IFAT = 1;

    /** Open circle with thick line. */
    public static final MarkerShape FAT_CIRCLE =
        new FatOpenShape( "fat circle", IFAT, false,
                          MarkerShape.OPEN_CIRCLE, MarkerShape.FILLED_CIRCLE );

    /** Open square with thick line. */
    public static final MarkerShape FAT_SQUARE =
        new FatOpenShape( "fat square", IFAT, false,
                          MarkerShape.OPEN_SQUARE, MarkerShape.FILLED_SQUARE );

    /** Open diamond with thick line. */
    public static final MarkerShape FAT_DIAMOND =
        new FatOpenShape( "fat diamond", IFAT, true,
                          MarkerShape.OPEN_DIAMOND,
                          MarkerShape.FILLED_DIAMOND );

    /** Open upward triangle with thick line. */
    public static final MarkerShape FAT_TRIANGLE_UP =
        new FatOpenShape( "fat triangle up", IFAT, true,
                          MarkerShape.OPEN_TRIANGLE_UP,
                          MarkerShape.FILLED_TRIANGLE_UP );

    /** Open downward triangle with thick line. */
    public static final MarkerShape FAT_TRIANGLE_DOWN =
        new FatOpenShape( "fat triangle down", IFAT, true,
                          MarkerShape.OPEN_TRIANGLE_DOWN,
                          MarkerShape.FILLED_TRIANGLE_DOWN );

    /** Plus-shaped marker with thick lines. */
    public static final MarkerShape FAT_CROSS =
        new FatCross( "fat cross", IFAT );

    /** X-shaped marker with thick lines. */
    public static final MarkerShape FAT_CROXX =
        new FatCroxx( "fat x", IFAT );

    /**
     * MarkerShape implementation class that draws a thick-lined shape by
     * using the corresponding filled and open thin-lined shape.
     */
    private static class FatOpenShape extends MarkerShape {

        private final int ifat_;
        private final boolean isBoost_;
        private final MarkerShape openShape_;
        private final MarkerShape fillShape_;
        private final BasicStroke stroke_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  ifat   fatness measure
         * @param  openShape  single-pixel open shape
         * @param  fillShape  filled shape
         */
        FatOpenShape( String name, int ifat, boolean isBoost,
                      MarkerShape openShape, MarkerShape fillShape ) {
            super( name );
            ifat_ = ifat;
            isBoost_ = isBoost;
            openShape_ = openShape;
            fillShape_ = fillShape;
            stroke_ = new BasicStroke( ifat * 2 );
        }

        public MarkerStyle getStyle( Color color, final int size ) {
            final MarkerStyle openStyle =
                openShape_.getStyle( color, size - ifat_ + (isBoost_ ? 1 : 0) );
            Consumer<Graphics> drawShape = g -> {
                Graphics2D g2 = (Graphics2D) g;
                Stroke stroke0 = g2.getStroke();
                g2.setStroke( stroke_ );
                openStyle.drawShape( g2 );
                g2.setStroke( stroke0 );
            };
            Pixer pixer = createPixer( size );
            return new MarkerStyle( this, color, size, drawShape, pixer );
        }

        /**
         * Returns a pixer for this fat shape at a given marker size.
         * Neither this method nor its returned pixer is used directly
         * when plotting bulk points, so it doesn't need to be
         * optimally implemented.
         *
         * @param  marker size
         */
        private Pixer createPixer( int size ) {
            Set<Point> points = getFillPoints( size + ( isBoost_ ? 1 : 0 ) );
            int innerSize = size - ( ifat_ * 2 + 0 );
            if ( innerSize > 0 ) {
                points.removeAll( getFillPoints( innerSize ) );
            }
            return Pixers.createPointsPixer( points.toArray( new Point[ 0 ] ) );
        }

        /**
         * Returns a set of all the points in the filled shape at a given size.
         *
         * @param  filled shape size
         * @param  set of all pixels in filled shape
         */
        private Set<Point> getFillPoints( int size ) {
            Set<Point> set = new HashSet<Point>();
            Pixer pixer = fillShape_.getStyle( Color.BLACK, size )
                                    .getPixerFactory().createPixer();
            while ( pixer.next() ) {
                set.add( new Point( pixer.getX(), pixer.getY() ) );
            }
            return set;
        }
    }

    /**
     * Fat-lined plus shape.
     */
    private static class FatCross extends MarkerShape {

        private final int ifat_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  ifat   fatness measure
         */
        public FatCross( String name, int ifat ) {
            super( name );
            ifat_ = ifat;
        }

        public MarkerStyle getStyle( Color color, int size ) {
            Object otherAtts = new Integer( ifat_ << 16 | size );
            final int s1 = 2 * size + 1;
            final int s2 = 2 * ifat_ + 1;
            Consumer<Graphics> drawShape = g -> {
                g.fillRect( -size, -ifat_, s1, s2 );
                g.fillRect( -ifat_, -size, s2, s1 );
            };
            return new MarkerStyle( this, color, size, size + 1, drawShape );
        }
    }

    /**
     * Fat-lined cross X shape.
     */
    private static class FatCroxx extends MarkerShape {

        private final int ifat_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  ifat   fatness measure
         */
        public FatCroxx( String name, int ifat ) {
            super( name );
            ifat_ = ifat;
        }

        public MarkerStyle getStyle( Color color, final int size ) {
            final BasicStroke stroke =
                new BasicStroke( ifat_ * 2, BasicStroke.CAP_ROUND,
                                 BasicStroke.JOIN_ROUND );
            Consumer<Graphics> drawShape = g -> {
                Graphics2D g2 = (Graphics2D) g;
                Stroke stroke0 = g2.getStroke();
                g2.setStroke( stroke );
                g2.drawLine( -size, -size, size, size );
                g2.drawLine( size, -size, -size, size );
                g2.setStroke( stroke0 );
            };
            Set<Point> points = new HashSet<Point>();
            for ( int i = 1 - size; i < size; i++ ) {
                for ( int j = -ifat_; j <= ifat_; j++ ) {
                    points.add( new Point( i + j, +i ) );
                    points.add( new Point( i + j, -i ) );
                }
            }

            /* Touch up round the corners.
             * This might need some adjustment for ifat>1. */
            int s1 = size - 1;
            points.add( new Point( -s1, -size ) );
            points.add( new Point( -s1, +size ) );
            points.add( new Point( +s1, -size ) );
            points.add( new Point( +s1, +size ) );
            points.add( new Point( -size, -size ) );
            points.add( new Point( -size, +size ) );
            points.add( new Point( +size, -size ) );
            points.add( new Point( +size, +size ) );
            Pixer pixer =
                Pixers.createPointsPixer( points.toArray( new Point[ 0 ] ) );
            return new MarkerStyle( this, color, size, drawShape, pixer );
        }
    }
}
