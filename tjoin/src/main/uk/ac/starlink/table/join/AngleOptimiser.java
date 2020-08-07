package uk.ac.starlink.table.join;

import java.util.logging.Logger;

abstract class AngleOptimiser {

    private final double tol_;
    private final int maxIts_;
    private final int nRestart_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    public AngleOptimiser( double tol, int maxIts, int nRestart ) {
        tol_ = tol;
        maxIts_ = maxIts;
        nRestart_ = nRestart;
    }

    public abstract double[] calcDerivs( double phi );

    private double findMinimum( double phi0 ) {
        for ( int ir = 0; ir < nRestart_; ir++ ) {
            double phi = phi0 + Math.PI * 2.0 * ir / nRestart_;
            double min = findExtremum( phi, Boolean.TRUE );
            if ( ! Double.isNaN( min ) ) {
                if ( ir > 0 ) {
                    logger_.info( "Minimisation required " + ir + " restarts" );
                }
                return min;
            }
        }
        logger_.warning( "Fail to find any minimum" );
        return Double.NaN;
    }

    public double findExtremum( double phi0, Boolean requireMin ) {
        double phi = phi0;
        int n = 0;
        while ( true ) {
            double[] derivs = calcDerivs( phi );
            double dphi = derivs[ 1 ] / derivs[ 2 ];
            if ( ! ( Math.abs( dphi ) < Math.PI * 0.5 ) ) {
                dphi = Math.signum( dphi ) * Math.PI * 0.01;
            }
            phi = ( phi - dphi + 2.0 * Math.PI ) % ( 2.0 * Math.PI );
            if ( Math.abs( dphi ) < tol_ ) {
                if ( requireMin == null ) {
                    return phi;
                }
                if ( derivs[ 2 ] == 0 ) {
                    return Double.NaN;
                }
                boolean reqMin = requireMin.booleanValue();
                boolean isMin = derivs[ 2 ] > 0;
                assert Math.signum( calcDerivs( phi + 10 * tol_ )[ 0 ]
                                  - calcDerivs( phi )[ 0 ] )
                       == ( isMin ? +1 : -1 );
                assert Math.signum( calcDerivs( phi - 10 * tol_ )[ 0 ]
                                  - calcDerivs( phi )[ 0 ] )
                       == ( isMin ? +1 : -1 );
                return reqMin == isMin ? phi : Double.NaN;
            }
            if ( n++ > maxIts_ ) {
                return Double.NaN;
            }
        }
    }
}
