package uk.ac.starlink.ttools;

import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.filter.BasicFilter;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.StepFactory;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.ObjectFactory;

public class FactoryTest extends TestCase {

    public FactoryTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
    }

    public void testFilterFactory() throws Exception {
        ObjectFactory fact = StepFactory.getInstance().getFilterFactory();
        String[] names = fact.getNickNames();
        for ( int i = 0; i < names.length; i++ ) {
            String name = names[ i ];
            ProcessingFilter filter = 
                (ProcessingFilter) fact.createObject( name );
            assertEquals( name, ((BasicFilter) filter).getName() );
        }
    }

    public void testModeFactory() throws Exception {
        ObjectFactory fact = Stilts.getModeFactory();
        String[] names = fact.getNickNames();
        for ( int i = 0; i < names.length; i++ ) {
            String name = names[ i ];
            ProcessingMode mode = (ProcessingMode) fact.createObject( name );
        }
    }

    public void testTaskFactory() throws Exception {
        ObjectFactory fact = Stilts.getTaskFactory();
        String[] names = fact.getNickNames();
        for ( int i = 0; i < names.length; i++ ) {
            String name = names[ i ];
            Task task = (Task) fact.createObject( name );
        }
    }
}
