package uk.ac.starlink.topcat.join;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.SequentialRowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.topcat.AlignedBox;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ColumnSelectorModel;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.Scheduler;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.ConeErrorPolicy;
import uk.ac.starlink.ttools.cone.ConeMatcher;
import uk.ac.starlink.ttools.cone.ConeQueryCoverage;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.MocCoverage;
import uk.ac.starlink.ttools.cone.ParallelResultRowSequence;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.task.TableProducer;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component for performing a multicone-type join between a selected input
 * table and a remote DAL service.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2009
 */
public class DalMultiPanel extends JPanel {

    private final DalMultiService service_;
    private final ContentCoding coding_;
    private final JProgressBar progBar_;
    private final JTextField urlField_;
    private final boolean hasCoverage_;
    private final CoverageView serviceCoverageView_;
    private final CoverageView queryCoverageView_;
    private final CoverageView overlapCoverageView_;
    private final ColumnSelector raSelector_;
    private final ColumnSelector decSelector_;
    private final ColumnSelector srSelector_;
    private final JComboBox<MulticoneMode> modeSelector_;
    private final SpinnerNumberModel parallelModel_;
    private final JComboBox<ConeErrorPolicy> erractSelector_;
    private final Action startAction_;
    private final Action stopAction_;
    private final JComponent[] components_;
    private final TopcatListener tcListener_;
    private final ToggleButtonModel coverageModel_;
    private final JComponent urlLine_;
    private Coverage lastCoverage_;
    private URL lastCoverageUrl_;
    private TopcatModel tcModel_;
    private MatchWorker matchWorker_;

    /** Metadata for index column - used internally on transient tables only. */
    private static final DefaultValueInfo INDEX_INFO =
        new DefaultValueInfo( "__MulticoneIndex__", Integer.class,
                              "Table row index, 0-based" );

    /** Name for distance column. */
    private static final String DIST_NAME = "Separation";

    /** Name for erract selector box. */
    private static final String ERRACT_LABEL = "Error Handling";

