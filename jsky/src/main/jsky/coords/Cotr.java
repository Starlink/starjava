/*
 * ESO Archive
 *
 * $Id: Cotr.java,v 1.3 2002/08/20 09:57:58 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/10  port of cotr.c, Francois Ochsenbein [ESO-IPG]
 */

package jsky.coords;

/**
 * Based on C routintes by Francois Ochsenbein [ESO-IPG].
 * <p>
 * The static methods provided in this class all deal with coordinate transformations.
 * All spherical coordinates are assumed to be expressed in DEGREES.
 * No function is traced.
 *
 * The parameter mnemonics are:
 * <dl>
 *
 * <dd>o</dd>
 * <dt>array [alpha, delta] of spherical coordinates, expressed in degrees.</dt>
 *
 * <dd>R</dd>
 * <dt> 3 x 3  Rotation (orthogonal) matrix from old to new coordinate frame</dt>
 *
 * <dd>u</dd>
 * <dt>vector[x,y,z] of Unit (cosine) direction (x^2+y^2+z^2=1)
 *
 * </dl>
 */
public class Cotr {

    /**
     * Compute the rotation matrix from Euler angles
     * (z, theta, zeta). This rotation matrix is actually defined by
     * R = R_z(-z) * R_y(theta) * R_z(-zeta) (from old to new frame).
     *
     * @param euler_angles IN: Euler angles (z, theta, zeta)
     * @param R OUT: rotation matrix
     */
    public static void tr_Euler(double[] euler_angles, double[][] R) {
        int ze = 0, theta = 1, zeta = 2;
        double w;

        R[0][2] = Trigod.cosd(euler_angles[ze]);
        R[1][2] = Trigod.sind(euler_angles[ze]);
        R[2][0] = Trigod.cosd(euler_angles[zeta]);
        R[2][1] = Trigod.sind(euler_angles[zeta]);
        R[2][2] = Trigod.cosd(euler_angles[theta]);
        w = Trigod.sind(euler_angles[theta]);
        R[0][0] = R[2][0] * R[2][2] * R[0][2] - R[2][1] * R[1][2];
        R[1][0] = R[2][0] * R[2][2] * R[1][2] + R[2][1] * R[0][2];
        R[0][1] = -R[2][1] * R[2][2] * R[0][2] - R[2][0] * R[1][2];
        R[1][1] = -R[2][1] * R[2][2] * R[1][2] + R[2][0] * R[0][2];
        R[2][0] = R[2][0] * w;
        R[2][1] = -R[2][1] * w;
        R[0][2] = -w * R[0][2];
        R[1][2] = -w * R[1][2];
    }

    /**
     * Rotate polar coordinates using an R rotation matrix (old to new frame)
     *  and unit vectors.
     *
     * @param o  IN: Original angles
     * @param o2 OUT: rotated angles
     * @param R  IN: Rotation matrix
     */
    public static void tr_oo(double[] o, double[] o2, double[][] R) {
        double[] us = new double[3];

        tr_ou(o, us);		// tranforms polar angles into dir cos
        tr_uu(us, us, R);	// rotates dir cos
        tr_uo(us, o2);		// transform unit vector to angles
    }


    /**
     * Rotate polar coordinates, using the inversed R matrix
     * (new to old frame).
     * Use unit vectors
     *
     * @param o  IN: Original angles
     * @param o2 OUT: rotated angles
     * @param R  IN: Rotation matrix
     */
    public static void tr_oo1(double[] o, double[] o2, double[][] R) {
        double[] us = new double[3];

        tr_ou(o, us);		// tranforms polar angles into dir cos
        tr_uu1(us, us, R);	// rotates dir cos
        tr_uo(us, o2);		// transform unit vector to angles
    }

    /**
     * Creates the rotation matrix R[3][3].
     * R[3][3] is defined as:
     * <p>
     * <dl>
     * <dd>R[0]</dd>
     * <dt>(first row) = unit vector towards Zenith</dt>
     *
     * <dd>R[1]</dd>
     * <dt>(second row) = unit vector towards East</dt>
     *
     * <dd>R[2]</dd>
     * <dt>(third row) = unit vector towards North</dt>
     * </dl>
     * <p>
     * The resulting R matrix can then be used to get the components
     * of a vector v in the local frame.
     *
     * @param o IN: original angles
     * @param R OUT: rotation matrix
     */
    public static void tr_oR(double[] o, double[][] R) {
        double ra = o[0];
        double dec = o[1];
        R[2][2] = Trigod.cosd(dec);
        R[0][2] = Trigod.sind(dec);
        R[1][1] = Trigod.cosd(ra);
        R[1][0] = -Trigod.sind(ra);
        R[1][2] = 0.e0;
        R[0][0] = R[2][2] * R[1][1];
        R[0][1] = -R[2][2] * R[1][0];
        R[2][0] = -R[0][2] * R[1][1];
        R[2][1] = R[0][2] * R[1][0];
    }

    /**
     * Transformation from polar coordinates to Unit vector.
     *
     * @param o IN: angles ra + dec in degrees
     * @param u OUT: dir cosines
     */
    public static void tr_ou(double[] o, double[] u) {
        double ra = o[0];
        double dec = o[1];
        double cosdec = Trigod.cosd(dec);
        u[0] = cosdec * Trigod.cosd(ra);
        u[1] = cosdec * Trigod.sind(ra);
        u[2] = Trigod.sind(dec);
    }

