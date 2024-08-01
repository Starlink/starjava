package uk.ac.starlink.ttools.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.Arrays;

/**
 * Convenience partial implementation of Style which has a defined colour
 * and stroke style, with other attributes given by a single object.
 * The <code>otherAtts</code> attribute
 * characterises everything apart from colour, stroke and class which 
 * distinguish one instance of this class from another, and 
 * is used by the {@link #equals} implementation to determine object equality.
 * <code>otherAtts</code> probably ought to be immutable.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public abstract class DefaultStyle implements Style {

    private Color color_;
    private int lineWidth_ = 1;
    private float[] dash_;
    private final Object otherAtts_;
    private static final Object DUMMY_ATTS = new Object();

    /**
     * Constructs a style given a colour, style and <code>otherAtts</code>
     * object.
     *
     * @param  color  initial colour
     * @param  otherAtts  object distinguishing this instance
     */
    @SuppressWarnings("this-escape")
    protected DefaultStyle( Color color, Object otherAtts ) {
        otherAtts_ = otherAtts == null ? DUMMY_ATTS : otherAtts;
        setColor( color );
    }

    /**
     * Sets the colour of this style.
     *
     * @param   color  new colour
     */
    public void setColor( Color color ) {
        color_ = color;
    }

    /**
     * Returns the colour of this style.
     *
     * @return  colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Sets the line width associated with this style.
     *
     * @param  width  line width (&gt;=1)
     */
    public void setLineWidth( int width ) {
        lineWidth_ = width;
    }

    /**
     * Returns the line width associated with this style.
     *
     * @return  line width
     */
    public int getLineWidth() {
        return lineWidth_;
    }

    /**
     * Sets the dash pattern associated with this style. 
     * This is like the dash array in {@link java.awt.BasicStroke},
     * except that it is multiplied by the line width before use.
     * May be null for a solid line.
     *
     * @param  dash   dash array
     */
    public void setDash( float[] dash ) {
        dash_ = dash;
    }

    /**
     * Returns the dash pattern associated with this style.
     * May be null for a solid line.
     *
     * @return  dash array
     */
    public float[] getDash() {
        return dash_;
    }

    /**
     * Returns a stroke suitable for drawing lines in this style.
     * The line join and cap policy must be provided.
     *
     * @param  cap     one of {@link java.awt.BasicStroke}'s CAP_* constants
     * @param  join    one of {@link java.awt.BasicStroke}'s JOIN_* constants
     * @return  stroke
     */
    public Stroke getStroke( int cap, int join ) {
        int thick = getLineWidth();
        float[] dash = getDash();
        if ( dash != null && thick != 1 ) {
            dash = dash.clone();
            for ( int i = 0; i < dash.length; i++ ) {
                dash[ i ] *= thick;
            }
        }
        return new BasicStroke( thick, cap, join, 10f, dash, 0f );
    }

    /**
     * Returns the object which distinguishes this object from other ones
     * of the same colour and class.
     *
     * @return  otherAtts object
     */
    public Object getOtherAtts() {
        return otherAtts_;
    }

    /**
     * Returns true if <code>o</code> satisfies the following conditions
     * <ol>
     * <li>It has the same class as this one
     * <li>It has the same colour as this one
     * <li>It has the same stroke as this one
     * <li>The <code>otherAtts</code> object specified at its creation
     *     matches (according to <code>equals()</code> this one's
     * </ol>
     */
    public boolean equals( Object o ) {
        if ( o instanceof DefaultStyle ) {
            DefaultStyle other = (DefaultStyle) o;
            return getClass().equals( other.getClass() )
                && getColor().equals( other.getColor() )
                && getLineWidth() == other.getLineWidth()
                && Arrays.equals( getDash(), other.getDash() )
                && getOtherAtts().equals( other.getOtherAtts() );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = 5555;
        code = code * 23 + getClass().hashCode();
        code = code * 23 + getColor().hashCode();
        code = code * 23 + getStroke( 0, 0 ).hashCode();
        code = code * 23 + getOtherAtts().hashCode();
        return code;
    }

    public String toString() {
        return new StringBuffer()
            .append( getClass().getName() )
            .append( getColor() )
            .append( getOtherAtts() )
            .toString();
    }

    /**
     * Returns a stroke which resembles a given template but has specified
     * end cap and line join policies.
     *
     * @param  stroke  template stroke
     * @param  cap     one of {@link java.awt.BasicStroke}'s CAP_* constants
     * @param  join    one of {@link java.awt.BasicStroke}'s JOIN_* constants
     * @return  fixed stroke, may be the same as the input one
     */
    public static Stroke getStroke( Stroke stroke, int cap, int join ) {
        if ( stroke instanceof BasicStroke ) {
            BasicStroke bstroke = (BasicStroke) stroke;
            if ( bstroke.getEndCap() != cap || bstroke.getLineJoin() != join ) {
                return new BasicStroke( bstroke.getLineWidth(), cap, join,
                                        bstroke.getMiterLimit(),
                                        bstroke.getDashArray(),
                                        bstroke.getDashPhase() );
            }
        }
        return stroke;
    }
}
