package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * LayerControl for plotting spectrograms.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class SpectrogramLayerControl extends ConfigControl
                                     implements LayerControl {

    private final SpectrogramPlotter plotter_;
    private final TablesListComboBox tableSelector_;
    private final CoordPanel coordPanel_;
    private final JComboBox subsetSelector_;
    private final ComboBoxModel dummyComboBoxModel_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  plotter  spectrogram plotter
     */
    public SpectrogramLayerControl( SpectrogramPlotter plotter ) {
        super( null, plotter.getPlotterIcon() );
        plotter_ = plotter;
        assert ! plotter.hasPosition();

        /* Create data selection components. */
        tableSelector_ = new TablesListComboBox();
        coordPanel_ = new CoordPanel( plotter.getExtraCoords(), false );
        subsetSelector_ = new JComboBox();
        dummyComboBoxModel_ = subsetSelector_.getModel();

        /* Ensure listeners are notified. */
        final ActionListener forwarder = getActionForwarder();
        tableSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                tableChanged();
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
        dataPanel.add( coordPanel_ );
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
        SpectrogramPlotter.SpectroStyle style =
            plotter_.createStyle( getConfig() );
        DataSpec dataSpec = new GuiDataSpec( tcModel_, subset, coordContents );
        PlotLayer layer = plotter_.createLayer( null, dataSpec, style );
        return layer == null ? new PlotLayer[ 0 ] : new PlotLayer[] { layer };
    }

    public String getCoordLabel( String userCoordName ) {
        return GuiCoordContent
              .getCoordLabel( userCoordName, coordPanel_.getContents() );
    }

    /**
     * It's difficult to know how to represent a spectrogram in a legend,
     * and it's probably not necessary.  The current implementation
     * just returns an empty array.
     */
    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
    }

    public TopcatModel getTopcatModel( DataSpec dataSpec ) {
        return tcModel_.getDataModel() == dataSpec.getSourceTable()
             ? tcModel_
             : null;
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
     * Called when the TopcatModel for which this control is generating plots
     * is changed.  Usually this will be because the user has selected
     * a new table from the table selector.
     */
    private void tableChanged() {
        tcModel_ = (TopcatModel) tableSelector_.getSelectedItem();
        coordPanel_.setTable( tcModel_ );

        /* Fix default xExtent value to null, since it gives reasonable
         * results. */
        coordPanel_.getColumnSelector( plotter_.getExtentCoordIndex(), 0 )
                   .setSelectedItem( null );

        /* Replace the default column selector for spectrum.
         * The default picks any column [with content class descended
         * from Object], because that's how the ValueInfo is described.
         * But we really only want columns with numeric vector values. */
        if ( tcModel_ != null ) {
            coordPanel_
           .setColumnSelector( plotter_.getSpectrumCoordIndex(), 0,
                               createSpectrumColumnSelector( tcModel_ ) );
        }

        /* Set up subset selector. */
        final ComboBoxModel subselModel;
        if ( tcModel_ == null ) {
            subselModel = dummyComboBoxModel_;
        }
        else {
            subselModel = tcModel_.getSubsets().makeComboBoxModel();
            subselModel.setSelectedItem( RowSubset.ALL );
        }
        subsetSelector_.setModel( subselModel );
    }

    /**
     * Returns a column selector that will only allow selection of
     * columns suitable for use as spectra.  That means ones with
     * non-tiny numeric array values.
     *
     * @param   tcModel   topcat model
     * @return  spectrum column selector model for tcModel
     */
    private static ColumnDataComboBoxModel
            createSpectrumColumnSelector( TopcatModel tcModel ) {
        ColumnDataComboBoxModel.Filter vectorFilter =
                new ColumnDataComboBoxModel.Filter() {
            public boolean acceptColumn( ValueInfo info ) {
                Class clazz = info.getContentClass();
                int[] shape = info.isArray() ? info.getShape() : null;
                boolean isNumericArray =
                       double[].class.isAssignableFrom( clazz )
                    || float[].class.isAssignableFrom( clazz )
                    || long[].class.isAssignableFrom( clazz )
                    || int[].class.isAssignableFrom( clazz )
                    || short[].class.isAssignableFrom( clazz );
                boolean is1d = shape != null && shape.length == 1;
                boolean isVerySmall = is1d && shape[ 0 ] > 0 && shape[ 0 ] < 4;
                return isNumericArray && is1d && ! isVerySmall;
            }
        };
        return new ColumnDataComboBoxModel( tcModel, vectorFilter,
                                            true, false );
    }
}
