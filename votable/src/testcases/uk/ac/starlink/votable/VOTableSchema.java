package uk.ac.starlink.votable;

import com.sun.msv.verifier.jarv.TheFactoryImpl;
import java.io.IOException;
import java.net.URL;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.xml.sax.SAXException;

public class VOTableSchema {

    private static Schema schema11_;

    public static Schema getSchema( String version )
            throws VerifierConfigurationException, SAXException, IOException {
        if ( version.equals( "1.1" ) ) {
            if ( schema11_ == null ) {
                URL schemaUrl =
                    VOTableSchema.class
                   .getResource( "/uk/ac/starlink/util/text/VOTable1.1.xsd" );
                schema11_ = new TheFactoryImpl()
                           .compileSchema( schemaUrl.toString() );
            }
            return schema11_;
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
