package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Provides some implementations of the {@link Shader} interface.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class Shaders {

    /** Fixes red level at parameter value. */
    public static Shader FIX_RED = new FixComponentShader( 0 );

    /** Fixes green level at parameter value. */
    public static Shader FIX_GREEN = new FixComponentShader( 1 );

    /** Fixes blue level at parameter value. */
    public static Shader FIX_BLUE = new FixComponentShader( 2 );

    /** Scales red level by parameter value. */
    public static Shader SCALE_RED = new ScaleComponentShader( 0 );

    /** Scales green level by parameter value. */
    public static Shader SCALE_GREEN = new ScaleComponentShader( 1 );

    /** Scales blue level by parameter value. */
    public static Shader SCALE_BLUE = new ScaleComponentShader( 2 );

    /** Interpolates between red (0) and blue (1). */
    public static Shader RBSCALE =
        createInterpolationShader( Color.RED, Color.BLUE );

    /** Interpolates between black (0) and white (1). */
    public static Shader GREYSCALE =
        createInterpolationShader( Color.BLACK, Color.WHITE );

    /** Selection of useful shader implementations. */
    public static Shader[] STANDARD_SHADERS = new Shader[] {
        FIX_RED,
        FIX_GREEN,
        FIX_BLUE,
        GREYSCALE,
        RBSCALE,
    };

    /**
     * Constructs a shader which interpolates smoothly between two colours.
     *
     * @param  color0  colour corresponding to parameter value 0
     * @param  color1  colour corresponding to parameter value 1
     */
    public static Shader createInterpolationShader( Color color0,
                                                    Color color1 ) {
        final float[] rgba0 = color0.getRGBComponents( null );
        final float[] rgba1 = color1.getRGBComponents( null );
        return new Shader() {
            public void adjustRgba( float[] rgba, float value ) {
                for ( int i = 0; i < 4; i++ ) {
                    float f0 = rgba0[ i ];
                    float f1 = rgba1[ i ];
                    rgba[ i ] = f0 + ( f1 - f0 ) * value;
                }
            }
        };
    }

    /**
     * Shader implementation which fixes one component of the sRGB array
     * at its parameter's value.
     */
    private static class FixComponentShader implements Shader {

        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  icomp   modified component index
         */
        FixComponentShader( int icomp ) {
            icomp_ = icomp;
        }
        
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] = value;
        }
    }

    /**
     * Shader implementation which scales one component of the sRGB array
     * by its parameter's value.
     */
    private static class ScaleComponentShader implements Shader {

        private final int icomp_;

        /**
         * Constructor.
         *
         * @param   icomp  modified component index
         */
        ScaleComponentShader( int icomp ) {
            icomp_ = icomp;
        }
 
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] *= value;
        }
    }
}
