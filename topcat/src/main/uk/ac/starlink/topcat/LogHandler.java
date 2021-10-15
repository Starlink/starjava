package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.task.LineFormatter;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Log handler which can provide a window displaying recent log events.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Aug 2021
 */
public class LogHandler extends Handler {

    private final LogRing ring_;
    private LogWindow logWindow_;

    /** Maximum number of log records retained. */
    public static final int RING_SIZE = 1000;

    private static LogHandler instance_;
    private static final String CNAME_SEQ = "Seq";
    private static final String CNAME_TIME = "Time";
    private static final String CNAME_LEVEL = "Level";
    private static final String CNAME_LOGGER = "Source";
    private static final String CNAME_MESSAGE = "Message";
    private static List<ArrayTableColumn<IndexedLogRecord,?>> LOG_COLS =
        createLogCols();
    private static final Shader LOGGER_SHADER = 
        Shaders.createInterpolationShader( "LogInfo", new Color[] {
            new Color( 0x9999ff ),
            new Color( 0x00cc00 ),
            new Color( 0xcc9900 ),
            new Color( 0xcc66ff ),
        } );

    /**
     * Constructor.  Invoked lazily by {@link #getInstance}.
     */
    protected LogHandler() {
        ring_ = new LogRing( RING_SIZE );
    }

    public void publish( LogRecord record ) {
        ring_.add( record );
        if ( logWindow_ != null && logWindow_.isVisible() ) {
            SwingUtilities.invokeLater( logWindow_::updateGui );
        }
    }

    public void flush() {
    }

    public void close() {
    }

    /**
     * Maps log records to display colours.
     *
     * @param  record  log record
     * @return   display colour
     */
    public Color getRecordColor( LogRecord record ) {
        Level level = record.getLevel();
        if ( level.intValue() >= Level.SEVERE.intValue() ) {
            return Color.BLACK;
        }
        else if ( level.intValue() >= Level.WARNING.intValue() ) {
            return Color.RED;
        }
        else {
            int loggerCode = record.getLoggerName().hashCode();
            float[] rgb = new float[ 4 ];
            LOGGER_SHADER.adjustRgba( rgb, ( loggerCode & 0xff ) / 255f );
            return new Color( rgb[ 0 ], rgb[ 1 ], rgb[ 2 ] );
        }
    }

    /**
     * Displays a logging window which displays recent (the last 1000) and
     * any future log messages.
     *
     * @param  parent  parent component, may be used for positioning
     */
    public void showWindow( Component parent ) {
        if ( logWindow_ == null ) {
            logWindow_ = new LogWindow( parent, ring_, this::getRecordColor );
            logWindow_.updateGui();
            logWindow_.configureWidths();
        }
        logWindow_.makeVisible();
    }

    /**
     * Returns the standard instance of this class.
     *
     * @return   singleton handler
     */
    public static LogHandler getInstance() {
        if ( instance_ == null ) {
            instance_ = new LogHandler();
        }
        return instance_;
    }

    /**
     * Sets up the columns for the LogRecord display table.
     *
     * @return  array of columns for display
     */
    private static List<ArrayTableColumn<IndexedLogRecord,?>> createLogCols() {
        List<ArrayTableColumn<IndexedLogRecord,?>> list = new ArrayList<>();
        list.add( new ArrayTableColumn<IndexedLogRecord,Integer>
                                      ( CNAME_SEQ, Integer.class ) {
            public Integer getValue( IndexedLogRecord record ) {
                return Integer.valueOf( 1 + record.index_ );
            }
        } );
        list.addAll( Arrays.asList(
            createStringCol( CNAME_TIME,
                             r -> new SimpleDateFormat( "HH:mm:ss" )
                                 .format( new Date( r.getMillis() ) ) ),
            createStringCol( CNAME_LEVEL, r -> r.getLevel().toString() ),
            createStringCol( CNAME_LOGGER, LogRecord::getLoggerName ),
            createStringCol( CNAME_MESSAGE, LogRecord::getMessage )
        ) );
        return list;
    }

    /**
     * Utility method to create a table column for displaying some
     * aspect of a LogRecord as a string.
     *
     * @param  name  column name
     * @param  extractor   mapping from LogRecord to string value
     * @return  column definition
     */
    private static ArrayTableColumn<IndexedLogRecord,String>
                   createStringCol( String name,
                                    Function<LogRecord,String> extractor ) {
        return new ArrayTableColumn<IndexedLogRecord,String>
                                   ( name, String.class ) {
            public String getValue( IndexedLogRecord ixRec ) {
                return extractor.apply( ixRec.record_ );
            }
        };
    }

