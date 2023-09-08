package uk.ac.starlink.ttools.plot2;

import java.awt.Insets;
import java.awt.Rectangle;

/**
 * Defines user preferences for padding a rectangular area.
 * This resembles {@link java.awt.Insets}, except that each member
 * may be null, to indicate that the user has no preference.
 * And it's immutable.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2016
 */
@Equality
public class Padding {

    private final Integer top_;
    private final Integer left_;
    private final Integer bottom_;
    private final Integer right_;

    /**
     * Constructs an empty padding object (no preferences).
     */
    public Padding() {
        this( null, null, null, null );
    }

    /**
     * Constructs a padding object with preferences for all dimensions.
     * Any of the arguments may be null.
     *
     * @param   top     required top margin in pixels, or null
     * @param   left    required left margin in pixels, or null
     * @param   bottom  required bottom margin in pixels, or null
     * @param   right   required right margin in pixels, or null
     */
    public Padding( Integer top, Integer left, Integer bottom, Integer right ) {
        top_ = top;
        left_ = left;
        bottom_ = bottom;
        right_ = right;
    }

    /**
     * Returns the required top margin.
     *
     * @return  top margin in pixels, or null for no preference
     */
    public Integer getTop() {
        return top_;
    }

    /**
     * Returns the required left margin.
     *
     * @return  left margin in pixels, or null for no preference
     */
    public Integer getLeft() {
        return left_;
    }

    /**
     * Returns the required bottom margin.
     *
     * @return  bottom margin in pixels, or null for no preference
     */
    public Integer getBottom() {
        return bottom_;
    }

    /**
     * Returns the required right margin.
     *
     * @return  right margin in pixels, or null for no preference
     */
    public Integer getRight() {
        return right_;
    }

    /**
     * Applies the requirements specified by this object to an existing
     * Insets object.  The members of the returned insets object are
     * those of this object where they are non-null, and those of the
     * supplied insets otherwise.
     *
     * @param  insets   input insets object, not null
     * @return  new insets object with values taken from this padding
     *          where available
     */
    public Insets overrideInsets( Insets insets ) {
        return new Insets( top_ == null ? insets.top : top_.intValue(),
                           left_ == null ? insets.left : left_.intValue(),
                           bottom_ == null ? insets.bottom : bottom_.intValue(),
                           right_ == null ? insets.right : right_.intValue() );
    }

    /**
     * Returns true if all the members of this padding object are non-null.
     *
     * @return   true iff all margins have definite values
     */
    public boolean isDefinite() {
        return top_ != null
            && left_ != null
            && bottom_ != null
            && right_ != null;
    }

    /**
     * Returns the insets object corresponding to this padding object
     * if all the members are non-null, and null otherwise.
     *
     * @return   insets if <code>isDefinite()</code>, otherwise null
     */
    public Insets toDefiniteInsets() {
        return isDefinite()
             ? new Insets( top_.intValue(),
                           left_.intValue(),
                           bottom_.intValue(),
                           right_.intValue() )
             : null;
    }

    @Override
    public int hashCode() {
        int code = 33267;
        code = 23 * code + PlotUtil.hashCode( top_ );
        code = 23 * code + PlotUtil.hashCode( left_ );
        code = 23 * code + PlotUtil.hashCode( bottom_ );
        code = 23 * code + PlotUtil.hashCode( right_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Padding ) {
            Padding other = (Padding) o;
            return PlotUtil.equals( this.top_, other.top_ )
                && PlotUtil.equals( this.left_, other.left_ )
                && PlotUtil.equals( this.bottom_, other.bottom_ )
                && PlotUtil.equals( this.right_, other.right_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns a non-null insets object based on a supplied Insets
     * which will be modified by the state of a supplied Padding.
     *
     * <p>This convenience method calls {@link #overrideInsets} if
     * <code>padding</code> is non-null,
     * otherwise it returns the input <code>insets</code>.
     *
     * @param  padding   padding to override insets value, may be null
     * @param  insets    default insets, not null
     * @return  effective insets
     */
    public static Insets padInsets( Padding padding, Insets insets ) {
        return padding == null ? insets : padding.overrideInsets( insets );
    }
}
