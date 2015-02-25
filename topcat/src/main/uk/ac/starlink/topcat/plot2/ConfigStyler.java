package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.JOptionPane;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Manages creation of PlotLayers from Plotters by turning ConfigMaps into
 * appropriate Style instances.
 * This would be just a case of calling the relevant Plotter method,
 * except that method can throw a ConfigException, and we have to manage
 * behaviour in the case that that happens.
 *
 * This is currently done by popping up a dialogue window the first time the
 * error occurs.
 * 
 * @author   Mark Taylor
 * @since    25 Feb 2015
 */
public class ConfigStyler {

    private final Component parent_;
    private boolean lastFailed_;

    /**
     * Constructor.
     *
     * @param   parent   parent component for dialogue windows
     */
    public ConfigStyler( Component parent ) {
        parent_ = parent;
    }

    /**
     * Creates a new layer from a plotter.
     *
     * @param  plotter  plotter
     * @param  geom  data geom
     * @param  dataSpec   data spec
     * @param  config   style configuration
     * @return   layer, or null in case of failure
     */
    public <S extends Style> PlotLayer createLayer( Plotter<S> plotter,
                                                    DataGeom geom,
                                                    DataSpec dataSpec,
                                                    ConfigMap config ) {
        S style;
        try {
            style = plotter.createStyle( config );
            lastFailed_ = false;
        }
        catch ( ConfigException e ) {
            if ( ! lastFailed_ ) {
                String name = e.getConfigKey().getMeta().getLongName();
                String[] msg = new String[] { name + ": ", e.getMessage() };
                JOptionPane.showMessageDialog( parent_, msg, name + " Error",
                                               JOptionPane.ERROR_MESSAGE );
                lastFailed_ = true;
            }
            return null;
        }
        try {
            S dupStyle = plotter.createStyle( config );
            assert style.equals( dupStyle );
            assert style.hashCode() == dupStyle.hashCode();
        }
        catch ( ConfigException e ) {
            assert false : "Shouldn't happen";
        }
        return plotter.createLayer( geom, dataSpec, style );
    }
}
