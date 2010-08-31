package uk.ac.starlink.topcat.join;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InterruptedIOException;
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
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ColumnSelectorModel;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.ConeErrorPolicy;
import uk.ac.starlink.ttools.cone.ConeMatcher;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.task.TableProducer;
import uk.ac.starlink.util.gui.ErrorDialog;
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
    private final JProgressBar progBar_;
    private final JTextField urlField_;
    private final ColumnSelector raSelector_;
    private final ColumnSelector decSelector_;
    private final ColumnSelector srSelector_;
    private final JComboBox modeSelector_;
    private final SpinnerNumberModel parallelModel_;
    private final JComboBox erractSelector_;
    private final Action startAction_;
    private final Action stopAction_;
    private final JComponent[] components_;
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
    public DalMultiPanel( DalMultiService service, JProgressBar progBar ) {
        super( new BorderLayout() );
        service_ = service;
        progBar_ = progBar;
        progBar.setStringPainted( true );
        JComponent main = Box.createVerticalBox();
        List cList = new ArrayList();
        add( main );

        /* Field for service URL. */
        urlField_ = new JTextField();
        JLabel urlLabel = new JLabel( service.getName() + " URL: " );
        Box urlLine = Box.createHorizontalBox();
        cList.add( urlField_ );
        cList.add( urlLabel );
        urlLine.add( urlLabel );
        urlLine.add( urlField_ );
        main.add( urlLine );
        main.add( Box.createVerticalStrut( 10 ) );

        /* Field for input table. */
        final JComboBox tableSelector = new TablesListComboBox();
        tableSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setInputTable( (TopcatModel) tableSelector.getSelectedItem() );
            }
        } );
        JLabel tableLabel = new JLabel( "Input Table: " );
        cList.add( tableLabel );
        cList.add( tableSelector );
        Box tableLine = Box.createHorizontalBox();
        tableLine.add( tableLabel );
        tableLine.add( new ShrinkWrapper( tableSelector ) );
        tableLine.add( Box.createHorizontalGlue() );
        main.add( tableLine );
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
        alignLabels( new JLabel[] { raSelector_.getLabel(),
                                    decSelector_.getLabel(),
                                    srSelector_.getLabel(), } );
        alignLabels( new JLabel[] { raSysLabel, decSysLabel, srSysLabel, } );

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
        modeSelector_ = new JComboBox( getMulticoneModes() );
        Box modeLine = Box.createHorizontalBox();
        JLabel modeLabel = new JLabel( "Output Mode: " );
        modeLine.add( modeLabel );
        modeLine.add( new ShrinkWrapper( modeSelector_ ) );
        modeLine.add( Box.createHorizontalGlue() );
        cList.add( modeLabel );
        cList.add( modeSelector_ );
        main.add( modeLine );
        main.add( Box.createVerticalStrut( 5 ) );

        /* Service access parameters. */
        parallelModel_ = new SpinnerNumberModel( 5, 1, 1000, 1 );
        JLabel parallelLabel = new JLabel( "Parallelism: " );
        JSpinner parallelSpinner = new JSpinner( parallelModel_ );
        cList.add( parallelLabel );
        cList.add( parallelSpinner );
        erractSelector_ = new JComboBox( getConeErrorPolicies( service ) );
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
        components_ = (JComponent[]) cList.toArray( new JComponent[ 0 ] );
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
        tcModel_ = tcModel;

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
            ComboBoxModel srColModel = srSelector_.getModel().getColumnModel();
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
            matchWorker_.interrupt();
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
        String serviceUrl = urlField_.getText();
        if ( serviceUrl == null || serviceUrl.trim().length() == 0 ) {
            throw new IllegalArgumentException( "No " + service_.getName()
                                              + " URL given" );
        }
        try {
            new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad " + service_.getName()
                                              + " URL syntax: " + serviceUrl )
                 .initCause( e );
        }
        ConeErrorPolicy erract =
            (ConeErrorPolicy) erractSelector_.getSelectedItem();
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
        Number parNum = parallelModel_.getNumber();
        int parallelism = parNum == null ? 1 : parNum.intValue();
        MulticoneMode mcMode = (MulticoneMode) modeSelector_.getSelectedItem();
        StarTableFactory tfact = ControlWindow.getInstance().getTableFactory();

        /* Assemble objects based on this information. */
        ConeSearcher searcher = service_.createSearcher( serviceUrl, tfact );
        searcher = erract.adjustConeSearcher( searcher );
        DatasQuerySequenceFactory qsf =
            new DatasQuerySequenceFactory( raData, decData, srData, rowMap );
        ConeMatcher matcher =
            mcMode.createConeMatcher( searcher, inTable, qsf, parallelism );
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
        List plist = new ArrayList();
        plist.add( ConeErrorPolicy
                  .addAdvice( ConeErrorPolicy.ABORT, abortAdvice ) );
        plist.add( ConeErrorPolicy.IGNORE );
        int[] retries = new int[] { 1, 2, 3, 5, 10, };
        for ( int i = 0; i < retries.length; i++ ) {
            int ntry = retries[ i ];
            plist.add( ConeErrorPolicy
                      .createRetryPolicy( "Retry " + ntry + " times", ntry ) );
        }
        plist.add( ConeErrorPolicy
                  .createRetryPolicy( "Retry indefinitely", 0 ) );
        return (ConeErrorPolicy[]) plist.toArray( new ConeErrorPolicy[ 0 ] );
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
     * Reshapes a set of components so that they all have the same
     * preferred size (that of the largest one).
     *
     * @param   labels  labels to align
     */
    private static void alignLabels( JLabel[] labels ) {
        int maxw = 0;
        int maxh = 0;
        for ( int i = 0; i < labels.length; i++ ) {
            Dimension prefSize = labels[ i ].getPreferredSize();
            maxw = Math.max( maxw, prefSize.width );
            maxh = Math.max( maxh, prefSize.height );
        }
        Dimension prefSize = new Dimension( maxw, maxh );
        for ( int i = 0; i < labels.length; i++ ) {
            labels[ i ].setPreferredSize( prefSize );
        }
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
                return new Integer( (int) irow );
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
                    if ( matchWorker_.isInterrupted() ) {
                        throw new InterruptedIOException();
                    }
                    boolean retval = rseq.next();
                    if ( matchWorker_.isInterrupted() ) {
                        throw new InterruptedIOException();
                    }
                    if ( retval ) {
                        irow_++;
                        matchWorker_.setInputRow( (int) irow_ );
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
        private int inRow_;
        private int outRow_;
        private boolean done_;

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
        }

        public void run() {

            /* Initialise progress GUI. */
            final int nrow = (int) inTable_.getRowCount();
            schedule( new Runnable() {
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
                StarTable streamTable = matcher_.getTable();
                StarTable progressTable = new WrapperStarTable( streamTable ) {
                    public RowSequence getRowSequence() throws IOException {
                        return new WrapperRowSequence( super
                                                      .getRowSequence() ) {
                            long irow_ = -1;
                            public boolean next() throws IOException {
                                if ( isInterrupted() ) {
                                    throw new InterruptedIOException();
                                }
                                boolean retval = super.next();
                                if ( isInterrupted() ) {
                                    throw new InterruptedIOException();
                                }
                                if ( retval ) {
                                    irow_++;
                                    setOutputRow( (int) irow_ );
                                }
                                return retval;
                            }
                        };
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
                schedule( new Runnable() {
                    public void run() {
                        ErrorDialog.showError( DalMultiPanel.this,
                                               "Multi-" + service_.getName()
                                             + " Error", e );
                    }
                } );
            }
            catch ( final OutOfMemoryError e ) {
                schedule( new Runnable() {
                    public void run() {
                        TopcatUtils.memoryError( e );
                    }
                } );
            }

            /* In any case, deinstall this worker thread.  No further actions
             * on its behalf will now affect the GUI (important, since another
             * worker thread might take over). */
            finally {
                done_ = true;
                schedule( new Runnable() {
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
            schedule( new Runnable() {
                public void run() {
                    progBar_.setValue( ( inRow_ + 1 ) );
                    progBar_.setString( ( inRow_ ) + " -> "
                                      + ( outRow_ ) );
                }
            } );
        }

        /**
         * Schedules a runnable to execute on the event dispatch thread,
         * as long as this worker is still the active one.
         * If this worker has been deinstalled, the event is discarded.
         */
        private void schedule( final Runnable runnable ) {
            if ( isActive( MatchWorker.this ) ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( isActive( MatchWorker.this ) ) {
                            runnable.run();
                        }
                    }
                } );
            }
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
         * @param  inTable  input table
         * @param  qsFact   object which can produce a ConeQueryRowSequence
         *                  from the <code>inTable</code>
         * @param  parallelism  number of threads to execute matches
         * @return   new cone matcher
         */
        public abstract ConeMatcher
                createConeMatcher( ConeSearcher coneSearcher,
                                   StarTable inTable,
                                   QuerySequenceFactory qsFact,
                                   int parallelism );

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
         * Schedules a runnable on the event dispatch thread, which will
         * execute only if this handler's match worker is still active.
         *
         * @param  runnable  object to run
         */
        protected void schedule( Runnable runnable ) {
            matchWorker_.schedule( runnable );
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
                schedule( new Runnable() {
                    public void run() {
                        JOptionPane
                       .showMessageDialog( parent_, "No matches were found",
                                           "Empty Match",
                                           JOptionPane.ERROR_MESSAGE );
                    }
                } );
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
            schedule( new Runnable() {
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
                                              StarTable inTable,
                                              QuerySequenceFactory qsFact,
                                              int parallelism ) {
            return
                new ConeMatcher( coneSearcher, toProducer( inTable ), qsFact,
                                 best_, includeBlanks_, true, parallelism, "*",
                                 DIST_NAME, JoinFixAction.NO_ACTION,
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
                                              StarTable inTable,
                                              QuerySequenceFactory qsFact,
                                              int parallelism ) {
            return new ConeMatcher( coneSearcher,
                                    toProducer( prependIndex( inTable ) ),
                                    qsFact, true, false, true, parallelism,
                                    INDEX_INFO.getName(), null,
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
                    schedule( new Runnable() {
                        public void run() {
                            addSubset( parent, inTcModel, matchMask );
                        }
                    } );
                }

                /**
                 * Using input from the user, adds a new (or reused) Row Subset
                 * to the given TopcatModel based on a given BitSet.
                 *
                 * @param  parent   parent component
                 * @param  tcModel   topcat model
                 * @param  matchMask  mask for included rows
                 */
                public void addSubset( JComponent parent, TopcatModel tcModel,
                                       BitSet matchMask ) {
                    int nmatch = matchMask.cardinality();
                    Box nameLine = Box.createHorizontalBox();
                    JComboBox nameSelector =
                         tcModel.createNewSubsetNameSelector();
                    nameSelector.setSelectedItem( "multi"
                                                + service_.getLabel() );
                    nameLine.add( new JLabel( "Subset name: " ) );
                    nameLine.add( nameSelector );
                    Object msg = new Object[] {
                        "Multiple " + service_.getName() + " successful; " +
                        "matches found for " + nmatch + " rows.",
                        " ",
                        "Define new subset for matched rows",
                        nameLine,
                    };
                    int opt =
                        JOptionPane.showOptionDialog(
                             parent, msg,
                             "Multi-" + service_.getName() + " Success",
                             JOptionPane.OK_CANCEL_OPTION,
                             JOptionPane.QUESTION_MESSAGE, null, null, null );
                    String name = getSubsetName( nameSelector );
                    if ( opt == JOptionPane.OK_OPTION && name != null ) {
                        tcModel.addSubset( new BitsRowSubset( name,
                                                              matchMask ) );
                    }
                }

                /**
                 * Returns the subset name corresponding to the currently
                 * selected value of a row subset selector box.
                 *
                 * @param rsetSelector  combo box returned by
                 *        TopcatModel.createNewSubsetNameSelector
                 * @return   subset name as string, or null
                 */
                private String getSubsetName( JComboBox rsetSelector ) {
                    Object item = rsetSelector.getSelectedItem();
                    if ( item == null ) {
                        return null;
                    }
                    else if ( item instanceof String ) {
                        String name = (String) item;
                        return name.trim().length() > 0 ? name : null;
                    }
                    else if ( item instanceof RowSubset ) {
                        return ((RowSubset) item).getName();
                    }
                    else {
                        assert false;
                        return item.toString();
                    }
                }
            };
        }
    }
}
