/*
 * ESO Archive
 *
 * $Id: JPrec.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/10  port of jprec.c, Francois Ochsenbein [ESO-IPG]
 */

package jsky.coords;

/**
 * Precession of Coordinates in new IAU system.
 * <p>
 * This class is based on C routintes by Francois Ochsenbein [ESO-IPG].
 * It uses the IAU 76 precession constants and assumes the FK5 system.
 * <p>
 * Precession constants are taken from Lederle and Schwan (Astron. Astrophys.
 * <b>134</b>, 1, 1984), Liske J.H. (Astron. Astrophys. <b>73</b>, 282, 1979).
 * Dates must be expressed in <em>Julian Years</em>.
 * <p>
 * The precession may be applied on unit vectors (mnemonic <tt>u</tt>), or
 * on equatorial coordinates (mnemonic <tt>q</tt>).
 */
public class JPrec {

    /**
     * Compute the precession matrix, using the new IAU constants.
     * (IAU 1976). The resulting matrix is such that
     * u(t_1) = R * u(t_0) (old to new frame).
     *
     * @param R OUT: rotation matrix
     * @param eq0 IN: Initial equinox (Julian Years)
     * @param eq1 IN: Final equinox (Julian Years)
     */
    public static void prej_R(double[][] R, double eq0, double eq1) {
        double t0, dt, w;
        double[] euler_angles = new double[3];
        int ze = 0, theta = 1, zeta = 2;

        t0 = (eq0 - 2000.e0) / 100.e0;	// Origin is J2000
        dt = (eq1 - eq0) / 100.e0;


        w = 2306.2181e0 + (1.39656e0 - 0.000139e0 * t0) * t0;		// Arc seconds
        euler_angles[zeta] = (w + ((0.30188e0 - 0.000344e0 * t0) + 0.017998e0 * dt) * dt)
                * dt / 3600.e0;  					// Degrees
        euler_angles[ze] = (w + ((1.09468e0 + 0.000066e0 * t0) + 0.018203e0 * dt) * dt)
                * dt / 3600.e0;					// Degrees
        euler_angles[theta] = ((2004.3109e0 + (-0.85330e0 - 0.000217e0 * t0) * t0)
                + ((-0.42665e0 - 0.000217e0 * t0) - 0.041833e0 * dt) * dt) * dt / 3600.e0;

        // Computation of rotation matrix
        Cotr.tr_Euler(euler_angles, R);
    }


    /**
     * Performs a complete precession between 2 equinoxes.
     * Use the new IAU constants.
     * Compute the precession rotation matrix if necessary,
     * then apply the rotation.
     *
     * @param q0 IN: ra+dec at equinox eq0 in degrees
     * @param q1 OUT: precessed to equinox eq1
     * @param eq0 IN: Initial equinox (Julian Years)
     * @param eq1 IN: Final equinox (Julian Years)
     */
    public static void prej_q(double[] q0, double[] q1, double eq0, double eq1) {
        double[] us = new double[3];

        if (eq0 == eq1) {	// No precession at all, same equinox!!!
            q1[0] = q0[0];
            q1[1] = q0[1];
            return;
        }

        Cotr.tr_ou(q0, us);		// Convert to unit vector...
        prej_u(us, us, eq0, eq1); // precess on unit vectors...
        Cotr.tr_uo(us, q1);		// And finally -> coordinates
    }

    /**
     * Performs a complete precession between 2 equinoxes.
     * Use the new IAU constants.
     * Compute the precession rotation matrix if necessary,
     * then apply the rotation.
     *
     * @param u0 IN: Unit vector at equinox eq0
     * @param u1 OUT: precessed to equinox eq1
     * @param eq0 IN: Initial equinox (Julian Years)
     * @param eq1 IN: Final equinox (Julian Years)
     */
    public static void prej_u(double u0[], double u1[], double eq0, double eq1) {
        double _eq0 = 2000.e0;
        double _eq1 = 2000.e0;
        double[][] _r = {
            {1.e0, 0.e0, 0.e0},
            {0.e0, 1.e0, 0.e0},
            {0.e0, 0.e0, 1.e0}
        };

        if (eq0 == eq1) {	// No precession at all, same equinox!!!
            u1[0] = u0[0];
            u1[1] = u0[1];
            u1[2] = u0[2];
            return;
        }

        if (_eq0 != eq0 || _eq1 != eq1) {
            _eq0 = eq0;
            _eq1 = eq1;
            prej_R(_r, eq0, eq1); // Compute precession matrix
        }

        Cotr.tr_uu(u0, u1, _r);	// And finally rotate...
    }
}

