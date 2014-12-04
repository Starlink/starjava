package uk.ac.starlink.ttools.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;

/**
 * Example programmatic use of stilts plot2 classes.
 * This program plots a number of points near a sinusoidal curve.
 * Optionally, the data can change and be replotted at time intervals.
 * Either way, the plot is "live"; you can pan and zoom round it using
 * the mouse.
 *
 * <p>Two alternative ways of setting up the plot are provided by way of
 * example, but they have the same effect.
 *
 * <p>To use this class invoke the main() method from the command line.
 * Use the -h flag for options.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2014
 */
public class SinePlot {

    private final int count_;
    private final double[] xs_;
    private final double[] ys_;
    private final StarTable table_;
    private PlanePlotter planePlotter_;

    /**
     * Constructor.
     *
     * @param  planePlotter  object that does the plot
     * @param  count  number of points to plot
     */
    public SinePlot( PlanePlotter planePlotter, int count ) throws Exception {
        planePlotter_ = planePlotter;
        count_ = count;

        /* Set up a table containing data to plot, backed by a double[] array
         * for each column.  Changing the values in these arrays in place
         * will change the content of the tables. */
        xs_ = new double[ count ];
        ys_ = new double[ count ];
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( count );
        table.addColumn( ArrayColumn.makeColumn( "x", xs_ ) );
        table.addColumn( ArrayColumn.makeColumn( "y", ys_ ) );
        table_ = table;

        /* Populate the table with some data. */
        updateTableData();
    }

    /**
     * Constructs a plot and posts it to the screen.
     *
     * @param   updateMillis  if positive, gives an interval in millisecnds
     *                        at which the plot should be refreshed;
     *                        if non-positive, the data is considered static
     */
    public void run( int updateMillis ) throws Exception {

        /* Determine whether we are going to be doing an animated plot
         * or a static one. */
        boolean dataWillChange = updateMillis >= 0;

        /* This does the work of turning the table into a plot. */
        final PlotDisplay plotComp =
            planePlotter_.createPlotComponent( table_, dataWillChange );

        /* If we are doing animation, set up a timer to change the table
         * data in place every few milliseconds.  For a static plot, 
         * you can ignore this. */
        if ( dataWillChange ) {
            new Timer( true ).schedule( new TimerTask() {
                public void run() {
                    updateTableData();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            plotComp.repaint();
                        }
                    } );
                }
            }, 0, updateMillis );
        }

        /* Post the plot component to the screen. */
        JFrame frame = new JFrame( SinePlot.class.getName() );
        frame.getContentPane().add( plotComp );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * Populates the underlying data of the table with some noisy values.
     */
    private void updateTableData() {
        double rc = 1. / count_;
        Random rnd = new Random();
        for ( int i = 0; i < count_; i++ ) {
            double x = i * rc;
            xs_[ i ] = x;
            ys_[ i ] = Math.sin( 2 * Math.PI * x ) + rnd.nextGaussian() * 0.1;
        }
    }

    /**
     * Abstracts the way that the table is turned into a 2d plot component.
     */
    public interface PlanePlotter {

        /**
         * Creates a JComponent holding a plot of the data in the first two
         * columns of the supplied table.
         * This plot is live, and can be resized (as required by Swing)
         * and panned and zoomed (by various mouse drag and wheel gestures).
         *
         * <p>The <code>dataMayChange</code> parameter is used to determine
         * whether the plot positions should be cached or not for repaints,
         * either as required by Swing move/resize actions or
         * as the result of the user navigating around the plot.
         * It's always safe to set it true, but if the data is static,
         * setting it false will give better performance.
         *
         * @param   table  table to plot
         * @param   dataMayChange  true if the table data may change during
         *                         the lifetime of the plot
         */
        PlotDisplay createPlotComponent( StarTable table,
                                         boolean dataMayChange )
            throws Exception;
    }

    /**
     * Main method.  Use with -help.
     */
    public static void main( String[] args ) throws Exception {
        String usage = new StringBuffer()
             .append( "\n   " )
             .append( "Usage: " )
             .append( "\n      " )
             .append( SinePlot.class.getName().replaceFirst( ".*\\.", "" ) )
             .append( " [-[no]move]" )
             .append( " [-api/-env]" )
             .append( " [-count npoint]" )
             .append( " [-verbose [-verbose]]" )
             .append( "\n" )
             .toString();
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        boolean move = true;
        boolean isApi = true;
        int count = 1000;
        int verbLevel = 0;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-move" ) ) {
                it.remove();
                move = true;
            }
            else if ( arg.equals( "-nomove" ) ) {
                it.remove();
                move = false;
            }
            else if ( arg.equals( "-api" ) ) {
                it.remove();
                isApi = true;
            }
            else if ( arg.equals( "-env" ) ) {
                it.remove();
                isApi = false;
            }
            else if ( arg.equals( "-count" ) ) {
                it.remove();
                count = Integer.parseInt( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-verbose" ) ) {
                it.remove();
                verbLevel++;
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return;
            }
        }
        if ( argList.size() > 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        int updateMillis = move ? 100 : -1;

        /* Configure logging level as requested by -verbose flags. */
        Logger.getLogger( "uk.ac.starlink.ttools" )
              .setLevel( new Level[] { Level.WARNING,
                                       Level.INFO,
                                       Level.CONFIG }[ verbLevel ] );

        /* Prepare an object which turns a table into a PlotDisplay component.
         * There are two choices, one which uses the MapEnvironment
         * and the other which uses the low-level API.  Both produce
         * just the same plot, it's a matter of taste which API you
         * prefer.  All the action (things you're likely to want to
         * use as a template for your own plots) is in the implementation
         * of these classes. */
        PlanePlotter planePlotter = isApi ? new ApiPlanePlotter()
                                          : new EnvPlanePlotter();
        new SinePlot( planePlotter, count ).run( updateMillis );
    }
}
