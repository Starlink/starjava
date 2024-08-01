package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import uk.ac.starlink.topcat.Safety;

/**
 * ActivatorConfigurator implementation that allows the user to choose
 * between a number of supplied GUI options.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2022
 */
public class ChoiceConfigurator extends AbstractActivatorConfigurator {

    private final ActivatorConfigurator[] configurators_;
    private final ButtonGroup buttGrp_;
    private final ButtonModel[] buttModels_;
    private static final String ISUB_KEY = "isub";
    private static final int IDFLT = 0;
    static {
        assert new ConfigState().getInt( ISUB_KEY ) == IDFLT;
    }

    /**
     * Constructor.
     * A list of supplied configurator options is provided.
     * Note that in addition to the normal ActivatorConfigurator interface,
     * these instances ought to provide a human-readable implementation of
     * {@link Object#toString}, and the component returned by their
     * {@link ActivatorConfigurator#getPanel} method should give a
     * visual indication for its {JComponent#setEnabled enabled} property.
     * 
     * @param  configurators  configurator options
     */
    @SuppressWarnings("this-escape")
    public ChoiceConfigurator( ActivatorConfigurator[] configurators ) {
        super( new JPanel( new BorderLayout() ) );
        configurators_ = configurators;
        JComponent mainPanel = Box.createVerticalBox();
        getPanel().add( mainPanel, BorderLayout.NORTH );
        int nc = configurators.length;
        buttModels_ = new ButtonModel[ nc ];
        buttGrp_ = new ButtonGroup();
        for ( int ic = 0; ic < nc; ic++ ) {
            ActivatorConfigurator subConfigurator = configurators_[ ic ];
            subConfigurator.addActionListener( getActionForwarder() );
            JComponent subBox = Box.createVerticalBox();
            subBox.setBorder( BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 )
            ) );
            JComponent subPanel = subConfigurator.getPanel();
            JRadioButton butt = new JRadioButton( subConfigurator.toString() );
            subPanel.setEnabled( butt.isSelected() );
            butt.addChangeListener( evt -> {
                subPanel.setEnabled( butt.isSelected() );
            } );
            butt.addActionListener( getActionForwarder() );
            buttGrp_.add( butt );
            buttModels_[ ic ] = butt.getModel();
            JComponent buttLine = Box.createHorizontalBox();
            buttLine.add( butt );
            buttLine.add( Box.createHorizontalGlue() );
            subBox.add( buttLine );
            subBox.add( subPanel );
            mainPanel.add( Box.createVerticalStrut( 5 ) );
            mainPanel.add( subBox );
        }
        setSelectedIndex( IDFLT );
        assert getSelectedConfigurator() != null;
    }

    public Activator getActivator() {
        return getSelectedConfigurator().getActivator();
    }

    public String getConfigMessage() {
        return getSelectedConfigurator().getConfigMessage();
    }

    public Safety getSafety() {
        return getSelectedConfigurator().getSafety();
    }

    public ConfigState getState() {
        ConfigState state = new ConfigState();
        state.setInt( ISUB_KEY, getSelectedIndex() );
        for ( ActivatorConfigurator subConfigurator : configurators_ ) {
            state.getMap().putAll( subConfigurator.getState().getMap() );
        }
        return state;
    }

    public void setState( ConfigState state ) {
        setSelectedIndex( state.getInt( ISUB_KEY ) );
        for ( ActivatorConfigurator subConfigurator : configurators_ ) {
             subConfigurator.setState( state );
        }
    }

    /**
     * Returns the currently selected configurator.
     *
     * @return  selected configurator, not null
     */
    private ActivatorConfigurator getSelectedConfigurator() {
        return configurators_[ getSelectedIndex() ];
    }

    /**
     * Returns the index of the currently selected configurator.
     *
     * @return  selected index
     */
    private int getSelectedIndex() {
        return Arrays.asList( buttModels_ ).indexOf( buttGrp_.getSelection() );
    }

    /**
     * Sets the index for the currently selected configurator.
     *
     * @param  ix  index in range
     */
    private void setSelectedIndex( int ix ) {
        buttModels_[ ix ].setSelected( true );
        assert ix == getSelectedIndex();
    }
}
