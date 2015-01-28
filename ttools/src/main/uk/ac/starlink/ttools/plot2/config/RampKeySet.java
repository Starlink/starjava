package uk.ac.starlink.ttools.plot2.config;

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
    private final ConfigKey<Subrange> shadeclipKey_;
    private final ConfigKey<Boolean> flipKey_;
    private final OptionConfigKey<Scaling> scalingKey_;
    private final ConfigKey<Subrange> dataclipKey_;
    private final ConfigKey[] keys_;

    /**
     * Constructor.
     *
     * @param  axname  short form of axis name, used in text parameter names
     * @param  axName  long form of axis name, used in descriptions
     * @param  shaders  array of preset shader options
     * @param  dfltScaling  default scaling function
     * @param  hasDataclip  true iff a data subrange key is to be included
     */
    public RampKeySet( String axname, String axName, Shader[] shaders,
                       Scaling dfltScaling, boolean hasDataclip ) {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();

        List<Shader> shaderList = new ArrayList<Shader>();
        shaderList.addAll( Arrays.asList( shaders ) );
        shaderList.addAll( Arrays.asList( Shaders.getCustomShaders() ) );
        shaderKey_ = new ShaderConfigKey(
            new ConfigMeta( axname + "map", axName + " Shader" )
           .setShortDescription( "Color map for " + axName + " shading" )
           .setXmlDescription( new String[] {
                "<p>Color map used for",
                axName,
                "axis shading.",
                "</p>",
            } )
            , shaderList.toArray( new Shader[ 0 ] ), shaderList.get( 0 )
        ).appendShaderDescription()
         .setOptionUsage();
        keyList.add( shaderKey_ );

        shadeclipKey_ =
            new SubrangeConfigKey( SubrangeConfigKey
                                  .createShaderClipMeta( axname, axName ) );
        keyList.add( shadeclipKey_ );

        ConfigMeta flipMeta = new ConfigMeta( axname + "flip", "Shader Flip" );
        flipMeta.setShortDescription( "Flip " + axName + " colour ramp?" );
        flipMeta.setXmlDescription( new String[] {
            "<p>If true, the colour map on the",
            axName,
            "axis will be reversed.",
            "</p>",
        } );
        flipKey_ = new BooleanConfigKey( flipMeta );
        keyList.add( flipKey_ );

        ConfigMeta scalingMeta = new ConfigMeta( axname + "func", "Scaling" );
        scalingMeta.setShortDescription( axName + " scaling function" );
        scalingMeta.setXmlDescription( new String[] {
            "<p>Defines the way that values in the",
            axName,
            "range are mapped to the selected colour ramp.",
            "</p>",
        } );
        scalingKey_ =
            new OptionConfigKey<Scaling>( scalingMeta, Scaling.class,
                                          Scaling.getStretchOptions(),
                                          dfltScaling );
        scalingKey_.setOptionUsage();
        scalingKey_.addOptionsXml();
        keyList.add( scalingKey_ );

        dataclipKey_ =
            new SubrangeConfigKey( SubrangeConfigKey
                                  .createAxisSubMeta( axname, axName ) );
        if ( hasDataclip ) {
            keyList.add( dataclipKey_ );
        }

        keys_ = keyList.toArray( new ConfigKey[ 0 ] );
    }

    public ConfigKey[] getKeys() {
        return keys_;
    }

    public Ramp createValue( ConfigMap config ) {

        /* Determine configured shader instance. */
        Shader shader = config.get( shaderKey_ );
        Subrange shadeclip = config.get( shadeclipKey_ );
        boolean isFlip = config.get( flipKey_ );
        if ( ! Subrange.isIdentity( shadeclip ) ) {
            shader = Shaders.stretch( shader, (float) shadeclip.getLow(),
                                              (float) shadeclip.getHigh() );
        }
        if ( isFlip ) {
            shader = Shaders.invert( shader );
        }

        /* Determine configured scaling instance. */
        Scaling scaling = config.get( scalingKey_ );
        Subrange dataclip = config.get( dataclipKey_ );
        if ( ! Subrange.isIdentity( dataclip ) ) {
            scaling = Scaling.subrangeScaling( scaling, dataclip );
        }

        /* Construct and return a Ramp instance. */
        final Shader shader0 = shader;
        final Scaling scaling0 = scaling;
        return new Ramp() {
            public Shader getShader() {
                return shader0;
            }
            public Scaling getScaling() {
                return scaling0;
            }
        };
    }

    /**
     * Creates a ShadeAxisFactory for a given ramp.
     *
     * @param  ramp   ramp
     * @param  captioner  shader ramp captioner
     * @param  label   shader ramp label
     * @param  crowding   tick crowding factor (1 is normal)
     * @return   new factory
     */
    public static ShadeAxisFactory
            createShadeAxisFactory( Ramp ramp, final Captioner captioner,
                                    final String label,
                                    final double crowding ) {
        final Shader shader = ramp.getShader();
        final Scaling scaling = ramp.getScaling();
        final boolean isLog = scaling.isLogLike();
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

    /**
     * Defines ramp characteristics by aggregating a Shader and a Scaling.
     */
    public interface Ramp {

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
    }
}
