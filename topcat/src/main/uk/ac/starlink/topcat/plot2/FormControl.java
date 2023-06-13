package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Control for providing coordinate and configuration information
 * additional to base position coords and config.
 * Combining the information gathered from this control with 
 * base config and coords provides enough to generate a PlotLayer.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public abstract class FormControl implements Control {

    private final ActionForwarder forwarder_;
    private final Configger baseConfigger_;
    private SubsetConfigManager subManager_;
    private FormStylePanel stylePanel_;
    private JComponent panel_;
    private ConfigStyler styler_;
    private ReportPanel reportPanel_;

    /**
     * Constructor.
     *
     * @param  baseConfigger  provides global configuration info
     */
    protected FormControl( Configger baseConfigger ) {
        forwarder_ = new ActionForwarder();
        baseConfigger_ = baseConfigger;
    }

    /**
     * Returns the panel into which the user enters additional coordinate
     * information required for this control, if any.
     *
     * @return   coordinate entry panel
     */
    protected abstract JComponent getCoordPanel();

    /**
     * Returns the Plotter object which generates plot layers from the
     * information provided by this control
     *
     * @return  plotter
     */
    protected abstract Plotter<?> getPlotter();

    /**
     * Returns the keys defining the additional configuration required
     * by this control.
     *
     * @return   config keys
     */
    protected abstract ConfigKey<?>[] getConfigKeys();

    public JComponent getPanel() {
        if ( panel_ == null ) {
            panel_ = new JPanel( new BorderLayout() );
            final JComponent coordPanel = getCoordPanel();
            panel_.add( coordPanel, BorderLayout.NORTH );
            if ( getPlotter().hasReports() ) {
                reportPanel_ = new ReportPanel( this::getPlotter ) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension( coordPanel.getPreferredSize()
                                                        .width,
                                              super.getPreferredSize().height );
                    }
                };
                reportPanel_.setBorder( AuxWindow
                                       .makeTitledBorder( "Report" ) );
                panel_.add( reportPanel_, BorderLayout.SOUTH );
            }
        }
        return panel_;
    }

    /**
     * Returns this component's ConfigStyler.
     *
     * @return  config styler
     */
    private synchronized ConfigStyler getStyler() {

        /* Constructed lazily because it needs the component. */
        if ( styler_ == null ) {
            styler_ = new ConfigStyler( getPanel() );
        }
        return styler_;
    }

    /**
     * Returns the data and metadata for the additional coordinates
     * entered by the user in this control.
     * If these constitute insufficient information to generate a plot
     * (not all required coords have been filled in), null is returned.
     *
     * @return   extra coordinate information, or null if incomplete
     */ 
    public abstract GuiCoordContent[] getExtraCoordContents();

    /**
     * Returns the additional configuration entered by the user in
     * this control.
     *
     * @return  extra config information
     */
    public abstract ConfigMap getExtraConfig();

    /**
     * Configures this control for use with a given TopcatModel.
     * The subset manager is supplied as well to provide subset-specific
     * configuration defaults.
     *
     * @param  tcModel   topcat model
     * @param  subManager  subset manager with info about the row subsets
     *                     for <code>tcModel</code>
     * @param  subStack   subset stack controlling/displaying subset visibility
     */
    public void setTable( TopcatModel tcModel, SubsetConfigManager subManager,
                          SubsetStack subStack ) {
        JComponent panel = getPanel();

        /* Clear out an old style panel if there was one. */
        if ( stylePanel_ != null ) {
            stylePanel_.removeActionListener( forwarder_ );
            panel.remove( stylePanel_ );
        }

        /* Add a new style panel if the table is non-null. */
        if ( tcModel != null ) {

            /* If any of the config keys are supplied by the base configger,
             * don't acquire them from the layer style panel. */
            List<ConfigKey<?>> klist = new ArrayList<ConfigKey<?>>();
            klist.addAll( Arrays.asList( getConfigKeys() ) );
            klist.removeAll( baseConfigger_.getConfig().keySet() );
            ConfigKey<?>[] keys = klist.toArray( new ConfigKey<?>[ 0 ] );
            FormStylePanel sp =
                new FormStylePanel( keys, baseConfigger_, this::getPlotter,
                                    subManager, subStack, tcModel );
            if ( stylePanel_ != null ) {
                sp.configureFrom( stylePanel_ );
            }
            stylePanel_ = sp;
            stylePanel_.addActionListener( forwarder_ );
            panel.add( stylePanel_, BorderLayout.CENTER );
        }
        else {
            stylePanel_ = null;
        }

        /* Install the subset manager and table. */
        subManager_ = subManager;
        setTable( tcModel );
    }

    /**
     * Performs implementation-specific configuration of this control
     * for a given TopcatModel.
     *
     * @param  tcModel  new topcat model to install
     */
    protected abstract void setTable( TopcatModel tcModel );

    /**
     * Returns the style panel currently in use.
     *
     * @return  style panel
     */
    public FormStylePanel getStylePanel() {
        return stylePanel_;
    }

    /**
     * Creates a plot layer given the current state of this control and
     * some additional information. 
     *
     * @param  geom  data position geometry
     * @param  dataSpec  data specification, which must contain any data
     *                   required by this control's extra coords and be
     *                   set up for the given subset
     * @param  subset   row subset in the current table for which the
     *                  layer is to be plotted
     * @return   new plot layer, may be null in case of incorrect GUI config
     */
    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  RowSubset subset ) {
        if ( stylePanel_ != null ) {
            ConfigMap config = stylePanel_.getConfig( subset.getKey() );
            config.putAll( getExtraConfig() );
            return getStyler()
                  .createLayer( getPlotter(), geom, dataSpec, config );
        }
        else {
            return null;
        }
    }

    /**
     * Returns an object which will forward actions to listeners registered
     * with this panel.
     *
     * @return  action forwarder
     */
    public ActionListener getActionForwarder() {
        return forwarder_;
    }

    /**
     * Adds a listener to be messaged when the state of this control
     * changes in a way which might affect the plot layer it would create.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a previously added listener.
     *
     * @param   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public String getControlLabel() {
        return getPlotter().getPlotterName();
    }

    public Icon getControlIcon() {
        return getPlotter().getPlotterIcon();
    }

    /**
     * Accepts plot reports generated by plotting layers.
     * The supplied map is indexed by RowSubset.
     *
     * @param  reports  map of row subsets to plot reports
     */
    public void submitReports( Map<RowSubset,ReportMap> reports ) {
        if ( reportPanel_ != null ) {
            reportPanel_.submitReports( reports );
        }
        if ( stylePanel_ != null ) {
            stylePanel_.submitReports( reports );
        }
    }
}
