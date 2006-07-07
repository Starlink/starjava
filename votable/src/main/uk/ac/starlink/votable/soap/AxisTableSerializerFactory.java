package uk.ac.starlink.votable.soap;

import java.util.Collections;
import java.util.Iterator;
import javax.xml.rpc.encoding.Serializer;
import org.apache.axis.Constants;
import org.apache.axis.encoding.SerializerFactory;

/**
 * Custom serializer factory for streaming StarTables using AXIS.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class AxisTableSerializerFactory implements SerializerFactory {

    public Serializer getSerializerAs( String mechanismType ) {
        return new AxisTableSerializer();
    }

    public Iterator getSupportedMechanismTypes() {
        return Collections.singleton( Constants.AXIS_SAX ).iterator();
    }
}
