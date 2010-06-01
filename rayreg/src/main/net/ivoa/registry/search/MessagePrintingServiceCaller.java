/*
 * Created by Ray Plante for the US National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;

import java.net.URL;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import org.w3c.dom.Element;

/**
 * a ServiceCaller that will print the SOAP messages to an OutputStream.  This
 * is useful for debugging service communication.
 */
public class MessagePrintingServiceCaller extends DefaultServiceCaller {
    OutputStream out = System.err;

    /**
     * create a default ServiceCaller object
     * @param connection   the SOAPConnection object to use to connect to the 
     *                        service.  If null, a default connection will be 
     *                        created.
     * @param endpoint     the service's endpoint URL.  
     */
    public MessagePrintingServiceCaller(SOAPConnection connection, URL endpoint)
    {
        super(connection, endpoint);
    }

    /**
     * create a default ServiceCaller object
     * @param endpoint     the service's endpoint URL
     */
    public MessagePrintingServiceCaller(URL endpoint) {
        super(endpoint);
    }

    /**
     * create a default ServiceCaller object
     * @param connection   the SOAPConnection object to use to connect to the 
     *                        service  If null, a default connection will be 
     *                        created.
     */
    public MessagePrintingServiceCaller(SOAPConnection connection) {
        super(connection);
    }

    /**
     * create a default ServiceCaller object
     */
    public MessagePrintingServiceCaller() {
        super();
    }

    /**
     * set the stream to send the messages to.
     * @param strm   the destination OutputStream; if null, System.err is set.
     */
    public void setMessageDestination(OutputStream strm) {
        if (strm == null) strm = System.err;
        out = strm;
    }

    /**
     * return the stream that messages are being sent to
     */
    public OutputStream getMessageDestination() { return out; }

    /**
     * call the SOAP service and return the content of the body the 
     * SOAP response.  This assumes a <em>wrapped document<em> message
     * style in which the body contains a single child element.  
     * @param msg     the input SOAP message to send
     * @param action  the SOAP action URI to use
     * @exception SOAPException  if an error occurs while handling the SOAP.
     * @exception RegistryServiceException   if any errors occurs while 
     *                communicating with the service.
     */
    public Element call(SOAPMessage msg, String actionURI)
         throws RegistryServiceException, SOAPException
    {
        PrintWriter nlout = new PrintWriter(out);
        try {
            nlout.println("REQUEST:");
            nlout.flush();
            msg.writeTo(out);
            nlout.println();
            nlout.flush();

            SOAPMessage reply = getReplyFromCall(msg, actionURI);

            nlout.println("RESPONSE:");
            nlout.flush();
            reply.writeTo(out);
            nlout.println();
            nlout.println("------");
            nlout.flush();

            return extractContent(reply);
        }
        catch (IOException ex) {
            throw new RegistryServiceException(ex.getMessage());
        }
    }
}
