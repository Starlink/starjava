package uk.ac.starlink.table.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
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

    private StarTable stable;
    private TableColumnModel tcmodel;
    private JComboBox xColBox;
    private JComboBox yColBox;
    private JCheckBox xLogBox;
    private JCheckBox yLogBox;
    private JComboBox subCombo;
    private JPanel plotPanel;
    private PlotState lastState;
    private BitSet plottedRows = new BitSet();
    private PlotBox plot;

    private static final double MILLISECONDS_PER_YEAR 
                              = 365.25 * 24 * 60 * 60 * 1000;

    /**
     * Constructs a PlotWindow for a given <tt>TableModel</tt> and 
     * <tt>TableColumnModel</tt>.
     *
     * @param   stable  the StarTable which to plot columns
     * @param   tcmodel  the TableColumnModel
     * @param   subsets the list of known row subsets
     * @param   parent  the parent component
     */
    public PlotWindow( StarTable stable, TableColumnModel tcmodel,
                       final OptionsListModel subsets, Component parent ) {
        super( "Table Plotter", stable, parent );
        this.stable = stable;
        this.tcmodel = tcmodel;

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
        for ( int i = 0; i < tcmodel.getColumnCount(); i++ ) {
            StarTableColumn tcol = (StarTableColumn) tcmodel.getColumn( i );
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
        tcmodel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                TableColumnModel tcmodel = PlotWindow.this.tcmodel;
                assert tcmodel == evt.getSource();
                StarTableColumn added = (StarTableColumn)
                                        tcmodel.getColumn( evt.getToIndex() );
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

        /* Construct a component for a row subset selectors. */
        JPanel sConfig = new JPanel();
        sConfig.setBorder( BorderFactory
                          .createTitledBorder( lineBorder, "Row subset" ) );
        subCombo = new JComboBox( subsets.makeComboBoxModel() );
        subCombo.setPreferredSize( 
            new Dimension( 110, subCombo.getPreferredSize().height ) );
        subCombo.setSelectedItem( RowSubset.ALL );
        subCombo.addActionListener( this );
        sConfig.add( subCombo );
        configPanel.add( sConfig );

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
        Container cp = getContentPane();
        cp.add( plotPanel, BorderLayout.CENTER );
        cp.add( configPanel, BorderLayout.SOUTH );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.add( new BasicAction( "New subset from visible",
                                         "Define a new row subset containing " +
                                         "only currently plotted points" ) {
            public void actionPerformed( ActionEvent evt ) {
                String name = TableViewer.enquireSubsetName( PlotWindow.this );
                if ( name != null ) {
                    subsets.add( new BitsRowSubset( name, calcVisibleRows() ) );
                }
            }
        } );

  // doesn't work properly
  //    Action applysubsetAct = new AbstractAction() {
  //        public void actionPerformed( ActionEvent evt ) {
  //            int index = evt.getID();
  //            subCombo.setSelectedIndex( index );
  //            PlotWindow.this.actionPerformed( null );
  //        }
  //    };
  //    JMenu applysubsetMenu = 
  //        subsets.makeJMenu( "Apply subset", applysubsetAct );
  //    subsetMenu.add( applysubsetMenu );

        getJMenuBar().add( subsetMenu );

        /* Do the plotting. */
        actionPerformed( null );

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
        RowSubset rset = state.subset;
        PlotBox plotbox;
        if ( state.type.equals( "Scatter" ) ) {
            Plot plot = new Plot();
            plotbox = plot;
            plot.setMarksStyle( "dots", 0 );
            plot.setXLog( state.xLog );
            plot.setYLog( state.yLog );
            long nrow = stable.getRowCount();
            long ngood = 0;
            long nerror = 0;
            plottedRows.clear();
            for ( long lrow = 0; lrow < nrow; lrow++ ) {
                if ( rset == null || rset.isIncluded( lrow ) ) {
                    try {
                        Object xval = stable.getCell( lrow, xcol );
                        Object yval = stable.getCell( lrow, ycol );
                        double x = doubleValue( xval );
                        double y = doubleValue( yval );
                        if ( ! Double.isNaN( x ) && ! Double.isNaN( y ) ) {
                            if ( state.xLog && x <= 0.0 ||
                                 state.yLog && y <= 0.0 ) {
                                // can't take log of negative value
                            }
                            else {
                                plot.addPoint( 0, x, y, state.plotline );
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
        plotbox.setTitle( stable.getName() );

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

        /* Set the visible range to the range of the data. */
        plotbox.fillPlot();

        /* Return. */
        return plotbox;
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
        int nrow = (int) stable.getRowCount();
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
                    Object xval = stable.getCell( lrow, xcol );
                    Object yval = stable.getCell( lrow, ycol );
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
     * This method is called whenever something happens which may cause
     * the plot to need to be updated.
     *
     * @param  evt  ignored
     */
    public void actionPerformed( ActionEvent evt ) {
        PlotState state = getPlotState();
        if ( ! state.equals( lastState ) ) {
            lastState = state;
            plotPanel.removeAll();
            plot = makePlot( state );
            plotPanel.add( plot, BorderLayout.CENTER );
            plotPanel.revalidate();
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
        state.subset = (RowSubset) subCombo.getSelectedItem();
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
        RowSubset subset;
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
                    && subset.equals( other.subset )
                    && plotline == other.plotline
                    && type.equals( other.type );
            }
            else {
                return false;
            }
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
                .append( "subset=" )
                .append( subset )
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
