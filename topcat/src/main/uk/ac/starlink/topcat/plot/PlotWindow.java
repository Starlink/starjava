package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.StreamPrintServiceFactory;
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
import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
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
    private final JComboBox xColBox_;
    private final JComboBox yColBox_;
    private final JCheckBox xLogBox_;
    private final JCheckBox yLogBox_;
    private final ListSelectionModel subSelModel_;
    private final ButtonModel gridModel_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final Action fromvisibleAction_;
    private JFileChooser printSaver_;
    private BitSet visibleRows_;

    private static final String PRINT_MIME_TYPE = "application/postscript";
    private static final double MILLISECONDS_PER_YEAR
                              = 365.25 * 24 * 60 * 60 * 1000;
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

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
        plot_ = new ScatterPlot( new PtPlotSurface( this ) );
        JPanel plotPanel = new JPanel( new BorderLayout() );
        plotPanel.add( plot_, BorderLayout.CENTER );
        plotPanel.setPreferredSize( new Dimension( 500, 300 ) );

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
        Action printAction = new BasicAction( "Print", ResourceIcon.PRINT,
                                              "Export to postscript" ) {
            public void actionPerformed( ActionEvent evt ) {
                print();
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
        subsetMenu.add( fromvisibleAction_ );
        getJMenuBar().add( subsetMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( printAction );
        getToolBar().add( resizeAction );
        getToolBar().add( gridButton );
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

            /* Schedule for repainting so the changes can take effect. */
            plot_.repaint();

            /* Since the points (will) have been replotted, it's necessary
             * to update our record of what points appear on the plotting
             * surface. */
            Points points = plot_.getPoints();
            recordVisiblePoints( state, points, plot_.getSurface() );
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
        int nVisible = 0;
        for ( int ip = 0; ip < np; ip++ ) {
            Point point = surface.dataToGraphics( xv[ ip ], yv[ ip ] );
            if ( point != null ) {
                int xp = point.x;
                int yp = point.y;
                long lp = (long) ip;
                for ( int is = 0; is < nset; is++ ) {
                    if ( sets[ is ].isIncluded( lp ) ) {
                        nVisible++;
                        visible.set( ip );
                        break;
                    }
                }
            }
        }
        visibleRows_ = visible;
        fromvisibleAction_.setEnabled( nVisible > 0 );
    }

    /**
     * Prints the currently plotted graph.
     */
    private void print() {
        StreamPrintServiceFactory factory = 
            getPrintServiceFactory( PRINT_MIME_TYPE );
        if ( factory != null ) {
            JFileChooser chooser = getPrintSaver();
            if ( chooser.showSaveDialog( this ) 
                 == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = chooser.getSelectedFile();
                    OutputStream ostrm = new FileOutputStream( file );
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPrintService( factory.getPrintService( ostrm ) );
                    job.setPrintable( plot_ );
                    if ( job.printDialog() ) {
                        job.print();
                    }
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( e, "Error writing to file", this );
                }
                catch ( PrinterException e ) {
                    ErrorDialog.showError( e, "Printer error", this );
                }
            }
        }
        else {
            JOptionPane.showMessageDialog( this, "Sorry, no print service " +
                                           "is available", "No Print Service",
                                           JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Look for a stream printing service which can print in a given format.
     * If one with the requested MIME type is available, it is returned.
     * Otherwise, if some other streaming print service is available,
     * that is returned.  If there are no streaming print services,
     * <tt>null</tt> is returned.
     *
     * @param  mtype  desired MIME type for output
     * @return  print service, preferably of type <tt>mimeType</tt>,
     *          or <tt>null</tt>
     */
    private StreamPrintServiceFactory getPrintServiceFactory( String mtype ) {
        StreamPrintServiceFactory[] services =
            PrinterJob.lookupStreamPrintServices( null );
        for ( int i = 0; i < services.length; i++ ) {
            if ( mtype.equals( services[ i ].getOutputFormat() ) ) {
                return services[ i ];
            }
        }
        return services.length > 0 ? services[ 0 ] : null;
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
            styles[ isel ] = MarkStyle.defaultStyle( isub );
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

}
