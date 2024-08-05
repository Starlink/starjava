package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;
import uk.ac.starlink.topcat.AlignedBox;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.Scheduler;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.BlockUploader;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.CoverageQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.ServiceFindMode;
import uk.ac.starlink.ttools.cone.UploadMatcher;
import uk.ac.starlink.ttools.cone.WrapperQuerySequence;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.Downloader;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.DoubleValueField;

/**
 * Panel that allows the user to specify and execute an upload-match
 * operation using the CDS X-Match service.
 *
 * @author   Mark Taylor
 */
public class UploadMatchPanel extends JPanel {

    private final JProgressBar progBar_;
    private final CdsTableSelector cdsTableSelector_;
    private final ColumnSelector raSelector_;
    private final ColumnSelector decSelector_;
    private final DoubleValueField srField_;
    private final JComboBox<String> blockSelector_;
    private final JComboBox<UploadFindMode> modeSelector_;
    private final JoinFixSelector fixSelector_;
    private final JComponent[] components_;
    private final Action startAction_;
    private final Action stopAction_;
    private final ToggleButtonModel coverageModel_;
    private final Downloader<CdsUploadMatcher.VizierMeta> metaDownloader_;
    private final ContentCoding coding_;
    private TopcatModel tcModel_;
    private volatile MatchWorker matchWorker_;

    private static final long MAXREC = -1;
    private static final int[] BLOCK_SIZES =
        { 100, 1000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, };
    private static final int DEFAULT_BLOCKSIZE = 50000;
    private static final ValueInfo SR_INFO =
        new DefaultValueInfo( "Radius", Double.class, "Search Radius" );

