package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;

/**
 * Defines the view of a SkySurface.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class SkyAspect {
    private final double[] rotmat_;
    private final double zoom_;
    private final double xoff_;
    private final double yoff_;

    /**
     * Constructor.
     *
     * @param  rotmat  9-element rotation matrix
     * @param  zoom    zoom factor; 1 means the sky is approximately
     *                 the same size as plot bounds
     * @param  xoff  x offset of plot centre from plot bounds centre
     *               in dimensionless units; 0 is centred
     * @param  yoff  y offset of plot centre from plot bounds centre
     *               in dimensionless units; 0 is centred
     */
    public SkyAspect( double[] rotmat, double zoom, double xoff, double yoff ) {
        rotmat_ = rotmat.clone();
        zoom_ = zoom;
        xoff_ = xoff;
        yoff_ = yoff;
    }

    /**
     * Returns rotation matrix.
     *
     * @return  9-element coordinate rotation matrix
     */
    public double[] getRotation() {
        return rotmat_.clone();
    }

    /**
     * Returns zoom factor.
     * A value of 1 means the whole sky takes up approximately all the
     * available plotting region.
     *
     * @return  zoom factor
     */
    public double getZoom() {
        return zoom_;
    }

    /**
     * Returns the offset in the graphical X direction of the centre of
     * the sky drawing from the centre of the available plotting region.
     * Units are dimensionless; 0 is centred.
     *
     * @return  x offset
     */
    public double getOffsetX() {
        return xoff_;
    }

    /**
     * Returns the offset in the graphical Y direction of the centre of
     * the sky drawingn from the centre of the available plotting region.
     * Units are dimensionless; 0 is centred.
     *
     * @return  y offset
     */
    public double getOffsetY() {
        return yoff_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SkyAspect ) {
            SkyAspect other = (SkyAspect) o;
            return Arrays.equals( this.rotmat_, other.rotmat_ )
                && this.zoom_ == other.zoom_
                && this.xoff_ == other.xoff_
                && this.yoff_ == other.yoff_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 21123;
        code = 23 * code + Arrays.hashCode( rotmat_ );
        code = 23 * code + Float.floatToIntBits( (float) zoom_ );
        code = 23 * code + Float.floatToIntBits( (float) xoff_ );
        code = 23 * code + Float.floatToIntBits( (float) yoff_ );
        return code;
    }

    /**
     * Returns an optionally reflected unit matrix.
     *
     * @param   reflect  true for reflection
     * @return  unit matrix, possibly reflected
     */
    public static double[] unitMatrix( boolean reflect ) {
        double r = reflect ? -1 : +1;
        return new double[] {
            1, 0, 0,
            0, r, 0,
            0, 0, 1,
        };
    }
}
