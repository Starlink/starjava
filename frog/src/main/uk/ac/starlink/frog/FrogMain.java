package uk.ac.starlink.frog;

import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import uk.ac.starlink.frog.FrogSplash;

/**
 * Class which spawns the FROG application. Uses and event thread to
 * make the interface visible. Parses command line arguements and passes 
 * these to the Frog class which actually does the work to construct the
 * interface.
 *
 * @see Frog
 * @since $Date$
 * @author Alasdair Allan (Starlink, University of Exeter)
 * @version $Id$
 */
public class FrogMain
{
    /**
     * Create the main window adding any command-line time series.
     *
     * @param args list of input time series
     */
    public FrogMain( String[] args ) 
    {
        String[] realArgs = null;
        if ( args.length != 0 && ! "".equals( args[0] ) ) {
            realArgs = args;
        }
        final String[] timeseries = realArgs;
 
       JFrame splashFrame = new JFrame();
       final FrogSplash splash = new FrogSplash( splashFrame );

        //  Make interface visible. Do from event thread as GUI could
        //  be realized before returning (not thread safe).
        Runnable doWork = new Runnable() {
                public void run() {
                    
                    Frog frame = new Frog( timeseries );
                    frame.setVisible( true );
                    splash.toFront();
               }
            };
        SwingUtilities.invokeLater( doWork );
    }

    /**
     * Main method. Starting point for FROG application.
     *
     * @param args list of input time series
     */
    public static void main( String[] args ) 
    {
        new FrogMain( args );
    }
}