    /**
     * Computes angles from direction cosines.
     *
     @param u IN: Dir cosines
     @param o OUT: Angles ra + dec in degrees
     */
    public static void tr_uo(double[] u, double[] o) {
        double x = u[0], y = u[1], z = u[2];
        double r2 = x * x + y * y;
        o[0] = 0.e0;	// ra
        if (r2 == 0.e0) {	// in case of poles
            if (z == 0.e0)
                return;	// not ok
            o[1] = (z > 0.e0 ? 90.e0 : -90.e0); // dec
            return;
        }

        o[1] = Trigod.atand(z / Math.sqrt(r2));
        o[0] = Trigod.atan2d(y, x);
        if (o[0] < 0.e0) o[0] += 360.e0;
    }


    /**
     * Creates the rotation matrix.
     * Creates the rotation matrix R[3][3] with
     * <p>
     * <dl>
     * <dd>R[0]</dd> <dt>(first row) = unit vector towards Zenith</dt>
     * <dd>R[1]</dd> <dt>(second row) = unit vector towards East</dt>
     * <dd>R[2]</dd> <dt>(third row) = unit vector towards North</dt>
     * </dl>
     * <p>
     * For the poles,(|z|=1), the rotation axis is assumed be the y axis, i.e.
     * the right ascension is assumed to be 0.
     *
     * @param u IN: Original direction
     * @param R OUT: Rotation matrix
     */
    public static void tr_uR(double[] u, double[][] R) {
        double x = u[0], y = u[1], z = u[2];
        R[0][0] = x;
        R[0][1] = y;
        R[0][2] = z;
        R[2][2] = Math.sqrt(x * x + y * y);
        R[1][0] = 0.e0;
        R[1][1] = 1.e0;		/* These are defaults for poles	*/
        R[1][2] = 0.e0;
        if (R[2][2] != 0.e0) {
            R[1][1] = x / R[2][2];
            R[1][0] = -y / R[2][2];
        }
        R[2][0] = -R[0][2] * R[1][1];
        R[2][1] = R[0][2] * R[1][0];
    }

    /**
     * Rotates the unit vector u1 to u2, as
     * u_2 = R * u_1 (old to new frame)
     *
     * @param u1 IN: Unit vector
     * @param u2 OUT: Resulting unit vector after rotation
     * @param R IN: rotation matrix (e.g. created by tr_oR)
     */
    public static void tr_uu(double[] u1, double[] u2, double[][] R) {
        int i,j;
        double val;
        double[] u_stack = new double[3]; // allows same address for input/output

        for (i = 0; i < 3; i++) {
            val = 0.e0;
            for (j = 0; j < 3; j++)
                val += R[i][j] * u1[j];
            u_stack[i] = val;
        }

        for (i = 0; i < 3; i++)
            u2[i] = u_stack[i];	// copies to output
    }

    /**
     * Rotates the unit vector u1 to u2, as
     *  u_2 = R^{-1} * u_1 (new to old frame).
     *
     * @param u1 IN: Unit vector
     * @param u2 OUT: Resulting unit vector after rotation
     * @param R IN: rotation matrix (e.g. created by tr_oR)
     */
    public static void tr_uu1(double[] u1, double[] u2, double[][] R) {
        int i, j;
        double val;
        double[] u_stack = new double[3]; // allows same address for input/output

        for (i = 0; i < 3; i++) {
            for (j = 0, val = 0.0e0; j < 3; j++)
                val += R[j][i] * u1[j];
            u_stack[i] = val;
        }
        for (i = 0; i < 3; i++)
            u2[i] = u_stack[i];		// copies to output
    }

    /**
     * Product of orthogonal matrices B = R * A.
     *
     * @param A IN: First Matrix
     * @param B OUT: Result Matrix
     * @param R IN: Rotation Matrix
     */
    public static void tr_RR(double[][] A, double[][] B, double[][] R) {
        int i, j, k;
        double val;
        double[][] Rs = new double[3][3]; // Local copy

        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                for (k = 0, val = 0.0e0; k < 3; k++)
                    val += R[i][k] * A[k][j];
                Rs[i][j] = val;
            }
        }

        for (i = 0; i < 3; i++)
            for (j = 0; j < 3; j++)
                B[i][j] = Rs[i][j];
    }

    /**
     * Product of orthogonal matrices B = R^{-1} * A.
     *
     * @param A IN: First Matrix
     * @param B OUT: Result Matrix
     * @param R IN: Rotation Matrix
     */
    public static void tr_RR1(double[][] A, double[][] B, double[][] R) {
        int i, j, k;
        double val;
        double[][] Rs = new double[3][3]; // Local copy

        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                for (k = 0, val = 0.0e0; k < 3; k++)
                    val += R[k][i] * A[k][j];
                Rs[i][j] = val;
            }
        }

        for (i = 0; i < 3; i++)
            for (j = 0; j < 3; j++)
                B[i][j] = Rs[i][j];
    }
}
