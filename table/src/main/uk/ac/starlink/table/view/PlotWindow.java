package uk.ac.starlink.table.view;

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
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ListSelectionModel;
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
import uk.ac.starlink.table.gui.StarTableColumn;

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
    private BitSet plottedRows = new BitSet();
    private PlotBox plot;
    private boolean showGrid = true;
    private String markStyle = "dots";

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
        JPanel configPanel = new JPanel();
        configPanel.add( xConfig );
        configPanel.add( yConfig );

        /* Add axis selectors for X and Y. */
        xColBox = new JComboBox();
        yColBox = new JComboBox();
        int nok = 0;
        for ( int i = 0; i < columnModel.getColumnCount(); i++ ) {
            StarTableColumn tcol = (StarTableColumn) columnModel.getColumn( i );
            ColumnInfo cinfo = tcol.getColumnInfo();
            int index = tcol.getModelIndex();
            if ( Number.class.isAssignableFrom( cinfo.getContentClass() ) ||
                 Date.class.isAssignableFrom( cinfo.getContentClass() ) ) {
                ColumnEntry colent = new ColumnEntry( cinfo, index );
                xColBox.addItem( colent );
                yColBox.addItem( colent );
                nok++;
            }
        }
        xConfig.add( xColBox );
        yConfig.add( yColBox );
        xColBox.setSelectedIndex( 0 );
        yColBox.setSelectedIndex( 1 );
        xColBox.addActionListener( this );
        yColBox.addActionListener( this );

        /* If there are no numeric columns then inform the user and bail out. */
        if ( nok == 0 )  {
            JOptionPane.showMessageDialog( null, "No numeric columns in table",
                                           "Plot error", 
                                           JOptionPane.ERROR_MESSAGE );
            dispose();
        }

        /* Add linear/log selectors for X and Y. */
        xLogBox = new JCheckBox( "Log plot" );
        yLogBox = new JCheckBox( "Log plot" );
        xConfig.add( xLogBox );
        yConfig.add( yLogBox );
        xLogBox.addActionListener( this );
        yLogBox.addActionListener( this );

        /* Arrange for new columns to be reflected in this window,
         * by adding them to the plot column selection boxes.  Don't bother 
         * changing things if a column is removed or moved though. */
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                TableColumnModel columnModel = PlotWindow.this.columnModel;
                assert columnModel == evt.getSource();
                StarTableColumn added = 
                    (StarTableColumn) columnModel.getColumn( evt.getToIndex() );
                int index = added.getModelIndex();
                ColumnInfo cinfo = added.getColumnInfo();
                if ( Number.class
                           .isAssignableFrom( cinfo.getContentClass() ) ||
                     Date.class
                         .isAssignableFrom( cinfo.getContentClass() ) ) {
                    ColumnEntry colent = new ColumnEntry( cinfo, index );
                    xColBox.addItem( colent );
                    yColBox.addItem( colent );
                }
            }
        } );

        /* Add a menu for printing the graph. */
        Action printAct = new BasicAction( "Print as EPS", 
                                           "Print the graph to an EPS file" ) {
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
                        JOptionPane
                       .showMessageDialog( parent, e.toString(),
                                           "Error writing EPS",
                                           JOptionPane.ERROR_MESSAGE );
                    }
                }
            }
        };
        fileMenu.add( new JMenuItem( printAct ), 0 );

        /* Get a menu for selecting row subsets to plot. */
        CheckBoxMenu subMenu = subsets.makeCheckBoxMenu( "Subsets to plot" );
        subSelModel = subMenu.getSelectionModel();

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

  //  The plot is not currently live - this would require a listener
  //  on the startable itself.  It may not really be desirable.
  //  Could want a replot button though.
  //
  //    /* Arrange for changes in relevant parts of the table data itself 
  //     * to cause a replot (this liveness of the plot should perhaps
  //     * be optional for performance reasons and just so that you know
  //     * where you are? */
  //    stmodel.addTableModelListener( new TableModelListener() {
  //        public void tableChanged( TableModelEvent evt ) {
  //            int icol = evt.getColumn();
  //            if ( evt.getFirstRow() == TableModelEvent.HEADER_ROW ||
  //                 icol == TableModelEvent.ALL_COLUMNS ||
  //                 icol == lastState.xCol.index ||
  //                 icol == lastState.yCol.index ) {
  //                lastState = null;
  //                actionPerformed( null );
  //            }
  //        }
  //    } );
      
        /* Construct a panel which will hold the plot itself. */
        plotPanel = new JPanel( new BorderLayout() );

        /* Arrange the components in the top level window. */
        JPanel mainArea = new JPanel( new BorderLayout() );
        mainArea.add( plotPanel, BorderLayout.CENTER );
        mainArea.add( configPanel, BorderLayout.SOUTH );
        getContentPane().add( mainArea, BorderLayout.CENTER );

        /* Add a progress bar. */
        progBar = new JProgressBar( JProgressBar.HORIZONTAL );
        getContentPane().add( progBar, BorderLayout.SOUTH );

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
                                "Select whether grid lines are displayed" ) {
            public void actionPerformed( ActionEvent evt ) {
                gridModel.setSelected( ! gridModel.isSelected() );
            }
        };

        /* Action for resizing the plot. */
        Action resizeAction = new BasicAction( "Fit to points",
                                           "Resize plot to show all points" ) {
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
        Action replotAction = new BasicAction( "Replot",
                                 "Redraw the plot with current table data" ) {
            public void actionPerformed( ActionEvent evt ) {
                PlotWindow.this.actionPerformed( FORCE_REPLOT );
            }
        };

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.add( resizeAction );
        plotMenu.add( replotAction );
        JMenuItem gridItem = new JCheckBoxMenuItem( gridAction );
        gridItem.setModel( gridModel );
        plotMenu.add( gridItem );
        plotMenu.add( markMenu );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.add( subMenu );
        subsetMenu.add( new BasicAction( "New subset from visible",
                                         "Define a new row subset containing " +
                                         "only currently plotted points" ) {
            public void actionPerformed( ActionEvent evt ) {
                String name = TableViewer.enquireSubsetName( PlotWindow.this );
                if ( name != null ) {
                    int inew = subsets.size();
                    subsets.add( new BitsRowSubset( name, calcVisibleRows() ) );
                    subSelModel.addSelectionInterval( inew, inew );
                }
            }
        } );
        getJMenuBar().add( subsetMenu );

        /* Do the plotting. */
        actionPerformed( FORCE_REPLOT );

        /* Render this component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Returns a plot component based on a given plotting state.
     *
     * @param  state  the PlotState determining plot characteristics
     * @return  a PlotBox component representing the plot
     */
    private PlotBox makePlot( PlotState state ) {
        int xcol = state.xCol.index;
        int ycol = state.yCol.index;
        ColumnInfo xColumn = state.xCol.info;
        ColumnInfo yColumn = state.yCol.info;
        RowSubset[] rsets = new RowSubset[ subsets.size() ];
        int nrsets = rsets.length;
        List useList = state.subsets;
        for ( int i = 0; i < nrsets; i++ ) {
            RowSubset rset = (RowSubset) subsets.getElementAt( i );
            rsets[ i ] = useList.contains( rset ) ? rset : null;
        }
        boolean[] inclusions = new boolean[ nrsets ];
        PlotBox plotbox;
        if ( state.type.equals( "Scatter" ) ) {
            Plot plot = new Plot();
            plot.setGrid( showGrid );
            plotbox = plot;
            for ( int i = 0; i < nrsets; i++ ) {
                RowSubset rset = rsets[ i ];
                if ( rset != null ) {
                    plot.addLegend( setFor( i ), rset.toString() );
                }
            }
            setMarkStyle( plot, markStyle );
            plot.setXLog( state.xLog );
            plot.setYLog( state.yLog );
            long nrow = dataModel.getRowCount();
            long ngood = 0;
            long nerror = 0;
            plottedRows.clear();
            for ( long lrow = 0; lrow < nrow; lrow++ ) {
                boolean any = false;
                for ( int i = 0; i < nrsets; i++ ) {
                    RowSubset rset = rsets[ i ];
                    boolean in = ( rset != null ) && rset.isIncluded( lrow );
                    inclusions[ i ] = in;
                    any = any || in;
                }
                if ( any ) {
                    try {
                        Object xval = dataModel.getCell( lrow, xcol );
                        Object yval = dataModel.getCell( lrow, ycol );
                        double x = doubleValue( xval );
                        double y = doubleValue( yval );
                        if ( ! Double.isNaN( x ) && ! Double.isNaN( y ) ) {
                            if ( state.xLog && x <= 0.0 ||
                                 state.yLog && y <= 0.0 ) {
                                // can't take log of negative value
                            }
                            else {
                                for ( int i = nrsets - 1; i >= 0; i-- ) {
                                    if ( inclusions[ i ] ) {
                                        plot.addPoint( setFor( i ), x, y,
                                                       state.plotline );
                                    }
                                }
                            }
                            ngood++;
                            plottedRows.set( (int) lrow );
                        }
                    }
                    catch ( IOException e ) {
                        nerror++;
                    }
                }
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

        /* Return. */
        return plotbox;
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
     * Returns a BitSet which corresponds to those rows which have are
     * currently visible; that is they were plotted and they fall within
     * the bounds of the curretly visible PlotBox.
     * 
     * @return  a vector of flags representing visible rows
     */
    private BitSet calcVisibleRows() {
        int xcol = lastState.xCol.index;
        int ycol = lastState.yCol.index;
        int nrow = (int) dataModel.getRowCount();
        double[] xr = plot.getXRange();
        double[] yr = plot.getYRange();
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

            /* Construct a new Plot object for the requested PlotState. */
            plot = makePlot( state );

            /* Configure the visible range of the plot.  If the axes are the
             * same as for the last plot, we will retain the last plot's range;
             * otherwise, fit to include all the data points. */
            if ( state.sameAxes( lastState ) ) {
                double[] xr = lastPlot.getXRange();
                double[] yr = lastPlot.getYRange();
                plot.setXRange( xr[ 0 ], xr[ 1 ] );
                plot.setYRange( yr[ 0 ], yr[ 1 ] );
            }
            else {
                plot.fillPlot();
            }

            /* Replace the old plot by the new one. */
            plotPanel.removeAll();
            plotPanel.add( plot, BorderLayout.CENTER );
            plotPanel.revalidate();
            lastPlot = plot;
            lastState = state;
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
     * Helper class to hold objects which represent columns in a 
     * combobox.
     */
    private static class ColumnEntry {
        int index;  // index in TableModel, not TableColumnModel
        ColumnInfo info;
        public ColumnEntry( ColumnInfo info, int index ) {
            this.info = info;
            this.index = index;
        }
        public String toString() { 
            return info.getName();
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
     * Returns the current PlotState of this window.
     *
     * @return  the current state
     */
    private PlotState getPlotState() {
        PlotState state = new PlotState();
        state.xCol = (ColumnEntry) xColBox.getSelectedItem();
        state.yCol = (ColumnEntry) yColBox.getSelectedItem();
        state.xLog = xLogBox.isSelected();
        state.yLog = yLogBox.isSelected();
        state.subsets = getSelectedSubsets();
        state.plotline = false;
        state.type = "Scatter";
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
        ColumnEntry xCol;
        ColumnEntry yCol;
        boolean xLog;
        boolean yLog;
        List subsets;
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
                    && subsets.equals( other.subsets )
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
                .append( "subsets=" )
                .append( subsets )
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
