package uk.ac.starlink.ttools.plot;

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
    private static final float BYTE_SCALE = 255.99f;

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
     * @param   fogginess  thickness of fog for depth shading
     */
    protected PlotVolume( Component c, Graphics g, MarkStyle[] styles, 
                          double padFactor, int[] padBorders, 
                          double fogginess ) {
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
        fogger_.setFogginess( fogginess );
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
     * @param  label   label for point, or null
     * @param  nerr    the number of error points, or zero for no errors
     * @param  xerrs   <code>nerr</code>-element array of X coordinates of
     *                 error points 
     * @param  yerrs   <code>nerr</code>-element array of Y coordinates of
     *                 error points
     * @param  zerrs   <code>nerr</code>-element array of Z coordinates of
     *                 error points
     * @return  true iff the point was actually plotted
     */
    public boolean plot3d( double[] centre, int istyle,
                           boolean showPoint, String label, int nerr,
                           double[] xerrs, double[] yerrs, double[] zerrs ) {

        /* Return directly if there is no work to do. */
        if ( nerr == 0 && ! showPoint && label == null ) {
            return false;
        }

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
            plot2d( xp, yp, z, centre, istyle, showPoint, label,
                    nerr, xoffs, yoffs, zerrs );
            return true;
        }
        else {
            assert showPoint;
            plot2d( xp, yp, z, centre, istyle, true, label,
                    0, null, null, null );
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
     * Plots an marker and optional associated error values at a given 
     * point in graphics coordinates with given additional Z coordinates.
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
     * @param  z   depth of point; a point with a greater <code>z</code>
     *             should obscure a point with a lesser one
     * @param  coords  original coordinate array; as well as (redundant)
     *                 x,y,z values it may contain auxiliary axis coordinates
     * @param  istyle  index of the style used to plot the point
     * @param  showPoint  whether the central point is to be plotted
     * @param  label   label for point, or null
     * @param  nerr  number of error points, or zero for no errors
     * @param  xoffs   <code>nerr</code>-element array of graphics space 
     *                 X coordinates for error points
     * @param  yoffs   <code>nerr</code>-element array of graphics space
     *                 Y coordinates for error points
     * @param  zerrs   <code>nerr</code>-element array of depths for
     *                 error points
     */
    protected abstract void plot2d( int px, int py, double z, double[] coords,
                                    int istyle, boolean showPoint, String label,
                                    int nerr, int[] xoffs, int[] yoffs,
                                    double[] zerrs );

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

    public DataColorTweaker createFoggingTweaker( DataColorTweaker tweaker ) {
        Fogger fogger = getFogger();
        if ( fogger != null && fogger.getFogginess() > 0 ) {
            return tweaker == null
                 ? fogger.createTweaker( 2, 3 )
                 : fogger.createTweaker( 2, tweaker );
        }
        else {
            return tweaker;
        }
    }

    /**
     * Utility method to convert an RGBA float[] array into an integer.
     *
     * @param   rgba  float array
     * @return   integer
     */
    public static int packRgba( float[] rgba ) {
        return ( (int) ( rgba[ 0 ] * BYTE_SCALE ) & 0xff ) <<  0
             | ( (int) ( rgba[ 1 ] * BYTE_SCALE ) & 0xff ) <<  8
             | ( (int) ( rgba[ 2 ] * BYTE_SCALE ) & 0xff ) << 16
             | ( (int) ( rgba[ 3 ] * BYTE_SCALE ) & 0xff ) << 24;
    }

    /**
     * Utility method to convert an RGBA integer into a float[] array.
     *
     * @param  rgba  integer
     * @param  buf   4-element float array to receive result
     */
    public static void unpackRgba( int rgba, float[] buf ) {
        buf[ 0 ] = (float) ( ( rgba >>  0 ) & 0xff ) / 255f;
        buf[ 1 ] = (float) ( ( rgba >>  8 ) & 0xff ) / 255f;
        buf[ 2 ] = (float) ( ( rgba >> 16 ) & 0xff ) / 255f;
        buf[ 3 ] = (float) ( ( rgba >> 24 ) & 0xff ) / 255f;
    }
}
