package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Layer control not related to a table.
 * Note that the style should have a <code>toString</code> method 
 * which gives a sensible legend label for the given layer.
 *
 * <p>This class is not as general as it looks - there is currently
 * only one dataless plotter,
 * {@link uk.ac.starlink.ttools.plot2.layer.FunctionPlotter},
 * and some of this implementation (FUNCLABEL_KEY) is specific to that.
 * Will need to revise if other dataless plotters are introduced.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class DatalessLayerControl<S extends Style> extends ConfigControl
                                                   implements LayerControl {

    private final Plotter<S> plotter_;
    private static final ConfigKey<String> FUNCLABEL_KEY =
        new StringConfigKey( new ConfigMeta( "label", "Label" ), "Function" );

    /**
     * Constructor.
     *
     * @param  plotter   dataless plotter
     */
    public DatalessLayerControl( Plotter<S> plotter ) {
        super( plotter.getPlotterName(), plotter.getPlotterIcon() );
        plotter_ = plotter;
        AutoConfigSpecifier legendSpecifier =
            new AutoConfigSpecifier( new ConfigKey[] { FUNCLABEL_KEY } );
        final AutoSpecifier<String> labelSpecifier =
            legendSpecifier.getAutoSpecifier( FUNCLABEL_KEY );
        final ConfigSpecifier styleSpecifier =
            new ConfigSpecifier( plotter.getStyleKeys() );
        addSpecifierTab( "Style", styleSpecifier );
        addSpecifierTab( "Label", legendSpecifier );

        /* Fix it so the default value of the legend label is the
         * text of the function. */
        labelSpecifier.setAutoValue( plotter.getPlotterName() );
        styleSpecifier.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                ConfigMap config = styleSpecifier.getSpecifiedValue();
                S style;
                try {
                    style = plotter_.createStyle( config );
                }
                catch ( ConfigException e ) {
                    styleSpecifier.reportError( e );
                    style = null;
                }
                labelSpecifier.setAutoValue( style == null
                                           ? plotter_.getPlotterName()
                                           : style.toString() );
            }
        } );
    }

    public PlotLayer[] getPlotLayers() {
        S style = plotter_.createStyle( getConfig() );
        PlotLayer layer = plotter_.createLayer( null, null, style );
        return layer == null ? new PlotLayer[ 0 ] : new PlotLayer[] { layer };
    }

    public LegendEntry[] getLegendEntries() {
        ConfigMap config = getConfig();
        S style = plotter_.createStyle( config );
        String label = config.get( FUNCLABEL_KEY );
        return style != null && label != null
             ? new LegendEntry[] { new LegendEntry( label, style ) }
             : new LegendEntry[ 0 ];
    }

    public String getCoordLabel( String userCoordName ) {
        return null;
    }

    public TopcatModel getTopcatModel( DataSpec dataSpec ) {
        return null;
    }

    public static Action createStackAction( final ControlStack stack,
                                            final Plotter plotter ) {
        Action act = new AbstractAction( "Add " + plotter.getPlotterName()
                                       + " Layer",
                                         plotter.getPlotterIcon() ) {
            public void actionPerformed( ActionEvent evt ) {
                @SuppressWarnings( "unchecked" )
                DatalessLayerControl control =
                    new DatalessLayerControl( plotter );
                stack.addControl( control );
            }
        };
        act.putValue( Action.SHORT_DESCRIPTION,
                      "Add a new " + plotter.getPlotterName().toLowerCase()
                    + " layer control to the stack" );
        return act;
    }
}
