package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import skyview.geometry.Deprojecter;
import skyview.geometry.Projecter;
import skyview.geometry.Transformer;
import skyview.geometry.projecter.Ait;
import skyview.geometry.projecter.Arc;
import skyview.geometry.projecter.Car;
import skyview.geometry.projecter.Stg;
import skyview.geometry.projecter.Tan;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * SkyviewProjection subclass that does not use mouse gestures to
 * rotate the sky, only to pan and zoom over the projected plane.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2019
 */
public abstract class FixedSkyviewProjection extends SkyviewProjection {

    /** Aitoff projection, lon=0 at center. */
    public static final FixedSkyviewProjection AIT =
         createProjection( new Ait(), "Aitoff",
                           "Hammer-Aitoff projection with lon=0 at center",
                           FixedRotation.UNIT );

    /** Aitoff projection, lon=0 at edge. */
    public static final FixedSkyviewProjection AIT0 =
         createProjection( new Ait(), "Aitoff0",
                           "Hammer-Aitoff projection with lon=180 at center",
                           FixedRotation.LON_PI );

    /** Cartesian projection, lon=0 at center. */
    public static final FixedSkyviewProjection CAR1 =
         createProjection( new Car1(), "Car",
                           "Plate Carree projection (lon/lat on Cartesian axes)"
                         + " with lon=0 at center",
                           FixedRotation.UNIT );

    /** Cartesian projection, lon=0 at edge. */
    public static final FixedSkyviewProjection CAR0 =
         createProjection( new Car1(), "Car0",
                           "Plate Carree Projection (lon/lat on Cartesian axes)"
                         + " with lon=180 at center",
                           FixedRotation.LON_PI );

    /** Gnomonic projection. */
    public static final FixedSkyviewProjection TAN =
        createSimpleProjection( new Tan() );

    // Not whole sky and not currently rotatable - not much use.
    private static final FixedSkyviewProjection ARC =
        createSimpleProjection( new Arc() );
    private static final FixedSkyviewProjection STG =
        createSimpleProjection( new Stg() );

    /**
     * Constructor.
     *
     * @param  projecter  projecter object
     * @param  shape   shape of the sky in this projection
     * @param  name   projection name
     * @param  description   projection description
     */
    protected FixedSkyviewProjection( Projecter projecter, Shape shape,
                                      String name, String description ) {
        super( projecter, shape, name, description );
    }

    /**
     * Returns the fixed rotation matrix to use for this projection.
     *
     * @param   reflect   true to reflect longitude
     * @return  rotation matrix
     */
    protected abstract double[] getFixedRotation( boolean reflect );

    /**
     * Returns null - rotation not implemented.
     */
    public double[] cursorRotate( double[] rotmat, Point2D.Double pos0,
                                  Point2D.Double pos1 ) {
        return null;
    }

    /**
     * Returns null - rotation not implemented.
     */
    public double[] projRotate( double[] rotmat, Point2D.Double pos0,
                                Point2D.Double pos1 ) {
        return null;
    }

    /**
     * Returns false - ranging not used.
     */
    public boolean useRanges( boolean reflect, double[] r3, double radiusRad ) {
        return false;
    }

    public SkyAspect createAspect( boolean reflect, double[] r3,
                                   double radiusRad, Range[] ranges ) {
        double[] rotmat = getFixedRotation( reflect );
        final double zoom;
        final double xoff;
        final double yoff;
        double[] rr3 = r3 == null ? null : Matrices.mvMult( rotmat, r3 );
        Point2D.Double dpos = new Point2D.Double();
        if ( rr3 != null &&
             project( rr3[ 0 ], rr3[ 1 ], rr3[ 2 ], dpos ) ) {
            zoom = Math.PI / radiusRad;
            xoff = -dpos.x * zoom;
            yoff =  dpos.y * zoom;
        }
        else {
            zoom = 1;
            xoff = 0;
            yoff = 0;
        }
        return new SkyAspect( rotmat, zoom, xoff, yoff );
    }

    public SkyFov getFov( SkySurface surf ) {
        if ( surf.getZoom() == 1 &&
             surf.getOffsetX() == 0 && surf.getOffsetY() == 0 ) {
            return null;
        }
        Rectangle bounds = surf.getPlotBounds();
        int npix = Math.max( bounds.width, bounds.height );
        Point2D.Double gpos =
            new Point2D.Double( bounds.x + bounds.width / 2,
                                bounds.y + bounds.height / 2 );
        double[] r3 = surf.graphicsToData( gpos, null );
        if ( r3 != null ) {
            double[] lonLat = surf.getRoundedLonLatDegrees( r3 );
            double rdeg = 180. / surf.getZoom();
            double radiusDeg =
                PlotUtil.roundNumber( rdeg, rdeg / ( 10 * npix ) );
            return new SkyFov( lonLat[ 0 ], lonLat[ 1 ], radiusDeg );
        }
        else {
            return null;
        }
    }

    /**
     * Creates a projection with no discontinuities.
     * Name and description are taken from the skyview metadata.
     *
     * @param  projecter  skyview projecter
     * @return   projection
     */
    private static FixedSkyviewProjection
            createSimpleProjection( Projecter projecter ) {
        return new FixedSkyviewProjection( projecter, getShape( projecter ),
                                           projecter.getName(),
                                           projecter.getDescription() ) {
            public boolean isContinuous() {
                return true;
            }
            public boolean isContinuousLine( double[] r3a, double[] r3b ) {
                return true;
            }
            public double[] getFixedRotation( boolean reflect ) {
                return SkyAspect.unitMatrix( reflect );
            }
        };
    }

