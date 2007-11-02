package uk.ac.starlink.ttools.cea;

import com.thaiopensource.validate.ValidationDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.LoadException;

public class CeaTest extends TestCase {

    public static final String APP_DESCRIPTION_SCHEMA =
        "http://software.astrogrid.org/schema/cea/CEAImplementation/v1.0/" +
        "CEAImplementation.xsd";
    private static final String DUMMY_CMD =
        "<" + CeaTest.class.getName() + ">";
    private CeaTask[] tasks_;

    public CeaTest( String name ) {
        super( name );
    }

    public void setUp() throws LoadException {
        tasks_ = CeaWriter.createTaskList();
    }

    public void testImplementationDoc() throws Exception {
        checkCeaOutput( new ImplementationCeaWriter( null, tasks_, DUMMY_CMD ),
                        new String[] { "-path", "/usr/bin/stilts", } );
    }

    public void testServiceDoc() throws Exception {
        checkCeaOutput( new ServiceCeaWriter( null, tasks_, DUMMY_CMD ),
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
            writer.configure( DUMMY_CMD, extraArgs );
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
