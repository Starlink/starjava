package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scale;

/**
 * Represents the range of angular coordinates that are covered
 * by a given region.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2018
 */
public class SphereAngleRange {

    private final double phiLo_;
    private final double phiHi_;
    private final double thetaLo_;
    private final double thetaHi_;

    /**
     * Constructor.
     *
     * @param   phiLo  phi lower bound
     * @param   phiHi  phi upper bound
     * @param   thetaLo  theta lower bound
     * @param   thetaHi  theta upper bound
     */
    public SphereAngleRange( double phiLo, double phiHi,
                             double thetaLo, double thetaHi ) {
        phiLo_ = phiLo;
        phiHi_ = phiHi;
        thetaLo_ = thetaLo;
        thetaHi_ = thetaHi;
    }

    /**
     * Returns the limits of azimuthal angle.
     * A pair of values is returned; the first is always lower than the
     * second, but neither is guaranteed to lie within a given range.
     *
     * @return  2-element array giving (lo, hi) limits on phi
     */
    public double[] getPhiLimits() {
        return new double[] { phiLo_, phiHi_ };
    }

    /**
     * Returns the limits of polar angle.
     * A pair of values is returned; the first is lower than the second,
     * and both are in the range (-PI/2..+PI/2).
     *
     * @return  2-element array giving (lo, hi) limits on theta
     */
    public double[] getThetaLimits() {
        return new double[] { thetaLo_, thetaHi_ };
    }

    /**
     * Determines the angle range for a cube defined by Cartesian limits.
     *
     * <strong>Note:</strong> This code hasn't been tried or tested at all,
     * it may contain more or less serious errors.
     *
     * @param  dlos  3-element array giving cube lower bounds
     * @param  dhis  3-element array giving cube upper bounds
     * @return   angle range
     */
    public static SphereAngleRange calculateRange( double[] dlos,
                                                   double[] dhis ) {
        double[] phiRange = null;
        double[] thetaRange = null;
        if ( zeroNearCenter( dlos[ 0 ], dhis[ 0 ] ) &&
             zeroNearCenter( dlos[ 1 ], dhis[ 1 ] ) ) {
            phiRange = new double[] { - Math.PI, + Math.PI };
        }
        if ( zeroNearCenter( dlos[ 2 ], dhis[ 2 ] ) ) {
            thetaRange = new double[] { - 0.5 * Math.PI, 0.5 * Math.PI };
        }
        if ( phiRange == null || thetaRange == null ) {
            double[] dc = new double[] {
                frac( dlos[ 0 ], dhis[ 0 ], 0.5 ),
                frac( dlos[ 1 ], dhis[ 1 ], 0.5 ),
                frac( dlos[ 2 ], dhis[ 2 ], 0.5 ),
            };
            double phiCent = Math.atan2( dc[ 1 ], dc[ 0 ] );
            boolean phiFlip = Math.abs( phiCent ) > 0.5 * Math.PI;
            double thetaLo = + 0.5 * Math.PI;
            double thetaHi = - 0.5 * Math.PI;
            double phiLo = 2 * Math.PI;
            double phiHi = - Math.PI;
            int nsamp = 3;
            double dn1 = 1.0 / ( nsamp - 1.0 );
            for ( int ix = 0; ix < nsamp; ix++ ) {
                double dx = frac( dlos[ 0 ], dhis[ 0 ], ix * dn1 );
                for ( int iy = 0; iy < nsamp; iy++ ) {
                    double dy = frac( dlos[ 1 ], dhis[ 1 ], iy * dn1 );
                    for ( int iz = 0; iz < nsamp; iz++ ) {
                        double dz = frac( dlos[ 2 ], dhis[ 2 ], iz * dn1 );
                        double r = Math.sqrt( dx * dx + dy * dy + dz * dz );
                        double phi = Math.atan2( dy, dx );
                        if ( phiFlip && phi < 0 ) {
                            phi += 2 * Math.PI;
                        }
                        double theta = Math.acos( dz / r );
                        thetaLo = Math.min( thetaLo, theta );
                        thetaHi = Math.min( thetaHi, theta );
                        phiLo = Math.min( phiLo, phi );
                        phiHi = Math.max( phiHi, phi );
                    }
                }
            }
            if ( phiRange == null ) {
                phiRange = new double[] { phiLo, phiHi };
            }
            if ( thetaRange == null ) {
                thetaRange = new double[] { thetaLo, thetaHi };
            }
        }
        return new SphereAngleRange( phiRange[ 0 ], phiRange[ 1 ],
                                     thetaRange[ 0 ], thetaRange[ 1 ] );
    }

    /**
     * Indicates whether the value zero falls fairly near the center
     * of a given range.
     *
     * @param   dlo  lower limit
     * @param   dhi  upper limit
     * @return  true iff zero is near the center of the specified range
     */
    private static boolean zeroNearCenter( double dlo, double dhi ) {
        double zf = PlotUtil.unscaleValue( dlo, dhi, 0.0, Scale.LINEAR );
        return zf > 0.25 && zf < 0.75;
    }

    /**
     * Returns a value determined by a fixed range and a fractional
     * scale point within it. If the point is zero the minimum value
     * is returned, and if it is one the maximum value is returned. 
     *
     * @param  dlo  scale lower limit
     * @param  dhi  scale upper limit
     * @param  f   fraction
     * @return  scaled value
     */
    private static double frac( double dlo, double dhi, double f ) {
        return dlo + ( dhi - dlo ) * f;
    }
}
