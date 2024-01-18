package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ComboBoxSpecifier;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.ModePlotter;

/**
 * FormControl implementation for a collection of ModePlotters.
 *
 * <p>A Mode selector is presented at the top to determine the actual
 * plotter used.  The style config and non-positional coordinate
 * information required is then divided into two parts:
 * those keys which are common to all the modes,
 * and those keys which are specific to the currently selected mode.
 * The common ones share common specifier components so that the values
 * don't change if the mode changes, while the specific ones appear
 * in a mode-specific location (near the mode selector) only for the
 * mode they apply to.
 * From a programmatic point of view, it can be treated just like a normal
 * FormControl.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class ModeFormControl extends FormControl {

    private final List<Coord> excludeCoords_;
    private final Map<ModePlotter.Mode,ModeState> modeMap_;
    private final JComponent panel_;
    private final Specifier<ModePlotter.Mode> modeSpecifier_;
    private final JComponent modeCoordHolder_;
    private final JComponent modeConfigHolder_;
    private final CoordPanel commonExtraCoordPanel_;
    private final String formLabel_;
    private final ConfigKey<?>[] nonModeKeys_;
    private ModeState state_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  baseConfigger  provides global configuration info
     * @param  plotters  family of plotters with different modes but
     *                   the same form,
     *                   for which this control gathers configuration
     * @param  subsetKeys  config keys which are managed on a per-subset
     *                     basis by some other component
     * @param  excludeCoords  coordinates that may belong to the plotters but
     *                        are taken care of elsewhere, and so should not
     *                        be presented in this control
     */
    public ModeFormControl( Configger baseConfigger, ModePlotter<?>[] plotters,
                            ConfigKey<?>[] subsetKeys, Coord[] excludeCoords ) {
        super( baseConfigger );
        excludeCoords_ = Arrays.asList( excludeCoords );
        final ActionListener forwarder = getActionForwarder();

        /* Work out non-positional coordinates common to all modes
         * and prepare a panel for entering them. */
        List<Coord> commonExtraCoordList = getCommonCoords( plotters );
        commonExtraCoordList.removeAll( excludeCoords_ );
        commonExtraCoordPanel_ = new BasicCoordPanel( commonExtraCoordList
                                                     .toArray( new Coord[0] ) );
        if ( ! commonExtraCoordList.isEmpty() ) {
            commonExtraCoordPanel_
           .getComponent()
           .setBorder( AuxWindow.makeTitledBorder( "Coordinates" ) );
        }
        commonExtraCoordPanel_.addActionListener( forwarder );

        List<ConfigKey<?>> commonKeyList = getCommonKeys( plotters );
        ModePlotter.Form commonForm = plotters[ 0 ].getForm();
        modeMap_ = new LinkedHashMap<ModePlotter.Mode,ModeState>();
        for ( int ip = 0; ip < plotters.length; ip++ ) {
            ModePlotter<?> plotter = plotters[ ip ];
            assert plotter.getForm().equals( commonForm );

            /* Get all coords specific to this mode. */
            Collection<Coord> coordList =
                new ArrayList<Coord>( Arrays.asList( plotter.getCoordGroup()
                                                    .getExtraCoords() ) );
            coordList.removeAll( commonExtraCoordList );
            coordList.removeAll( excludeCoords_ );
            Coord[] modeCoords = coordList.toArray( new Coord[ 0 ] );

            /* Get all config keys specific to this mode. */
            Collection<ConfigKey<?>> keyList =
                new ArrayList<ConfigKey<?>>( Arrays.asList( plotter
                                                           .getStyleKeys() ) );
            keyList.removeAll( commonKeyList );
            keyList.removeAll( baseConfigger.getConfig().keySet() );
            keyList.removeAll( Arrays.asList( subsetKeys ) );
            ConfigKey<?>[] modeConfigKeys =
                keyList.toArray( new ConfigKey<?>[ 0 ] );

            /* Prepare and store a ModeState for this mode. */
            ModePlotter.Mode mode = plotter.getMode();
            ModeState state =
                new ModeState( plotter, modeCoords, modeConfigKeys );
            state.modeCoordPanel_.addActionListener( forwarder );
            state.modeConfigSpecifier_.addActionListener( forwarder );
            modeMap_.put( mode, state );
        }

        /* Prepare a list of the non-mode-specific config keys. */
        List<ConfigKey<?>> otherKeyList =
            new ArrayList<ConfigKey<?>>( commonKeyList );
        otherKeyList.addAll( Arrays.asList( subsetKeys ) );
        nonModeKeys_ = otherKeyList.toArray( new ConfigKey<?>[ 0 ] );

        /* Work out the label for this control. */
        formLabel_ = commonForm == null ? null : commonForm.getFormName();

        /* Set up a selector for which mode to use, and get it to
         * modify the GUI appropriately when a mode is selected. */
        modeSpecifier_ = createPlotterModeSpecifier( modeMap_.keySet() );
        modeSpecifier_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateMode();
                forwarder.actionPerformed( evt );
            }
        } );

        /* Place components. */
        modeCoordHolder_ = Box.createHorizontalBox();
        modeConfigHolder_ = Box.createHorizontalBox();
        JComponent modePanel = Box.createVerticalBox();
        modePanel.add( new LineBox( "Mode", modeSpecifier_.getComponent(),
                                    true ) );
        modePanel.add( modeCoordHolder_ );
        modePanel.add( modeConfigHolder_ );
        modePanel.setBorder( AuxWindow.makeTitledBorder( "Shading" ) );
        panel_ = new JPanel( new BorderLayout() );
        panel_.add( modePanel, BorderLayout.NORTH );
        panel_.add( commonExtraCoordPanel_.getComponent(),
                    BorderLayout.CENTER );
        setMode( modeMap_.keySet().iterator().next() );
    }

    @Override
    public String getControlLabel() {
        return formLabel_ == null ? super.getControlLabel() : formLabel_;
    }

    @Override
    public GuiCoordContent[] getExtraCoordContents() {
        List<GuiCoordContent> contents = new ArrayList<GuiCoordContent>();
        GuiCoordContent[] coordExtras = commonExtraCoordPanel_.getContents();
        if ( coordExtras == null ) {
            return null;
        }
        contents.addAll( Arrays.asList( coordExtras ) );
        if ( state_ != null && state_.modeCoordPanel_ != null ) {
            GuiCoordContent[] modeExtras = state_.modeCoordPanel_.getContents();
            if ( modeExtras == null ) {
                return null;
            }
            contents.addAll( Arrays.asList( modeExtras ) );
        }
        return contents.toArray( new GuiCoordContent[ 0 ] );
    }

    @Override
    public ConfigMap getExtraConfig() {
        if ( state_ != null && state_.modeConfigSpecifier_ != null ) {
            return state_.modeConfigSpecifier_.getSpecifiedValue();
        }
        else {
            return new ConfigMap();
        }
    }

    protected void setTable( TopcatModel tcModel ) {
        commonExtraCoordPanel_.setTable( tcModel, false );
        for ( ModeState state : modeMap_.values() ) {
            state.modeCoordPanel_.setTable( tcModel, false );
        }
    }

    /**
     * Sets the current mode for this control.
     *
     * @param   mode  new mode
     */
    public void setMode( ModePlotter.Mode mode ) {
        modeSpecifier_.setSpecifiedValue( mode );
        updateMode();
    }

    protected Plotter<?> getPlotter() {
        return state_.plotter_;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return nonModeKeys_;
    }

    public JComponent getCoordPanel() {
        return panel_;
    }

    /**
     * Update the GUI for use with the currently selected mode.
     */
    private void updateMode() {
        ModePlotter.Mode mode = modeSpecifier_.getSpecifiedValue();
        ActionListener forwarder = getActionForwarder();

        /* Clear out existing mode, if any. */
        if ( state_ != null ) {
            state_.modeCoordPanel_.removeActionListener( forwarder );
            state_.modeConfigSpecifier_.removeActionListener( forwarder );
        }
        modeCoordHolder_.removeAll();
        modeConfigHolder_.removeAll();

        /* Store new mode. */
        state_ = modeMap_.get( mode );

        /* Add components for new mode. */
        if ( state_ != null ) {
            CoordPanel coordPanel = state_.modeCoordPanel_;
            coordPanel.addActionListener( forwarder );
            modeCoordHolder_.add( coordPanel.getComponent() );
            ConfigSpecifier configSpecifier = state_.modeConfigSpecifier_;
            configSpecifier.addActionListener( forwarder );
            modeConfigHolder_.add( configSpecifier.getComponent() );
        }
        modeCoordHolder_.repaint();
        modeConfigHolder_.repaint();
        modeCoordHolder_.revalidate();
        modeConfigHolder_.revalidate();
    }

    /**
     * Returns a list of Extra (non-positional) required plot coordinates
     * that are common to every one of a supplied list of plotters.
     *
     * @param  plotters  list of plotters
     * @return   common non-positional coordinates
     */
    private static List<Coord> getCommonCoords( Plotter<?>[] plotters ) {
        List<Coord> commonList = new ArrayList<Coord>();
        commonList.addAll( Arrays.asList( plotters[ 0 ].getCoordGroup()
                                                       .getExtraCoords() ) );
        for ( int ip = 1; ip < plotters.length; ip++ ) {
            Collection<Coord> coordSet =
                new HashSet<Coord>( Arrays.asList( plotters[ ip ]
                                                  .getCoordGroup()
                                                  .getExtraCoords() ) );
            for ( Iterator<Coord> it = commonList.iterator();
                  it.hasNext(); ) {
                Coord candidate = it.next();
                if ( ! coordSet.contains( candidate ) ) {
                    it.remove();
                }
            }
        }
        return commonList;
    }

    /**
     * Returns a list of style config keys that are common to every one
     * of a supplied list of plotters.
     *
     * @param  plotters  list of plotters
     * @return   common style keys
     */
    private static List<ConfigKey<?>> getCommonKeys( Plotter<?>[] plotters ) {
        List<ConfigKey<?>> commonList = new ArrayList<ConfigKey<?>>();
        commonList.addAll( Arrays.asList( plotters[ 0 ].getStyleKeys() ) );
        for ( int ip = 1; ip < plotters.length; ip++ ) {
            commonList.retainAll( Arrays
                                 .asList( plotters[ ip ].getStyleKeys() ) );
        }
        return commonList;
    }

    /**
     * Returns a specifier for selecting plotter modes.
     *
     * @param   modes   mode list
     * @return  new specifier
     */
    private static Specifier<ModePlotter.Mode>
            createPlotterModeSpecifier( Collection<ModePlotter.Mode> modes ) {
        JComboBox<ModePlotter.Mode> comboBox =
            new JComboBox<ModePlotter.Mode>(
                modes.toArray( new ModePlotter.Mode[ 0 ] ) );
        comboBox.setSelectedIndex( 0 );
        comboBox.setRenderer( new DefaultListCellRenderer() {
            public Component getListCellRendererComponent( JList<?> list,
                                                           Object value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean hasFocus ) {
                Component c =
                    super.getListCellRendererComponent( list, value, index,
                                                        isSelected, hasFocus );
                if ( c instanceof JLabel &&
                     value instanceof ModePlotter.Mode ) {
                    JLabel label = (JLabel) c;
                    ModePlotter.Mode mode = (ModePlotter.Mode) value;
                    label.setText( mode.getModeName() );
                    label.setIcon( mode.getModeIcon() );
                }
                return c;
            }
        } );
        return new ComboBoxSpecifier<ModePlotter.Mode>( ModePlotter.Mode.class,
                                                        comboBox );
    }

    /**
     * Stores state specific to a particular mode for this control.
     */
    private static class ModeState {
        final ModePlotter<?> plotter_;
        final CoordPanel modeCoordPanel_;
        final ConfigSpecifier modeConfigSpecifier_;

        /**
         * Constructor.
         *
         * @param   plotter  plotter
         * @param   modeCoords  mode-specific required plotting coordinates
         * @param   configKeys  mode-specific style config keys
         */
        ModeState( ModePlotter<?> plotter, Coord[] modeCoords,
                   ConfigKey<?>[] configKeys ) {
            plotter_ = plotter;
            modeCoordPanel_ = new BasicCoordPanel( modeCoords );
            if ( modeCoords.length > 0 ) {
                modeCoordPanel_.getComponent()
                               .setBorder( BorderFactory
                                          .createEmptyBorder( 0, 0, 5, 0 ) );
            }
            modeConfigSpecifier_ = new ConfigSpecifier( configKeys );
        }
    }
}
