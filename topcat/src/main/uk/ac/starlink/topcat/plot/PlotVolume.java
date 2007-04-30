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
 * <code>plot3D</code> methods,
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
    private final int width_;
    private final int height_;

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
        width_ = c.getWidth();
        height_ = c.getHeight();
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
     *
     * <p>Note that the <code>coords</code> array is not guaranteed to retain
     * its contents after this call returns; this method must make copies
     * of the values if it needs to retain them.
     *
     * <p>The return value indicates whether the point was actually plotted;
     * it may be false if the point was known to be off screen.
     * This isn't guaranteed to be exact; there may be false positives or
     * false negatives near the edge of the plotting area.
     *
     * @param   coords   normalised (x,y,z) coordinates
     * @param   istyle   index into the array of styles set up for this volume
     *                   which will define how the marker is plotted
     * @return  true iff the point was actually plotted
     */
    public boolean plot3d( double[] coords, int istyle ) {
        return plot3d( coords, istyle, true, 0, null, null, null );
    }

    /**
     * Submits a point with associated errors for plotting.
     * The graphical effect is not guaranteed to occur until a 
     * subsequent call to {@link #flush}.
     * The 3D point coordinates (one central point in <code>centre</code> and
     * <code>nerr</code> additional points in 
     * <code>xerrs</code>, <code>yerrs</code>, <code>zerrs</code>) are in
     * normalised coordinates, in which points inside the unit cube
     * centred at (.5,.5,.5) are intended to be visible under normal
     * circumstances.
     * The ordering of the error points is that required by the
     * {@link ErrorRenderer} class.
     *
     * <p>Note that the <code>centre</code> array is not guaranteed to retain
     * its contents after this call returns; this method must make copies
     * of the values if it needs to retain them.
     *
     * <p>The return value indicates whether the point was actually plotted;
     * it may be false if the point was known to be off screen.
     * This isn't guaranteed to be exact; there may be false positives or
     * false negatives near the edge of the plotting area.
     *
     * @param  centre  normalised (x,y,z) coordinates of main point
     * @param  istyle  index into the array of styles set up for this volume
     *                 which will define how the marker is plotted
     * @param  showPoint  whether the central point is to be plotted
     * @param  nerr    the number of error points
     * @param  xerrs   <code>nerr</code>-element array of X coordinates of
     *                 error points 
     * @param  yerrs   <code>nerr</code>-element array of Y coordinates of
     *                 error points
     * @param  zerrs   <code>nerr</code>-element array of Z coordinates of
     *                 error points
     * @return  true iff the point was actually plotted
     */
    public boolean plot3d( double[] centre, int istyle,
                           boolean showPoint, int nerr,
                           double[] xerrs, double[] yerrs, double[] zerrs ) {

        /* Calculate the position of the point in 2D graphics coordinates. */
        int xp = projectX( centre[ 0 ] );
        int yp = projectY( centre[ 1 ] );
        double z = centre[ 2 ];

        /* Check whether this point will be visible.  I arguably ought to 
         * check the bounds of the error bars as well for this, but 
         * (a) that can be a bit expensive and (b) if only the error bar 
         * and not the point is visible it is debatable whether you want 
         * to see it.  Use the component dimensions here and not the clip,
         * since the clip may correspond only to the part of the window
         * being repainted following a partial obscuration. */
        int maxr = styles_[ istyle ].getMaximumRadius();
        if ( xp - maxr < 0 || xp + maxr > width_ ||
             yp - maxr < 0 || yp + maxr > height_ ) {
            return false;
        }

        /* Calculate the positions of the error bar offsets in 2D graphics
         * coordinates. */
        if ( nerr > 0 ) {
            int[] xoffs = new int[ nerr ];
            int[] yoffs = new int[ nerr ];
            for ( int ierr = 0; ierr < nerr; ierr++ ) {
                if ( Double.isNaN( xerrs[ ierr ] ) ||
                     Double.isNaN( yerrs[ ierr ] ) ) {
                    xoffs[ ierr ] = 0;
                    yoffs[ ierr ] = 0;
                }
                else {
                    xoffs[ ierr ] = projectX( xerrs[ ierr ] ) - xp;
                    yoffs[ ierr ] = projectY( yerrs[ ierr ] ) - yp;
                }
            }

            /* Hand off the actual plotting to the concrete subclass. */
            plot2d( xp, yp, z, istyle, showPoint, nerr, xoffs, yoffs, zerrs );
            return true;
        }
        else {
            plot2d( xp, yp, z, istyle );
            return true;
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
        double dx = xoff_ + Math.round( x * scale_ );
        return (int) Math.max( (double) Integer.MIN_VALUE,
                               Math.min( (double) Integer.MAX_VALUE, dx ) );
    }

    /**
     * Determines the integer Y value in graphics space from a Y value
     * in normalised 3d space.
     *
     * @param   y  normalised space Y coordinate
     * @return  graphics space Y coordinate
     */
    public int projectY( double y ) {
        double dy = yoff_ - Math.round( y * scale_ );
        return (int) Math.max( (double) Integer.MIN_VALUE,
                               Math.min( (double) Integer.MAX_VALUE, dy ) );
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
    protected abstract void plot2d( int px, int py, double z, int istyle );

    /**
     * Plots a marker and associated error values at a given point in 
     * graphics coordinates with given additional Z coordinates.
     * Points with greater Z values should obscure points
     * with lesser ones.  
     * The ordering of the error points is that required by the 
     * {@link ErrorRenderer} class.
     *
     * <p>Most implementations currently ignore the
     * Z values associated with the error points, and put everything at
     * the depth of the central point, because it's too hard to do otherwise.
     * Does this produce seriously confusing visualisation?
     *
     * @param  px  graphics space X coordinate of the central point
     * @param  py  graphics space Y coordinate of the central point
     * @param  z   depth of point
     * @param  istyle  index of the style used to plot the point
     * @param  showPoint  whether the central point is to be plotted
     * @param  nerr  number of error points
     * @param  xoffs   <code>nerr</code>-element array of graphics space 
     *                 X coordinates for error points
     * @param  yoffs   <code>nerr</code>-element array of graphics space
     *                 Y coordinates for error points
     * @param  zerrs   <code>nerr</code>-element array of depths for
     *                 error points
     */
    protected abstract void plot2d( int px, int py, double z, int istyle,
                                    boolean showPoint, int nerr, 
                                    int[] xoffs, int[] yoffs, double[] zerrs );

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
