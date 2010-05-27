package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

public class VOTableSchema {

    private static Schema schema11_;
    private static Schema schema12_;

    public static Schema getSchema( String version ) throws IOException, SAXException {
        if ( version.equals( "1.1" ) ) {
            if ( schema11_ == null ) {
                schema11_ =
                    getSchema(
                        VOTableSchema.class
                       .getResource( "/uk/ac/starlink/util/text/VOTable1.1.xsd" ) );
            }
            return schema11_;
        }
        else {
            throw new UnsupportedOperationException( "Unknown version "
                                                   + version );
        }
    }

    private static Schema getSchema( URL url ) throws SAXException {
        return SchemaFactory
              .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
              .newSchema( url );
    }
}
