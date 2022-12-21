package uk.ac.starlink.ttools.plot2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

/**
 * Graphical representation of aux shading range which can be placed
 * near the plot.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public class ShadeAxis {

    private final Shader shader_;
    private final Scaler scaler_;
    private final String label_;
    private final Captioner captioner_;
    private final double crowding_;
    private final int rampWidth_;
    private static final Orientation ORIENTATION = Orientation.ANTI_Y;
    private static final Caption PAD_CAPTION = Caption.createCaption( "0" );
    private static final TickLook TICKLOOK =
        TickLook.createStandardLook( "outside", -1 );

    /**
     * Constructor.
     *
     * @param  shader  object performing the actual shading
     * @param  scaler   maps data values to unit range
     * @param  label  axis label
     * @param  captioner   text rendering object
     * @param  crowding   1 for normal tick density, lower for fewer labels,
     *                    higher for more
     * @param  rampWidth  preferred number of pixels in the lateral direction
     *                    for the the ramp icon;
     *                    this value is not used by this class, but this
     *                    class serves as a useful place to keep it
     */
    public ShadeAxis( Shader shader, Scaler scaler, String label,
                      Captioner captioner, double crowding, int rampWidth ) {
        shader_ = shader;
        scaler_ = scaler;
        label_ = label;
        captioner_ = captioner;
        crowding_ = crowding;
        rampWidth_ = rampWidth;
    }

    /**
     * Returns an icon containing a graphical representation of the
     * shading axis including axis annotations (label and numbers).
     * The supplied rectangle gives the dimensions of the actual
     * scale ramp, not including annotations.
     *
     * @param  rampBounds  ramp position
     * @return  axis icon, with equality semantics
     */
    @Equality
    public Icon createAxisIcon( Rectangle rampBounds ) {
        return createShaderAxisIcon( rampBounds );
    }

    /**
     * Returns the insets that the bounds icon would like to have for
     * annotating the axis given the dimensions of the actual scale ramp
     * graphic.
     *
     * @param  rampBounds  ramp position
     * @return  insets surrounding <code>rampBounds</code>
     *          required for annotation
     */
    public Insets getRampInsets( Rectangle rampBounds ) {
        return createShaderAxisIcon( rampBounds ).getInsets();
    }

    /**
     * Returns a nominal number of pixels required at the top and bottom
     * of the ramp icon to accommodated possible axis labels.
     * This is currently half the height of a digit caption.
     *
     * @return   ramp icon vertical padding for labels
     */
    public int getEndPadding() {
        return ( captioner_.getCaptionBounds( PAD_CAPTION ).height + 1 ) / 2;
    }

    /**
     * Preferred number of pixels in the lateral direction for the axis
     * colour ramp.  Not enforced by any behaviour of this class.
     *
     * @return  preferred ramp width
     */
    public int getRampWidth() {
        return rampWidth_;
    }

    /**
     * Returns the minimum data value represented on this axis.
     *
     * @return   data lower limit
     */
    public double getDataLow() {
        return scaler_.getLow();
    }

    /**
     * Returns the maximum data value represented on this axis.
     *
     * @return   data upper limit
     */
    public double getDataHigh() {
        return scaler_.getHigh();
    }

    /**
     * Returns the text label for this axis.
     *
     * @return  axis label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the crowding factor for this axis.
     *
     * @return   1 for normal tick density, lower for fewer labels,
     *           higher for more
     */
    public double getCrowding() {
        return crowding_;
    }

    /**
     * Returns a new icon for this axis given bounds of the scale graphic
     * itself.
     *
     * @param   rampBounds  ramp position
     * @return  icon
     */
    private ShaderIcon createShaderAxisIcon( Rectangle rampBounds ) {
        Tick[] ticks = ( scaler_.isLogLike() ? BasicTicker.LOG
                                             : BasicTicker.LINEAR )
                      .getTicks( scaler_.getLow(), scaler_.getHigh(),
                                 false, captioner_, ORIENTATION,
                                 rampBounds.height, crowding_ );
        return new ShaderIcon( shader_, scaler_, label_, captioner_,
                               rampBounds, ticks );
    }

    /**
     * Icon displaying scale ramp graphic and associated axis annotation.
     */
    @Equality
    private static class ShaderIcon implements Icon {
        private final Shader shader_;
        private final Scaler scaler_;
        private final String label_;
        private final Captioner captioner_;
        private final Rectangle box_;
        private final Tick[] ticks_;
        private final Axis axis_;

        /**
         * Constructor.
         *
         * @param  shader  object performing the actual shading
         * @param  scaler   maps data to unit range
         * @param  label  axis label
         * @param  captioner   text rendering object
         * @param  rampBounds  bounds of actual ramp scale graphic
         * @param  ticks   axis ticks for annotation
         */
        public ShaderIcon( Shader shader, Scaler scaler,
                           String label, Captioner captioner,
                           Rectangle rampBounds, Tick[] ticks ) {
            shader_ = shader;
            scaler_ = scaler;
            label_ = label;
            captioner_ = captioner;
            box_ = rampBounds;
            ticks_ = ticks;
            axis_ = Axis.createAxis( box_.y, box_.y + box_.height,
                                     scaler.getLow(), scaler.getHigh(),
                                     scaler.isLogLike(), false );
        }

        public int getIconWidth() {
            return box_.width;
        }

        public int getIconHeight() {
            return box_.height;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {

            /* Remember graphics context. */
            Graphics2D g2 = (Graphics2D) g;
            Color color0 = g2.getColor();

            /* Paint the ramp.  First prepare a line of pixels that
             * appear in the ramp transversely to the shading direction.
             * If the shader is absolute these will have no effect,
             * but for non-absoloute shaders they form the base colours
             * which the shader modifies for output.  So you see a ramp
             * of stripes, showing how the shader affects a range of
             * different input colours.  The initial range is given by
             * a base shader. */
            Shader baseShader = Shaders.DFLT_GRID_SHADER;
            float[] baseRgba = Color.BLACK.getRGBComponents( null );
            int nx = box_.width;
            float[][] baseRgbas = new float[ nx ][ 4 ];
            for ( int ix = 0; ix < nx; ix++ ) {
                baseRgbas[ ix ] = baseRgba.clone();
                baseShader.adjustRgba( baseRgbas[ ix ],
                                       ix / (float) ( nx - 1 ) );
            }

            /* Then shade each row of pixels in turn along the shading
             * direction of the axis. */
            for ( int iy = 0; iy < box_.height; iy++ ) {

                /* The ramp is notionally a vertical stack of coloured
                 * rectangles each one pixel high.  Drawing the rectangles
                 * like that works fine for bitmapped output formats.
                 * However, in some cases it can result in tiny (sub-pixel)
                 * just-visible gaps between rectangles in certain rendering
                 * contexts (e.g. at time of writing PDF output when
                 * rendered by Atril but not by Okular).  This is presumably
                 * down to rounding errors somewhere or other.
                 * So, except for the top line where it would overflow the box,
                 * draw the rectangles two pixels high rather than one,
                 * so that if any gap is left it's just the next-door colour
                 * that shows through rather than the white background.
                 * Apart from any such sub-pixel gap, the additional pixel
                 * of height is then overplotted by the next rectangle. */
                int lineHeight = iy < box_.height - 1 ? 2 : 1;
                int hy = box_.y + ( box_.height - iy ) - ( lineHeight - 1 );

                /* Work out the fractional value to pass to the shader. */
                int gy = box_.y + iy;
                double dval = axis_.graphicsToData( gy + 0.5 );
                float frac = (float) scaler_.scaleValue( dval );

                /* Then draw the pixels, either as a block for an absolute
                 * shader, or a pixel at a time for non-absoloute. */
                if ( shader_.isAbsolute() ) {
                    float[] rgba = baseRgbas[ 0 ].clone();
                    shader_.adjustRgba( rgba, frac );
                    g.setColor( new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ],
                                           rgba[ 3 ] ) );
                    g.fillRect( box_.x, hy, nx, lineHeight );
                }
                else {
                    for ( int ix = 0; ix < nx; ix++ ) {
                        float[] rgba = baseRgbas[ ix ].clone();
                        shader_.adjustRgba( rgba, frac );
                        g.setColor( new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ],
                                               rgba[ 3 ] ) );
                        g.fillRect( box_.x + ix, hy, 1, lineHeight );
                    }
                }
            }

            /* Paint the ramp frame and axis annotations. */
            g2.setColor( Color.BLACK );
  
            /* Paint the rectangular frame by filling four 1-pixel-wide
             * rectangles, one for each side of the frame.
             * This may look like an eccentric alternative
             * to a simple drawRect call, but the trouble is that for
             * some vector Graphics implementations (PDF/PostScript outputs)
             * you tend to get subpixel offsets in line positioning,
             * so that mixing drawRect and fillRect calls leads to
             * a ramp frame that is slightly offset from the contents.
             * That might (or might not) be a bug in those Graphics
             * implementations, but it's more reliable and straightforward
             * to work round that behaviour than to try fixing it. */
            int fw = 1;           // frame width
            int bx = box_.x;
            int by = box_.y + 1;  // 1-pixel offset in drawing code above
            g2.fillRect( bx, by, fw, box_.height );
            g2.fillRect( bx, by + box_.height - fw, box_.width, fw );
            g2.fillRect( bx, by, box_.width, fw );
            g2.fillRect( bx + box_.width - fw, by, fw, box_.height );

            /* Paint the axis labels. */
            AffineTransform trans0 = g2.getTransform();
            g2.translate( box_.x + box_.width, box_.y + box_.height );
            g2.rotate( - Math.PI / 2 );
            axis_.drawLabels( ticks_, label_, captioner_, TICKLOOK,
                              ORIENTATION, false, g2 );

            /* Reset graphics context. */
            g2.setColor( color0 );
            g2.setTransform( trans0 );
        }

        /**
         * Returns the required insets for axis annotation.
         *
         * @return  space outside of ramp bounds required for axis labels
         */
        public Insets getInsets() {
            Rectangle bounds =
                axis_.getLabelBounds( ticks_, label_, captioner_,
                                      ORIENTATION, false );
            bounds = AffineTransform.getRotateInstance( - Math.PI / 2 )
                                    .createTransformedShape( bounds )
                                    .getBounds();
            int bottom = Math.max( 0, bounds.y + bounds.height - box_.height );
            int top = Math.max( 0, - bounds.y );
            int left = 0;
            int right = Math.max( 0, bounds.x + bounds.width );
            return new Insets( top, left, bottom, right );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ShaderIcon ) {
                ShaderIcon other = (ShaderIcon) o;
                return this.shader_.equals( other.shader_ )
                    && this.scaler_.equals( other.scaler_ )
                    && PlotUtil.equals( this.label_, other.label_ )
                    && this.captioner_.equals( other.captioner_ )
                    && this.box_.equals( other.box_ )
                    && Arrays.equals( this.ticks_, other.ticks_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 271223;
            code = 23 * code + shader_.hashCode();
            code = 23 * code + scaler_.hashCode();
            code = 23 * code + PlotUtil.hashCode( label_ );
            code = 23 * code + captioner_.hashCode();
            code = 23 * code + box_.hashCode();
            code = 23 * code + Arrays.hashCode( ticks_ );
            return code;
        }
    }
}
