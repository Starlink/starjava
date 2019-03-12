package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Shape;
import java.awt.geom.Point2D;
import skyview.geometry.Deprojecter;
import skyview.geometry.Projecter;

/**
 * Partial projection implementation based on classes from the Skyview package.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public abstract class SkyviewProjection implements Projection {

    private final Projecter projecter_;
    private final Deprojecter deprojecter_;
    private final Shape shape_;
    private final String name_;
    private final String description_;

    /**
     * Constructor.
     *
     * @param  projecter  projecter object
     * @param  shape   shape of the sky in this projection
     * @param  name   projection name
     * @param  description   projection description
     */
    protected SkyviewProjection( Projecter projecter, Shape shape,
                                 String name, String description ) {
        projecter_ = projecter;
        deprojecter_ = projecter.inverse();
        shape_ = shape;
        name_ = name;
        description_ = description;
    }

    public String getProjectionName() {
        return name_;
    }

    public String getProjectionDescription() {
        return description_;
    }

    public Shape getProjectionShape() {
        return shape_;
    }

    public boolean project( double rx, double ry, double rz,
                            Point2D.Double pos ) {
        double[] r2 = projecter_.transform( new double[] { rx, ry, rz } );
        if ( Double.isNaN( r2[ 0 ] ) ) {
            return false;
        }
        else {
            pos.x = r2[ 0 ];
            pos.y = r2[ 1 ];
        }
        return true;
    }

    public boolean unproject( Point2D.Double pos, double[] r3 ) {
        double[] xy = new double[] { pos.x, pos.y };
        if ( projecter_.validPosition( xy ) ) {
            deprojecter_.transform( xy, r3 );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the projecter object used by this SkyviewProjection.
     *
     * @return  projecter
     */
    public Projecter getSkyviewProjecter() {
        return projecter_;
    }
}