    /**
     * Writes log records to an external file.
     *
     * @param  records  records
     * @param  file   destination file
     */
    private static void exportRecords( IndexedLogRecord[] records, File file )
            throws IOException {
        LineFormatter formatter = new LineFormatter( true );
        try ( BufferedWriter writer =
                  new BufferedWriter(
                      new OutputStreamWriter( new FileOutputStream( file ),
                                              StandardCharsets.UTF_8 ) ) ) {
            for ( IndexedLogRecord irec : records ) {
                writer.write( irec.index_ + ": "
                            + formatter.format( irec.record_ ) );
            }
        }
    }

    /**
     * Aggregates a LogRecord and a unique sequence ID.
     */
    private static class IndexedLogRecord {
        final int index_;
        final LogRecord record_;

        /**
         * Constructor.
         * @param  index  unique ID
         * @param  record  record
         */
        IndexedLogRecord( int index, LogRecord record ) {
            index_ = index;
            record_ = record;
        }
    }

    /**
     * Fixed length ring buffer for storing LogRecords.
     * Older ones are purged if too many new ones are added.
     */
    private static class LogRing {

        private final LogRecord[] records_;
        private int nrec_;

        /**
         * Constructs a buffer with a given maximum size.
         *
         * @param  limit  maximum record storage count
         */
        LogRing( int limit ) {
            records_ = new LogRecord[ limit ];
        }

        /**
         * Constructs a buffer with a default maximum size.
         */
        LogRing() {
            this( 1000 );
        }

        /**
         * Adds a LogRecord to this buffer.
         * A sequence number is associated with it for later reference.
         *
         * @param  rec  record to add
         */
        public synchronized void add( LogRecord rec ) {
            records_[ nrec_++ % records_.length ] = rec;
        }

        /**
         * Returns the actual number of records currently stored
         * in this buffer.
         *
         * @return   current record count
         */
        public int size() {
            return Math.min( records_.length, nrec_ );
        }

        /**
         * Returns the sequence index of the earliest record currently
         * stored in this buffer.
         *
         * @return  index of earliest retained record
         */
        public int getLowestIndex() {
            return Math.max( 0, nrec_ - records_.length );
        }

        /**
         * Returns a record by sequence number.
         *
         * @param  ix  sequence number
         * @throws  IllegalArgumentException if requested record is not,
         *          or is no longer, present in this buffer
         */
        public LogRecord getRecord( int ix ) {
            if ( ix >= 0 && ix < nrec_ ) {
                return records_[ ix % records_.length ];
            }
            else {
                throw new IllegalArgumentException( "No entry " + ix );
            }
        }

        /**
         * Resets this buffer to the empty state.
         * All records are removed and the sequence index is reset.
         */
        public void clear() {
            nrec_ = 0;
        }

        /**
         * Copies the content of this buffer to an array of indexed records,
         * optionally filtered by a given predicate.
         *
         * @param  filter  filter for records to include
         * @return  array of indexed records
         */
        public synchronized IndexedLogRecord[]
                toIndexedRecordArray( Predicate<LogRecord> filter ) {
            int ir0 = getLowestIndex();
            int nr = nrec_;
            List<IndexedLogRecord> list = new ArrayList<>( nr - ir0 );
            for ( int ir = ir0; ir < nr; ir++ ) {
                LogRecord rec = getRecord( ir );
                if ( filter.test( rec ) ) {
                    list.add( new IndexedLogRecord( ir, rec ) );
                }
            }
            return list.toArray( new IndexedLogRecord[ 0 ] );
        }
    }

    /**
     * Window that can display log records.
     */
    private static class LogWindow extends AuxWindow {

        private final ArrayTableModel<IndexedLogRecord> model_;
        private final LogRing ring_;
        private final JTable jtable_;
        private final JScrollPane scroller_;
        private final JComboBox<Level> levelSelector_;
        private final ToggleButtonModel pauseModel_;
        private final ToggleButtonModel scrollModel_;
        private int lastNrec_;
        private int lastIrec0_;
        private Level lastLevel_;

