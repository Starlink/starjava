package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.AnisotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.CdsHealpixSkyPixellator;
import uk.ac.starlink.table.join.CombinedMatchEngine;
import uk.ac.starlink.table.join.CuboidCartesianMatchEngine;
import uk.ac.starlink.table.join.EllipseCartesianMatchEngine;
import uk.ac.starlink.table.join.EllipseSkyMatchEngine;
import uk.ac.starlink.table.join.ErrorCartesianMatchEngine;
import uk.ac.starlink.table.join.ErrorSkyMatchEngine;
import uk.ac.starlink.table.join.ErrorSummation;
import uk.ac.starlink.table.join.EqualsMatchEngine;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.table.join.HtmSkyPixellator;
import uk.ac.starlink.table.join.HumanMatchEngine;
import uk.ac.starlink.table.join.IsotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RangeModelProgressIndicator;
import uk.ac.starlink.table.join.SphericalPolarMatchEngine;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.StiltsAction;
import uk.ac.starlink.topcat.StiltsReporter;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatTableNamer;
import uk.ac.starlink.topcat.TupleSelector;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.join.MatchEngineParameter;
import uk.ac.starlink.ttools.join.SkyMatch2Mapper;
import uk.ac.starlink.ttools.task.MapperTask;
import uk.ac.starlink.ttools.task.RowRunnerParameter;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.SettingGroup;
import uk.ac.starlink.ttools.task.SkyMatch2;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableMatch1;
import uk.ac.starlink.ttools.task.TableMatch2;
import uk.ac.starlink.ttools.task.TableMatchN;
import uk.ac.starlink.ttools.task.TablesInput;

