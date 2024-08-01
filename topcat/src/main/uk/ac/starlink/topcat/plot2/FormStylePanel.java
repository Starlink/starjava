package uk.ac.starlink.topcat.plot2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.IconUtils;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * GUI component for acquiring style information for a plot for
 * each row subset of a particular table.
 * One part of the panel allows selection of global (per-table) style
 * configuration, and another part allows selection of subset-specific
 * overrides.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class FormStylePanel extends JPanel {

    private final Configger plotConfigger_;
    private final Supplier<Plotter<?>> plotterSupplier_;
    private final SubsetConfigManager subManager_;
    private final SubsetStack subStack_;
    private final TopcatModel tcModel_;
    private final SpecialDefault<?>[] specialDflts_;
    private final OptionalConfigSpecifier globalSpecifier_;
    private final ActionForwarder forwarder_;
    private final Map<RowSubset.Key,ConfigMap> subsetConfigs_;
    private final JLabel iconLabel_;
    private final JComponent subsetSpecifierHolder_;
    private final JComboBox<RowSubset> subsetSelector_;
    private final ConfigSpecifier subsetSpecifier_;

    /**
     * Constructor.
     *
     * @param  keys  style configuration keys that this panel is to acquire
     * @param  plotConfigger   global config defaults
     * @param  plotterSupplier   obtains on demand the plotter for which this
     *                           panel is acquiring style information
     * @param  subManager   provides per-subset defaults for some config keys
     * @param  subStack    controls/display per-subset visibility
     * @param  tcModel   topcat model whose subsets are being configured
     */
    @SuppressWarnings("this-escape")
    public FormStylePanel( ConfigKey<?>[] keys, Configger plotConfigger,
                           Supplier<Plotter<?>> plotterSupplier,
                           SubsetConfigManager subManager,
                           SubsetStack subStack, TopcatModel tcModel ) {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        plotConfigger_ = plotConfigger;
        plotterSupplier_ = plotterSupplier;
        subManager_ = subManager;
        subStack_ = subStack;
        tcModel_ = tcModel;
        specialDflts_ = new SpecialDefault<?>[] { SpecialDefault.SIZE };
        forwarder_ = new ActionForwarder();

        /* Set up specifier for global keys. */
        globalSpecifier_ =
            new OptionalConfigSpecifier( keys, subManager.getConfigKeys(),
                                         "By Subset" );

        /* Ensure that any change to the global key specifier results in
         * an immediate change to the current state of the per-subset
         * configs. */
        globalSpecifier_
           .addActionListener( new GlobalChangeListener( globalSpecifier_ ) );

        /* Ensure that any change to the subset manager specifiers results in
         * an immediate change to the current state of the appropriate
         * per-subset configs. */
        subManager.addActionListener( new SubChangeListener( subManager ) );

        /* Place global specifier component. */
        JComponent globalComp = globalSpecifier_.getComponent();
        globalComp.setBorder( AuxWindow.makeTitledBorder( "Global Style" ) );
        add( globalComp );

        /* Place subset specifier components. */
        iconLabel_ = new JLabel();
        iconLabel_.setBorder( BorderFactory
                             .createBevelBorder( BevelBorder.RAISED ) );
        Dimension iconSize = new Dimension( 24, 24 );
        iconLabel_.setPreferredSize( iconSize );
        iconLabel_.setMinimumSize( iconSize );
        iconLabel_.setMaximumSize( iconSize );
        subsetSpecifierHolder_ = Box.createVerticalBox();

        /* Set up a selector which will allow access to per-subset configs. */
        subsetSelector_ =
            new JComboBox<RowSubset>(
                new Plus1ListModel<RowSubset>( tcModel.getSubsets(), null ) );
        subsetSelector_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                RowSubset.Key rsKey = getSelectedSubsetKey();
                if ( rsKey != null ) {
                    restoreConfig( rsKey );
                }
                updateLegendIcon();
                setSubsetSpecifierActive( rsKey != null );
            }
        } );

        /* Set up a per-subset specifier which can be configured for
         * different subsets. */
        subsetSpecifier_ = new ConfigSpecifier( keys );
        subsetSpecifier_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                RowSubset.Key rsKey = getSelectedSubsetKey();
                if ( rsKey != null ) {
                    saveConfig( rsKey );
                }
                updateLegendIcon();
                forwarder_.actionPerformed( evt );
            }
        } );
        subsetConfigs_ = new HashMap<RowSubset.Key,ConfigMap>();
        subsetSelector_.setSelectedItem( null );

        /* Apply table-sensitive default values. */
        applySpecialDefaults();

        /* Set up a checkbox to display/control visibility of the selected
         * subset.  This can be controlled elsewhere (the SubsetStack in
         * the Subsets tab), but it's useful to have a reminder here.
         * One particular circumstance is if you're changing something
         * here and can't see any changes in the plot. */
        JCheckBox subsetVisibilityBox =
            createSelectedSubsetVisibilityBox( "Visible" );

        /* Place components. */
        JComponent subsetPanel = Box.createVerticalBox();
        JComponent subsetLine = Box.createHorizontalBox();
        subsetLine.add( new JLabel( "Subset: " ) );
        subsetLine.add( new ShrinkWrapper( subsetSelector_ ) );
        subsetLine.add( Box.createHorizontalStrut( 10 ) );
        subsetLine.add( iconLabel_ );
        subsetLine.add( Box.createHorizontalStrut( 10 ) );
        subsetLine.add( subsetVisibilityBox );
        subsetLine.add( Box.createHorizontalGlue() );
        subsetPanel.add( subsetLine );
        subsetPanel.add( Box.createVerticalStrut( 5 ) );
        subsetPanel.add( subsetSpecifierHolder_ );
        subsetPanel.setBorder( AuxWindow.makeTitledBorder( "Subset Styles" ) );
        add( subsetPanel );
    }

    /**
     * Adds a listener which will be notified when there is a change to
     * any of this panel's configuration.
     *
     * @param listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param   listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    /**
     * Returns the configuration for one of this panel's row subsets.
     * This is a combination of global and per-subset selected items.
     *
     * @param   rsKey  row subset identifier
     * @return   style configuration
     */
    public ConfigMap getConfig( RowSubset.Key rsKey ) {
        ConfigMap config = getDefaultSubsetConfig( rsKey );
        ConfigMap subMap = subsetConfigs_.get( rsKey );
        if ( subMap != null ) {
            config.putAll( subMap );
        }
        return config;
    }

    /**
     * Sets global (not subset-specific) configuration options for this panel.
     * Any supplied config options not used by this style panel are ignored.
     *
     * @param  config  configuration map containing zero or more entries
     *                 for this panel's options
     */
    public void setGlobalConfig( ConfigMap config ) {
        globalSpecifier_.setSpecifiedValue( config );
    }

    /**
     * Configures this panel with the current state of a supplied template.
     *
     * @param template  panel supplying required configuration
     */
    public void configureFrom( FormStylePanel template ) {

        /* Copy global configuration. */
        globalSpecifier_.configureFrom( template.globalSpecifier_ );

        /* Copying subset configurations is more complicated, since they
         * are keyed by RowSubset objects, which in general are unique
         * within a given table.  Copy subset configurations if they
         * match by subset name. */
        Map<String,ConfigMap> rsNameConfigs = new HashMap<String,ConfigMap>();
        for ( RowSubset.Key rsKey : template.subsetConfigs_.keySet() ) {
            RowSubset rset = template.tcModel_.getSubsetByKey( rsKey );
            if ( rset != null ) {
                rsNameConfigs.put( rset.getName(),
                                   template.subsetConfigs_.get( rsKey ) );
            }
        }
        for ( RowSubset rset : tcModel_.getSubsets() ) {
            ConfigMap config = rsNameConfigs.get( rset.getName() );
            if ( config != null ) {
                subsetConfigs_.put( rset.getKey(), config );
            }
        }

        /* Overwrite options that have table-specific defaults
         * as appropriate. */
        applySpecialDefaults();
    }

    /**
     * Accepts plot reports indexed by subset, and passes them on
     * to the relevant specifiers.
     *
     * @param   reports   map of subset-&gt;plot report maps for the plot layers
     *                    generated by this panel
     */
    public void submitReports( Map<RowSubset,ReportMap> reports ) {
        if ( reports.size() == 0 ) {
            return;
        }

        /* Identify report entries which are common to all subsets.
         * These can get passed to the global config panel. */
        ReportMap report1 = reports.values().iterator().next();
        ReportMap commonReport = new ReportMap( report1 );
        for ( ReportMap report : reports.values() ) {
            for ( Iterator<ReportKey<?>> keyIt =
                      commonReport.keySet().iterator();
                  keyIt.hasNext(); ) {
                ReportKey<?> key = keyIt.next();
                if ( ! PlotUtil.equals( commonReport.get( key ),
                                        report.get( key ) ) ) {
                    keyIt.remove();
                }
            }
        }
        globalSpecifier_.submitReport( commonReport );

        /* Any report entries which are specific to the currently selected
         * subset can get passed to the subset-specific specifier. */
        ReportMap rmap = reports.get( getSelectedSubset() );
        subsetSpecifier_.submitReport( rmap == null ? new ReportMap() : rmap );
    }

    /**
     * Stores the current state of the per-subset specifier component
     * as the value for a given subset.
     *
     * @param  rsKey  row subset identifier
     */
    private void saveConfig( RowSubset.Key rsKey ) {
        subsetConfigs_.put( rsKey, subsetSpecifier_.getSpecifiedValue() );
    }

    /**
     * Sets the state of the per-subset specifier component to the value
     * stored for a given subset.  A default configuration is created lazily
     * if no value has previously been stored for the subset.
     *
     * @param  rsKey  row subset identifier
     */
    private void restoreConfig( RowSubset.Key rsKey ) {

        /* Lazily create an entry if no config has explicitly been saved
         * for subset. */
        if ( ! subsetConfigs_.containsKey( rsKey ) ) {
            ConfigMap config = getDefaultSubsetConfig( rsKey );
            config.keySet().retainAll( Arrays.asList( subsetSpecifier_
                                                     .getConfigKeys() ) );
            subsetConfigs_.put( rsKey, config );
        }

        /* Retrieve config value for subset and restore the GUI from it. */
        subsetSpecifier_.setSpecifiedValue( subsetConfigs_.get( rsKey ) );
    }

    /**
     * Returns the default config values for a given subset.
     * This is not affected by user actions in this component.
     *
     * @param  rsKey  row subset identifier
     * @return   default config
     */
    private ConfigMap getDefaultSubsetConfig( RowSubset.Key rsKey ) {
        ConfigMap config = plotConfigger_.getConfig();
        config.putAll( subManager_.getConfigger( rsKey ).getConfig() );
        config.putAll( globalSpecifier_.getSpecifiedValue() );
        return config;
    }

    /**
     * Configures the per-subset specifier to be capable of user interaction
     * or not.
     * Currently, the components are removed from the GUI when inactive.
     * 
     * @param  isActive  whether per-subset specifier will be usable
     */
    private void setSubsetSpecifierActive( boolean isActive ) {
        subsetSpecifierHolder_.removeAll();
        if ( isActive ) {
            subsetSpecifierHolder_.add( subsetSpecifier_.getComponent() );
        }
        subsetSpecifierHolder_.revalidate();
    }

    /**
     * Returns the subset currently selected for subset-specific configuration,
     * or null if none is selected.
     *
     * @return  selected subset, may be null
     */
    private RowSubset getSelectedSubset() {
        return subsetSelector_.getItemAt( subsetSelector_.getSelectedIndex() );
    }

    /**
     * Returns the identifier for the subset selected for subset-specific
     * configuration, or null if none is selected.
     *
     * @return  selected subset key, may be null
     */
    private RowSubset.Key getSelectedSubsetKey() {
        RowSubset rset = getSelectedSubset();
        return rset == null ? null : rset.getKey();
    }

    /**
     * There is space for a little icon near the per-subset specifier.
     * This updates it to make sure it shows the right thing, which
     * will change according to which subset is being configured and
     * the state of its configuration.
     */
    private void updateLegendIcon() {
        Style style;
        if ( getSelectedSubset() != null ) {
            ConfigMap config = subsetSpecifier_.getSpecifiedValue();
            try {
                style = plotterSupplier_.get().createStyle( config );
            }
            catch ( ConfigException e ) {
                style = null;
            }
        }
        else {
            style = null;
        }
        Icon icon = style == null ? IconUtils.emptyIcon( 24, 24 )
                                  : style.getLegendIcon();
        iconLabel_.setIcon( icon );
    }

    /**
     * Returns a checkbox that displays/controls the visibility state
     * of whatever is this panel's currently selected subset.
     *
     * @param  boxName   name to label the checkbox
     * @return  checkbox
     */
    private JCheckBox createSelectedSubsetVisibilityBox( String boxName ) {

        /* Create the checkbox. */
        final JCheckBox visBox = new JCheckBox( boxName );
        visBox.setToolTipText( "Reports/sets whether the selected subset "
                             + "is currently plotted" );

        /* Make sure the checkbox state is upadated on changes to
         * either the inclusion status of any of this panel's subsets,
         * or the identity of this panel's currently selected subset. */
        final ActionListener viewListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                RowSubset rset = getSelectedSubset();
                boolean isVisible =
                    Arrays.asList( subStack_.getSelectedSubsets() )
                          .contains( rset );
                visBox.setSelected( isVisible );
                visBox.setEnabled( rset != null );
            }
        };
        subStack_.addActionListener( viewListener );
        subsetSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                viewListener.actionPerformed( null );
            }
        } );

        /* If the checkbox is toggled, inform the subset inclusion model. */
        visBox.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                RowSubset rset = getSelectedSubset();
                if ( rset != null ) {
                    subStack_.setSelected( rset, visBox.isSelected() );
                }
            }
        } );

        /* Return the checkbox. */
        return visBox;
    }

    /**
     * Customises this panel's specifiers according to table-sensitive
     * defaulting policies.
     */
    private void applySpecialDefaults() {
        if ( tcModel_ != null ) {
            for ( SpecialDefault<?> special : specialDflts_ ) {
                customiseDefault( globalSpecifier_, special, tcModel_ );
                customiseDefault( subsetSpecifier_, special, tcModel_ );
            }
        }
    }

    /**
     * Customises a ConfigSpecifier according to a supplied table-sensitive
     * default policy.
     *
     * @param  configSpecifier   specifier that may contain a relevant
     *                           config item
     * @param  special  object that applies table-sensitive default values
     * @param  tcModel   table
     */
    private static <T> void customiseDefault( ConfigSpecifier configSpecifier,
                                              SpecialDefault<T> special,
                                              TopcatModel tcModel ) {
        Specifier<T> itemSpecifier =
            configSpecifier.getSpecifier( special.getKey() );
        if ( itemSpecifier != null ) {
            T dflt = special.getDefaultValue( tcModel );
            if ( dflt != null ) {
                itemSpecifier.setSpecifiedValue( dflt );
            }
        }
    }

    /**
     * Listens to changes in the global style config panel.
     * Any changes made explicitly to it are written in to the per-subset
     * config records, thus overwriting them.
     */
    private class GlobalChangeListener implements ActionListener {
        private final ConfigSpecifier configSpecifier_;
        private ConfigMap lastConfig_;
 
        /**
         * Constructor.
         *
         * @param  configSpecifier   specifier we are listening to
         */
        GlobalChangeListener( ConfigSpecifier configSpecifier ) {
            configSpecifier_ = configSpecifier;
            lastConfig_ = configSpecifier_.getSpecifiedValue();
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Find out what config items have just changed. */
            ConfigMap config = configSpecifier_.getSpecifiedValue();
            Set<ConfigKey<?>> changeSet = new HashSet<ConfigKey<?>>();
            for ( ConfigKey<?> key : config.keySet() ) {
                if ( ! PlotUtil.equals( config.get( key ),
                                        lastConfig_.get( key ) ) ) {
                    changeSet.add( key );
                }
            }
            lastConfig_ = config;

            /* Where applicable write those into the stored per-subset
             * records. */
            if ( ! changeSet.isEmpty() ) {
                ConfigMap changeMap = new ConfigMap( config );
                changeMap.keySet().retainAll( changeSet );
                for ( RowSubset.Key rsKey : subsetConfigs_.keySet() ) {
                    ConfigMap savedConfig = subsetConfigs_.get( rsKey );
                    savedConfig.putAll( changeMap );
                    if ( rsKey.equals( getSelectedSubsetKey() ) ) {
                        subsetSpecifier_.setSpecifiedValue( savedConfig );
                    }
                }
            }

            /* Notify listeners. */
            forwarder_.actionPerformed( evt );
        }
    }

    /**
     * Listens for changes in the subset manager configuration.
     * Any relevant changes made explicitly to it are written in to
     * the per-subset config records, thus overwriting them.
     */
    private class SubChangeListener implements ActionListener {
        private final SubsetConfigManager subManager_;
        private Map<RowSubset.Key,ConfigMap> lastConfigs_;

        /**
         * Constructor.
         *
         * @param  subManager   subset config manager we are listening to
         */
        SubChangeListener( SubsetConfigManager subManager ) {
            subManager_ = subManager;
            lastConfigs_ = new HashMap<RowSubset.Key,ConfigMap>();
        }

        public void actionPerformed( ActionEvent evt ) {
            boolean changed = false;

            /* Iterate over known subsets. */
            for ( RowSubset.Key rsKey : subsetConfigs_.keySet() ) {

                /* Work out what config items have changed for the current
                 * row subset since last time. */
                ConfigMap lastConfig = lastConfigs_.get( rsKey );
                if ( lastConfig == null ) {
                    lastConfig = new ConfigMap();
                }
                ConfigMap config =
                    subManager_.getConfigger( rsKey ).getConfig();
                Set<ConfigKey<?>> changeSet = new HashSet<ConfigKey<?>>();
                for ( ConfigKey<?> key : config.keySet() ) {
                    if ( ! PlotUtil.equals( config.get( key ),
                                            lastConfig.get( key ) ) ) {
                        changeSet.add( key );
                    }
                }
                lastConfigs_.put( rsKey, config );

                /* Where applicable write those changes into the stored
                 * per-subset records. */
                if ( ! changeSet.isEmpty() ) {
                    ConfigMap changeMap = new ConfigMap( config );
                    changeMap.keySet().retainAll( changeSet );
                    ConfigMap savedConfig = subsetConfigs_.get( rsKey );
                    savedConfig.putAll( changeMap );
                    if ( rsKey.equals( getSelectedSubsetKey() ) ) {
                        subsetSpecifier_.setSpecifiedValue( savedConfig );
                    }
                }
                changed = true;
            }

            /* Notify listeners if anything actually happened. */
            if ( changed ) {
                forwarder_.actionPerformed( evt );
            }
        }
    }

    /**
     * Wrapper ComboBoxModel which just adds an entry before all the
     * existing ones.  It is backed by a live base model, and any changes
     * to that will be reflected here.
     */
    private static class Plus1ListModel<T> extends AbstractListModel<T>
                                           implements ComboBoxModel<T> {
        private final ListModel<T> baseModel_;
        private final T item0_;
        private Object selectedItem_;

        /**
         * Constructor.
         *
         * @param  baseModel   base list model
         * @param  item0  entry to insert first in the list
         */
        Plus1ListModel( ListModel<T> baseModel, T item0 ) {
            baseModel_ = baseModel;
            item0_ = item0;
            baseModel.addListDataListener( new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    fireContentsChanged( evt.getSource(),
                                         evt.getIndex0(), evt.getIndex1() );
                }
                public void intervalAdded( ListDataEvent evt ) {
                    fireIntervalAdded( evt.getSource(),
                                       evt.getIndex0(), evt.getIndex1() );
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    fireIntervalRemoved( evt.getSource(),
                                         evt.getIndex0(), evt.getIndex1() );
                }
            } );
        }

        public int getSize() {
            return baseModel_.getSize() + 1;
        }

        public T getElementAt( int ix ) {
            return ix == 0 ? item0_ : baseModel_.getElementAt( ix - 1 );
        }

        public void setSelectedItem( Object item ) {
            selectedItem_ = item;
        }

        public Object getSelectedItem() {
            return selectedItem_;
        }
    }
}
