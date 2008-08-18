package uk.ac.starlink.ttools.plot;

import uk.ac.starlink.util.TestCase;

public class MatrixTest extends TestCase {

    public MatrixTest( String name ) {
        super( name );
    }

    public void testDet() {
        assertEquals( 1.0, Matrices.det( unit() ) );
        assertEquals( 27.0, Matrices.det( Matrices.mult( unit(), 3.0 ) ) );
    }

    public void testMult() {
        assertArrayEquals( unit(), Matrices.adj( unit() ) );
        assertArrayEquals( unit(), Matrices.invert( unit() ) );

        assertArrayEquals( Matrices.mult( unit(), 6 ),
                           Matrices.mmMult( Matrices.mult( unit(), 2 ),
                                            Matrices.mult( unit(), 3 ) ) );

        assertEquals( (double) 2*7 + 3*11 + 5*13,
                      Matrices.dot( new double[] { 2, 3, 5 },
                                    new double[] { 7, 11, 13 } ) );

        assertArrayEquals( Matrices.unit( 2 ),
                           Matrices.cross( Matrices.unit( 0 ),
                                           Matrices.unit( 1 ) ) );
        assertArrayEquals( Matrices.mult( Matrices.unit( 2 ), -1. ),
                           Matrices.cross( Matrices.unit( 1 ),
                                           Matrices.unit( 0 ) ) );
    }

    public void testVec() {
        assertEquals( 5., Matrices.mod( new double[] { 3., 0., -4. } ) );
        assertArrayEquals(
            new double[] { 0, -1, 0 },
            Matrices.normalise( new double[] { 0., -Math.PI, 0. } ) );
    }

    public void testInvert() {
        checkInverse( unit() );
        checkInverse( Matrices.mult( unit(), 0.5 ) );
        checkInverse( random() );
    }

    public void checkInverse( double[] m ) {
        assertArrayEquals( Matrices.mmMult( Matrices.invert( m ), m ), unit(),
                           1e-10 );
        assertArrayEquals( unit(), Matrices.mmMult( Matrices.invert( m ), m ),
                           1e-10 );
    }

    public void checkMult( double[] m ) {
        double[] im = Matrices.invert( m );
        assertArrayEquals( m, Matrices.mmMult( Matrices.mmMult( m, m ), im ) );
        assertArrayEquals( m, Matrices.mmMult( im, Matrices.mmMult( m, m ) ) );
    }

    private static double[] unit() {
        double[] m = new double[ 9 ];
        m[ 0 ] = 1.0;
        m[ 4 ] = 1.0;
        m[ 8 ] = 1.0;
        return m;
    }

    private static double[] random() {
        double[] m = new double[ 9 ];
        for ( int i = 0; i < 9; i++ ) {
            m[ i ] = Math.random();
        }
        return m;
    }
}
