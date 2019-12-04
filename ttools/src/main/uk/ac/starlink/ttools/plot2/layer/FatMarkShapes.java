package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.PointArrayPixellator;

/**
 * MarkShape implementations based on line drawings (open shapes,
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
public class FatMarkShapes {

    /** Standard fatness measure; 0 corresponds to single-pixel lines. */
    public static final int IFAT = 1;

    /** Open circle with thick line. */
    public static final MarkShape FAT_CIRCLE =
        new FatOpenShape( "fat circle", IFAT, false,
                          MarkShape.OPEN_CIRCLE, MarkShape.FILLED_CIRCLE );

    /** Open square with thick line. */
    public static final MarkShape FAT_SQUARE =
        new FatOpenShape( "fat square", IFAT, false,
                          MarkShape.OPEN_SQUARE, MarkShape.FILLED_SQUARE );

    /** Open diamond with thick line. */
    public static final MarkShape FAT_DIAMOND =
        new FatOpenShape( "fat diamond", IFAT, true,
                          MarkShape.OPEN_DIAMOND, MarkShape.FILLED_DIAMOND );

    /** Open upward triangle with thick line. */
    public static final MarkShape FAT_TRIANGLE_UP =
        new FatOpenShape( "fat triangle up", IFAT, true,
                          MarkShape.OPEN_TRIANGLE_UP,
                          MarkShape.FILLED_TRIANGLE_UP );

    /** Open downward triangle with thick line. */
    public static final MarkShape FAT_TRIANGLE_DOWN =
        new FatOpenShape( "fat triangle up", IFAT, true,
                          MarkShape.OPEN_TRIANGLE_DOWN,
                          MarkShape.FILLED_TRIANGLE_DOWN );

    /** Plus-shaped marker with thick lines. */
    public static final MarkShape FAT_CROSS =
        new FatCross( "fat cross", IFAT );

    /** X-shaped marker with thick lines. */
    public static final MarkShape FAT_CROXX =
        new FatCroxx( "fat x", IFAT );

    /**
     * MarkShape implementation class that draws a thick-lined shape by
     * using the corresponding filled and open thin-lined shape.
     */
    private static class FatOpenShape extends MarkShape {

        private final int ifat_;
        private final boolean isBoost_;
        private final MarkShape openShape_;
        private final MarkShape fillShape_;
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
                      MarkShape openShape, MarkShape fillShape ) {
            super( name );
            ifat_ = ifat;
            isBoost_ = isBoost;
            openShape_ = openShape;
            fillShape_ = fillShape;
            stroke_ = new BasicStroke( ifat * 2 );
        }

        public MarkStyle getStyle( Color color, final int size ) {
            final MarkStyle openStyle =
                openShape_.getStyle( color, size - ifat_ + (isBoost_ ? 1 : 0) );
            List<Object> otherAtts = Arrays.asList(
                new Integer( size ),
                new Integer( ifat_ ),
                openShape_
            );
            return new MarkStyle( color, otherAtts, this, size, size + 1 ) {
                private Pixellator pixoffs_;
                public void drawShape( Graphics g ) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( stroke_ );
                    openStyle.drawShape( g2 );
                    g2.setStroke( stroke0 );
                }
                public Pixellator getPixelOffsets() {
                    if ( pixoffs_ == null ) {
                        pixoffs_ = createPixellator( size );
                    }
                    return pixoffs_;
                }
            };
        }

        /**
         * Returns a pixellator for this fat shape at a given marker size.
         * Neither this method nor its returned pixellator is used
         * every point in the plot2 framework,
         * so it doesn't need to be optimally implemented.
         *
         * @param  marker size
         */
        private Pixellator createPixellator( int size ) {
            Set<Point> points = getFillPoints( size + ( isBoost_ ? 1 : 0 ) );
            int innerSize = size - ( ifat_ * 2 + 0 );
            if ( innerSize > 0 ) {
                points.removeAll( getFillPoints( innerSize ) );
            }
            return new PointArrayPixellator( points.toArray( new Point[ 0 ] ) );
        }

        /**
         * Returns a set of all the points in the filled shape at a given size.
         *
         * @param  filled shape size
         * @param  set of all pixels in filled shape
         */
        private Set<Point> getFillPoints( int size ) {
            Pixellator pixer =
                fillShape_.getStyle( Color.BLACK, size ).getPixelOffsets();
            Set<Point> set = new HashSet<Point>();
            for( pixer.start(); pixer.next(); ) {
                set.add( new Point( pixer.getX(), pixer.getY() ) );
            }
            return set;
        }
    }

    /**
     * Fat-lined plus shape.
     */
    private static class FatCross extends MarkShape {

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

        public MarkStyle getStyle( Color color, int size ) {
            Object otherAtts = new Integer( ifat_ << 16 | size );
            final int s1 = 2 * size + 1;
            final int s2 = 2 * ifat_ + 1;
            return new MarkStyle( color, otherAtts, this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.fillRect( -size, -ifat_, s1, s2 );
                    g.fillRect( -ifat_, -size, s2, s1 );
                }
            };
        }
    }

    /**
     * Fat-lined cross X shape.
     */
    private static class FatCroxx extends MarkShape {

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

        public MarkStyle getStyle( Color color, int size ) {
            Object otherAtts = new Integer( ifat_ << 16 | size );
            final BasicStroke stroke =
                new BasicStroke( ifat_ * 2, BasicStroke.CAP_ROUND,
                                 BasicStroke.JOIN_ROUND );
            return new MarkStyle( color, otherAtts, this, size, size + 1 ) {
                private Pixellator pixoffs_;
                public void drawShape( Graphics g ) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( stroke );
                    g2.drawLine( -size, -size, size, size );
                    g2.drawLine( size, -size, -size, size );
                    g2.setStroke( stroke0 );
                }
                public Pixellator getPixelOffsets() {
                    if ( pixoffs_ == null ) {
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
                        Point[] pts = points.toArray( new Point[ 0 ] );
                        pixoffs_ = new PointArrayPixellator( pts );
                    }
                    return pixoffs_;
                };
            };
        }
    }
}
