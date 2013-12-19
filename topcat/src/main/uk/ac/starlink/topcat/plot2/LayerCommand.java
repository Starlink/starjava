package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Specifies the characteristics of a new plot layer to add to a plot.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2013
 */
public class LayerCommand {

    private final Plotter plotter_;
    private final TopcatModel tcModel_;
    private final Map<String,String> coordValues_;
    private final ConfigMap config_;
    private final RowSubset rset_;

    /**
     * Constructor.
     *
     * @param   plotter   plotter
     * @param   tcModel   table providing plot data
     * @param   coordValues  string values to be entered into column input
     *          fields, keyed by coordinate name
     * @param   config    configuration options to apply to the plot;
     *                    default values will be used for any not supplied
     * @param   rset     row subset for which the plot will be made
     */
    public LayerCommand( Plotter plotter, TopcatModel tcModel,
                         Map<String,String> coordValues, ConfigMap config,
                         RowSubset rset ) {
        plotter_ = plotter;
        tcModel_ = tcModel;
        coordValues_ = coordValues;
        config_ = config;
        rset_ = rset;
    }

    /**
     * Returns the plotter that will generate the layer.
     *
     * @return   plotter
     */
    public Plotter getPlotter() {
        return plotter_;
    }

    /**
     * Returns the table supplying the table data.
     * May be null for a data-less layer.
     *
     * @return   table
     */
    public TopcatModel getTopcatModel() {
        return tcModel_;
    }

    /**
     * Returns a mapping which gives the values of the coordinates used
     * by the layer.
     * The map keys are the names of the user coordinates
     * (<code>Coord.getUserInfos()[i].getName()</code>).
     * The map values are the strings that appear in column selectors
     * or on a command line to specify the column value - generally a
     * column name or JEL expression.
     *
     * @return   user coordinate name->specification map
     */
    public Map<String,String> getCoordValues() {
        return coordValues_;
    }

    /**
     * Returns a configuration map containing any explicit values
     * required for layer configuration.
     * Any unspecified options will take their default values.
     *
     * @return  explicit configuration options
     */
    public ConfigMap getConfig() {
        return config_;
    }

    /**
     * Returns the row subset for which the layer is to be plotted.
     *
     * @return  row subset
     */
    public RowSubset getRowSubset() {
        return rset_;
    }

    @Override
    public String toString() {
        return new StringBuffer()
              .append( plotter_.getPlotterName() )
              .append( "; " )
              .append( tcModel_.toString() )
              .append( "; " )
              .append( coordValues_ )
              .append( "; " )
              .append( config_ )
              .append( "; " )
              .append( rset_ )
              .toString();
    }
}