        /**
         * Constructor.
         *
         * @param  parent  parent window, may be used for positioning
         * @param  ring   ring buffer
         * @param  colorMapper  defines log record display colours
         */
        public LogWindow( Component parent, LogRing ring,
                          Function<LogRecord,Color> colorMapper ) {
            super( "Log Messages", parent );
            ring_ = ring;

            /* Set up the table model. */
            model_ = new ArrayTableModel<IndexedLogRecord>
                                        ( new IndexedLogRecord[ 0 ] ) {

                /* Flag cells as editable as a quick hack to allow the
                 * cell contents to be selected for copy/paste.
                 * Actual editing of the values doesn't do anything.
                 */
                @Override
                public boolean isCellEditable( int irow, int icol ) {
                    return true;
                }
            };
            model_.setColumns( LOG_COLS );

            /* Set up the display table. */
            jtable_ = new JTable( model_ );
            jtable_.setCellSelectionEnabled( true );
            for ( Class<?> clazz :
                  new Class<?>[] { String.class, Integer.class } ) {
                final TableCellRenderer baseRend =
                   jtable_.getDefaultRenderer( clazz );
                jtable_.setDefaultRenderer( clazz, new TableCellRenderer() {
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSel,
                            boolean hasFocus, int irow, int icol ) {
                        Component comp =
                            baseRend
                           .getTableCellRendererComponent( table, value,
                                                           isSel, hasFocus,
                                                           irow, icol );
                        LogRecord record = model_.getItems()[ irow ].record_;
                        comp.setForeground( colorMapper.apply( record ) );
                        return comp;
                    }
                } );
            }
            StarJTable.alignHeadersLeft( jtable_ );

            /* Place the table within a scroll panel. */
            scroller_ = new JScrollPane( jtable_ );
            scroller_.setPreferredSize( new Dimension( 700, 300 ) );
            scroller_.setVerticalScrollBarPolicy(
                          ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
            scroller_.setHorizontalScrollBarPolicy(
                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS );
            JComponent main = getMainArea();
            main.setLayout( new BorderLayout() );
            main.add( scroller_ );

            /* Add a selector for display filtering by log level. */
            levelSelector_ = new JComboBox<Level>();
            levelSelector_.addItem( Level.CONFIG );
            levelSelector_.addItem( Level.INFO );
            levelSelector_.addItem( Level.WARNING );
            levelSelector_.setSelectedItem( Level.INFO );
            JComponent levelBox = Box.createHorizontalBox();
            levelBox.add( Box.createHorizontalGlue() );
            levelBox.add( new JLabel( "Log Level: " ) );
            levelBox.add( new ShrinkWrapper( levelSelector_ ) );
            levelBox.add( Box.createHorizontalStrut( 5 ) );
            levelBox.add( new ComboBoxBumper( levelSelector_ ) );
            levelBox.add( Box.createHorizontalGlue() );
            levelSelector_.addItemListener( evt -> updateGui() );
            getControlPanel().add( levelBox );

            /* Action to clear the window. */
            Action clearAct =
                    new BasicAction( "Clear", ResourceIcon.CLEAR,
                                     "Clear existing messages from log" ) {
                public void actionPerformed( ActionEvent evt ) {
                    ring_.clear();
                    updateGui();
                }
            };

            /* Action to pause updates. */
            pauseModel_ =
                new ToggleButtonModel( "Pause logging", ResourceIcon.PAUSE,
                                       "Prevents display being updated"
                                     + " with new messages" );
            pauseModel_.addActionListener( evt -> {
                if ( ! pauseModel_.isSelected() ) {
                    updateGui();
                }
            } );

            /* Action to toggle scrolling. */
            scrollModel_ =
                new ToggleButtonModel( "Auto-scroll", ResourceIcon.SCROLL,
                                       "Automatically scrolls"
                                     + " to latest update" );
            scrollModel_.setSelected( true );
            scrollModel_.addActionListener( evt -> {
                if ( scrollModel_.isSelected() ) {
                    updateGui();
                }
            } );

            /* Action to dump to file. */
            Action exportAct =
                    new BasicAction( "Export to File", ResourceIcon.SAVE,
                                     "Dump log messages to text file" ) {
                public void actionPerformed( ActionEvent evt ) {
                    IndexedLogRecord[] records =
                        ring_.toIndexedRecordArray( r -> true );
                    JFileChooser chooser = new JFileChooser();
                    int status = chooser.showSaveDialog( LogWindow.this );
                    if ( status == JFileChooser.APPROVE_OPTION ) {
                        File file = chooser.getSelectedFile();
                        try {
                            exportRecords( records, file );
                        }
                        catch ( IOException e ) {
                            ErrorDialog.showError( LogWindow.this,
                                                   "Export Error", e );
                        }
                    }
                }
            };

            /* Add actions to toolbar. */
            getToolBar().add( clearAct );
            getToolBar().add( scrollModel_.createToolbarButton() );
            getToolBar().add( pauseModel_.createToolbarButton() );
            getToolBar().addSeparator();
            getToolBar().add( exportAct );

            /* Add actions to menu. */
            JMenu logMenu = new JMenu( "Logging" );
            logMenu.add( new JMenuItem( clearAct ) );
            logMenu.add( scrollModel_.createMenuItem() );
            logMenu.add( pauseModel_.createMenuItem() );
            logMenu.addSeparator();
            logMenu.add( new JMenuItem( exportAct ) );
            getJMenuBar().add( logMenu );

            /* Complete window initialisation. */
            getToolBar().addSeparator();
            addHelp( "LogWindow" );
        }

