package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.TextFieldSpecifier;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;

/**
 * Layer control for plotting functions.
 *
 * @author   Mark Taylor
 * @since    26 Mar 2013
 */
public class FunctionLayerControl extends ConfigControl
                                  implements LayerControl {

    private final FunctionPlotter plotter_;
    private final Specifier<ZoneId> zsel_;
    private static final ConfigKey<String> FUNCLABEL_KEY =
        new StringConfigKey( new ConfigMeta( "label", "Label" ), "Function" );

    /**
     * Constructor.
     *
     * @param   plotter  function plotter
     * @param   zsel    zone id specifier, may be null for single-zone plots
     */
    @SuppressWarnings("this-escape")
    public FunctionLayerControl( FunctionPlotter plotter,
                                 Specifier<ZoneId> zsel ) {
        super( plotter.getPlotterName(), plotter.getPlotterIcon() );
        plotter_ = plotter;
        zsel_ = zsel;
        AutoConfigSpecifier legendSpecifier =
            new AutoConfigSpecifier( new ConfigKey<?>[] { FUNCLABEL_KEY,
                                                          StyleKeys.SHOW_LABEL},
                                     new ConfigKey<?>[] { FUNCLABEL_KEY } );
        final AutoSpecifier<String> labelSpecifier =
            legendSpecifier.getAutoSpecifier( FUNCLABEL_KEY );

        /* Split up style keys into two parts for more logical presentation
         * in the GUI. */
        final ConfigKey<?>[] funcKeys = plotter.getFunctionStyleKeys();
        List<ConfigKey<?>> otherKeyList =
            new ArrayList<ConfigKey<?>>( Arrays
                                        .asList( plotter.getStyleKeys() ) );
        otherKeyList.removeAll( Arrays.asList( funcKeys ) );
        final ConfigKey<?>[] otherKeys =
            otherKeyList.toArray( new ConfigKey<?>[ 0 ] );
        final FuncSpecifier funcSpecifier = new FuncSpecifier( funcKeys );
        final ConfigSpecifier otherSpecifier = new ConfigSpecifier( otherKeys );

        /* Fix it so the default value of the legend label is the
         * text of the function. */
        labelSpecifier.setAutoValue( plotter.getPlotterName() );
        funcSpecifier.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                FunctionPlotter.FunctionStyle style;
                ConfigException error;
                try {
                    style = plotter_.createStyle( getConfig() );
                    error = null;
                }
                catch ( ConfigException e ) {
                    style = null;
                    error = e;
                }
                funcSpecifier.setError( error );
                labelSpecifier.setAutoValue( style == null
                                           ? plotter_.getPlotterName()
                                           : style.toString() );
            }
        } );

        /* Add tabs. */
        addSpecifierTab( "Function", funcSpecifier );
        addSpecifierTab( "Style", otherSpecifier );
        addSpecifierTab( "Label", legendSpecifier );
        if ( zsel != null ) {
            addZoneTab( zsel );
        }
    }

    public Plotter<?>[] getPlotters() {
        return new Plotter<?>[] { plotter_ };
    }

    public boolean hasLayers() {
        ConfigMap config = getConfig();
        return plotter_.createLayer( null, null, getFunctionStyle( config ) )
               != null;
    }

    public TopcatLayer[] getLayers( Ganger<?,?> ganger ) {
        ConfigMap config = getConfig();
        PlotLayer plotLayer =
            plotter_.createLayer( null, null, getFunctionStyle( config ) );
        if ( plotLayer == null ) {
            return new TopcatLayer[ 0 ];
        }
        else {
            PlotLayer[] plotLayers = new PlotLayer[ ganger.getZoneCount() ];
            Arrays.fill( plotLayers, plotLayer );
            LegendEntry[] legents = getLegendEntries();
            String leglabel = legents.length > 0
                            ? legents[ 0 ].getLabel()
                            : null;
            TopcatLayer tcLayer =
                new TopcatLayer( plotLayers, config, leglabel );
            return new TopcatLayer[] { tcLayer };
        }
    }

    public LegendEntry[] getLegendEntries() {
        ConfigMap config = getConfig();
        FunctionPlotter.FunctionStyle style = getFunctionStyle( config );
        Boolean showLabel = config.get( StyleKeys.SHOW_LABEL );
        String label = config.get( FUNCLABEL_KEY );
        return showLabel && style != null && label != null
             ? new LegendEntry[] { new LegendEntry( label, style ) }
             : new LegendEntry[ 0 ];
    }

    public void submitReports( Map<LayerId,ReportMap> reports,
                               Ganger<?,?> ganger ) {
        // currently no reporting from Function layer
    }

    public Specifier<ZoneId> getZoneSpecifier() {
        return zsel_;
    }

    public TablesListComboBox getTableSelector() {
        return null;
    }

    public String getCoordLabel( String userCoordName ) {
        return null;
    }

    /**
     * Returns the style for a given config without error.
     * In case of ConfigException, null is returned.
     *
     * @param  config  config map
     * @return  style, or null
     */
    private FunctionPlotter.FunctionStyle getFunctionStyle( ConfigMap config ) {
        try {
            return plotter_.createStyle( config );
        }
        catch ( ConfigException e ) {
            return null;
        }
    }

    /**
     * Specifier for the function expression and related configuration items.
     * This provides special handling for display of the,
     * possibly uncompilable, function expression.
     */
    private static class FuncSpecifier extends ConfigSpecifier {
        final static ConfigKey<String> FEXPR_KEY = FunctionPlotter.FEXPR_KEY;
        final JTextField fexprField_;
        final Color okColor_;
        final Color errColor_;

        /**
         * Constructor.
         *
         * @param   funcKeys  keys  config keys for this specifier;
         *                    should include FEXPR_KEY
         */
        FuncSpecifier( ConfigKey<?>[] funcKeys ) {
            super( funcKeys );

            /* Prepare to do special manipulation of the text field
             * used for configuring the function expression. */
            Specifier<String> fexprSpecifier = getSpecifier( FEXPR_KEY );
            if ( fexprSpecifier instanceof TextFieldSpecifier ) {
                fexprField_ = ((TextFieldSpecifier) fexprSpecifier)
                             .getTextField();
            }
            else {
                assert false : "FuncSpecifier unexpected type";
                fexprField_ = null;
            }
            okColor_ = UIManager.getColor( "TextField.foreground" );
            errColor_ = UIManager.getColor( "TextField.inactiveForeground" );
            setError( null );
        }

        /**
         * Call this method when it's time to evaluate the expression.
         * The argument should be the error if there was one, or null
         * if a style could be created.
         *
         * @param  err  configuration error, or null
         */
        public void setError( ConfigException err ) {
            boolean isCompilationError = isCompilationError( err );
            if ( fexprField_ != null ) {
                fexprField_.setForeground( isCompilationError ? errColor_
                                                              : okColor_ );
            }
            if ( isCompilationError ) {
                String name = FEXPR_KEY.getMeta().getLongName();
                Object msg = new String[] {
                    name + " error:",
                    err.getMessage(),
                };
                JOptionPane.showMessageDialog( getComponent(), msg,
                                               name + "Error",
                                               JOptionPane.ERROR_MESSAGE ); 
            }
        }

        /**
         * Indicates whether a JEL compilation error is present somewhere
         * in the stack trace for a given throwable.
         *
         * @param  err  exception to test
         * @return   true iff a gnu.jel.CompilationException
         *           is in the stack trace
         */
        private static boolean isCompilationError( Throwable err ) {
            if ( err == null ) {
                return false;
            }
            else if ( err instanceof CompilationException ) {
                return true;
            }
            else {
                return isCompilationError( err.getCause() );
            }
        }
    }
}
