package uk.ac.starlink.ttools.plot2.geom;

/**
 * Defines the data range covered by a CubeSurface.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubeAspect {
    private final double[] xlimits_;
    private final double[] ylimits_;
    private final double[] zlimits_;
    private final double[] rotmat_;
    private final double zoom_;
    private final double xoff_;
    private final double yoff_;

    /**
     * Constructor.
     *
     * @param  xlimits  2-element array giving (min,max) X data coordinates
     * @param  ylimits  2-element array giving (min,max) Y data coordinates
     * @param  zlimits  2-element array giving (min,max) Z data coordinates
     * @param  rotmat  9-element rotation matrix applied before viewing
     * @param  zoom    zoom factor, 1 means cube roughly fills plot bounds
     * @param  xoff  graphics X offset in pixels, 0 means centred in plot bounds
     * @param  yoff  graphics Y offset in pixels, 0 means centred in plot bounds
     */
    public CubeAspect( double[] xlimits, double[] ylimits, double[] zlimits,
                       double[] rotmat, double zoom,
                       double xoff, double yoff ) {
        xlimits_ = xlimits;
        ylimits_ = ylimits;
        zlimits_ = zlimits;
        rotmat_ = rotmat.clone();
        zoom_ = zoom;
        xoff_ = xoff;
        yoff_ = yoff;
    }

    /**
     * Returns rotation matrix.
     *
     * @return  9-element rotation matrix
     */
    public double[] getRotation() {
        return rotmat_;
    }

    /**
     * Returns zoom factor.
     *
     * @return  zoom factor, 1 means cube roughly fills plot bounds
     */
    public double getZoom() {
        return zoom_;
    }

    /**
     * Return graphics X offset.
     *
     * @return  X offset in pixels, 0 means centred in plot bounds
     */
    public double getOffsetX() {
        return xoff_;
    }

    /**
     * Return graphics Y offset.
     *
     * @return  Y offset in pixels, 0 means centred in plot bounds
     */
    public double getOffsetY() {
        return yoff_;
    }

    /**
     * Returns 3D data bounds.
     *
     * @return  [3][2]-element array giving (min,max) data bounds for X, Y, Z
     */
    public double[][] getLimits() {
        return new double[][] { xlimits_, ylimits_, zlimits_ };
    }
}
