package uk.ac.starlink.topcat.plot;

import Acme.JPM.Encoders.GifEncoder;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import org.jibble.epsgraphics.EpsGraphics2D;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.CheckBoxMenu;
import uk.ac.starlink.topcat.CheckBoxStack;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Abstract superclass for windows doing N-dimensional plots of table data.
 *
 * @author   Mark Taylor
 * @since    26 Oct 2005
 */
public abstract class GraphicsWindow extends AuxWindow
                                     implements SurfaceListener,
                                                TopcatListener {

    private final TopcatModel tcModel_;
    private final OptionsListModel subsets_;
    private final int ndim_;

    private final JComboBox[] colBoxes_;
    private final JCheckBox[] logBoxes_;
    private final JCheckBox[] flipBoxes_;
    private final JComponent stackPanel_;
    private final ListSelectionModel subSelModel_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final ReplotListener replotListener_;
    private final Action replotAction_;
    private final JToggleButton gridButton_;

    private Points points_;
    private JFileChooser exportSaver_;
    private PlotState lastState_;

    private static FileFilter psFilter_;
    private static FileFilter gifFilter_;
    private static final Object EPS = "EPS";
    private static final Object GIF = "GIF";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructor.
     *
     * @param   tcModel  topcat model
     * @param   viewName  name of the view window
     * @param   axisNames  array of labels by which each axis is known;
     *          the length of this array defines the dimensionality of the plot
     * @param   parent   parent window - may be used for positioning
     */
    public GraphicsWindow( TopcatModel tcModel, String viewName, 
                           String[] axisNames, Component parent ) {
        super( null, parent );
        labelView( tcModel, viewName );

        tcModel_ = tcModel;
        subsets_ = tcModel_.getSubsets();
        ndim_ = axisNames.length;
        replotListener_ = new ReplotListener();
        tcModel_.addTopcatListener( this );

        /* Prepare arrays of components for each axis in this display. */
        colBoxes_ = new JComboBox[ ndim_ ];
        logBoxes_ = new JCheckBox[ ndim_ ];
        flipBoxes_ = new JCheckBox[ ndim_ ];

        /* Prepare and place a component for axis selection. */
        Box axisBox = new Box( BoxLayout.Y_AXIS );
        getControlPanel().add( axisBox );

        /* Construct and place panels for selection and configuration of
         * each axis. */
        for ( int i = 0; i < ndim_; i++ ) {
            String aName = axisNames[ i ];
            JPanel configPanel = new JPanel();
            configPanel.setBorder( makeTitledBorder( aName + " Axis" ) );
            if ( i > 0 ) {
                axisBox.add( Box.createGlue() );
            }
            axisBox.add( configPanel );

            /* Create a column selector box. */
            colBoxes_[ i ] = makePlottableColumnComboBox();

            /* If there are too few numeric columns then inform the user
             * and bail out. */
            if ( colBoxes_[ i ].getItemCount() < ndim_ ) {
                JOptionPane.showMessageDialog( null, "Too few numeric columns "
                                             + "in table", "Plot Error",
                                               JOptionPane.ERROR_MESSAGE );
                dispose();
            }

            /* Configure and place the column selector box. */
            colBoxes_[ i ].setSelectedIndex( i );
            colBoxes_[ i ].addActionListener( replotListener_ );
            configPanel.add( colBoxes_[ i ] );

            /* Add and configure a linear/log selector. */
            logBoxes_[ i ] = new JCheckBox( "Log plot" );
            logBoxes_[ i ].addActionListener( replotListener_ );
            configPanel.add( logBoxes_[ i ] );

            /* Add and configure an axis direction flip selector. */
            flipBoxes_[ i ] = new JCheckBox( "Flip" );
            flipBoxes_[ i ].addActionListener( replotListener_ );
            configPanel.add( flipBoxes_[ i ] );
        }

        /* Make a scrollable box in the control panel for selecting subsets. */
        CheckBoxStack stack = new CheckBoxStack( subsets_ );
        subSelModel_ = stack.getSelectionModel();
        stackPanel_ = new JScrollPane( stack );
        stackPanel_.setBorder( makeTitledBorder( "Row subsets" ) );
        getControlPanel().add( stackPanel_ );

        /* Initialise the selections. */
        modelChanged( new TopcatEvent( tcModel_, TopcatEvent.SUBSET, null ) );

        /* Maintain a list of selected subsets updated from this model.
         * This cannot be worked out from the model on request, since the
         * order in which selections have been made is significant, and
         * is not preserved by the model. */
        subSelRecorder_ = new OrderedSelectionRecorder( subSelModel_ );
        subSelModel_.addListSelectionListener( replotListener_ );
        subSelModel_.addListSelectionListener( subSelRecorder_ );

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

        /* Action for replotting. */
        replotAction_ = new BasicAction( "Replot", ResourceIcon.REDO,
                                               "Redraw the plot" ) {
            public void actionPerformed( ActionEvent evt ) {
                doReplot( true, true );
            }
        };

        /* Action for showing grid. */
        String gridName = "Show Grid";
        Icon gridIcon = ResourceIcon.GRID_ON;
        String gridTip = "Select whether grid lines are displayed";
        gridButton_ = new JToggleButton( gridIcon );
        gridButton_.setToolTipText( gridTip );
        ButtonModel gridModel = gridButton_.getModel();
        gridModel.setSelected( true );
        gridModel.addActionListener( replotListener_ );
    }

    /**
     * Redraws the plot if any of the characteristics indicated by the
     * currently-requested plot state have changed since the last time
     * it was done.
     */
    public void replot() {
        doReplot( false, false );
    }                              

    /**
     * Redraws the plot unconditionally.
     */
    public void forceReplot() {
        doReplot( true, false );
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
     * Creates a new PlotState object based on the current state of this
     * component.  The returned PlotState should be configured to contain
     * all the information about the current state of the specifics of
     * this GraphicsWindow subclass.  The parts which are generic to
     * all GraphicsWindow instances will be filled in (during a 
     * {@link #getPlotState} call) by the superclass.
     *
     * @return  state of this component vis-a-vis plot detail requests
     */
    protected abstract PlotState createPlotState();

    /**
     * Returns the marker style to be used for one of the subsets.
     *
     * @param  isub  index into the subsets list
     * @return   marker style to use for subset number <code>isub</code>
     */
    protected abstract MarkStyle getMarkStyle( int isub );

    /**
     * Returns an object which characterises the choices the user has
     * made in the GUI to indicate the plot that s/he wants to see.
     *
     * @return  snapshot of the currently-selected plot request
     */
    public PlotState getPlotState() {

        /* Create a plot state as delegated to the current instance. */
        PlotState state = createPlotState();

        /* Set per-column characteristics. */
        StarTableColumn[] cols = new StarTableColumn[ ndim_ ];
        boolean[] logFlags = new boolean[ ndim_ ];
        boolean[] flipFlags = new boolean[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            cols[ i ] = (StarTableColumn) colBoxes_[ i ].getSelectedItem();
            logFlags[ i ] = logBoxes_[ i ].isSelected();
            flipFlags[ i ] = flipBoxes_[ i ].isSelected();
        }
        state.setColumns( cols );
        state.setLogFlags( logFlags );
        state.setFlipFlags( flipFlags );

        /* Set selected subsets and associated characteristics. */
        int[] selection = getOrderedSubsetSelection();
        int nrsets = selection.length;
        RowSubset[] usedSubsets = new RowSubset[ nrsets ];
        MarkStyle[] styles = new MarkStyle[ nrsets ];
        for ( int isel = 0; isel < nrsets; isel++ ) {
            int isub = selection[ isel ];
            usedSubsets[ isel ] = (RowSubset) subsets_.get( isub );
            styles[ isel ] = getMarkStyle( isub );
            assert state.getSubsets() == null
                || state.getSubsets()[ isel ] == usedSubsets[ isel ];
            assert state.getStyles() == null
                || state.getStyles()[ isel ].equals( styles[ isel ] );
        }

        /* This step isn't necessary since the superclass getPlotState method
         * will fill these values in.  However currently it does an assertion
         * test that they are the same if it is filled in here. */
        state.setSubsets( usedSubsets, styles );

        /* Set grid status. */
        state.setGrid( gridButton_.isSelected() );

        /* Return the configured state for use. */
        return state;
    }

    /**
     * Returns the model which holds information about which row subsets 
     * have been selected for display in the plot.
     *
     * @param  subset selection model
     */
    public ListSelectionModel getSubsetSelectionModel() {
        return subSelModel_;
    }

    /**
     * Returns a list of the indices of the subsets which have been 
     * selected.  As well as being in a different form, this actually
     * contains some information which is not given by
     * {@link #getSubsetSelectionModel}, since it stores the order
     * in which the subsets have been selected.  This can be reflected
     * in the plot.
     *
     * @return   array of subset indices
     */
    public int[] getOrderedSubsetSelection() {
        return subSelRecorder_.getOrderedSelection();
    }

    /**
     * Returns the toggle button used to select whether a grid will be
     * drawn or not.  The <code>GraphicsWindow</code> class has not placed
     * this button.  Its model can be manipulated.
     *
     * @return   grid toggle button
     */
    public JToggleButton getGridButton() {
        return gridButton_;
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
     * Redraws the plot, perhaps taking account of whether the plot state
     * has changed since last time it was done.
     *
     * @param  forcePlot  if true, do the replot in any case;
     *                    if false, only do it if the PlotState has changed
     * @param  forceData  if true, re-acquire data in any case;
     *                    if false, only do it if the data selection has changed
     */
    private void doReplot( boolean forcePlot, boolean forceData ) {
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
                try {
                    points_ = readPoints( state );
                }
                catch ( IOException e ) {
                    logger_.log( Level.SEVERE, "Error reading table data", e );
                    return;
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
     * Bugfix override.
     */
    public void setVisible( boolean visible ) {

        /* Invoke superclass method. */
        super.setVisible( visible );

        /* Don't know why, but this seems to be necessary at Java 1.5
         * otherwise the stackPanel appears with zero height, at least
         * the first time the window is made visible.
         * Possibly a 1.5 Swing bug. */
        if ( visible == true ) {
            stackPanel_.revalidate();
        }
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
        box.setRenderer( new ColumnCellRenderer( box ) );
        return box;
    }

    /**
     * Ensures that a subset with a given index is selected for display.
     *
     * @param  index  index into subsets list
     */
    public void selectSubset( int index ) {
        subSelModel_.addSelectionInterval( index, index );
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
    public Points readPoints( PlotState state ) throws IOException {
        int[] icols = new int[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            icols[ i ] = getColumnIndex( state.getColumns()[ i ] );
        }
        return Points.createPoints( tcModel_.getDataModel(), icols );
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
        EpsGraphics2D g2 = new EpsGraphics2D( tcModel_.getLabel(), ostrm,
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
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.SUBSET ) {
            RowSubset currentSet = tcModel_.getSelectedSubset();
            subSelModel_.setValueIsAdjusting( true );
            subSelModel_.clearSelection();
            int nrsets = subsets_.size();
            for ( int i = 0; i < nrsets; i++ ) {
                if ( subsets_.get( i ) == currentSet ) {
                    selectSubset( i );
                }
            }
            subSelModel_.setValueIsAdjusting( false );
        }
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
     * General purpose listener which replots given an event.
     */
    protected class ReplotListener implements ActionListener, ItemListener,
                                              ListSelectionListener {
        public void actionPerformed( ActionEvent evt ) {
            replot();
        }
        public void itemStateChanged( ItemEvent evt ) {
            replot();
        }
        public void valueChanged( ListSelectionEvent evt ) {
            replot();
        }
    }

}
