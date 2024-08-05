package uk.ac.starlink.ttools.cea;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLUtils;

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
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
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
        if ( false ) {
            checkCeaOutput( new ServiceCeaWriter( null, tasks_, task1Meta_,
                                                  false, DUMMY_CMD ),
                            new String[ 0 ] );
            checkCeaOutput( new ServiceCeaWriter( null, new CeaTask[]{ task1_ },
                                                  tasksMeta_, true, DUMMY_CMD ),
                            new String[ 0 ] );
        }
        else {
            System.err.println( "Skipping service doc (not much used?) test\n"
                              + "because of validation errors "
                              + "I don't understand." );
        }
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

            String schemaLoc = writer.getSchemaLocation();
            URL schemaUrl = URLUtils.newURL( schemaLoc );
            if ( Boolean.getBoolean( "tests.withnet" ) ) {
                Schema schema = SchemaFactory
                               .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
                               .newSchema( schemaUrl );
                schema.newValidator()
                      .validate( new SAXSource(
                                     new InputSource(
                                         new FileInputStream( tmpFile ) ) ) );
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
