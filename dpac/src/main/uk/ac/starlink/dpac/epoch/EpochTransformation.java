package uk.ac.starlink.dpac.epoch;

// package gaia.cu1.tools.cu1crossmatch.epochtransformation;

/**
 * Propagates the 6-dimensional vector of barycentric astrometric parameters and
 * the associated covariance matrix from one epoch to another assuming uniform
 * space motion
 * 
 * @author Alexey Butkevich (AGB) and Daniel Michalik (MIC), based on the
 *         algorithm developed by L. Lindegren. The algorithm is described in
 *         The Hipparcos and Tycho Catalogues (ESA SP-1200), Volume 1, Section
 *         1.5.5, 'Epoch Transformation: Rigorous Treatment'.
 */
public final class EpochTransformation {

    private static double eps = 1.e-15;
    private static final double PI = Math.PI;

    private EpochTransformation() {

    }

    /**
     * Propagates the 6-dimensional vector of barycentric astrometric parameters
     * WITHOUT the associated covariance matrix from epoch t0 to epoch t assuming
     * uniform space motion.
     * 
     * All values are given in rad (/timeunit). The time unit can be chosen
     * arbitrarily, however needs to be consistent regarding tau and the time
     * dependent values in astrometric parameters.
     * 
     * @param tau
     *            epoch difference
     * @param a0
     *            original source parameters in rad(/timeunit) in order:
     *            <p>
     *            <ul>
     *            <li>alpha, right ascension at t0 [rad]
     *            <li>delta, declination at t0 [rad]
     *            <li>varPi, parallax at t0 [rad]
     *            <li>muAlphaStar, proper motion in R.A., mult by cos(Dec), at t0
     *            [rad/timeunit]
     *            <li>muDelta, proper motion in Dec at t0 [rad/timeunit]
     *            <li>mu_R, normalised radial velocity at t0 [rad/timeunit]
     *            </ul>
     *            <p>
     *            <p style="margin-left: 20px">
     *            The normalised radial velocity at epoch t0 is given by
     *            vr0*varPi/4.740470446 where vr0 is the barycentric radial velocity
     *            in [km/s] at epoch t0; similarly, the propagated radial velocity
     *            is given as vr*varPi/4.740470446 at epoch t.
     *            </p>
     * @param a
     *            propagated source parameters, order and units same as a0
     */
    static public void propagate(double tau, double[] a0, double[] a) {
        propagate(tau, a0, null, a, null);
    }

    /**
     * Propagates the 6-dimensional vector of barycentric astrometric parameters and
     * the associated covariance matrix from epoch t0 to epoch t assuming uniform
     * space motion.
     * 
     * All values are given in rad (/timeunit) and rad(/timeunit) * rad(/timeunit)
     * for the six astrometric parameters and the source covariance elements,
     * respectively. The time unit can be chosen arbitrarily, however needs to be
     * consistent regarding tau and the time dependent values in the astrometric
     * parameters and the covariance matrix.
     * 
     * @param tau
     *            epoch difference in arbitrary timeunit
     * @param a0
     *            original source parameters in rad(/timeunit) in order:
     *            <p>
     *            <ul>
     *            <li>alpha, right ascension at t0 [rad]
     *            <li>delta, declination at t0 [rad]
     *            <li>varPi, parallax at t0 [rad]
     *            <li>muAlphaStar, proper motion in R.A., mult by cos(Dec), at t0
     *            [rad/timeunit]
     *            <li>muDelta, proper motion in Dec at t0 [rad/timeunit]
     *            <li>mu_R, normalised radial velocity at t0 [rad/timeunit]
     *            </ul>
     *            <p>
     *            <p style="margin-left: 20px">
     *            The normalised radial velocity at epoch t0 is given by
     *            vr0*varPi/4.740470446 where vr0 is the barycentric radial velocity
     *            in [km/s] at epoch t0; similarly, the propagated radial velocity
     *            is given as vr*varPi/4.740470446 at epoch t.
     *            </p>
     * @param c0
     *            original 6x6 covariance matrix in rad(/timeunit) * rad(/timeunit)
     *            defining the variance-covariance of the six astrometric parameters
     *            in their order defined as for array a0.
     * @param a
     *            propagated source parameters, order and units as a0
     * @param c
     *            propagated covariance matrix, order and units as c0
     */
    static public void propagate(double tau, double[] a0, double[][] c0, double[] a, double[][] c) {
        propagate(tau, a0, c0, a, c, false);
    }

