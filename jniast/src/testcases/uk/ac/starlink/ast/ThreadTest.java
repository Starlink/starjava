package uk.ac.starlink.ast;

import junit.framework.AssertionFailedError;
import uk.ac.starlink.util.TestCase;

public class ThreadTest extends TestCase {

    public ThreadTest( String name ) {
        super( name );
    }

    public void testResample() throws InterruptedException {
        double zoom = 2.0;
        Mapping[] zmaps = createZoomMaps( zoom );
        int nThread = 24;
        ResampleThread[] threads = new ResampleThread[ nThread ];
        for ( int i = 0; i < nThread; i++ ) {
            threads[ i ] =
                new ResampleThread( "Worker " + i, 200,
                                    zmaps[ i % zmaps.length ], zoom );
            threads[ i ].start();
        }
        for ( int i = 0; i < nThread; i++ ) {
            threads[ i ].join();
            threads[ i ].assertOK();
        }
    }

    public void testIntraMap() throws InterruptedException {
        double z = 3.0;
        final IntraMap map = new IntraMap( new ZoomTransformer2( z ) );
        final int np = 2;
        final double[] xin = new double[] { 1, 4 };
        final double[] yin = new double[] { 8, 2 };
        final double[] xout = new double[ np ];
        final double[] yout = new double[ np ];
        Thread thread = new Thread( "IntraThread" ) {
            public void run() {
                double[][] out = map.tran2( np, xin, yin, true );
                System.arraycopy( out[ 0 ], 0, xout, 0, np );
                System.arraycopy( out[ 1 ], 0, yout, 0, np );
            }
        };
        thread.start();
        thread.join();
        assertArrayEquals( new double[] { xin[ 0 ] * z, xin[ 1 ] * z }, xout );
        assertArrayEquals( new double[] { yin[ 0 ] * z, yin[ 1 ] * z }, yout );
    }

    public void error() {
        try {
            new Frame( 1 ).getC( "Sir Not-appearing-in-this-class" );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__BADAT, e.getStatus() );
        }

        AstException e1 = new AstException( "not an error",
                                            AstException.AST__UK1ER );
        AstException e2 = new AstException( "also not an error",
                                            AstException.AST__BADUN );
        assertEquals( AstException.AST__UK1ER, e1.getStatus() );
        assertEquals( "AST__UK1ER", e1.getStatusName() );
        assertEquals( AstException.AST__BADUN, e2.getStatus() );
        assertEquals( "AST__BADUN", e2.getStatusName() );
        assertTrue( e1.getMessage().startsWith( "not an error" ) );
        assertTrue( e2.getMessage().startsWith( "also not an error" ) );

        AstException e3 = new AstException( "Mary had a little lamb, "+
                                            "whoose fleece was white "+
                                            "as snow. And every where" +
                                            "that Mary went the lamb "+
                                            "was sure to go.",
                                            AstException.AST__BADUN );
        assertTrue( e3.getMessage().indexOf( "sure to go." ) > 0 );
    }

    public void resample( Mapping zoomMap, double zoom, int nx, int ny ) {
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
        int izoom = (int) zoom;
        Mapping.Interpolator interp = Mapping.Interpolator.nearest();
        ResampleFlags rflags = new ResampleFlags();
        double tol = 0.1;
        int maxpix = 4;  // low number to work harder.
        int badval = Integer.MIN_VALUE;
        int ndim_out = 2;
        int[] lbnd_out = new int[] { 0, 0 };
        int[] ubnd_out = new int[] { nx * izoom - 1, ny * izoom - 1 };
        int[] lbnd = lbnd_out;
        int[] ubnd = ubnd_out;
        int[] out = new int[ nx * izoom * ny * izoom ];
        int[] out_var = null;

        long start = System.currentTimeMillis();
        int nbad =
            zoomMap.resampleI( ndim_in, lbnd_in, ubnd_in, in, in_var,
                               interp, rflags, tol, maxpix, badval,
                               ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                               out, out_var );
        for ( int ix = 1; ix < nx; ix +=4 ) {
            for ( int iy = 1; iy < ny; iy+=4 ) {
                int ixy = in[ ix + iy * nx ];
                int oxy =
                    out[ ( ix * izoom ) + ( iy * izoom ) * ( nx * izoom ) ];
                assertEquals( ixy, oxy );
            }
        }
    }

    public Mapping[] createZoomMaps( double zoom ) {
        return new Mapping[] {
            new ZoomMap( 2, zoom ),
            new MathMap( 2, 2,
                         new String[]{ "x = a * " + zoom, "y = b * " + zoom },
                         new String[]{ "a = x / " + zoom, "b = y / " + zoom } ),
            new IntraMap( new ZoomTransformer2( 2.0 ) ),
        };
    }

    private class ResampleThread extends Thread {
        private AssertionFailedError error_;
        private final int size_;
        private final Mapping zoomMap_;
        private final double zoom_;

        ResampleThread( String name, int size, Mapping zoomMap, double zoom ) {
            super( name );
            size_ = size;
            zoomMap_ = zoomMap;
            zoom_ = zoom;
        }

        public void run() {
            try {
                error();
                resample( zoomMap_, zoom_, size_, size_ );
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

    private static class ZoomTransformer2 extends Transformer2 {
        private final double zoom_;

        ZoomTransformer2( double zoom ) {
            zoom_ = zoom;
        }

        public double[][] tran2( int npoint, double[] xin, double[] yin,
                                 boolean forward ) {
            double[][] result = new double[ 2 ][ npoint ];
            double zfact = forward ? zoom_ : ( 1.0 / zoom_ );
            for ( int ip = 0; ip < npoint; ip++ ) {
                result[ 0 ][ ip ] = xin[ ip ] * zfact;
                result[ 1 ][ ip ] = yin[ ip ] * zfact;
            }
            return result;
        }

        public String getAuthor() {
            return "Mark";
        }

        public String getContact() {
            return "m.b.taylor@bristol.ac.uk";
        }

        public String getPurpose() {
            return "zooms";
        }
    }

    public static void main( String[] args ) {
        junit.textui.TestRunner.run(ThreadTest.class);
    }
}
