package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Provides a set of MarkStyles which form some sort of a compatible set.
 * A given instance of this class might give all spots with different
 * colours, or all outline-type markers of different shapes for instance.
 * The idea is that a scatter plot can derive all its marker styles from a
 * single MarkStyleProfile to achieve a uniform visual style.
 * All markstyles got from a given profile should therefore be of a 
 * similar size, and look compatible with each other in some sense.
 *
 * <p>Static factory methods are provided giving several pre-defined
 * profiles.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Jul 2004
 */
public abstract class MarkStyleProfile {

    private String name_;

    private static final Color[] COLORS = new Color[] {
        Color.red,
        Color.blue,
        Color.green.darker(),
        Color.gray,
        Color.magenta,
        Color.pink.darker(),
        Color.orange.darker(),
        Color.cyan.darker(),
        Color.yellow.darker(),
        Color.black,
    };

    /**
     * Returns a marker style corresponding to a particular index.
     * The same index will always give the same style (or one equivalent
     * in the sense of <tt>equals</tt>), but for indices beyond 
     * a certain value ({@link #getStyleCount}) the markers may wrap
     * around.
     *
     * @param  index  code for the requested style
     * @return  style for code <tt>index</tt>
     */
    public abstract MarkStyle getStyle( int index );

    /**
     * Sets the name of this profile.
     *
     * @param  name  profile name
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Returns the name of this profile.
     *
     * @return  profile name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the name of this profile.
     *
     * @return   string representation
     */
    public String toString() {
        return getName();
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
     * Returns a profile which gives filled circles of a given size 
     * in a variety of colours.
     *
     * @param   size  approximate radius of markers
     * @return  profile providing coloured spots
     */
    public static MarkStyleProfile spots( final int size ) {
        return new MarkStyleProfile() {
            public MarkStyle getStyle( int index ) {
                return MarkStyle.filledCircleStyle( getColor( index ), size );
            }
        };
    }

    /**
     * Returns a profile which gives filled semi-transparent circles of
     * a given size in a variety of colours.
     *
     * @param  size  approximate radius of markers
     * @param  alpha  transparency of spots (0 is invisible, 1 is opaque)
     * @return profile providing ghostly spots
     */
    public static MarkStyleProfile ghosts( final int size, float alpha ) {
        final int iAlpha = (int) ( alpha * 255.99 );
        return new MarkStyleProfile() {
            public MarkStyle getStyle( int index ) {
                Color baseColor = getColor( index );
                Color color = new Color( baseColor.getRed(),
                                         baseColor.getBlue(),
                                         baseColor.getGreen(),
                                         iAlpha );
                return MarkStyle.filledCircleStyle( color, size );
            }
        };
    }

    /**
     * Returns a profile which gives line-drawn shapes of various kinds.
     *
     */
    public static MarkStyleProfile openShapes( final Color color, 
                                               final int size ) {
        return new MarkStyleProfile() {
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

    public static MarkStyleProfile filledShapes( final Color color,
                                                 final int size ) {
        return new MarkStyleProfile() {
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
