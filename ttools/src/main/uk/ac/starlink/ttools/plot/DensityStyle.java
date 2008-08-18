package uk.ac.starlink.ttools.plot;

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

    private final Channel channel_;
    private final float[] rgba_;
    private Shader shader_;

    private static final int ICON_WIDTH = 24;
    private static final int ICON_HEIGHT = 8;

    /** Red colour channel. */
    public static final Channel RED = new Channel( "Red", 0 );

    /** Green colour channel. */
    public static final Channel GREEN = new Channel( "Green", 1 );

    /** Blue colour channel. */
    public static final Channel BLUE = new Channel( "Blue", 2 );

    /**
     * Constructs a new style which plots in a given colour channel.
     *
     * @param  channel  colour channel
     */
    public DensityStyle( Channel channel ) {
        channel_ = channel;
        rgba_ = new float[ 4 ];
        shader_ = Shaders.BLACK_WHITE;
    }

    /**
     * Sets the shader to use in indexed (non-RGB) mode.
     * This should be an absolute shader.
     *
     * @param  shader  shader
     */
    public void setShader( Shader shader ) {
        assert shader_.isAbsolute();
        shader_ = shader;
    }

    /**
     * Returns the shader to use in indexed (non-RGB) mode.
     * This should be an absolute shader.
     *
     * @return  shader
     */
    public Shader getShader() {
        return shader_;
    }

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
    public int levelBits( byte level ) {
        if ( isRGB() ) {
            return ( 0x000000ff & level ) << channel_.shift_;
        }
        else {
            getShader().adjustRgba( rgba_, (float) ( 0xff & level ) / 255f );
            return ( (int) ( rgba_[ 2 ] * 255.9f ) & 0xff ) << 0
                 | ( (int) ( rgba_[ 1 ] * 255.9f ) & 0xff ) << 8
                 | ( (int) ( rgba_[ 0 ] * 255.9f ) & 0xff ) << 16
                 | ( (int) ( rgba_[ 3 ] * 255.9f ) & 0xff ) << 24;
        }
    }

    /**
     * Returns the colour channel.
     *
     * @return  0 = red, 1 = green, 2 = blue
     */
    private Channel getChannel() {
        return channel_;
    }

    /**
     * Indicates whether the plotting is currently to be treated as
     * three-channel RGB plotting or as single-channel intensity plotting.
     *
     * @return   true iff plotting is currently three-channel
     */
    protected abstract boolean isRGB();

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

    public String toString() {
        return channel_.toString();
    }

    /**
     * Enumeration class which describes a colour channel.
     */
    public static class Channel {
        private final String name_;
        private final int irgb_;
        private final int shift_;

        /**
         * Constructor.
         *
         * @param   name   channel (colour) name
         * @param   irgb   RGB index: 0 = red, 1 = green, 2 = blue
         * @param   number of bits to shift left to turn an 8-bit value into
         *                 an or-able 24-bit value for this channel
         */
        private Channel( String name, int irgb ) {
            name_ = name;
            irgb_ = irgb;
            shift_ = 8 * ( 2 - irgb );
        }

        /**
         * Returns channel name.
         */
        public String toString() {
            return name_;
        }
    }
}
