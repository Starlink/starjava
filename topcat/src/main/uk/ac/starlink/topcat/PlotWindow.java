package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
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
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import ptolemy.plot.Plot;
import ptolemy.plot.PlotBox;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.ProgressBarStarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Top level window which presents plots derived from a <tt>StarTable</tt>.
 * A number of plot configuration options are available to be configured
 * interactively by the user.
 *
 * @author   Mark Taylor (Starlink)
 */
public class PlotWindow extends AuxWindow implements ActionListener {

    private TableViewer tv;
    private StarTable dataModel;
    private TableColumnModel columnModel;
    private OptionsListModel subsets;
    private JComboBox xColBox;
    private JComboBox yColBox;
    private JCheckBox xLogBox;
    private JCheckBox yLogBox;
    private ListSelectionModel subSelModel;
    private JPanel plotPanel;
    private JProgressBar progBar;
    private JFileChooser printSaver;
    private PlotState lastState;
    private PlotBox lastPlot;
    private BitSet plottedRows;
    private ScatterPlotter activePlotter;
    private boolean showGrid = true;
    private String markStyle = "dots";
    private Action fromvisibleAction;

    private static final double MILLISECONDS_PER_YEAR 
                              = 365.25 * 24 * 60 * 60 * 1000;
    private static final ActionEvent FORCE_REPLOT = 
        new ActionEvent( new Object(), 0, null );

