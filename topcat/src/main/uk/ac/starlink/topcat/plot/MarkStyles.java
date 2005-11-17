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
                return MarkStyle.pointStyle( Styles.getColor( index ) );
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
                return MarkStyle
                      .filledCircleStyle( Styles.getColor( index ), size );
            }
        };
    }

    /**
     * Returns a style set which gives filled semi-transparent circles of
     * a given size in a variety of colours.
     *
     * @param  name  set name
     * @param  size  approximate radius of markers
     * @param  alpha  transparency of spots (0 is invisible, 1 is opaque)
     * @return style set providing ghostly spots
     */
    public static StyleSet ghosts( final String name, final int size,
                                   float alpha ) {
        final int iAlpha = (int) ( alpha * 255.99 );
        final int lAlpha = Math.max( iAlpha, 96 );
        final int lsize = Math.max( size, 1 );
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color baseColor = Styles.getColor( index );
                Color color = new Color( baseColor.getRed(),
                                         baseColor.getGreen(),
                                         baseColor.getBlue(),
                                         iAlpha );
                Color lcolor = new Color( baseColor.getRed(),
                                          baseColor.getGreen(),
                                          baseColor.getBlue(),
                                          lAlpha );
                return MarkStyle.compositeMarkStyle(
                    MarkStyle.filledSquareStyle( color, size ),
                    MarkStyle.filledSquareStyle( lcolor, lsize ) );
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
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color col = color == null ? Styles.getColor( index ) : color;
                switch ( Math.abs( index ) % 7 ) {
                    case 0: return MarkStyle.crossStyle( col, size );
                    case 1: return MarkStyle.xStyle( col, size );
                    case 2: return MarkStyle.openCircleStyle( col, size );
                    case 3: return MarkStyle.openSquareStyle( col, size );
                    case 4: return MarkStyle.openDiamondStyle( col, size );
                    case 5: return MarkStyle.openTriangleStyle( col, size,
                                                                true );
                    case 6: return MarkStyle.openTriangleStyle( col, size,
                                                                false );
                    default: throw new AssertionError();
                }
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
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color col = color == null ? Styles.getColor( index ) : color;
                switch ( Math.abs( index ) % 5 ) {
                    case 0: return MarkStyle.filledCircleStyle( col, size );
                    case 1: return MarkStyle.filledSquareStyle( col, size );
                    case 2: return MarkStyle.filledDiamondStyle( col, size );
                    case 3: return MarkStyle.filledTriangleStyle( col, size,
                                                                  true );
                    case 4: return MarkStyle.filledTriangleStyle( col, size,
                                                                  false );
                    default: throw new AssertionError();
                }
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
