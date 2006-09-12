package uk.ac.starlink.ttools.cea;

import com.thaiopensource.validate.ValidationDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.ttools.LoadException;

public class CeaTest extends TestCase {

    public static final String APP_DESCRIPTION_SCHEMA =
        "http://software.astrogrid.org/schema/cea/CEAImplementation/v1.0/" +
        "CEAImplementation.xsd";

    public CeaTest( String name ) {
        super( name );
    }

    public void testAppDescription()
           throws IOException, SAXException, LoadException {
       File tmpFile = File.createTempFile( "app-description", ".xml" );
       tmpFile.deleteOnExit();
       try {
           PrintStream out = new PrintStream( new FileOutputStream( tmpFile ) );
           new CeaWriter( out, CeaWriter.getTasks(), "stilts",
                          "starjava/stilts" )
              .writeConfig( getClass().getName() );
           out.close();

           String schema = APP_DESCRIPTION_SCHEMA;

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