    /**
     * Constructs a PlotWindow for a given <tt>TableModel</tt> and 
     * <tt>TableColumnModel</tt>.
     *
     * @param   tableviewer  the viewer whose data are to be plotted
     */
    public PlotWindow( TableViewer tableviewer ) {
        super( "Table Plotter", tableviewer );
        this.tv = tableviewer;
        this.dataModel = tv.getDataModel();
        this.columnModel = tv.getColumnModel();
        this.subsets = tv.getSubsets();

        /* Do some window setup. */
        setSize( 400, 400 );

        /* Construct a panel for configuration of X and Y axes. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        JPanel xConfig = new JPanel();
        JPanel yConfig = new JPanel();
        xConfig.setBorder( BorderFactory
                          .createTitledBorder( lineBorder, "X axis" ) );
        yConfig.setBorder( BorderFactory
                          .createTitledBorder( lineBorder, "Y axis" ) );
        Box axisBox = new Box( BoxLayout.Y_AXIS );
        axisBox.add( xConfig );
        axisBox.add( Box.createGlue() );
        axisBox.add( yConfig );
        getControlPanel().add( axisBox );

        /* Construct axis selectors for X and Y. */
        xColBox = makePlottableColumnComboBox();
        yColBox = makePlottableColumnComboBox();

        /* If there are too few numeric columns then inform the user and
         * bail out. */
        assert xColBox.getItemCount() == yColBox.getItemCount();
        if ( xColBox.getItemCount() < 2 ) {
            JOptionPane.showMessageDialog( null,
                                           "Too few numeric columns in table",
                                           "Plot error", 
                                           JOptionPane.ERROR_MESSAGE );
            dispose();
        }

        /* Place the selectors in this window. */
        xConfig.add( xColBox );
        yConfig.add( yColBox );
        xColBox.setSelectedIndex( 0 );
        yColBox.setSelectedIndex( 1 );
        xColBox.addActionListener( this );
        yColBox.addActionListener( this );

        /* Add linear/log selectors for X and Y. */
        xLogBox = new JCheckBox( "Log plot" );
        yLogBox = new JCheckBox( "Log plot" );
        xConfig.add( xLogBox );
        yConfig.add( yLogBox );
        xLogBox.addActionListener( this );
        yLogBox.addActionListener( this );

        /* Add a menu for printing the graph. */
        Action printAction = new BasicAction( "Print as EPS",
                                              ResourceIcon.PRINT,
                                              "Print the graph to an " +
                                              "EPS file" ) {
            public void actionPerformed( ActionEvent evt ) {

                /* Ask the user where to output the postscript. */
                Component parent = PlotWindow.this;
                JFileChooser chooser = getPrintSaver();
                if ( chooser.showSaveDialog( parent ) 
                     == JFileChooser.APPROVE_OPTION ) {
                    try {
                        File file = chooser.getSelectedFile();
                        OutputStream ostrm = new FileOutputStream( file );

                        /* Only one mark style makes much sense for the 
                         * EPSGraphics class used here, so change the plot 
                         * to that for the duration of the write, then
                         * back again. */
                        setMarkStyle( lastPlot, "various" );
                        try {
                            lastPlot.export( ostrm );
                        }
                        finally {
                            setMarkStyle( lastPlot, markStyle );
                            ostrm.close();
                        }
                    }
                    catch ( IOException e ) {
                        ErrorDialog.showError( e, "Error writing EPS", parent );
                    }
                }
            }
        };
        getFileMenu().insert( printAction, 0 ).setIcon( null );

        /* Get a menu for selecting row subsets to plot. */
        CheckBoxMenu subMenu = subsets.makeCheckBoxMenu( "Subsets to plot" );
        subSelModel = subMenu.getSelectionModel();

        /* Do the same thing as a scrollable box in the control panel. */
        CheckBoxStack stack = new CheckBoxStack( subsets );
        JComponent stackPanel = new JScrollPane( stack );
        stackPanel.setBorder( BorderFactory
                             .createTitledBorder( lineBorder, "Row subsets" ) );
        getControlPanel().add( stackPanel );
        stack.setSelectionModel( subSelModel );

        /* Initialise its selections so that both ALL and the current subset,
         * of the viewer, if any, are plotted. */
        subSelModel.addSelectionInterval( 0, 0 );  // ALL
        int nrsets = subsets.size();
        RowSubset currentSet = tv.getViewModel().getSubset();
        if ( currentSet != RowSubset.ALL ) {
            for ( int i = 1; i < nrsets; i++ ) {
                if ( subsets.get( i ) == currentSet ) {
                    subSelModel.addSelectionInterval( i, i );
                }
            }
        }

        /* Ensure that the plot will respond to the subset selection being
         * changed. */
        subSelModel.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                PlotWindow.this.actionPerformed( null );
            }
        } );

        /* Construct a panel which will hold the plot itself. */
        plotPanel = new JPanel( new BorderLayout() );

        /* Arrange the components in the top level window. */
        JPanel mainArea = getMainArea();
        mainArea.add( plotPanel, BorderLayout.CENTER );

        /* Add a progress bar. */
        progBar = placeProgressBar();

        /* Action for showing the grid. */
        final ButtonModel gridModel = new DefaultButtonModel();
        gridModel.setSelected( showGrid );
        gridModel.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( gridModel.isSelected() != showGrid ) {
                    showGrid = ! showGrid;
                    lastPlot.setGrid( showGrid );
                }
                lastPlot.repaint();
            }
        } );
        Action gridAction = new BasicAction( "Show grid",
                                             ResourceIcon.GRID_ON,
                                             "Select whether grid " +
                                             "lines are displayed" ) {
            public void actionPerformed( ActionEvent evt ) {
                gridModel.setSelected( ! gridModel.isSelected() );
            }
        };
        final AbstractButton gridToolButton = new JButton( gridAction );
        gridToolButton.setText( null );
        gridModel.addItemListener( new ItemListener() {
            { itemStateChanged( null ); }
            public void itemStateChanged( ItemEvent evt ) {
                gridToolButton.setIcon( gridModel.isSelected() 
                                            ? ResourceIcon.GRID_OFF
                                            : ResourceIcon.GRID_ON );
            }
        } );

        /* Action for resizing the plot. */
        Action resizeAction = new BasicAction( "Rescale",
                                               ResourceIcon.RESIZE,
                                               "Rescale the plot to show " +
                                               "all points points" ) {
            public void actionPerformed( ActionEvent evt ) {
                lastPlot.fillPlot();
            }
        };

        /* Menu item for selecting marker types. */
        JMenu markMenu = new JMenu( "Marker type" );
        String[] markStyles = 
            new String[] { "points", "dots", "various", "pixels" };
        ButtonGroup bgroup = new ButtonGroup();
        for ( int i = 0; i < markStyles.length; i++ ) {
            String style = markStyles[ i ];
            Action act = new MarkerAction( style );
            JMenuItem item = new JRadioButtonMenuItem( act );
            if ( style.equals( this.markStyle ) ) {
                item.setSelected( true );
            }
            markMenu.add( item );
            bgroup.add( item );
        }

        /* Action for repdrawing the current plot. */
        Action replotAction = new BasicAction( "Replot", ResourceIcon.REDO,
                                 "Redraw the plot with current table data" ) {
            public void actionPerformed( ActionEvent evt ) {
                PlotWindow.this.actionPerformed( FORCE_REPLOT );
            }
        };

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.add( resizeAction ).setIcon( null );
        plotMenu.add( replotAction ).setIcon( null );
        JMenuItem gridItem = new JCheckBoxMenuItem( gridAction );
        gridItem.setModel( gridModel );
        plotMenu.add( gridItem ).setIcon( null );
        plotMenu.add( markMenu );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.add( subMenu );
        fromvisibleAction = new BasicAction( "New subset from visible",
                                             ResourceIcon.VISIBLE_SUBSET,
                                             "Define a new row subset " +
                                             "containing only currently " +
                                             "visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                String name = TableViewer.enquireSubsetName( PlotWindow.this );
                if ( name != null ) {
                    int inew = subsets.size();
                    subsets.add( new BitsRowSubset( name, calcVisibleRows() ) );
                    subSelModel.addSelectionInterval( inew, inew );
                }
            }
        };
        subsetMenu.add( fromvisibleAction ).setIcon( null );
        getJMenuBar().add( subsetMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( printAction );
        getToolBar().add( replotAction );
        getToolBar().add( resizeAction );
        getToolBar().add( gridToolButton );
        getToolBar().add( fromvisibleAction );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "PlotWindow" );

        /* Do the plotting. */
        actionPerformed( FORCE_REPLOT );

        /* Render this component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Constructs, installs and fills a plot component based on a given
     * plotting state.
     */
    private void makePlot( PlotState state, double[] xrange, double[] yrange ) {

        /* If there is already a plotter doing plotting, stop it. */
        if ( activePlotter != null ) {
            activePlotter.interrupt();
        }

        /* Interrogate the plot state. */
        int xcol = getColumnIndex( state.xCol );
        int ycol = getColumnIndex( state.yCol );
        ColumnInfo xColumn = state.xCol.getColumnInfo();
        ColumnInfo yColumn = state.yCol.getColumnInfo();
        RowSubset[] rsets = state.subsetMask;
        int nrsets = rsets.length;
        boolean autoSize = xrange == null || yrange == null;

        /* Configure the plot. */
        PlotBox plotbox;
        if ( state.type.equals( "Scatter" ) ) {
            Plot plot = new Plot();
            plot.setGrid( showGrid );
            plotbox = plot;
            for ( int i = 0; i < nrsets; i++ ) {
                RowSubset rset = rsets[ i ];
                if ( rset != null ) {
                    plot.addLegend( setFor( i ), rset.getName() );
                }
            }
            setMarkStyle( plot, markStyle );
            plot.setXLog( state.xLog );
            plot.setYLog( state.yLog );
            if ( ! autoSize ) {
                plot.setXRange( xrange[ 0 ], xrange[ 1 ] );
                plot.setYRange( yrange[ 0 ], yrange[ 1 ] );
            }
        }
        else {
            throw new AssertionError( "Unknown plot type" );
        }

        /* Generic configuration. */
        plotbox.setTitle( dataModel.getName() );

        /* Axis labels. */
        String xName = xColumn.getName();
        String yName = yColumn.getName();
        String xUnit = xColumn.getUnitString();
        String yUnit = yColumn.getUnitString();
        String xLabel = xName;
        String yLabel = yName;
        if ( xUnit != null && xUnit.trim().length() > 0 ) {
            xLabel = xName + " / " + xUnit;
        }
        if ( yUnit != null && yUnit.trim().length() > 0 ) {
            yLabel = yName + " / " + yUnit;
        }
        plotbox.setXLabel( xLabel );
        plotbox.setYLabel( yLabel );

        /* Replace any old plot by the new one. */
        plotPanel.removeAll();
        plotPanel.add( plotbox, BorderLayout.CENTER );
        plotPanel.revalidate();
        lastPlot = plotbox;
        lastState = state;

        /* Initiate plotting of the points. */
        if ( state.type.equals( "Scatter" ) ) {
            activePlotter = new ScatterPlotter( state, (Plot) plotbox, 
                                                autoSize );
        }
        else {
            throw new AssertionError();
        }
        activePlotter.start();
    }

    /**
     * Maps a subset index to the index of a dataset to use in the
     * plot.
     *
     * @param   rsetIndex  index of the RowSubset
     * @return  corresponding dataset index
     */
    private int setFor( int rsetIndex ) {
        if ( rsetIndex <= 9 ) {
            return 9 - rsetIndex;
        }
        else {
            return rsetIndex;
        }
    }

    /**
     * Records the bitset which contains the set of points plotted on 
     * the currently visible plotting surface.
     *
     * @param  prows  a BitSet representing all rows in any of the 
     *         currently selected row subsets.  This should include only
     *         those which have actually appeared on the plotting surface,
     *         though not necesarily in its visible bounds.
     *         May be null.
     */
    public void setPlottedRows( BitSet prows ) {
        plottedRows = prows;
        fromvisibleAction.setEnabled( plottedRows != null );
    }

    /**
     * Returns a BitSet which corresponds to those rows which have are
     * currently visible; that is they were plotted and they fall within
     * the bounds of the curretly visible PlotBox.
     * 
     * @return  a vector of flags representing visible rows
     */
    private BitSet calcVisibleRows() {

        /* The list of plotted rows shouldn't be null, but it might just
         * be if the timing is unlucky - return an empty list. */
        if ( plottedRows == null ) {
            return new BitSet();
        }

        /* Otherwise work out the proper answer. */
        int xcol = getColumnIndex( lastState.xCol );
        int ycol = getColumnIndex( lastState.yCol );
        int nrow = (int) dataModel.getRowCount();
        double[] xr = lastPlot.getXRange();
        double[] yr = lastPlot.getYRange();
        double x0 = xr[ 0 ];
        double y0 = yr[ 0 ];
        double x1 = xr[ 1 ];
        double y1 = yr[ 1 ];
        BitSet visibleRows = new BitSet();
        try {
            for ( int irow = 0; irow < nrow; irow++ ) {
                long lrow = (long) irow;
                if ( plottedRows.get( irow ) ) {
                    Object xval = dataModel.getCell( lrow, xcol );
                    Object yval = dataModel.getCell( lrow, ycol );
                    double x = doubleValue( xval );
                    double y = doubleValue( yval );
                    if ( x >= x0 && x <= x1 && y >= y0 && y <= y1 ) {
                        visibleRows.set( irow );
                    }
                }
            }
        }
        catch ( IOException e ) {
            // oh well
        }
        return visibleRows;
    }

    /**
     * Returns a list of all the RowSubsets which are currently selected
     * for plotting in this window.
     *
     * @return  a List of RowSubsets
     */
    public List getSelectedSubsets() {
        int min = subSelModel.getMinSelectionIndex();
        int max = subSelModel.getMaxSelectionIndex();
        List selected = new ArrayList();
        if ( min >= 0 ) {  // selection not empty
            assert max >= 0;
            for ( int i = min; i <= max; i++ ) {
                if ( subSelModel.isSelectedIndex( i ) ) {
                    selected.add( subsets.get( i ) );
                }
            }
        }
        return selected;
    }

    /**
     * Returns a new JComboBox from which can be selected any of the
     * columns of the table which can be plotted.
     * This box will be updated when new columns are added to the 
     * table model and so on.
     *
     * @param  combo box
     */
    public JComboBox makePlottableColumnComboBox() {

        /* Construct a model which contains an entry for each column 
         * which contains Numbers or Dates. */
        ComboBoxModel boxModel = 
            new RestrictedColumnComboBoxModel( tv.getColumnModel(), false ) {
                protected boolean acceptColumn( TableColumn tcol ) {
                    StarTableColumn stcol = (StarTableColumn) tcol;
                    Class clazz = stcol.getColumnInfo().getContentClass();
                    return Number.class.isAssignableFrom( clazz )
                        || Date.class.isAssignableFrom( clazz );
                }
            };

        /* Create a new combobox. */
        JComboBox box = new JComboBox( boxModel );

        /* Give it a suitable renderer. */
        box.setRenderer( tv.getColumnRenderer() );
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
     * This method is called whenever something happens which may cause
     * the plot to need to be updated.
     *
     * @param  evt  ignored
     */
    public void actionPerformed( ActionEvent evt ) {

        /* Action is only required if the requested state of the plot has
         * changed since last time we plotted it. */
        PlotState state = getPlotState();
        if ( ! state.equals( lastState ) || evt == FORCE_REPLOT ) {

            /* Configure the visible range of the plot.  If the axes are the
             * same as for the last plot, we will retain the last plot's range;
             * otherwise, fit to include all the data points. */
            double[] xrange;
            double[] yrange;
            if ( state.sameAxes( lastState ) ) {
                xrange = lastPlot.getXRange();
                yrange = lastPlot.getYRange();
            }
            else {
                xrange = null;
                yrange = null;
            }
            makePlot( state, xrange, yrange );
        }
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

    /**
     * Action implementation for changing the markers used in plotting.
     */
    private class MarkerAction extends BasicAction {
        private String style;
        MarkerAction( String style ) {
            super( style, "Change point markers to style " + style );
            this.style = style;
        }
        public void actionPerformed( ActionEvent evt ) {
            markStyle = style;
            setMarkStyle( lastPlot, style );
            lastPlot.repaint();
        }
    }

    /**
     * Sets the style of all the markers in a given plot to a given type.
     * This will only have an affect if the plot in question is a Plot 
     * object; a Histogram for instance has no markers to affect.
     *
     * @param  plotbox  the plot to affect
     * @param  style  the name of the style, as per the 
     *         {@link ptolemy.plot.Plot#setMarksStyle} method
     */
    private void setMarkStyle( PlotBox plotbox, String style ) {
        if ( plotbox instanceof Plot ) {
            Plot plot = (Plot) plotbox;
            int nset = plot.getNumDataSets();
            for ( int i = 0; i < nset; i++ ) {
                plot.setMarksStyle( style, i );
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
        if ( printSaver == null ) {
            printSaver = new JFileChooser( "." );
            printSaver.setAcceptAllFileFilterUsed( true );
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
            printSaver.addChoosableFileFilter( psFilter );
            printSaver.setFileFilter( psFilter );
        }
        return printSaver;
    }

    /**
     * Extends the dispose method to interrupt any pending calculations.
     */
    public void dispose() {
        super.dispose();
        if ( activePlotter != null ) {
            activePlotter.interrupt();
            activePlotter = null;
        }
    }

    /**
     * This class does the work of turning points in the table data model
     * into points in a scatter graph.  The table reading and point posting
     * is done in a separate thread.
     */
    private class ScatterPlotter extends Thread {

        PlotState state;
        Plot plot;
        boolean autoSize;
        BitSet plottedRows = new BitSet();

        /**
         * Construct a new plotter object.
         *
         * @param  state  the state which controls how the plot is to be done
         * @param  plot  the Plot object into which the points must be put
         * @param  autoSize  true iff the plot should be resized to fit the
         *         plotted points
         */
        public ScatterPlotter( PlotState state, Plot plot, boolean autoSize ) {
            this.state = state;
            this.plot = plot;
            this.autoSize = autoSize;
        }

        /**
         * Invokes the plotting routine.  Also sets the busy cursor as
         * appropriate at the start/end of the calculation, as long as
         * this plotter has not been superceded as the active plotter
         * of the window.
         */
        public void run() {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ScatterPlotter.this == activePlotter ) {
                        setBusy( true );
                    }
                }
            } );
            try {
                setPlottedRows( null );
                doPlotting();
                if ( autoSize ) {
                    plot.fillPlot();
                }
                setPlottedRows( ScatterPlotter.this.plottedRows );
            }
            catch ( IOException e ) {
                // no action
            }
            finally {
                plot.repaint();
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( ScatterPlotter.this == activePlotter ) {
                            activePlotter = null;
                            setBusy( false );
                        }
                    }
                } );
            }
        }

        /**
         * Actually does the adding of points to the plot.
         */
        private void doPlotting() throws IOException {

            /* Obtain the things we need to know from the plot state. */
            int xcol = getColumnIndex( state.xCol );
            int ycol = getColumnIndex( state.yCol );
            boolean xLog = state.xLog;
            boolean yLog = state.yLog;
            boolean plotline = state.plotline;
            RowSubset[] rsets = state.subsetMask;
            int nrsets = rsets.length;
            boolean[] inclusions = new boolean[ nrsets ];

            /* Iterate over the rows in the table. */
            long ngood = 0;
            long nerror = 0;
            RowSequence rseq = new ProgressBarStarTable( dataModel, progBar )
                              .getRowSequence();
            long sincePaint = new Date().getTime();
            for ( long lrow = 0; rseq.hasNext(); lrow++ ) {

                /* Move to the next row.  Note this may throw an IOException
                 * due to thread interruption (see ProgressBarStarTable). */
                rseq.next();

                /* See which subsets need to be plotted for this row. */
                boolean any = false;
                for ( int i = 0; i < nrsets; i++ ) {
                    RowSubset rset = rsets[ i ];
                    boolean in = ( rset != null ) && rset.isIncluded( lrow );
                    inclusions[ i ] = in;
                    any = any || in;
                }
                if ( any ) {
                    Object xval = rseq.getCell( xcol );
                    Object yval = rseq.getCell( ycol );
                    double x = doubleValue( xval );
                    double y = doubleValue( yval );
                    if ( ! Double.isNaN( x ) && ! Double.isNaN( y ) ) {
                        if ( xLog && x < 0.0 || yLog && y < 0.0 ) {
                            // can't take log of negative value
                        }
                        else {
                            for ( int i = nrsets - 1; i >= 0; i-- ) {
                                if ( inclusions[ i ] ) {
                                    plot.addPoint( setFor( i ), x, y,
                                                   plotline );
                                }
                            }
                            ngood++;
                            plottedRows.set( (int) lrow );
                        }
                    }
                }

                /* If we are autosizing this plot, make sure that we 
                 * haven't gone too long without a repaint, since this
                 * ensures that the visible region contains all the points. */
                if ( autoSize ) {
                    long now = new Date().getTime();
                    if ( now - sincePaint > 1000L ) {
                        plot.fillPlot();
                        sincePaint = now;
                    }
                }
            }
        }
    }


    /**
     * Returns the current PlotState of this window.
     *
     * @return  the current state
     */
    private PlotState getPlotState() {
        PlotState state = new PlotState();
        state.xCol = (StarTableColumn) xColBox.getSelectedItem();
        state.yCol = (StarTableColumn) yColBox.getSelectedItem();
        state.xLog = xLogBox.isSelected();
        state.yLog = yLogBox.isSelected();
        state.plotline = false;
        state.type = "Scatter";

        /* Construct an array of the subsets that are used. */
        int nrsets = subsets.size();
        RowSubset[] subsetMask = new RowSubset[ nrsets ];
        List useList = getSelectedSubsets();
        for ( int i = 0; i < nrsets; i++ ) {
            RowSubset rset = (RowSubset) subsets.getElementAt( i );
            subsetMask[ i ] = useList.contains( rset ) ? rset : null;
        }
        state.subsetMask = subsetMask;
        return state;
    }

    /**
     * Private class for characterising the state of a plot.
     * An <tt>equals</tt> comparison is used to find out whether two
     * states of this window differ in such a way as to require a 
     * new plot to be made.
     * <p>
     * Note this is not designed for general purpose use; in particular the
     * <tt>hashCode</tt> method is not re-implemented for consistency with 
     * the <tt>equals</tt> method.
     */
    private static class PlotState {
        StarTableColumn xCol;
        StarTableColumn yCol;
        boolean xLog;
        boolean yLog;
        RowSubset[] subsetMask;
        boolean plotline;
        String type;

        public boolean equals( Object otherObject ) {
            if ( otherObject instanceof PlotState ) {
                PlotState other = (PlotState) otherObject;
                return other instanceof PlotState 
                    && xCol.equals( other.xCol )
                    && yCol.equals( other.yCol )
                    && xLog == other.xLog
                    && yLog == other.yLog
                    && Arrays.equals( subsetMask, other.subsetMask )
                    && plotline == other.plotline
                    && type.equals( other.type );
            }
            else {
                return false;
            }
        }

        public boolean sameAxes( PlotState other ) {
            return other != null
                && xCol.equals( other.xCol )
                && yCol.equals( other.yCol )
                && xLog == other.xLog
                && yLog == other.yLog;
        }

        public String toString() {
            return new StringBuffer()
                .append( "xCol=" )
                .append( xCol )
                .append( "," )
                .append( "yCol=" )
                .append( yCol )
                .append( "," )
                .append( "xLog=" )
                .append( xLog )
                .append( "," )
                .append( "yLog=" )
                .append( yLog )
                .append( "," )
                .append( "plotline=" )
                .append( plotline )
                .append( "," )
                .append( "type=" )
                .append( type )
                .toString();
        }
    }

}