    /**
     * Creates a projection that is assumed to have discontinuities and
     * has a given fixed rotation from that of the base projecter.
     *
     * @param  projecter  projecter object
     * @param  name   projection name
     * @param  description   projection description
     * @param  rotation   fixed rotation
     * @return  new projection
     */
    private static FixedSkyviewProjection
            createProjection( Projecter projecter, String name, String descrip,
                              final FixedRotation rotation ) {
        return new FixedSkyviewProjection( projecter, getShape( projecter ),
                                           name, descrip ) {
            public boolean isContinuous() {
                return false;
            }
            public boolean isContinuousLine( double[] r3a, double[] r3b ) {
                return rotation.isContinuousLine( r3a, r3b );
            }
            public double[] getFixedRotation( boolean reflect ) {
                return rotation.getMatrix( reflect );
            }
        };
    }

    /**
     * Returns the shape of the sky for some projections.
     * This is the region of normalised plane coordinates into which
     * all invocations of the <code>project</code> method will map.
     *
     * @param  projecter  projecter
     * @param   sky projection shape in dimensionless units
     */
    private static Shape getShape( Projecter projecter ) {
        final double S2 = Math.sqrt( 2.0 );
        final double PI = Math.PI;
        Class<?> clazz = projecter.getClass();
        if ( clazz.equals( Ait.class ) ) {
            return new Ellipse2D.Double( -2 * S2, -S2, 4 * S2, 2 * S2 );
        }
        else if ( clazz.equals( Arc.class ) ) {
            return new Ellipse2D.Double( -PI, -PI, 2 * PI, 2 * PI );
        }
        else if ( clazz.equals( Car1.class ) ) {
            return new Rectangle2D.Double( -PI, -0.5 * PI, 2 * PI, PI );
        }
        else if ( clazz.equals( Stg.class ) ) {
            return new Ellipse2D.Double( -2, -2, 4, 4 );
        }
        // Hmm, Tan projection fills the entire plane.
        else if ( clazz.equals( Tan.class ) ) {
            return null;
        }
        throw new IllegalArgumentException( "Don't know shape for projection" );
    }

    /**
     * Characterises a fixed rotation to be applied to a projection.
     */
    private static abstract class FixedRotation {

        /** Unit transformation (no rotation). */
        public static final FixedRotation UNIT = new FixedRotation() {
            public double[] getMatrix( boolean reflect ) {
                return reflect ? new double[] { 1,0,0, 0,-1,0, 0,0,1 }
                               : new double[] { 1,0,0, 0, 1,0, 0,0,1 };
            }
            public boolean isContinuousLine( double[] r3a, double[] r3b ) {
                 return r3a[ 1 ] * r3b[ 1 ] >= 0
                     || ( r3a[ 0 ] >= 0 && r3b[ 0 ] >= 0 );
            }
        };

        /* Rotation 180 degrees around the Z axis. */
        public static final FixedRotation LON_PI = new FixedRotation() {
            public double[] getMatrix( boolean reflect ) {
                return reflect ? new double[] { -1,0,0, 0, 1,0, 0,0,1 }
                               : new double[] { -1,0,0, 0,-1,0, 0,0,1 };
            }
            public boolean isContinuousLine( double[] r3a, double[] r3b ) {
                return r3a[ 1 ] * r3b[ 1 ] >= 0
                    || ( r3a[ 0 ] <= 0 && r3b[ 0 ] <= 0 );
            }
        };

        /**
         * Returns the rotation matrix.
         *
         * @param  reflect  true to reflect longitude axis
         * @return  9-element rotation matrix
         */
        abstract double[] getMatrix( boolean reflect );

        /**
         * Indicates whether a line between the two given sky positions
         * is (believed to be) continuous.
         *
         * @param  r3a  3-element array giving normalised X,Y,Z coordinates of
         *              line start
         * @param  r3b  3-element array giving normalised X,Y,Z coordinates of
         *              line end
         * @return  true if line is believed to be continuous
         * @see   Projection#isContinuousLine
         */
        abstract boolean isContinuousLine( double[] r3a, double[] r3b );
    }

    /**
     * Like the skyview Car projection, but does not repeat to tile the plane.
     * Like Car, it is centered on the origin.
     */
    private static class Car1 extends Projecter {
        final Car base_;

        /**
         * Constructor.
         */
        Car1() {
            base_ = new Car();
        }

        public String getName() {
            return base_.getName();
        }

        public String getDescription() {
            return base_.getDescription();
        }

        @Override
        public Deprojecter inverse() {
            // this is wrong!  but I don't think it matters for our purposes.
            return base_.inverse();
        }

        @Override
        public double getXTiling() {
            return 0;
        }

        @Override
        public double getYTiling() {
            return 0;
        }

        @Override
        public boolean isInverse( Transformer t ) {
            return base_.isInverse( t );
        }

        @Override
        public void transform( double[] sphere, double[] plane ) {
            base_.transform( sphere, plane );
        }

        @Override
        public boolean allValid() {
            return false;
        }

        @Override
        public boolean validPosition( double[] pos ) {
            double x = pos[ 0 ];
            double y = pos[ 1 ];
            return x >= -Math.PI && x <= Math.PI
                && y >= -Math.PI * 0.5 && y <= Math.PI * 0.5;
        }
    }
}
