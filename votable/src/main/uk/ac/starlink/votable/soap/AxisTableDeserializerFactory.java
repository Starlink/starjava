package uk.ac.starlink.votable.soap;

import java.util.Collections;
import java.util.Iterator;
import javax.xml.rpc.encoding.Deserializer;
import org.apache.axis.Constants;
import org.apache.axis.encoding.DeserializerFactory;
import uk.ac.starlink.table.StoragePolicy;

/**
 * Custom deserializer factory for streaming StarTables using AXIS.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class AxisTableDeserializerFactory implements DeserializerFactory {

    public Deserializer getDeserializerAs( String mechanismType ) {
        return new AxisTableDeserializer( StoragePolicy.getDefaultPolicy() );
    }

    public Iterator getSupportedMechanismTypes() {
        return Collections.singleton( Constants.AXIS_SAX ).iterator();
    }
}
