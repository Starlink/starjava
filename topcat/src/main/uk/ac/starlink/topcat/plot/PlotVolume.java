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
 * {@link #plot(double[],int)} method,
 * and finally call {@link #flush()} to ensure that the plotting has
 * taken place.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public abstract class PlotVolume {

    private final Graphics graphics_;
    private final MarkStyle[] styles_;
    private final int scale_;
    private final int xoff_;
    private final int yoff_;
    private final Fogger fogger_;

    /**
     * Constructor.
     *
     * @param   c  component on which points will be plotted
     * @param   g  graphics context on which points will be plotted
     * @param   styles  array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     */
    protected PlotVolume( Component c, Graphics g, MarkStyle[] styles, 
                          double padFactor, int[] padBorders ) {
        graphics_ = g;
        styles_ = (MarkStyle[]) styles.clone();
        int padLeft = padBorders[ 0 ];
        int padRight = padBorders[ 1 ];
        int padBottom = padBorders[ 2 ];
        int padTop = padBorders[ 3 ];
        int w = c.getWidth() - padLeft - padRight;
        int h = c.getHeight() - padBottom - padTop;
        scale_ = (int) Math.round( Math.min( h, w ) / padFactor );
        xoff_ = 0 + (int) ( ( w - scale_ ) / 2. ) + padLeft;
        yoff_ = h - (int) ( ( h - scale_ ) / 2. ) + padTop;
        fogger_ = new Fogger( 1.0 );
    }

    /**
     * Returns the array of styles whose markers can be plotted on this volume.
     *
     * @return  mark style array
     */
    public MarkStyle[] getStyles() {
        return styles_;
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
     * Returns the fogger used for rendering depth effects.
     *
     * @return  fogger
     */
    public Fogger getFogger() {
        return fogger_;
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
     * @param   istyle   index into the array of styles set up for this volume
     *                   which will define how the marker is plotted
     */
    public void plot( double[] coords, int istyle ) {
        int xp = projectX( coords[ 0 ] );
        int yp = projectY( coords[ 1 ] );
        int maxr = styles_[ istyle ].getMaximumRadius();
        int maxr2 = maxr * 2;
        if ( graphics_.hitClip( xp - maxr, yp - maxr, maxr2, maxr2 ) ) {
            plot( xp, yp, coords[ 2 ], istyle );
        }
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
     * the Z value may be used as a cue to do some depth rendering.
     *
     * @param  px  graphics space X coordinate
     * @param  py  graphics space Y coordinate
     * @param  z   depth of point; a point with a greater <code>z</code>
     *             should obscure a point with a lesser one
     * @param  istyle  index of the style used to plot the point
     */
    protected abstract void plot( int px, int py, double z, int istyle );

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
