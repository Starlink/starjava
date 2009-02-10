package uk.ac.starlink.ast;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class ThreadTest extends TestCase {

    public void testResample() throws InterruptedException {
        Mapping dmap1 =
            new MathMap( 2, 2,
                         new String[] { "x = a * 2", "y = b * 2" },
                         new String[] { "a = x / 2", "b = y / 2" } );
        Mapping dmap2 = new ZoomMap( 2, 2 );
        Mapping[] dmaps = new Mapping[] { dmap1, dmap2, };
        int nThread = 20;
        ResampleThread[] threads = new ResampleThread[ nThread ];
        for ( int i = 0; i < nThread; i++ ) {
            threads[ i ] =
                new ResampleThread( "Worker " + i, 400,
                                    dmaps[ i % 2 ] );
            threads[ i ].start();
        }
        for ( int i = 0; i < nThread; i++ ) {
            threads[ i ].join();
            threads[ i ].assertOK();
        }
    }

    public void error() {
        try {
            new Frame( 1 ).getC( "Sir Not-appearing-in-this-class" );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__BADAT, e.getStatus() );
        }
    }

    public void resample( Mapping doubleMap, int nx, int ny ) {
        int ndim_in = 2;
        int[] lbnd_in = new int[] { 0, 0 };
        int[] ubnd_in = new int[] { nx - 1, ny - 1 };
        int[] in = new int[ nx * ny ];
        int[] in_var = null;
        for ( int ix = 0; ix < nx; ix++ ) {
            for ( int iy = 0; iy < ny; iy++ ) {
                in[ ix + iy * nx ] = ix + iy * nx;
            }
        }
        Mapping.Interpolator interp = Mapping.Interpolator.nearest();
        boolean usebad = false;
        double tol = 0.1;
        int maxpix = 10;  // low number to work harder.
        int badval = Integer.MIN_VALUE;
        int ndim_out = 2;
        int[] lbnd_out = new int[] { 0, 0 };
        int[] ubnd_out = new int[] { nx * 2 - 1, ny * 2 - 1 };
        int[] lbnd = lbnd_out;
        int[] ubnd = ubnd_out;
        int[] out = new int[ nx * 2 * ny * 2 ];
        int[] out_var = null;

        long start = System.currentTimeMillis();
        int nbad =
            doubleMap.resampleI( ndim_in, lbnd_in, ubnd_in, in, in_var,
                                 interp, usebad, tol, maxpix, badval,
                                 ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                 out, out_var );
        for ( int ix = 1; ix < nx; ix +=4 ) {
            for ( int iy = 1; iy < ny; iy+=4 ) {
                int ixy = in[ ix + iy * nx ];
                int oxy = out[ ( ix * 2 )  + ( iy * 2 ) *  ( nx * 2 ) ];
                assertEquals( ixy, oxy );
            }
        }
    }

    private class ResampleThread extends Thread {
        private AssertionFailedError error_;
        private final int size_;
        private final Mapping doubleMap_;

        ResampleThread( String name, int size, Mapping doubleMap ) {
            super( name );
            size_ = size;
            doubleMap_ = doubleMap;
        }

        public void run() {
            try {
                error();
                resample( doubleMap_, size_, size_ );
                error();
            }
            catch ( AssertionFailedError e ) {
                error_ = e;
            }
        }

        public void assertOK() {
            if ( error_ != null ) {
                throw error_;
            }
        }
    }

    public static void main( String[] args ) throws InterruptedException {
        long now = System.currentTimeMillis();
        new ThreadTest().testResample();
        System.out.println( System.currentTimeMillis() - now );
    }
}
