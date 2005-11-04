package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Utility class relating to the {@link MarkStyleProfile} interface.
 * Provides several factory methods for constructing MarkStyleProfiles
 * amongst other things.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class MarkStyleProfiles {

    private static final Color[] COLORS = new Color[] {
        Color.red,
        Color.blue.brighter(),
        Color.green.darker(),
        Color.gray,
        Color.magenta,
        Color.cyan.darker(),
        Color.orange,
        Color.blue.darker(),
        Color.pink,
        Color.yellow,
        Color.black,
    };

    /**
     * Returns an icon which represents a given profile.  It consists of
     * a row of example markers which it would generate.
     *
     * @return  icon for this profile
     */
    public static Icon getIcon( final MarkStyleProfile profile ) {
        return new Icon() {
            final int NMARK = 5;
            final int SEPARATION = 16;
            final int HEIGHT = SEPARATION;

            public int getIconHeight() {
                return HEIGHT;
            }

            public int getIconWidth() {
                return ( NMARK + 1 ) * SEPARATION;
            }

            public void paintIcon( Component c, Graphics g,
                                   int xoff, int yoff ) {
                int y = yoff + HEIGHT / 2;
                int x = xoff + SEPARATION / 2;
                for ( int i = 0; i < NMARK; i++ ) {
                    profile.getStyle( i ).drawMarker( g, x += SEPARATION, y );
                }
            }
        };
    }

    /**
     * Returns a colour related to a given index.  The same index always
     * maps to the same colour.
     *
     * @param  index  code
     * @return   colour correspoding to <tt>index</tt>
     */
    private static Color getColor( int index ) {
        return COLORS[ Math.abs( index ) % COLORS.length ];
    }

    /**
     * Returns a profile which gives pixels in a variety of colours.
     *
     * @param   name  profile name
     * @return  profile providing coloured pixels
     */
    public static MarkStyleProfile points( final String name ) {
        return new MarkStyleProfile() {
            public String getName() {
                return name;
            }
            public MarkStyle getStyle( int index ) {
                return MarkStyle.pointStyle( getColor( index ) );
            }
        };
    }

    /**
     * Returns a profile which gives filled circles of a given size
     * in a variety of colours.
     *
     * @param   name  profile name
     * @param   size  approximate radius of markers
     * @return  profile providing coloured spots
     */
    public static MarkStyleProfile spots( final String name, final int size ) {
        return new MarkStyleProfile() {
            public String getName() {
                return name;
            }
            public MarkStyle getStyle( int index ) {
                return MarkStyle.filledCircleStyle( getColor( index ), size );
            }
        };
    }

    /**
     * Returns a profile which gives filled semi-transparent circles of
     * a given size in a variety of colours.
     *
     * @param  name  profile name
     * @param  size  approximate radius of markers
     * @param  alpha  transparency of spots (0 is invisible, 1 is opaque)
     * @return profile providing ghostly spots
     */
    public static MarkStyleProfile ghosts( final String name, final int size,
                                           float alpha ) {
        final int iAlpha = (int) ( alpha * 255.99 );
        final int lAlpha = Math.max( iAlpha, 96 );
        final int lsize = Math.max( size, 1 );
        return new MarkStyleProfile() {
            public String getName() {
                return name;
            }
            public MarkStyle getStyle( int index ) {
                Color baseColor = getColor( index );
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
     * Returns a profile which gives line-drawn shapes of various kinds.
     *
     * @param  name  profile name
     * @param  size  approximate radius of markers
     * @param  color color of markers, or <tt>null</tt> for various
     * @return  profile providing open shapes
     */
    public static MarkStyleProfile openShapes( final String name,
                                               final int size,
                                               final Color color ) {
        return new MarkStyleProfile() {
            public String getName() {
                return name;
            }
            public MarkStyle getStyle( int index ) {
                Color col = color == null ? getColor( index ) : color;
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
     * Returns a profile which gives filled shapes of various kinds.
     *
     * @param  name  profile name
     * @param  size  approximate radius of markers
     * @param  color color of markers, or <tt>null</tt> for various
     * @return  profile providing filled shapes
     */
    public static MarkStyleProfile filledShapes( final String name,
                                                 final int size,
                                                 final Color color ) {
        return new MarkStyleProfile() {
            public String getName() {
                return name;
            }
            public MarkStyle getStyle( int index ) {
                Color col = color == null ? getColor( index ) : color;
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
}
