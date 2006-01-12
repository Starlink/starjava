package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Style for the way that a density map (2d histogram) is plotted.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public abstract class DensityStyle implements Style, Icon {

    private static final int ICON_WIDTH = 24;
    private static final int ICON_HEIGHT = 8;

    /** Style which gives levels of redness. */
    public static final DensityStyle RED = new DensityStyle() {
        public int levelBits( byte level ) {
            return ( 0x000000ff & level ) << 16;
        }
    };

    /** Style which gives levels of greenness. */
    public static final DensityStyle GREEN = new DensityStyle() {
        public int levelBits( byte level ) {
            return ( 0x000000ff & level ) << 8;
        }
    };

    /** Style which gives levels of blueness. */
    public static final DensityStyle BLUE = new DensityStyle() {
        public int levelBits( byte level ) {
            return ( 0x000000ff & level );
        }
    };

    /** Greyscale style. */
    public static final DensityStyle WHITE = new DensityStyle() {
        public int levelBits( byte level ) {
            int lev = 0x000000ff & level;
            assert lev >= 0 && lev < 256;
            return lev | ( lev << 8 ) | ( lev << 16 );
        }
    };

    /** Blank (invisible) style. */
    public static final DensityStyle BLANK = new DensityStyle() {
        public int levelBits( byte level ) {
            return 0;
        }
    };

    /**
     * Styleset which contains RED, GREEN and BLUE.  Any styles beyond the
     * first three are invisible (transparent).
     */
    public static final StyleSet RGB = new StyleSet() {
        public String getName() {
            return "RGB";
        }
        public Style getStyle( int index ) {
            switch ( index ) {
                case 0: return RED;
                case 1: return GREEN;
                case 2: return BLUE;
                default: return BLANK;
            }
        }
    };

    /**
     * Styleset for which every entry is greyscale.
     */
    public static final StyleSet MONO = new StyleSet() {
        public String getName() {
            return "Monochrome";
        }
        public Style getStyle( int index ) {
            return WHITE;
        }
    };

    /**
     * Defines how the style looks.
     * This converts an unsigned byte value (that is a value in the
     * range 0-255 got by doing <code>value=0x000000ff&level</code>)
     * to a bitmask which can be OR-ed with an existing integer to
     * give a 32-type ARGB colour value.
     *
     * @param  level  unsigned byte value
     * @return  ORable bit mask for modifying a colour value
     */
    public abstract int levelBits( byte level );

    public Icon getLegendIcon() {
        return this;
    }

    public int getIconHeight() {
        return ICON_HEIGHT;
    }

    public int getIconWidth() {
        return ICON_WIDTH;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        g = g.create();
        int ylo = y;
        int yhi = y + ICON_HEIGHT;
        for ( int i = 0; i < ICON_WIDTH; i++ ) {
            byte level = (byte) ( 255 * i / ICON_WIDTH );
            g.setColor( new Color( 0xff000000 | levelBits( level ), true ) );
            g.drawLine( x + i, ylo, x + i, yhi );
        }
    }
}
