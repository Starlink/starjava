package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.ttools.func.Arrays;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Sine (orthographic) projection.
 * This is the one that gives you a rotatable sphere.
 * North always faces directly up (is aligned along the screen Y direction).
 *
 * This is a singleton class, see {@link #INSTANCE}.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public class SinProjection extends SkyviewProjection {

    private static final double[] RX = new double[] { 1, 0, 0 };
    private static final double[] RY = new double[] { 0, 1, 0 };
    private static final double[] RZ = new double[] { 0, 0, 1 };
    private static final double MAX_RANGE_ZOOM = 1e7;

    /** Singleton instance. */
    public static SinProjection INSTANCE = new SinProjection();

    /**
     * Private singleton constructor.
     */
    private SinProjection() {
        super( new Sin2(), new Ellipse2D.Double( -1, -1, 2, 2 ), "Sin",
               "rotatable sphere" );
    }

    /**
     * Overridden for slight efficiency gain.
     */
    @Override
    public boolean project( double rx, double ry, double rz,
                            Point2D.Double pos ) {
        if ( rx >= 0 ) {
            pos.x = ry;
            pos.y = rz;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isContinuous() {
        return true;
    }

    public boolean isContinuousLine( double[] r3a, double[] r3b ) {
        return true;
    }

    public double[] cursorRotate( double[] rot0, Point2D.Double pos0,
                                                 Point2D.Double pos1 ) {

        /* Attempt the rotation that transforms a point from one
         * projected plane position to another. */
        double[] rot1 = genericRotate( rot0, pos0, pos1 );
        if ( rot1 != null ) {
            return rot1;
        }

        /* That may fail because one or other of the supplied points is
         * not in the projection region.  In that case do something
         * that feels like dragging the sphere around.
         * This rotation could be improved.  It is algebraically messy,
         * and it also does not transition smoothly from the genericRotate
         * case, though perhaps that's not possible in general. */
        else {
            boolean reflect = isReflected( rot0 );
            double fr = reflect ? -1 : +1;
            double phi = ( pos1.x - pos0.x );
            double psi = ( pos1.y - pos0.y );
            double[] rm = rot0;
            double[] sightvec = Matrices.mvMult( Matrices.invert( rm ),
                                                 new double[] { 1, 0, 0 } );
            double[] hvec = Matrices.normalise( Matrices.cross( sightvec,
                                                                RZ ) );
            rm = rotateAround( rm, hvec, -psi );
            rm = rotateAround( rm, RZ, -phi * fr );
            if ( Matrices.mvMult( rm, RZ )[ 2 ] >= 0 ) {
                return rm;
            }
            else {
                double delta = Math.atan2( -rm[ 2 ], rm[ 8 ] );
                double alpha = Math.atan2( -rm[ 3 ], rm[ 4 ] * fr );
                delta = Math.min( +0.5 * Math.PI, delta );
                delta = Math.max( -0.5 * Math.PI, delta );
                return verticalRotate( delta, alpha, reflect );
            }
        }
    }

    public double[] projRotate( double[] rot0, Point2D.Double pos0,
                                               Point2D.Double pos1 ) {
        double[] rot1 = genericRotate( rot0, pos0, pos1 );
        return rot1 == null ? rot0 : rot1;
    }

    public boolean useRanges( boolean reflect, double[] r3, double radiusRad ) {
        return ! isFovSpecified( r3, radiusRad );
    }

    public SkyAspect createAspect( boolean reflect, double[] r3,
                                   double radiusRad, Range[] vxyzRanges ) {

        /* If we have a specified field of view, rotate so the centre of it
         * is at the centre of the projection plane,
         * set the zoom accordingly, and use that. */
        if ( isFovSpecified( r3, radiusRad ) ) {
            double[] rotmat = rotateToCenter( r3, reflect );
            double zoom = 1.0 / Math.sin( Math.min( Math.PI / 2, radiusRad ) );
            return new SkyAspect( rotmat, zoom, 0, 0 );
        }

        /* Otherwise, if we have range information, use that. */
        else if ( vxyzRanges != null ) {
            assert vxyzRanges.length == 3;

            /* If the range is effectively all-sky, return the default view. */
            if ( SkySurfaceFactory.isAllSky( vxyzRanges ) ) {
                return getDefaultAspect( reflect );
            }

            /* Otherwise get the central position from the range data. */
            else {
                double[] crot = getRangeRotation( vxyzRanges, reflect );

                /* If there's no data, just use a default. */
                if ( crot == null ) {
                    return getDefaultAspect( reflect );
                }

                /* If the data represents a single position, don't try to
                 * zoom in around it, since the zoom would be arbitrary;
                 * but rotate the view so it's at the center of the
                 * projection plane. */
                else if ( isSinglePoint( vxyzRanges ) ) {
                    return new SkyAspect( crot, 1.0, 0, 0 );
                }

                /* If we have a finite-sized range, rotate the view and
                 * zoom in far enough to accommodate it all. */
                else {

                    /* Get an estimate of the extreme values of the
                     * projected coordinates these will lead to for
                     * the rotated projection. */
                    Range[] pxyRanges = readProjectedRanges( vxyzRanges, crot );
                    double[] pxBounds = pxyRanges[ 0 ].getBounds();
                    double[] pyBounds = pxyRanges[ 1 ].getBounds();
                    double pmax = Arrays.maximum( new double[] {
                        Math.abs( pxBounds[ 0 ] ), Math.abs( pxBounds[ 1 ] ),
                        Math.abs( pyBounds[ 0 ] ), Math.abs( pyBounds[ 1 ] ),
                        1.0 / MAX_RANGE_ZOOM,
                    } );

                    /* Work out a zoom factor based on this extent. */
                    double zoom = 1.0 / pmax;

                    /* Return an aspect based on the rotation and zoom
                     * we've determined. */
                    return new SkyAspect( crot, zoom, 0, 0 );
                }
            }
        }

        /* Insufficient information, return a default view. */
        else {
            return getDefaultAspect( reflect );
        }
    }

    public SkyFov getFov( SkySurface surf ) {
        if ( isDefaultAspect( surf ) ) {
            return null;
        }
        else {
            double[] rotmat = surf.getRotation();
            double zoom = surf.getZoom();
            double[] center = Matrices.mvMult( Matrices.invert( rotmat ), RX );
            double[] lonLat = surf.getRoundedLonLatDegrees( center );
            double rdeg = Math.toDegrees( Math.asin( 1.0 / zoom ) );
            Rectangle bounds = surf.getPlotBounds();
            int npix = Math.max( bounds.width, bounds.height );
            double radiusDeg =
                PlotUtil.roundNumber( rdeg, rdeg / ( 10. * npix ) );
            return new SkyFov( lonLat[ 0 ], lonLat[ 1 ], radiusDeg );
        }
    }

    /**
     * Returns the default view for this projection. 
     *
     * @param  reflect  whether longitude runs right to left
     * @return  default aspect
     */
    private static SkyAspect getDefaultAspect( boolean reflect ) {
        double[] rot = verticalRotate( Math.toRadians( -15 ),
                                       Math.toRadians( -10 ), reflect );
        return new SkyAspect( rot, 1, 0, 0 );
    }

    /**
     * Indicates whether a given sky surface using this projection
     * is displayed in the default aspect.
     *
     * @param   surf   surface using SinProjection
     * @return  true  iff surf is using the default aspect
     */
    private static boolean isDefaultAspect( SkySurface surf ) {
        double[] rotmat = surf.getRotation();
        SkyAspect dflt = getDefaultAspect( isReflected( rotmat ) );
        return surf.getZoom() == dflt.getZoom()
            && surf.getOffsetX() == dflt.getOffsetX()
            && surf.getOffsetY() == dflt.getOffsetY()
            && java.util.Arrays.equals( rotmat, dflt.getRotation() );
    }

    /**
     * Given an optional central vector and optional radius, determine
     * whether enough information is present to specify a field of view.
     *
     * @param   normalized vector giving central coordinates, may be null
     * @param   radius of field of view size, may be NaN
     * @return  true iff both <code>r3</code> and <code>radiusRad</code>
     *          are sensible
     */
    private boolean isFovSpecified( double[] r3, double radiusRad ) {
        return r3 != null
            && ! Double.isNaN( r3[ 0 ] )
            && ! Double.isNaN( r3[ 1 ] )
            && ! Double.isNaN( r3[ 2 ] )
            && radiusRad > 0;
    }

    /**
     * Takes X,Y,Z normalised coordinate ranges and tries to work out
     * the ranges in projected plane coordinates they represent for
     * a given rotation matrix.  The determination is approximate.
     *
     * @param   vxyzRanges  3-element array giving ranges of normalised
     *                      X,Y,Z data coordinates
     * @param   rot   9-element rotation matrix to be applied before projection
     * @return  2-element array giving plane coordinate X and Y ranges
     *          covered by the supplied data coordinate ranges
     */
    private Range[] readProjectedRanges( Range[] vxyzRanges, double[] rot ) {
        double[] vxBounds = vxyzRanges[ 0 ].getBounds();
        double[] vyBounds = vxyzRanges[ 1 ].getBounds();
        double[] vzBounds = vxyzRanges[ 2 ].getBounds();
        double[] r3 = new double[ 3 ];
        Range pxRange = new Range();
        Range pyRange = new Range();

        /* Iterate over each corner of the cuboid represented by the XYZ
         * ranges, and use each of these corners (actually, their projection
         * on the unit sphere surface) as sample points to mark out the
         * limits of X and Y projected position ranges.
         * This is pretty rough, but should provide an overestimate of the
         * actual X, Y ranges (I think?).  The smaller the range in XYZ
         * the better the estimate. */
        Point2D.Double point = new Point2D.Double();
        for ( int jx = 0; jx < 2; jx++ ) {
            r3[ 0 ] = vxBounds[ jx ];
            for ( int jy = 0; jy < 2; jy++ ) {
                r3[ 1 ] = vyBounds[ jy ];
                for ( int jz = 0; jz < 2; jz++ ) {
                    r3[ 2 ] = vzBounds[ jz ];
                    double[] s3 =
                        Matrices.normalise( Matrices.mvMult( rot, r3 ) );
                    if ( project( s3[ 0 ], s3[ 1 ], s3[ 2 ], point ) ) {
                        pxRange.submit( point.x );
                        pyRange.submit( point.y );
                    }
                }
            }
        }
        return new Range[] { pxRange, pyRange };
    }

    /**
     * Attempts to return a rotation matrix corresponding to moving
     * the plane between two cursor positions, with a given initial
     * rotation matrix in effect.
     *
     * @param  rot0   initial rotation matrix
     * @param  pos0   initial projected position
     * @param  pos1   destination projected position
     * @return   destination rotation matrix, or null
     * @see   Projection#projRotate
     */
    private double[] genericRotate( double[] rot0, Point2D.Double pos0,
                                                   Point2D.Double pos1 ) {
        double[] rv0 = new double[ 3 ];
        if ( unproject( pos0, rv0 ) &&
             getSkyviewProjecter()
            .validPosition( new double[] { pos1.x, pos1.y } ) ) {
            double[] unrot0 = Matrices.invert( rot0 );
            double[] ru0 = Matrices.mvMult( unrot0, rv0 );
            return getRotation( ru0, pos1, rot0 );
        }
        else {
            return null;
        }
    }

    /**
     * Determines whether an array of ranges represents a single
     * dimensionless point, that is whether all the ranges have
     * an extent of zero.
     *
     * @param  ranges  array of ranges
     * @return  true iff all ranges represent a definite interval
     *               of zero extent
     */
    private static boolean isSinglePoint( Range[] ranges ) {
        for ( Range range : ranges ) {
            double[] bounds = range.getBounds();
            if ( bounds[ 0 ] != bounds[ 1 ] ) {
                return false;
            }
        }
        return true;
    }

    private static double[] getRotation( double[] rv0, Point2D.Double pos1,
                                         double[] rot0 ) {
        boolean reflect = isReflected( rot0 );
        final double fr = reflect ? -1 : +1;
        final double rx = rv0[ 0 ];
        final double ry = rv0[ 1 ];
        final double rz = rv0[ 2 ];
        final double px = pos1.x;
        final double py = pos1.y;

        // Use algebra from verticalRotate matrix.
        double delta0 = Math.atan2( -rot0[ 2 ], rot0[ 8 ] );
        double alpha0 = Math.atan2( -rot0[ 3 ], rot0[ 4 ] * fr ) ;

        // Find alpha and delta rotation angles which put the given vector
        // at the target screen position.
        double alpha = new Solver() {
            double[] derivs( double a ) {
                double sa = Math.sin( a );
                double ca = Math.cos( a );
                return new double[] {
                    - sa * rx + ca * ry * fr - px,
                    - ca * rx - sa * ry * fr,
                };
            }
        }.solve( alpha0 );
        if ( Double.isNaN( alpha ) ) {
            return null;
        }
        final double ca = Math.cos( alpha );
        final double sa = Math.sin( alpha );
        double delta = new Solver() {
            double[] derivs( double d ) {
                double sd = Math.sin( d );
                double cd = Math.cos( d );
                return new double[] {
                    sd * ( ca * rx + sa * ry * fr ) + cd * rz - py,
                    cd * ( ca * rx + sa * ry * fr ) - sd * rz,
                };
            }
        }.solve( delta0 );
        if ( Double.isNaN( delta ) ) {
            return null;
        }

        double[] rot1 = verticalRotate( delta, alpha, reflect ); 
        if ( Matrices.mvMult( rot1, rv0 )[ 0 ] >= 0 &&
             Matrices.mvMult( rot1, RZ )[ 2 ] > 0 ) {
            return rot1;
        }
        else {
            return null;
        }
    }

    /**
     * Rotation matrix which results in an orientation with the
     * viewing plane X coordinate of the north pole equal to zero.
     * This is a rotation invariant we wish to preserve.
     *
     * @param  delta   rotation of pole from vertical (0..pi)
     * @param  alpha   rotation around pole (0..2pi)
     * @param  reflect if true, alpha increases right to left
     * @return rotation matrix
     */
    public static double[] verticalRotate( double delta, double alpha,
                                           boolean reflect ) {
        double fr = reflect ? -1.0 : +1.0;
        double ca = Math.cos( alpha );
        double sa = Math.sin( alpha );
        double cd = Math.cos( delta );
        double sd = Math.sin( delta );
        double[] rot = new double[] {
            cd * ca,  cd * sa * fr,  -sd,
                -sa,       ca * fr,    0,
            sd * ca,  sd * sa * fr,   cd,
        };
        return rot;
    }

    /**
     * Returns the result of rotating a matrix about a given unit vector
     * by a given angle.
     *
     * @param  matrix   initial matrix
     * @param  unitvect  vector to rotate around, must be normalised
     * @param  angle   rotation angle in radians
     * @return  rotated matrix
     */
    private static double[] rotateAround( double[] matrix, double[] unitvec,
                                          double angle ) {
        assert Math.abs( ( unitvec[ 0 ] * unitvec[ 0 ] +
                           unitvec[ 1 ] * unitvec[ 1 ] +
                           unitvec[ 2 ] * unitvec[ 2 ] ) - 1 ) < 1e6;
        double[] rm =
            Matrices.fromPal( new Pal()
                             .Dav2m( Matrices.mult( unitvec, angle ) ) );
        return Matrices.mmMult( matrix, rm );
    }

    /**
     * Indicates whether a rotation matrix represents reflected coordinates.
     *
     * @param   rotmat  rotation matrix
     * @return   true if determinant is less than 0
     */
    private static boolean isReflected( double[] rotmat ) {
        return Matrices.det( rotmat ) < 0;
    }

    /**
     * Returns a rotation matrix that rotates a given vector to the
     * centre of the projection plane (facing towards the viewer).
     *
     * @param   r3   X,Y,Z normalised vector
     * @param  reflect  whether rotation matrix should be reflected
     * @return  9-element rotation matrix
     */
    private static double[] rotateToCenter( double[] r3, boolean reflect ) {
        double rx = r3[ 0 ];
        double ry = r3[ 1 ];
        double rz = r3[ 2 ];
        double fr = reflect ? -1 : +1;
        double alpha = Math.atan2( ry, rx );
        double ca = Math.cos( alpha );
        double sa = Math.sin( alpha );
        double delta = Math.atan2( -rz, ca * rx + sa * ry );
        return verticalRotate( delta, fr * alpha, reflect );
    }

    /**
     * Works out the rotation matrix to use to center the positions
     * represented by Cartesian coordinate ranges on the plot surface.
     * Null may be returned if there is no appropriate rotation.
     *
     * @return  9-element rotation matrix or null.
     */
    private static double[] getRangeRotation( Range[] vxyzRanges,
                                              boolean reflect ) {
        double[] center = new double[ 3 ];
        for ( int id = 0; id < 3; id++ ) {
            double[] bounds = vxyzRanges[ id ].getBounds();
            double mid = 0.5 * ( bounds[ 0 ] + bounds[ 1 ] ) ;
            if ( Double.isNaN( mid ) ) {
                return null;
            }
            assert mid >= -1.001 && mid <= +1.001;
            center[ id ] = mid;
        }
        if ( Matrices.mod( center ) < 0.3 ) {
            return null;
        }
        center = Matrices.normalise( center );
        return rotateToCenter( center, reflect );
    }


    /**
     * Utility class for solving equations iteratively using Newton's method.
     * The convergeance criterion is appropriate for roots that are angles.
     */
    private static abstract class Solver {
        private static final double SMALL = Math.PI / 180 / 3600 * 1e-6; // 1uas
        private static final int IMAX = 24;

        /**
         * Returns zero'th and first derivatives of function at a given point.
         *
         * @param  x
         * @return  [f(x), f'(x)]
         */
        abstract double[] derivs( double x );

        /**
         * Returns an iterative solution of the function defined by the
         * <code>derivs</code> method.
         *
         * @param  x0  initial approximation for root
         * @return   root of equation, or NaN
         */
        double solve( double x0 ) {
            double x = x0;
            for ( int i = 0; i < IMAX; i++ ) {
                double[] dxs = derivs( x );
                double f0 = dxs[ 0 ];
                double f1 = dxs[ 1 ];
                if ( Math.abs( f0 ) <= SMALL ) {
                    return x;
                }
                x -= f0 / f1;
            }
            return Double.NaN;
        }
    }
}
