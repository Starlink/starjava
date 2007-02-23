package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Represents the style in which N-dimensional errors will be drawn 
 * around a marker.  This class encapsulates one {@link ErrorRenderer} 
 * and and N-element array of {@link ErrorMode}s.  Instances are immutable.
 *
 * @author    Mark Taylor
 * @since     23 Feb 2007
 */
public class ErrorStyle {

    private final ErrorMode[] modes_;
    private final ErrorRenderer renderer_;
    private final boolean isBlank_;

    /** Style representing no errors drawn. */
    public static final ErrorStyle NONE =
        new ErrorStyle( new ErrorMode[ 0 ], ErrorRenderer.NONE );

    /**
     * Constructor.
     *
     * @param  modes   error mode array
     * @param  renderer  error renderer
     */
    public ErrorStyle( ErrorMode[] modes, ErrorRenderer renderer ) {
        modes_ = modes;
        renderer_ = renderer;
        boolean isBlank = true;
        for ( int im = 0; im < modes.length; im++ ) {
            isBlank = isBlank && modes[ im ] == ErrorMode.NONE;
        }
        isBlank_ = isBlank;
    }

    /**
     * Returns the array of error modes.
     *
     * @return   error modes
     */
    public ErrorMode[] getModes() {
        return modes_;
    }

    /**
     * Returns the error renderer.
     *
     * @return  error renderer
     */
    public ErrorRenderer getRenderer() {
        return renderer_;
    }

    /**
     * Indicates whether this style will result in drawing nothing.
     *
     * @return  if true, drawing this style will draw nothing
     */
    public boolean isBlank() {
        return isBlank_;
    }

    /**
     * Returns an icon suitable for representing this style in a legend.
     *
     * @param  width   icon total width
     * @param  height  icon total height
     */
    public Icon getLegendIcon( int width, int height ) {
        return new ErrorIcon( width, height, 1, 1 );
    }

    public boolean equals( Object o ) {
        if ( ! getClass().isAssignableFrom( o.getClass() ) ) {
            return false;
        }
        ErrorStyle other = (ErrorStyle) o;
        if ( this.isBlank() && other.isBlank() ) {
            return true;
        }
        int nmode = this.modes_.length;
        if ( other.modes_.length != nmode ) {
            return false;
        }
        for ( int im = 0; im < nmode; im++ ) {
            ErrorMode mode = this.modes_[ im ];
            if ( mode != other.modes_[ im ] ) {
                return false;
            }
        }
        if ( ! this.renderer_.equals( other.renderer_ ) ) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        if ( isBlank() ) {
            return 0;
        }
        int code = 9997;
        int nmode = modes_.length;
        for ( int im = 0; im < nmode; im++ ) {
            code = ( 23 * code ) + modes_[ im ].hashCode();
        }
        code = ( 23 * code ) + renderer_.hashCode();
        return code;
    }

    /**
     * Icon which will drawn an ErrorStyle.
     */
    private class ErrorIcon implements Icon {
        private final int width_;
        private final int height_;
        private final int[] xoffs_;
        private final int[] yoffs_;

        /**
         * Constructor.
         *
         * @param  width   total icon width
         * @param  height  total icon height
         * @param  xpad  internal padding in X direction
         * @param  ypad  internal padding in Y direction
         */
        ErrorIcon( int width, int height, int xpad, int ypad ) {
            width_ = width;
            height_ = height;
            int w2 = width / 2 - xpad;
            int h2 = height / 2 - ypad;
            int ndim = modes_.length;
            xoffs_ = new int[ ndim * 2 ];
            yoffs_ = new int[ ndim * 2 ];
            if ( ndim > 0 ) {
                ErrorMode mode = modes_[ 0 ];
                xoffs_[ 0 ] = (int) Math.round( - mode.getExampleLower() * w2 );
                yoffs_[ 0 ] = 0;
                xoffs_[ 1 ] = (int) Math.round( + mode.getExampleUpper() * w2 );
                yoffs_[ 1 ] = 0;
            }
            if ( ndim > 1 ) {
                ErrorMode mode = modes_[ 1 ];
                xoffs_[ 2 ] = 0;
                yoffs_[ 2 ] = (int) Math.round( + mode.getExampleLower() * h2 );
                xoffs_[ 3 ] = 0;
                yoffs_[ 3 ] = (int) Math.round( - mode.getExampleUpper() * h2 );
            }
            if ( ndim > 2 ) {
                ErrorMode mode = modes_[ 2 ];
                double theta = Math.toRadians( 40 );
                double slant = 0.8;
                double c = Math.cos( theta ) * slant;
                double s = Math.sin( theta ) * slant;
                double lo = mode.getExampleLower();
                double hi = mode.getExampleUpper();
                xoffs_[ 4 ] = (int) Math.round( - c * lo * w2 );
                yoffs_[ 4 ] = (int) Math.round( + s * lo * h2 );
                xoffs_[ 5 ] = (int) Math.round( + c * hi * w2 );
                yoffs_[ 5 ] = (int) Math.round( - s * hi * h2 );
            }
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            renderer_.drawErrors( g, x + width_ / 2, y + height_ / 2,
                                  xoffs_, yoffs_ );
        }
    }
}
