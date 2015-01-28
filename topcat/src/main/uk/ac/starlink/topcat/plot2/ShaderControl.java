package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;

/**
 * Control for configuring shader scale and axis characteristics.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class ShaderControl extends ConfigControl {

    private final ControlStackModel stackModel_;
    private final Configger configger_;
    private final AutoSpecifier<String> labelSpecifier_;
    private final AutoSpecifier<Boolean> visibleSpecifier_;
    private final ConfigSpecifier rangeSpecifier_;
    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<String> AUXLABEL_KEY =
        new StringConfigKey( new ConfigMeta( "auxlabel", "Aux Axis Label" ),
                             null );
    private static final ConfigKey<Boolean> AUXVISIBLE_KEY =
        new BooleanConfigKey( new ConfigMeta( "auxaxis", "Show Scale" ),
                              false );

    /**
     * Constructor.
     *
     * @param   stackModel   model containing layer controls
     * @param   configger   config source containing some plot-wide config,
     *                      specifically captioner style
     */
    public ShaderControl( ControlStackModel stackModel, Configger configger ) {
        super( SCALE.getName() + " Axis", ResourceIcon.COLORS );
        stackModel_ = stackModel;
        configger_ = configger;
        ActionListener forwarder = getActionForwarder();

        AutoConfigSpecifier axisSpecifier = new AutoConfigSpecifier(
            new ConfigKey[] { AUXVISIBLE_KEY, AUXLABEL_KEY,
                              StyleKeys.AUX_CROWD },
            new ConfigKey[] { AUXVISIBLE_KEY, AUXLABEL_KEY, }
        );
        labelSpecifier_ = axisSpecifier.getAutoSpecifier( AUXLABEL_KEY );
        visibleSpecifier_ = axisSpecifier.getAutoSpecifier( AUXVISIBLE_KEY );
        labelSpecifier_.setAutoValue( null );
        visibleSpecifier_.setAutoValue( false );

        stackModel.addPlotActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                adjustAutoConfig();
            }
        } );
        adjustAutoConfig();

        rangeSpecifier_ = new ConfigSpecifier( new ConfigKey[] {
            StyleKeys.SHADE_LOW, StyleKeys.SHADE_HIGH, StyleKeys.SHADE_SUBRANGE,
        } );
        rangeSpecifier_.addActionListener( forwarder );

        ConfigKey[] shaderKeys =
            PlotUtil.arrayConcat( RAMP_KEYS.getKeys(),
                                  new ConfigKey[] { StyleKeys.AUX_NULLCOLOR } );
        addSpecifierTab( "Map", new ConfigSpecifier( shaderKeys ) );
        addSpecifierTab( "Ramp", axisSpecifier );
        addSpecifierTab( "Range", rangeSpecifier_ );
    }

    /**
     * Returns an aux value range explicitly fixed by the user.
     *
     * @return   shader fixed range, either or both bounds may be absent
     */
    public Range getFixRange() {
        ConfigMap rangeConfig = rangeSpecifier_.getSpecifiedValue();
        double lo = toDouble( rangeConfig.get( StyleKeys.SHADE_LOW ) );
        double hi = toDouble( rangeConfig.get( StyleKeys.SHADE_HIGH ) );
        if ( lo > hi ) {
            hi = Double.NaN;
            rangeSpecifier_.getSpecifier( StyleKeys.SHADE_HIGH )
                           .setSpecifiedValue( new Double( hi ) );
            Toolkit.getDefaultToolkit().beep();
        }
        return new Range( lo, hi );
    }

    /**
     * Returns an aux value subrange set by the user.
     *
     * @return   shader subrange
     */
    public Subrange getSubrange() {
        return rangeSpecifier_.getSpecifiedValue()
                              .get( StyleKeys.SHADE_SUBRANGE );
    }

    /**
     * Returns an object which can turn a range into a ShadeAxis
     * based on current config of this component.
     *
     * @return   shade axis factory
     */
    public ShadeAxisFactory createShadeAxisFactory() {
        final ConfigMap config = getConfig();
        final boolean visible = config.get( AUXVISIBLE_KEY );
        if ( ! visible ) {
            return new ShadeAxisFactory() {
                public ShadeAxis createShadeAxis( Range range ) {
                    return null;
                }
                public boolean isLog() {
                    return false;
                }
            };
        }
        String label = config.get( AUXLABEL_KEY );
        double crowd = config.get( StyleKeys.AUX_CROWD ).doubleValue();
        Captioner captioner =
            StyleKeys.CAPTIONER.createValue( configger_.getConfig() );
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        return RampKeySet
              .createShadeAxisFactory( ramp, captioner, label, crowd );
    }

    public boolean isLog() {
        return RAMP_KEYS.createValue( getConfig() ).getScaling().isLogLike();
    }

    /**
     * Configures state according to the current state of the control stack.
     */
    private void adjustAutoConfig() {
        LayerControl[] controls = stackModel_.getLayerControls( true );
        labelSpecifier_.setAutoValue( getDefaultAxisLabel( controls ) );
        visibleSpecifier_.setAutoValue( hasShaders( controls ) );
    }

    /**
     * Returns a suitable shade axis label given a set of layer controls.
     *
     * @param  controls  layer control list
     * @return  shade axis label
     */
    private static String getDefaultAxisLabel( LayerControl[] controls ) {
        for ( int ic = 0; ic < controls.length; ic++ ) {
            String label = controls[ ic ].getCoordLabel( SCALE.getName() );
            if ( label != null ) {
                return label;
            }
        }
        return "Aux";
    }

    /**
     * Inticates whether there is shader usage from any of the plot
     * layers obtained from a given set of layer controls.
     *
     * @param  controls  layer control list
     * @return  true iff there is shader usage
     */
    private static  boolean hasShaders( LayerControl[] controls ) {
        for ( int ic = 0; ic < controls.length; ic++ ) {
            PlotLayer[] layers = controls[ ic ].getPlotLayers();
            for ( int il = 0; il < layers.length; il++ ) {
                if ( layers[ il ].getAuxRangers().containsKey( SCALE ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Turns a Double object into a primitive.
     *
     * @param  dval  object
     * @return   primitive
     */
    private static double toDouble( Double dval ) {
        return dval == null ? Double.NaN : dval.doubleValue();
    }
}
