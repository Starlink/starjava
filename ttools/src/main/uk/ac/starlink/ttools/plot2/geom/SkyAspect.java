package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot.Matrices;

/**
 * Defines the view of a SkySurface.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class SkyAspect {
    private final Projection projection_;
    private final double[] rotmat_;
    private final double zoom_;
    private final double xoff_;
    private final double yoff_;

    private static final Projection[] KNOWN_PROJECTIONS =
        createKnownProjections();

    /**
     * Constructor.
     *
     * @param  projection  sky projection
     * @param  rotmat  9-element rotation matrix
     * @param  zoom    zoom factor; 1 means the sky is approximately
     *                 the same size as plot bounds
     * @param  xoff  x offset of plot centre from plot bounds centre
     *               in dimensionless units; 0 is centred
     * @param  yoff  y offset of plot centre from plot bounds centre
     *               in dimensionless units; 0 is centred
     */
    public SkyAspect( Projection projection, double[] rotmat, double zoom,
                      double xoff, double yoff ) {
        projection_ = projection;
        rotmat_ = rotmat.clone();
        zoom_ = zoom;
        xoff_ = xoff;
        yoff_ = yoff;
    }

    /**
     * Constructs a default aspect from a given projection.
     *
     * @param  projection   sky projection
     * @param  reflect   whether longitude runs right to left
     */
    public SkyAspect( Projection projection, boolean reflect ) {
        this( projection, unitMatrix( reflect ), 1, 0, 0 );
        assert reflect == isReflected();
    }

    /**
     * Returns sky projection.
     *
     * @return  projection
     */
    public Projection getProjection() {
        return projection_;
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

    /**
     * Indicates whether the rotation matrix includes a reflection.
     *
     * @return  true iff longitude runs right to left (or equivalent)
     */
    public boolean isReflected() {
        return Matrices.det( rotmat_ ) < 0;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SkyAspect ) {
            SkyAspect other = (SkyAspect) o;
            return this.projection_.equals( other.projection_ )
                && Arrays.equals( this.rotmat_, other.rotmat_ )
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
        code = 23 * code + projection_.hashCode();
        code = 23 * code + Arrays.hashCode( rotmat_ );
        code = 23 * code + Float.floatToIntBits( (float) zoom_ );
        code = 23 * code + Float.floatToIntBits( (float) xoff_ );
        code = 23 * code + Float.floatToIntBits( (float) yoff_ );
        return code;
    }

    /**
     * Returns known projection options.
     *
     * @return   list of available projections
     */
    public static Projection[] getProjections() {
        return KNOWN_PROJECTIONS.clone();
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

    /**
     * Returns a new list of known projections.
     */
    private static Projection[] createKnownProjections() {
        return new Projection[] {
            // new HemisphereProjection(),  // toy projection, inferior to Sin
            SinProjection.INSTANCE,
            SkyviewProjection.AIT,
            SkyviewProjection.CAR1,
        };
    }
}
