package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import uk.ac.starlink.util.FloatList;

/**
 * Provides some implementations of the {@link Shader} interface.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class Shaders {

    /** Fixes red level at parameter value. */
    public static final Shader FIX_RED =
        new FixComponentShader( "Fix Red", 0 );

    /** Fixes green level at parameter value. */
    public static final Shader FIX_GREEN =
        new FixComponentShader( "Fix Green", 1 );

    /** Fixes blue level at parameter value. */
    public static final Shader FIX_BLUE =
        new FixComponentShader( "Fix Blue", 2 );

    /** Scales red level by parameter value. */
    public static final Shader SCALE_RED = 
        new ScaleComponentShader( "Scale Red", 0 );

    /** Scales green level by parameter value. */
    public static final Shader SCALE_GREEN =
        new ScaleComponentShader( "Scale Green", 1 );

    /** Scales blue level by parameter value. */
    public static final Shader SCALE_BLUE =
        new ScaleComponentShader( "Scale Blue", 2 );

    /** Interpolates between red (0) and blue (1). */
    public static final Shader RBSCALE =
        createInterpolationShader( "Red-Blue", Color.RED, Color.BLUE );

    /** Interpolates between black (0) and white (1). */
    public static final Shader GREYSCALE =
        createInterpolationShader( "Greyscale", Color.BLACK, Color.WHITE );

    /** Shader based on lookup table Aips0. */
    public static final Shader LUT_AIPS0;

    /** Shader based on lookup table Backgr. */
    public static final Shader LUT_BACKGR;

    /** Shader based on lookup table Color. */
    public static final Shader LUT_COLOR;

    /** Shader based on lookup table Heat. */
    public static final Shader LUT_HEAT;

    /** Shader based on lookup table IDL2. */
    public static final Shader LUT_IDL2;

    /** Shader based on lookup table IDL4. */
    public static final Shader LUT_IDL4;

    /** Shader based on lookup table Isophot. */
    public static final Shader LUT_ISOPHOT;

    /** Shader based on lookup table Light. */
    public static final Shader LUT_LIGHT;

    /** Shader based on lookup table Manycol. */
    public static final Shader LUT_MANYCOL;

    /** Shader based on lookup table Pastel. */
    public static final Shader LUT_PASTEL;

    /** Shader based on lookup table Rainbow. */
    public static final Shader LUT_RAINBOW;

    /** Shader based on lookup table Ramp. */
    public static final Shader LUT_RAMP;

    /** Shader based on lookup table Random. */
    public static final Shader LUT_RANDOM;

    /** Shader based on lookup table Real. */
    public static final Shader LUT_REAL;

    /** Shader based on lookup table Smooth. */
    public static final Shader LUT_SMOOTH;

    /** Shader based on lookup table Staircase. */
    public static final Shader LUT_STAIRCASE;

    /** Shader based on lookup table Standard. */
    public static final Shader LUT_STANDARD;

    /** Selection of lookup table-based shaders. */
    public final static Shader[] LUT_SHADERS = new Shader[] {
        LUT_AIPS0 = new ResourceLutShader( "AIPS0", "aips0.lut" ),
        LUT_BACKGR = new ResourceLutShader( "Background", "backgr.lut" ),
        LUT_COLOR = new ResourceLutShader( "Colour", "color.lut" ),
        LUT_HEAT = new ResourceLutShader( "Heat", "heat.lut" ),
        LUT_IDL2 = new ResourceLutShader( "IDL2", "idl2.lut" ),
        LUT_IDL4 = new ResourceLutShader( "IDL4", "idl4.lut" ),
        LUT_ISOPHOT = new ResourceLutShader( "Isophot", "isophot.lut" ),
        LUT_LIGHT = new ResourceLutShader( "Light", "light.lut" ),
        LUT_MANYCOL = new ResourceLutShader( "Manycol", "manycol.lut" ),
        LUT_PASTEL = new ResourceLutShader( "Pastel", "pastel.lut" ),
        LUT_RAINBOW = new ResourceLutShader( "Rainbow", "rainbow1.lut" ),
        LUT_RAMP = new ResourceLutShader( "Ramp", "ramp.lut" ),
        LUT_RANDOM = new ResourceLutShader( "Random", "random.lut" ),
        LUT_REAL = new ResourceLutShader( "Real", "real.lut" ),
        LUT_SMOOTH = new ResourceLutShader( "Smooth", "smooth.lut" ),
        LUT_STAIRCASE = new ResourceLutShader( "Staircase", "staircase.lut" ),
        LUT_STANDARD = new ResourceLutShader( "Standard", "standard.lut" ),
    };

    /** Selection of useful shader implementations. */
    public static Shader[] STANDARD_SHADERS = new Shader[] {
        FIX_RED,
        FIX_GREEN,
        FIX_BLUE,
        GREYSCALE,
        RBSCALE,
        LUT_STANDARD,
        LUT_COLOR,
        LUT_HEAT,
        LUT_PASTEL,
        LUT_RAINBOW,
    };

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );
    private final static String LUT_BASE = "/uk/ac/starlink/topcat/colormaps/";

    /**
     * Constructs a shader which interpolates smoothly between two colours.
     *
     * @param  name   name
     * @param  color0  colour corresponding to parameter value 0
     * @param  color1  colour corresponding to parameter value 1
     */
    public static Shader createInterpolationShader( final String name,
                                                    Color color0,
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
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Reads an array of float values stored (as from a DataOutput) at a URL.
     * Each input value must be in the range 0..1 or an IOException is thrown.
     *
     * @param  loc  location of data
     * @return   array of floats each in range 0..1
     * @throws  IOException if there is trouble reading or any values are
     *          out of range
     */
    private static float[] readFloatArray( URL loc ) throws IOException {
        DataInputStream in =
            new DataInputStream( new BufferedInputStream( loc.openStream() ) );
        FloatList flist = new FloatList();
        try {
            while ( true ) {
                float value = in.readFloat();
                if ( value >= 0f && value <= 1f ) {
                    flist.add( value );
                }
                else {
                    throw new IOException( "RGB values out of range" );
                }
            }
        }
        catch ( EOFException e ) {
            return flist.toFloatArray();
        }
        finally {
            in.close();
        }
    }

    /**
     * Shader implementation which fixes one component of the sRGB array
     * at its parameter's value.
     */
    private static class FixComponentShader implements Shader {

        private final String name_;
        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         */
        FixComponentShader( String name, int icomp ) {
            name_ = name;
            icomp_ = icomp;
        }
        
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] = value;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Shader implementation which scales one component of the sRGB array
     * by its parameter's value.
     */
    private static class ScaleComponentShader implements Shader {

        private final String name_;
        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param   icomp  modified component index
         */
        ScaleComponentShader( String name, int icomp ) {
            name_ = name;
            icomp_ = icomp;
        }
 
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] *= value;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Abstract superclass of lookup table-based shader.
     */
    private static abstract class LutShader implements Shader {

        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  shader name
         */
        LutShader( String name ) {
            name_ = name;
        }

        /**
         * Returns the lookup table, which must be a 3N-element array
         * with each element in the range 0..1.
         *
         * @return  lookup table (r1,g1,b1, r2,g2,b2, ...)
         */
        protected abstract float[] getRgbLut();

        public void adjustRgba( float[] rgba, float value ) {
            float[] lut = getRgbLut();
            int nsamp = lut.length / 3;
            int is3 = 3 * Math.round( value * ( nsamp - 1 ) );
            rgba[ 0 ] = lut[ is3 + 0 ];
            rgba[ 1 ] = lut[ is3 + 1 ];
            rgba[ 2 ] = lut[ is3 + 2 ];
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Lookup table-based shader which reads lookup tables data from 
     * raw float arrays stored in resources in the directory 
     * {@link #LUT_BASE}.
     */
    private static class ResourceLutShader extends LutShader {

        private final String resourceName_;
        private float[] lut_;

        /**
         * Constructor.
         *
         * @param  name  shader name
         * @param  resourceName  name of file within resource directory
         *         {@link #LUT_BASE}
         */
        ResourceLutShader( String name, String resourceName ) {
            super( name );
            resourceName_ = resourceName;
        }

        protected float[] getRgbLut() {

            /* Lazily acquire table. */
            if ( lut_ == null ) {
                String loc = LUT_BASE + resourceName_;
                logger_.config( "Reading lookup table at " + loc );
                URL url = Shaders.class.getResource( loc );
                try {
                    if ( url == null ) {
                        throw new FileNotFoundException( "No resource " + loc );
                    }
                    else {
                        lut_ = readFloatArray( url );
                    }
                }
                catch ( IOException e ) {
                    logger_.warning( "No colour map for " + this + ": " + e );
                    lut_ = new float[ 3 ];
                }
            }
            return lut_;
        }
    }
}
