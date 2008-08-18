package uk.ac.starlink.topcat.plot;

import junit.framework.TestCase;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.StyleSet;

public class StyleTest extends TestCase {

    public StyleTest( String name ) {
        super( name );
    }

    public void testDensityShaders() {
        Shader[] shaders = DensityWindow.INDEXED_SHADERS;
        for ( int i = 0; i < shaders.length; i++ ) {
            assertTrue( shaders[ i ].isAbsolute() );
        }
    }

    public void testMarkStyles() {
        checkProfiles( GraphicsWindow.getStandardMarkStyleSets() );
    }

    private void checkProfiles( StyleSet[] profiles ) {
        for ( int i = 0; i < profiles.length; i++ ) {
            StyleSet profile = profiles[ i ];
            for ( int j = 0; j < 16; j++ ) { 
                MarkStyle style = (MarkStyle) profile.getStyle( j );

                assertEquals( style, profile.getStyle( j ) );
                for ( int k = 0; k < 16; k++ ) {
                    if ( k == j ) {
                        assertEquals( style, profile.getStyle( k ) );
                    }
                    else {
                        assertTrue( style != profile.getStyle( k ) );
                    }
                }

                MarkStyle copy = style.getShapeId()
                                .getStyle( style.getColor(), style.getSize() );
                copy.setOpaqueLimit( style.getOpaqueLimit() );
                assertEquals( style, copy );
            }
        }
    }
}
