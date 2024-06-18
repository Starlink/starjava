package uk.ac.starlink.ttools.plot;

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
        return points( name, Styles.COLORS );
    }

    /**
     * Returns a style set which gives pixels using a given sequence of colours.
     *
     * @param   name  set name
     * @param   colors  colour sequence
     * @return  style set providing coloured pixels
     */
    public static StyleSet points( final String name, final Color[] colors ) {
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color color = colors[ Math.abs( index ) % colors.length ];
                return MarkShape.POINT.getStyle( color, 1 );
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
     * @param  color color of markers, or <code>null</code> for various
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
     * @param  color color of markers, or <code>null</code> for various
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
     * Returns a StyleSet based on another one but with a given opaque limit
     * for each of the dispensed styles.
     *
     * @param   name   name of the returned style set
     * @param   base   style set on which the returns will be based
     * @param   opaqueLimit   initial opaque limit of styles dispensed by
     *          the return
     * @return  new style set
     */
    public static StyleSet faded( final String name, final StyleSet base,
                                  final int opaqueLimit ) {
         if ( opaqueLimit < 1 ) {
             throw new IllegalArgumentException();
         }
         return new StyleSet() {
             public String getName() {
                 return name;
             }
             public Style getStyle( int index ) {
                 MarkStyle style = (MarkStyle) base.getStyle( index );
                 style.setOpaqueLimit( opaqueLimit );
                 return style;
             }
         };
    }

    /**
     * Returns an icon which represents a given marker style set. 
     * It consists of a row of example legends corresponding to the set.
     *
     * @param   styleSet   style set
     * @return  icon for <code>styles</code>
     */
    public static Icon getIcon( final StyleSet styleSet ) {
        final int nmark = 5;
        final int separation = 5;
        int w = separation * ( nmark - 1 ); 
        int h = 0;
        for ( int i = 0; i < nmark; i++ ) {
            Icon icon = styleSet.getStyle( i ).getLegendIcon();
            w += icon.getIconWidth();
            h = Math.max( h, icon.getIconHeight() );
        }
        final int width = w;
        final int height = h;
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return width;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                for ( int i = 0; i < nmark; i++ ) {
                    Icon icon = styleSet.getStyle( i ).getLegendIcon();
                    icon.paintIcon( c, g, x, y );
                    x += separation + icon.getIconWidth();
                }
            }
        };
    }

    /**
     * Returns a style set which draws lines using a default 
     * sequence of colours.
     *
     * @param   name  set name
     * @return  style set providing coloured lines
     */
    public static StyleSet lines( final String name ) {
        return lines( name, Styles.COLORS );
    }

    /**
     * Returns a style set which draws lines using a given sequence of colours.
     *
     * @param   name  set name
     * @param   colors  colour sequence
     * @return  style set providing coloured lines
     */
    public static StyleSet lines( final String name, final Color[] colors ) {
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                Color color = colors[ Math.abs( index ) % colors.length ];
                MarkStyle style = MarkShape.POINT.getStyle( color, 1 );
                style.setLine( MarkStyle.DOT_TO_DOT );
                style.setHidePoints( true );
                return style;
            }
        };
    }

    /** 
     * Returns a style set which draws black lines with variable dash patterns.
     *
     * @param  name  set name
     */
    public static StyleSet dashedLines( final String name ) {
        final float[][] dashes = Styles.DASHES;
        return new StyleSet() {
            public String getName() {
                return name;
            }
            public Style getStyle( int index ) {
                MarkStyle style = MarkShape.POINT.getStyle( Color.BLACK, 1 );
                style.setLine( MarkStyle.DOT_TO_DOT );
                style.setHidePoints( true );
                style.setDash( dashes[ Math.abs( index ) % dashes.length ] );
                return style;
            }
        };
    }
}
