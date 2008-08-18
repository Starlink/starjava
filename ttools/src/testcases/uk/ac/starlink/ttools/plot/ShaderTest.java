package uk.ac.starlink.ttools.plot;

import junit.framework.TestCase;

public class ShaderTest extends TestCase {

    public ShaderTest( String name ) {
        super( name );
    }

    public void testLuts() {
        Shader[] lutShaders = Shaders.LUT_SHADERS;
        float[] rgba = new float[] { 9f, 9f, 9f, 1f, };
        for ( int i = 0; i < lutShaders.length; i++ ) {
            Shader shader = lutShaders[ i ];

            float[] rgba0 = (float[]) rgba.clone();
            float[] rgba5 = (float[]) rgba.clone();
            float[] rgba1 = (float[]) rgba.clone();
            shader.adjustRgba( rgba0, 0f );
            assertIsRgb( rgba0 );
            assertDifferent( rgba, rgba0 );
            shader.adjustRgba( rgba5, 0.5f );
            assertIsRgb( rgba5 );
            assertDifferent( rgba, rgba5 );
            shader.adjustRgba( rgba1, 1f );
            assertIsRgb( rgba1 );
            assertDifferent( rgba, rgba1 );

            assertDifferent( rgba0, rgba5 );
            assertDifferent( rgba5, rgba1 );
        }
    }

    private void assertIsRgb( float[] rgb ) {
        boolean same = true;
        for ( int i = 0; i < 3; i++ ) {
           assertTrue( rgb[ i ] >= 0f );
           assertTrue( rgb[ i ] <= 1f );
        }
    }

    private void assertDifferent( float[] rgb1, float[] rgb2 ) {
        boolean same = true;
        for ( int i = 0; i < 3; i++ ) {
           same = same && rgb1[ i ] == rgb2[ i ];
        }
        assertTrue( ! same );
    }
}
