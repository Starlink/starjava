package uk.ac.starlink.ttools.plot2.layer;

import java.util.Random;
import java.util.logging.Level;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;

public class KernelTest extends TestCase {

    private Random rand_ = new Random( 235089454L );

    public KernelTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testKernels() {
        for ( Kernel1dShape kshape :
              StandardKernel1dShape.getStandardOptions() ) {
            for ( double width : new double[] { 0.0, 1.0, 4.25, 12.8 } ) {
                checkExactNormKernel( kshape.createFixedWidthKernel( width ) );
            }
            checkUnitKernel( kshape.createFixedWidthKernel( 0 ) );

            /* Hard to know what checks to do here, since the sum over bins
             * is no longer constant.  At least check you can create them. */
            for ( int k = 0; k < 5; k++ ) {
                kshape.createKnnKernel( k, false, 2, 10 );
                kshape.createKnnKernel( k, true, 1, 18 );
            }
        }
    }

    public void testKnnCombiner() throws ConfigException {

        /* Check that the Combiner must be extensive for the KNN plotter.
         * The logic in that class implicitly relies on it, since there
         * is currently no support for an averaging kernel, only a
         * summing one.  An averaging KNN kernel probably could be
         * implemented, but it seems like a bit of a specialist requirement,
         * at least wait until somebody asks for one. */
        KnnKernelDensityPlotter knnPlotter = null;
        for ( Plotter plotter : PlanePlotType.getInstance().getPlotters() ) {
            if ( plotter instanceof KnnKernelDensityPlotter ) {
                knnPlotter = (KnnKernelDensityPlotter) plotter;
            }
        }
        assertNotNull( knnPlotter );
        ConfigKey<Combiner> combinerKey = null;
        for ( ConfigKey key : knnPlotter.getStyleKeys() ) {
            if ( Combiner.class.isAssignableFrom( key.getValueClass() ) ) {
                combinerKey = (ConfigKey<Combiner>) key;
            }
        }
        assertNull( combinerKey );
        ConfigMap config = new ConfigMap();
        config.put( knnPlotter.MINSIZER_CKEY,
                    BinSizer.createCountBinSizer( 15 ) );
        config.put( knnPlotter.MAXSIZER_CKEY,
                    BinSizer.createCountBinSizer( 3 ) );
        KnnKernelDensityPlotter.KDenseStyle style =
            knnPlotter.createStyle( config );
        assertTrue( knnPlotter.getCombiner( style ).getType().isExtensive() );
    }

    private void checkExactNormKernel( Kernel1d kernel ) {
        checkNormKernel( kernel, kernel.getExtent() );
    }

    private void checkNormKernel( Kernel1d kernel, double ext ) {
        int extent = (int) Math.ceil( ext );
        for ( int ns = 1; ns < 20; ns++ ) {
            double sum = 0;
            int np = ns + extent * 2;
            double[] in = new double[ np ];
            int nd = rand_.nextInt( 10 );
            for ( int id = 0; id < nd; id++ ) {
                in[ extent + rand_.nextInt( ns ) ]++;
                sum++;
            }
            double[] out = kernel.convolve( in );
            assertEquals( in.length, out.length );
            double tin = 0;
            double tout = 0;
            for ( int ip = 0; ip < np; ip++ ) {
                tin += in[ ip ];
                tout += out[ ip ];
            }
            assertEquals( sum, tin );
            assertEquals( sum, tout, sum * 1e-10 );
        }
    }

    private void checkUnitKernel( Kernel1d kernel ) {
        for ( int ns = 1; ns < 20; ns++ ) {
            double[] in = new double[ ns ];
            int nd = rand_.nextInt( 10 );
            for ( int id = 0; id < nd; id++ ) {
                in[ rand_.nextInt( ns ) ]++;
            }
            double[] out = kernel.convolve( in );
            assertArrayEquals( in, out );
        }
    }
}
