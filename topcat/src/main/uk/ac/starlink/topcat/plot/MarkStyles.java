package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Provides several factory methods for constructing StyleSets
 * which dispense {@link MarkStyle}s.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class MarkStyles {

    /**
     * Returns a style set which gives pixels in a variety of colours.
     *
     * @param   name  set name
     * @return  style set providing coloured pixels
     */
    public static StyleSet points( final String name ) {
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                return MarkShape.POINT.getStyle( Styles.getColor( index ), 1 );
            }
        };
    }

    /**
     * Returns a style set which gives filled circles of a given size
     * in a variety of colours.
     *
     * @param   name  set name
     * @param   size  approximate radius of markers
     * @return  style set providing coloured spots
     */
    public static StyleSet spots( final String name, final int size ) {
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                return MarkShape.FILLED_CIRCLE
                      .getStyle( Styles.getColor( index ), size );
            }
        };
    }

    /**
     * Returns a style set which gives line-drawn shapes of various kinds.
     *
     * @param  name  set name
     * @param  size  approximate radius of markers
     * @param  color color of markers, or <tt>null</tt> for various
     * @return  style set providing open shapes
     */
    public static StyleSet openShapes( final String name, final int size,
                                       final Color color ) {
        final MarkShape[] shapes = new MarkShape[] {
            MarkShape.CROSS,
            MarkShape.CROXX,
            MarkShape.OPEN_CIRCLE,
            MarkShape.OPEN_SQUARE,
            MarkShape.OPEN_DIAMOND,
            MarkShape.OPEN_TRIANGLE_UP,
            MarkShape.OPEN_TRIANGLE_DOWN,
        };
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color col = color == null ? Styles.getColor( index ) : color;
                MarkShape shape = shapes[ Math.abs( index ) % shapes.length ];
                return shape.getStyle( col, size );
            }
        };
    }

    /**
     * Returns a style set which gives filled shapes of various kinds.
     *
     * @param  name  set name
     * @param  size  approximate radius of markers
     * @param  color color of markers, or <tt>null</tt> for various
     * @return  style set providing filled shapes
     */
    public static StyleSet filledShapes( final String name, final int size,
                                         final Color color ) {
        final MarkShape[] shapes = new MarkShape[] {
            MarkShape.FILLED_CIRCLE,
            MarkShape.FILLED_SQUARE,
            MarkShape.FILLED_DIAMOND,
            MarkShape.FILLED_TRIANGLE_UP,
            MarkShape.FILLED_TRIANGLE_DOWN,
        };
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color col = color == null ? Styles.getColor( index ) : color;
                MarkShape shape = shapes[ Math.abs( index ) % shapes.length ];
                return shape.getStyle( col, size );
            }
        };
    }

    /**
     * Returns an icon which represents a given marker style set. 
     * It consists of a row of example legends corresponding to the set.
     *
     * @param   styles   style set
     * @return  icon for <code>styles</code>
     */
    public static Icon getIcon( final StyleSet styleSet ) {
        return new Icon() {
            final int NMARK = 5;
            final int SEPARATION = 16;
            final int HEIGHT = SEPARATION;

            public int getIconHeight() {
                return HEIGHT;
            }

            public int getIconWidth() {
                return NMARK * SEPARATION + SEPARATION / 2;
            }

            public void paintIcon( Component c, Graphics g,
                                   int xoff, int yoff ) {
                int y = yoff + HEIGHT / 2;
                for ( int i = 0; i < NMARK; i++ ) {
                    int x = xoff + i * SEPARATION + SEPARATION / 2;
                    styleSet.getStyle( i ).drawLegend( g, x, y );
                }
            }
        };
    }
}