    /**
     * Propagates the 6-dimensional vector of barycentric astrometric parameters and
     * the associated covariance or normal matrix from epoch t0 to epoch t assuming
     * uniform space motion.
     * 
     * All values are given in rad (/timeunit) and rad(/timeunit) * rad(/timeunit)
     * for the six astrometric parameters and the source covariance elements,
     * respectively. If Normals are supplied, the units are 1/unit stated. The time
     * unit can be chosen arbitrarily, however needs to be consistent regarding tau
     * and the time dependent values in the astrometric parameters and the
     * covariance matrix.
     * 
     * @param tau
     *            epoch difference in arbitrary timeunit
     * @param a0
     *            original source parameters in rad(/timeunit) in order:
     *            <p>
     *            <ul>
     *            <li>alpha, right ascension at t0 [rad]
     *            <li>delta, declination at t0 [rad]
     *            <li>varPi, parallax at t0 [rad]
     *            <li>muAlphaStar, proper motion in R.A., mult by cos(Dec), at t0
     *            [rad/timeunit]
     *            <li>muDelta, proper motion in Dec at t0 [rad/timeunit]
     *            <li>mu_R, normalised radial velocity at t0 [rad/timeunit]
     *            </ul>
     *            <p>
     *            <p style="margin-left: 20px">
     *            The normalised radial velocity at epoch t0 is given by
     *            vr0*varPi/4.740470446 where vr0 is the barycentric radial velocity
     *            in [km/s] at epoch t0; similarly, the propagated radial velocity
     *            is given as vr*varPi/4.740470446 at epoch t.
     *            </p>
     * @param c0
     *            original 6x6 covariance matrix in rad(/timeunit) * rad(/timeunit)
     *            defining the variance-covariance of the six astrometric parameters
     *            in their order defined as for array a0.
     * @param a
     *            propagated source parameters, order and units as a0
     * @param c
     *            propagated covariance matrix, order and units as c0
     * @param covInvertedToNormals
     *            if true then c0 and c are normal matrices rather than covariances
     *            and will be handled accordingly
     */
    static public void propagate(double tau, double[] a0, double[][] c0, double[] a, double[][] c,
            boolean covInvertedToNormals) {
        // Initial parameters at t0
        // Source parameters
        double alpha0 = a0[0]; // right ascension at t0 [rad]
        double delta0 = a0[1]; // declination at t0 [rad]
        double par0 = a0[2]; // parallax at t0 [rad]
        double pma0 = a0[3]; // proper motion in R.A., mult by cos(Dec), at t0
                             // [rad/timeunit]
        double pmd0 = a0[4]; // proper motion in Dec at t0 [rad/timeunit]
        double zeta0 = a0[5]; // normalised radial velocity at t0 [rad/timeunit]

        // Calculate normal triad [p0 q0 r0] at t0; r0 is
        // also the unit vector to the star at epoch t0:
        GVector3d[] triad = GVector3d.localTriad(alpha0, delta0);
        GVector3d p0 = triad[0];
        GVector3d q0 = triad[1];
        GVector3d r0 = triad[2];

        // Proper motion vector
        GVector3d pmv0 = GVector3d.scaleAdd(GVector3d.scale(pma0, p0), pmd0, q0);

        // Various auxiliary quantities
        double tau2 = tau * tau;
        double pm02 = pma0 * pma0 + pmd0 * pmd0;
        double w = 1.0 + zeta0 * tau;
        double f2 = 1.0 / (1.0 + 2.0 * zeta0 * tau + (pm02 + zeta0 * zeta0) * tau2);
        double f = Math.sqrt(f2);
        double f3 = f2 * f;
        double f4 = f2 * f2;

        // The position vector and parallax at t
        GVector3d r = GVector3d.scaleAdd(GVector3d.scale(w, r0), tau, pmv0).scale(f);
        double par = par0 * f;

        // The proper motion vector and normalised radial velocity at t
        GVector3d pmv = GVector3d.scaleAdd(GVector3d.scale(w, pmv0), -pm02 * tau, r0).scale(f3);
        double zeta = (zeta0 + (pm02 + zeta0 * zeta0) * tau) * f2;

        // The normal triad [p q r] at t; if r is very
        // close to the pole, select p towards RA=90 deg
        double xy = Math.sqrt(r.x() * r.x() + r.y() * r.y());
        GVector3d p = new GVector3d();
        if (xy < eps) {
            p.set(0.0, 1.0, 0.0);
        } else {
            p.set(-r.y() / xy, r.x() / xy, 0.0);
        }
        GVector3d q = GVector3d.cross(r, p);

        // Convert parameters at t to external units
        double alpha = Math.atan2(-p.x(), p.y());
        if (alpha < 0.0) {
            alpha += PI * 2.0;
        }
        double delta = Math.atan2(r.z(), xy);
        double pma = p.dot(pmv);
        double pmd = q.dot(pmv);

        a[0] = alpha;
        a[1] = delta;
        a[2] = par;
        a[3] = pma;
        a[4] = pmd;
        a[5] = zeta;

        // If no covariance matrix is given we are done with the propagation and
        // return.
        // Otherwise the following code block will take care of propagating the
        // covariance matrix to the new epoch.
        if (c0 == null) {
            return;
        }

        // Auxiliary quantities for the partial derivatives
        GVector3d pmz = GVector3d.scaleAdd(GVector3d.scale(f, pmv0), -3.0 * w, pmv);
        double pp0 = p.dot(p0);
        double pq0 = p.dot(q0);
        double pr0 = p.dot(r0);
        double qp0 = q.dot(p0);
        double qq0 = q.dot(q0);
        double qr0 = q.dot(r0);
        double ppmz = p.dot(pmz);
        double qpmz = q.dot(pmz);

        // Partial derivatives
        double[][] d = new double[6][6];
        d[0][0] = pp0 * w * f - pr0 * pma0 * tau * f;
        d[0][1] = pq0 * w * f - pr0 * pmd0 * tau * f;
        d[0][2] = 0.0;
        d[0][3] = pp0 * tau * f;
        d[0][4] = pq0 * tau * f;
        d[0][5] = -pma * tau2;

        d[1][0] = qp0 * w * f - qr0 * pma0 * tau * f;
        d[1][1] = qq0 * w * f - qr0 * pmd0 * tau * f;
        d[1][2] = 0.0;
        d[1][3] = qp0 * tau * f;
        d[1][4] = qq0 * tau * f;
        d[1][5] = -pmd * tau2;

        d[2][0] = 0.0;
        d[2][1] = 0.0;
        d[2][2] = f;
        d[2][3] = -par * pma0 * tau2 * f2;
        d[2][4] = -par * pmd0 * tau2 * f2;
        d[2][5] = -par * w * tau * f2;

        d[3][0] = -pp0 * pm02 * tau * f3 - pr0 * pma0 * w * f3;
        d[3][1] = -pq0 * pm02 * tau * f3 - pr0 * pmd0 * w * f3;
        d[3][2] = 0.0;
        d[3][3] = pp0 * w * f3 - 2.0 * pr0 * pma0 * tau * f3 - 3.0 * pma * pma0 * tau2 * f2;
        d[3][4] = pq0 * w * f3 - 2.0 * pr0 * pmd0 * tau * f3 - 3.0 * pma * pmd0 * tau2 * f2;
        d[3][5] = ppmz * tau * f2;

        d[4][0] = -qp0 * pm02 * tau * f3 - qr0 * pma0 * w * f3;
        d[4][1] = -qq0 * pm02 * tau * f3 - qr0 * pmd0 * w * f3;
        d[4][2] = 0.0;
        d[4][3] = qp0 * w * f3 - 2.0 * qr0 * pma0 * tau * f3 - 3.0 * pmd * pma0 * tau2 * f2;
        d[4][4] = qq0 * w * f3 - 2.0 * qr0 * pmd0 * tau * f3 - 3.0 * pmd * pmd0 * tau2 * f2;
        d[4][5] = qpmz * tau * f2;

        d[5][0] = 0.0;
        d[5][1] = 0.0;
        d[5][2] = 0.0;
        d[5][3] = 2.0 * pma0 * w * tau * f4;
        d[5][4] = 2.0 * pmd0 * w * tau * f4;
        d[5][5] = (w * w - pm02 * tau2) * f4;

        // Propagate the covariance as c = d*c0*d'
        double sum;
        final int dim = 6;
        double tmpKI;
        if (covInvertedToNormals) {
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    sum = 0.0;
                    for (int k = 0; k < dim; k++) {
                        tmpKI = d[k][i];
                        for (int l = 0; l < dim; l++) {
                            sum += tmpKI * c0[k][l] * d[l][j];
                        }
                    }
                    c[i][j] = sum;
                }
            }
        } else {
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    sum = 0.0;
                    for (int k = 0; k < dim; k++) {
                        tmpKI = d[i][k];
                        for (int l = 0; l < dim; l++) {
                            sum += tmpKI * c0[k][l] * d[j][l];
                        }
                    }
                    c[i][j] = sum;
                }
            }
        }
        return;
    }
}
