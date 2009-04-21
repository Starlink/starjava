package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.TablesListComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.cone.ConeErrorPolicy;
import uk.ac.starlink.ttools.cone.ConeMatcher;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.ServiceConeSearcher;
import uk.ac.starlink.ttools.task.TableProducer;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.ConeSearch;

/**
 * Component for performing a multicone join between a selected input table
 * and a cone search service.
 *
 * @author   Mark Taylor
 * @since    21 Apr 2009
 */
public class MulticonePanel extends JPanel {

    private final JTextField urlField_;
    private final ColumnSelector raSelector_;
    private final ColumnSelector decSelector_;
    private final ColumnSelector srSelector_;
    private final BestSelector bestSelector_;
    private final SpinnerNumberModel parallelModel_;
    private final JComboBox erractSelector_;
    private final JProgressBar progBar_;
    private final Action startAction_;
    private final Action stopAction_;
    private final JComponent[] components_;
    private TopcatModel tcModel_;
    private ConeSearch coneSearch_;
    private MatchWorker matchWorker_;
    private int inRow_;
    private int outRow_;

    /** Metadata for search radius field. */
    private static final DefaultValueInfo SR_INFO =
        new DefaultValueInfo( "Search Radius", Number.class,
                              "Maximum distance from target for match" );
    static {
        SR_INFO.setUnitString( "radians" );
        SR_INFO.setUCD( "pos.angDistance" );
    }

