package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * KeySet for defining the characteristics of a colour ramp.
 *
 * @author   Mark Taylor
 * @since    16 Sep 2014
 */
public class RampKeySet implements KeySet<RampKeySet.Ramp> {

    private final ConfigKey<Shader> shaderKey_;
    private final ConfigKey<Subrange> subrangeKey_;
    private final ConfigKey<Boolean> logKey_;
    private final ConfigKey<Boolean> flipKey_;
    private final ConfigKey<Color> nullcolorKey_;

    /**
     * Constructor.
     *
     * @param  axname  short form of axis name
     * @param  axName  long form of axis name
     */
    public RampKeySet( String axname, String axName ) {
        shaderKey_ = new ShaderConfigKey(
            new ConfigMeta( axname + "map", axName + "Map" )
           .setShortDescription( "Color map for " + axName + " shading" )
           .setXmlDescription( new String[] {
                "<p>Color map used for " + axName + " axis shading.",
                "</p>",
            } )
            , createAuxShaders(), Shaders.LUT_RAINBOW
        ).appendShaderDescription()
         .setOptionUsage();
        subrangeKey_ =
            new SubrangeConfigKey( SubrangeConfigKey
                                  .createShaderClipMeta( axname, axName ) );
        logKey_ = PlaneSurfaceFactory.createAxisLogKey( axName );
        flipKey_ = PlaneSurfaceFactory.createAxisFlipKey( axName );
        nullcolorKey_ = new ColorConfigKey(
            ColorConfigKey.createColorMeta( axname + "null",
                                            "points with a null value of the "
                                          + axName + " coordinate" )
           .appendXmlDescription( new String[] {
                "<p>If the value is null, then points with a null",
                axName,
                "value will not be plotted at all.",
                "</p>",
            } )
            , Color.GRAY, true );
    }

    public ConfigKey[] getKeys() {
        return new ConfigKey[] {
            shaderKey_,
            subrangeKey_,
            logKey_,
            flipKey_,
            nullcolorKey_,
        };
    }

    public Ramp createValue( ConfigMap config ) {
        final Shader shader =
            StyleKeys.createShader( config, shaderKey_, subrangeKey_ );
        final boolean log = config.get( logKey_ );
        final boolean flip = config.get( flipKey_ );
        final Color nullcolor = config.get( nullcolorKey_ );
        return new Ramp() {
            public Shader getShader() {
                return shader;
            }
            public boolean isLog() {
                return log;
            }
            public boolean isFlip() {
                return flip;
            }
            public Color getNullColor() {
                return nullcolor;
            }
            public ShadeAxisFactory
                    createShadeAxisFactory( final Captioner captioner,
                                            final String label ) {
                return new ShadeAxisFactory() {
                    public boolean isLog() {
                        return log;
                    }
                    public ShadeAxis createShadeAxis( Range range ) {
                        if ( range == null ) {
                            range = new Range();                
                        }
                        double[] bounds = range.getFiniteBounds( log );
                        double lo = bounds[ 0 ];
                        double hi = bounds[ 1 ];
                        return new ShadeAxis( shader, log, flip, lo, hi,
                                              label, captioner );
                    }
                };
            }
        };
    }

    /**
     * Defines ramp characteristics.
     *
     * <p>Possibly the code could be simplified by combining the functions
     * of this class and ShadeAxisFactory.
     */
    public interface Ramp {

        /**
         * Creates a ShadeAxisFactory for this ramp.
         *
         * @param  captioner  shader ramp captioner
         * @param  label   shader ramp label
         * @return   new factory
         */
        ShadeAxisFactory createShadeAxisFactory( Captioner captioner,
                                                 String label );

        /**
         * Returns this ramp's shader.
         *
         * @return  shader
         */
        Shader getShader();

        /**
         * Indicates whether this ramp is logarithmic.
         *
         * @return   true for logarithmic scaling, false for linear
         */
        boolean isLog();

        /**
         * Indicates whether this ramp is inverted.
         *
         * @return   true if the ramp is flipped
         */
        boolean isFlip();

        /**
         * Returns the colour in which items with no ramp data value
         * should be painted.
         * 
         * @return  null colour; if null, blank values are not painted
         */
        Color getNullColor();
    }

    /**
     * Returns a list of shaders suitable for aux axis shading.
     *
     * @return  shaders
     */
    private static Shader[] createAuxShaders() {
        List<Shader> shaderList = new ArrayList<Shader>();
        shaderList.addAll( Arrays.asList( new Shader[] {
            Shaders.LUT_RAINBOW,
            Shaders.LUT_GLNEMO2,
            Shaders.LUT_PASTEL,
            Shaders.LUT_ACCENT,
            Shaders.LUT_GNUPLOT,
            Shaders.LUT_GNUPLOT2,
            Shaders.LUT_CUBEHELIX,
            Shaders.LUT_SPECXB2Y,
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.LUT_HEAT,
            Shaders.LUT_COLD,
            Shaders.LUT_LIGHT,
            Shaders.LUT_COLOR,
            Shaders.WHITE_BLACK,
            Shaders.LUT_STANDARD,
            Shaders.createMaskShader( "Mask", 0f, 1f, true ),
            Shaders.FIX_HUE,
            Shaders.TRANSPARENCY,
            Shaders.FIX_INTENSITY,
            Shaders.FIX_RED,
            Shaders.FIX_GREEN,
            Shaders.FIX_BLUE,
            Shaders.HSV_H,
            Shaders.HSV_S,
            Shaders.HSV_V,
            Shaders.FIX_Y,
            Shaders.FIX_U,
            Shaders.FIX_V,
            Shaders.BREWER_BUGN,
            Shaders.BREWER_BUPU,
            Shaders.BREWER_ORRD,
            Shaders.BREWER_PUBU,
            Shaders.BREWER_PURD,
        } ) );
        shaderList.addAll( Arrays.asList( Shaders.getCustomShaders() ) );
        return shaderList.toArray( new Shader[ 0 ] );
    }
}
