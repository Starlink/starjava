package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Input;

/**
 * Specifies the characteristics of a new plot layer to add to a plot.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2013
 */
public class LayerCommand {

    private final Plotter plotter_;
    private final TopcatModel tcModel_;
    private final Map<String,String> inputValues_;
    private final ConfigMap config_;
    private final RowSubset rset_;

    /**
     * Constructor.
     *
     * @param   plotter   plotter
     * @param   tcModel   table providing plot data
     * @param   inputValues  string values to be entered into column input
     *          fields, keyed by coordinate input short name
     * @param   config    configuration options to apply to the plot;
     *                    default values will be used for any not supplied
     * @param   rset     row subset for which the plot will be made
     */
    public LayerCommand( Plotter plotter, TopcatModel tcModel,
                         Map<String,String> inputValues, ConfigMap config,
                         RowSubset rset ) {
        plotter_ = plotter;
        tcModel_ = tcModel;
        inputValues_ = inputValues;
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
     * Returns a mapping which gives the values of the input
     * coordinates used by the layer.
     * The map keys are obtained from {@link #getInputName}.
     * The map values are the strings that appear in column selectors
     * or on a command line to specify the column value - generally a
     * column name or JEL expression.
     *
     * @return   user coordinate name-&gt;specification map
     */
    public Map<String,String> getInputValues() {
        return inputValues_;
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
              .append( inputValues_ )
              .append( "; " )
              .append( config_ )
              .append( "; " )
              .append( rset_ )
              .toString();
    }

    /**
     * Obtains a unique name for an input coordinate specifier.
     *
     * @param  input   input coordinate specifier
     * @return  name suitable as map key
     */
    public static String getInputName( Input input ) {
        return input.getMeta().getShortName();
    }
}
