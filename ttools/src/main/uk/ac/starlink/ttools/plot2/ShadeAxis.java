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
    private final boolean log_;
    private final boolean flip_;
    private final double dlo_;
    private final double dhi_;
    private final String label_;
    private final Captioner captioner_;
    private static final Orientation ORIENTATION = Orientation.ANTI_Y;

    /**
     * Constructor.
     *
     * @param  shader  object performing the actual shading
     * @param  log   true for logarithmic scaling, false for linear
     * @param  flip  true to invert scale
     * @param  dlo   minimum data value
     * @param  dhi   maximum data value
     * @param  label  axis label
     * @param  captioner   text rendering object
     */
    public ShadeAxis( Shader shader, boolean log, boolean flip,
                      double dlo, double dhi,
                      String label, Captioner captioner ) {
        shader_ = shader;
        log_ = log;
        flip_ = flip;
        dlo_ = dlo;
        dhi_ = dhi;
        label_ = label;
        captioner_ = captioner;
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
     * Indicates whether scale is logarithmic.
     *
     * @return  true for logarithmic, false for linear
     */
    public boolean isLog() {
        return log_;
    }

    /**
     * Indicates whether scale is reversed.
     *
     * @return  true for low to high, false for high to low
     */
    public boolean isFlip() {
        return flip_;
    }

    /**
     * Returns a new icon for this axis given bounds of the scale graphic
     * itself.
     *
     * @param   rampBounds  ramp position
     * @return  icon
     */
    private ShaderIcon createShaderAxisIcon( Rectangle rampBounds ) {
        Tick[] ticks =
            Tick.getTicks( dlo_, dhi_, log_, false, captioner_, ORIENTATION,
                           rampBounds.height, 2 );
        return new ShaderIcon( shader_, log_, flip_, dlo_, dhi_, label_,
                               captioner_, rampBounds, ticks );
    }

    /**
     * Icon displaying scale ramp graphic and associated axis annotation.
     */
    @Equality
    private static class ShaderIcon implements Icon {
        private final Shader shader_;
        private final boolean log_;
        private final boolean flip_;
        private final double dlo_;
        private final double dhi_;
        private final String label_;
        private final Captioner captioner_;
        private final Rectangle box_;
        private final Tick[] ticks_;
        private final Icon rampIcon_;
        private final Axis axis_;

        /**
         * Constructor.
         *
         * @param  shader  object performing the actual shading
         * @param  log   true for logarithmic scaling, false for linear
         * @param  flip  true to invert scale
         * @param  dlo   minimum data value
         * @param  dhi   maximum data value
         * @param  label  axis label
         * @param  captioner   text rendering object
         * @param  rampBounds  bounds of actual ramp scale graphic
         * @param  ticks   axis ticks for annotation
         */
        public ShaderIcon( Shader shader, boolean log, boolean flip,
                           double dlo, double dhi, String label,
                           Captioner captioner, Rectangle rampBounds,
                           Tick[] ticks ) {
            shader_ = shader;
            log_ = log;
            flip_ = flip;
            dlo_ = dlo;
            dhi_ = dhi;
            label_ = label;
            captioner_ = captioner;
            box_ = rampBounds;
            ticks_ = ticks;
            rampIcon_ = Shaders.invert( shader )
                       .createIcon( false, box_.width, box_.height, 0, 0 );
            axis_ = Axis.createAxis( box_.y, box_.y + box_.height, dlo, dhi,
                                     log_, flip_ );
        }

        public int getIconWidth() {
            return rampIcon_.getIconWidth();
        }

        public int getIconHeight() {
            return rampIcon_.getIconHeight();
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2 = (Graphics2D) g;
            rampIcon_.paintIcon( c, g2, x, y );
            Color color0 = g2.getColor();
            g2.setColor( Color.BLACK );
            AffineTransform trans0 = g2.getTransform();
            g2.drawRect( box_.x, box_.y, box_.width, box_.height );
            g2.translate( box_.x + box_.width, box_.y + box_.height );
            g2.rotate( - Math.PI / 2 );
            axis_.drawLabels( ticks_, label_, captioner_, ORIENTATION,
                              false, g2 );
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
                    && this.log_ == other.log_
                    && this.flip_ == other.flip_
                    && this.dlo_ == other.dlo_
                    && this.dhi_ == other.dhi_
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
            code = 23 * code + ( log_ ? 5 : 7 );
            code = 23 * code + ( flip_ ? 11 : 13 );
            code = 23 * code + Float.floatToIntBits( (float) dlo_ );
            code = 23 * code + Float.floatToIntBits( (float) dhi_ );
            code = 23 * code + PlotUtil.hashCode( label_ );
            code = 23 * code + captioner_.hashCode();
            code = 23 * code + box_.hashCode();
            code = 23 * code + Arrays.hashCode( ticks_ );
            return code;
        }
    }
}
