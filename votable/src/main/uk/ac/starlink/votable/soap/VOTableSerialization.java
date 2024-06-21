package uk.ac.starlink.votable.soap;

import javax.xml.namespace.QName;
import org.apache.axis.AxisEngine;
import org.apache.axis.client.Call;
import uk.ac.starlink.table.StarTable;

/**
 * Utility class for use with VOTable custom serialization for AXIS.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class VOTableSerialization {

    /**
     * QName used to describe objects which are serialized using this
     * serializer.  This is currently
     * <code>{http://www.ivoa.net/xml/VOTable/v1.1}VOTABLE</code>.
     */
    public static final QName QNAME_VOTABLE =
        new QName( "http://www.ivoa.net/xml/VOTable/v1.1", "VOTABLE" );

    /**
     * Configures a <code>Call</code> object for use with VOTable custom
     * serialization by registering the requisite type mappings,
     * and perhaps other things too.
     *
     * @param  call   call to configure
     */
    public static void configureCall( Call call ) {

        /* Register the type mappings for the custom de/serialization. */
        call.registerTypeMapping( StarTable.class, QNAME_VOTABLE,
                                  new AxisTableSerializerFactory(),
                                  new AxisTableDeserializerFactory() );

        /* Dissuade AXIS from using 'multiRef'-style serialization.
         * This is where only a reference is put into the argument
         * elements which form the RPC part of the SOAP message, and
         * the actual data elements are elsewhere in the message located
         * using id/href attributes.  It makes sense if what's being
         * serialized is a network of objects with multiple internal
         * references, which doesn't apply to custom StarTable serialization.
         * It seems likely that kind of game could inhibit SAX-like
         * streaming of the VOTable XML rather than turning it all into
         * a DOM before/after sending it.  I suspect however that AXIS
         * does turn it into a DOM, so this doesn't make much odds,
         * but it makes the SOAP message slightly less baroque, so
         * leave it for now. */
        call.setProperty( AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE );

        /* Set the call to do streaming serializations.  I think this only
         * has an effect at AXIS 1.2 (at time of writing, we're building
         * and running against 1.1).
         * See http://wiki.apache.org/ws/FrontPage/Axis/StreamingService. */
        call.setProperty( "axis.streaming", Boolean.TRUE );
    }

    /**
     * Private constructor prevents construction.
     */
    private VOTableSerialization() {
    }
}