        @Override
        public void setVisible( boolean isVis ) {
            super.setVisible( isVis );
            if ( isVis ) {
                updateGui();
            }
        }

        /**
         * Configures column widths.  Should be called when some
         * representative content is present in the table.
         */
        void configureWidths() {
            StarJTable.configureColumnWidths( jtable_, 1000, 1000 );
            TableColumn loggerCol = jtable_.getColumn( CNAME_LOGGER );
            TableColumn timeCol = jtable_.getColumn( CNAME_TIME );
            TableColumn levelCol = jtable_.getColumn( CNAME_LEVEL );
            assert loggerCol != null && timeCol != null && levelCol != null;
            if ( loggerCol != null && timeCol != null ) {
                loggerCol.setPreferredWidth( timeCol.getPreferredWidth() * 3 );
            }
            if ( levelCol != null ) {
                levelCol
               .setPreferredWidth( (int) (levelCol.getPreferredWidth() * 1.3) );
            }
        }

        /**
         * Updates the display according to the current state of the
         * ring buffer.
         * This method must be called in order to reflect buffer changes
         * in the window.
         * This method must be called on the Event Dispatch Thread.
         */
        public void updateGui() {
            if ( pauseModel_.isSelected() ) {
                return;
            }
            Level level = (Level) levelSelector_.getSelectedItem();

            /* Synchronize on the ring buffer, since it may be updated in
             * threads other than the EDT.  So we want to minimise the
             * amount of time spent here.  In particular, don't do any
             * work if the state hasn't changed since last time. */
            synchronized ( ring_ ) {
                if ( lastNrec_ != ring_.nrec_ ||
                     lastIrec0_ != ring_.getLowestIndex() ||
                     ! level.equals( lastLevel_ ) ) {
                    int nrec = ring_.nrec_;
                    int irec0 = ring_.getLowestIndex();

                    /* Take steps to preserve row selections over a complete
                     * TableModel change. */
                    int[] isels = jtable_.getSelectedRows();
                    final Collection<Integer> ixSels;
                    if ( isels.length > 0 ) {
                        IndexedLogRecord[] items = model_.getItems();
                        ixSels = new HashSet<Integer>();
                        for ( int isel : isels ) {
                            ixSels.add( Integer
                                       .valueOf( items[ isel ].index_ ) );
                        }
                    }
                    else {
                        ixSels = null;
                    }

                    /* Prepare an array of IndexedRecords to serve as the
                     * basis of the TableModel. */
                    final int filterThresh = level.intValue();
                    Predicate<LogRecord> filter =
                        rec -> rec.getLevel().intValue() >= filterThresh;
                    model_.setItems( ring_.toIndexedRecordArray( filter ) );

                    /* Restore the row selections. */
                    if ( ixSels != null ) {
                        IndexedLogRecord[] items = model_.getItems();
                        ListSelectionModel selModel =
                            jtable_.getSelectionModel();
                        selModel.setValueIsAdjusting( true );
                        selModel.clearSelection();
                        for ( int i = 0; i < items.length; i++ ) {
                            if ( ixSels
                                .contains( Integer
                                          .valueOf( items[ i ].index_ ) ) ) {
                                selModel.addSelectionInterval( i, i );
                            }
                        }
                        selModel.setValueIsAdjusting( false );
                    }

                    /* Update the state. */
                    lastNrec_ = nrec;
                    lastIrec0_ = irec0;
                    lastLevel_ = level;
                }
            }
            if ( scrollModel_.isSelected() ) {
                JScrollBar vbar = scroller_.getVerticalScrollBar();
                vbar.setValue( vbar.getMaximum() );
            }
        }
    }
}
