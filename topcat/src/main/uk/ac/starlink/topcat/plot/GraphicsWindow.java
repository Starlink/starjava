package uk.ac.starlink.topcat.plot;

import Acme.JPM.Encoders.GifEncoder;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ListModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import org.jibble.epsgraphics.EpsGraphics2D;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Abstract superclass for windows doing N-dimensional plots of table data.
 *
 * @author   Mark Taylor
 * @since    26 Oct 2005
 */
public abstract class GraphicsWindow extends AuxWindow
                                     implements SurfaceListener {

    private final int ndim_;
    private final PointSelectorSet pointSelectors_;

    private final ReplotListener replotListener_;
    private final Action replotAction_;
    private final ToggleButtonModel gridModel_;
    private final ToggleButtonModel[] flipModels_;
    private final ToggleButtonModel[] logModels_;

    private StyleSet styleSet_;
    private Points points_;
    private JFileChooser exportSaver_;
    private PlotState lastState_;
    private Box statusBox_;

    private static FileFilter psFilter_;
    private static FileFilter gifFilter_;
    private static final Object EPS = "EPS";
    private static final Object GIF = "GIF";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructor.
     *
     * @param   viewName  name of the view window
     * @param   axisNames  array of labels by which each axis is known;
     *          the length of this array defines the dimensionality of the plot
     * @param   parent   parent window - may be used for positioning
     */
    public GraphicsWindow( String viewName, String[] axisNames,
                           Component parent ) {
        super( viewName, parent );
        ndim_ = axisNames.length;

        /* Set up point selector component. */
        StyleSet proxyStyleSet = new StyleSet() {
            public String getName() {
                return styleSet_.getName();
            }
            public Style getStyle( int index ) {
                return styleSet_.getStyle( index );
            }
        };
        pointSelectors_ = new PointSelectorSet( axisNames, proxyStyleSet );
        getControlPanel().setLayout( new BoxLayout( getControlPanel(),
                                                    BoxLayout.Y_AXIS ) );
        getControlPanel().add( new SizeWrapper( pointSelectors_ ) );

        /* Ensure that changes to the point selection trigger a replot. */
        replotListener_ = new ReplotListener();
        pointSelectors_.addActionListener( replotListener_ );
        pointSelectors_.addNewSelector();

         /* Actions for exporting the plot. */
        Action gifAction =
            new ExportAction( GIF, "Export as GIF", ResourceIcon.IMAGE,
                                   "Save plot as a GIF file" );
        Action epsAction =
            new ExportAction( EPS, "Export as EPS", ResourceIcon.PRINT,
                                   "Export to Encapsulated Postscript file" );
        int fileMenuPos = 0;
        getFileMenu().insert( epsAction, fileMenuPos++ );
        getFileMenu().insert( gifAction, fileMenuPos++ );
        getFileMenu().insertSeparator( fileMenuPos++ );
        getToolBar().add( epsAction );
        getToolBar().add( gifAction );
        getToolBar().addSeparator();

        /* Other actions. */
        replotAction_ =
            new GraphicsAction( "Replot", ResourceIcon.REDO,
                                "Redraw the plot" );

        /* Action for showing grid. */
        gridModel_ = new ToggleButtonModel( "Show Grid", ResourceIcon.GRID_ON,
                                            "Select whether grid lines are " +
                                            "drawn" );
        gridModel_.setSelected( true );
        gridModel_.addActionListener( replotListener_ );

        /* Axis flags. */
        flipModels_ = new ToggleButtonModel[ ndim_ ];
        logModels_ = new ToggleButtonModel[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            String ax = axisNames[ i ];
            flipModels_[ i ] = new ToggleButtonModel( "Flip " + ax + " Axis",
                null, "Reverse the sense of the " + axisNames[ i ] + " axis" );
            logModels_[ i ] = new ToggleButtonModel( "Log " + ax + " Axis",
                null, "Logarithmic scale for the " + axisNames[ i ] + " axis" );
            flipModels_[ i ].addActionListener( replotListener_ );
            logModels_[ i ].addActionListener( replotListener_ );
        }
        if ( ndim_ > 0 ) {
            flipModels_[ 0 ].setIcon( ResourceIcon.XFLIP );
            logModels_[ 0 ].setIcon( ResourceIcon.XLOG );
            if ( ndim_ > 1 ) {
                flipModels_[ 1 ].setIcon( ResourceIcon.YFLIP );
                logModels_[ 1 ].setIcon( ResourceIcon.YLOG );
            }
        }
    }

    public void setVisible( boolean visible ) {
        super.setVisible( visible );
        if ( lastState_ == null ) {
            lastState_ = getPlotState();
            lastState_.setValid( false );
        }

        /* Set a suitable default style set. */
        long npoint = 0;
        ListModel tablesList = ControlWindow.getInstance().getTablesListModel();
        for ( int i = 0; i < tablesList.getSize(); i++ ) {
            TopcatModel tcModel = (TopcatModel) tablesList.getElementAt( i );
            npoint += tcModel.getDataModel().getRowCount();
        }
        styleSet_ = getDefaultStyles( (int) Math.min( npoint,
                                                      Integer.MAX_VALUE ) );
    }

    /**
     * Returns the PointSelectorSet component used by this window.
     *
     * @return  point selector set
     */
    public PointSelectorSet getPointSelectors() {
        return pointSelectors_;
    }

    /**
     * Returns an array of button models representing the inversion state
     * for each axis.  Selected state for each model indicates that that
     * axis has been flipped.
     *
     * @return   button models for flip state
     */
    public ToggleButtonModel[] getFlipModels() {
        return flipModels_;
    }

    /**
     * Returns an array of button models representing the log/linear state
     * for each axis.  Selected state for each model indicates that that
     * axis is logarithmic, unselected means linear.
     *
     * @return  button models for log state
     */
    public ToggleButtonModel[] getLogModels() {
        return logModels_;
    }

    /**
     * Returns a line suitable for putting status information into.
     *
     * @return  status  component
     */
    public Box getStatusBox() {
        if ( statusBox_ == null ) {
            statusBox_ = Box.createHorizontalBox();
            getControlPanel().add( Box.createVerticalStrut( 5 ) );
            getControlPanel().add( statusBox_ );
        }
        return statusBox_;
    }

    /**
     * Returns the component containing the graphics output of this 
     * window.  This is the component which is exported or printed etc,
     * so should contain only the output data, not any user interface
     * decoration.
     *
     * @return   plot component
     */
    protected abstract JComponent getPlot();

    /**
     * Performs an actual plot.
     *
     * @param  state  plot state determining details of plot configuration
     * @param  points  data to plot
     */
    protected abstract void doReplot( PlotState state, Points points );

    /**
     * Returns a StyleSet which can supply markers.
     * The <code>npoint</code> may be used as a hint for how many 
     * points are expected to be drawn with it.
     *
     * @param    npoint  approximate number of points - use -1 for unknown
     * @return   style factory
     */
    public abstract StyleSet getDefaultStyles( int npoint );

    /**
     * Sets the style set to use for this window.
     * 
     * @param   styleSet  new style set
     */
    public void setStyles( StyleSet styleSet ) {
        styleSet_ = styleSet;
    }

    /**
     * Constructs a new PlotState.  This is called by {@link #getPlotState}
     * prior to the PlotState configuration done there.  Thus if a 
     * subclass wants to provide and configure a particular state
     * (for instance one of a specialised subclass of PlotState) it can
     * override this method to do so.
     * The default implementation just invokes <code>new PlotState()</code>.
     *
     * @return   returns a new PlotState object ready for generic
     *           configuration
     */
    protected PlotState createPlotState() {
        return new PlotState();
    }

    /**
     * Returns an object which characterises the choices the user has
     * made in the GUI to indicate the plot that s/he wants to see.
     *
     * @return  snapshot of the currently-selected plot request
     */
    public PlotState getPlotState() {

        /* Create a plot state as delegated to the current instance. */
        PlotState state = createPlotState();

        /* Can't plot, won't plot. */
        if ( ! pointSelectors_.getMainSelector().isValid() ) {
            state.setValid( false );
            return state;
        }

        /* Set per-axis characteristics. */
        StarTableColumn[] axcols =
            pointSelectors_.getMainSelector().getColumns();
        ColumnInfo[] axinfos = new ColumnInfo[ ndim_ ];
        boolean[] flipFlags = new boolean[ ndim_ ];
        boolean[] logFlags = new boolean[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            axinfos[ i ] = axcols[ i ].getColumnInfo();
            flipFlags[ i ] = flipModels_[ i ].isSelected();
            logFlags[ i ] = logModels_[ i ].isSelected();
        }
        state.setAxes( axinfos );
        state.setLogFlags( logFlags );
        state.setFlipFlags( flipFlags );

        /* Set point selection. */
        state.setPointSelection( pointSelectors_.getPointSelection() );

        /* Set grid status. */
        state.setGrid( gridModel_.isSelected() );

        /* Return the configured state for use. */
        state.setValid( true );
        return state;
    }

    /**
     * Returns the button model used to select whether a grid will be
     * drawn or not.
     *
     * @return   grid toggle model
     */
    public ToggleButtonModel getGridModel() {
        return gridModel_;
    }

    /**
     * Returns an action which can be used to force a replot of the plot.
     *
     * @return   replot action
     */
    public Action getReplotAction() {
        return replotAction_;
    }

    /**
     * Sets the main table in the point selector component.
     *
     * @param  tcModel  new table
     */
    public void setMainTable( TopcatModel tcModel ) {
        PointSelector mainSel = pointSelectors_.getMainSelector();
        mainSel.setTable( tcModel, true );
    }

    /**
     * Redraws the plot if any of the characteristics indicated by the
     * currently-requested plot state have changed since the last time
     * it was done.  
     * This method schedules a replot on the event dispatch thread,
     * so it may be called from any thread.
     */
    public void replot() {
        scheduleReplot( false, false );
    }                              

    /**
     * Redraws the plot unconditionally.
     * This method schedules a replot on the event dispatch thread,
     * so it may be called from any thread.
     */
    public void forceReplot() {
        scheduleReplot( true, false );
    }

    /**
     * Schedules a conditional replot on the event dispatch thread,
     * perhaps taking account of whether the plot state has changed since
     * the last time it was done.
     *
     * @param  forcePlot  if true, do the replot in any case;
     *                    if false, only do it if the PlotState has changed
     * @param  forceData  if true, re-acquire data in any case;
     *                    if false, only do it if the data selection has changed
     */
    private void scheduleReplot( final boolean forcePlot,
                                 final boolean forceData ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                performReplot( forcePlot, forceData );
            }
        } );
    }

    /**
     * Redraws the plot, perhaps taking account of whether the plot state
     * has changed since last time it was done.
     *
     * @param  forcePlot  if true, do the replot in any case;
     *                    if false, only do it if the PlotState has changed
     * @param  forceData  if true, re-acquire data in any case;
     *                    if false, only do it if the data selection has changed
     */
    private void performReplot( boolean forcePlot, boolean forceData ) {
        PlotState state = getPlotState();
        PlotState lastState = lastState_;
        if ( forcePlot || ! state.equals( lastState ) ) {

            /* If the data points have changed since last time, re-acquire
             * them from the table.  This step is potentially time-consuming,
             * and should possibly be out of the event dispatch thread,
             * but up to a million rows it seems to be fast enough for
             * most tables.  Doing it like that would complicate the
             * programming (and UI) significantly, so for now leave it
             * until it appears to be a problem. */
            if ( forceData || ! state.sameData( lastState ) ) {
                if ( state.getValid() ) { 
                    try {
                        points_ = readPoints( state );
                    }
                    catch ( IOException e ) {
                        logger_.log( Level.SEVERE,
                                     "Error reading table data", e );
                        return;
                    }
                }
                else {
                    points_ = null;
                }
            }
            doReplot( state, points_ );
            lastState_ = state;
        }
    }

    /**
     * Returns a listener which will perform a replot when any event occurs.
     *
     * @return   replot listener
     */
    protected ReplotListener getReplotListener() {
        return replotListener_;
    }

    /**
     * Adds a new row subset to tables associated with this window as
     * appropriate.  The subset is based on a bit vector
     * representing the points in this window's Points object.
     *
     * @param  pointsMask  bit vector giving included points
     */
    protected void addNewSubsets( BitSet pointsMask ) {

        /* If the subset is empty, just warn the user and return. */
        if ( pointsMask.cardinality() == 0 ) {
            JOptionPane.showMessageDialog( this, "Empty subset",
                                           "Blank Selection",
                                           JOptionPane.ERROR_MESSAGE );
            return;                                  
        }
  
        /* Get the name for the new subset(s). */
        String name = enquireSubsetName();
        if ( name == null ) {
            return;
        }

        /* For the given mask, which corresponds to all the plotted points,
         * deconvolve it into individual masks for any of the tables
         * that our point selection is currently dealing with. */
        PointSelection.TableMask[] tableMasks =
            lastState_.getPointSelection().getTableMasks( pointsMask );

        /* Handle each of the affected tables separately. */
        for ( int i = 0; i < tableMasks.length; i++ ) {
            TopcatModel tcModel = tableMasks[ i ].getTable();
            BitSet tmask = tableMasks[ i ].getMask();

            /* Try adding a new subset to the table. */
            if ( tmask.cardinality() > 0 ) {
                OptionsListModel subsets = tcModel.getSubsets();
                int inew = subsets.size();
                assert tmask.length() <= tcModel.getDataModel().getRowCount();
                subsets.add( new BitsRowSubset( name, tmask ) );

                /* Then make sure that the newly added subset is selected
                 * in each of the point selectors. */
                PointSelectorSet pointSelectors = getPointSelectors();
                for ( int ips = 0; ips < pointSelectors.getSelectorCount();
                      ips++ ) {
                    PointSelector psel = pointSelectors.getSelector( ips );
                    if ( psel.getTable() == tcModel ) {
                        boolean[] flags = psel.getSubsetSelection();
                        assert flags.length == inew + 1;
                        flags[ inew ] = true;
                        psel.setSubsetSelection( flags );
                    }
                }
            }
        }
    }

    /**
     * Acquires and caches the numeric data required for a given plot state.
     * This is done so that reading one or more columns of the table,
     * which might be a bit time-consuming, doesn't have to get done
     * every time a replot is done.  However, this method probably will
     * be called in the event dispatch thread, so it's still somewhat
     * worrying.
     *
     * @param  state  plot state
     * @return points  object containing data values for <tt>state</tt>
     */
    private Points readPoints( PlotState state ) throws IOException {
        return state.getPointSelection().readPoints();
    }

    /**
     * Exports the currently displayed plot to encapsulated postscript.
     *
     * @param  ostrm  destination stream for the EPS
     */
    private void exportEPS( OutputStream ostrm ) throws IOException {

        /* Construct a graphics object which will write postscript
         * down this stream. */
        JComponent plot = getPlot();
        Rectangle bounds = plot.getBounds();
        EpsGraphics2D g2 = new EpsGraphics2D( getTitle(), ostrm,
                                              bounds.x, bounds.y,
                                              bounds.x + bounds.width,
                                              bounds.y + bounds.height );

        /* Do the drawing. */
        plot.print( g2 );

        /* Note this close call *must* be made, otherwise the
         * eps file is not flushed or correctly terminated.
         * This closes the output stream too. */
        g2.close();
    }

    /**
     * Exports the currently displayed plot to GIF format.
     *
     * <p>There's something wrong with this - it ought to produce a
     * transparent background, but it doesn't.  I'm not sure why, or
     * even whether it's to do with the plot or the encoder.
     *
     * @param  ostrm  destination stream for the gif
     */
    private void exportGif( OutputStream ostrm ) throws IOException {

        /* Get the component which will be plotted and its dimensions. */
        JComponent plot = getPlot();
        int w = plot.getWidth();
        int h = plot.getHeight();

        /* Draw it onto a new BufferedImage. */
        BufferedImage image =
            new BufferedImage( w, h, BufferedImage.TYPE_4BYTE_ABGR );
        plot.paint( image.getGraphics() );

        /* Count the number of colours represented in the resulting image. */
        Set colors = new HashSet();
        for ( int ix = 0; ix < w; ix++ ) {
            for ( int iy = 0; iy < h; iy++ ) {
                colors.add( new Integer( image.getRGB( ix, iy ) ) );
            }
        }

        /* If there are too many, redraw the image into an indexed image
         * instead.  This is necessary since the GIF encoder we're using
         * here just gives up if there are too many. */
        if ( colors.size() > 254 ) {
            image = new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED );
            plot.paint( image.getGraphics() );
        }

        /* Write the image as a gif down the provided stream. */
        new GifEncoder( image, ostrm ).encode();
    }

    /**
     * Returns a file chooser widget with which the user can select a
     * file to output postscript of the currently plotted graph to.
     *
     * @return   a file chooser
     */
    private JFileChooser getExportSaver() {
        if ( exportSaver_ == null ) {
            exportSaver_ = new JFileChooser( "." );
            psFilter_ = new FileFilter() {
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
            gifFilter_ = new FileFilter() {
                public String getDescription() {
                    return ".gif";
                }
                public boolean accept( File file ) {
                    if ( file.isDirectory() ) {
                        return true;
                    }
                    String name = file.getName();
                    int dotpos = name.indexOf( '.' );
                    if ( dotpos > 0 ) {
                        String ext = name.substring( dotpos + 1 ).toLowerCase();
                        return ext.equals( "gif" );
                    }
                    return false;
                }
            };
            exportSaver_.setAcceptAllFileFilterUsed( true );
            exportSaver_.addChoosableFileFilter( psFilter_ );
            exportSaver_.addChoosableFileFilter( gifFilter_ );
        }
        return exportSaver_;
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
     * SurfaceListener implementation.
     */
    public void surfaceChanged() {
        forceReplot();
    }

    /**
     * Actions for exporting the plot to a file.
     */
    private class ExportAction extends BasicAction {
        final Object format_;

        ExportAction( Object format, String name, Icon icon, String desc ) {
            super( name, icon, desc );
            format_ = format;
        }

        public void actionPerformed( ActionEvent evt ) {
            Component parent = GraphicsWindow.this;

            /* Acquire and configure the file chooser. */
            JFileChooser chooser = getExportSaver();
            String approve;
            String title;
            FileFilter filter;
            if ( format_ == EPS ) {
                approve = "Write EPS";
                title = "Export plot as EPS";
                filter = psFilter_;
            }
            else if ( format_ == GIF ) {
                approve = "Write GIF";
                title = "Export plot as GIF";
                filter = gifFilter_;
            }
            else {
                throw new AssertionError();
            }
            chooser.setDialogTitle( title );
            chooser.setFileFilter( filter );

            /* Prompt the user to select a file for output. */
            if ( chooser.showDialog( parent, approve ) ==
                 JFileChooser.APPROVE_OPTION ) {
                OutputStream ostrm = null;
                try {

                    /* Construct the output stream. */
                    File file = chooser.getSelectedFile();
                    ostrm = new BufferedOutputStream(
                                    new FileOutputStream( file ) );

                    /* Write output to it. */
                    if ( format_ == GIF ) {
                        exportGif( ostrm );
                    }
                    else if ( format_ == EPS ) {
                        exportEPS( ostrm );
                    }
                    else {
                        assert false;
                    }
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( parent, "Write Error", e );
                }
                finally {
                    if ( ostrm != null ) {
                        try {
                            ostrm.close();
                        }
                        catch ( IOException e ) {
                            // no action
                        }
                    }
                }
            }
        }
    }

    /**
     * Miscellaneous actions.
     */
    private class GraphicsAction extends BasicAction {
        GraphicsAction( String name, Icon icon, String desc ) {
            super( name, icon, desc );
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( this == replotAction_ ) {
                scheduleReplot( true, true );
            }
        }
    }

    /**
     * General purpose listener which replots given an event.
     */
    protected class ReplotListener implements ActionListener, ItemListener,
                                              ListSelectionListener,
                                              ChangeListener {
        public void actionPerformed( ActionEvent evt ) {
            replot();
        }
        public void itemStateChanged( ItemEvent evt ) {
            replot();
        }
        public void valueChanged( ListSelectionEvent evt ) {
            replot();
        }
        public void stateChanged( ChangeEvent evt ) {
            replot();
        }
    }

}
