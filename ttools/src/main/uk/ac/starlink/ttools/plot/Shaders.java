package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import uk.ac.starlink.util.FloatList;

/**
 * Provides some implementations of the {@link Shader} interface.
 *
 * <p>Many other lookup tables are available.
 * See for instance
 * <a href="http://www.ncl.ucar.edu/Document/Graphics/color_table_gallery.shtml"
 *    >http://www.ncl.ucar.edu/Document/Graphics/color_table_gallery.shtml</a>.
 * The utility class {@link LutSteal} is available to help turn these into
 * lut files that can be used here.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class Shaders {

    /**
     * Property containing a File.pathSeparator-separated list of text
     * files containing custom lookup tables.  Each line should contain
     * three space-separated floating point values between zero and one
     * giving the Red, Green and Blue components of a colour.
     * @see  #getCustomShaders
     */
    public static final String LUTFILES_PROPERTY = "lut.files";

    /** Fixes red level at parameter value. */
    public static final Shader FIX_RED =
        new FixRGBComponentShader( "RGB Red", 0 );

    /** Fixes green level at parameter value. */
    public static final Shader FIX_GREEN =
        new FixRGBComponentShader( "RGB Green", 1 );

    /** Fixes blue level at parameter value. */
    public static final Shader FIX_BLUE =
        new FixRGBComponentShader( "RGB Blue", 2 );

    /** Scales red level by parameter value. */
    public static final Shader SCALE_RED = 
        new ScaleRGBComponentShader( "Scale Red", 0 );

    /** Scales green level by parameter value. */
    public static final Shader SCALE_GREEN =
        new ScaleRGBComponentShader( "Scale Green", 1 );

    /** Scales blue level by parameter value. */
    public static final Shader SCALE_BLUE =
        new ScaleRGBComponentShader( "Scale Blue", 2 );

    /** Fixes Y in YUV colour space. */
    public static final Shader FIX_Y = new YuvShader( "YUV Y", 0, true );

    /** Fixes U in YUV colour space. */
    public static final Shader FIX_U = new YuvShader( "YUV U", 1, true );

    /** Fixes V in YUV colour space. */
    public static final Shader FIX_V = new YuvShader( "YUV V", 2, true );

    /** Scales Y in YUV colour space. */
    public static final Shader SCALE_Y = new YuvShader( "Scale YUV Y",
                                                        0, false );

    /** Fixes H in HSV colour space. */
    public static final Shader HSV_H = new HsvShader( "HSV H", 0, true );
 
    /** Fixes S in HSV colour space. */
    public static final Shader HSV_S = new HsvShader( "HSV S", 1, true );

    /** Fixes V in HSV colour space. */
    public static final Shader HSV_V = new HsvShader( "HSV V", 2, true );

    /** Scales H in HSV colour space. */
    public static final Shader SCALE_H =
        new HsvShader( "Scale HSV H", 0, false );

    /** Scales S in HSV colour space. */
    public static final Shader SCALE_S =
        new HsvShader( "Scale HSV S", 1, false );

    /** Scales V in HSV colour space. */
    public static final Shader SCALE_V =
        new HsvShader( "Scale HSV V", 2, false );

    /** Interpolates between red (0) and blue (1). */
    public static final Shader RED_BLUE =
        createInterpolationShader( "Red-Blue", Color.RED, Color.BLUE );

    /** Interpolates between cyan (0) and magenta (1). */
    public static final Shader CYAN_MAGENTA =
        createInterpolationShader( "Cyan-Magenta", Color.CYAN, Color.MAGENTA );

    /** Interpolates between white (0) and black (1). */
    public static final Shader WHITE_BLACK =
        createInterpolationShader( "Greyscale", Color.WHITE, Color.BLACK );

    /** Interpolates between black (0) and white (1). */
    public static final Shader BLACK_WHITE =
        createInterpolationShader( "Greyscale", Color.BLACK, Color.WHITE );

    /** Rainbow shader copied from SRON technical note SRON/EPS/TN/09-002. */
    public static final Shader SRON_RAINBOW =
        createSronRainbowShader( "SRON" );

    /** Standard cubehelix shader. */
    public static final Shader CUBEHELIX =
        createCubehelixShader( "Cubehelix", 0.5, -1.5, 1.0, 1.0 );

    /** Enhanced hue cubehelix shader. */
    public static final Shader CUBEHELIX2 =
        createCubehelixShader( "Cubehelix2", 0.5, -1.5, 1.5, 1.0 );

    /** Lurid cubehelix shader. */
    public static final Shader CUBEHELIX3 =
        createCubehelixShader( "Cubehelix3", 2.0, 1.0, 3.0, 1.0 );

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

    /** Shader based on lookup table Accent. */
    public static final Shader LUT_ACCENT;

    /** Shader based on lookup table Cold. */
    public static final Shader LUT_COLD;

    /** Shader copied from glNemo2 application. */
    public static final Shader LUT_GLNEMO2;

    /** Shader copied from Matplotlib Gnuplot lookup table. */
    public static final Shader LUT_GNUPLOT;

    /** Shader copied from Matplotlib Gnuplot2 lookup table. */
    public static final Shader LUT_GNUPLOT2;

    /** Shader copied from SPECX blue2yellow lookup table. */
    public static final Shader LUT_SPECXB2Y;

    /** Shader copied from Matplotlib BRG lookup table. */
    public static final Shader LUT_BRG;

    /** Shader copied from Matplotlib Paired lookup table. */
    public static final Shader LUT_PAIRED;

    /** Shader copied from Matplotlib gist_rainbow lookup table. */
    public static final Shader LUT_RAINBOW3;

    /** Shader copied from Matplotlib Set1 lookup table. */
    public static final Shader LUT_SET1;

    /** Shader copied from Matplotlib 2.0 Magma lookup table. */
    public static final Shader LUT_MPL2MAGMA;

    /** Shader copied from Matplotlib 2.0 Inferno lookup table. */
    public static final Shader LUT_MPL2INFERNO;

    /** Shader copied from Matplotlib 2.0 Plasma lookup table. */
    public static final Shader LUT_MPL2PLASMA;

    /** Shader copied from Matplotlib 2.0 Viridis lookup table. */
    public static final Shader LUT_MPL2VIRIDIS;

    /** Diverging hot-cold shader provided by Daniel Michalik. */
    public static final Shader LUT_HOTCOLD;

    /** Cividis shader derived from DOI:10.1371/journal.pone.0199239. */
    public static final Shader LUT_CIVIDIS;

    /** Painbow shader scraped from https://xkcd.com/2537/.  Thanks Randall. */
    public static final Shader LUT_PAINBOW;

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
        LUT_ACCENT = new ResourceLutShader( "Accent", "accent.lut" ),
        LUT_COLD = new ResourceLutShader( "Cold", "cold.lut" ),
        LUT_GLNEMO2 = new ResourceLutShader( "Rainbow2", "glnemo2.lut" ),
        LUT_GNUPLOT = new ResourceLutShader( "Gnuplot", "MPL_gnuplot.lut" ),
        LUT_GNUPLOT2 = new ResourceLutShader( "Gnuplot2", "MPL_gnuplot2.lut" ),
        LUT_SPECXB2Y = new ResourceLutShader( "SpecxBY", "specxbl2yel.lut" ),
        LUT_BRG = new ResourceLutShader( "BRG", "brg.lut" ),
        LUT_RAINBOW3 = new ResourceLutShader( "Rainbow3", "gist_rainbow.lut" ),
        LUT_PAIRED = new ResourceLutShader( "Paired", "paired.lut" ),
        LUT_SET1 = new ResourceLutShader( "Set1", "set1.lut" ),
        LUT_MPL2MAGMA = new ResourceLutShader( "Magma", "mpl2_magma.lut" ),
        LUT_MPL2INFERNO = new ResourceLutShader( "Inferno",
                                                 "mpl2_inferno.lut" ),
        LUT_MPL2PLASMA = new ResourceLutShader( "Plasma", "mpl2_plasma.lut" ),
        LUT_MPL2VIRIDIS = new ResourceLutShader( "Viridis",
                                                 "mpl2_viridis.lut" ),
        LUT_CIVIDIS = new ResourceLutShader( "Cividis", "cividis.lut" ),
        LUT_HOTCOLD = new ResourceLutShader( "HotCold", "hotcold.lut" ),
        LUT_PAINBOW = new ResourceLutShader( "Painbow", "painbow.lut" ),
    };

    /* ColorBrewer.
     * Colors from www.colorbrewer.org by Cynthia A Brewer,
     * Geography, Pennsylvania State University.
     * These are intented for cartography. */

    /** ColorBrewer sequential blue-green shader. */
    public static final Shader BREWER_BUGN =
        new SampleShader( "BuGn", new int[] {
            0xedf8fb, 0xccece6, 0x99d8c9, 0x66c2a4, 0x2ca25f, 0x006d2c, } );

    /** ColorBrewer sequential blue-purple shader. */
    public static final Shader BREWER_BUPU =
        new SampleShader( "BuPu", new int[] {
            0xedf8fb, 0xbfd3e6, 0x9ebcda, 0x8c96c6, 0x8856a7, 0x810f7c, } );

    /** ColorBrewer sequential orange-red shader. */
    public static final Shader BREWER_ORRD =
        new SampleShader( "OrRd", new int[] {
            0xfef0d9, 0xfdd49e, 0xfdbb84, 0xfc8d59, 0xe34a33, 0xb30000, } );

    /** ColorBrewer sequential purple-blue shader. */
    public static final Shader BREWER_PUBU =
        new SampleShader( "PuBu", new int[] {
            0xf1eef6, 0xd0d1e6, 0xa6bddb, 0x74a9cf, 0x2b8cbe, 0x045a8d, } );

    /** ColorBrewer sequential purple-red shader. */
    public static final Shader BREWER_PURD =
        new SampleShader( "PuRd", new int[] {
            0xf1eef6, 0xd4b9da, 0xc994c7, 0xdf65b0, 0xdd1c77, 0x980043, } );

    /** ColorBrewer diverging red-blue shader. */
    public static final Shader BREWER_RDBU =
        new SampleShader( "RdBu", new int[] {
            0xb2182b, 0xd6604d, 0xf4a582, 0xfddbc7,
            0xd1e5f0, 0x92c5de, 0x4393c3, 0x2166ac, } );

    /** ColorBrewer diverging pink-green shader. */
    public static final Shader BREWER_PIYG =
        new SampleShader( "PiYG", new int[] {
            0xc51b7d, 0xde77ae, 0xf1b6da, 0xfde0ef,
            0xe6f5d0, 0xb8e186, 0x7fbc41, 0x4d9221, } );

    /** ColorBrewer diverging brown-blue-green shader. */
    public static final Shader BREWER_BRBG =
        new SampleShader( "BrBG", new int[] {
            0x8c510a, 0xbf812d, 0xdfc27d, 0xf6e8c3,
            0xc7eae5, 0x80cdc1, 0x35978f, 0x01665e, } );

    /** Hue-chroma-luminance cyclic shader.
     *  Got from hclwizard.org with parameters:
     *  qualitative, H1=0 H2=351, N=40, C1=90, L1=60 */
    public static final Shader HCL_POLAR =
        new SampleShader( "HueCL", new int[] {
            0xE96485, 0xE56972, 0xE16F5E, 0xDB7546, 0xD47B24, 0xCB8000,
            0xC28600, 0xB88B00, 0xAC9000, 0x9F9500, 0x909900, 0x7F9D00,
            0x6BA100, 0x51A400, 0x25A702, 0x00AA37, 0x00AC51, 0x00AE67,
            0x00AF7A, 0x00B08B, 0x00B09B, 0x00AFAA, 0x00AEB8, 0x00ACC5,
            0x00A9D0, 0x00A4DB, 0x009FE3, 0x0099EA, 0x4B92EE, 0x748AF1,
            0x9281F1, 0xA978EE, 0xBB70EA, 0xCA68E3, 0xD662DA, 0xDF5DCF,
            0xE55BC3, 0xE95BB5, 0xEB5CA7, 0xEB6096, 
        } );

    /** Shader used by default for the transverse axis of non-absolute ramps. */
    public static final Shader DFLT_GRID_SHADER = LUT_BRG;
    
    /** Base directory for locating binary colour map lookup table resources. */
    private final static String LUT_BASE = "/uk/ac/starlink/ttools/colormaps/";

    private static Shader[] customShaders_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Basic abstract partial shader implementation.
     */
    private static abstract class BasicShader implements Shader {

        private final String name_;
        private final boolean isAbsolute_;

        /**
         * Constructs an absolute shader (one with no dependence on original
         * colour).
         *
         * @param   name  shader name
         */
        BasicShader( String name ) {
            this( name, true );
        }

        /**
         * Constructs a shader that may or may not be absolute.
         *
         * @param  name  shader name
         * @param  isAbsolute  true iff shader output is not dependent
         *                     on input colour
         */
        BasicShader( String name, boolean isAbsolute ) {
            name_ = name;
            isAbsolute_ = isAbsolute;
        }

        public String getName() {
            return name_;
        }

        public boolean isAbsolute() {
            return isAbsolute_;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Abstract Shader implementation for setting components in foreign
     * (non-RGB) colour spaces.
     */
    private static abstract class ColorSpaceComponentShader 
                                  extends BasicShader {

        private final int icomp_;
        private final boolean fix_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         * @param  fix    if true, component is fixed at given value,
         *                if false, it is scaled to given value
         */
        ColorSpaceComponentShader( String name, int icomp, boolean fix ) {
            super( name, false );
            icomp_ = icomp;
            fix_ = fix;
        }

        /**
         * Converts RGB array to foreign colour space.
         *
         * @param  rgb  colour component array; on entry and exit all elements
         *         must be in the range 0..1
         */
        protected abstract void toSpace( float[] rgb );

        /**
         * Converts foreign colour space array to RGB.
         *
         * @param  abc  colour component array; on entry and exit all elements
         *         must be in the range 0..1
         */
        protected abstract void fromSpace( float[] abc );

        public void adjustRgba( float[] rgba, float value ) {
            toSpace( rgba );
            rgba[ icomp_ ] = fix_ ? value
                                  : value * rgba[ icomp_ ];
            fromSpace( rgba );
        }
    }

    /** Shader which does nothing. */
    public static final Shader NULL = new BasicShader( "None", false ) {
        public void adjustRgba( float[] rgba, float value ) {
        }
    };

    /** Scales alpha channel by parameter value. */
    public static final Shader TRANSPARENCY =
            new BasicShader( "Transparency", false ) {
        public void adjustRgba( float[] rgba, float value ) {

            /* Squaring seems to adjust the range better. */
            rgba[ 3 ] *= value * value;
        }
    };

    /** Shader which sets intensity. */
    public static final Shader FIX_INTENSITY =
            new BasicShader( "Intensity", false ) {
        public void adjustRgba( float[] rgba, float value ) {
            float max = Math.max( rgba[ 0 ], Math.max( rgba[ 1 ], rgba[ 2 ] ) );
            float m1 = 1f / max;
            for ( int i = 0; i < 3; i++ ) {
                rgba[ i ] = 1f - value * ( 1f - rgba[ i ] * m1 );
            }
        }
    };

    /** Shader which scales intensity. */
    public static final Shader SCALE_INTENSITY =
            new BasicShader( "Scale Intensity", false ) {
        public void adjustRgba( float[] rgba, float value ) {
            for ( int i = 0; i < 3; i++ ) {
                rgba[ i ] = 1f - value * ( 1f - rgba[ i ] );
            }
        }
    };

    /** Shader which interpolates between the base colour and black. */
    public static final Shader FADE_BLACK =
        new FadeShader( "Blacker", Color.BLACK );

    /** Shader which interpolates between the base colour and white. */
    public static final Shader FADE_WHITE =
        new FadeShader( "Whiter", Color.WHITE );

    /** Shader which fixes hue. */
    public static final Shader FIX_HUE = new BasicShader( "Hue" ) {
        public void adjustRgba( float[] rgba, float value ) {
            float h = value * 359.99f;
            float h6 = h / 60f;
            int hi = (int) h6;
            float f = h6 - hi;
            float s = 1f;
            float v = 1f;
            float p = v * ( 1f - s );
            float q = v * ( 1f - f * s );
            float t = v * ( 1f - ( 1f - f ) * s );
            float r;
            float g;
            float b;
            switch ( hi ) {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                case 5: r = v; g = p; b = q; break;
                default: r = 0; g = 0; b = 0;
            }
            rgba[ 0 ] = r;
            rgba[ 1 ] = g;
            rgba[ 2 ] = b;
        }
    };

    /**
     * Constructs a shader which interpolates smoothly between two colours.
     *
     * @param  name   name
     * @param  color0  colour corresponding to parameter value 0
     * @param  color1  colour corresponding to parameter value 1
     * @return  shader
     */
    public static Shader createInterpolationShader( String name,
                                                    Color color0,
                                                    Color color1 ) {
        final float[] rgba0 = color0.getRGBComponents( null );
        final float[] rgba1 = color1.getRGBComponents( null );

        /* Optimisation, since calculations are simpler for two colours.
         * This still benefits from the equality semantics of
         * InterpolationShader though. */
        return new InterpolationShader( name, new Color[] { color0, color1 } ) {
            @Override
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
     * Constructs a shader which interpolates between a sequence of colours.
     * The shading is defined by a sequence of linear segments.
     *
     * @param  name   name
     * @param  colors  sequence of colours; value 0 corresponds to the
     *                 first colour and value 1 corresponds to the last
     * @return  shader
     */
    public static Shader createInterpolationShader( final String name,
                                                    Color[] colors ) {
        return colors.length == 2
             ? createInterpolationShader( name, colors[ 0 ], colors[ 1 ] )
             : new InterpolationShader( name, colors );
    }

    /**
     * Constructs a shader to give the rainbow colour map favoured
     * (though only above other rainbow colour maps) by SRON.
     * SRON is the Netherlands Institute for Space Research.
     * This colourmap is described in document SRON/EPS/TN/09-002,
     * by Paul Tol.  It's in equation 3 of that paper.
     *
     * @param  name  colour map name
     * @return  colour map
     * @see   <a href="https://personal.sron.nl/~pault/">Paul Tol's notes</a>
     */
    private static Shader createSronRainbowShader( String name ) {
        return new BasicShader( name ) {
            public void adjustRgba( float[] rgba, float value ) {
                double x = value;
                double x2 = x * x;
                double x3 = x * x2;
                double x4 = x * x3;
                double x5 = x * x4;
                double x6 = x * x5;
                double r = ( 0.472 - 0.567*x + 4.05*x2 )
                         / ( 1 + 8.72*x - 19.17*x2 + 14.1*x3);
                double g = 0.108932 - 1.22635*x + 27.284*x2
                         - 98.577*x3 + 163.3*x4 - 131.395*x5 + 40.634*x6;
                double b = 1./( 1.97 + 3.54*x - 68.5*x2 + 243*x3
                              - 297*x4 + 125*x5 );
                rgba[ 0 ] = (float) r;
                rgba[ 1 ] = (float) g;
                rgba[ 2 ] = (float) b;
            }
        };
    }

    /**
     * Create one of the cubehelix family of shaders, devised by Dave Green.
     *
     * @param  name  shader name
     * @param  start  colour (1=red, 2=green, 3=blue; 0.5=purple)
     * @param  rots   rotations in colour, (typically -1.5 to 1.5,
     *                -1.0 is one blue-green-red cycle
     * @param  hue    for hue intensity scaling (in the range 0.0 (B+W) to 1.0;
     *                to be strictly correct, larger values may be OK with
     *                particular start/end colours
     * @param  gamma  set the gamma correction for intensity
     * @return  shader
     * @see <a href="http://adsabs.harvard.edu/abs/2011BASI...39..289G"
     *                                            >2011BASI...39..289G</a>
     * @see <a href="https://www.mrao.cam.ac.uk/~dag/CUBEHELIX/"
     *              >https://www.mrao.cam.ac.uk/~dag/CUBEHELIX/</a>
     */
    private static Shader createCubehelixShader( String name,
                                                 final double start,
                                                 final double rots,
                                                 final double hue,
                                                 final double gamma ) {
        final double third = 1.0 / 3.0;
        final boolean gamma1 = gamma == 1;
        return new BasicShader( name ) {
            public void adjustRgba( float[] rgba, float value ) {
                double angle =
                    2.0 * Math.PI * ( start * third + 1.0 + rots * value );
                double fract = gamma1 ? value : Math.pow( value, gamma );
                double amp = hue * fract * 0.5 * ( 1.0 - fract );
                double c = Math.cos( angle );
                double s = Math.sin( angle );
                rgba[ 0 ] =
                    (float) ( fract + amp * ( -0.14861 * c + 1.78277 * s ) );
                rgba[ 1 ] =
                    (float) ( fract + amp * ( -0.29227 * c - 0.90649 * s ) );
                rgba[ 2 ] =
                    (float) ( fract + amp * (  1.97294 * c ) );
                for ( int i = 0; i < 3; i++ ) {
                    rgba[ i ] = Math.max( 0f, Math.min( 1f, rgba[ i ] ) );
                }
            }
        };
    }

    /**
     * Creates a shader which always returns a fixed colour regardless of the
     * supplied parameter.
     *
     * @param  name  shader name
     * @param  color  fixed output colour
     * @return   fixed colour shader
     */
    public static Shader createFixedShader( final String name, Color color ) {
        final float[] fixedRgba = color.getRGBComponents( new float[ 4 ] );
        return new BasicShader( name ) {
            public void adjustRgba( float[] rgba, float value ) {
                for ( int i = 0; i < 4; i++ ) {
                    rgba[ i ] = fixedRgba[ i ];
                }
            }
        };
    }

    /**
     * Creates a shader which for each value either does nothing to a colour
     * or turns it completely transparent.  The pass range is specified
     * by a minimum and maximum value and a sense.
     *
     * @param  name  shader name
     * @param  minMask  minimum value (exclusive) for range
     * @param  maxMask  maximum value (exclusive) for range
     * @param  sense  if true, values outside the range are transparent,
     *                if false, values within the range are transparent
     */
    public static Shader createMaskShader( String name,
                                           final float minMask,
                                           final float maxMask,
                                           final boolean sense ) {
        return new BasicShader( name, false ) {
            public void adjustRgba( float[] rgba, float value ) {
                if ( ( value > minMask && value < maxMask ) ^ sense ) {
                    rgba[ 3 ] = 0;
                }
            }
        };
    }

    /**
     * Returns an array of any custom shaders specified by the 
     * {@link #LUTFILES_PROPERTY} property.
     *
     * @return  array of zero or more custom shaders
     */
    public static Shader[] getCustomShaders() {
        if ( customShaders_ == null ) {
            String fileset;
            try {
                fileset = System.getProperty( LUTFILES_PROPERTY );
            }
            catch ( SecurityException e ) {
                fileset = null;
            }
            List<Shader> shaderList = new ArrayList<Shader>();
            if ( fileset != null && fileset.length() > 0 ) {
                String[] files =
                    fileset.split( "\\Q" + File.pathSeparator + "\\E" );
                for ( int i = 0; i < files.length; i++ ) {
                    String f = files[ i ];
                    try {
                        shaderList
                           .add( new TextFileLutShader( new File( f ), 256 ) );
                    }
                    catch ( IOException e ) {
                        logger_.warning( "Failed to load custom lookup table "
                                       + f + " (" + e + ")" );
                    }
                }
            }
            customShaders_ = shaderList.toArray( new Shader[ 0 ] );
        }
        return customShaders_;
    }

    /**
     * Shader implementation which fixes one component of the sRGB array
     * at its parameter's value.
     */
    private static class FixRGBComponentShader extends BasicShader {

        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         */
        FixRGBComponentShader( String name, int icomp ) {
            super( name, false );
            icomp_ = icomp;
        }
        
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] = value;
        }
    }

    /**
     * Shader implementation which sets one component of the YUV colour
     * space according to its parameter's value.
     */
    private static class YuvShader extends ColorSpaceComponentShader {
 
        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         * @param  fix    true for fix, false for scale
         */
        YuvShader( String name, int icomp, boolean fix ) {
            super( name, icomp, fix );
        }

        protected void toSpace( float[] rgb ) {
            float r = rgb[ 0 ];
            float g = rgb[ 1 ];
            float b = rgb[ 2 ];
            float y = 0.299f * r + 0.587f * g + 0.114f * b;
            float u = 0.436f * ( b - y ) / ( 1f - 0.114f );
            float v = 0.615f * ( r - y ) / ( 1f - 0.299f );
            float su = 0.5f * ( u / 0.436f ) + 0.5f;
            float sv = 0.5f * ( v / 0.615f ) + 0.5f;
            rgb[ 0 ] = enforceBounds( y );
            rgb[ 1 ] = enforceBounds( su );
            rgb[ 2 ] = enforceBounds( sv );
        }

        protected void fromSpace( float[] yuv ) {
            float y = yuv[ 0 ];
            float su = yuv[ 1 ];
            float sv = yuv[ 2 ];
            float u = ( ( su * 2f ) - 1f ) * 0.436f;
            float v = ( ( sv * 2f ) - 1f ) * 0.615f;
            float r = y + 1.13983f * v;
            float g = y - 0.39466f * u - 0.58060f * v;
            float b = y + 2.03211f * u;
            yuv[ 0 ] = enforceBounds( r );
            yuv[ 1 ] = enforceBounds( g );
            yuv[ 2 ] = enforceBounds( b );
        }
    }

    /**
     * Shader implementation which sets one component of the YPbPr colour
     * space according to its parameter's value.
     */
    private static class YPbPrShader extends ColorSpaceComponentShader {

        private static final float[] T;
        private static final float[] F;
        static {
            double[] toRgb = new double[] {  .299,      .587,    .114, 
                                            -.168736, -.331264,  .5,
                                             .5,      -.418688, -.081312, };
            double[] fromRgb = Matrices.invert( toRgb );
            T = new float[ 9 ];
            F = new float[ 9 ];
            for ( int i = 0; i < 9; i++ ) {
                T[ i ] = (float) toRgb[ i ];
                F[ i ] = (float) fromRgb[ i ];
            }
        }

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         * @param  fix    true for fix, false for scale
         */
        YPbPrShader( String name, int icomp, boolean fix ) {
            super( name, icomp, fix );
        }

        protected void toSpace( float[] rgb ) {
            float r = rgb[ 0 ];
            float g = rgb[ 1 ];
            float b = rgb[ 2 ];
            float y  = r * T[ 0 ] + g * T[ 1 ] + b * T[ 2 ];
            float pb = r * T[ 3 ] + g * T[ 4 ] + b * T[ 5 ];
            float pr = r * T[ 6 ] + g * T[ 7 ] + b * T[ 8 ];
            rgb[ 0 ] = enforceBounds( y );
            rgb[ 1 ] = enforceBounds( pb + 0.5f );
            rgb[ 2 ] = enforceBounds( pr + 0.5f );
        }

        protected void fromSpace( float[] yPbPr ) {
            float y =  yPbPr[ 0 ];
            float pb = yPbPr[ 1 ] - 0.5f;
            float pr = yPbPr[ 2 ] - 0.5f;
            float r = y * F[ 0 ] + pb * F[ 1 ] + pr * F[ 2 ];
            float g = y * F[ 3 ] + pb * F[ 4 ] + pr * F[ 5 ];
            float b = y * F[ 6 ] + pb * F[ 7 ] + pr * F[ 8 ];
            yPbPr[ 0 ] = enforceBounds( r );
            yPbPr[ 1 ] = enforceBounds( g );
            yPbPr[ 2 ] = enforceBounds( b );
        }
    }

    /**
     * Shader implementation which fixes one component of the HSV colour
     * space at its parameter's value.
     */
    private static class HsvShader extends ColorSpaceComponentShader {

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         * @param  fix    true for fix, false for scale
         */
        HsvShader( String name, int icomp, boolean fix ) {
            super( name, icomp, fix );
        }

        protected void toSpace( float[] rgb ) {
            float r = rgb[ 0 ];
            float g = rgb[ 1 ];
            float b = rgb[ 2 ];
            float max = Math.max( r, Math.max( g, b ) );
            float min = Math.min( r, Math.min( g, b ) );
            float v = max;
            float s = max > 0 ? 1f - min / max : 0;
            float h;
            float diff = max - min;
            if ( diff == 0 ) {
                h = 0f;
            }
            else if ( r == max ) {
                h = ( 60f * ( g - b ) / diff ) + 0f;
            }
            else if ( g == max ) {
                h = ( 60f * ( b - r ) / diff ) + 120f;
            }
            else if ( b == max ) {
                h = ( 60f * ( r - g ) / diff ) + 240f;
            }
            else {
                assert false;
                h = 0f;
            }
            if ( h < 0f ) {
                h += 360f;
            }
            float sh = h / 360f;
            rgb[ 0 ] = sh;
            rgb[ 1 ] = s;
            rgb[ 2 ] = v;
        }

        protected void fromSpace( float[] hsv ) {
            float sh = hsv[ 0 ];
            float s = hsv[ 1 ];
            float v = hsv[ 2 ];
            float h6 = sh * 6f;
            int ih = (int) h6;
            float f = h6 - ih;
            float p = v * ( 1 - s );
            float q = v * ( 1 - f * s );
            float t = v * ( 1 - ( 1 - f ) * s );
            final float r;
            final float g;
            final float b;
            switch ( ih ) {
                case 0:
                    r = v; g = t; b = p; break;
                case 1:
                    r = q; g = v; b = p; break;
                case 2:
                    r = p; g = v; b = t; break;
                case 3:
                    r = p; g = q; b = v; break;
                case 4:
                    r = t; g = p; b = v; break;
                case 5:
                case 6:
                    r = v; g = p; b = q; break;
                default:
                    r = 0; g = 0; b = 0; assert false : ih;
            }
            hsv[ 0 ] = r;
            hsv[ 1 ] = g;
            hsv[ 2 ] = b;
        }
    }

    /**
     * Shader implementation that interpolates between the base
     * colour and a given fixed colour.
     */
    private static class FadeShader extends BasicShader {
        private final float[] fadeRgba_;

        /**
         * Constructor.
         *
         * @param   name  shader name
         * @param   fadeColor  common output colour for value=1
         */
        FadeShader( String name, Color fadeColor ) {
            super( name, false );
            fadeRgba_ = fadeColor.getComponents( new float[ 4 ] );
        }

        public void adjustRgba( float[] rgba, float value ) {
            for ( int i = 0; i < 4; i++ ) {
                rgba[ i ] += value * ( fadeRgba_[ i ] - rgba[ i ] );
            }
        }

        public int hashCode() {
            int code = 3344;
            code = 23 * code + Arrays.hashCode( fadeRgba_ );
            return code;
        }

        public boolean equals( Object o ) {
            if ( o instanceof FadeShader ) {
                FadeShader other = (FadeShader) o;
                return Arrays.equals( this.fadeRgba_, other.fadeRgba_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Shader implementation that fades between a list of colours
     * using linear interpolation between each pair.
     */
    private static class InterpolationShader extends BasicShader {
        private final float[][] rgbas_;
        private final int nRange_;
        private final float fRange_;
        private final float[] rgbaMax_;

        /**
         * Constructor.
         *
         * @param  name  shader name
         * @param  colors   list of colours; value 0 corresponds to first one,
         *                  value 1 corresponds to last
         */
        InterpolationShader( String name, Color[] colors ) {
            super( name );
            int nc = colors.length;
            nRange_ = nc - 1;
            fRange_ = 1f / nRange_;
            rgbas_ = new float[ nc ][];
            for ( int ic = 0; ic < nc; ic++ ) {
                rgbas_[ ic ] = colors[ ic ].getRGBComponents( null );
            }
            rgbaMax_ = rgbas_[ nc - 1 ];
        }

        public void adjustRgba( float[] rgba, float value ) {
            if ( value >= 1 ) {
                for ( int i = 0; i < 4; i++ ) {
                    rgba[ i ] = rgbaMax_[ i ];
                }
            }
            else {
                int ic0 = (int) ( nRange_ * value );
                int ic1 = ic0 + 1;
                float v = nRange_ * ( value % fRange_ );
                float[] rgba0 = rgbas_[ ic0 ];
                float[] rgba1 = rgbas_[ ic1 ];
                for ( int i = 0; i < 4; i++ ) {
                    float f0 = rgba0[ i ];
                    float f1 = rgba1[ i ];
                    rgba[ i ] = f0 + ( f1 - f0 ) * v;
                }
            }
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode( rgbas_ );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof InterpolationShader ) {
                InterpolationShader other = (InterpolationShader) o;
                return Arrays.deepEquals( this.rgbas_, other.rgbas_ );
            }
            else {
                return false;
            }
        }
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
     * Forces a value to be within the range 0..1.
     *
     * @param   f  input value
     * @return  f if it's in range, otherwise 0 or 1
     */
    private static final float enforceBounds( float f ) {
        if ( f >= 0f ) {
            if ( f <= 1f ) {
                return f;
            }
            else {
                return 1f;
            }
        }
        else {
            return 0f;
        }
    }

    /**
     * Shader which represents a sub-range of a given base shader.
     * Icon drawing is not handled perfectly for some non-absolute base shaders.
     */
    private static class StretchedShader implements Shader {
        private final String name_;
        private final Shader baseShader_;
        private final float f0_;
        private final float fScale_; 

        /**
         * Constructor.
         *
         * @param   name    shader name
         * @param   shader  base shader
         * @param   frac0   parameter value in base shader corresponding to
         *                  value 0 in this one (must be in range 0-1)
         * @param   frac1   parameter value in base shader corresponding to
         *                  value 1 in this one (must be in range 0-1)
         */
        public StretchedShader( String name, Shader base, float f0, float f1 ) {
            if ( ! ( f0 >= 0 && f0 <= 1 ) ||
                 ! ( f1 >= 0 && f1 <= 1 ) ) {
                throw new IllegalArgumentException( "Bad fraction" );
            }
            name_ = name;
            baseShader_ = base;
            f0_ = f0;
            fScale_ = f1 - f0;
        }

        /**
         * Constructor with an automatically-generated name.
         *
         * @param   shader  base shader
         * @param   frac0   parameter value in base shader corresponding to
         *                  value 0 in this one (must be in range 0-1)
         * @param   frac1   parameter value in base shader corresponding to
         *                  value 1 in this one (must be in range 0-1)
         */
        public StretchedShader( Shader base, float f0, float f1 ) {
            this( "Stretch-" + base.getName(), base, f0, f1 );
        }

        public String getName() {
            return name_;
        }

        public boolean isAbsolute() {
            return baseShader_.isAbsolute();
        }

        public void adjustRgba( float[] rgba, float value ) {
            baseShader_.adjustRgba( rgba, f0_ + fScale_ * value );
        }

        public boolean equals( Object o ) {
            if ( o instanceof StretchedShader ) {
                StretchedShader other = (StretchedShader) o;
                return this.baseShader_.equals( other.baseShader_ )
                    && this.f0_ == other.f0_
                    && this.fScale_ == other.fScale_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 79;
            code = code * 23 + baseShader_.hashCode();
            code = code * 23 + Float.floatToIntBits( f0_ );
            code = code * 23 + Float.floatToIntBits( fScale_ );
            return code;
        }
    }

    /**
     * Shader implementation which reverses the sense of an existing one.
     */
    private static class InvertedShader implements Shader {
        private final Shader base_;

        /**
         * Constructor.
         *
         * @param  base  base shader
         */
        public InvertedShader( Shader base ) {
            base_ = base;
        }

        public void adjustRgba( float[] rgba, float value ) {
            base_.adjustRgba( rgba, 1f - value );
        }

        public boolean isAbsolute() {
            return base_.isAbsolute();
        }

        public String getName() {
            return "-" + base_.getName();
        }

        public boolean equals( Object o ) {
            if ( o instanceof InvertedShader ) {
                InvertedShader other = (InvertedShader) o;
                return this.base_.equals( other.base_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            return - base_.hashCode();
        }
    }

    /**
     * Shader implementation which scales the alpha component of an existing
     * one by a fixed factor.
     */
    private static class FadedShader implements Shader {
        private final Shader base_;
        private final float scaleAlpha_;

        /**
         * Constructor.
         *
         * @param  base  base shader
         */
        public FadedShader( Shader base, float scaleAlpha ) {
            base_ = base;
            scaleAlpha_ = scaleAlpha;
        }

        public void adjustRgba( float[] rgba, float value ) {
            base_.adjustRgba( rgba, value );
            rgba[ 3 ] *= scaleAlpha_;
        }

        public boolean isAbsolute() {
            return base_.isAbsolute();
        }

        public String getName() {
            return base_.getName() + "*" + scaleAlpha_;
        }

        public boolean equals( Object o ) {
            if ( o instanceof FadedShader ) {
                FadedShader other = (FadedShader) o;
                return this.base_.equals( other.base_ )
                    && this.scaleAlpha_ == other.scaleAlpha_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 2223432;
            code = 23 * code + base_.hashCode();
            code = 23 * code + Float.floatToIntBits( scaleAlpha_ );
            return code;
        }
    }

    /**
     * Shader implementation which quantises the colour map into a
     * set of discrete colour values.
     */
    private static class QuantisedShader implements Shader {

        private final Shader base_;
        private final float nlevel_;

        /**
         * Constructor.
         *
         * @param  base  base shader
         * @param  nlevel   number of discrete colours for output
         */
        public QuantisedShader( Shader base, double nlevel ) {
            base_ = base;
            nlevel_ = (float) nlevel;
        }

        public void adjustRgba( float[] rgba, float value ) {
            base_.adjustRgba( rgba, quantise( value ) );
        }

        private float quantise( float value ) {
            return Math.min( (int) ( value * nlevel_ ) / ( nlevel_ - 1 ), 1f );
        }

        public boolean isAbsolute() {
            return base_.isAbsolute();
        }

        public String getName() {
            return base_.getName() + "-" + Math.round( nlevel_ );
        }

        @Override
        public int hashCode() {
            int code = 4392;
            code = 23 * code + base_.hashCode();
            code = 23 * code + Float.floatToIntBits( nlevel_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof QuantisedShader ) {
                QuantisedShader other = (QuantisedShader) o;
                return this.base_.equals( other.base_ )
                    && this.nlevel_ == other.nlevel_;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Wrapper shader which just changes the supplied name.
     * All other behaviour is delegated to the supplied base instance.
     */
    private static class RenamedShader implements Shader {
        private final Shader base_;
        private final String name_;

        /**
         * Constructor.
         *
         * @param  base  base shader
         * @param  name  shader name
         */
        public RenamedShader( Shader base, String name ) {
            base_ = base;
            name_ = name;
        }

        public String getName() {
            return name_;
        }

        public boolean isAbsolute() {
            return base_.isAbsolute();
        }

        public void adjustRgba( float[] rgba, float value ) {
            base_.adjustRgba( rgba, value );
        }

        public int hashCode() {
            int code = 9191;
            code = 23 * code + base_.hashCode();
            code = 23 * code + name_.hashCode();
            return code;
        }

        public boolean equals( Object o ) {
            if ( o instanceof RenamedShader ) {
                RenamedShader other = (RenamedShader) o;
                return this.base_.equals( other.base_ )
                    && this.name_.equals( other.name_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Shader implementation which scales one component of the sRGB array
     * by its parameter's value.
     */
    private static class ScaleRGBComponentShader extends BasicShader {

        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param   icomp  modified component index
         */
        ScaleRGBComponentShader( String name, int icomp ) {
            super( name, false );
            icomp_ = icomp;
        }
 
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] *= value;
        }
    }

    /**
     * Abstract superclass of lookup table-based shader.
     */
    private static abstract class LutShader extends BasicShader {

        /**
         * Constructor.
         *
         * @param  name  shader name
         */
        LutShader( String name ) {
            super( name );
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
            int is3 = 3 * ( (int) ( value * ( nsamp - 1 ) + 0.5f ) );
            rgba[ 0 ] = lut[ is3 + 0 ];
            rgba[ 1 ] = lut[ is3 + 1 ];
            rgba[ 2 ] = lut[ is3 + 2 ];
        }
    }

    /**
     * Lookup table-based shader which reads lookup table data from 
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

    /**
     * Lookup table-based shader which reads lookup table data from
     * a text file containing three columns giving R, G, B in the range
     * 0-1.
     */
    private static class TextFileLutShader extends LutShader {

        private final float[] lut_;
        private static final Pattern TRIPLE_REGEX = 
            Pattern.compile( "\\s*([0-9.e]+)\\s+([0-9.e]+)\\s+([0-9.e]+)\\s*" );

        /**
         * Constructor.
         *
         * @param file  file containing N rows of 3 float values
         * @param minSamples  the minimum number of samples in the resulting
         *        lookup table; if the file contains fewer than this,
         *        interpolation will be performed
         */
        TextFileLutShader( File file, int minSamples ) throws IOException {
            super( file.getName() );
            FloatList flist = new FloatList();
            int iline = 0;
            BufferedReader in = new BufferedReader( new FileReader( file ) );
            try {
                for ( String line; ( line = in.readLine() ) != null; ) {
                    iline++;
                    String tline = line.replaceFirst( "#.*", "" ).trim();
                    if ( tline.length() > 0 ) {
                        Matcher matcher = TRIPLE_REGEX.matcher( tline );
                        if ( matcher.matches() ) {
                            for ( int i = 0; i < 3; i++ ) {
                                float val =
                                    Float.parseFloat( matcher.group( i + 1 ) );
                                if ( val >= 0f && val <= 1f ) {
                                    flist.add( val );
                                }
                                else {
                                    throw new IOException( "Not in range 0-1: "
                                                         + file + " line "
                                                         + iline );
                                }
                            }
                        }
                        else {
                            throw new IOException( "Not 3 numbers: "
                                                 + file + " line " + iline );
                        }
                    }
                }
            }
            finally {
                in.close();
            }
            float[] lut = flist.toFloatArray();
            lut_ = lut.length < minSamples ? interpolateRgb( lut, minSamples )
                                           : lut;
            assert lut_.length % 3 == 0;
        }

        protected float[] getRgbLut() {
            return lut_;
        }
    }

    /**
     * Shader which works with a few RGB samples supplied in the constructor
     * and interpolates between them.
     */
    private static class SampleShader extends LutShader {
        private static final float FF1 = 1f / 255f;
        protected final float[] lut_;

        /**
         * Constructor.
         *
         * @param  name  shader name
         * @param  array of rrggbb hexadecimal colour triples
         */
        SampleShader( String name, int[] rgbs ) {
            super( name );
            lut_ = interpolateRgb( toFloats( rgbs ), 256 );
        }

        protected float[] getRgbLut() {
            return lut_;
        }

        /**
         * Turns an N-element rrggbb int array into a
         * 3N-element r, g, b float array.
         *
         * @param   rgbs  int rgb samples
         * @return  float samples
         */
        private static float[] toFloats( int[] rgbs ) {
            float[] flut = new float[ rgbs.length * 3 ];
            for ( int i = 0; i < rgbs.length; i++ ) {
                int j = i * 3;
                int rgb = rgbs[ i ];
                flut[ j++ ] = ( ( rgb >> 16 ) & 0xff ) * FF1;
                flut[ j++ ] = ( ( rgb >> 8  ) & 0xff ) * FF1;
                flut[ j++ ] = ( ( rgb >> 0  ) & 0xff ) * FF1;
            }
            return flut;
        }
    }

    /**
     * LutShader which creates a lookup table by applying an existing
     * (presumably non-absolute) shader to a given colour.
     */
    private static class AppliedLutShader extends LutShader {
        private final Shader baseShader_;
        private final Color baseColor_;
        private final int nsample_;
        private final float[] lut_;

        /**
         * Constructor.
         *
         * @param  baseShader  shader which will be applied
         * @param  baseColor   colour to which shader will be applied
         * @param  nsample   number of entries in the lookup table
         *                   which will be created
         */
        AppliedLutShader( Shader baseShader, Color baseColor, int nsample ) {
            super( baseShader.getName() + "-fix" );
            baseShader_ = baseShader;
            baseColor_ = baseColor;
            nsample_ = nsample;
            float[] baseRgba =
                baseColor.getRGBColorComponents( new float[ 4 ] );
            float[] rgba = new float[ 4 ];
            lut_  = new float[ 3 * nsample ];
            for ( int is = 0; is < nsample; is++ ) {
                float level = (float) is / (float) ( nsample - 1 );
                System.arraycopy( baseRgba, 0, rgba, 0, 4 );
                baseShader.adjustRgba( rgba, level );
                System.arraycopy( rgba, 0, lut_, is * 3, 3 );
            }
        }

        protected float[] getRgbLut() {
            return lut_;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AppliedLutShader ) {
                AppliedLutShader other = (AppliedLutShader) o;
                return this.baseShader_.equals( other.baseShader_ )
                    && ( baseShader_.isAbsolute()
                         || this.baseColor_.equals( other.baseColor_ ) )
                    && this.nsample_ == other.nsample_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 5501;
            code = code * 23 + baseShader_.hashCode();
            code = code * 23 + ( baseShader_.isAbsolute()
                                     ? 99
                                     : baseColor_.hashCode() );
            code = code * 23 + nsample_;
            return code;
        }
    }

    /**
     * Creates a new RGB array with a specificied number of samples 
     * from a given one by interpolation.
     *
     * @param  in  input RGB array in order r, g, b, r, g, b, ...
     * @param  nsamp  number of RGB samples in the output array
     * @return  output RGB array in order r, g, b, r, g, b, ...
     */
    private static float[] interpolateRgb( float[] in, int nsamp ) {
        int nin = in.length / 3;
        float[] out = new float[ nsamp * 3 ];
        for ( int is = 0; is < nsamp; is++ ) {
            for ( int ic = 0; ic < 3; ic++ ) {
                int ilo = is * ( nin - 1 ) / ( nsamp - 1 );
                int ihi = Math.min( ilo + 1, nin - 1 );
                float frac = is * ( nin - 1 ) / (float) ( nsamp - 1 ) - ilo;
                out[ is * 3 + ic ] =
                    in[ ilo * 3 + ic ] +
                    frac * ( in[ ihi * 3 + ic ] - in[ ilo * 3 + ic ] );
            }
        }
        return out;
    }

    /**
     * Returns a shader which runs in the opposite direction to a given one.
     *
     * @param  shader  base shader
     * @return  inverted version
     */
    public static Shader invert( Shader shader ) {
        return new InvertedShader( shader );
    }

    /**
     * Returns a shader which scales the alpha value of all its colours
     * by a given constant.
     *
     * @param  shader  input shader
     * @param  scaleAlpha  alpha scaling factor, in range 0..1
     * @return   fading shader; same as input if <code>scaleAlpha</code>==1
     */
    public static Shader fade( Shader shader, float scaleAlpha ) {
        return scaleAlpha == 1f ? shader
                                : new FadedShader( shader, scaleAlpha );
    }

    /**
     * Returns a shader which delegates all behaviour to a supplied base
     * instance, except it has a given name.
     *
     * @param  shader  base shader
     * @param  name  new name
     * @return  new shader
     */
    public static Shader rename( Shader shader, String name ) {
        return new RenamedShader( shader, name );
    }

    /**
     * Returns a shader which corresponds to a sub-range of a given shader.
     *
     * @param   shader  base shader
     * @param   frac0   parameter value in base shader corresponding to
     *                  value 0 in this one (must be in range 0-1)
     * @param   frac1   parameter value in base shader corresponding to
     *                  value 1 in this one (must be in range 0-1)
     * @return   new shader
     */
    public static Shader stretch( Shader shader, float frac0, float frac1 ) {
        return new StretchedShader( shader, frac0, frac1 );
    }

    /**
     * Returns a shader which splits the colour map into a set of
     * discrete levels.
     *
     * @param  shader  base shader
     * @param  nlevel  number of discrete colour levels required
     * @return  new shader
     */
    public static Shader quantise( Shader shader, double nlevel ) {
        return new QuantisedShader( shader, nlevel );
    }

    /**
     * Creates a shader by applying an existing shader to a given base colour.
     * This only does useful work if the existing shader is non-absolute.
     *
     * @param  shader  shader to apply (presumably non-absolute)
     * @param  baseColor  colour to which the shader will be applied
     * @param  nsample  number of samples in the lookup table
     * @return  new absolute shader
     */
    public static Shader applyShader( Shader shader, Color baseColor,
                                      int nsample ) {
        return new AppliedLutShader( shader, baseColor, nsample );
    }

    /**
     * Constructs an icon which represents a shader in one dimension.
     * A horizontal or vertical bar is drawn
     * which gives the full range of colours produced by the shader as
     * operating on a given base colour.
     *
     * @param  shader   shader
     * @param  horizontal   true for a horizontal bar, false for vertical
     * @param  baseColor   the base colour modified by the shader
     * @param  width    total width of the icon
     * @param  height   total height of the icon
     * @param  xpad     internal padding in the X direction
     * @param  ypad     internal padding in the Y direction
     * @return  icon
     */
    private static Icon create1dIcon( Shader shader, boolean horizontal,
                                      Color baseColor, int width, int height,
                                      int xpad, int ypad ) {
        return new ShaderIcon1( shader, horizontal, baseColor, width, height,
                                xpad, ypad );
    }

    /**
     * Constructs an icon which represents two shaders in two dimensions.
     *
     * @param  xShader  shader for X direction
     * @param  yShader  shader for Y direction
     * @param  xFirst   true if X shader is to be applied first
     * @param  baseColor  base colour for the shaders to work on 
     * @param  width    total width of the icon
     * @param  height   total height of the icon
     * @param  xpad     internal padding in the X direction
     * @param  ypad     internal padding in the Y direction
     * @return icon
     */
    public static Icon create2dIcon( Shader xShader, Shader yShader,
                                     boolean xFirst, Color baseColor,
                                     int width, int height,
                                     int xpad, int ypad ) {
        return new ShaderIcon2( xShader, yShader, xFirst, baseColor,
                                width, height, xpad, ypad );
    }

    /**
     * Returns an icon which displays a shader in action,
     * using an explicitly provided grid shader.
     *
     * @param  shader   shader to illustrate
     * @param  gridShader  defines the pixels along the transverse direction
     *                     of the shader; ignored (may be null) for absolute
     *                     shaders
     * @param  horizontal  true for shading running horizontally,
     *                     false for vertically
     * @param  width  total icon width in pixels
     * @param  height total icon height in pixels
     * @param  xpad   internal padding in the X direction
     * @param  ypad   internal padding in the Y direction
     * @return  icon
     */
    public static Icon createShaderIcon( Shader shader, Shader gridShader,
                                         boolean horizontal, int width,
                                         int height, int xpad, int ypad ) {
        return shader.isAbsolute()
             ? create1dIcon( shader, horizontal, Color.BLACK,
                             width, height, xpad, ypad )
             : create2dIcon( horizontal ? shader : gridShader,
                             horizontal ? gridShader : shader,
                             ! horizontal, Color.BLACK,
                             width, height, xpad, ypad );
    }

    /**
     * Returns an icon which displays a shader in action,
     * using a default grid shader if required.
     *
     * @param  shader   shader to illustrate
     * @param  horizontal  true for shading running horizontally,
     *                     false for vertically
     * @param  width  total icon width in pixels
     * @param  height total icon height in pixels
     * @param  xpad   internal padding in the X direction
     * @param  ypad   internal padding in the Y direction
     * @return  icon
     */
    public static Icon createShaderIcon( Shader shader,
                                         boolean horizontal, int width,
                                         int height, int xpad, int ypad ) {
        return createShaderIcon( shader, DFLT_GRID_SHADER, horizontal,
                                 width, height, xpad, ypad );
    }

    /**
     * Indicates whether the given shader object is capable of introducing
     * transparency into a colour (modifying rgba[3] from 1 to a lower value).
     *
     * @param  shader  shader to test
     * @return  true if shader adjusts transparency or is null
     */
    public static boolean isTransparent( Shader shader ) {
        if ( shader == null ) {
            return false;
        }

        /* Currently just tries a couple of colours.  Not foolproof, but 
         * likely to work for sensible shaders. */
        float[] rgba = new float[] { 1.0f, 0.1f, 0.6f, 1.0f,};
        shader.adjustRgba( rgba, 0.8f );
        if ( rgba[ 3 ] != 1.0f ) {
            return true;
        }
        shader.adjustRgba( rgba, 0.2f );
        if ( rgba[ 3 ] != 1.0f ) {
            return true;
        }
        return false;
    }

    /**
     * Icon representing a Shader in one dimension.  
     * A horizontal or vertical bar is drawn
     * which gives the full range of colours produced by the shader as
     * operating on a given base colour.
     */
    private static class ShaderIcon1 implements Icon {

        private final Shader shader_;
        private final boolean horizontal_;
        private final int width_;
        private final int height_;
        private final int xpad_;
        private final int ypad_;
        private final float[] baseRgba_;

        /**
         * Constructor.
         *
         * @param  shader   shader
         * @param  horizontal   true for a horizontal bar, false for vertical
         * @param  baseColor   the base colour modified by the shader
         * @param  width    total width of the icon
         * @param  height   total height of the icon
         * @param  xpad     internal padding in the X direction
         * @param  ypad     internal padding in the Y direction
         */
        public ShaderIcon1( Shader shader, boolean horizontal, Color baseColor,
                            int width, int height, int xpad, int ypad ) {
            shader_ = shader;
            horizontal_ = horizontal;
            width_ = width;
            height_ = height;
            xpad_ = xpad;
            ypad_ = ypad;
            baseRgba_ = baseColor.getRGBComponents( null );
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color origColor = g.getColor();
            int npix = horizontal_ ? width_ - 2 * xpad_
                                   : height_ - 2 * ypad_;
            float np1 = 1f / ( npix - 1 );
            int xlo = x + xpad_;
            int xhi = x + width_ - xpad_;
            int ylo = y + ypad_;
            int yhi = y + height_ - ypad_;
            for ( int ipix = 0; ipix < npix; ipix++ ) {
                g.setColor( getColor( ipix * np1 ) );
                if ( horizontal_ ) {
                    g.fillRect( xlo + ipix - 1, ylo, 1, yhi - ylo );
                }
                else {
                    g.fillRect( xlo, ylo + ipix - 1, xhi - xlo, 1 );
                }
            }
            g.setColor( origColor );
        }

        /**
         * Returns the colour corresponding to a given parameter value for
         * this icon's shader.
         *
         * @param  value  parameter value
         * @return colour
         */
        private Color getColor( float value ) {
            float[] rgba = baseRgba_.clone();
            shader_.adjustRgba( rgba, value );
            return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        }
    }


    /**
     * Icon representing two Shaders in two dimensions.
     */
    private static class ShaderIcon2 implements Icon {

        private final Shader xShader_;
        private final Shader yShader_;
        private final boolean xFirst_;
        private final int width_;
        private final int height_;
        private final int xpad_;
        private final int ypad_;
        private final float[] baseRgba_;

        /**
         * Constructor.
         *
         * @param  xShader  shader for X direction
         * @param  yShader  shader for Y direction
         * @param  xFirst   true if X shader is to be applied first
         * @param  baseColor  base colour for the shaders to work on 
         * @param  width    total width of the icon
         * @param  height   total height of the icon
         * @param  xpad     internal padding in the X direction
         * @param  ypad     internal padding in the Y direction
         */
        public ShaderIcon2( Shader xShader, Shader yShader, boolean xFirst, 
                            Color baseColor, int width, int height,
                            int xpad, int ypad ) {
            xShader_ = xShader;
            yShader_ = yShader;
            xFirst_ = xFirst;
            width_ = width;
            height_ = height;
            xpad_ = xpad;
            ypad_ = ypad;
            baseRgba_ = baseColor.getRGBComponents( null );
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color origColor = g.getColor();
            int nx = width_ - 2 * xpad_;
            int ny = height_ - 2 * ypad_;
            float nx1 = 1f / ( nx - 1 );
            float ny1 = 1f / ( ny - 1 );
            for ( int ix = 0; ix < nx; ix++ ) {
                for ( int iy = 0; iy < ny; iy++ ) {
                    g.setColor( getColor( ix * nx1, iy * ny1 ) );
                    g.drawRect( x + ix + xpad_, y + iy + ypad_, 1, 1 );
                }
            }
            g.setColor( origColor );
        }

        /**
         * Returns the colour generated by this icon's shaders at particular
         * values of the X and Y parameters.
         *
         * @param   xval  X shader parameter
         * @param   yval  Y shader parameter
         * @return  colour
         */
        private Color getColor( float xval, float yval ) {
            float[] rgba = baseRgba_.clone();
            if ( xFirst_ ) {
                xShader_.adjustRgba( rgba, xval );
                yShader_.adjustRgba( rgba, yval );
            }
            else {
                yShader_.adjustRgba( rgba, yval );
                xShader_.adjustRgba( rgba, xval );
            }
            return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        }
    }
}
