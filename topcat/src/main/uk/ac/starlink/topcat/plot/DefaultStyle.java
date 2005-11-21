package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Stroke;

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
    private Stroke stroke_;
    private final Object otherAtts_;
    private static final Object DUMMY_ATTS = new Object();

    /**
     * Constructs a style given a colour, style and <code>otherAtts</code>
     * object.
     *
     * @param  color  initial colour
     * @param  stroke  initial stroke
     * @param  otherAtts  object distinguishing this instance
     */
    protected DefaultStyle( Color color, Stroke stroke, Object otherAtts ) {
        otherAtts_ = otherAtts == null ? DUMMY_ATTS : otherAtts;
        setColor( color );
        setStroke( stroke );
    }

    /**
     * Constructs a style given a colour and an <code>otherAtts</code> object.
     */
    protected DefaultStyle( Color color, Object otherAtts ) {
        this( color, Styles.PLAIN_STROKE, otherAtts );
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
     * Sets the stroke of this style.
     *
     * @param  stroke  new stroke
     */
    public void setStroke( Stroke stroke ) {
        stroke_ = stroke;
    }

    /**
     * Returns the stroke of this style.
     *
     * @return  stroke
     */
    public Stroke getStroke() {
        return stroke_;
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
                && getStroke().equals( other.getStroke() )
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
        code = code * 23 + getStroke().hashCode();
        code = code * 23 + getOtherAtts().hashCode();
        return code;
    }
}