    /**
     * Constructor.
     *
     * @param  progBar  progress bar for progress display
     */
    public MulticonePanel( JProgressBar progBar ) {
        super( new BorderLayout() );
        progBar_ = progBar;
        progBar_.setStringPainted( true );
        JComponent main = Box.createVerticalBox();
        List cList = new ArrayList();
        add( main );

        /* Field for cone search service URL. */
        urlField_ = new JTextField();
        urlField_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    coneSearch_ = new ConeSearch( urlField_.getText() );
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( MulticonePanel.this, "Bad URL", e,
                                           "Bad Cone Search URL" );
                }
            }
        } );
        JLabel urlLabel = new JLabel( "Cone Search URL: " );
        Box urlLine = Box.createHorizontalBox();
        cList.add( urlField_ );
        cList.add( urlLabel );
        urlLine.add( urlLabel );
        urlLine.add( urlField_ );
        main.add( urlLine );
        main.add( Box.createVerticalStrut( 10 ) );

        /* Field for input table. */
        final JComboBox tableSelector =
            new JComboBox( new TablesListComboBoxModel() );
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
        tableLine.add( tableSelector );
        tableLine.add( Box.createHorizontalGlue() );
        main.add( tableLine );
        main.add( Box.createVerticalStrut( 5 ) );

        /* Fields for cone search parameters (RA, Dec, radius). */
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
        srSelector_ = new ColumnSelector( SR_INFO, true );
        Box srLine = Box.createHorizontalBox();
        srLine.add( srSelector_ );
        JLabel srSysLabel = new JLabel( "" );
        srLine.add( srSysLabel );
        srLine.add( Box.createHorizontalGlue() );
        main.add( srLine );
        cList.add( srSelector_ );
        cList.add( srSysLabel );
        main.add( Box.createVerticalStrut( 10 ) );

        /* Align the value fields. */
        alignLabels( new JLabel[] { raSelector_.getLabel(),
                                    decSelector_.getLabel(),
                                    srSelector_.getLabel(), } );
        alignLabels( new JLabel[] { raSysLabel, decSysLabel, srSysLabel, } );

        /* Best/All output selector. */
        bestSelector_ = new BestSelector();
        cList.add( bestSelector_ );
        Box bestLine = Box.createHorizontalBox();
        bestLine.add( bestSelector_ );
        bestLine.add( Box.createHorizontalGlue() );
        main.add( bestLine );

        /* Service access parameters. */
        parallelModel_ = new SpinnerNumberModel( 5, 1, 1000, 1 );
        JLabel parallelLabel = new JLabel( "Parallelism: " );
        JSpinner parallelSpinner = new JSpinner( parallelModel_ );
        cList.add( parallelLabel );
        cList.add( parallelSpinner );
        erractSelector_ = new JComboBox( getConeErrorPolicies() );
        JLabel erractLabel = new JLabel( "Error Handling: " );
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
                                        "Start multicone running" ) {
            public void actionPerformed( ActionEvent evt ) {
                startMatch();
            }
        };

        /* Action to interrupt processing. */
        stopAction_ = new BasicAction( "Stop", null,
                                       "Interrupt running multicone; "
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
     * Sets the cone search service URL.
     *
     * @param   url  cone search base URL
     */
    public void setServiceUrl( String url ) {
        urlField_.setText( url );
        urlField_.setCaretPosition( 0 );
    }

    /**
     * Returns the action which starts a multicone operation.
     *
     * @return  start action
     */
    public Action getStartAction() {
        return startAction_;
    }

    /**
     * Returns the action which can interrupt a multicone operation.
     *
     * @return  stop action
     */
    public Action getStopAction() {
        return stopAction_;
    }

    /**
     * Sets the input table whose rows will form the multicone queries.
     *
     * @param  tcModel   input table
     */
    private void setInputTable( TopcatModel tcModel ) {
        tcModel_ = tcModel;

        /* Set the column selectors up for selecting from the correct table. */
        raSelector_.setTable( tcModel );
        decSelector_.setTable( tcModel );
        srSelector_.setTable( tcModel );

        /* Jump through some hoops to get a numeric default for the radius
         * selector.  Although formally this is just the same as the RA and Dec
         * selectors (may be JEL expression or column name), in practice it
         * will often be a number, so putting an example number in should
         * make it more obvious for users.  Unfortunately it's a bit fiddly. */
        String srtxt = srSelector_.getStringValue();
        if ( srtxt != null ) {
            try {
                Double.parseDouble( srtxt );
            }
            catch ( RuntimeException e ) {
                srtxt = null;
            }
        }
        if ( srtxt == null ) {
            srtxt = "1.0";
        }
        if ( srSelector_.getStringValue() == null ||
             srSelector_.getStringValue().trim().length() == 0 ) {
            srSelector_.setStringValue( srtxt );
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
        if ( ! isActive ) {
            progBar_.setValue( 0 );
            progBar_.setMinimum( 0 );
            progBar_.setMaximum( 1 );
            progBar_.setString( " " );
        }
    }

    /**
     * Constructs and returns a worker thread which will perform a match
     * as specified by the current state of this component.
     *
     * @return  new worker thread
     * @throws  RuntimeException   if the current state does not fully
     *          specify a multicone job; such exceptions will have 
     *          comprehansible messages
     */
    private MatchWorker createMatchWorker() {
        String coneUrl = urlField_.getText();
        if ( coneUrl == null ) {
            throw new NullPointerException( "No cone search"
                                          + " service URL given" );
        }
        ConeSearch csearch = new ConeSearch( urlField_.getText() );
        if ( csearch == null ) {
            throw new NullPointerException( "No cone search URL selected" );
        }
        int verbose = -1;
        StarTableFactory tfact = ControlWindow.getInstance().getTableFactory();
        ConeSearcher searcher =
            new ServiceConeSearcher( csearch, verbose, false, tfact );
        ConeErrorPolicy erract =
            (ConeErrorPolicy) erractSelector_.getSelectedItem();
        searcher = erract.adjustConeSearcher( searcher );
        TopcatModel tcModel = tcModel_;
        if ( tcModel == null ) {
            throw new NullPointerException( "No table selected" );
        }
        final StarTable inTable = tcModel.getApparentStarTable();
        TableProducer inProd = new TableProducer() {
            public StarTable getTable() {
                return inTable;
            }
        };
        ColumnData raData = raSelector_.getColumnData();
        ColumnData decData = decSelector_.getColumnData();
        ColumnData srData = srSelector_.getColumnData();
        if ( raData == null ) {
            throw new NullPointerException( "No RA column given" );
        }
        if ( decData == null ) {
            throw new NullPointerException( "No Dec column given" );
        }
        if ( srData == null ) {
            throw new NullPointerException( "No Search Radius"
                                          + " (column or constant) given" );
        }
        DatasQuerySequenceFactory qsf =
            new DatasQuerySequenceFactory( raData, decData, srData, inTable );
        boolean best = bestSelector_.isBest();
        Number parNum = parallelModel_.getNumber();
        int parallelism = parNum == null ? 1 : parNum.intValue();
        String copycolIdList = "*";
        String distanceCol = "Separation";
        JoinFixAction inFixAct = JoinFixAction.NO_ACTION;
        JoinFixAction coneFixAct =
            JoinFixAction.makeRenameDuplicatesAction( "_cone" );
        ConeMatcher matcher =
            new ConeMatcher( searcher, inProd, qsf, best, parallelism,
                             copycolIdList, distanceCol, inFixAct, coneFixAct );
        MatchWorker worker = new MatchWorker( matcher, tcModel, inTable );
        qsf.setMatchWorker( worker );
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
    private static ConeErrorPolicy[] getConeErrorPolicies() {
        List plist = new ArrayList();
        plist.add( ConeErrorPolicy.IGNORE );
        plist.add( ConeErrorPolicy.ABORT );
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
     * Reshapes a set of components so that they all have the same 
     * preferred size (that of the largest one).
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
     * Cone query sequence factory implementation which uses the 
     * current state of this component to supply the data.
     */
    private static class DatasQuerySequenceFactory
            implements QuerySequenceFactory {

        private final ColumnData raData_;
        private final ColumnData decData_;
        private final ColumnData srData_;
        private final StarTable inTable_;
        private MatchWorker matchWorker_;

        /**
         * Constructor.
         *
         * @param  raData  right ascension data column
         * @param  decData declination data column
         * @param  srData  search radius data column
         * @param  inTable input table
         */
        DatasQuerySequenceFactory( ColumnData raData, ColumnData decData,
                                   ColumnData srData, StarTable inTable ) {
            raData_ = raData;
            decData_ = decData;
            srData_ = srData;
            inTable_ = inTable;
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
            assert table == inTable_;
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
                    Object value = cdata.readValue( irow_ );
                    return value instanceof Number
                         ? ((Number) value).doubleValue()
                         : Double.NaN;
                }
            };
        }
    }

    /**
     * Thread which performs the multicone work.
     */
    private class MatchWorker extends Thread {

        private final ConeMatcher matcher_;
        private final TopcatModel inTcModel_;
        private final StarTable inTable_;
        private boolean done_;

        /**
         * Constructor.
         *
         * @param  matcher  cone matcher defining match parameters
         * @param  inTcModel   input TopcatModel
         * @param  inTable  input (apparent) table
         */
        MatchWorker( ConeMatcher matcher, TopcatModel inTcModel,
                     StarTable inTable ) {
            super( "MultiCone" );
            matcher_ = matcher;
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
            StoragePolicy storagePolicy =
                ControlWindow.getInstance().getTableFactory()
                                           .getStoragePolicy();
            StarTable outTable;
            Exception error;
            try {
                StarTable streamTable = matcher_.getTable();
                StarTable progressTable =
                        new WrapperStarTable( streamTable ) {
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
                assert ! progressTable.isRandom();

                /* This step will block until all the rows have
                 *  been acquired. */
                outTable = storagePolicy.copyTable( progressTable );
                error = null;
            }
            catch ( Exception e ) {
                outTable = null;
                error = e;
            }
            final StarTable outTable0 = outTable; 
            final Exception error0 = error;
            done_ = true;


            /* Pass the result, success or failure, to the GUI. */
            schedule( new Runnable() {
                public void run() {
                    if ( outTable0 != null ) {
                        gotTable( outTable0 );
                    }
                    else {
                        ErrorDialog.showError( MulticonePanel.this,
                                               "Multicone Error", error0 );
                    }
                    setActive( null );
                }
            } );
        }

        /**
         * Called in the event dispatch thread when a successful result
         * table has been obtained.
         *
         * @param  result  result table (random access)
         */
        private void gotTable( StarTable result ) {
            assert result.isRandom();
            long nrow = result.getRowCount();
            if ( nrow == 0 ) {
                JOptionPane.showMessageDialog( MulticonePanel.this,
                                               "No matches were found",
                                               "Empty Match",
                                               JOptionPane.ERROR_MESSAGE );
            }
            else {
                String tname = "cones(" + inTcModel_.getID() + ")";
                TopcatModel outTcModel = ControlWindow.getInstance()
                                        .addTable( result, tname, true );
                JOptionPane.showMessageDialog( MulticonePanel.this,
                                               "New table created by "
                                             + "multicone: " + outTcModel
                                             + " (" + nrow + " rows)" );
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
                    progBar_.setString( ( inRow_ + 1 ) + " -> "
                                      + ( outRow_ + 1 ) );
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
}
