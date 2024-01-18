package uk.ac.starlink.topcat.plot2;

import javax.swing.JComponent;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Form control which works with a single fixed plotter.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class SimpleFormControl extends FormControl {

    private final Plotter<?> plotter_;
    private final CoordPanel extraCoordPanel_;

    /**
     * Constructor.
     *
     * @param  baseConfigger  provides global configuration info
     * @param  plotter  plotter for which this control provides style config
     * @param  extraCoords  any coordinates which are to be solicited from
     *                      the form control
     */
    public SimpleFormControl( Configger baseConfigger, Plotter<?> plotter,
                              Coord[] extraCoords ) {
        super( baseConfigger );
        plotter_ = plotter;
        extraCoordPanel_ = new BasicCoordPanel( extraCoords );
        if ( extraCoords.length > 0 ) {
            extraCoordPanel_.getComponent()
                            .setBorder( AuxWindow
                                       .makeTitledBorder( "Coordinates" ) );
        }
        extraCoordPanel_.addActionListener( getActionForwarder() );
    }

    protected Plotter<?> getPlotter() {
        return plotter_;
    }

    protected ConfigKey<?>[] getConfigKeys() {
        return plotter_.getStyleKeys();
    }

    protected JComponent getCoordPanel() {
        return extraCoordPanel_.getComponent();
    }

    public GuiCoordContent[] getExtraCoordContents() {
        return extraCoordPanel_.getContents();
    }

    public ConfigMap getExtraConfig() {
        return new ConfigMap();
    }

    protected void setTable( TopcatModel tcModel ) {
        extraCoordPanel_.setTable( tcModel, false );
    }
}
