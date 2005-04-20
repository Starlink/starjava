package uk.ac.starlink.ttools;

import uk.ac.starlink.ttools.pipe.PipelineTask;

/**
 * Pipeline tool for generic table manipulation.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2005
 */
public class TablePipe {

    /**
     * Main method. 
     * Use <tt>-h</tt> for flags.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) {
        if ( ! new PipelineTask().run( args ) ) {
            System.exit( 1 );
        }
    }

}
