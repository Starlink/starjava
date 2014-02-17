package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Positioned icon, with equality semantics.
 * A decoration is assumed fast to plot.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public class Decoration {
    private final Icon icon_;
    private final int gx_;
    private final int gy_;
    
    /**
     * Constructor.
     *
     * @param  icon   decoration content; this icon must have equality semantics
     * @param  gx   x position for icon
     * @param  gy   y position for icon
     */
    public Decoration( Icon icon, int gx, int gy ) {
        icon_ = icon;
        gx_ = gx;
        gy_ = gy;
    }
 
    /**
     * Returns this decoration's icon.
     *
     * @return  icon
     */
    public Icon getIcon() {
        return icon_;
    }

    /**
     * Returns this decoration's X position.
     *
     * @return   x position
     */
    public int getPosX() {
        return gx_;
    }

    /**
     * Returns this decoration's Y position.
     *
     * @return  y position
     */
    public int getPosY() {
        return gy_;
    }

    /**
     * Paints this decoration.
     * 
     * @param   g  graphics context
     */
    public void paintDecoration( Graphics g ) {
        icon_.paintIcon( null, g, gx_, gy_ );
    }

    @Override
    public String toString() {
        return "(" + gx_ + "," + gy_ + "):" + icon_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Decoration ) {
            Decoration other = (Decoration) o;
            return this.icon_.equals( other.icon_ )
                && this.gx_ == other.gx_
                && this.gy_ == other.gy_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 99901;
        code = 23 * code + icon_.hashCode();
        code = 23 * code + gx_;
        code = 23 * code + gy_;
        return code;
    }
}
