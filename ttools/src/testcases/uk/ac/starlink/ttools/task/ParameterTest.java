package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.mode.ProcessingMode;

public class ParameterTest extends TestCase {

    private Collection noDefaults = new HashSet( Arrays.asList( new String[] {
        "protocol", "database", "newtable",
    } ) );

    public ParameterTest( String name ) {
        super( name );
    }

    public void testParams() throws LoadException {
        Task[] tasks = getTasks();
        for ( int i = 0; i < tasks.length; i++ ) {
            checkParams( tasks[ i ].getParameters() );
        }

        ProcessingMode[] modes = getModes();
        for ( int i = 0; i < modes.length; i++ ) {
            checkParams( modes[ i ].getAssociatedParameters() );
        }
    }

    private void checkParams( Parameter[] params ) {
        for ( int i = 0; i < params.length; i++ ) {
            checkParam( params[ i ] );
        }
    }

    private void checkParam( Parameter param ) {
        assertTrue( param.getName(),
                    param.getDefault() != null ||
                    param.isNullPermitted() ||
                    noDefaults.contains( param.getName() ) );
    }

    private ProcessingMode[] getModes() throws LoadException {
        ObjectFactory fact = Stilts.getModeFactory();
        List nameList = new ArrayList( Arrays.asList( fact.getNickNames() ) );
        List modeList = new ArrayList();
        for ( Iterator it = nameList.iterator(); it.hasNext(); ) {
            modeList.add( fact.createObject( (String) it.next() ) );
        }
        return (ProcessingMode[]) modeList.toArray( new ProcessingMode[ 0 ] );
    }

    private Task[] getTasks() throws LoadException {
        ObjectFactory fact = Stilts.getTaskFactory();
        List nameList = Arrays.asList( fact.getNickNames() );
        List taskList = new ArrayList();
        for ( Iterator it = nameList.iterator(); it.hasNext(); ) {
            taskList.add( fact.createObject( (String) it.next() ) );
        }
        return (Task[]) taskList.toArray( new Task[ 0 ] );
    }
}
