package uk.ac.starlink.ttools.cea;

import com.thaiopensource.validate.ValidationDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.LoadException;

public class CeaTest extends TestCase {

    public static final String APP_DESCRIPTION_SCHEMA =
        "http://software.astrogrid.org/schema/cea/CEAImplementation/v1.0/" +
        "CEAImplementation.xsd";
    private static final String DUMMY_CMD =
        "<" + CeaTest.class.getName() + ">";
    private CeaTask[] tasks_;
    private CeaTask task1_;
    private CeaMetadata tasksMeta_;
    private CeaMetadata task1Meta_;

    public CeaTest( String name ) {
        super( name );
    }

    public void setUp() throws LoadException {
        tasks_ = CeaWriter.createTaskList();
        String task1Name = "tskymatch2";
        task1_ = new CeaTask( (Task) Stilts.getTaskFactory()
                                           .createObject( task1Name ),
                              task1Name );
        tasksMeta_ = CeaMetadata.createStiltsMetadata( tasks_ );
        task1Meta_ = CeaMetadata.createTaskMetadata( task1_ );
    }

    public void testImplementationDoc() throws Exception {
        checkCeaOutput( new ImplementationCeaWriter( null, tasks_, tasksMeta_,
                                                     true, DUMMY_CMD ),
                        new String[] { "-path", "/usr/bin/stilts", } );
        checkCeaOutput( new ImplementationCeaWriter( null,
                                                     new CeaTask[] { task1_ },
                                                     task1Meta_, false,
                                                     DUMMY_CMD ),
                        new String[] { "-path", "/usr/bin/stilts_"
                                                + task1_.getName() } );
    }

    public void testServiceDoc() throws Exception {
        checkCeaOutput( new ServiceCeaWriter( null, tasks_, task1Meta_,
                                              false, DUMMY_CMD ),
                        new String[ 0 ] );
        checkCeaOutput( new ServiceCeaWriter( null, new CeaTask[] { task1_ },
                                              tasksMeta_, true, DUMMY_CMD ),
                        new String[ 0 ] );
    }

    private void checkCeaOutput( CeaWriter writer, String[] extraArgs )
            throws Exception {
        File tmpFile = File.createTempFile( "cea-doc", ".xml" );
        tmpFile.deleteOnExit();
        try {
            PrintStream out =
                new PrintStream( new FileOutputStream( tmpFile ) );
            writer.setOut( out );
            writer.configure( extraArgs );
            writer.writeDocument();
            out.close();

            String schema = writer.getSchemaLocation();
            if ( Boolean.getBoolean( "tests.withnet" ) ) {
                ValidationDriver validor = new ValidationDriver();
                validor.loadSchema( validor.uriOrFileInputSource( schema ) );
                assertTrue( validor.validate( validor
                                            .fileInputSource( tmpFile ) ) );
            }
            else {
                System.out.println( "Skipping network-dependent tests " 
                                  + "(tests.withnet not set)" );
            }
        }
        finally {
            tmpFile.delete();
        }
    }
}