    /**
     * Constructor.
     *
     * @param   progBar  progress bar that this component may use to indicate
     *                   progress of matches
     */
    @SuppressWarnings("this-escape")
    public UploadMatchPanel( JProgressBar progBar ) {
        super( new BorderLayout() );
        coding_ = ContentCoding.GZIP;
        progBar_ = progBar;
        progBar_.setStringPainted( true );
        JComponent main = AlignedBox.createVerticalBox();
        List<JComponent> cList = new ArrayList<JComponent>();
        add( main );

        /* Field for remote table. */
        cdsTableSelector_ = new CdsTableSelector();
        cdsTableSelector_.setBorder(
            BorderFactory.createCompoundBorder(
                AuxWindow.makeTitledBorder( "Remote Table" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        cList.add( cdsTableSelector_ );

        /* Make sure the Start button is only enabled when there is metadata
         * for the table.  This should be an indicator of whether a legal
         * table name is selected. */
        metaDownloader_ = cdsTableSelector_.getMetadataDownloader();
        metaDownloader_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateState();
            }
        } );

        /* Containers for different input fields. */
        JComponent localBox = AlignedBox.createVerticalBox();
        JComponent paramBox = Box.createVerticalBox();
        localBox.setBorder(
            BorderFactory.createCompoundBorder(
                AuxWindow.makeTitledBorder( "Local Table" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        paramBox.setBorder(
            BorderFactory.createCompoundBorder(
                AuxWindow.makeTitledBorder( "Match Parameters" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        main.add( cdsTableSelector_ );
        main.add( Box.createVerticalStrut( 5 ) );
        main.add( localBox );
        main.add( Box.createVerticalStrut( 5 ) );
        main.add( paramBox );

        /* Field for input table. */
        final JComboBox<TopcatModel> tableSelector =
            new TablesListComboBox( 200 );
        tableSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setInputTable( tableSelector
                              .getItemAt( tableSelector.getSelectedIndex() ) );
            }
        } );
        cList.add( tableSelector );
        Box tableLine = Box.createHorizontalBox();
        tableLine.add( new JLabel( "Input Table: " ) );
        tableLine.add( tableSelector );
        localBox.add( tableLine );
        localBox.add( Box.createVerticalStrut( 5 ) );

        /* Fields for sky position parameters. */
        raSelector_ = new ColumnSelector( Tables.RA_INFO, true );
        Box raLine = Box.createHorizontalBox();
        raLine.add( raSelector_ );
        raLine.add( new JLabel( " (J2000)" ) );
        raLine.add( Box.createHorizontalGlue() );
        localBox.add( raLine );
        localBox.add( Box.createVerticalStrut( 5 ) );
        cList.add( raSelector_ );
        decSelector_ = new ColumnSelector( Tables.DEC_INFO, true );
        Box decLine = Box.createHorizontalBox();
        decLine.add( decSelector_ );
        decLine.add( new JLabel( " (J2000)" ) );
        decLine.add( Box.createHorizontalGlue() );
        localBox.add( decLine );
        localBox.add( Box.createVerticalStrut( 5 ) );
        cList.add( decSelector_ );
        TopcatUtils.alignComponents( new JComponent[] {
            raSelector_.getLabel(),
            decSelector_.getLabel(),
        } );
        TopcatUtils.alignComponents( new JComponent[] {
            raSelector_.getColumnComponent(),
            decSelector_.getColumnComponent(),
        } );
        TopcatUtils.alignComponents( new JComponent[] {
            raSelector_.getUnitComponent(),
            decSelector_.getUnitComponent(),
        } );

        /* Search radius field. */
        srField_ = DoubleValueField.makeSizeDegreesField( SR_INFO );
        Box srLine = Box.createHorizontalBox();
        srLine.add( srField_.getLabel() );
        srLine.add( Box.createHorizontalStrut( 5 ) );
        srLine.add( srField_.getEntryField() );
        srLine.add( Box.createHorizontalStrut( 5 ) );
        srLine.add( new ShrinkWrapper( srField_.getConverterSelector() ) );
        srLine.add( Box.createHorizontalGlue() );
        srField_.getConverterSelector().setSelectedIndex( 2 );
        srField_.getEntryField().setText( "1.0" );
        paramBox.add( srLine );
        paramBox.add( Box.createVerticalStrut( 5 ) );
        cList.add( srField_.getEntryField() );
        cList.add( srField_.getConverterSelector() );

        /* Find mode selector. */
        Box modeLine = Box.createHorizontalBox();
        modeSelector_ = new JComboBox<>( UploadFindMode.getInstances() );
        modeLine.add( new JLabel( "Find mode: " ) );
        modeLine.add( new ShrinkWrapper( modeSelector_ ) );
        modeLine.add( Box.createHorizontalGlue() );
        cList.add( modeSelector_ );
        paramBox.add( modeLine );
        paramBox.add( Box.createVerticalStrut( 5 ) );

        /* Column deduplication selector. */
        Box fixLine = Box.createHorizontalBox();
        fixSelector_ = new JoinFixSelector();
        fixSelector_.getSuffixField().setText( "_x" );
        fixLine.add( new JLabel( "Rename columns: " ) );
        fixLine.add( fixSelector_ );
        fixLine.add( Box.createHorizontalGlue() );
        cList.add( fixSelector_ );
        paramBox.add( fixLine );
        paramBox.add( Box.createVerticalStrut( 5 ) );

        /* Block size selector. */
        Box blockLine = Box.createHorizontalBox();
        blockSelector_ = new JComboBox<String>();
        for ( int i = 0; i < BLOCK_SIZES.length; i++ ) {
            blockSelector_.addItem( Integer.toString( BLOCK_SIZES[ i ] ) );
        }
        blockSelector_.setSelectedItem( Integer.toString( DEFAULT_BLOCKSIZE ) );
        blockSelector_.setEditable( true );
        blockLine.add( new JLabel( "Block size: " ) );
        blockLine.add( new ShrinkWrapper( blockSelector_ ) );
        blockLine.add( Box.createHorizontalStrut( 5 ) );
        blockLine.add( new ComboBoxBumper( blockSelector_ ) );
        blockLine.add( Box.createHorizontalGlue() );
        cList.add( blockSelector_ );
        paramBox.add( blockLine );

        /* Actions to start/stop match. */
        startAction_ = new BasicAction( "Go", null,
                                        "Start upload crossmatch running" ) {
            public void actionPerformed( ActionEvent evt ) {
                startMatch();
            }
        };
        stopAction_ = new BasicAction( "Stop", null,
                                       "Interrupt running crossmatch; "
                                     + "results will be discarded" ) {
            public void actionPerformed( ActionEvent evt ) {
                setActive( null );
            }
        };

        /* Configure coverage display toggle. */
        coverageModel_ =
            new ToggleButtonModel( "Use Service Coverage",
                                   ResourceIcon.FOOTPRINT,
                                   "Use service coverage information (MOCs) "
                                 + "where available to avoid unnecessary "
                                 + "queries" );
        coverageModel_.setSelected( true );

        /* Initialise enabledness of controls etc. */
        components_ = cList.toArray( new JComponent[ 0 ] );
        updateState();
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
     * Returns a toggle model which controls whether coverage icons
     * are displayed in this panel.
     *
     * @return   coverage display model
     */
    public ToggleButtonModel getCoverageModel() {
        return coverageModel_;
    }

    /**
     * Attempts to start a match job.  If insufficient or incorrect
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
        new Thread( worker, "Upload Matcher" ).start();
    }

    /**
     * Sets the input table whose rows will specify the multiple queries.
     *
     * @param  tcModel   input table
     */
    private void setInputTable( TopcatModel tcModel ) {
        tcModel_ = tcModel;
        raSelector_.setTable( tcModel );
        decSelector_.setTable( tcModel );
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
        matchWorker_ = worker;
        updateState();
    }

    /**
     * Updates components based on current state.
     * In particular enabledness is set appropriately.
     */
    private void updateState() {
        boolean isActive = matchWorker_ != null;
        boolean hasMeta = metaDownloader_.getData() != null;
        startAction_.setEnabled( ! isActive && hasMeta );
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
     * Creates a MatchWorker instance based on the current state of
     * this panel.
     *
     * @return  match worker
     */
    private MatchWorker createMatchWorker() {

        /* Get input table and column data. */
        TopcatModel tcModel = tcModel_;
        if ( tcModel == null ) {
            throw new NullPointerException( "No table selected" );
        }
        int[] rowMap = tcModel.getViewModel().getRowMap();
        ColumnData raData = raSelector_.getColumnData();
        ColumnData decData = decSelector_.getColumnData();
        if ( raData == null ) {
            throw new NullPointerException( "No RA column given" );
        }
        if ( decData == null ) {
            throw new NullPointerException( "No Dec column given" );
        }

        /* Get search radius. */
        double srDeg = srField_.getValue();
        if ( ! ( srDeg > 0 ) ) {
            srField_.getEntryField().setText( "1.0" );
            throw new IllegalArgumentException( "Bad search radius value" );
        }

        /* Get remote table identifier. */
        String cdsName = cdsTableSelector_.getTableName();
        String cdsId = CdsUploadMatcher.toCdsId( cdsName );
        if ( cdsId == null ) {
            throw new IllegalArgumentException( "Bad remote table name \""
                                              + cdsName + "\"" );
        }
        Coverage serviceCoverage = cdsTableSelector_.getCoverage();

        /* Get other search parameters. */
        Object bf = blockSelector_.getSelectedItem();
        String bfTxt = bf == null ? null : bf.toString();
        int blocksize;
        try {
            blocksize = Integer.parseInt( bfTxt );
        }
        catch ( NumberFormatException e ) {
            blockSelector_.setSelectedItem( null );
            throw new IllegalArgumentException( "Bad blocksize: " + bfTxt );
        }
        UploadFindMode upMode =
            modeSelector_.getItemAt( modeSelector_.getSelectedIndex() );
        ServiceFindMode serviceMode = upMode.getServiceMode();
        long maxrec = MAXREC;
        String surl = CdsUploadMatcher.XMATCH_URL;
        final URL url;
        try {
            url = URLUtils.newURL( surl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Bad URL: " + surl );
        }

        /* Prepare objects to do the match. */
        QuerySequenceFactory qsFact =
            new DataQuerySequenceFactory( raData, decData, srDeg, rowMap );
        if ( serviceCoverage != null && coverageModel_.isSelected() ) {
            qsFact = new CoverageQuerySequenceFactory( qsFact,
                                                       serviceCoverage );
        }
        UploadMatcher umatcher =
            new CdsUploadMatcher( url, cdsId, srDeg * 3600., serviceMode,
                                  coding_ );
        StoragePolicy storage =
            ControlWindow.getInstance().getTableFactory().getStoragePolicy();
        String outName = tcModel.getID() + "x" + cdsName;
        JoinFixAction inFixAct = JoinFixAction.NO_ACTION;
        JoinFixAction cdsFixAct = fixSelector_.getJoinFixAction();
        boolean oneToOne = upMode.isOneToOne();
        boolean uploadEmpty = CdsUploadMatcher.UPLOAD_EMPTY;
        BlockUploader blocker =
            new BlockUploader( umatcher, blocksize, maxrec, outName,
                               inFixAct, cdsFixAct, serviceMode, oneToOne,
                               uploadEmpty );

        /* Create and return the match worker. */
        return new MatchWorker( blocker, upMode, tcModel, qsFact, storage );
    }

    /**
     * QuerySequenceFactory implementation that acquires data from
     * supplied ColumnData objects.
     */
    private static class DataQuerySequenceFactory
            implements QuerySequenceFactory {

        private final ColumnData raData_;
        private final ColumnData decData_;
        private final double srDeg_;
        private final int[] rowMap_;

        /**
         * Constructor.
         *
         * @param  raData   column data for right ascension in radians (sorry)
         * @param  decData  column data for declination in radians
         * @param  srDeg   constant search radius in degrees
         */
        DataQuerySequenceFactory( ColumnData raData, ColumnData decData,
                                  double srDeg, int[] rowMap ) {
            raData_ = raData;
            decData_ = decData;
            srDeg_ = srDeg;
            rowMap_ = rowMap;
        }

        public ConeQueryRowSequence createQuerySequence( StarTable table )
                throws IOException {
            assert rowMap_ == null || rowMap_.length == table.getRowCount();
            final RowSequence rseq = table.getRowSequence();
            return new ConeQueryRowSequence() { 
                long irow_ = -1;
                public boolean next() throws IOException {
                    boolean hasNext = rseq.next();
                    if ( hasNext ) {
                        irow_++;
                    }
                    return hasNext;
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
                public double getRadius() {
                    return srDeg_;
                }
                public long getIndex() {
                    return irow_;
                }

                /**
                 * Gets a numeric value from a given column data object
                 * corresponding to this sequence's current row.
                 *
                 * @param  cdata  numeric column data
                 * @return   value at current row
                 */
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
     * Runnable that knows how to perform a particular upload match operation.
     */
    private class MatchWorker implements Runnable {
        private final BlockUploader blocker_;
        private final UploadFindMode upMode_;
        private final TopcatModel tcModel_;
        private final QuerySequenceFactory qsFact_;
        private final StoragePolicy storage_;
        private final StarTable inTable_;
        private final int[] rowMap_;
        private final Scheduler scheduler_;
        private long inRow_;
        private long outRow_;

        /**
         * Constructor.
         *
         * @param  blocker  block uploader
         * @param  upMode   match mode
         * @param  tcModel  topcat model supplying input table
         * @param  qsFact   object for obtaining a sequence of positional
         *                  queries from the input table
         * @param  storage  policy for storing retrieved results
         */
        MatchWorker( BlockUploader blocker, UploadFindMode upMode,
                     TopcatModel tcModel, QuerySequenceFactory qsFact,
                     StoragePolicy storage ) {
            blocker_ = blocker;
            upMode_ = upMode;
            tcModel_ = tcModel;
            qsFact_ = qsFact;
            storage_ = storage;
            inTable_ = tcModel.getApparentStarTable();
            rowMap_ = tcModel.getViewModel().getRowMap();
            scheduler_ = new Scheduler( UploadMatchPanel.this ) {
                public boolean isActive() {
                    return MatchWorker.this.isActive();
                }
            };
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

            /* Manage input table progress. */
            final QuerySequenceFactory progQsFact = new QuerySequenceFactory() {
                public ConeQueryRowSequence createQuerySequence( StarTable t )
                        throws IOException {
                    return new WrapperQuerySequence(
                                       qsFact_.createQuerySequence( t ) ) {
                        public boolean next() throws IOException {
                            boolean hasNext = isActive() && super.next();
                            if ( hasNext ) {
                                inRow_ = getIndex();
                            }
                            return hasNext;
                        }
                    };
                }
            };

            /* Manage output table progress. */
            final MonitorStoragePolicy[] progStorageHolder =
                new MonitorStoragePolicy[ 1 ];
            MonitorStoragePolicy progStorage =
                    new MonitorStoragePolicy( storage_, new TableSink() {
                long irow = 0;
                public void acceptMetadata( StarTable table ) {
                }
                public void acceptRow( Object[] row ) {
                    if ( isActive() ) {
                        outRow_ = ++irow;
                    }
                    else {
                        progStorageHolder[ 0 ].interrupt();
                    }
                }
                public void endRows() {
                }
            } );
            progStorageHolder[ 0 ] = progStorage;

            /* Update progress bar periodically while the match is running. */
            final Timer progTimer = new Timer( 100, new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    if ( isActive() ) {
                        progBar_.setValue( (int) inRow_ );
                        progBar_.setString( inRow_ + " -> " + outRow_ );
                    }
                } 
            } );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progTimer.start();
                }
            } );

            /* Run the match. */
            try {
                upMode_.runMatch( blocker_, inTable_, progQsFact, progStorage,
                                  scheduler_, tcModel_, rowMap_ );
            }

            /* When completed, successfully or otherwise, deinstall
             * this worker thread.  No further actions on its behalf
             * will now affect the GUI (important, since another
             * worker thread might take over). */
            finally {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        progTimer.stop();
                    }
                } );
                scheduler_.schedule( new Runnable() {
                    public void run() {
                        setActive( null );
                    }
                } );
            }
        }

        /**
         * Indicates whether this match worker is currently running on
         * behalf of this UploadMatchPanel.  It is guaranteed that
         * only one match worker will be active at once, and once inactive
         * it will never reactivate.
         *
         * @return  true iff this match worker is still active
         */
        private boolean isActive() {
            return UploadMatchPanel.this.isActive( MatchWorker.this );
        }
    }
}
