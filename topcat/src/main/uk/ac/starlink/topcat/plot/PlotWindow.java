package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.OverlayLayout;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.jibble.epsgraphics.EpsGraphics2D;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.CheckBoxMenu;
import uk.ac.starlink.topcat.CheckBoxStack;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatViewWindow;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Top level window which presents plots derived from a <tt>StarTable</tt>.
 * A number of plot configuration options are available to be configured
 * interactively by the user.
 *
 * @author   Mark Taylor (Starlink)
 */
public class PlotWindow extends TopcatViewWindow 
                        implements ActionListener, ListSelectionListener,
                                   ItemListener, SurfaceListener {

    private final TopcatModel tcModel_;
    private final OptionsListModel subsets_;
    private final ScatterPlot plot_;
    private final BlobPanel blobPanel_;
    private final JComboBox xColBox_;
    private final JComboBox yColBox_;
    private final JCheckBox xLogBox_;
    private final JCheckBox yLogBox_;
    private final ListSelectionModel subSelModel_;
    private final ButtonModel gridModel_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final Action fromvisibleAction_;
    private final Action blobAction_;
    private JFileChooser printSaver_;
    private BitSet visibleRows_;
    private PointRegistry visiblePoints_;
    private MarkStyleProfile markers_ = MARKER_PROFILES[ 1 ];
    private boolean activeBlob_;
    private boolean replotted_;

    private static final double MILLISECONDS_PER_YEAR
                              = 365.25 * 24 * 60 * 60 * 1000;
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    private static MarkStyleProfile[] MARKER_PROFILES = new MarkStyleProfile[] {
        MarkStyleProfile.spots( "Pixels", 0 ),
        MarkStyleProfile.spots( "Dots", 1 ),
        MarkStyleProfile.spots( "Spots", 2 ),
        MarkStyleProfile.filledShapes( "Coloured Shapes", 3, null ),
        MarkStyleProfile.filledShapes( "Black Shapes", 3, Color.black ),
        MarkStyleProfile.openShapes( "Coloured Outlines", 3, null ),
        MarkStyleProfile.openShapes( "Black Outlines", 3, Color.black ),
        MarkStyleProfile.ghosts( "Faint Transparent Pixels", 0, 0.1f ),
        MarkStyleProfile.ghosts( "Medium Transparent Pixels", 0, 0.4f ),
        MarkStyleProfile.ghosts( "Faint Transparent Dots", 1, 0.1f ),
        MarkStyleProfile.ghosts( "Medium Transparent Dots", 1, 0.4f ),
    };

    /**
     * Constructs a new PlotWindow.
     *
     * @param  tcModel  data model whose data the window will plot
     * @param  parent   parent component (may be used for positioning)
     */
    public PlotWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, "Table Plotter", parent );
        tcModel_ = tcModel;
        subsets_ = tcModel_.getSubsets();

        /* Construct a panel for configuration of X and Y axes. */
        JPanel xConfig = new JPanel();
        JPanel yConfig = new JPanel();
        xConfig.setBorder( makeTitledBorder( "X axis" ) );
        yConfig.setBorder( makeTitledBorder( "Y axis" ) );
        Box axisBox = new Box( BoxLayout.Y_AXIS );
        axisBox.add( xConfig );
        axisBox.add( Box.createGlue() );
        axisBox.add( yConfig );
        getControlPanel().add( axisBox );

        /* Construct axis selectors for X and Y. */
        xColBox_ = makePlottableColumnComboBox();
        yColBox_ = makePlottableColumnComboBox();

        /* If there are too few numeric columns then inform the user and
         * bail out. */ 
        int numCols = xColBox_.getItemCount();
        assert numCols == yColBox_.getItemCount();
        if ( numCols < 2 ) {
            JOptionPane.showMessageDialog( null,
                                           "Too few numeric columns in table",
                                           "Plot error", 
                                           JOptionPane.ERROR_MESSAGE );
            dispose();
        }

        /* Place the selectors in this window. */
        xConfig.add( xColBox_ );
        yConfig.add( yColBox_ );
        xColBox_.setSelectedIndex( 0 );
        yColBox_.setSelectedIndex( 1 );
        xColBox_.addActionListener( this );
        yColBox_.addActionListener( this );

        /* Add linear/log selectors for X and Y. */
        xLogBox_ = new JCheckBox( "Log plot" );
        yLogBox_ = new JCheckBox( "Log plot" );
        xConfig.add( xLogBox_ );
        yConfig.add( yLogBox_ );
        xLogBox_.addActionListener( this );
        yLogBox_.addActionListener( this );

        /* Get a menu for selecting row subsets to plot. */
        CheckBoxMenu subMenu = subsets_.makeCheckBoxMenu( "Subsets to plot" );
        subSelModel_ = subMenu.getSelectionModel();

        /* Do the same thing as a scrollable box in the control panel. */
        CheckBoxStack stack = new CheckBoxStack( subsets_ );
        JComponent stackPanel = new JScrollPane( stack );
        stackPanel.setBorder( makeTitledBorder( "Row subsets" ) );
        getControlPanel().add( stackPanel );
        stack.setSelectionModel( subSelModel_ );

        /* Initialise its selections so that both ALL and the apparent table's
         * current subset if any, are plotted. */
        subSelModel_.addSelectionInterval( 0, 0 );  // ALL
        int nrsets = subsets_.size();
        RowSubset currentSet = tcModel_.getSelectedSubset();
        if ( currentSet != RowSubset.ALL ) {
            for ( int i = 1; i < nrsets; i++ ) {
                if ( subsets_.get( i ) == currentSet ) {
                    subSelModel_.addSelectionInterval( i, i );
                }
            }
        }

        /* Maintain a list of selected subsets updated from this model. 
         * This cannot be worked out from the model on request, since the
         * order in which selections have been made is significant, and
         * is not preserved by the model. */
        subSelRecorder_ = new OrderedSelectionRecorder( subSelModel_ );
        subSelModel_.addListSelectionListener( this );
        subSelModel_.addListSelectionListener( subSelRecorder_ );

        /* Construct a panel which will hold the plot itself. */

        /* Construct the plot component.  The paint method is
         * overridden so that when the points are replotted we maintain
         * a record of their current positions.  The Swing tutorial 
         * generally recommends against overriding paint itself 
         * (normally one should override paintComponent) but since all 
         * we're doing here is invoking the superclass implementation
         * and some non-graphics-related stuff, it should be OK. 
         * Overriding paintComponent would be no good, since it needs 
         * to be called following paintChildren. */
        plot_ = new ScatterPlot( new PtPlotSurface( this ) ) {
            int lastHeight_;
            int lastWidth_;
            public void paint( Graphics g ) {
                super.paint( g );
                int height = getHeight();
                int width = getWidth();
                if ( replotted_ || height != lastHeight_
                                || width != lastWidth_ ) {
                    recordVisiblePoints( getState(), getPoints(),
                                         getSurface() );
                    lastHeight_ = height;
                    lastWidth_ = width;
                    replotted_ = false;
                }
            }
        };

        /* Construct and populate the plot panel with the plot itself
         * and a transparent layer for doodling blobs on. */
        JPanel plotPanel = new JPanel();
        blobPanel_ = new BlobPanel();
        blobPanel_.setVisible( false );
        plotPanel.setLayout( new OverlayLayout( plotPanel ) );
        plotPanel.add( blobPanel_ );
        plotPanel.add( plot_ );
        plotPanel.setPreferredSize( new Dimension( 500, 300 ) );

        /* Listen for point-clicking events on the plot. */
        /* I have to reach right in to find the plot surface component to
         * add the mouse listener to it; it would be tidier to just add
         * the listener to the plot component itself, but that doesn't
         * receive the mouse events, since it's not the deepest visible
         * component.  Doing it this way is probably easier than mucking
         * about with glassPanes. */
        plot_.getSurface().getComponent()
             .addMouseListener( new PointClickListener() );

        /* Arrange the components in the top level window. */
        JPanel mainArea = getMainArea();
        mainArea.add( plotPanel, BorderLayout.CENTER );

        /* Action for showing the grid. */
        String gridName = "Show Grid";
        Icon gridIcon = ResourceIcon.GRID_ON;
        String gridTip = "Select whether grid lines are displayed";
        JToggleButton gridButton = new JToggleButton( gridIcon );
        gridButton.setToolTipText( gridTip );
        gridModel_ = gridButton.getModel();
        gridModel_.setSelected( true );
        gridModel_.addActionListener( this );
        JCheckBoxMenuItem gridMenuItem = 
            new JCheckBoxMenuItem( gridName, gridIcon );
        gridMenuItem.setModel( gridModel_ );

        /* Action for resizing the plot. */
        Action resizeAction = new BasicAction( "Rescale", ResourceIcon.RESIZE,
                                               "Rescale the plot to show " +
                                               "all points" ) {
            public void actionPerformed( ActionEvent evt ) {
                plot_.rescale();
                forceReplot();
            }
        };

        /* Action for replotting (not very useful except for debugging etc). */
        Action replotAction = new BasicAction( "Replot", ResourceIcon.REDO,
                                               "Redraw the plot" ) {
            public void actionPerformed( ActionEvent evt ) {
                forceReplot();
            }
        };

        /* Action for printing. */
        Action printAction = new BasicAction( "Export as EPS",
                                              ResourceIcon.PRINT,
                                              "Export to Encapsulated " +
                                              "Postscript" ) {
            public void actionPerformed( ActionEvent evt ) {
                exportEPS();
            }
        };
        int fileMenuPos = 0;
        getFileMenu().insert( printAction, fileMenuPos++ );
        getFileMenu().insertSeparator( fileMenuPos++ );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( resizeAction );
        plotMenu.add( gridMenuItem );
        plotMenu.add( replotAction );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        subsetMenu.add( subMenu );
        blobAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( activeBlob_ ) {
                    useBlob();
                    setActiveBlob( false );
                }
                else {
                    setActiveBlob( true );
                }
            }
        };
        setActiveBlob( false );
        blobAction_.setEnabled( false );
        fromvisibleAction_ = new BasicAction( "New subset from visible",
                                              ResourceIcon.VISIBLE_SUBSET,
                                              "Define a new row subset " +
                                              "containing only " +
                                              "currently visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                String name = tcModel_.enquireSubsetName( PlotWindow.this );
                if ( name != null ) {
                    int inew = subsets_.size();
                    RowSubset visibleSet =
                        new BitsRowSubset( name, visibleRows_ );
                    subsets_.add( visibleSet );
                    subSelModel_.addSelectionInterval( inew, inew );
                }
            }
        };
        fromvisibleAction_.setEnabled( false );
        subsetMenu.add( blobAction_ );
        subsetMenu.add( fromvisibleAction_ );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for marker profile selection. */
        JMenu markerMenu = new JMenu( "Marker Types" );
        markerMenu.setMnemonic( KeyEvent.VK_M );
        MarkStyleProfile[] profiles = MARKER_PROFILES;
        for ( int i = 0; i < profiles.length; i++ ) {
            final MarkStyleProfile profile = profiles[ i ];
            String name = profile.getName();
            Icon icon = profile.getIcon();
            Action profileAct = new BasicAction( name, icon, 
                                                 "Set default marker types to "
                                                 + name ) {
                public void actionPerformed( ActionEvent evt ) {
                    markers_ = profile;
                    replot();
                }
            };
            markerMenu.add( profileAct );
        }
        getJMenuBar().add( markerMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( printAction );
        getToolBar().add( resizeAction );
        getToolBar().add( gridButton );
        getToolBar().add( blobAction_ );
        getToolBar().add( fromvisibleAction_ );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "PlotWindow" );

        /* Perform an initial plot. */
        replot();

        /* Render this component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Redraws the plot if any of the characteristics indicated by the 
     * currently-requested plot state have changed since the last time
     * it was done.
     */
    public void replot() {
        doReplot( false );
    }

    /**
     * Redraws the plot unconditionally.
     */
    public void forceReplot() {
        doReplot( true );
    }

    /**
     * Redraws the plot, perhaps taking account of whether the plot state
     * has changed since last time it was done.
     *
     * @param  force  if true, do the replot in any case;
     *                if false, only do it if the PlotState has changed
     */
    private void doReplot( boolean force ) {
        PlotState state = getPlotState();
        PlotState lastState = plot_.getState();
        if ( force || ! state.equals( lastState ) ) {

            /* Cancel any active blob-drawing.  This is necesary since 
             * the replot may put a different set of points inside it.
             * As a secondary consideration, forcing a replot by resizing 
             * the window etc is an intuitive way for the user to escape 
             * a blob-drawing session). */
            setActiveBlob( false );

            /* Send the plot component the most up to date plotting state. */
            plot_.setState( state );

            /* If the data points have changed since last time, re-acquire
             * them from the table.  This step is potentially time-consuming,
             * and should possibly be out of the event dispatch thread,
             * but up to a million rows it seems to be fast enough for
             * most tables.  Doing it like that would complicate the 
             * programming (and UI) significantly, so for now leave it
             * until it appears to be a problem. */
            if ( ! state.sameData( lastState ) ) {
                Points points;
                try {
                    points = readPoints( state );
                }
                catch ( IOException e ) {
                    logger.log( Level.SEVERE, "Error reading table data", e );
                    return;
                }
                plot_.setPoints( points );
            }

            /* If the axes are different from the last time we plotted, 
             * fix it so that all the points are included. */
            if ( ! state.sameAxes( lastState ) ) {
                plot_.rescale();
            }

            /* Schedule for repainting so the changes can take effect.
             * It's tempting to call recordVisiblePoints here and have done
             * with it, but it has to be done from within the painting
             * system since the window geometry might not be correct here. */
            replotted_ = true;
            plot_.repaint();
        }
    }

    /**
     * Works out which points are currently visible and stores this
     * information for possible later use.  Although this information
     * has to be calculcated by the ScatterPlot's paintComponent method,
     * it's not really possible to use this version of the information,
     * since it might be done in several calls of paintComponent
     * (with different clips) and it's not clear how to amalgamate all
     * these.  So we have to do this work more than once.
     *
     * <p>This method works out which points are visible in the current
     * plotting surface and stores the answer in a BitSet.  Elements of
     * this will be false if they either fall outside the range of the
     * current axes or do not appear in any of the plot state's
     * included subsets. 
     *
     * @param  state  plotting state
     * @param  points  data points
     * @param  surface  plotting surface
     */
    private void recordVisiblePoints( PlotState state, Points points, 
                                      PlotSurface surface ) {
        double[] xv = points.getXVector();
        double[] yv = points.getYVector();
        int np = points.getCount();
        RowSubset[] sets = state.getSubsets();
        int nset = sets.length;
        BitSet visible = new BitSet();
        PointRegistry plotted = new PointRegistry();
        int nVisible = 0;
        for ( int ip = 0; ip < np; ip++ ) {
            Point point = surface.dataToGraphics( xv[ ip ], yv[ ip ] );
            if ( point != null ) {
                int xp = point.x;
                int yp = point.y;
                long lp = (long) ip;
                for ( int is = 0; is < nset; is++ ) {
                    if ( sets[ is ].isIncluded( lp ) ) {
                        visible.set( ip );
                        plotted.addPoint( ip, point );
                        nVisible++;
                        break;
                    }
                }
            }
        }
        plotted.ready();
        visiblePoints_ = plotted;
        visibleRows_ = visible;
        fromvisibleAction_.setEnabled( nVisible > 0 );
        blobAction_.setEnabled( nVisible > 0 );
    }

    /**
     * Do something with the point of a given index.
     * This is invoked, for instance, when a point has been clicked on.
     *
     * @param   ip   row index for the point to be activated
     */
    private void activatePoint( int ip ) {
        System.out.println( "    row index: " + ip );
    }

    /**
     * Determine whether we are currently in blob-drawing mode or not.
     *
     * @param  activeBlob true iff blob is being drawn
     */
    private void setActiveBlob( boolean active ) {
        activeBlob_ = active;
        blobPanel_.clear();
        blobPanel_.setVisible( active );
        blobAction_.putValue( Action.NAME, 
                              active ? "Finish Drawing Region"
                                     : "Draw Subset Region" );
        blobAction_.putValue( Action.SMALL_ICON,
                              active ? ResourceIcon.BLOB_SUBSET_END
                                     : ResourceIcon.BLOB_SUBSET );
        blobAction_.putValue( Action.SHORT_DESCRIPTION,
                              active ? "Define subset from currently-drawn " +
                                       "region"
                                     : "Draw a region on the plot to define " +
                                       "a new row subset" );
    }

    /**
     * Called when the user has indicated that the blob he has drawn is
     * finished and ready to be turned into a subset.
     */
    private void useBlob() {
        Shape blob = blobPanel_.getBlob();
        String name = tcModel_.enquireSubsetName( PlotWindow.this );
        if ( name != null ) {
            int inew = subsets_.size();
            RowSubset blobSet =
                new BitsRowSubset( name, 
                                   visiblePoints_.getContainedPoints( blob ) );
            subsets_.add( blobSet );
            subSelModel_.addSelectionInterval( inew, inew );
        }
    }

    /**
     * Exports the currently displayed plot to encapsulated postscript.
     * The user is prompted to supply a filename.
     */
    private void exportEPS() {

        /* Prompt the user to select a file for output. */
        JFileChooser chooser = getPrintSaver();
        if ( chooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
            OutputStream ostrm = null;
            try {

                /* Construct a stream for the EPS to be written to.
                 * We shove an extra 'showpage' at the end, since the jibble
                 * class doesn't do this, and it's more useful with. */
                File file = chooser.getSelectedFile();
                ostrm = new FilterOutputStream( new FileOutputStream( file ) ) {
                    public void close() throws IOException {
                        String tailString = "\nshowpage\n";
                        int nTail = tailString.length();
                        byte[] tailBytes = new byte[ nTail ];
                        for ( int i = 0; i < nTail; i++ ) {
                            tailBytes[ i ] = (byte) tailString.charAt( i );
                        }
                        write( tailBytes );
                        super.close();
                    }
                };

                /* Construct a graphics object which will write postscript
                 * down this stream. */
                Rectangle bounds = plot_.getBounds();
                EpsGraphics2D g2 = 
                    new EpsGraphics2D( tcModel_.getLabel(), ostrm, 
                                       bounds.x, bounds.y, 
                                       bounds.x + bounds.width,
                                       bounds.y + bounds.height );

                /* Do the drawing. */
                plot_.print( g2 );

                /* Note this close call *must* be made, otherwise the 
                 * eps file is not flushed or correctly terminated. 
                 * This closes the output stream too. */
                g2.close();
            }
            catch ( IOException e ) {
                ErrorDialog.showError( e, "Error writing to file", this );
            }
        }
    }

    /**
     * Returns a file chooser widget with which the user can select a
     * file to output postscript of the currently plotted graph to.
     *
     * @return   a file chooser
     */
    private JFileChooser getPrintSaver() {
        if ( printSaver_ == null ) {
            printSaver_ = new JFileChooser( "." );
            printSaver_.setApproveButtonText( "Write EPS" );
            printSaver_.setDialogTitle( "Export Plot as EPS" );
            printSaver_.setAcceptAllFileFilterUsed( true );
            FileFilter psFilter = new FileFilter() {
                public String getDescription() {
                    return ".ps, .eps";
                }
                public boolean accept( File file ) {
                    if ( file.isDirectory() ) {
                        return true;
                    }
                    String name = file.getName();
                    int dotpos = name.indexOf( '.' );
                    if ( dotpos > 0 ) {
                        String ext = name.substring( dotpos + 1 ).toLowerCase();
                        return ext.equals( "ps" )
                            || ext.equals( "eps" );
                    }
                    return false;
                }
            };
            printSaver_.addChoosableFileFilter( psFilter );
            printSaver_.setFileFilter( psFilter );
        }
        return printSaver_;
    }

    /**
     * Returns an object which characterises the choices the user has
     * made in the GUI to indicate the plot that s/he wants to see.
     *
     * @return  snapshot of the currently-selected plot request
     */
    public PlotState getPlotState() {

        /* Initialise a new PlotState object from gui components. */
        PlotState state =
            new PlotState( (StarTableColumn) xColBox_.getSelectedItem(),
                           (StarTableColumn) yColBox_.getSelectedItem() );
        state.setXLog( xLogBox_.isSelected() );
        state.setYLog( yLogBox_.isSelected() );
        state.setGrid( gridModel_.isSelected() );

        /* Construct an array of the subsets that are used. */
        int[] selection = subSelRecorder_.getOrderedSelection();
        int nrsets = selection.length;
        RowSubset[] usedSubsets = new RowSubset[ nrsets ];
        MarkStyle[] styles = new MarkStyle[ nrsets ];
        for ( int isel = 0; isel < nrsets; isel++ ) {
            int isub = selection[ isel ];
            usedSubsets[ isel ] = (RowSubset) subsets_.get( isub );
            styles[ isel ] = markers_.getStyle( isub );
        }
        state.setSubsets( usedSubsets, styles );
        return state;
    }

    /**
     * Acquires the numeric data required for a given plot state.
     *
     * @param  state  plot state
     * @return points  object containing data values for <tt>state</tt>
     */
    public Points readPoints( PlotState state ) throws IOException {
        int xcol = getColumnIndex( state.getXColumn() );
        int ycol = getColumnIndex( state.getYColumn() );
        StarTable dataModel = tcModel_.getDataModel();
        int nrow = AbstractStarTable
                  .checkedLongToInt( dataModel.getRowCount() );
        double[] x = new double[ nrow ];
        double[] y = new double[ nrow ];
        RowSequence rseq = dataModel.getRowSequence();
        for ( int irow = 0; rseq.hasNext(); irow++ ) {
            rseq.next();
            x[ irow ] = doubleValue( rseq.getCell( xcol ) );
            y[ irow ] = doubleValue( rseq.getCell( ycol ) );
        } 
        return new Points( x, y );
    }

    /**
     * Returns a new JComboBox from which can be selected any of the
     * columns of the table which can be plotted.
     * This box will be updated when new columns are added to the
     * table model and so on.
     *
     * @return  combo box
     */
    private JComboBox makePlottableColumnComboBox() {

        /* Construct a model which contains an entry for each column
         * which contains Numbers or Dates. */
        ComboBoxModel boxModel =
            new RestrictedColumnComboBoxModel( tcModel_.getColumnModel(),
                                               false ) {
                public boolean acceptColumn( ColumnInfo cinfo ) {
                    Class clazz = cinfo.getContentClass();
                    return Number.class.isAssignableFrom( clazz )
                        || Date.class.isAssignableFrom( clazz );
                }
            };

        /* Create a new combobox. */
        JComboBox box = new JComboBox( boxModel );

        /* Give it a suitable renderer. */
        box.setRenderer( new ColumnCellRenderer() );
        return box;
    }

    /**
     * Returns the index in the TableModel (not the TableColumnModel) of
     * the given TableColumn.
     *
     * @param   tcol   the column whose index is to be found
     * @return  the index of <tt>tcol</tt> in the table model
     */
    public int getColumnIndex( TableColumn tcol ) {
        return tcol.getModelIndex();
    }

    /**
     * Returns a numeric (double) value for the given object where it
     * can be done.
     *
     * @param  value  an object
     * @return  floating point representation of <tt>value</tt>, or
     *          NaN if it can't be done
     */
    private double doubleValue( Object value ) {
        if ( value instanceof Number ) {
            return ((Number) value).doubleValue();
        }
        else if ( value instanceof Date ) {
            long milliseconds = ((Date) value).getTime();
            return 1970.0 + milliseconds / MILLISECONDS_PER_YEAR;
        }
        else {
            return Double.NaN;
        }
    }


    /*
     * Listener implementations to do a replot when UI selections have changed.
     */

    public void valueChanged( ListSelectionEvent evt ) {
        replot();
    }

    public void actionPerformed( ActionEvent evt ) {
        replot();
    }

    public void itemStateChanged( ItemEvent evt ) {
        replot();
    }

    public void surfaceChanged() {
        forceReplot();
    }

    /**
     * Mouse listener to handle clicks on given points.
     */
    private class PointClickListener extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            int butt = evt.getButton();
            if ( butt == MouseEvent.BUTTON1 ) {
                int ip = visiblePoints_.getClosestPoint( evt.getPoint(), 3 );
                if ( ip >= 0 ) {
                    activatePoint( ip );
                }
            }
        }
    }

}
