package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Rotates vectors between sky systems.
 *
 * @author   Mark Taylor
 * @since    20 Apr 2016
 */
@Equality
public abstract class Rotation {

    /** Identity rotation; the rotate method is a no-op. */
    public static Rotation IDENTITY = new Rotation() {
        public void rotate( double[] r3 ) {
        }
        public Rotation invert() {
            return this;
        }
    };

    /**
     * Rotates a 3-vector in place.
     *
     * @param   r3  3-element unit vector, changed on output
     */
    public abstract void rotate( double[] r3 );

    /**
     * Returns the inverse of this rotation.
     *
     * @return   inverse rotation
     */
    public abstract Rotation invert();

    /**
     * Indicates whether the rotation between two sky systems is known
     * to be an identity (no-op) transformation.
     * If either or both sky systems is null, it is assumed that no
     * rotation is required.
     *
     * @param  inSys   input sky system; may be null
     * @param  outSys  output sky system; may be null
     * @return  true iff no rotation operation is required
     */
    public static boolean isIdentityRotation( SkySys inSys, SkySys outSys ) {
        return inSys == null || outSys == null || inSys.equals( outSys );
    }

    /**
     * Returns a rotation instance that can transform between two submitted
     * sky systems.
     *
     * @param  inSys   input sky system; may be null
     * @param  outSys  output sky system; may be null
     * @return  rotation from input to output sky systems, not null
     */
    public static Rotation createRotation( SkySys inSys, SkySys outSys ) {
        if ( isIdentityRotation( inSys, outSys ) ) {
            return IDENTITY;
        }
        else {
            double[] rotmat =
                Matrices.mmMult( Matrices.invert( outSys.toEquatorial() ),
                                 inSys.toEquatorial() );
            return new MatrixRotation( rotmat );
        }
    }

    /**
     * Rotation implementation that uses a transformation matrix.
     */
    private static class MatrixRotation extends Rotation {

        private final double[] rotmat_;
        private final double r0_;
        private final double r1_;
        private final double r2_;
        private final double r3_;
        private final double r4_;
        private final double r5_;
        private final double r6_;
        private final double r7_;
        private final double r8_;

        /**
         * Constructor.
         *
         * @param  rotmat  transformation matrix
         */
        MatrixRotation( double[] rotmat ) {
            r0_ = rotmat[ 0 ];
            r1_ = rotmat[ 1 ];
            r2_ = rotmat[ 2 ];
            r3_ = rotmat[ 3 ];
            r4_ = rotmat[ 4 ];
            r5_ = rotmat[ 5 ];
            r6_ = rotmat[ 6 ];
            r7_ = rotmat[ 7 ];
            r8_ = rotmat[ 8 ];
            rotmat_ =
                new double[] { r0_, r1_, r2_, r3_, r4_, r5_, r6_, r7_, r8_ };
        }

        public void rotate( double[] dpos ) {
            double dx = dpos[ 0 ];
            double dy = dpos[ 1 ];
            double dz = dpos[ 2 ];
            dpos[ 0 ] = r0_ * dx + r1_ * dy + r2_ * dz;
            dpos[ 1 ] = r3_ * dx + r4_ * dy + r5_ * dz;
            dpos[ 2 ] = r6_ * dx + r7_ * dy + r8_ * dz;
        }

        public Rotation invert() {
            return new MatrixRotation( Matrices.invert( rotmat_ ) );
        }

        @Override
        public int hashCode() {
            int code = 55442;
            code = 23 * code + Arrays.hashCode( rotmat_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MatrixRotation ) {
                MatrixRotation other = (MatrixRotation) o;
                return Arrays.equals( this.rotmat_, other.rotmat_ );
            }
            else {
                return false;
            }
        }
    }
}
