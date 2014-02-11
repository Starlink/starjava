package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Plot layer control which manages coordinates and subsets in a common way
 * for multiple layers defined by one or more forms.
 * It provides a tab for common coordinates (including table)
 * and a tab for configuring subset-specific defaults.
 * Concrete subclasses must provide their form panels.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
public abstract class FormLayerControl
        extends TabberControl implements LayerControl {

    private final PositionCoordPanel posCoordPanel_;
    private final boolean autoPopulate_;
    private final TablesListComboBox tableSelector_;
    private final WrapperListModel subListModel_;
    private final SubsetConfigManager subsetManager_;
    private final TopcatListener tcListener_;
    private final SubsetStack subStack_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  posCoordPanel  panel for entering table and basic positional
     *                        coordinates
     * @param  autoPopulate  if true, when the table is changed an attempt
     *                       will be made to initialise the coordinate fields
     *                       with some suitable values
     * @param  nextSupplier  manages global dispensing for some style options
     * @param  tcListener  listener for TopcatEvents; this manager will arrange
     *                     for it to listen to whatever is the currently
     *                     selected TopcatModel
     * @param  controlIcon  icon for control stack
     */
    protected FormLayerControl( PositionCoordPanel posCoordPanel,
                                boolean autoPopulate,
                                NextSupplier nextSupplier,
                                TopcatListener tcListener, Icon controlIcon ) {
        super( null, controlIcon );
        posCoordPanel_ = posCoordPanel;
        autoPopulate_ = autoPopulate;
        final TopcatListener externalTcListener = tcListener;

        /* Set up a selector for which table to plot. */
        tableSelector_ = new TablesListComboBox();

        /* Ensure listeners are notified when the table selection changes,
         * or when anything else about this control changes. */
        final ActionListener forwarder = getActionForwarder();
        posCoordPanel_.addActionListener( forwarder );
        tableSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                tableChanged();
                forwarder.actionPerformed( new ActionEvent( this, 0,
                                                            "Table" ) );
            }
        } );

        /* Set up the panel for selecting the table and position coordinates. */
        JComponent posbox = Box.createVerticalBox();
        posbox.add( new LineBox( "Table", new ShrinkWrapper( tableSelector_ ),
                                 true ) );
        posbox.add( posCoordPanel_.getComponent() );

        /* Set up a manager for per-subset configuration. */
        subsetManager_ =
            new SubsetConfigManager( nextSupplier,
                                     new ConfigKey[] { StyleKeys.LABEL,
                                                       StyleKeys.SHOW_LABEL } );
        subsetManager_.addActionListener( forwarder );

        /* By using a wrapper list here and resetting its base model when
         * the table changes, other components in this control can just
         * listen for changes to the wrapper model, they don't have to be
         * messaged explicitly when the underlying model changes. */
        subListModel_ = new WrapperListModel();
        subStack_ = new SubsetStack( subListModel_, subsetManager_ );
        subStack_.addActionListener( forwarder );

        /* Required to make the controlLabel display sensitive to changes in
         * the topcat model label. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                handleTopcatEvent( evt );
                externalTcListener.modelChanged( evt );
            }
        };

        /* Position the components in tabs of this control. */
        addControlTab( "Position", posbox, true );
        addControlTab( "Subsets", subStack_.getComponent(), false );
    }

    @Override
    public String getControlLabel() {
        return tcModel_ == null ? "<no table>" : tcModel_.toString();
    }

    /**
     * Returns the panel in which positional coordinates are entered.
     *
     * @return  positional coordinate panel
     */
    public PositionCoordPanel getPositionCoordPanel() {
        return posCoordPanel_;
    }

    /**
     * Returns this control's per-subset configuration manager.
     *
     * @return   subset manager
     */
    public SubsetConfigManager getSubsetManager() {
        return subsetManager_;
    }

    /**
     * Returns this control's selectable stack of subsets.
     *
     * @return  subset stack
     */
    public SubsetStack getSubsetStack() {
        return subStack_;
    }

    /**
     * Returns the controls in the form control list which are contributing
     * to the plot.  Controls that the user has deactivated (unchecked)
     * are ignored.
     *
     * @return  list of active form controls
     */
    protected abstract FormControl[] getActiveFormControls();

    public PlotLayer[] getPlotLayers() {
        RowSubset[] subsets = subStack_.getSelectedSubsets();
        GuiCoordContent[] posContents = posCoordPanel_.getContents();
        if ( tcModel_ == null || posContents == null || subsets == null ) {
            return new PlotLayer[ 0 ];
        }
        DataGeom geom = posCoordPanel_.getDataGeom();
        FormControl[] fcs = getActiveFormControls();
        List<PlotLayer> layerList = new ArrayList<PlotLayer>();
        for ( int is = 0; is < subsets.length; is++ ) {
            RowSubset subset = subsets[ is ];
            for ( int ifc = 0; ifc < fcs.length; ifc++ ) {
                FormControl fc = fcs[ ifc ];
                GuiCoordContent[] extraContents = fc.getExtraCoordContents();
                if ( extraContents != null ) {
                    GuiCoordContent[] contents =
                        PlotUtil.arrayConcat( posContents, extraContents );
                    DataSpec dspec =
                        new GuiDataSpec( tcModel_, subset, contents );
                    layerList.add( fc.createLayer( geom, dspec, subset ) );
                }
            }
        }
        return layerList.toArray( new PlotLayer[ 0 ] );
    }

    public LegendEntry[] getLegendEntries() {
        if ( tcModel_ == null ) {
            return new LegendEntry[ 0 ];
        }
        Map<RowSubset,List<Style>> rsetStyles =
            getStylesBySubset( getPlotLayers() );
        List<LegendEntry> entries = new ArrayList<LegendEntry>();
        for ( RowSubset rset : rsetStyles.keySet() ) {
            Style[] styles = rsetStyles.get( rset ).toArray( new Style[ 0 ] );
            ConfigMap config =
                subsetManager_.getConfigger( rset ).getConfig();
            boolean show = config.get( StyleKeys.SHOW_LABEL );
            if ( show ) {
                String label = config.get( StyleKeys.LABEL );
                if ( label == null || label.trim().length() == 0 ) {
                    label = tcModel_.getID() + ": " + rset.getName();
                }
                LegendEntry entry = new LegendEntry( label, styles );
                assert entry.equals( new LegendEntry( label, styles ) );
                entries.add( entry );
            }
        }
        return entries.toArray( new LegendEntry[ 0 ] );
    }

    /**
     * Invoked when an event is received for the currently selected
     * TopcatModel.
     */
    private void handleTopcatEvent( TopcatEvent evt ) {
        int code = evt.getCode();

        /* Label change event: forward to the plot listener so it can
         * replot with a new legend label etc. */
        if ( code == TopcatEvent.LABEL ) {
            getActionForwarder()
           .actionPerformed( new ActionEvent( this, 0, "label" ) );
        }

        /* Subset show event: ensure that the subset is selected in this plot
         * and forward to the plot listener so it can replot if required. */
        else if ( code == TopcatEvent.SHOW_SUBSET ) {
            RowSubset rset = (RowSubset) evt.getDatum();
            subStack_.setSelected( rset, true );
            getActionForwarder()
           .actionPerformed( new ActionEvent( this, 0, "subset" ) );
        }
    }

    /**
     * Returns all those style objects associated with a given RowSubset
     * for a given list of plot layers.
     *
     * @param    layers  plot layers
     * @return   ordered RowSubset->Styles map
     */
    private static Map<RowSubset,List<Style>>
                   getStylesBySubset( PlotLayer[] layers ) {
        Map<RowSubset,List<Style>> map =
            new LinkedHashMap<RowSubset,List<Style>>();
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            DataSpec dspec = layer.getDataSpec();
            if ( dspec != null ) {
                Object mask = dspec.getMaskId();
                assert mask instanceof RowSubset;
                if ( mask instanceof RowSubset ) {
                    RowSubset rset = (RowSubset) mask;
                    if ( ! map.containsKey( rset ) ) {
                        map.put( rset, new ArrayList<Style>() );
                    }
                    map.get( rset ).add( layer.getStyle() );
                }
            }
        }
        return map;
    }

    public String getCoordLabel( String userCoordName ) {
        String label = GuiCoordContent
                      .getCoordLabel( userCoordName,
                                      posCoordPanel_.getContents() );
        FormControl[] fcs = getActiveFormControls();
        for ( int ifc = 0; ifc < fcs.length && label == null; ifc++ ) {
            label = GuiCoordContent
                   .getCoordLabel( userCoordName,
                                   fcs[ ifc ].getExtraCoordContents() );
        }
        return label;
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
     * Returns the table for which this control is currently making plots.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        return (TopcatModel) tableSelector_.getSelectedItem();
    }

    /**
     * Called when the TopcatModel for which this control is generating plots
     * is changed.  Usually this will be because the user has selected
     * a new one from the table selector.
     */
    private void tableChanged() {

        /* Reassign the listener to listen to the new current model
         * not the old one. */
        if ( tcModel_ != null ) {
            tcModel_.removeTopcatListener( tcListener_ );
        }
        tcModel_ = (TopcatModel) tableSelector_.getSelectedItem();
        if ( tcModel_ != null ) {
            tcModel_.addTopcatListener( tcListener_ );
        }

        /* Message the position selection panel and form controls so they
         * can update their lists of selectable columns etc. */
        posCoordPanel_.setTable( tcModel_, autoPopulate_ );
        FormControl[] fcs = getActiveFormControls();
        for ( int ifc = 0; ifc < fcs.length; ifc++ ) {
            fcs[ ifc ].setTable( tcModel_, subsetManager_ );
        }

        /* Set up a sensible selection of at least one visible subset. */
        subStack_.setSelectedSubsets( new RowSubset[ 0 ] );
        if ( tcModel_ == null ) {
            subListModel_.setBaseModel( new DefaultListModel() );
        }
        else {
            subListModel_.setBaseModel( tcModel_.getSubsets() );
            RowSubset rset = tcModel_.getSelectedSubset();
            subStack_.setSelectedSubsets( new RowSubset[] { rset } );
        }
    }

    /**
     * List model which serves as a wrapper for some base list model,
     * but the base model can be changed.  If that happens, an appropriate
     * ContentsChanged event is sent to listeners of this model.
     * This allows external listeners just to listen to this model,
     * and not to have to worry about keeping changing the target of their
     * listeners when state of the layer control changes.
     */
    private static class WrapperListModel implements ListModel {
        private final List<ListDataListener> listenerList_;
        private final ListDataListener listDataForwarder_;
        private ListModel baseModel_;

        /**
         * Constructor.
         */
        WrapperListModel() {
            listenerList_ = new ArrayList<ListDataListener>();
            listDataForwarder_ = new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    for ( ListDataListener l : listenerList_ ) {
                        l.contentsChanged( evt );
                    }
                }
                public void intervalAdded( ListDataEvent evt ) {
                    for ( ListDataListener l : listenerList_ ) {
                        l.intervalAdded( evt );
                    }
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    for ( ListDataListener l : listenerList_ ) {
                        l.intervalRemoved( evt );
                    }
                }
            };
        }

        /**
         * Sets the model whose data will back this model.
         *
         * @param  baseModel  base list model
         */
        public void setBaseModel( ListModel baseModel ) {
            int imax = 0;
            if ( baseModel_ != null ) {
                baseModel_.removeListDataListener( listDataForwarder_ );
                imax = Math.max( imax, baseModel_.getSize() );
            }
            baseModel_ = baseModel;
            if ( baseModel_ != null ) {
                baseModel_.addListDataListener( listDataForwarder_ );
                imax = Math.max( imax, baseModel_.getSize() );
            }
            ListDataEvent evt =
                new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                   0, imax );
            for ( ListDataListener l : listenerList_ ) {
                l.contentsChanged( evt );
            }
        } 

        public Object getElementAt( int index ) {
            return baseModel_ == null ? null : baseModel_.getElementAt( index );
        }

        public int getSize() {
            return baseModel_ == null ? 0 : baseModel_.getSize();
        }

        public void addListDataListener( ListDataListener listener ) {
            listenerList_.add( listener );
        }

        public void removeListDataListener( ListDataListener listener ) {
            listenerList_.remove( listener );
        }
    }
}
