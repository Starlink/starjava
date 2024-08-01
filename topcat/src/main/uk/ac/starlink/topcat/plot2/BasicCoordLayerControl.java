package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.AlignedBox;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * LayerControl for a single plotter with coordinates.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 */
public class BasicCoordLayerControl extends SingleZoneLayerControl {

    private final Plotter<?> plotter_;
    private final TablesListComboBox tableSelector_;
    private final PositionCoordPanel coordPanel_;
    private final Configger baseConfigger_;
    private final boolean autoPopulate_;
    private final JComboBox<RowSubset> subsetSelector_;
    private final ComboBoxModel<RowSubset> dummyComboBoxModel_;
    private final ConfigStyler styler_;
    private final Specifier<ZoneId> zsel_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param   plotter  plotter
     * @param   zsel    zone id specifier, may be null for single-zone case
     * @param   coordPanel   panel which displays the plotter's coordinates,
     *                       and supplies a DataGeom
     * @param   tablesModel  list of available tables
     * @param   baseConfigger   provides global configuration info
     * @param  autoPopulate  if true, when the table is changed an attempt
     *                       will be made to initialise the coordinate fields
     *                       with some suitable values
     */
    @SuppressWarnings("this-escape")
    public BasicCoordLayerControl( Plotter<?> plotter, Specifier<ZoneId> zsel,
                                   PositionCoordPanel coordPanel,
                                   ListModel<TopcatModel> tablesModel,
                                   Configger baseConfigger,
                                   boolean autoPopulate ) {
        super( (String) null, plotter.getPlotterIcon(), zsel );
        plotter_ = plotter;
        zsel_ = zsel;
        coordPanel_ = coordPanel;
        baseConfigger_ = baseConfigger;
        autoPopulate_ = true;
        styler_ = new ConfigStyler( coordPanel_.getComponent() );

        /* Create data selection components. */
        tableSelector_ = new TablesListComboBox( tablesModel, 250 );
        subsetSelector_ = new JComboBox<RowSubset>();
        dummyComboBoxModel_ = subsetSelector_.getModel();

        /* Ensure listeners are notified. */
        final ActionListener forwarder = getActionForwarder();
        tableSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                tcModel_ = (TopcatModel) tableSelector_.getSelectedItem();
                tableChanged( tcModel_ );
                forwarder.actionPerformed( new ActionEvent( this, 0,
                                                            "Table" ) );
            }
        } );
        coordPanel_.addActionListener( forwarder );
        subsetSelector_.addActionListener( forwarder );

        /* Configure panel for specifying the data. */
        JComponent dataPanel = AlignedBox.createVerticalBox();
        JComponent tline = Box.createHorizontalBox();
        tline.add( new JLabel( "Table: " ) );
        tline.add( tableSelector_ );
        dataPanel.add( tline );
        dataPanel.add( Box.createVerticalStrut( 5 ) );
        dataPanel.add( coordPanel_.getComponent() );
        dataPanel.add( Box.createVerticalStrut( 5 ) );
        dataPanel.add( new LineBox( "Row Subset",
                                    new ShrinkWrapper( subsetSelector_ ),
                                    true ) );

        /* Configure panel for specifying style.
         * If any of the config keys are supplied by the base configger,
         * don't re-acquire them here. */
        List<ConfigKey<?>> klist = new ArrayList<ConfigKey<?>>();
        klist.addAll( Arrays.asList( plotter.getStyleKeys() ) );
        klist.removeAll( baseConfigger_.getConfig().keySet() );
        klist.removeAll( Arrays.asList( coordPanel_.getConfigSpecifier()
                                                   .getConfigKeys() ) );
        ConfigKey<?>[] keys = klist.toArray( new ConfigKey<?>[ 0 ] );
        ConfigSpecifier styleSpecifier = new ConfigSpecifier( keys );

        /* Add tabs. */
        addControlTab( "Data", dataPanel, true );
        if ( styleSpecifier.getConfigKeys().length > 0 ) {
            addSpecifierTab( "Style", styleSpecifier );
        }
        if ( zsel != null ) {
            addZoneTab( zsel );
        }
    }

    @Override
    public String getControlLabel() {
        return tcModel_ == null ? "<no table>" : tcModel_.toString();
    }

    public Plotter<?>[] getPlotters() {
        return new Plotter<?>[] { plotter_ };
    }

    protected SingleZoneLayer getSingleZoneLayer() {
        RowSubset subset =
            subsetSelector_.getItemAt( subsetSelector_.getSelectedIndex() );
        GuiCoordContent[] coordContents = coordPanel_.getContents();
        if ( tcModel_ == null || coordContents == null || subset == null ) {
            return null;
        }
        DataGeom geom = coordPanel_.getDataGeom();
        DataSpec dataSpec = new GuiDataSpec( tcModel_, subset, coordContents );
        ConfigMap config = getConfig();
        config.putAll( baseConfigger_.getConfig() );
        config.putAll( coordPanel_.getConfig() );
        PlotLayer plotLayer =
            styler_.createLayer( plotter_, geom, dataSpec, config );
        return plotLayer == null
             ? null
             : new SingleZoneLayer( plotLayer, config, null, tcModel_,
                                    coordContents, subset );
    }

    public String getCoordLabel( String userCoordName ) {
        return GuiCoordContent
              .getCoordLabel( userCoordName, coordPanel_.getContents() );
    }

    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
    }

    public Specifier<ZoneId> getZoneSpecifier() {
        return zsel_;
    }

    public TablesListComboBox getTableSelector() {
        return tableSelector_;
    }

    @Override
    public ConfigMap getConfig() {
        ConfigMap config = super.getConfig();
        config.putAll( coordPanel_.getConfig() );
        return config;
    }

    /**
     * Sets in the GUI the topcat model for which this control
     * is making plots.
     *
     * @param  tcModel  new topcat model
     */
    public void setTopcatModel( TopcatModel tcModel ) {
        tableSelector_.setSelectedItem( tcModel );
    }

    /**
     * Returns the topcat model currently selected in the GUI.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        Object selected = tableSelector_.getSelectedItem();
        return selected instanceof TopcatModel ? (TopcatModel) selected : null;
    }

    /**
     * Returns the coord panel.
     *
     * @return  coord panel
     */
    protected PositionCoordPanel getCoordPanel() {
        return coordPanel_;
    }

    /**
     * Called when the TopcatModel for which this control is generating plots
     * is changed.  Usually this will be because the user has selected
     * a new table from the table selector.
     *
     * @param   tcModel   new topcat model, may be null
     */
    protected void tableChanged( TopcatModel tcModel ) {
        coordPanel_.setTable( tcModel, autoPopulate_ );

        /* Set up subset selector. */
        final ComboBoxModel<RowSubset> subselModel;
        if ( tcModel == null ) {
            subselModel = dummyComboBoxModel_;
        }
        else {
            subselModel = tcModel.getSubsets().makeComboBoxModel();
            subselModel.setSelectedItem( tcModel.getSelectedSubset() );
        }
        subsetSelector_.setModel( subselModel );
    }
}
