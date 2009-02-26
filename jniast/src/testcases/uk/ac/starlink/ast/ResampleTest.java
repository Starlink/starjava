package uk.ac.starlink.ast;

import uk.ac.starlink.util.TestCase;

public class ResampleTest extends TestCase {

    public ResampleTest( String name ) {
        super( name );
    }

    public void testRebin() {
        Mapping map = new UnitMap( 1 );
        double wlim = 0.1;
        int ndim_in = 1;
        int[] lbnd_in = new int[] { 0 };
        int[] ubnd_in = new int[] { 4 };
        boolean usebad = true;
        double tol = 0.1;
        int maxpix = 100;
        int ndim_out = 1;
        int[] lbnd_out = new int[] { 0 };
        int[] ubnd_out = new int[] { 4 };
        int[] lbnd = lbnd_out;
        int[] ubnd = ubnd_out;

        double[] inD = new double[] { 1., 2., 3., 4., 5. };
        double[] outD = new double[ 5 ];
        double badD = Double.NaN;
        map.rebinD( wlim, ndim_in, lbnd_in, ubnd_in, inD, null,
                    Mapping.NEAREST_SPREADER,
                    usebad, tol, maxpix, badD, ndim_out, 
                    lbnd_out, ubnd_out, lbnd, ubnd, outD, null );
        assertArrayEquals( new double[] { 1., 2., 3., 4., 5. }, outD );

        float[] inF = new float[] { 1f, 2f, 3f, 4f, 5f };
        float[] outF = new float[ 5 ];
        float badF = Float.NaN;
        new ShiftMap( new double[] { 0.5 } )
           .rebinF( wlim, ndim_in, lbnd_in, ubnd_in, inF, null,
                    Mapping.LINEAR_SPREADER,
                    usebad, tol, maxpix, badF, ndim_out,
                    lbnd_out, ubnd_out, lbnd, ubnd, outF, null );
        assertArrayEquals( new float[] { 0.5f, 1.5f, 2.5f, 3.5f, 2.0f }, outF );
    }

    public void testResample() {
        Mapping map = new UnitMap( 1 );
        int ndim_in = 1;
        int[] lbnd_in = new int[] { 1 };
        int[] ubnd_in = new int[] { 3 };
        double tol = 0.1;
        int maxpix = 100;
        int ndim_out = 1;
        int[] lbnd_out = new int[] { 0 };
        int[] ubnd_out = new int[] { 4 };
        int[] lbnd = lbnd_out;
        int[] ubnd = ubnd_out;
        Mapping.Interpolator interp = Mapping.NEAREST_INTERPOLATOR;
        ResampleFlags flags = new ResampleFlags();
        flags.setUseBad( true );

        double[] inD = new double[] { 1., 1., 1, };
        double[] outD = new double[ 5 ];
        double badD = Double.NaN;
        assertEquals( 2, map.resampleD( ndim_in, lbnd_in, ubnd_in, 
                                        inD, null, interp, 
                                        flags, tol, maxpix, badD,
                                        ndim_out, lbnd_out, ubnd_out,
                                        lbnd, ubnd, outD, null ) );
        assertArrayEquals( new double[] { badD, 1., 1., 1., badD }, outD );

        int[] inI = new int[] { 1, 1, 1 };
        int[] outI = new int[ 5 ];
        int badI = 999;
        flags.setUseBad( false );
        assertEquals( 2, map.resampleI( ndim_in, lbnd_in, ubnd_in,
                                        inI, null, interp,
                                        flags, tol, maxpix, badI,
                                        ndim_out, lbnd_out, ubnd_out,
                                        lbnd, ubnd, outI, null ) );
        assertArrayEquals( new int[] { 999, 1, 1, 1, 999 }, outI );

        float[] inF = new float[] { 2f, 4f, 2f, };
        float[] outF = new float[ 5 ];
        float badF = Float.NaN;
        assertEquals( 1, map.resampleF( ndim_in, lbnd_in, ubnd_in,
                                        inF, null, 
                                        Mapping.Interpolator.blockAve( 1 ),
                                        flags, tol, maxpix, badF,
                                        ndim_out, lbnd_out, ubnd_out,
                                        lbnd, ubnd, outF, null ) );
        assertArrayEquals( outF, new float[] { 2f, 3f, 3f, 2f, badF } );

        short[] inS = new short[] { (short) 1, (short) 2, (short) 3 };
        short[] outS = new short[ 5 ];
        short badS = (short) -1;
        Mapping.Interpolator ukinterp = 
            Mapping.Interpolator.ukern1( new Shift1(), 2 );
        assertEquals( 3, map.resampleS( ndim_in, lbnd_in, ubnd_in,
                                        inS, null, ukinterp,
                                        flags, tol, maxpix, badS,
                                        ndim_out, lbnd_out, ubnd_out,
                                        lbnd, ubnd, outS, null ) );
        assertArrayEquals( new short[] { badS, (short) 2, (short) 3,
                                         badS, badS }, outS );
        

        ResampleFlags xflags = new ResampleFlags();
        xflags.setNoBad( true );
        xflags.setConserveFlux( true );
        try {
            map.resampleS( ndim_in, lbnd_in, ubnd_in, inS, null, ukinterp,
                           xflags, tol, maxpix, badS, ndim_out,
                           lbnd_out, ubnd_out, lbnd, ubnd, outS, null );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__BADFLG, e.getStatus() );
        }

    }

    public void testResampleFlags() {
        ResampleFlags flags = new ResampleFlags();
        assertEquals( 0, flags.getFlagsInt() );
        flags.setNoBad( true );
        assertEquals( AstObject.getAstConstantI( "AST__NOBAD" ),
                      flags.getFlagsInt() );
        flags.setConserveFlux( true );
        assertEquals( AstObject.getAstConstantI( "AST__NOBAD" )
                    | AstObject.getAstConstantI( "AST__CONSERVEFLUX" ),
                      flags.getFlagsInt() );
        flags.setNoBad( false );
        assertEquals( AstObject.getAstConstantI( "AST__CONSERVEFLUX" ),
                      flags.getFlagsInt() );
        assertTrue( flags.getConserveFlux() );
        assertTrue( ! flags.getNoBad() );
    }

    private static class Shift1 implements Ukern1Calculator {
        public double ukern1( double offset ) {
            return ( offset > 0. && offset <= 1. )
                 ? 1.0
                 : 0.0;
        }
    }
}
