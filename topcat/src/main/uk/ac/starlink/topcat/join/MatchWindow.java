package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.join.CartesianMatchEngine;
import uk.ac.starlink.table.join.HTMMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RangeModelProgressIndicator;
import uk.ac.starlink.table.join.SphericalPolarMatchEngine;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;

/**
 * Window for setting up and executing table matches.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Mar 2004
 */
public class MatchWindow extends AuxWindow implements ItemListener {

    private final JComboBox engineSelector;
    private final JComboBox specSelector;
    private final Map matchSpecs = new HashMap();
    private final CardLayout paramCards;
    private final JComponent paramContainer;
    private final JScrollPane specScroller;
    private final JTextArea logArea;
    private final JScrollPane logScroller;
    private final Action startAct;
    private final Action stopAct;
    private final JProgressBar progBar;
    private MatchProgressIndicator currentIndicator;

    /**
     * Constructs a new match window.
     *
     * @param  parent  parent window, may be used for window positioning
     */
    public MatchWindow( Component parent ) {
        super( "Match Tables", parent );

        /* Get the list of all the match engines we know about. */
        MatchEngine[] engines = getEngines();
        int nEngine = engines.length;

        /* Prepare specific parameter fields for each engine. */
        paramCards = new CardLayout();
        paramContainer = new JPanel( paramCards );
        for ( int i = 0; i < nEngine; i++ ) {
            MatchEngine engine = engines[ i ];
            String label = engine.toString();
            paramContainer.add( new ParameterPanel( engine ), label );
        }

        /* Prepare a combo box which can select the engines. */
        engineSelector = new JComboBox( engines );
        engineSelector.addItemListener( this );

        /* Prepare a combo box which can select match types. */
        specScroller = new JScrollPane();
        specSelector = new JComboBox( MatchSpecType.ALL );
        specSelector.addItemListener( this );

        /* Set up an action to start the match. */
        startAct = new MatchAction( "Go", null, "Perform the match" );
        stopAct = new MatchAction( "Stop", null, "Cancel the calculation" );
        stopAct.setEnabled( false );

        /* Place the components. */
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( startAct ) );
        buttonBox.add( Box.createHorizontalStrut( 10 ) );
        buttonBox.add( new JButton( stopAct ) );
        buttonBox.add( Box.createHorizontalGlue() );
        getControlPanel().add( buttonBox );

        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        Box common = Box.createVerticalBox();
        main.add( common, BorderLayout.NORTH );
        main.add( specScroller, BorderLayout.CENTER );

        Box engineBox = Box.createVerticalBox();
        Box line;
        line = Box.createHorizontalBox();
        line.add( new JLabel( "Algorithm: " ) );
        line.add( engineSelector );
        line.add( Box.createHorizontalGlue() );
        engineBox.add( line );
        line = Box.createHorizontalBox();
        line.add( paramContainer );
        line.add( Box.createHorizontalGlue() );
        engineBox.add( line );
        engineBox.setBorder( makeTitledBorder( "Match Details" ) );
        common.add( engineBox );

        Box specBox = Box.createVerticalBox();
        line = Box.createHorizontalBox();
        line.add( new JLabel( "Match Action: " ) );
        line.add( specSelector );
        specBox.add( line );
        common.add( Box.createVerticalStrut( 5 ) );
        common.add( specBox );
        common.add( Box.createVerticalStrut( 5 ) );

        /* Add standard help actions. */
        addHelp( "MatchWindow" );

        /* Set up components associated with logging calculation progress. */
        logArea = new JTextArea();
        logArea.setEditable( false );
        logArea.setRows( 5 );
        progBar = placeProgressBar();
        logScroller = new JScrollPane( logArea );
        main.add( logScroller, BorderLayout.SOUTH );

        /* Initialise to an active state. */
        engineSelector.setSelectedIndex( 0 );
        specSelector.setSelectedItem( MatchSpecType.M2 );
        updateDisplay();

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Returns the MatchSpec object which is indicated by the current
     * state of the selectors in this window.  This may return an old
     * one if a suitable one exists, or it may create a new one.
     *
     * @return match-type specific specification of the match that will happen
     */
    private MatchSpec getMatchSpec() {
        MatchSpecType specType = getMatchSpecType();
        MatchEngine engine = getMatchEngine();
        Object key = Arrays.asList( new Object[] { specType, engine } );
        if ( ! matchSpecs.containsKey( key ) ) {
            matchSpecs.put( key, specType.makeMatchSpec( engine ) );
        }
        return (MatchSpec) matchSpecs.get( key );
    }

    /**
     * Returns the currently selected match engine.
     *
     * @return  match engine
     */
    private MatchEngine getMatchEngine() {
        return (MatchEngine) engineSelector.getSelectedItem();
    }

    /**
     * Returns the currently selected MatchSpecType.
     * This defines what kind of MatchSpec will be created if one is needed.
     *
     * @return  matchspec type
     */
    private MatchSpecType getMatchSpecType() {
        return (MatchSpecType) specSelector.getSelectedItem();
    }

    /**
     * Returns a list of the known match engines.
     *
     * @return  match engine array
     */
    private static MatchEngine[] getEngines() {
        double someAngle = 0.001;
        double someLength = 0.1;
        return new MatchEngine[] {
            new HTMMatchEngine( someAngle ),
            new SphericalPolarMatchEngine( someLength ),
            new CartesianMatchEngine( 1, someLength ),
            new CartesianMatchEngine( 2, someLength ),
            new CartesianMatchEngine( 3, someLength ),
            new CartesianMatchEngine( 4, someLength ),
        };
    }

