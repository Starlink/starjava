package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * LayerControl for a single plotter with coordinates.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 */
public class BasicCoordLayerControl extends ConfigControl
                                    implements LayerControl {

    private final Plotter<?> plotter_;
    private final TablesListComboBox tableSelector_;
    private final PositionCoordPanel coordPanel_;
    private final JComboBox subsetSelector_;
    private final ComboBoxModel dummyComboBoxModel_;
    private final ReportLogger reportLogger_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param   plotter  plotter
     * @param   coordPanel   panel which displays the plotter's coordinates,
     *                       and supplies a DataGeom
     */
    public BasicCoordLayerControl( Plotter<?> plotter,
                                   PositionCoordPanel coordPanel ) {
        super( null, plotter.getPlotterIcon() );
        plotter_ = plotter;
        coordPanel_ = coordPanel;
        reportLogger_ = new ReportLogger( this );

        /* Create data selection components. */
        tableSelector_ = new TablesListComboBox();
        subsetSelector_ = new JComboBox();
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
        JComponent dataPanel = Box.createVerticalBox();
        dataPanel.add( new LineBox( "Table",
                                    new ShrinkWrapper( tableSelector_ ),
                                    true ) );
        dataPanel.add( coordPanel_.getComponent() );
        dataPanel.add( Box.createVerticalStrut( 5 ) );
        dataPanel.add( new LineBox( "Row Subset: ",
                                    new ShrinkWrapper( subsetSelector_ ),
                                    true ) );

        /* Configure panel for specifying style. */
        ConfigSpecifier styleSpecifier =
            new ConfigSpecifier( plotter.getStyleKeys() );

        /* Add tabs. */
        addControlTab( "Data", dataPanel, true );
        addSpecifierTab( "Style", styleSpecifier );
    }

    @Override
    public String getControlLabel() {
        return tcModel_ == null ? "<no table>" : tcModel_.toString();
    }

    public PlotLayer[] getPlotLayers() {
        RowSubset subset = (RowSubset) subsetSelector_.getSelectedItem();
        GuiCoordContent[] coordContents = coordPanel_.getContents();
        if ( tcModel_ == null || coordContents == null || subset == null ) {
            return new PlotLayer[ 0 ];
        }
        DataGeom geom = coordPanel_.getDataGeom();
        DataSpec dataSpec = new GuiDataSpec( tcModel_, subset, coordContents );
        ConfigMap config = getConfig();
        PlotLayer layer = createLayer( plotter_, geom, dataSpec, config );
        return layer == null ? new PlotLayer[ 0 ] : new PlotLayer[] { layer };
    }

    public String getCoordLabel( String userCoordName ) {
        return GuiCoordContent
              .getCoordLabel( userCoordName, coordPanel_.getContents() );
    }

    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
    }

    public void submitReports( Map<LayerId,ReportMap> reports ) {
        reportLogger_.submitReports( reports );
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
        coordPanel_.setTable( tcModel, false );

        /* Set up subset selector. */
        final ComboBoxModel subselModel;
        if ( tcModel == null ) {
            subselModel = dummyComboBoxModel_;
        }
        else {
            subselModel = tcModel.getSubsets().makeComboBoxModel();
            subselModel.setSelectedItem( RowSubset.ALL );
        }
        subsetSelector_.setModel( subselModel );
    }

    /**
     * Creates a new layer from a plotter.
     *
     * @param  plotter  plotter
     * @param  geom  data geom
     * @param  dataSpec   data spec
     * @param  config   style configuration
     */
    private static <S extends Style>
            PlotLayer createLayer( Plotter<S> plotter, DataGeom geom,
                                   DataSpec dataSpec, ConfigMap config ) {
        S style = plotter.createStyle( config );
        return plotter.createLayer( geom, dataSpec, style );
    }
}
