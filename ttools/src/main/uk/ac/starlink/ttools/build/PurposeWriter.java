package uk.ac.starlink.ttools.build;

import java.util.logging.Level;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Writes entity definitions for the purposes of all the known STILTS tasks.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2006
 */
public class PurposeWriter {
    public static void main( String[] args ) throws LoadException {
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
        ObjectFactory<Task> taskFactory = Stilts.getTaskFactory();
        String[] taskNames = taskFactory.getNickNames();
        for ( int i = 0; i < taskNames.length; i++ ) {
            String name = taskNames[ i ];
            Task task = taskFactory.createObject( name );
            String purpose = task.getPurpose();
            String entDef = new StringBuffer()
                .append( "<!ENTITY " )
                .append( name )
                .append( "-purpose '" )
                .append( purpose )
                .append( "'>" )
                .toString();
            System.out.println( entDef );
        }
    }
}