    /**
     * Called when one of the selection controls is changed and aspects
     * of the GUI need to be updated.
     */
    private void updateDisplay() {
        MatchEngine engine = (MatchEngine) engineSelector.getSelectedItem();
        MatchSpecType spec = (MatchSpecType) specSelector.getSelectedItem();
        if ( engine != null && spec != null ) {
            String label = engine.toString();
            paramCards.show( paramContainer, label );
            specScroller.setViewportView( getMatchSpec().getPanel() );
        }
    }

    /**
     * Implements ItemListener to update the GUI appearence when some of
     * the selections are changed by the user.
     */
    public void itemStateChanged( ItemEvent evt ) {
        updateDisplay();
    }

    /**
     * Provides visual feedback that the window is/is not available for
     * interaction, as well as enabling/disabling most of its interatable
     * components.  The window is set busy when it's doing a calculation.
     * 
     * @param  busy  true iff the window should be closed to new business
     */
    public void setBusy( boolean busy ) {
        recursiveSetEnabled( getMainArea(), ! busy );
        logScroller.setEnabled( true );
        logArea.setEnabled( true );
        stopAct.setEnabled( busy );
        startAct.setEnabled( ! busy );
        super.setBusy( busy );
    }

    /**
     * Adds a line of text to the logging window.
     *
     * @param   line  line to add (excluding terminal newline)
     */
    private void appendLogLine( String line ) {
        logArea.append( line );
        logArea.append( "\n" );
        logArea.setCaretPosition( logArea.getDocument().getLength() );
    }

    /**
     * Extends the dispose method to interrupt any pending calculation.
     */
    public void dispose() {
        stopAct.actionPerformed( null );
        super.dispose();
    }

    /**
     * Helper class representing the thread which performs the 
     * (possibly time-consuming) match calculations. 
     */
    private class MatchWorker extends Thread {
        final MatchSpec spec;
        final MatchEngine engine;
        
        MatchWorker( MatchSpec spec, MatchEngine engine ) {
            super( "Row Matcher" );
            this.spec = spec;
            this.engine = engine;
        }

        /**
         * Performs the calculation defined by this MatchWindow, and 
         * dispatches whatever window updates are necessary before and after
         * to the event dispatch thread.
         */
        public void run() {
            currentIndicator = new MatchProgressIndicator();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    setBusy( true );
                    logArea.setText( "" );
                    progBar.setModel( currentIndicator );
                }
            } );
            try {
                spec.calculate( currentIndicator );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        setBusy( false );
                        spec.matchSuccess( MatchWindow.this );
                        appendLogLine( "Match succeeded" );
                    }
                } );
            }
            catch ( final Throwable e ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( e instanceof InterruptedException ) {
                            // no further action
                        }
                        else { // IOException - other unchecked ones??
                            appendLogLine( "Match failed: " + e.getMessage() );
                            spec.matchFailure( e, MatchWindow.this );
                        }
                        setBusy( false );
                    }
                } );
            }
        }
    }

    /**
     * ProgressIndicator implementation which controls updating the window
     * with information about how the calculation is going.
     */
    private class MatchProgressIndicator extends RangeModelProgressIndicator {
        public void startStage( String stage ) {
            if ( currentIndicator == this ) {
                appendLogLine( stage + "..." );
            }
            super.startStage( stage );
        }
        public void logMessage( String msg ) {
            if ( currentIndicator == this ) {
                appendLogLine( msg );
            }
            super.logMessage( msg );
        }
        public void setLevel( double level ) throws InterruptedException {
            if ( currentIndicator != this ) {
                throw new InterruptedException( "Interrupted by user" );
            }
            super.setLevel( level );
        }
    }

    /**
     * Implements actions for this window.
     */
    private class MatchAction extends BasicAction {
        MatchAction( String name, Icon icon, String description ) {
            super( name, icon, description );
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( this == startAct ) {
                MatchSpec spec = getMatchSpec();
                MatchEngine engine = getMatchEngine();
                try {
                    spec.checkArguments();
                    new MatchWorker( spec, engine ).start();
                }
                catch ( IllegalStateException e ) {
                    JOptionPane
                   .showMessageDialog( MatchWindow.this, e.getMessage(),
                                       "Invalid Match Arguments",
                                       JOptionPane.WARNING_MESSAGE );
                }
            }
            else if ( this == stopAct ) {
                appendLogLine( "Calculation interrupted" );
                progBar.setValue( 0 );
                currentIndicator = null;
            }
        }
    }

    /**
     * Helper class enumerating the possible MatchSpec types.
     * Instances of the class are effectively MatchSpec factories.
     */
    private static class MatchSpecType {
        static final MatchSpecType M1 = new MatchSpecType( "Internal" );
        static final MatchSpecType M2 = new MatchSpecType( "Pair" );
        static final MatchSpecType M3 = new MatchSpecType( "Triple" );
        static final MatchSpecType M4 = new MatchSpecType( "Quadruple" );
        static MatchSpecType[] ALL = new MatchSpecType[] { M1, M2, M3, M4 };
        private String name;
        private MatchSpecType( String name ) {
            this.name = name;
        }
        MatchSpec makeMatchSpec( MatchEngine engine ) {
            if ( this == M1 ) {
                return new IntraMatchSpec( engine );
            }
            else if ( this == M2 ) {
                return new InterMatchSpec( engine, 2 );
            }
            else if ( this == M3 ) {
                return new InterMatchSpec( engine, 3 );
            }
            else if ( this == M4 ) {
                return new InterMatchSpec( engine, 4 );
            }
            else {
                throw new AssertionError();
            }
        }
        public String toString() {
            return name;
        }
    }
}
