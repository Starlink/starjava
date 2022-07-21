package uk.ac.starlink.ttools.plot2;

import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.util.LogUtils;

public class ScalingTest extends TestCase {

    private final double err = 1e-8;

    public ScalingTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testAsinh() {
        double delta = 0.125;
        Scaling.RangeScaling scaling =
            Scalings.createAsinhScaling( "Test", delta );
        Scaler s10 = scaling.createScaler( 0, 10 );
        assertEquals( 0.0, s10.scaleValue( 0 ), err );
        assertEquals( delta, s10.scaleValue( 1 ), err );
        assertEquals( 1.0, s10.scaleValue( 10 ), err );
    }

    public void testRange() {
        for ( Scaling scaling : Scaling.STRETCHES ) {
            if ( scaling instanceof Scaling.RangeScaling ) {
                Scaling.RangeScaling rscaling = (Scaling.RangeScaling) scaling;
                checkScaling( rscaling, 10, 323 );
                checkScaling( rscaling, Math.PI, 5e9 );
            }
            // non-positive values don't work well with e.g. LOG & LINEAR
        }
        checkScaling( Scaling.AUTO, 10, 323 );
    }

    private void checkScaling( Scaling.RangeScaling scaling,
                               double lo, double hi ) {
        Scaler scaler = scaling.createScaler( lo, hi );
        assertEquals( 0.0, scaler.scaleValue( lo ), err );
        assertEquals( 0.0, scaler.scaleValue( lo - 1 ), err );
        assertEquals( 1.0, scaler.scaleValue( hi ), err );
        assertEquals( 1.0, scaler.scaleValue( hi + 1 ), err );
        double mid = scaler.scaleValue( ( hi + lo ) * 0.5 );
        assertTrue( mid > 0.01 && mid < 0.99 );

        for ( int i = 0; i < 16; i++ ) {
            double frac = i / 16.0;
            double dFrac = Scalings.unscale( scaler, lo, hi, frac );
            assert Math.abs( scaler.scaleValue( dFrac ) - frac ) <= 0.001;
        }
    }
}
