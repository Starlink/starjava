package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Scaling;
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
    private final ConfigKey<Boolean> flipKey_;
    private final OptionConfigKey<Scaling> scalingKey_;
    private final ConfigKey<Color> nullcolorKey_;

    /**
     * Constructor.
     *
     * @param  axname  short form of axis name
     * @param  axName  long form of axis name
     */
    public RampKeySet( String axname, String axName ) {
        shaderKey_ = new ShaderConfigKey(
            new ConfigMeta( axname + "map", axName + " Map" )
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
        ConfigMeta flipMeta = new ConfigMeta( axname + "flip", "Flip" );
        flipMeta.setShortDescription( "Flip " + axName + " colour ramp?" );
        flipMeta.setXmlDescription( new String[] {
            "<p>If true, the colour map on the " + axName + " axis",
            "will be reversed.",
            "</p>",
        } );
        flipKey_ = new BooleanConfigKey( flipMeta );
        ConfigMeta scalingMeta = new ConfigMeta( axname + "func", "Scaling" );
        scalingMeta.setShortDescription( axName + " scaling function" );
        scalingMeta.setXmlDescription( new String[] {
            "<p>Defines the way that values in the (possibly clipped)",
            axname + " range are mapped to the selected colour ramp.",
            "</p>",
        } );
        scalingKey_ =
            new OptionConfigKey<Scaling>( scalingMeta, Scaling.class,
                                          Scaling.getStretchOptions(),
                                          Scaling.LINEAR );
        scalingKey_.setOptionUsage();
        scalingKey_.addOptionsXml();
        nullcolorKey_ = new ColorConfigKey(
            ColorConfigKey.createColorMeta( axname + "nullcolor", "Null Color",
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
            flipKey_,
            scalingKey_,
            nullcolorKey_,
        };
    }

    public Ramp createValue( ConfigMap config ) {
        final Shader shader = StyleKeys.createShader( config, shaderKey_,
                                                      subrangeKey_, flipKey_ );
        final Scaling scaling = config.get( scalingKey_ );
        final Color nullcolor = config.get( nullcolorKey_ );
        final boolean isLog = scaling.isLogLike();
        return new Ramp() {
            public Shader getShader() {
                return shader;
            }
            public Scaling getScaling() {
                return scaling;
            }
            public Color getNullColor() {
                return nullcolor;
            }
            public ShadeAxisFactory
                    createShadeAxisFactory( final Captioner captioner,
                                            final String label,
                                            final double crowding ) {
                return new ShadeAxisFactory() {
                    public boolean isLog() {
                        return isLog;
                    }
                    public ShadeAxis createShadeAxis( Range range ) {
                        if ( range == null ) {
                            range = new Range();                
                        }
                        double[] bounds = range.getFiniteBounds( isLog );
                        double lo = bounds[ 0 ];
                        double hi = bounds[ 1 ];
                        return new ShadeAxis( shader, scaling, lo, hi,
                                              label, captioner, crowding );
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
         * @param  crowding   tick crowding factor (1 is normal)
         * @return   new factory
         */
        ShadeAxisFactory createShadeAxisFactory( Captioner captioner,
                                                 String label,
                                                 double crowding );

        /**
         * Returns this ramp's shader.
         *
         * @return  shader
         */
        Shader getShader();

        /**
         * Returns the scaling function used to map data values to
         * the shader range.
         *
         * @return  scaling
         */
        Scaling getScaling();

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
            Shaders.LUT_SET1,
            Shaders.LUT_PAIRED,
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.LUT_BRG,
            Shaders.LUT_HEAT,
            Shaders.LUT_COLD,
            Shaders.LUT_LIGHT,
            Shaders.LUT_COLOR,
            Shaders.WHITE_BLACK,
            Shaders.LUT_STANDARD,
            Shaders.LUT_RAINBOW3,
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
