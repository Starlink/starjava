package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Graphics;

/**
 * Plots 3D points on a 2D graphics context.
 * This is an abstract class which does much of the common work but 
 * stops short of actually drawing the points.  The main problem to be
 * solved by concrete subclasses is that of ensuring that pixels 
 * 'in front' (ones with larger Z coordinates) obscure those 'behind'.
 *
 * <p>To enable strategies which may require some kind of sorting on a
 * full set of points prior to doing any plotting, clients of this class
 * must first submit all the points to be plotted using the
 * {@link #plot(double[],uk.ac.starlink.topcat.plot.MarkStyle)} method,
 * and finally call {@link #flush()} to ensure that the plotting has
 * taken place.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public abstract class PlotVolume {

    private final Graphics graphics_;
    private final int scale_;
    private final int xoff_;
    private final int yoff_;
    private DepthTweaker tweaker_;

    /**
     * Constructor.
     *
     * @param   c  component on which points will be plotted
     * @param   g  graphics context on which points will be plotted
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     */
    protected PlotVolume( Component c, Graphics g, double padFactor ) {
        graphics_ = g;
        int w = c.getWidth();
        int h = c.getHeight();
        scale_ = (int) Math.round( Math.min( h, w ) / padFactor );
        xoff_ = 0 + (int) ( ( w - scale_ ) / 2. );
        yoff_ = h - (int) ( ( h - scale_ ) / 2. );
        tweaker_ = new DepthTweaker( 1.0 );
    }

    /**
     * Returns the scaling constant for this volume.
     * This is the value by which the normalised coordinates are multiplied
     * to give the length on the screen in pixels of a line if it is
     * being plotted normal to the viewer.  It is approximately the linear
     * dimension of the component in pixels.
     *
     * @return  scale length
     */
    public int getScale() {
        return scale_;
    }

    /**
     * Submits a point for plotting.  The graphical effect is not guaranteed
     * to occur until a subsequent call to {@link #flush}.
     * The coordinate array gives the point's 3D position in normalised 
     * coordinates, in which points inside the unit cube 
     * centred at (.5,.5,.5) are intended to be visible under normal 
     * circumstances.
     * Note that the <code>coords</code> array is not guaranteed to retain
     * its contents after this call returns; this method must make copies
     * of the values if it needs to retain them.
     *
     * @param   coords   normalised (x,y,z) coordinates
     * @param   style    marker style for point
     */
    public void plot( double[] coords, MarkStyle style ) {
        int xp = projectX( coords[ 0 ] );
        int yp = projectY( coords[ 1 ] );
        int maxr = style.getMaximumRadius();
        int maxr2 = maxr * 2;
        if ( graphics_.hitClip( xp - maxr, yp - maxr, maxr2, maxr2 ) ) {
            plot( xp, yp, coords[ 2 ], style );
        }
    }

    /**
     * Returns the graphics tweaker used for rendering depth effects.
     *
     * @return  depth tweaker
     */
    public DepthTweaker getDepthTweaker() {
        return tweaker_;
    }

    /**
     * Determines the integer X value in graphics space from an X value
     * in normalised 3d space.
     *
     * @param   x  normalised space X coordinate
     * @return  graphics space X coordinate
     */
    public int projectX( double x ) {
        return xoff_ + (int) Math.round( x * scale_ );
    }

    /**
     * Determines the integer Y value in graphics space from a Y value
     * in normalised 3d space.
     *
     * @param   y  normalised space Y coordinate
     * @return  graphics space Y coordinate
     */
    public int projectY( double y ) {
        return yoff_ - (int) Math.round( y * scale_ );
    }

    /**
     * Plots a marker at a given point in graphics coordinates with a 
     * given additional Z coordinate.  As well as providing a z-buffer
     * type ordering to determine which marks obscure which others,
     * the Z value can be passed to this volume's
     * <code>DepthTweaker</code> object to perform depth rendering.
     *
     * @param  px  graphics space X coordinate
     * @param  py  graphics space Y coordinate
     * @param  z   depth of point; a point with a greater <code>z</code>
     *             should obscure a point with a lesser one
     * @param  style  object used to plot the point
     */
    protected abstract void plot( int px, int py, double z, MarkStyle style );

    /**
     * Ensures that all points submitted through the <code>plot</code>
     * method have been painted on the graphics context.
     */
    public abstract void flush();

    /**
     * Returns this object's graphics context.
     *
     * @return  graphics context
     */
    public Graphics getGraphics() {
        return graphics_;
    }
}
