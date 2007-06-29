package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.topcat.EmptyIcon;
import uk.ac.starlink.util.FloatList;

/**
 * Provides some implementations of the {@link Shader} interface.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class Shaders {

    private static final Color DARK_GREEN = new Color( 0, 160, 0 );

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
    public static final Shader FIX_Y = new FixYuvShader( "YUV Y", 0 );

    /** Fixes U in YUV colour space. */
    public static final Shader FIX_U = new FixYuvShader( "YUV U", 1 );

    /** Fixes V in YUV colour space. */
    public static final Shader FIX_V = new FixYuvShader( "YUV V", 2 );

    /** Fixes H in HSV colour space. */
    public static final Shader HSV_H = new FixHsvShader( "HSV H", 0 );
 
    /** Fixes S in HSV colour space. */
    public static final Shader HSV_S = new FixHsvShader( "HSV S", 1 );

    /** Fixes V in HSV colour space. */
    public static final Shader HSV_V = new FixHsvShader( "HSV V", 2 );

    /** Interpolates between red (0) and blue (1). */
    public static final Shader RED_BLUE =
        createInterpolationShader( "Red-Blue", Color.RED, Color.BLUE );

    /** Interpolates between white (0) and black (1). */
    public static final Shader WHITE_BLACK =
        createInterpolationShader( "Greyscale", Color.WHITE, Color.BLACK );

    /** Interpolates between black (0) and white (1). */
    public static final Shader BLACK_WHITE =
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

    /** ListCellRenderer suitable for a combo box containing Shaders. */
    public static final ListCellRenderer SHADER_RENDERER =
        new ShaderListCellRenderer();

    /** Base directory for locating binary colour map lookup table resources. */
    private final static String LUT_BASE = "/uk/ac/starlink/topcat/colormaps/";

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Basic abstract partial shader implementation.
     */
    private static abstract class BasicShader implements Shader {

        private final String name_;
        private final Color baseColor_;

        /**
         * Constructs an absolute shader (one with no dependence on original
         * colour).
         *
         * @param   name  shader name
         */
        BasicShader( String name ) {
            this( name, null );
        }

        /**
         * Constructs a non-absolute shader.  The supplied 
         * <code>baseColor</code> is the one which is used for constructing
         * an icon.
         *
         * @param  name  shader name
         * @param   baseColor  base colour for constructing icon
         */
        BasicShader( String name, Color baseColor ) {
            name_ = name;
            baseColor_ = baseColor;
        }

        public String getName() {
            return name_;
        }

        public Icon createIcon( boolean horizontal, int width, int height,
                                int xpad, int ypad ) {
            return create1dIcon( this, horizontal,
                                 baseColor_ == null ? Color.BLACK : baseColor_,
                                 width, height, xpad, ypad );
        }

        public boolean isAbsolute() {
            return baseColor_ == null;
        }

        public String toString() {
            return "Shader: " + name_;
        }
    }

    /**
     * Abstract Shader implementation for fixing components in foreign
     * (non-RGB) colour spaces.
     */
    private static abstract class FixColorSpaceComponentShader 
                                  extends BasicShader {

        private final int icomp_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  baseColor  base colour for generating icon
         * @param  icomp   modified component index
         */
        FixColorSpaceComponentShader( String name, Color baseColor,
                                      int icomp ) {
            super( name, baseColor );
            icomp_ = icomp;
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
            rgba[ icomp_ ] = value;
            fromSpace( rgba );
        }
    }

    /** Shader which does nothing. */
    public static final Shader NULL =
            new BasicShader( "None", new Color( 1, 1, 1, 0 ) ) {
        public void adjustRgba( float[] rgba, float value ) {
        }
    };

    /** Scales alpha channel by parameter value. */
    public static final Shader TRANSPARENCY =
            new BasicShader( "Transparency", Color.BLACK ) {
        public void adjustRgba( float[] rgba, float value ) {

            /* Squaring seems to adjust the range better. */
            rgba[ 3 ] *= value * value;
        }
    };

    /** Shader which sets intensity. */
    public static final Shader FIX_INTENSITY =
            new BasicShader( "Intensity", Color.BLUE ) {
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
            new BasicShader( "Scale Intensity", Color.BLUE ) {
        public void adjustRgba( float[] rgba, float value ) {
            for ( int i = 0; i < 3; i++ ) {
                rgba[ i ] = 1f - value * ( 1f - rgba[ i ] );
            }
        }
    };

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
        return new BasicShader( name ) {
            public void adjustRgba( float[] rgba, float value ) {
                for ( int i = 0; i < 3; i++ ) {
                    float f0 = rgba0[ i ];
                    float f1 = rgba1[ i ];
                    rgba[ i ] = f0 + ( f1 - f0 ) * value;
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
            super( name, Color.BLACK );
            icomp_ = icomp;
        }
        
        public void adjustRgba( float[] rgba, float value ) {
            rgba[ icomp_ ] = value;
        }
    }

    /**
     * Shader implementation which fixes one component of the YUV colour
     * space at its parameter's value.
     */
    private static class FixYuvShader extends FixColorSpaceComponentShader {
 
        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         */
        FixYuvShader( String name, int icomp ) {
            super( name, DARK_GREEN, icomp );
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
     * Shader implementation which fixes one component of the YPbPr colour
     * space at its parameter's value.
     */
    private static class FixYPbPrShader extends FixColorSpaceComponentShader {

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
         */
        FixYPbPrShader( String name, int icomp ) {
            super( name, DARK_GREEN, icomp );
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
    private static class FixHsvShader extends FixColorSpaceComponentShader {

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  icomp   modified component index
         */
        FixHsvShader( String name, int icomp ) {
            super( name, Color.RED, icomp );
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
            super( name, Color.GRAY );
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

    /**
     * Returns a shader which runs in the opposite direction to a given one.
     *
     * @param  shader  base shader
     * @return  inverted version
     */
    public static Shader invert( final Shader shader ) {
        return new Shader() {
            public void adjustRgba( float[] rgba, float value ) {
                shader.adjustRgba( rgba, 1f - value );
            }
            public boolean isAbsolute() {
                return shader.isAbsolute();
            }
            public String getName() {
                return "-" + shader.getName();
            }
            public Icon createIcon( final boolean horizontal, final int width,
                                    final int height, int xpad, int ypad ) {
                final Icon icon =
                    shader.createIcon( horizontal, width, height, xpad, ypad );
                return new Icon() {
                    public int getIconWidth() {
                        return icon.getIconWidth();
                    }
                    public int getIconHeight() {
                        return icon.getIconHeight();
                    }
                    public void paintIcon( Component c, Graphics g,
                                           int x, int y ) {
                        Graphics2D g2 = (Graphics2D) g;
                        AffineTransform trans = g2.getTransform();
                        g2.translate( x + width / 2, y + height / 2 );
                        g2.scale( horizontal ? -1 : +1,
                                  horizontal ? +1 : -1 );
                        icon.paintIcon( c, g2, -width / 2, -height / 2 );
                        g2.setTransform( trans );
                    }
                };
            }
        };
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
                    int xpos = xlo + ipix;
                    g.drawLine( xpos, ylo, xpos, yhi );
                }
                else {
                    int ypos = ylo + ipix;
                    g.drawLine( xlo, ypos, xhi, ypos );
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
            float[] rgba = (float[]) baseRgba_.clone();
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
                    g.drawRect( ix + xpad_, iy + ypad_, 1, 1 );
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
            float[] rgba = (float[]) baseRgba_.clone();
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

    /**
     * ListCellRenderer suitable for a combo box containing shaders.
     */
    private static class ShaderListCellRenderer extends BasicComboBoxRenderer {

        private static final Map iconMap_ = new HashMap();

        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index, boolean isSel,
                                                       boolean hasFocus ) {
            Component comp =
                super.getListCellRendererComponent( list, value, index, isSel,
                                                    hasFocus );
            if ( comp instanceof JLabel && value instanceof Shader ) {
                JLabel label = (JLabel) comp;
                Shader shader = (Shader) value;
                String text = shader.getName();
                if ( ! shader.isAbsolute() ) {
                    text = "* " + text ;
                }
                label.setText( text );
                label.setIcon( getIcon( shader ) );
            }
            return comp;
        }

        private static Icon getIcon( Shader shader ) {
            if ( ! iconMap_.containsKey( shader ) ) {
                iconMap_.put( shader, shader.createIcon( true, 48, 16, 4, 1 ) );
            }
            return (Icon) iconMap_.get( shader );
        }
    }
}