    /**
     * Constructor.
     *
     * @param  service  defines type of service that queries will be 
     *                  carried out on
     */
    @SuppressWarnings("this-escape")
    public DalMultiPanel( DalMultiService service, JProgressBar progBar ) {
        super( new BorderLayout() );
        service_ = service;
        coding_ = ContentCoding.GZIP;
        progBar_ = progBar;
        progBar.setStringPainted( true );
        JComponent main = AlignedBox.createVerticalBox();
        List<JComponent> cList = new ArrayList<JComponent>();
        add( main );

        /* Field for service URL. */
        urlField_ = new JTextField();
        JLabel urlLabel = new JLabel( service.getName() + " URL: " );
        urlLine_ = Box.createHorizontalBox();
        cList.add( urlField_ );
        cList.add( urlLabel );
        urlLine_.add( urlLabel );
        urlLine_.add( urlField_ );
        main.add( urlLine_ );
        main.add( Box.createVerticalStrut( 10 ) );

        /* Field for input table. */
        final JComboBox<TopcatModel> tableSelector =
            new TablesListComboBox( 250 );
        tableSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setInputTable( tableSelector
                              .getItemAt( tableSelector.getSelectedIndex() ) );
            }
        } );
        JLabel tableLabel = new JLabel( "Input Table: " );
        cList.add( tableLabel );
        cList.add( tableSelector );
        Box tableLine = Box.createHorizontalBox();
        tableLine.add( tableLabel );
        tableLine.add( tableSelector );
        JComponent tpanel = new JPanel( new BorderLayout() );
        tpanel.add( tableLine, BorderLayout.CENTER );
        main.add( tpanel );
        main.add( Box.createVerticalStrut( 5 ) );

        /* Fields for position parameters. */
        raSelector_ = new ColumnSelector( Tables.RA_INFO, true );
        Box raLine = Box.createHorizontalBox();
        raLine.add( raSelector_ );
        JLabel raSysLabel = new JLabel( " (J2000)" );
        raLine.add( raSysLabel );
        raLine.add( Box.createHorizontalGlue() );
        main.add( raLine );
        main.add( Box.createVerticalStrut( 5 ) );
        cList.add( raSelector_ );
        cList.add( raSysLabel );
        decSelector_ = new ColumnSelector( Tables.DEC_INFO, true );
        Box decLine = Box.createHorizontalBox();
        decLine.add( decSelector_ );
        JLabel decSysLabel = new JLabel( " (J2000)" );
        decLine.add( decSysLabel );
        decLine.add( Box.createHorizontalGlue() );
        main.add( decLine );
        main.add( Box.createVerticalStrut( 5 ) );
        cList.add( decSelector_ );
        cList.add( decSysLabel );
        srSelector_ = new ColumnSelector( service.getSizeInfo(), true );
        service.setSizeDefault( srSelector_ );
        Box srLine = Box.createHorizontalBox();
        srLine.add( srSelector_ );
        JLabel srSysLabel = new JLabel( "" );
        srLine.add( srSysLabel );
        srLine.add( Box.createHorizontalGlue() );
        main.add( srLine );
        cList.add( srSelector_ );
        cList.add( srSysLabel );

        /* Align the positional fields. */
        TopcatUtils.alignComponents( new JLabel[] { raSelector_.getLabel(),
                                                    decSelector_.getLabel(),
                                                    srSelector_.getLabel(), } );
        TopcatUtils.alignComponents( new JLabel[] { raSysLabel, decSysLabel,
                                                    srSysLabel, } );

        /* Custom service controls. */
        JComponent servicePanel = service_.getControlPanel();
        if ( servicePanel != null ) {
            JComponent serviceLine = Box.createHorizontalBox();
            serviceLine.add( servicePanel );
            serviceLine.add( Box.createHorizontalGlue() );
            main.add( Box.createVerticalStrut( 5 ) );
            main.add( serviceLine );
            cList.add( servicePanel );
        }

        /* Multicone output mode selector. */
        main.add( Box.createVerticalStrut( 10 ) );
        modeSelector_ = new JComboBox<MulticoneMode>( getMulticoneModes() );
        Box modeLine = Box.createHorizontalBox();
        JLabel modeLabel = new JLabel( "Output Mode: " );
        modeLine.add( modeLabel );
        modeLine.add( new ShrinkWrapper( modeSelector_ ) );
        modeLine.add( Box.createHorizontalGlue() );
        cList.add( modeLabel );
        cList.add( modeSelector_ );
        main.add( modeLine );
        main.add( Box.createVerticalStrut( 5 ) );

        /* Set up coverage icons. */
        hasCoverage_ = service_.hasCoverages();
        if ( hasCoverage_ ) {

            /* Service coverage icon. */
            serviceCoverageView_ =
                new CoverageView( service_.getLabel() + " service" );
            serviceCoverageView_.setForeground( new Color( 0x0000ff ) );
            serviceCoverageView_.setBackground( new Color( 0xc0c0ff ) );
            urlField_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateServiceCoverage();
                }
            } );
            urlField_.addFocusListener( new FocusListener() {
                public void focusLost( FocusEvent evt ) {
                    updateServiceCoverage();
                }
                public void focusGained( FocusEvent evt ) {
                }
            } );
            urlLine_.add( Box.createHorizontalStrut( 5 ) );
            urlLine_.add( serviceCoverageView_ );

            /* Table coverage icon. */
            queryCoverageView_ = new CoverageView( "table" );
            queryCoverageView_.setForeground( new Color( 0xff0000 ) );
            queryCoverageView_.setBackground( new Color( 0xffc0c0 ) );
            ActionListener tableListener = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateQueryCoverage();
                }
            };
            tableSelector.addActionListener( tableListener );
            raSelector_.addActionListener( tableListener );
            decSelector_.addActionListener( tableListener );
            srSelector_.addActionListener( tableListener );
            tpanel.add( new ShrinkWrapper( queryCoverageView_ ),
                        BorderLayout.EAST );
            tcListener_ = new TopcatListener() {
                public void modelChanged( TopcatEvent evt ) {
                    if ( evt.getCode() == TopcatEvent.CURRENT_SUBSET ) {
                        updateQueryCoverage();
                    }
                }
            };

            /* Overlap coverage icon. */
            overlapCoverageView_ = new CoverageView( "potential matches" );
            overlapCoverageView_.setForeground( new Color( 0xd000d0 ) );
            overlapCoverageView_.setBackground( new Color( 0xffc0ff ) );
            modeLine.add( new ShrinkWrapper( overlapCoverageView_ ) );
        }
        else {
            tcListener_ = null;
            queryCoverageView_ = null;
            serviceCoverageView_ = null;
            overlapCoverageView_ = null;
        }

        /* Configure coverage display toggle. */
        coverageModel_ =
            new ToggleButtonModel( "Use Service Coverage",
                                   ResourceIcon.FOOTPRINT,
                                   "Use service coverage information (MOCs) "
                                 + "where available to avoid unnecessary "
                                 + "queries" );
        coverageModel_.setSelected( hasCoverage_ );
        coverageModel_.setEnabled( hasCoverage_ );
        coverageModel_.addChangeListener( new ChangeListener() {
            private boolean wasSelected_ = coverageModel_.isSelected();
            public void stateChanged( ChangeEvent evt ) {
                boolean isSelected = coverageModel_.isSelected();
                if ( isSelected ^ wasSelected_ ) {
                    updateServiceCoverage();
                    updateQueryCoverage();
                    updateOverlapCoverage();
                    wasSelected_ = isSelected;
                }
            }
        } );

        /* Service access parameters. */
        int maxpar = ParallelResultRowSequence.getMaxParallelism();
        parallelModel_ =
            new SpinnerNumberModel( Math.min( 3, maxpar ), 1, maxpar, 1 );
        JLabel parallelLabel = new JLabel( "Parallelism: " );
        JSpinner parallelSpinner = new JSpinner( parallelModel_ );
        cList.add( parallelLabel );
        cList.add( parallelSpinner );
        erractSelector_ =
            new JComboBox<ConeErrorPolicy>( getConeErrorPolicies( service ) );
        JLabel erractLabel = new JLabel( ERRACT_LABEL + ": " );
        cList.add( erractLabel );
        cList.add( erractSelector_ );
        Box accessLine = Box.createHorizontalBox();
        accessLine.add( parallelLabel );
        accessLine.add( new ShrinkWrapper( parallelSpinner ) );
        accessLine.add( Box.createHorizontalStrut( 10 ) );
        accessLine.add( erractLabel );
        accessLine.add( new ShrinkWrapper( erractSelector_ ) );
        accessLine.add( Box.createHorizontalGlue() );
        main.add( accessLine );

        /* Action to initiate multicone. */
        startAction_ = new BasicAction( "Go", null,
                                        "Start multiple query running" ) {
            public void actionPerformed( ActionEvent evt ) {
                startMatch();
            }
        };

        /* Action to interrupt processing. */
        stopAction_ = new BasicAction( "Stop", null,
                                       "Interrupt running multiple query; "
                                     + "results will be discarded" ) {
            public void actionPerformed( ActionEvent evt ) {
                setActive( null );
            }
        };

        /* Initialise enabledness of controls etc. */
        components_ = cList.toArray( new JComponent[ 0 ] );
        updateState();
    }

    /**
     * Sets the query service URL.
     *
     * @param url  service access URL
     */
    public void setServiceUrl( String url ) {
        urlField_.setText( url );
        urlField_.setCaretPosition( 0 );
        updateServiceCoverage();
    }

    /**
     * Returns the component in which the URL selector is located.
     *
     * @return  URL selector container
     */
    public JComponent getServiceUrlBox() {
        return urlLine_;
    }

    /**
     * Update the service coverage component if the currently selected
     * service URL has changed.
     */
    private void updateServiceCoverage() {
        if ( hasCoverage_ ) {
            if ( coverageModel_.isSelected() ) {
                URL url = URLUtils.makeURL( urlField_.getText() );
                if ( ( url == null && lastCoverageUrl_ != null ) ||
                     ( url != null && ! url.equals( lastCoverageUrl_ ) ) ) {
                    Coverage cov = url == null ? null
                                               : service_.getCoverage( url );
                    serviceCoverageView_.setCoverage( cov );
                    lastCoverageUrl_ = url;
                    updateOverlapCoverage();
                }
            }
            else {
                serviceCoverageView_.setCoverage( null );
                lastCoverageUrl_ = null;
            }
        }
    }

    /**
     * Update the query coverage component.
     * Call this method if the effective selected table may have changed.
     */
    private void updateQueryCoverage() {
        if ( coverageModel_.isSelected() ) {
            Coverage coverage;
            if ( tcModel_ == null ) {
                coverage = null;
            }
            else {
                int[] rowMap = tcModel_.getViewModel().getRowMap();
                ColumnData raData = raSelector_.getColumnData();
                ColumnData decData = decSelector_.getColumnData();
                ColumnData srData = srSelector_.getColumnData();
                if ( raData != null && decData != null ) {
                    DatasQuerySequenceFactory qsf =
                        new DatasQuerySequenceFactory( raData, decData, srData,
                                                       rowMap );
                    try {
                        StarTable table = tcModel_.getApparentStarTable();
                        ConeQueryRowSequence qseq =
                            qsf.createQuerySequence( table );
                        double resDeg = 1.0;
                        coverage = new ConeQueryCoverage( qseq, resDeg );
                    }
                    catch ( IOException e ) {
                        coverage = null;
                    }
                }
                else {
                    coverage = null;
                }
            }
            queryCoverageView_.setCoverage( coverage );
            updateOverlapCoverage();
        }
        else {
            queryCoverageView_.setCoverage( null );
        }
    }

    /**
     * Update the overlap coverage component.
     * Call this method if the query table coverage, or the service coverage,
     * or both, may have changed.
     */
    private void updateOverlapCoverage() {
        Coverage tcov = queryCoverageView_.getCoverage();
        Coverage scov = serviceCoverageView_.getCoverage();
        Coverage ocov =
            tcov instanceof MocCoverage && scov instanceof MocCoverage
            ? new OverlapCoverage( new MocCoverage[] { (MocCoverage) tcov,
                                                       (MocCoverage) scov } )
            : null;
        overlapCoverageView_.setCoverage( ocov );
    }

    /**
     * Returns a toggle model which controls whether coverage icons
     * are displayed in this panel.
     *
     * @return   coverage display model
     */
    public ToggleButtonModel getCoverageModel() {
        return coverageModel_;
    }

    /**
     * Returns the action which starts a multiple query operation.
     *
     * @return  start action
     */
    public Action getStartAction() {
        return startAction_;
    }

    /**
     * Returns the action which can interrupt a multiple query operation.
     *
     * @return   stop action
     */
    public Action getStopAction() {
        return stopAction_;
    }

    /**
     * Sets the input table whose rows will specify the multiple queries.
     *
     * @param  tcModel   input table
     */
    private void setInputTable( TopcatModel tcModel ) {
        if ( tcModel_ != null && tcListener_ != null ) {
            tcModel_.removeTopcatListener( tcListener_ );
        }
        tcModel_ = tcModel;
        if ( tcModel_ != null && tcListener_ != null ) {
            tcModel_.addTopcatListener( tcListener_ );
        }

        /* Set the column selectors up to select from the correct table. */
        raSelector_.setTable( tcModel );
        decSelector_.setTable( tcModel );

        /* For the size selector, jump through some hoops.
         * Although its value has the same form as for the RA and Dec
         * selectors (value may be a JEL expression or column name),
         * in practice it will often be a constant expression, so 
         * making sure that a default constant value is in place should 
         * make it more obvious for users rather than leaving it blank.
         * Check if the existing value makes sense for the new table;
         * if it does, leave it, otherwise, set it to a sensible default. */
        if ( srSelector_.getModel() == null ) {
            srSelector_.setTable( tcModel );
            service_.setSizeDefault( srSelector_ );
        }
        else {
            String txt = srSelector_.getStringValue();
            Object conv = srSelector_.getModel().getConverterModel()
                                                .getSelectedItem();
            srSelector_.setTable( tcModel );
            ComboBoxModel<ColumnData> srColModel =
                srSelector_.getModel().getColumnModel();
            assert srColModel instanceof ColumnDataComboBoxModel;
            if ( srColModel instanceof ColumnDataComboBoxModel ) {
                try {
                    ((ColumnDataComboBoxModel) srColModel)
                   .stringToColumnData( txt );
                    srSelector_.setStringValue( txt );
                    srSelector_.getModel().getConverterModel()
                                          .setSelectedItem( conv );
                }
                catch ( CompilationException e ) {
                    service_.setSizeDefault( srSelector_ );
                }
            }
        }
    }

    /**
     * Indicates whether the given worker thread is currently working
     * on behalf of this panel.
     *
     * @param  worker  thread to query
     * @return   true iff worker is active
     */
    private boolean isActive( MatchWorker worker ) {
        return matchWorker_ == worker;
    }

    /**
     * Marks a given worker thread as currently working on behalf of
     * this panel.  If null is given, no worker is active.
     * Any existing active thread is interrupted.
     *
     * @param   worker  worker thread
     */
    private void setActive( MatchWorker worker ) {
        if ( matchWorker_ != null && ! matchWorker_.done_ ) {
            matchWorker_.cancel();
        }
        matchWorker_ = worker;
        updateState();
    }

    /**
     * Updates components based on current state.
     * In particular enabledness is set appropriately.
     */
    private void updateState() {
        boolean isActive = matchWorker_ != null;
        startAction_.setEnabled( ! isActive );
        stopAction_.setEnabled( isActive );
        for ( int i = 0; i < components_.length; i++ ) {
            components_[ i ].setEnabled( ! isActive );
        }
        if ( ! isActive && progBar_ != null ) {
            progBar_.setValue( 0 );
            progBar_.setMinimum( 0 );
            progBar_.setMaximum( 1 ); 
            progBar_.setString( " " );
        }
    }

    /**
     * Constructs and returns a worker thread which can perform a match
     * as specified by the current state of this component.
     *
     * @return  new worker thread
     * @throws  RuntimeException   if the current state does not fully
     *          specify a multi query job; such exceptions will have
     *          comprehensible messages
     */
    private MatchWorker createMatchWorker() {

        /* Acquire state from this panel's GUI components. */
        String sUrl = urlField_.getText();
        if ( sUrl == null || sUrl.trim().length() == 0 ) {
            throw new IllegalArgumentException( "No " + service_.getName()
                                              + " URL given" );
        }
        URL serviceUrl;
        try {
            serviceUrl = URLUtils.newURL( sUrl );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad " + service_.getName()
                                              + " URL syntax: " + sUrl )
                 .initCause( e );
        }
        ConeErrorPolicy erract =
            erractSelector_.getItemAt( erractSelector_.getSelectedIndex() );
        TopcatModel tcModel = tcModel_;
        if ( tcModel == null ) {
            throw new NullPointerException( "No table selected" );
        }
        final StarTable inTable = tcModel.getApparentStarTable();
        int[] rowMap = tcModel.getViewModel().getRowMap();
        ColumnData raData = raSelector_.getColumnData();
        ColumnData decData = decSelector_.getColumnData();
        ColumnData srData = srSelector_.getColumnData();
        if ( raData == null ) {
            throw new NullPointerException( "No RA column given" );
        }
        if ( decData == null ) {
            throw new NullPointerException( "No Dec column given" );
        }
        if ( srData == null && ! service_.allowNullSize() ) {
            throw new NullPointerException( "No "
                                          + service_.getSizeInfo().getName()
                                          + " column given" );
        }
        Number parNum = parallelModel_.getNumber();
        int parallelism = parNum == null ? 1 : parNum.intValue();
        MulticoneMode mcMode =
            modeSelector_.getItemAt( modeSelector_.getSelectedIndex() );
        StarTableFactory tfact = ControlWindow.getInstance().getTableFactory();

        /* Assemble objects based on this information. */
        ConeSearcher searcher =
            service_.createSearcher( serviceUrl, tfact, coding_ );
        Coverage coverage = coverageModel_.isSelected()
                          ? service_.getCoverage( serviceUrl )
                          : null;
        DatasQuerySequenceFactory qsf =
            new DatasQuerySequenceFactory( raData, decData, srData, rowMap );
        ConeMatcher matcher =
            mcMode.createConeMatcher( searcher, erract, inTable, qsf, coverage,
                                      parallelism );
        ResultHandler resultHandler =
            mcMode.createResultHandler( this, tfact.getStoragePolicy(),
                                        tcModel, inTable );

        /* Create MatchWorker encapsulating all of this. */
        MatchWorker worker =
            new MatchWorker( matcher, resultHandler, tcModel, inTable );

        /* Perform post-construction configuration of constituent objects
         * as required. */
        qsf.setMatchWorker( worker );
        resultHandler.setMatchWorker( worker ); 

        /* Return worker thread. */
        return worker;
    }

    /**
     * Attempts to start a multicone job.  If insufficient or incorrect
     * information is present in the component state, a popup error message
     * will be delivered instead. 
     */
    private void startMatch() {
        MatchWorker worker;
        try {
            worker = createMatchWorker();
        }
        catch ( RuntimeException e ) {
            Object msg = new String[] {
                "Query not specified:",
                e.getMessage(),
            };
            JOptionPane.showMessageDialog( this, msg, "Incomplete Query",
                                           JOptionPane.ERROR_MESSAGE );
            return;
        }
        setActive( worker );
        worker.start();
    }

    /**
     * Return the available error policies.
     *
     * @return  multicone error handling policy list
     */
    private static ConeErrorPolicy[]
            getConeErrorPolicies( DalMultiService service ) {
        String abortAdvice =
            service.getName()
          + " failed - try non-\"" + ConeErrorPolicy.ABORT + "\" value for "
          + ERRACT_LABEL + "?";
        List<ConeErrorPolicy> plist = new ArrayList<ConeErrorPolicy>();
        plist.add( ConeErrorPolicy
                  .createAdviceAbortPolicy( "abort", abortAdvice ) );
        plist.add( ConeErrorPolicy.IGNORE );
        int[] retries = new int[] { 1, 2, 3, 5, };
        for ( int i = 0; i < retries.length; i++ ) {
            int ntry = retries[ i ];
            plist.add( ConeErrorPolicy
                      .createRetryPolicy( "Retry " + ntry + " times", ntry ) );
        }
        plist.add( ConeErrorPolicy
                  .createRetryPolicy( "Retry indefinitely", 0 ) );
        return plist.toArray( new ConeErrorPolicy[ 0 ] );
    }

    /**
     * Returns the available multicone modes.
     *
     * @return  modes
     */
    private MulticoneMode[] getMulticoneModes() {
        return new MulticoneMode[] {
            new CreateTableMode( "New joined table with best matches",
                                 true, false, service_ ),
            new CreateTableMode( "New joined table with all matches",
                                 false, false, service_ ),
            new CreateTableMode( "New joined table, one row per input row",
                                 true, true, service_ ),
            new AddSubsetMode( "Add subset for matched rows", service_ ),
        };
    }

    /**
     * Turns a table into a TableProducer.
     *
     * @param  table  input table
     * @return   table producer that always produces <code>table</code>
     */
    private static TableProducer toProducer( final StarTable table ) {
        return new TableProducer() {
            public StarTable getTable() {
                return table;
            }
        };
    }

    /**
     * Returns a table which is the same as the input table, but with
     * a zero-based index column as the first column.
     *
     * @param  inTable  input table
     * @return  output table
     */
    private static StarTable prependIndex( StarTable inTable ) {
        final long nrow = inTable.getRowCount();
        ColumnStarTable indexTable = new ColumnStarTable( inTable ) {
            public long getRowCount() {
                return nrow;
            }
        };
        indexTable.addColumn( new ColumnData( INDEX_INFO ) {
            public Object readValue( long irow ) {
                return Integer.valueOf( (int) irow );
            }
        } );
        return new JoinStarTable( new StarTable[] { indexTable, inTable } );
    }

    /**
     * Cone query sequence factory implementation which uses objects acquired
     * from the state of this component to supply the data.
     */
    private static class DatasQuerySequenceFactory
            implements QuerySequenceFactory {

        private final ColumnData raData_;
        private final ColumnData decData_;
        private final ColumnData srData_;
        private final int[] rowMap_;
        private MatchWorker matchWorker_;

        /**
         * Constructor.
         *
         * @param  raData  right ascension data column
         * @param  decData declination data column
         * @param  srData  search radius data column
         * @param  rowMap  mapping from input table rows to column data rows;
         *                 null for unit mapping
         */
        DatasQuerySequenceFactory( ColumnData raData, ColumnData decData,
                                   ColumnData srData, int[] rowMap ) {
            raData_ = raData;
            decData_ = decData;
            srData_ = srData;
            rowMap_ = rowMap;
        }

        /**
         * Sets the worker thread for which this factory will be operating.
         * Must be done before use.
         *
         * @param  worker  worker thread
         */
        void setMatchWorker( MatchWorker matchWorker ) {
            matchWorker_ = matchWorker;
        }

        public ConeQueryRowSequence createQuerySequence( StarTable table )
                throws IOException {
            assert rowMap_ == null || rowMap_.length == table.getRowCount();
            final RowSequence rseq = table.getRowSequence();
            return new ConeQueryRowSequence() {
                long irow_ = -1;

                public boolean next() throws IOException {
                    if ( matchWorker_ != null && matchWorker_.cancelled_ ) {
                        throw new IOException( "Cancelled" );
                    }
                    boolean retval = rseq.next();
                    if ( matchWorker_ != null && matchWorker_.cancelled_ ) {
                        throw new IOException( "Cancelled" );
                    }
                    if ( retval ) {
                        irow_++;
                        if ( matchWorker_ != null ) {
                            matchWorker_.setInputRow( (int) irow_ );
                        }
                    }
                    return retval;
                }

                public Object getCell( int icol ) throws IOException {
                    return rseq.getCell( icol );
                }

                public Object[] getRow() throws IOException {
                    return rseq.getRow();
                }

                public void close() throws IOException {
                    rseq.close();
                }

                public double getRa() throws IOException {
                    return Math.toDegrees( getDoubleValue( raData_ ) );
                }

                public double getDec() throws IOException {
                    return Math.toDegrees( getDoubleValue( decData_ ) );
                }

                public double getRadius() throws IOException {
                    return Math.toDegrees( getDoubleValue( srData_ ) );
                }

                public long getIndex() {
                    return irow_;
                }

                private double getDoubleValue( ColumnData cdata )
                        throws IOException {
                    if ( cdata != null ) {
                        long jrow = rowMap_ == null ? irow_
                                                    : rowMap_[ (int) irow_ ];
                        Object value = cdata.readValue( jrow );
                        return value instanceof Number
                             ? ((Number) value).doubleValue()
                             : Double.NaN;
                    }
                    else {
                        return Double.NaN;
                    }
                }
            };
        }
    }

    /**
     * Thread which performs the multicone work.
     */
    private class MatchWorker extends Thread {

        private final ConeMatcher matcher_;
        private final ResultHandler resultHandler_;
        private final TopcatModel inTcModel_;
        private final StarTable inTable_;
        private final Scheduler scheduler_;
        private int inRow_;
        private int outRow_;
        private volatile Thread coneThread_;
        private volatile boolean done_;
        private volatile boolean cancelled_;

        /**
         * Constructor.
         *
         * @param  matcher  object which knows how to generate a basic
         *                  multicone result
         * @param  resultHandler  object for doing something with the result
         * @param  inTcModel   input TopcatModel
         * @param  inTable  input (apparent) table
         */
        MatchWorker( ConeMatcher matcher, ResultHandler resultHandler,
                     TopcatModel inTcModel, StarTable inTable ) {
            super( "Multi-" + service_.getName() );
            matcher_ = matcher;
            resultHandler_ = resultHandler;
            inTcModel_ = inTcModel;
            inTable_ = inTable;
            scheduler_ = new Scheduler( DalMultiPanel.this ) {
                public boolean isActive() {
                    return DalMultiPanel.this.isActive( MatchWorker.this );
                };
            };
        }

        /**
         * Terminates any activity (computations and queries)
         * associated with this worker.
         */
        public void cancel() {
            cancelled_ = true;
            if ( coneThread_ != null ) {
                coneThread_.interrupt();
            }
        }

        public void run() {

            /* Initialise progress GUI. */
            final int nrow = (int) inTable_.getRowCount();
            scheduler_.schedule( new Runnable() {
                public void run() {
                    progBar_.setMinimum( 0 );
                    progBar_.setMaximum( nrow );
                }
            } );
            setOutputRow( 0 );

            /* Acquire the table from the matcher in such a way that
             * when each row arrives the progress GUI is updated. */
            matcher_.setStreamOutput( true );
            try {
                ConeMatcher.ConeWorker coneWorker = matcher_.createConeWorker();
                coneThread_ = new Thread( coneWorker, "Cone worker" );
                coneThread_.setDaemon( true );
                coneThread_.start();
                StarTable streamTable = coneWorker.getTable();
                StarTable progressTable = new WrapperStarTable( streamTable ) {
                    public RowSequence getRowSequence() throws IOException {
                        return new WrapperRowSequence( super
                                                      .getRowSequence() ) {
                            long irow_ = -1;
                            public boolean next() throws IOException {
                                if ( cancelled_ ) {
                                    throw new IOException( "Cancelled" );
                                }
                                boolean retval = super.next();
                                if ( cancelled_ ) {
                                    throw new IOException( "Cancelled" );
                                }
                                if ( retval ) {
                                    irow_++;
                                    setOutputRow( (int) irow_ );
                                }
                                return retval;
                            }
                        };
                    }
                    public RowSplittable getRowSplittable() throws IOException {
                        return new SequentialRowSplittable( this );
                    }
                };

                /* And pass the table to the appropriate result handler.
                 * The row data has not been acquired yet, it will be pulled
                 * by the action of the processResult call. */
                resultHandler_.processResult( progressTable );
            }

            /* In case of error in result acquisition or processing,
             * inform the user. */
            catch ( final Exception e ) {
                scheduler_.scheduleError( "Multi-" + service_.getName()
                                        + " Error", e );
            }
            catch ( final OutOfMemoryError e ) {
                scheduler_.scheduleMemoryError( e );
            }

            /* In any case, deinstall this worker thread.  No further actions
             * on its behalf will now affect the GUI (important, since another
             * worker thread might take over). */
            finally {
                done_ = true;
                scheduler_.schedule( new Runnable() {
                    public void run() {
                        setActive( null );
                    }
                } );
            }
        }

        /**
         * Informs the GUI that a given input row has been processed.
         *
         * @param  irow  row index (0-based)
         */
        public void setInputRow( int irow ) {
            inRow_ = irow;
            updateProgress();
        }

        /**
         * Informs the GUI that a given output row has been processed.
         *
         * @param  irow  row index (0-based)
         */
        public void setOutputRow( int irow ) {
            outRow_ = irow;
            updateProgress();
        }

        /**
         * Updates the progress bar to display the current I/O row state.
         */
        private void updateProgress() {
            scheduler_.schedule( new Runnable() {
                public void run() {
                    progBar_.setValue( ( inRow_ + 1 ) );
                    progBar_.setString( ( inRow_ ) + " -> "
                                      + ( outRow_ ) );
                }
            } );
        }
    }

    /**
     * Mode defining mainly what happens to the results of the match.
     * It also has some influence on how the match is done.
     */
    private static abstract class MulticoneMode {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  mode name
         */
        MulticoneMode( String name ) {
            name_ = name;
        }

        /**
         * Constructs a ConeMatcher suitable for use with this mode.
         *
         * @param  coneSearcher  cone search implementation
         * @param  errAct   defines action on cone search invocation error
         * @param  inTable  input table
         * @param  qsFact   object which can produce a ConeQueryRowSequence
         *                  from the <code>inTable</code>
         * @param  coverage  coverage of coneSearcher, or null
         * @param  parallelism  number of threads to execute matches
         * @return   new cone matcher
         */
        public abstract ConeMatcher
                createConeMatcher( ConeSearcher coneSearcher,
                                   ConeErrorPolicy errAct, StarTable inTable,
                                   QuerySequenceFactory qsFact,
                                   Coverage coverage, int parallelism );

        /**
         * Constructs a ResultHandler suitable for use with this mode.
         * The results that the handler will be asked to process will be
         * ones that have been generated using the state of the
         * input TopcatModel at the time of calling this method,
         * so for instance it's OK to look at its apparent table.
         *
         * @param  parent  parent component
         * @param  policy   storage policy
         * @param  inTcModel   input topcat model
         * @param  inTable   input table (inTcModel's apparent table)
         * @return  new result handler
         */
        public abstract ResultHandler
                createResultHandler( JComponent parent, StoragePolicy policy,
                                     TopcatModel inTcModel, StarTable inTable );

        public String toString() {
            return name_;
        }
    }

    /**
     * Object which does something with the result of a multicone search.
     */
    private static abstract class ResultHandler {
        private MatchWorker matchWorker_;

        /**
         * Does something with the result of a multicone operation.
         *
         * @param  streamTable  sequential StarTable; its rowSequence
         *                      will only be read once
         */
        public abstract void processResult( StarTable streamTable )
                throws IOException;

        /**
         * Sets the match worker associated with this object.
         * Must be called before use.
         *
         * @param  matchWorker  worker thread
         */
        public void setMatchWorker( MatchWorker matchWorker ) {
            matchWorker_ = matchWorker;
        }

        /**
         * Returns a scheduler which will perform actions on the EDT
         * only as long as this handler's match worker is still active.
         *
         * @return  scheduler
         */
        public Scheduler getScheduler() {
            return matchWorker_.scheduler_;
        }
    }

    /**
     * Partial ResultHandler implementation which first randomises the
     * table and then does something else with it.
     * Concrete subclasses must implement processRandomResult.
     */
    private static abstract class RandomResultHandler extends ResultHandler {
        private final JComponent parent_;
        private final StoragePolicy policy_;
        private final DalMultiService service_;

        /**
         * Constructor.
         *
         * @param   parent  parent component
         * @param   policy   storage policy for randomising table
         */
        RandomResultHandler( JComponent parent, StoragePolicy policy,
                             DalMultiService service ) {
            parent_ = parent;
            policy_ = policy;
            service_ = service;
        }

        public void processResult( StarTable streamTable ) throws IOException {

            /* Blocks until all results are in. */
            StarTable randomTable = policy_.copyTable( streamTable );
            long nrow = randomTable.getRowCount();

            /* Either note that there are no results. */
            if ( nrow == 0 ) {
                getScheduler().scheduleMessage( "No matches were found",
                                                "Empty Match",
                                                JOptionPane.ERROR_MESSAGE );
            }

            /* Or invoke the method that does the work. */
            else {
                processRandomResult( randomTable );
            }
        }

        /**
         * Schedules a given table for adding to the global table list.
         * The user is notified of the new arrival.
         *
         * @param   name   table label
         * @param   table   table to add
         */
        protected void addTable( final String name, final StarTable table ) {
            final ControlWindow controlWindow = ControlWindow.getInstance();
            getScheduler().schedule( new Runnable() {
                public void run() {
                    TopcatModel outTcModel =
                        controlWindow.addTable( table, name, true );
                    String msg = "New table created by multiple "
                               + service_.getName() + ": "
                               + outTcModel + " ("
                               + table.getRowCount() + " rows)";
                    JOptionPane
                   .showMessageDialog( parent_, msg,
                                       "Multi-" + service_.getName()
                                     + " Success",
                                       JOptionPane.INFORMATION_MESSAGE );
                }
            } );
        }

        /**
         * Perform the actual result processing on a table which has been
         * randomised.
         *
         * @param  randomTable  multicone result table for which isRandom()
         *         returns true
         */
        protected abstract void processRandomResult( StarTable randomTable )
                throws IOException;
    }

    /**
     * MulticoneMode implementation which generates a new table consisting
     * of a match between the input table and remote service.
     */
    private static class CreateTableMode extends MulticoneMode {
        private final boolean best_;
        private final boolean includeBlanks_;
        private final DalMultiService service_;

        /**
         * Constructor.
         *
         * @param  name  mode name
         * @param  best  if true, only the best match for each input row
         *               is included in the output (max 1 output row per
         *               input row); if false all matches are included in
         *               output
         * @param  includeBlanks  if true, rows with no matches are included
         *                        int the output
         */
        CreateTableMode( String name, boolean best, boolean includeBlanks,
                         DalMultiService service ) {
            super( name );
            best_ = best;
            includeBlanks_ = includeBlanks;
            service_ = service;
        }

        public ConeMatcher createConeMatcher( ConeSearcher coneSearcher,
                                              ConeErrorPolicy errAct,
                                              StarTable inTable,
                                              QuerySequenceFactory qsFact,
                                              Coverage coverage,
                                              int parallelism ) {
            return
                new ConeMatcher( coneSearcher, errAct, toProducer( inTable ),
                                 qsFact, best_, coverage, includeBlanks_, true,
                                 parallelism, "*", DIST_NAME,
                                 JoinFixAction.NO_ACTION,
                                 JoinFixAction
                                .makeRenameDuplicatesAction( "_" +
                                                             service_
                                                            .getLabel() ) );
        }

        public ResultHandler createResultHandler( final JComponent parent,
                                                  StoragePolicy policy,
                                                  TopcatModel inTcModel,
                                                  StarTable inTable ) {
            final String tname =
                service_.getLabel() + "s(" + inTcModel.getID() + ")";
            return new RandomResultHandler( parent, policy, service_ ) {
                protected void processRandomResult( final StarTable table ) {
                    addTable( tname, table );
                }
            };
        }
    }

    /**
     * Multicone mode which just adds a new subset to the input table
     * marking which rows achieved matches.
     */
    private static class AddSubsetMode extends MulticoneMode {
        private final DalMultiService service_;

        /**
         * Constructor.
         *
         * @param   subset name
         */
        AddSubsetMode( String name, DalMultiService service ) {
            super( name );
            service_ = service;
        }

        public ConeMatcher createConeMatcher( ConeSearcher coneSearcher,
                                              ConeErrorPolicy errAct,
                                              StarTable inTable,
                                              QuerySequenceFactory qsFact,
                                              Coverage coverage,
                                              int parallelism ) {
            return new ConeMatcher( coneSearcher, errAct,
                                    toProducer( prependIndex( inTable ) ),
                                    qsFact, true, coverage, false, true,
                                    parallelism, INDEX_INFO.getName(), null,
                                    JoinFixAction.NO_ACTION,
                                    JoinFixAction.NO_ACTION );
        }

        public ResultHandler
               createResultHandler( final JComponent parent,
                                    StoragePolicy policy,
                                    final TopcatModel inTcModel,
                                    StarTable inTable ) {
            final int[] rowMap = inTcModel.getViewModel().getRowMap();
            return new ResultHandler() {
                public void processResult( StarTable streamTable )
                        throws IOException {

                    /* Prepare mask containing matched rows. */
                    RowSequence rseq = streamTable.getRowSequence();
                    final BitSet matchMask = new BitSet();
                    try {
                        while ( rseq.next() ) {
                            long irow =
                                ((Number) rseq.getCell( 0 )).longValue();
                            if ( irow < Integer.MAX_VALUE ) {
                                int jrow = rowMap == null
                                         ? (int) irow
                                         : rowMap[ (int) irow ];
                                matchMask.set( jrow );
                            }
                        }
                    }
                    finally {
                        rseq.close();
                    }

                    /* With user guidance, turn these into a subset. */
                    int nmatch = matchMask.cardinality();
                    final String[] msgLines = new String[] {
                        "Multiple " + service_.getName() + " successful; " +
                        "matches found for " + nmatch + " rows.",
                        " ",
                        "Define new subset for matched rows",
                    };
                    final String dfltName = "multi" + service_.getLabel();
                    final String title =
                        "Multi-" + service_.getName() + " Success";
                    getScheduler().schedule( new Runnable() {
                        public void run() {
                            TopcatUtils.addSubset( parent, inTcModel, matchMask,
                                                   dfltName, msgLines, title );
                        }
                    } );
                }
            };
        }
    }
}