/**
 * Window for selecting the characteristics of and invoking a match 
 * (table join) operation.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MatchWindow extends AuxWindow implements StiltsReporter {

    private final int nTable_;
    private final ActionForwarder forwarder_;
    private final JComboBox<MatchEngine> engineSelector_;
    private final Map<MatchEngine,MatchSpec> matchSpecs_;
    private final CardLayout paramCards_;
    private final JComponent paramContainer_;
    private final JTextArea logArea_;
    private final JScrollPane logScroller_;
    private final JScrollPane specScroller_;
    private final Action startAct_;
    private final Action stopAct_;
    private final JProgressBar progBar_;
    private final ToggleButtonModel tuningModel_;
    private final ToggleButtonModel profileModel_;
    private final ToggleButtonModel parallelModel_;
    private final Map<MatchEngine,String> engineMap_;
    private MatchProgressIndicator currentIndicator_;

    /**
     * Constructs a new MatchWindow.
     *
     * @param  parent  parent window, may be used for window positioning
     * @param  nTable  number of tables to participate in match
     */
    @SuppressWarnings("this-escape")
    public MatchWindow( Component parent, int nTable ) {
        super( "Match Tables", parent );
        nTable_ = nTable;
        matchSpecs_ = new HashMap<MatchEngine,MatchSpec>();
        forwarder_ = new ActionForwarder();

        /* Get the list of all the match engines we know about. */
        engineMap_ = getEngineMap();
        MatchEngine[] engines =
            engineMap_.keySet().toArray( new MatchEngine[ 0 ] );
        int nEngine = engines.length;

        /* Prepare specific parameter fields for each engine. */
        paramCards_ = new CardLayout();
        paramContainer_ = new JPanel( paramCards_ );
        final ParameterPanel[] paramPanels = new ParameterPanel[ nEngine ];
        for ( int i = 0; i < nEngine; i++ ) {
            MatchEngine engine = engines[ i ];
            paramPanels[ i ] = new ParameterPanel( engine );
            paramContainer_.add( paramPanels[ i ], labelFor( engine ) );
            paramPanels[ i ].addChangeListener( forwarder_ );
        }

        /* Prepare a combo box which can select the engines. */
        engineSelector_ = new JComboBox<MatchEngine>( engines );
        engineSelector_.addItemListener( evt -> updateDisplay() );
        engineSelector_.addActionListener( forwarder_ );

        /* Set up actions to start and stop the match. */
        progBar_ = placeProgressBar();
        startAct_ = BasicAction.create( "Go", null, "Perform the match",
                                        evt -> {
            forwarder_.actionPerformed( evt );
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
        } );
        stopAct_ = BasicAction.create( "Stop", null, "Cancel the calculation",
                                       evt -> {
            if ( currentIndicator_ != null ) {
                appendLogLine( "Calculation interrupted" );
                progBar_.setValue( 0 );
            }
            currentIndicator_ = null;
        } );
        stopAct_.setEnabled( false );

        /* Set up an action to display tuning information. */
        tuningModel_ = new ToggleButtonModel( "Tuning Parameters",
                                              ResourceIcon.TUNING,
                                              "Display tuning parameters" );
        tuningModel_.addChangeListener( evt -> {
            boolean showTuning = tuningModel_.isSelected();
            for ( int i = 0; i < paramPanels.length; i++ ) {
                paramPanels[ i ].setTuningVisible( showTuning );
            }
            paramContainer_.revalidate();
        } );
        tuningModel_.addChangeListener( forwarder_ );

        /* Set up an action to control parallel implementation. */
        parallelModel_ =
            new ToggleButtonModel( "Parallel execution", ResourceIcon.PARALLEL,
                                   "Set to run match using multithreaded "
                                 + "execution" );
        parallelModel_.setSelected( true );
        parallelModel_.addChangeListener( forwarder_ );

        /* Set up an action to perform profiling during match. */
        profileModel_ =
            new ToggleButtonModel( "Full Profiling", ResourceIcon.PROFILE,
                                   "Determine and show timing and memory "
                                 + "profiling information in calculation log" );
        profileModel_.addChangeListener( forwarder_ );

        /* Place the components. */
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( startAct_ ) );
        buttonBox.add( Box.createHorizontalStrut( 10 ) );
        buttonBox.add( new JButton( stopAct_ ) );
        buttonBox.add( Box.createHorizontalGlue() );
        getControlPanel().add( buttonBox );

        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        Box common = Box.createVerticalBox();
        main.add( common, BorderLayout.NORTH );
        specScroller_ = new JScrollPane();
        main.add( specScroller_, BorderLayout.CENTER );

        Box engineBox = Box.createVerticalBox();
        Box line;
        line = Box.createHorizontalBox();
        line.add( new JLabel( "Algorithm: " ) );
        line.add( engineSelector_ );
        line.add( Box.createHorizontalGlue() );
        engineBox.add( line );
        line = Box.createHorizontalBox();
        line.add( paramContainer_ );
        line.add( Box.createHorizontalGlue() );
        engineBox.add( line );
        engineBox.setBorder( makeTitledBorder( "Match Criteria" ) );
        common.add( engineBox );

        /* Place actions. */
        getToolBar().add( tuningModel_.createToolbarButton() );
        getToolBar().add( profileModel_.createToolbarButton() );
        getToolBar().add( parallelModel_.createToolbarButton() );
        getToolBar().add( new StiltsAction( this, () -> this ) );
        JMenu tuningMenu = new JMenu( "Tuning" );
        tuningMenu.setMnemonic( KeyEvent.VK_T );
        tuningMenu.add( tuningModel_.createMenuItem() );
        tuningMenu.add( profileModel_.createMenuItem() );
        tuningMenu.add( parallelModel_.createMenuItem() );
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
        logArea_ = new JTextArea();
        logArea_.setEditable( false );
        logArea_.setRows( 5 );
        logScroller_ = new JScrollPane( logArea_ );
        main.add( logScroller_, BorderLayout.SOUTH );

        /* Initialise to an active state. */
        engineSelector_.setSelectedIndex( 0 );
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
        if ( ! matchSpecs_.containsKey( engine ) ) {
            MatchSpec matchSpec = makeMatchSpec( engine );
            matchSpec.addActionListener( forwarder_ );
            matchSpecs_.put( engine, matchSpec );
        }
        return matchSpecs_.get( engine );
    }

    /**
     * Returns the currently selected match engine.
     *
     * @return  match engine
     */
    private MatchEngine getMatchEngine() {
        return engineSelector_.getItemAt( engineSelector_.getSelectedIndex() );
    }

    /**
     * Creates a new MatchSpec for this window based on a given MatchEngine.
     * 
     * @param   engine  match engine
     * @return  new MatchSpec
     */
    private MatchSpec makeMatchSpec( MatchEngine engine ) {
        Supplier<RowRunner> runnerFact = this::getRowRunner;
        switch( nTable_ ) {
            case 1:
                return new IntraMatchSpec( engine, runnerFact );
            case 2:
                return new PairMatchSpec( engine, runnerFact );
            default:
                return new InterMatchSpec( engine, runnerFact, nTable_ );
        }
    }

    /**
     * Returns the row runner which will be used to execute the match.
     *
     * @return  currently selected row runner
     */
    private RowRunner getRowRunner() {
        return parallelModel_.isSelected()
             ? RowRunnerParameter.DFLT_MATCH_RUNNER
             : RowRunner.SEQUENTIAL;
    }

    /**
     * Called when one of the selection controls is changed and aspects
     * of the GUI need to be updated.
     */
    private void updateDisplay() {
        MatchEngine engine = getMatchEngine();
        if ( engine != null ) {
            paramCards_.show( paramContainer_, labelFor( engine ) );
            specScroller_.setViewportView( getMatchSpec().getPanel() );
        }
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
        logScroller_.setEnabled( true );
        logArea_.setEnabled( true );
        stopAct_.setEnabled( busy );
        startAct_.setEnabled( ! busy );
        super.setBusy( busy );
    }

    /**
     * Adds a line of text to the logging window.
     *
     * @param   line  line to add (excluding terminal newline)
     */
    private void appendLogLine( String line ) {
        logArea_.append( line );
        logArea_.append( "\n" );
        logArea_.setCaretPosition( logArea_.getDocument().getLength() );
    }

    /**
     * Extends the dispose method to interrupt any pending calculation.
     */
    public void dispose() {
        stopAct_.actionPerformed( null );
        super.dispose();
    }

    public void addStiltsListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    public void removeStiltsListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public StiltsCommand createStiltsCommand( TopcatTableNamer tnamer ) {
        final Task task;
        final MatchEngine matcher = getMatchEngine();
        MatchSpec matchSpec = getMatchSpec();
        TupleSelector[] tupleSelectors = matchSpec.getTupleSelectors();
        DescribedValue[] tuningDvals = matcher.getTuningParameters();
        TopcatModel[] tcModels =
            Arrays.stream( tupleSelectors )
                  .map( t -> t.getTable() )
                  .toArray( n -> new TopcatModel[ n ] );
        if ( Arrays.stream( tcModels ).anyMatch( t -> t == null ) ) {
            return null;
        }

        /* Determine what type of match task will be used depending on
         * the number of input tables, and set up parameters for the
         * input tables. */
        List<Setting> tableSettings = new ArrayList<>();
        if ( nTable_ == 1 ) {
            TableMatch1 task1 = new TableMatch1();
            task = task1;
            tableSettings.addAll( tnamer.createInputTableSettings(
                                      task1.getInputTableParameter(),
                                      task1.getInputFilterParameter(),
                                      tcModels[ 0 ] ) );
        }
        else {
            final MapperTask mtask;
            if ( nTable_ == 2 ) {
                boolean hasSubsets =
                     Arrays.stream( tcModels )
                           .map( t -> t.getSelectedSubset() )
                           .anyMatch( s -> s != null && s != RowSubset.ALL );
                if ( matcher instanceof FixedSkyMatchEngine && ! hasSubsets ) {
                    mtask = new SkyMatch2();
                }
                else {
                    mtask = new TableMatch2();
                }
            }
            else {
                mtask = new TableMatchN();
            }
            task = mtask;
            TablesInput tinput = mtask.getTablesInput();
            for ( int i = 0; i < nTable_; i++ ) {
                tableSettings.addAll( tnamer.createInputTableSettings(
                                          tinput.getInputTableParameter( i ),
                                          tinput.getFilterParameter( i ),
                                          tcModels[ i ] ) );
            }
        }
        SettingGroup tablesGroup =
            new SettingGroup( 1, tableSettings.toArray( new Setting[ 0 ] ) );

        /* Determine the MatchEngine to be used, and acquire settings
         * specific to it. */
        List<Setting> matcherSettings = new ArrayList<>();
        final MatchEngineParameter matcherParam;
        if ( task instanceof SkyMatch2 ) {
            matcherParam = null;
            SkyMatch2Mapper tmapper = ((SkyMatch2) task).getMapper();
            DescribedValue errDval = matcher.getMatchParameters()[ 0 ];
            ValueInfo errInfo = errDval.getInfo();
            assert errInfo.getUnitString().startsWith( "rad" );
            Object errVal = errDval.getValue();
            if ( errVal instanceof Number ) {
                double errDeg =
                    Math.toDegrees( ((Number) errVal).doubleValue() );
                Setting errSetting = pset( tmapper.getErrorArcsecParameter(),
                                           Double.valueOf( errDeg * 3600 ) );
                matcherSettings.add( errSetting );
            }
            if ( tuningModel_.isSelected() && tuningDvals.length == 1 ) {
                DescribedValue dval = tuningDvals[ 0 ];
                ValueInfo info = dval.getInfo();
                if ( info.getName().toLowerCase().indexOf( "healpix" ) >= 0 &&
                     dval.getValue() instanceof Integer ) {
                    matcherSettings
                   .add( pset( tmapper.getHealpixLevelParameter(),
                               (Integer) dval.getValue() ) );
                }
            }
        }
        else {
            String matcherName = engineMap_.get( matcher );
            matcherParam =
                (MatchEngineParameter)
                StiltsCommand.getParameterByType( task, MatchEngine.class );

            // dflt is sky, but probably shouldn't be, so pretend it's null
            matcherSettings.add( new Setting( matcherParam.getName(),
                                              matcherName, null ) );
            DescribedValue[] matcherDvals = matcher.getMatchParameters();
            if ( matcherDvals.length > 0 ) {
                List<String> matcherConfigs = new ArrayList<>();
                for ( DescribedValue dval : matcherDvals ) {
                    String unit = dval.getInfo().getUnitString();
                    Object val = dval.getValue();
                    String sval;
                    if ( unit != null && unit.startsWith( "rad" ) &&
                         val instanceof Number ) {
                        double valRad = ((Number) val).doubleValue();
                        sval = Double.toString( Math.toDegrees( valRad )
                                              * 3600 );
                    }
                    else {
                        sval = String.valueOf( val );
                    }
                    matcherConfigs.add( sval );
                }
                matcherSettings.add( pset( matcherParam
                                          .getMatchParametersParameter(),
                                           matcherConfigs
                                          .toArray( new String[ 0 ] ) ) );
            }
            if ( tuningModel_.isSelected() && tuningDvals.length > 0 ) {
                List<String> tuningConfigs = new ArrayList<>();
                for ( DescribedValue dval : tuningDvals ) {
                    tuningConfigs.add( String.valueOf( dval.getValue() ) );
                }
                matcherSettings.add( pset( matcherParam
                                          .getTuningParametersParameter(),
                                           tuningConfigs
                                          .toArray( new String[ 0 ] ) ) );
            }
        }
        SettingGroup matcherGroup =
            new SettingGroup( 1, matcherSettings.toArray( new Setting[ 0 ] ) );

        /* Settings for the tuple match values. */
        List<Setting> valuesSettings = new ArrayList<>();
        HumanMatchEngine humanMatcher = new HumanMatchEngine( matcher );
        if ( task instanceof SkyMatch2 ) {
            SkyMatch2Mapper tmapper = ((SkyMatch2) task).getMapper();
            StringParameter[] raParams = tmapper.getRaParameters();
            StringParameter[] decParams = tmapper.getDecParameters();
            for ( int it = 0; it < 2; it++ ) {
                String[] exprs = tupleSelectors[ it ]
                                .getStiltsTupleExpressions( humanMatcher );
                valuesSettings.add( pset( raParams[ it ], exprs[ 0 ] ) );
                valuesSettings.add( pset( decParams[ it ], exprs[ 1 ] ) );
            }
        }
        else {
            for ( int it = 0; it < nTable_; it++ ) {
                TupleSelector tupleSelector = tupleSelectors[ it ];
                String[] exprs = tupleSelector
                                .getStiltsTupleExpressions( humanMatcher );
                String label = nTable_ == 1 ? "" : Integer.toString( 1 + it );
                valuesSettings.add( pset( matcherParam
                                         .createMatchTupleParameter( label ),
                                          exprs ) );
            }
        }
        SettingGroup valuesGroup =
            new SettingGroup( 1, valuesSettings.toArray( new Setting[ 0 ] ) );

        /* Miscellaneous settings. */
        SettingGroup outGroup =
            new SettingGroup( 1, matchSpec.getOutputSettings( task ) );

        Setting[] extraSettings = new Setting[] {
            pset( StiltsCommand.getParameterByType( task, RowRunner.class ),
                 getRowRunner() ),
        };
        SettingGroup extrasGroup =
            new SettingGroup( 1, extraSettings );


        /* Create and return the stilts command specification. */
        SettingGroup[] groups = new SettingGroup[] {
            matcherGroup, tablesGroup, valuesGroup, outGroup, extrasGroup,
        };
        return StiltsCommand.createCommand( task, groups );
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
            currentIndicator_ =
                new MatchProgressIndicator( profileModel_.isSelected() );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    setBusy( true );
                    logArea_.setText( "" );
                    progBar_.setModel( currentIndicator_ );
                }
            } );
            try {
                spec.calculate( currentIndicator_ );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        setBusy( false );
                        spec.matchSuccess( MatchWindow.this );
                        appendLogLine( "Match succeeded" );
                        progBar_.setValue( 0 );
                        currentIndicator_ = null;
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
            if ( currentIndicator_ == this ) {
                scheduleAppendLogLine( stage + "..." );
            }
            super.startStage( stage );
        }
        public void logMessage( String msg ) {
            if ( currentIndicator_ == this ) {
                scheduleAppendLogLine( msg );
            }
            super.logMessage( msg );
        }
        public void setLevel( double level ) throws InterruptedException {
            if ( currentIndicator_ != this ) {
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
     * Returns a map from MatchEngine to (best effort) stilts matcher name
     * of all the matchers offered by this window.
     *
     * @return  match engine map
     * @see  uk.ac.starlink.ttools.join.MatchEngineParameter#createEngine
     */
    private static Map<MatchEngine,String> getEngineMap() {
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
        ErrorSummation errSum = ErrorSummation.SIMPLE;
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
                new ErrorCartesianMatchEngine( 1, errSum, someLength ),
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

        /* The names here come from the stilts MatchEngineParameter,
         * in particular its createEngine(name) method. */
        Map<MatchEngine,String> map = new LinkedHashMap<>();
        map.put( new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                          someAngle ),
                 "sky" );
        map.put( new ErrorSkyMatchEngine( new CdsHealpixSkyPixellator(), errSum,
                                          someAngle ),
                 "skyerr" );
        map.put( new EllipseSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                            someAngle ),
                 "skyellipse" );
        map.put( new SphericalPolarMatchEngine( someLength ),
                 "sky3d" );
        map.put( new EqualsMatchEngine(),
                 "exact" );
        map.put( new IsotropicCartesianMatchEngine( 1, someLength, false ),
                 "1d" );
        map.put( new ErrorCartesianMatchEngine( 1, errSum, someLength ),
                 "1derr" );
        map.put( new IsotropicCartesianMatchEngine( 2, someLength, false ),
                 "2d" );
        map.put( new AnisotropicCartesianMatchEngine( someLengths2 ),
                 "2d_anisotropic" );
        map.put( new CuboidCartesianMatchEngine( someLengths2 ),
                 "2d_cuboid" );
        map.put( new ErrorCartesianMatchEngine( 2, errSum, someLength ),
                 "2derr" );
        map.put( new EllipseCartesianMatchEngine( someLength ),
                 "2d_ellipse" );
        map.put( new IsotropicCartesianMatchEngine( 3, someLength, false ),
                 "3d" );
        map.put( new AnisotropicCartesianMatchEngine( someLengths3 ),
                 "3d_anisotropic" );
        map.put( new CuboidCartesianMatchEngine( someLengths3 ),
                 "3d_cuboid" );
        map.put( new ErrorCartesianMatchEngine( 3, errSum, someLength ),
                 "3derr" );
        map.put( new AnisotropicCartesianMatchEngine( someLengths4 ),
                 "4d_anisotropic" );
        map.put( skyPlus1Engine,
                 "sky+1d" );
        map.put( skyPlus1ErrEngine,
                 "sky+1derr" );
        map.put( skyPlus2Engine,
                 "sky+2" );
        map.put( htmEngine,
                 "sky" );
        return Collections.unmodifiableMap( map );
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
