package uk.ac.starlink.ttools.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.server.PlotServlet;

/**
 * Utility methods for writing JupyterNotebooks that can be used with
 * the plot server.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2020
 */
public class Plot2Notebook {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.build" );

    /** Resource containing plotserv.py. */
    public static final String PLOTSERV_PY_RESOURCE =
        "/uk/ac/starlink/ttools/server/plotserv.py";

    /** JupyterCell instance that sets up {@link #PLOTWORDS_FUNC}. */
    public static final JupyterCell PLOT_CELL =
        new JupyterCell( readLines( PLOTSERV_PY_RESOURCE ) );

    /**
     * Name of python function that will yield a plot, taking only a
     * single argument which is a string array of the form
     * ["<taskname>", "<param>=<value>", "<param>=<value>", ...].
     * <strong>Must match content of {@link #PLOTSERV_PY_RESOURCE}.</strong>
     */
    public static final String PLOTWORDS_FUNC = "plot";

    /**
     * Private constructor prevents instantiation.
     */
    private Plot2Notebook() {
    }

    private static List<String> readLines( String resource ) {
        URL url = Plot2Notebook.class.getResource( resource );
        List<String> lines = new ArrayList<>();
        if ( url == null ) {
            logger_.warning( "Missing local resource: " + resource );
        }
        try {
            BufferedReader rdr =
                new BufferedReader( new InputStreamReader( url.openStream(),
                                                           "UTF-8" ) );
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                lines.add( line );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING,
                         "Error reading local resource: " + resource, e );
        }
        return lines;
    }

    /**
     * Creates a cell that will generate a plot.
     *
     * @param  words  arguments of plot function:
     *         ["<taskname>", "<param>=<value>", "<param>=<value>", ...]
     */
    public static JupyterCell createPlotWordsCell( String[] words ) {
        List<String> lines = new ArrayList<>();
        lines.add( PLOTWORDS_FUNC + "([" );
        for ( String word : words ) {
            lines.add( "    \"" + word + "\"," );
        }
        lines.add( "])" );
        return new JupyterCell( lines );
    }

    /**
     * Writes to standard output an ipynb file that will generate some
     * plots for any installation (no data required).
     *
     * @param  args   ignored
     */
    public static void main( String[] args ) {
        List<JupyterCell> cells = new ArrayList<>();
        cells.add( PLOT_CELL );
        cells.add( createPlotWordsCell( new String[] {
            "plot2plane",
            "layer1=function",
            "fexpr1=sin(x)/x",
            "thick1=3",
            "xmin=0",
            "xmax=30",
            "ymin=-0.25",
            "ymax=0.25",
        } ) );
        cells.add( createPlotWordsCell( new String[] {
            "plot2sky",
            "ofmt=png",
            "viewsys=ecliptic",
            "layer_g=skygrid",
            "gridsys_g=galactic",
            "gridcolor_g=hotpink",
            "labelpos_g=none",
        } ) );
        System.out.println( JupyterCell.toNotebook( cells ).toString( 1 ) );
    }
}
