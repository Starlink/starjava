package uk.ac.starlink.ttools.plot2.geom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot.Corner;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Plot3D;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.util.SplitCollector;

/**
 * Surface implementation for 3-d plotting.
 *
 * <p>Part of the surface definition involves a rotation matrix
 * by which the data positions are rotated before being plotted to
 * the screen.   If the rotation matrix is a unit matrix,
 * the data X coord is screen X increasing left to right (graphics positive),
 * the data Z coord is screen Y increasing bottom to top (graphics negative),
 * and the data Y coord is into the screen increasing front to back.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubeSurface implements Surface {

    private final int gxlo_;
    private final int gxhi_;
    private final int gylo_;
    private final int gyhi_;
    private final double[] dlos_;
    private final double[] dhis_;
    private final boolean[] logFlags_;
    private final boolean[] flipFlags_;
    private final double[] rotmat_;
    private final double zoom_;
    private final double xoff_;
    private final double yoff_;
    private final Tick[][] ticks_;
    private final String[] labels_;
    private final Captioner captioner_;
    private final boolean frame_;
    private final boolean antialias_;

    private final double gScale_;
    private final double gZoom_;
    private final int gXoff_;
    private final int gYoff_;
    private final double[] dScales_;
    private final double[] dOffs_;

    private static final Orientation ORIENTATION = Orientation.X;

    /**
     * Constructor.
     *
     * @param  gxlo   graphics X coordinate lower bound
     * @param  gxhi   graphics X coordinate upper bound
     * @param  gylo   graphics Y coordinate lower bound
     * @param  gyhi   graphics Y coordinate upper bound
     * @param  dlos   3-element array giving X,Y,Z data coordinate lower bounds
     * @param  dhis   3-element array giving X,Y,Z data coordinate upper bounds
     * @param  logFlags  3-element array flagging log scaling on X,Y,Z axis
     * @param  flipFlags 3-element array flagging axis inversion for X,Y,Z
     * @param  rotmat  9-element array giving graphics space rotation matrix
     * @param  zoom    zoom factor, 1 means cube roughly fills plot bounds
     * @param  xoff  graphics X offset in pixels, 0 means centred in plot bounds
     * @param  yoff  graphics Y offset in pixels, 0 means centred in plot bounds
     * @param  ticks  3-element array X,Y,Z tickmark arrays
     * @param  labels  3-element array of X,Y,Z axis label strings
     * @param  captioner  text renderer
     * @param  frame  whether to draw wire frame
     * @param  antialias  whether to antialias grid lines
     */
    public CubeSurface( int gxlo, int gxhi, int gylo, int gyhi,
                        double[] dlos, double[] dhis,
                        boolean[] logFlags, boolean[] flipFlags,
                        double[] rotmat, double zoom, double xoff, double yoff,
                        Tick[][] ticks, String[] labels, Captioner captioner,
                        boolean frame, boolean antialias ) {
        gxlo_ = gxlo;
        gxhi_ = gxhi;
        gylo_ = gylo;
        gyhi_ = gyhi;
        dlos_ = dlos.clone();
        dhis_ = dhis.clone();
        logFlags_ = logFlags.clone();
        flipFlags_ = flipFlags.clone();
        rotmat_ = rotmat.clone();
        zoom_ = zoom;
        xoff_ = xoff;
        yoff_ = yoff;
        ticks_ = ticks.clone();
        labels_ = labels.clone();
        captioner_ = captioner;
        frame_ = frame;
        antialias_ = antialias;

        /* Prepare precalculated values that will come in useful. */
        gScale_ = getPixelScale( gxhi - gxlo, gyhi - gylo );
        gZoom_ = zoom_ * gScale_ / 2;
        gXoff_ = gxlo_ + (int) ( xoff_ * gScale_ ) + ( gxhi_ - gxlo_ ) / 2;
        gYoff_ = gylo_ + (int) ( yoff_ * gScale_ ) + ( gyhi_ - gylo_ ) / 2;
        dOffs_ = new double[ 3 ];
        dScales_ = new double[ 3 ];
        for ( int id = 0; id < 3; id++ ) {
            boolean logFlag = logFlags_[ id ];
            double flipMult = flipFlags_[ id ] ? -1 : +1;
            boolean flipFlag = flipFlags_[ id ];
            double dlo = logFlag ? Math.log( dlos_[ id ] ) : dlos_[ id ];
            double dhi = logFlag ? Math.log( dhis_[ id ] ) : dhis_[ id ];
            dOffs_[ id ] = - ( dlo + dhi ) / 2;
            dScales_[ id ] = flipMult / ( dhi - dlo ) * 2;
            assert PlotUtil.approxEquals( -flipMult, normalise( dlos_, id ) );
            assert PlotUtil.approxEquals( +flipMult, normalise( dhis_, id ) );
        }
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public Rectangle getPlotBounds() {
        return new Rectangle( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
    }

    public Surround getSurround( boolean withScroll ) {
        return new Surround();
    }

    public Captioner getCaptioner() {
        return captioner_;
    }

    public boolean dataToGraphics( double[] dataPos, boolean visibleOnly,
                                   Point2D.Double gPos ) {
        return dataToGraphics3D( dataPos, visibleOnly, gPos, false );
    }

    public boolean dataToGraphicsOffset( double[] dataPos0,
                                         Point2D.Double gPos0,
                                         double[] dataPos1, boolean visibleOnly,
                                         Point2D.Double gpos1 ) {
        return dataToGraphics( dataPos1, visibleOnly, gpos1 );
    }

    /**
     * Returns graphics position plus Z coordinate for a data point.
     *
     * @param  dataPos  3-element X,Y,Z position in data coordinates
     * @param  visibleOnly  true if only data points that will be visible
     *                      on this surface are of interest
     * @param  gPos  the 3-d graphics position will be written into this point
     *               on success
     * @return  true  iff the conversion was successful
     * @see   #dataToGraphics
     */
    public boolean dataToGraphicZ( double[] dataPos, boolean visibleOnly,
                                   GPoint3D gPos ) {
        return dataToGraphics3D( dataPos, visibleOnly, gPos, true );
    }

    /**
     * Do the work for converting data to 3d graphics coordinates.
     *
     * @param  dataPos  3-element X,Y,Z position in data coordinates
     * @param  visibleOnly  true if only data points that will be visible
     *                      on this surface are of interest
     * @param  gPos   the graphics position will be written into this point
     *                on success
     * @param  is3d   if true, then gPos must be a GPoint3D instance,
     *                and the z coordinate will be written into it;
     *                if false the z coordinate will be discarded
     * @return  true  iff the conversion was successful
     */
    private boolean dataToGraphics3D( double[] dataPos, boolean visibleOnly,
                                      Point2D.Double gPos, boolean is3d ) {

        /* Determine whether the given data position is in the data range. */
        final boolean knownInCube;
        if ( visibleOnly ) {
            if ( inRange( dataPos ) ) {
                knownInCube = true;
            }
            else {
                return false;
            }
        }
        else {
            knownInCube = false;
        }

        /* Normalise the data coordinates to the range -1..+1. */
        double sx = normalise( dataPos, 0 );
        double sy = normalise( dataPos, 1 );
        double sz = normalise( dataPos, 2 );
        assert ( ! knownInCube )
               || ( isNormal( sx ) && isNormal( sy ) && isNormal( sz ) )
             : "(" + sx + ", " + sy + ", " + sz + ")";

        /* Apply current aspect rotation matrix. */
        double[] rot = rotmat_;
        double rx = rot[ 0 ] * sx + rot[ 1 ] * sy + rot[ 2 ] * sz;
        double ry = rot[ 3 ] * sx + rot[ 4 ] * sy + rot[ 5 ] * sz;
        double rz = rot[ 6 ] * sx + rot[ 7 ] * sy + rot[ 8 ] * sz;

        /* Apply graphics coordinates zoom and X/Y offsets,
         * determine success, and return. */
        double gx = gXoff_ + rx * gZoom_;
        double gy = gYoff_ - rz * gZoom_;
        if ( ! visibleOnly ||
             ( gx >= gxlo_ && gx < gxhi_ && gy >= gylo_ && gy < gyhi_ ) ) {
            gPos.x = gx;
            gPos.y = gy;
            if ( is3d ) {
                ((GPoint3D) gPos).z = ry;
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Converts normalised 3d coordinates to a graphics position plus Z
     * coordinate.  The normalised positions are as returned by
     * {@link #normalise normalise}.  If coordinates
     * outside of the normalised range (-1,1) are submitted, the output
     * position will be outside the visible cube.
     *
     * @param   sx   normalised X coordinate
     * @param   sy   normalised Y coordinate
     * @param   sz   normalised Z coordinate
     * @param  gPos  the graphics position will be written into this point
     */
    public void normalisedToGraphicZ( double sx, double sy, double sz,
                                      GPoint3D gPos ) {

        /* Apply current aspect rotation matrix. */
        double[] rot = rotmat_;
        double rx = rot[ 0 ] * sx + rot[ 1 ] * sy + rot[ 2 ] * sz;
        double ry = rot[ 3 ] * sx + rot[ 4 ] * sy + rot[ 5 ] * sz;
        double rz = rot[ 6 ] * sx + rot[ 7 ] * sy + rot[ 8 ] * sz;

        /* Apply graphics coordinates zoom and X/Y offsets,
         * determine success, and return. */
        double gx = gXoff_ + rx * gZoom_;
        double gy = gYoff_ - rz * gZoom_;
        double dz = ry;
        gPos.x = gx;
        gPos.y = gy;
        gPos.z = dz;
    }

    /** 
     * Determines whether a given data position is within the data space
     * cube represented by this surface.
     *
     * @param  dataPos  3-element array of non-normalised data coordinates
     * @return   true if it falls within this surface's data bounds
     */
    public boolean inRange( double[] dataPos ) {
        for ( int i = 0; i < 3; i++ ) {
            double d = dataPos[ i ];
            if ( ! ( dlos_[ i ] <= d && d <= dhis_[ i ] ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the data range boundaries in a specified dimension.
     *
     * @param  idim  dimension index (0..2)
     * @return   2-element array giving (lower,upper) limits in data coords
     *           in the specified dimension
     */
    public double[] getDataLimits( int idim ) {
        return new double[] { dlos_[ idim ], dhis_[ idim ] };
    }

    /**
     * Indicates the scaling along the three axes.
     *
     * @return  3-element array giving X, Y, Z scaling flags:
     *          false for linear, true for logarithmic
     */
    public boolean[] getLogFlags() {
        return logFlags_;
    }

    /**
     * Indicates which axes are reversed.
     *
     * @return  3-element array giving X, Y, Z flip flags;
     *          true to invert normal plot direction
     */
    public boolean[] getFlipFlags() {
        return flipFlags_;
    }

    /**
     * Maps a data space coordinate to a normalised space coordinate.
     * Normalised coordinates are in the range -1..+1.
     *
     * @param   dataPos  3-element data space coordinate array
     * @param   idim   index of dimension to convert (0, 1 or 2)
     * @return  normalised coordinate
     */
    public double normalise( double[] dataPos, int idim ) {
        return dScales_[ idim ]
             * ( dOffs_[ idim ]
                   + ( logFlags_[ idim ] ? Math.log( dataPos[ idim ] )
                                         : dataPos[ idim ] ) );
    }

    /**
     * Maps a normalised space coordinate to a data space coordinate.
     * Normalised coordinates are in the range -1..+1.
     *
     * @param  dataPos  3-element normalised space coordinate array
     * @param  idim    index of dimension to convert (0, 1 or 2)
     * @return  data space coordinate
     */
    private double unNormalise( double[] normPos, int idim ) {
        double x = normPos[ idim ] / dScales_[ idim ] - dOffs_[ idim ];
        return logFlags_[ idim ] ? Math.exp( x ) : x;
    }

    /**
     * Indicates whether a value is in the normalised range.
     * 
     * @param  d  value to check
     * @return  true  iff d is near (or close to) the range -1..+1
     */
    private boolean isNormal( double d ) {
        return d >= -1.0001 && d <= +1.0001;
    }

    /**
     * Determines the graphics position to which a normalised space
     * point will map.
     * This method is not maximally fast (it creates a Point object)
     * so should not be used in a tight loop.
     *
     * @param   nPos  3-element coordinate array in normalised space
     * @return  graphics position
     */
    public Point2D.Double projectNormalisedPos( double[] nPos ) {
        double[] r = Matrices.mvMult( rotmat_, nPos );
        return new Point2D.Double( gXoff_ + r[ 0 ] * gZoom_,
                                   gYoff_ - r[ 2 ] * gZoom_ );
    }

    /**
     * Returns null.
     * At time of writing this method is not called for CubeSurface.
     */
    public String formatPosition( double[] dataPos ) {
        return null;
    }

    /**
     * Only works if a point iterator is supplied, because of degeneracy
     * in mapping a cube to a plane.  If we have a point iterator, then
     * the Z coordinate is determined as the average Z coordinate of
     * all data points that fall near to the indicated graphics position.
     */
    public double[] graphicsToData( Point2D gpos0,
                                    Supplier<CoordSequence> dposSupplier ) {

        /* We can only work out the position if there are data points supplied,
         * since the third dimension means the 2d->3d mapping is degenerate. */
        if ( dposSupplier == null ) {
            return null;
        }
        else {

            /* First, set up a list of proximity thresholds.  Points within
             * these number of pixels will be searched for.   If any are found
             * within the smaller threshold, the average of those is used;
             * if not, try the next smaller etc. */
            int[] thresh1s = { 2, 4, 8, 16 };
            Arrays.sort( thresh1s );

            /* Go through all the points and find the mean of ones
             * near the test position. */
            NeighbourCollector collector =
                new NeighbourCollector( this, gpos0, thresh1s );
            NeighbourData ndata =
                PlotUtil.COORD_RUNNER.collect( collector, dposSupplier );
            return collector.getMeanPosition( ndata );
        }
    }

    public boolean isContinuousLine( double[] dpos0, double[] dpos1 ) {
        return true;
    }

    public void paintBackground( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g.create();
        Color color0 = g2.getColor();
        g2.setColor( Color.WHITE );
        g2.fillRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
        g2.setColor( color0 );

        /* Paint those parts of the wire frame that are known to fall behind
         * all the data points. */
        if ( frame_ ) {
            g2.clipRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 antialias_
                                     ? RenderingHints.VALUE_ANTIALIAS_ON
                                     : RenderingHints.VALUE_ANTIALIAS_OFF );
            plotFrame( g2, false );
        }
    }

    public void paintForeground( Graphics g ) {

        /* Paint those parts of the wire frame that are known to fall in
         * front of all the data points. */
        if ( frame_ ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.clipRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 antialias_
                                     ? RenderingHints.VALUE_ANTIALIAS_ON
                                     : RenderingHints.VALUE_ANTIALIAS_OFF );
            plotFrame( g2, true );
        }
    }

    /**
     * Returns approximate config to recreate this surface's aspect.
     *
     * @param  isIso  true for isotropic mode, false for anisotropic
     * @return  approximate aspect config
     */
    ConfigMap getAspectConfig( boolean isIso ) {
        int npix = Math.max( gxhi_ - gxlo_, gyhi_ - gylo_ );
        ConfigMap config = new ConfigMap();
        double xlo = dlos_[ 0 ];
        double ylo = dlos_[ 1 ];
        double zlo = dlos_[ 2 ];
        double xhi = dhis_[ 0 ];
        double yhi = dhis_[ 1 ];
        double zhi = dhis_[ 2 ];
        if ( isIso ) {
            double xr = xhi - xlo;
            double yr = yhi - ylo;
            double zr = zhi - zlo;
            config.put( CubeSurfaceFactory.XC_KEY,
                        PlotUtil.roundNumber( .5 * ( xlo + xhi ), xr / npix ) );
            config.put( CubeSurfaceFactory.YC_KEY,
                        PlotUtil.roundNumber( .5 * ( ylo + yhi ), yr / npix ) );
            config.put( CubeSurfaceFactory.ZC_KEY,
                        PlotUtil.roundNumber( .5 * ( zlo + zhi ), zr / npix ) );
            config.put( CubeSurfaceFactory.SCALE_KEY, ( xr + yr + zr ) / 3.0 );
        }
        else {
            config.putAll( PlotUtil
                          .configLimits( CubeSurfaceFactory.XMIN_KEY,
                                         CubeSurfaceFactory.XMAX_KEY,
                                         xlo, xhi, npix ) );
            config.putAll( PlotUtil
                          .configLimits( CubeSurfaceFactory.YMIN_KEY,
                                         CubeSurfaceFactory.YMAX_KEY,
                                         ylo, yhi, npix ) );
            config.putAll( PlotUtil
                          .configLimits( CubeSurfaceFactory.ZMIN_KEY,
                                         CubeSurfaceFactory.ZMAX_KEY,
                                         zlo, zhi, npix ) );
        }
        config.put( CubeSurfaceFactory.ZOOM_KEY, new Double( zoom_ ) );
        config.put( CubeSurfaceFactory.XOFF_KEY, new Double( xoff_ ) );
        config.put( CubeSurfaceFactory.YOFF_KEY, new Double( yoff_ ) );
        double[] eulers = CubeSurfaceFactory.rotationToEulerDegrees( rotmat_ );
        double degEpsilon = 0.01;
        config.put( CubeSurfaceFactory.PHI_KEY,
                    PlotUtil.roundNumber( eulers[ 0 ], degEpsilon ) );
        config.put( CubeSurfaceFactory.THETA_KEY,
                    PlotUtil.roundNumber( eulers[ 1 ], degEpsilon ) );
        config.put( CubeSurfaceFactory.PSI_KEY,
                    PlotUtil.roundNumber( eulers[ 2 ], degEpsilon ) );
        return config;
    }

    /**
     * Returns a cube surface like this one but rotated
     * according to the difference between two screen positions.
     * This is intended for use as the result of a pan/drag mouse gesture.
     *
     * @param  pos0  start position
     * @param  pos1  end position
     * @return   new cube
     */
    CubeAspect pan( Point2D pos0, Point2D pos1 ) {
        double xf = ( pos0.getX() - pos1.getX() ) / gScale_ / zoom_;
        double yf = ( pos0.getY() - pos1.getY() ) / gScale_ / zoom_;
        double phi = xf * Math.PI / 2;
        double psi = yf * Math.PI / 2;
        double[] rot = rotateXZ( rotmat_, phi, psi );
        return adjustAspect( rot, zoom_, xoff_, yoff_ );
    }

    /**
     * Returns a cube surface like this one but zoomed about its centre
     * in some or all dimensions by a given factor.
     *
     * @param  factor  zoom factor
     * @param   useFlags  3-element array of flags indicating whether
     *                    to zoom in X, Y, Z directions
     * @return   new cube
     */
    CubeAspect centerZoom( double factor, boolean[] useFlags ) {
        double[] midPos = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            midPos[ i ] = logFlags_[ i ]
                        ? Math.sqrt( dlos_[ i ] * dhis_[ i ] )
                        : ( dlos_[ i ] + dhis_[ i ] ) / 2.0;
        }
        return zoomData( midPos, useFlags[ 0 ] ? factor : 1,
                                 useFlags[ 1 ] ? factor : 1,
                                 useFlags[ 2 ] ? factor : 1 );
    }

    /**
     * Returns a cube surface like this one but zoomed in two dimensions
     * around a point indicated by a given screen position.
     * The two dimensions are those closest to the plane of the screen.
     *
     * @param   pos  reference point in graphics coordinates
     * @param   xZoom  zoom factor requested in X screen direction
     * @param   yZoom  zoom factor requested in Y screen direction
     * @return   new cube
     */
    CubeAspect pointZoom( Point2D gpos, double xZoom, double yZoom ) {
        int[] dirs = getScreenDirections();
        double[] factors = new double[] { 1, 1, 1 };
        factors[ dirs[ 0 ] ] = xZoom;
        factors[ dirs[ 1 ] ] = yZoom;
        return zoomData( graphicsToData( gpos ),
                         factors[ 0 ], factors[ 1 ], factors[ 2 ] );
    }

    /**
     * Returns a cube surface like this one but panned in two dimensions
     * given a screen start and end position.
     * The two dimensions are those closest to the plane of the screen.
     *
     * @param  gpos0  start point in graphics coordinates
     * @param  gpos1  end point in graphics coordinates
     * @return  new cube
     */
    CubeAspect pointPan( Point2D gpos0, Point gpos1 ) {
        double[] dp0 = graphicsToData( gpos0 );
        double[] dp1 = graphicsToData( gpos1 );
        double[][] limits = new double[ 3 ][];
        for ( int i = 0; i < 3; i++ ) {
            limits[ i ] = Axis.pan( dlos_[ i ], dhis_[ i ], dp0[ i ], dp1[ i ],
                                    logFlags_[ i ] );
        }
        return new CubeAspect( limits[ 0 ], limits[ 1 ], limits[ 2 ],
                               rotmat_, zoom_, xoff_, yoff_ );
    }

    /**
     * Identifies which data space axes are closest to the screen
     * horizontal, vertical and normal directions in the current state
     * of rotation.
     *
     * @return  3-element array, a permutation of the values 0,1,2;
     *          elements are indices of screen {horizontal, vertical, normal}
     *          axes respectively
     */
    public int[] getScreenDirections() {
        double[] screenXs = new double[ 3 ];
        double[] screenYs = new double[ 3 ];
        double[] screenNs = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            double[] r = Matrices.mvMult( rotmat_, Matrices.unit( i ) );
            screenXs[ i ] = r[ 0 ];
            screenYs[ i ] = r[ 2 ];
            screenNs[ i ] = r[ 1 ];
        }
        final int iaxX;
        {
            double maxX = 0;
            int imaxX = -1;
            for ( int i = 0; i < 3; i++ ) {
                if ( Math.abs( screenXs[ i ] ) > Math.abs( maxX ) ) {
                    maxX = screenXs[ i ];
                    imaxX = i;
                }
            }
            iaxX = imaxX;
        }
        final int iaxY;
        {
            double maxY = 0;
            int imaxY = -1;
            for ( int i = 0; i < 3; i++ ) {
                if ( Math.abs( screenYs[ i ] ) > Math.abs( maxY ) &&
                     i != iaxX ) {
                    maxY = screenYs[ i ];
                    imaxY = i;
                }
            }
            iaxY = imaxY;
        }
        final int iaxN;
        {
            double maxN = 0;
            int imaxN = -1;
            for ( int i = 0; i < 3; i++ ) {
                if ( i != iaxX && i != iaxY ) {
                    maxN = screenNs[ i ];
                    imaxN = i;
                }
            }
            iaxN = imaxN;
        }
        return new int[] { iaxX, iaxY, iaxN };
    }

    /**
     * Attempts to return a data space point corresponding to a graphics
     * position.  This is underdetermined because the graphics position
     * is 2d while the data space is 3d, so the graphics position can only
     * unambigously indicate a line of sight.
     * Try to pick something sensible.
     *
     * @param  gpos   graphics position
     * @return  corresponding data space position
     */
    private double[] graphicsToData( Point2D gpos ) {

        /* Work out the unit vectors in normalised space for the two
         * axes defining the cube face that is most nearly facing
         * towards the viewer.  For the other (most nearly screen normal)
         * direction, take the point in the middle of normalised space,
         * i.e. at the origin. */
        int[] dirs = getScreenDirections();
        int iscreenX = dirs[ 0 ];
        int iscreenY = dirs[ 1 ];
        int iscreenN = dirs[ 2 ];
        double[] normOrigin = new double[ 3 ];
        double[] normX1 = normOrigin.clone();
        double[] normY1 = normOrigin.clone();
        normX1[ iscreenX ] = 1;
        normY1[ iscreenY ] = 1;

        /* Get the positions of the origin and unit vector ends in graphics
         * space. */
        Point2D.Double g0 = projectNormalisedPos( normOrigin );
        Point2D.Double gX = projectNormalisedPos( normX1 );
        Point2D.Double gY = projectNormalisedPos( normY1 );

        /* Compare these with the given position in graphics space,
         * to give projections along the facing horizontal and vertical
         * data axes, and convert the results back to normalised space. */
        double[] normPos = new double[ 3 ];
        normPos[ iscreenN ] = 0;
        normPos[ iscreenX ] = projectGraphicsToNormalised( g0, gX, gpos );
        normPos[ iscreenY ] = projectGraphicsToNormalised( g0, gY, gpos );

        /* Convert from normalised space to data space and return. */
        return new double[] {
            unNormalise( normPos, 0 ),
            unNormalise( normPos, 1 ),
            unNormalise( normPos, 2 ),
        };
    }

    /**
     * Determines the projection of a given point along a unit vector,
     * returning the result as a normalised value.
     *
     * @param  origin   graphics position of origin
     * @param  unit     graphics position of end of unit vector
     * @param  point    graphics position of point
     * @return  normalised projection of point-origin along unit-origin
     */
    private double projectGraphicsToNormalised( Point2D origin, Point2D unit,
                                                Point2D point ) {
        double dux = unit.getX() - origin.getX();
        double duy = unit.getY() - origin.getY();
        double dpx = point.getX() - origin.getX();
        double dpy = point.getY() - origin.getY();
        return ( dpx * dux + dpy * duy ) / ( dux * dux + duy * duy );
    }

    /**
     * Returns a cube surface like this one but translated so that the
     * given data space point is at its visual centre.
     *
     * @param  dpos  data coordinates of position to centre
     * @return  new cube
     */
    CubeAspect center( double[] dpos ) {
        double[][] limits = new double[ 3 ][];
        for ( int i = 0; i < 3; i++ ) {
            double dp = dpos[ i ];
            double dlo = dlos_[ i ];
            double dhi = dhis_[ i ];
            final double min;
            final double max;
            if ( logFlags_[ i ] ) {
                double dmid = Math.sqrt( dlo * dhi );
                double offset = dp / dmid;
                min = dlo * offset;
                max = dhi * offset;
            }
            else {
                double dmid = 0.5 * ( dlo + dhi );
                double offset = dp - dmid;
                min = dlo + offset;
                max = dhi + offset;
            }
            limits[ i ] = new double[] { min, max };
        }
        return new CubeAspect( limits[ 0 ], limits[ 1 ], limits[ 2 ],
                               rotmat_, zoom_, xoff_, yoff_ );
    }

    /**
     * Returns a cube surface like this one but zoomed around a given
     * data position by a given factor.
     *
     * @param  dpos0  zoom centre in data coordinates
     * @param   xFactor  factor to zoom in X direction
     * @param   yFactor  factor to zoom in Y direction
     * @param   zFactor  factor to zoom in Z direction
     * @return   new cube aspect
     */
    private CubeAspect zoomData( double[] dpos0, double xFactor,
                                 double yFactor, double zFactor ) {
        double[] factors = new double[] { xFactor, yFactor, zFactor };
        double[][] limits = new double[ 3 ][];
        for ( int i = 0; i < 3; i++ ) {
            limits[ i ] = Axis.zoom( dlos_[ i ], dhis_[ i ],
                                     dpos0[ i ], factors[ i ], logFlags_[ i ] );
        }
        return new CubeAspect( limits[ 0 ], limits[ 1 ], limits[ 2 ],
                               rotmat_, zoom_, xoff_, yoff_ );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof CubeSurface ) {
            CubeSurface other = (CubeSurface) o;
            return this.gxlo_ == other.gxlo_
                && this.gxhi_ == other.gxhi_
                && this.gylo_ == other.gylo_
                && this.gyhi_ == other.gyhi_
                && Arrays.equals( this.dlos_, other.dlos_ )
                && Arrays.equals( this.dhis_, other.dhis_ )
                && Arrays.equals( this.logFlags_, other.logFlags_ )
                && Arrays.equals( this.flipFlags_, other.flipFlags_ )
                && this.zoom_ == other.zoom_
                && this.xoff_ == other.xoff_
                && this.yoff_ == other.yoff_
                && Arrays.equals( this.rotmat_, other.rotmat_ )
                && Arrays.deepEquals( this.ticks_, other.ticks_ )
                && Arrays.equals( this.labels_, other.labels_ )
                && this.captioner_.equals( other.captioner_ )
                && this.frame_ == other.frame_
                && this.antialias_ == other.antialias_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 7701;
        code = 23 * code + gxlo_;
        code = 23 * code + gxhi_;
        code = 23 * code + gylo_;
        code = 23 * code + gyhi_;
        code = 23 * code + Arrays.hashCode( dlos_ );
        code = 23 * code + Arrays.hashCode( dhis_ );
        code = 23 * code + Arrays.hashCode( logFlags_ );
        code = 23 * code + Arrays.hashCode( flipFlags_ );
        code = 23 * code + Float.floatToIntBits( (float) zoom_ );
        code = 23 * code + Float.floatToIntBits( (float) xoff_ );
        code = 23 * code + Float.floatToIntBits( (float) yoff_ );
        code = 23 * code + Arrays.hashCode( rotmat_ );
        code = 23 * code + Arrays.deepHashCode( ticks_ );
        code = 23 * code + Arrays.hashCode( labels_ );
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + ( frame_ ? 1 : 3 );
        code = 23 * code + ( antialias_ ? 5 : 7 );
        return code;
    }

    /**
     * Returns a cube surface like this one but with adjusted view parameters.
     *
     * @param  rotmat  new rotation matrix
     * @param  zoom   new zoom factor
     * @param  xoff  new graphics X offset
     * @param  yoff  new graphics Y offset
     * @return  new cube
     */
    private CubeAspect adjustAspect( double[] rotmat, double zoom,
                                     double xoff, double yoff ) {
        return new CubeAspect( new double[] { dlos_[ 0 ], dhis_[ 0 ] },
                               new double[] { dlos_[ 1 ], dhis_[ 1 ] },
                               new double[] { dlos_[ 2 ], dhis_[ 2 ] },
                               rotmat, zoom, xoff, yoff );
    }

    /**
     * Implements plotting of the wire frame bounding the data volume
     * represented by this surface.
     * This is done in two passes: a single call of this method
     * will draw either the back or the front part, not both.
     * The back is the part known to fall behind all data points
     * contained in the volume.  The front is the part known to fall
     * in front of all the data points contained in the volume.
     * Fortunately, put together that constitutes all the drawing we
     * need to do.
     *
     * @param   g  graphics context
     * @param   front  true to draw front part, false to draw back part
     */
    private void plotFrame( Graphics g, boolean front ) {

        /* Prepare workspace. */
        GPoint3D gp0 = new GPoint3D();
        GPoint3D gp1 = new GPoint3D();

        /* Identify the corner furthest away from the front.
         * The three edges that hit this corner will be the ones
         * obscured by the body of the cube. */
        Corner backCorner = null;
        double zmax = 0;
        for ( int ic = 0; ic < 8; ic++ ) {
            Corner corner = Corner.getCorner( ic );
            double[] dpos0 = getCornerDataPos( corner );
            dataToGraphicZ( dpos0, false, gp0 );
            if ( gp0.z > zmax ) {
                zmax = gp0.z;
                backCorner = corner;
            }
        }

        /* Set up graphics for drawing axes. */
        Graphics2D g2 = (Graphics2D) g;
        Stroke stroke0 = g2.getStroke();
        Color color0 = g2.getColor();
        float sWidth = stroke0 instanceof BasicStroke
                     ? ((BasicStroke) stroke0).getLineWidth()
                     : 1f;
        g2.setStroke( new BasicStroke( sWidth, BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND ) );
        g.setColor( front ? Color.BLACK : Color.LIGHT_GRAY );

        /* Iterate over each edge of the cube by doing a double loop
         * over the corners. */
        for ( int i0 = 0; i0 < 8; i0++ ) {
            Corner c0 = Corner.getCorner( i0 );
            double[] dpos0 = getCornerDataPos( c0 );
            Corner[] friends = c0.getAdjacent();
            for ( int i1 = 0; i1 < friends.length; i1++ ) {
                Corner c1 = friends[ i1 ];
                if ( c1.compareTo( c0 ) > 0 ) {
                    double[] dpos1 = getCornerDataPos( c1 );

                    /* Determine if the edge is included in this pass. */
                    boolean isHidden = c0 == backCorner || c1 == backCorner;
                    if ( isHidden ^ front ) {

                        /* If so draw the decorated or undecorated axis. */
                        assert c1 != Corner.ORIGIN;
                        if ( c0 == Corner.ORIGIN ) {
                            drawFrameAxis( g2, dpos0, dpos1 );
                        }
                        else {
                            drawFrameLine( g2, dpos0, dpos1 );
                        }
                    }
                }
            }
        }

        /* Restore graphics. */
        g2.setColor( color0 );
        g2.setStroke( stroke0 );
    }

    /**
     * Returns the data space coordinates of a given corner of the cube.
     *
     * @param  corner  corner object
     * @return  data space coordinates
     */
    private double[] getCornerDataPos( Corner corner ) {
        boolean[] cflags = corner.getFlags();
        double[] dpos = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            dpos[ i ] = cflags[ i ] ^ flipFlags_[ i ] ? dhis_[ i ] : dlos_[ i ];
        }
        return dpos;
    }

    /**
     * Draws a decorated axis between two data space positions.
     * The identity of the axis is worked out by looking at the positions
     * themselves.
     *
     * @param   g   graphics context
     * @param  dpos0   data space coordinates of one corner
     * @param  dpos1   data space coordinates of the other corner
     */
    private void drawFrameAxis( Graphics g, double[] dpos0, double[] dpos1 ) {

        /* Which axis are we looking at? */
        int iaxis = -1;
        for ( int i = 0; i < 3; i++ ) {
            if ( dpos0[ i ] != dpos1[ i ] ) {
                assert iaxis == -1;
                iaxis = i;
            }
        }
        assert iaxis >= 0 && iaxis < 3;

        /* Which way is up?  We need to decide on a unit vector which defines
         * the plane in which text will be written.  The direction must
         * be perpendicular to the axis but this leaves one degree of
         * freedom.  We want to choose it so that the up vector has a
         * zero component in the direction which will be perpendicular
         * to the viewing plane.  This is still undetermined to a factor
         * of -1; we may revise this later if we find out our choice
         * has given us upside-down text. */
        double[] up = Matrices.normalise( Matrices.cross( getDepthVector(),
                                          Matrices.unit( iaxis ) ) );
      
        /* Which way is forward?  Initially choose to write along the
         * axis with lower numbers at the left hand side, but we may
         * decide to revise this if it leads to inside out text. */
        boolean forward = true;

        /* Define a notional region on the graphics plane to which we
         * can plot text.  This is a rectangle based at the origin which
         * has the height of the current font and the width of the
         * relevant cube axis when it's viewed face on. */
        int sx = (int) gScale_;
        int sy = g.getFontMetrics().getHeight();
        Point2D.Double sp00 = new Point2D.Double( 0, 0 );
        Point2D.Double sp10 = new Point2D.Double( sx, 0 );
        Point2D.Double sp01 = new Point2D.Double( 0, sy );

        /* Construct a transform to apply to the graphics context which
         * allows you to write text in a normal font in the rectangle
         * (0,0)->(sx,sy) in such a way that it will appear as a label
         * on the current axis. */
        AffineTransform atf = null;
        for ( int itry = 0; atf == null; itry++ ) {

            /* There are a couple of conditions in the following algorithm
             * where a test is made, and if it fails an input is changed
             * and the loop runs again to get a better result.  In principle
             * this should always lead to the right result in a finite
             * number of iterations.  However there may be pathological
             * cases in which it leads to an infinite loop.  This flag
             * spots if it looks like this is happening and makes sure that
             * we just go with the possibly imperfect result we have rather
             * than hang the UI for ever.  Possibly a smarter algorithm
             * could guarantee to get this right first time. */
            boolean stopFiddling = itry > 5;

            /* Find the rectangle in normalised 3D space on which the
             * axis annotation should be written. */
            double[] n00 = new double[ 3 ];
            double[] n10 = new double[ 3 ];
            double[] n01 = new double[ 3 ];
            for ( int i = 0; i < 3; i++ ) {
                n00[ i ] = normalise( forward ? dpos0 : dpos1, i );
                n10[ i ] = normalise( forward ? dpos1 : dpos0, i );
            }
            double uscale = ( n10[ iaxis ] - n00[ iaxis ] ) * sy / sx;
            for ( int i = 0; i < 3; i++ ) {
                n01[ i ] = n00[ i ] + uscale * up[ i ];
            }

            /* Work out what rectangle on the graphics plane this 3d region
             * appears at. */
            Point2D.Double tp00 = projectNormalisedPos( n00 );
            Point2D.Double tp10 = projectNormalisedPos( n10 );
            Point2D.Double tp01 = projectNormalisedPos( n01 );

            /* See if the text is upside down.  If so, invert the up vector
             * and try again. */
            if ( tp01.y < tp00.y && ! stopFiddling ) {
                up = Matrices.mult( up, -1 );
            }
            else {

                /* Set up coefficients for an affine transform. */
                double[] sm = new double[] { sp00.x, sp10.x, sp01.x,
                                             sp00.y, sp10.y, sp01.y,
                                                  1,      1,      1, };
                double[] tm = new double[] { tp00.x, tp10.x, tp01.x,
                                             tp00.y, tp10.y, tp01.y,
                                                  1,      1,      1, };
                double[] m = Matrices.mmMult( tm, Matrices.invert( sm ) );
                double m00 = m[ 0 ];
                double m01 = m[ 1 ];
                double m02 = m[ 2 ];
                double m10 = m[ 3 ];
                double m11 = m[ 4 ];
                double m12 = m[ 5 ];
                assert PlotUtil.approxEquals( m[ 6 ], 0 ) : m[ 6 ];
                assert PlotUtil.approxEquals( m[ 7 ], 0 ) : m[ 7 ];
                assert PlotUtil.approxEquals( m[ 8 ], 1 ) : m[ 8 ];

                /* See if the text is inside out.  If so, flip the sense
                 * and try again. */
                if ( m00 * m11 - m01 * m10 < 0 && ! stopFiddling ) {
                    forward = ! forward;
                }

                /* If we've got this far our coefficients are going to
                 * give us text in the correct orientation.  Construct
                 * the transform. */
                else {
                    atf = new AffineTransform( m00, m10, m01, m11, m02, m12 );
                }
            }
        }

        /* If the determinant of the transform is zero, bail out now.
         * Although we could do some of the plotting, you won't miss
         * much from a singular transformation and it can cause some of
         * the drawing methods to throw unpredictable errors. */
        double det = atf.getDeterminant();
        if ( det == 0 || Double.isNaN( det ) ) {
            return;
        }

        /* Apply the transform to the graphics context.  Subsequent text
         * written to the region (0,0)->(sx,sy) will appear alongside
         * the relevant axis now. */
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform atf0 = g2.getTransform();
        g2.transform( atf );

        /* Draw the annotated axis. */
        g2.drawLine( 0, 0, sx, 0 );
        Axis ax = Axis.createAxis( 0, sx, dlos_[ iaxis ], dhis_[ iaxis ],
                                   logFlags_[ iaxis ],
                                   ( ! forward ) ^ flipFlags_[ iaxis ] );
        ax.drawLabels( ticks_[ iaxis ], labels_[ iaxis ],
                       captioner_, ORIENTATION, false, g2 );
        g2.setTransform( atf0 );
    }

    /**
     * Draws an undecorated line between two data space positions.
     *
     * @param   g   graphics context
     * @param  dpos0   data space coordinates of line start
     * @param  dpos1   data space coordinates of line end
     */
    private void drawFrameLine( Graphics g, double[] dpos0, double[] dpos1 ) {
        GPoint3D gp0 = new GPoint3D();
        GPoint3D gp1 = new GPoint3D();
        dataToGraphicZ( dpos0, false, gp0 );
        dataToGraphicZ( dpos1, false, gp1 );
        g.drawLine( PlotUtil.ifloor( gp0.x ), PlotUtil.ifloor( gp0.y ),
                    PlotUtil.ifloor( gp1.x ), PlotUtil.ifloor( gp1.y ) );
    }

    /**
     * Returns a vector pointing into the screen.
     *
     * @return  depth vector
     */
    private double[] getDepthVector() {
        return Matrices.mvMult( Matrices.invert( rotmat_ ),
                                new double[] { 0, 1, 0 } );
    }

    /**
     * Utility method to create a CubeSurface from available requirements.
     * It works out the tickmarks and then invokes the constructor.
     *
     * @param  plotBounds  rectangle within which the plot should be drawn
     * @param  aspect   surface view configuration
     * @param  logFlags  3-element array flagging log scaling on X,Y,Z axis
     * @param  flipFlags 3-element array flagging axis inversion for X,Y,Z
     * @param  labels  3-element array of X,Y,Z axis label strings
     * @param  crowdFactors  3-element array giving tick mark crowding factors
     *                       for X,Y,Z axes; 1 is normal
     * @param  captioner  text renderer
     * @param  frame  whether to draw wire frame
     * @param  minor  whether to draw minor tickmarks
     * @param  antialias  whether to antialias grid lines
     * @return  new plot surface
     */
    public static CubeSurface createSurface( Rectangle plotBounds,
                                             CubeAspect aspect,
                                             boolean[] logFlags,
                                             boolean[] flipFlags,
                                             String[] labels,
                                             double[] crowdFactors,
                                             Captioner captioner,
                                             boolean frame,
                                             boolean minor,
                                             boolean antialias ) {
        int gxlo = plotBounds.x;
        int gxhi = plotBounds.x + plotBounds.width;
        int gylo = plotBounds.y;
        int gyhi = plotBounds.y + plotBounds.height;
        double[] dlos = new double[ 3 ];
        double[] dhis = new double[ 3 ];
        double[][] limits = aspect.getLimits();
        Tick[][] ticks = new Tick[ 3 ][];
        int npix = getPixelScale( plotBounds.width, plotBounds.height );
        for ( int i = 0; i < 3; i++ ) {
            dlos[ i ] = limits[ i ][ 0 ];
            dhis[ i ] = limits[ i ][ 1 ];
            ticks[ i ] = ( logFlags[ i ] ? BasicTicker.LOG
                                         : BasicTicker.LINEAR )
                        .getTicks( dlos[ i ], dhis[ i ], minor, captioner,
                                   ORIENTATION, npix, crowdFactors[ i ] );
        }
        double[] rotmat = aspect.getRotation();
        double zoom = aspect.getZoom();
        double xoff = aspect.getOffsetX();
        double yoff = aspect.getOffsetY();
        return new CubeSurface( gxlo, gxhi, gylo, gyhi, dlos, dhis,
                                logFlags, flipFlags, rotmat, zoom, xoff, yoff,
                                ticks, labels, captioner, frame, antialias );
    }

    /**
     * Returns a representative number of pixels per axis for given
     * screen dimensions.
     *
     * @param  xpix  plot bounds width
     * @param  ypix  plot bounds height
     * @return  rough maximum pixel count for a plot cube axis
     */
    private static int getPixelScale( int xpix, int ypix ) {
        return (int) ( Math.min( xpix, ypix ) / Math.sqrt( 3. ) );
    }

    /**
     * Rotates a matrix using angles corresponding to screen displacements.
     *
     * @param  base  input rotation matrix
     * @param  phi   screen-X-like angle
     * @param  psi   screen-Y-like angle
     * @return  output rotation matrix
     */
    private static double[] rotateXZ( double[] base, double phi, double psi ) {
        double[] rotA = Plot3D.rotate( base, new double[] { 0, 0, 1 }, phi );
        double[] rotB = Plot3D.rotate( base, new double[] { 1, 0, 0 }, psi );
        return Matrices.mmMult( Matrices.mmMult( base, rotB ), rotA );
    }

    /**
     * Utility class for aggregating information about groups of
     * positions in the proximity of each other.
     */
    private static class NeighbourData {

        final long[] counts_;        // number of points per group
        final double[][] dposTots_;  // running totals of x,y,z values per group

        /**
         * Constructor.
         *
         * @param  ngrp   number of groups of points
         */
        NeighbourData( int ngrp ) {
            counts_ = new long[ ngrp ];
            dposTots_ = new double[ ngrp ][ 3 ];
        }
    }

    /**
     * SplitCollector implementation for gathering information about
     * positions of nearby data points.
     */
    private static class NeighbourCollector
            implements SplitCollector<CoordSequence,NeighbourData> {

        private final CubeSurface surf_;
        private final Point2D gpos0_;
        private final int nthresh_;
        private final int[] thresh2s_;
        private final double maxThresh2_;
        private final boolean[] logFlags_;

        /**
         * Constructor.
         * The supplied list of thresholds gives different pixel distances
         * defining what counts as "near" over which different answers
         * will be accumulated.
         *
         * @param  surf  plot surface
         * @param  gpos0   test point; graphics positions near this are
         *                 accumulated while others are ignored
         * @param  thresh1s  pixel distance thresholds; this must be an 
         *                   array in ascending order defining "near"-ness
         */
        NeighbourCollector( CubeSurface surf, Point2D gpos0, int[] thresh1s ) {
            surf_ = surf;
            gpos0_ = gpos0;
            nthresh_ = thresh1s.length;
            logFlags_ = surf.logFlags_;

            /* Set up a lookup table of square distances for efficiency. */
            thresh2s_ = new int[ nthresh_ ];
            Arrays.setAll( thresh2s_, i -> thresh1s[ i ] * thresh1s[ i ] );
            maxThresh2_ = thresh2s_[ nthresh_ - 1 ];
        }

        public NeighbourData createAccumulator() {
            return new NeighbourData( nthresh_ );
        }

        public void accumulate( CoordSequence cseq, NeighbourData acc ) {
            double[] dpos = cseq.getCoords();
            double[] dp0 = new double[ 3 ];
            Point2D.Double gp = new Point2D.Double();
            while ( cseq.next() ) {
                if ( surf_.dataToGraphics( dpos, true, gp ) ) {
                    double d2 = gpos0_.distanceSq( gp );

                    /* If any are nearby. */
                    if ( d2 <= maxThresh2_ ) {

                        /* Work out position values that can be combined
                         * linearly (totalled and averaged). */
                        for ( int idim = 0; idim < 3; idim++ ) {
                            double d = dpos[ idim ];
                            dp0[ idim ] = logFlags_[ idim ] ? Math.log( d ) : d;
                        }

                        /* Accumulate for each threshold. */
                        for ( int ith = 0; ith < nthresh_; ith++ ) {
                            if ( d2 <= thresh2s_[ ith ] ) {
                                for ( int idim = 0; idim < 3; idim++ ) {
                                    acc.dposTots_[ ith ][ idim ] += dp0[ idim ];
                                }
                                acc.counts_[ ith ]++;
                            }
                        }
                    }
                }
            }
        }

        public NeighbourData combine( NeighbourData acc1, NeighbourData acc2 ) {
            for ( int ith = 0; ith < nthresh_; ith++ ) {
                acc1.counts_[ ith ] += acc2.counts_[ ith ];
                for ( int id = 0; id < 3; id++ ) {
                    acc1.dposTots_[ ith ][ id ] += acc2.dposTots_[ ith ][ id ];
                }
            }
            return acc1;
        }

        /**
         * Returns the 'best' average position of the near neighbours
         * accumulated into a NeighbourData object.
         *
         * @param  acc  accumulator produced by this collector
         */
        public double[] getMeanPosition( NeighbourData acc ) {

            /* Assess starting from the nearest threshold. */
            for ( int ith = 0; ith < nthresh_; ith++ ) {
                long count = acc.counts_[ ith ];

                /* If this one hit any nearby points, return the average. */
                if ( count > 0 ) {
                    double c1 = 1.0 / count;
                    double[] dposMean = new double[ 3 ];
                    for ( int id = 0; id < 3; id++ ) {
                        double d = c1 * acc.dposTots_[ ith ][ id ];
                        dposMean[ id ] = logFlags_[ id ] ? Math.exp( d ) : d;
                    }
                    return dposMean;
                }
            }

            /* If no data points within any threshold, return null. */
            return null;
        }
    }
}
