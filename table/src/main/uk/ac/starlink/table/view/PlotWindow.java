package uk.ac.starlink.table.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import ptolemy.plot.Plot;
import ptolemy.plot.PlotBox;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Top level window which presents plots derived from a <tt>StarTable</tt>.
 * A number of plot configuration options are available to be configured
 * interactively by the user.
 *
 * @author   Mark Taylor (Starlink)
 */
public class PlotWindow extends JFrame implements ActionListener {

    private StarTable startab;
    private JComboBox xColBox;
    private JComboBox yColBox;
    private JCheckBox xLogBox;
    private JCheckBox yLogBox;
    private JPanel plotPanel;
    private PlotState lastState;

    /**
     * Constructs a PlotWindow from a given StarTable.
     *
     * @param   startab  the startable from which to plot columns
     * @param   title   the title of the window
     */
    public PlotWindow( StarTable startab ) {
        super();
        this.startab = startab;

        /* Do some window setup. */
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setSize( 400, 400 );
        if ( startab.getName() != null ) {
            setTitle( "Table Plotter: " + startab.getName() );
        }
        else {
            setTitle( "Table Plotter" );
        }

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
        for ( int i = 0; i < startab.getColumnCount(); i++ ) {
            ColumnInfo cinfo = startab.getColumnInfo( i );
            if ( Number.class.isAssignableFrom( cinfo.getContentClass() ) ) {
                ColumnEntry colent = new ColumnEntry( cinfo, i );
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

        /* Construct a panel which will hold the plot itself. */
        plotPanel = new JPanel( new BorderLayout() );

        /* Arrange the components in the top level window. */
        Container cp = getContentPane();
        cp.add( plotPanel, BorderLayout.CENTER );
        cp.add( configPanel, BorderLayout.SOUTH );

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
        PlotBox plotbox;
        if ( state.type.equals( "Scatter" ) ) {
            Plot plot = new Plot();
            plotbox = plot;
            plot.setMarksStyle( "dots", 0 );
            plot.setXLog( state.xLog );
            plot.setYLog( state.yLog );
            try {
                int nrow = 0;
                int ngood = 0;
                for ( RowSequence rseq = startab.getRowSequence();
                      rseq.hasNext(); ) {
                    rseq.next();
                    Object xval = rseq.getCell( xcol );
                    Object yval = rseq.getCell( ycol );
                    if ( xval instanceof Number &&
                         yval instanceof Number ) {
                        plot.addPoint( 0, ( (Number) xval ).doubleValue(),
                                          ( (Number) yval ).doubleValue(),
                                          state.plotline );
                        ngood++;
                    }
                    nrow++;
                }
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        else {
            throw new AssertionError( "Unknown plot type" );
        }

        /* Generic configuration. */
        plotbox.setTitle( startab.getName() );

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
            plotPanel.add( makePlot( state ), BorderLayout.CENTER );
            plotPanel.revalidate();
        }
    }

    /**
     * Helper class to hold objects which represent columns in a 
     * combobox.
     */
    private static class ColumnEntry {
        int index;
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
                .append( "plotline=" )
                .append( plotline )
                .append( "," )
                .append( "type=" )
                .append( type )
                .toString();
        }
    }

}
