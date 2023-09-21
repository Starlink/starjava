package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.join.AnisotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.CdsHealpixSkyPixellator;
import uk.ac.starlink.table.join.CombinedMatchEngine;
import uk.ac.starlink.table.join.CuboidCartesianMatchEngine;
import uk.ac.starlink.table.join.EllipseCartesianMatchEngine;
import uk.ac.starlink.table.join.EllipseSkyMatchEngine;
import uk.ac.starlink.table.join.ErrorCartesianMatchEngine;
import uk.ac.starlink.table.join.ErrorQuadraturesCartesianMatchEngine;
import uk.ac.starlink.table.join.ErrorSkyMatchEngine;
import uk.ac.starlink.table.join.EqualsMatchEngine;
import uk.ac.starlink.table.join.HtmSkyPixellator;
import uk.ac.starlink.table.join.IsotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RangeModelProgressIndicator;
import uk.ac.starlink.table.join.ErrorSkyMatchEngine;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.table.join.SphericalPolarMatchEngine;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.task.RowRunnerParameter;

/**
 * Window for selecting the characteristics of and invoking a match 
 * (table join) operation.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MatchWindow extends AuxWindow implements ItemListener {

    private final int nTable;
    private final JComboBox<MatchEngine> engineSelector;
    private final Map<MatchEngine,MatchSpec> matchSpecs;
    private final CardLayout paramCards;
    private final JComponent paramContainer;
    private final JTextArea logArea;
    private final JScrollPane logScroller;
    private final JScrollPane specScroller;
    private final Action startAct;
    private final Action stopAct;
    private final JProgressBar progBar;
    private final ToggleButtonModel profileModel;
    private final ToggleButtonModel parallelModel;
    private MatchProgressIndicator currentIndicator;

    /**
     * Constructs a new MatchWindow.
     *
     * @param  parent  parent window, may be used for window positioning
     * @param  nTable  number of tables to participate in match
     */
    public MatchWindow( Component parent, int nTable ) {
        super( "Match Tables", parent );
        this.nTable = nTable;
        matchSpecs = new HashMap<MatchEngine,MatchSpec>();

        /* Get the list of all the match engines we know about. */
        MatchEngine[] engines = getEngines();
        int nEngine = engines.length;

        /* Prepare specific parameter fields for each engine. */
        paramCards = new CardLayout();
        paramContainer = new JPanel( paramCards );
        final ParameterPanel[] paramPanels = new ParameterPanel[ nEngine ];
        for ( int i = 0; i < nEngine; i++ ) {
            MatchEngine engine = engines[ i ];
            paramPanels[ i ] = new ParameterPanel( engine );
            paramContainer.add( paramPanels[ i ], labelFor( engine ) );
        }

        /* Prepare a combo box which can select the engines. */
        engineSelector = new JComboBox<MatchEngine>( engines );
        engineSelector.addItemListener( this );

        /* Set up an action to start the match. */
        startAct = new MatchAction( "Go", null, "Perform the match" );
        stopAct = new MatchAction( "Stop", null, "Cancel the calculation" );
        stopAct.setEnabled( false );

        /* Set up an action to display tuning information. */
        final ToggleButtonModel tuningModel =
            new ToggleButtonModel( "Tuning Parameters", ResourceIcon.TUNING,
                                   "Display tuning parameters" );
        tuningModel.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                boolean showTuning = tuningModel.isSelected();
                for ( int i = 0; i < paramPanels.length; i++ ) {
                    paramPanels[ i ].setTuningVisible( showTuning );
                }
                paramContainer.revalidate();
            }
        } );

        /* Set up an action to control parallel implementation. */
        parallelModel =
            new ToggleButtonModel( "Parallel execution", ResourceIcon.PARALLEL,
                                   "Set to run match using multithreaded "
                                 + "execution" );
        parallelModel.setSelected( true );

        /* Set up an action to perform profiling during match. */
        profileModel =
            new ToggleButtonModel( "Full Profiling", ResourceIcon.PROFILE,
                                   "Determine and show timing and memory "
                                 + "profiling information in calculation log" );

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
        specScroller = new JScrollPane();
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
        engineBox.setBorder( makeTitledBorder( "Match Criteria" ) );
        common.add( engineBox );

        /* Place actions. */
        getToolBar().add( tuningModel.createToolbarButton() );
        getToolBar().add( profileModel.createToolbarButton() );
        getToolBar().add( parallelModel.createToolbarButton() );
        JMenu tuningMenu = new JMenu( "Tuning" );
        tuningMenu.setMnemonic( KeyEvent.VK_T );
        tuningMenu.add( tuningModel.createMenuItem() );
        tuningMenu.add( profileModel.createMenuItem() );
        tuningMenu.add( parallelModel.createMenuItem() );
        getJMenuBar().add( tuningMenu );

        /* Add standard help actions. */
        getToolBar().addSeparator();
        final String helpTag;
        if ( nTable == 1 ) {
            helpTag = "MatchWindow1";
        }
        else if ( nTable == 2 ) {
            helpTag = "MatchWindow";
        }
        else {
            helpTag = "MatchWindowN";
        }
        addHelp( helpTag );

        /* Set up components associated with logging calculation progress. */
        logArea = new JTextArea();
        logArea.setEditable( false );
        logArea.setRows( 5 );
        progBar = placeProgressBar();
        logScroller = new JScrollPane( logArea );
        main.add( logScroller, BorderLayout.SOUTH );

        /* Initialise to an active state. */
        engineSelector.setSelectedIndex( 0 );
        updateDisplay();
    }

    /**
     * Returns the MatchSpec object which is indicated by the current
     * state of the selectors in this window.  This may return an old
     * one if a suitable one exists, or it may create a new one.
     *
     * @return match-type specific specification of the match that will happen
     */
    private MatchSpec getMatchSpec() {
        MatchEngine engine = getMatchEngine();
        if ( ! matchSpecs.containsKey( engine ) ) {
            matchSpecs.put( engine, makeMatchSpec( engine ) );
        }
        return matchSpecs.get( engine );
    }

    /**
     * Returns the currently selected match engine.
     *
     * @return  match engine
     */
    private MatchEngine getMatchEngine() {
        return engineSelector.getItemAt( engineSelector.getSelectedIndex() );
    }

    /**
     * Creates a new MatchSpec for this window based on a given MatchEngine.
     * 
     * @param   engine  match engine
     * @return  new MatchSpec
     */
    private MatchSpec makeMatchSpec( MatchEngine engine ) {
        Supplier<RowRunner> runnerFact =
            () -> parallelModel.isSelected()
                ? RowRunnerParameter.DFLT_MATCH_RUNNER
                : RowRunner.SEQUENTIAL;
        switch( nTable ) {
            case 1:
                return new IntraMatchSpec( engine, runnerFact );
            case 2:
                return new PairMatchSpec( engine, runnerFact );
            default:
                return new InterMatchSpec( engine, runnerFact, nTable );
        }
    }

    /**
     * Called when one of the selection controls is changed and aspects
     * of the GUI need to be updated.
     */
    private void updateDisplay() {
        MatchEngine engine = getMatchEngine();
        if ( engine != null ) {
            paramCards.show( paramContainer, labelFor( engine ) );
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
     * Returns a string which identifies a MatchEngine.  This is used
     * by the CardLayoutManager; if the labels are not distinct for all
     * the engines, there's trouble.  If CardLayoutManager were written
     * sensibly it would be possible to key components by Object not
     * String, and this wouldn't be necessary.
     */
    private static String labelFor( MatchEngine engine ) {
        return engine.getClass() + ":" 
             + engine.toString() + "@"
             + System.identityHashCode( engine );
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
            currentIndicator =
                new MatchProgressIndicator( profileModel.isSelected() );
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
                        progBar.setValue( 0 );
                        currentIndicator = null;
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
        MatchProgressIndicator( boolean profile ) {
            super( profile );
        }
        public void startStage( String stage ) {
            if ( currentIndicator == this ) {
                scheduleAppendLogLine( stage + "..." );
            }
            super.startStage( stage );
        }
        public void logMessage( String msg ) {
            if ( currentIndicator == this ) {
                scheduleAppendLogLine( msg );
            }
            super.logMessage( msg );
        }
        public void setLevel( double level ) throws InterruptedException {
            if ( currentIndicator != this ) {
                throw new InterruptedException( "Interrupted by user" );
            }
            super.setLevel( level );
        }
        private void scheduleAppendLogLine( final String line ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    appendLogLine( line );
                }
            } );
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
                if ( currentIndicator != null ) {
                    appendLogLine( "Calculation interrupted" );
                    progBar.setValue( 0 );
                }
                currentIndicator = null;
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Returns a list of the known match engines.
     *
     * @return  match engine array
     */
    private static MatchEngine[] getEngines() {
        double someAngle = CoordsRadians.ARC_SECOND_RADIANS;
        double someLength = 1.0;
        double[] someLengths1 = new double[ 1 ];
        double[] someLengths2 = new double[ 2 ];
        double[] someLengths3 = new double[ 3 ];
        double[] someLengths4 = new double[ 4 ];
        Arrays.fill( someLengths1, someLength );
        Arrays.fill( someLengths2, someLength );
        Arrays.fill( someLengths3, someLength );
        Arrays.fill( someLengths4, someLength );
        MatchEngine skyPlus1Engine = createCombinedEngine( "Sky + X",
            new MatchEngine[] {
                new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                         someAngle ),
                new AnisotropicCartesianMatchEngine( someLengths1 ),
            }
        );
        MatchEngine skyPlus1ErrEngine =
                createCombinedEngine( "Sky + X with Errors",
            new MatchEngine[] {
                new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                         someAngle ),
                new ErrorCartesianMatchEngine( 1, someLength ),
            }
        );
        MatchEngine skyPlus2Engine = createCombinedEngine( "Sky + XY",
            new MatchEngine[] {
                new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                         someAngle ),
                new AnisotropicCartesianMatchEngine( someLengths2 ),
            }
        );
        MatchEngine htmEngine = new FixedSkyMatchEngine( new HtmSkyPixellator(),
                                                         someAngle ) {
            public String toString() {
                return "HTM";
            }
        };
        return new MatchEngine[] {
            new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(), someAngle ),
            new ErrorSkyMatchEngine( new CdsHealpixSkyPixellator(), someAngle ),
            new EllipseSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                       someAngle ),
            new SphericalPolarMatchEngine( someLength ),
            new EqualsMatchEngine(),
            new IsotropicCartesianMatchEngine( 1, someLength, false ),
            new ErrorCartesianMatchEngine( 1, someLength ),
            new ErrorQuadraturesCartesianMatchEngine( 1, someLength ),
            new IsotropicCartesianMatchEngine( 2, someLength, false ),
            new AnisotropicCartesianMatchEngine( someLengths2 ),
            new CuboidCartesianMatchEngine( someLengths2 ),
            new ErrorCartesianMatchEngine( 2, someLength ),
            new EllipseCartesianMatchEngine( someLength ),
            new IsotropicCartesianMatchEngine( 3, someLength, false ),
            new AnisotropicCartesianMatchEngine( someLengths3 ),
            new CuboidCartesianMatchEngine( someLengths3 ),
            new ErrorCartesianMatchEngine( 3, someLength ),
            new AnisotropicCartesianMatchEngine( someLengths4 ),
            skyPlus1Engine,
            skyPlus1ErrEngine,
            skyPlus2Engine,
            htmEngine,
        };
    }

    /**
     * Constructs a CombinedMatchEngine from an array of constituents.
     *
     * @param  name   output matcher name
     * @param  subEngines   constituent match engines
     * @return  resulting engine
     */
    private static MatchEngine
            createCombinedEngine( String name, MatchEngine[] subEngines ) {
        CombinedMatchEngine cEngine = new CombinedMatchEngine( subEngines );
        cEngine.setName( name );

        /* This tests that the resulting match engine has a suitable
         * match score calculation function; that is one that can
         * reasonably be compared between tuple pairs to provide
         * a "best" match.  A matcher could still be used in the
         * absence of this, but at present all the supplied combined
         * matchers do satisfy this, which is probably good policy since
         * it reduces surprising results. */
        assert cEngine.getScoreScale() > 0
             : "CombinedMatchEngine " + name + " is not scalable";
        return cEngine;
    }
}
