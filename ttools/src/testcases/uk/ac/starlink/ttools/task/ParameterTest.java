package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

public class ParameterTest extends TestCase {

    private Formatter formatter_ = new Formatter();

    private Collection noDefaults = new HashSet( Arrays.asList( new String[] {
        "protocol", "database", "newtable",
        "matcher", "values", "values1", "values2", "valuesN", "params",
        "in", "in1", "in2", "nin", "inN",
        "expression",
        "cols", "binsizes", "coords",
        "serviceurl", "ra", "dec", "sr",
        "tapurl", "joburl", "adql", "uploadN", "votable",
        "query",
        "cache", "href",
        "db", "dbtable", "dbra", "dbdec",
        "ra1", "dec1", "ra2", "dec2", "error",
        "select", "assign", "sql",
        "cdstable",
        "pixdata", "lon", "lat", "radius",
        "inlon", "inlat", "taplon", "taplat", "taptable",
        "out", "ofmt",
        "legend",
        "xdataN", "ydataN", "zdataN", "auxdataN",
        "subsetNS", "colourNS", "shapeNS", "transparencyNS",
        "layerN",
        "syncurl", "asyncurl", "tablesurl", "capabilitiesurl",
        "availabilityurl", "examplesurl",
        "doc",
        "end",
    } ) );

    public ParameterTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
    }

    public void testParams() throws LoadException, SAXException {
        Task[] tasks = getTasks();
        for ( int i = 0; i < tasks.length; i++ ) {
            checkParams( tasks[ i ].getParameters() );
        }

        ProcessingMode[] modes = getModes();
        for ( int i = 0; i < modes.length; i++ ) {
            checkParams( modes[ i ].getAssociatedParameters() );
        }
    }

    private void checkParams( Parameter[] params ) throws SAXException {
        for ( int i = 0; i < params.length; i++ ) {
            checkParam( params[ i ] );
        }
    }

    private void checkParam( Parameter param ) throws SAXException {
        assertTrue( param.getName(),
                    param.getStringDefault() != null ||
                    param.isNullPermitted() ||
                    noDefaults.contains( param.getName() ) );
        formatter_.formatXML( param.getDescription(), 0 );
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
