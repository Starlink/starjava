/*
 * Created by Ray Plante for the US National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;

import java.net.URL;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import javax.xml.soap.MimeHeaders;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

/**
 * a default implementation of the ServiceCaller interface for calling a 
 * SOAP service.  It simply calls sends the input SOAP message and returns 
 * the response.  It checks the response for faults and throws the appropriate 
 * exception.  
 *
 * This implementation can also be used as a base class for overriding or 
 * extending the call method.  
 *
 * This interface is intended for the so-called <em>wrapped document<em>
 * style of SOAP messaging in that the call() method is supposed to return 
 * the wrapping element from inside the SOAP body of the service response 
 * message.
 */
public class DefaultServiceCaller implements ServiceCaller {
    URL ep = null;
    protected SOAPConnection conn = null;

    /**
     * the SOAP v1.1 namespace URI string
     */
    public static final String SOAP_NS = 
        "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * the general registry interface fault element tag name
     */
    public static final String GENERAL_FAULT = "ErrorResponse";

    /**
     * the tag name for the id-not-found registry interface 
     * fault element 
     */
    public static final String NOT_FOUND_FAULT = "NotFound";

    /**
     * the tag name for the unsupported operation registry interface 
     * fault element 
     */
    public static final String UNSUPPORTED_OPERATION_FAULT = 
        "UnsupportedOperation";

    /**
     * create a default ServiceCaller object
     * @param connection   the SOAPConnection object to use to connect to the 
     *                        service.  If null, a default connection will be 
     *                        created.
     * @param endpoint     the service's endpoint URL.  
     */
    public DefaultServiceCaller(SOAPConnection connection, URL endpoint) {
        conn = connection;
        ep = endpoint;

        if (conn == null) {
            try {
                conn = SOAPConnectionFactory.newInstance().createConnection();
            }
            catch (SOAPException ex) {
                throw new InternalError("installation/config error: " + 
                                        ex.getMessage());
            }
        }
    }

    /**
     * create a default ServiceCaller object
     * @param endpoint     the service's endpoint URL
     */
    public DefaultServiceCaller(URL endpoint) {
        this(null, endpoint);
    }

    /**
     * create a default ServiceCaller object
     * @param connection   the SOAPConnection object to use to connect to the 
     *                        service  If null, a default connection will be 
     *                        created.
     */
    public DefaultServiceCaller(SOAPConnection connection) {
        this(connection, null);
    }

    /**
     * create a default ServiceCaller object
     */
    public DefaultServiceCaller() {
        this(null, null);
    }

    /**
     * set the service endpoint URL
     */
    public void setEndpoint(URL endpoint) { ep = endpoint; }

    /**
     * get the service endpoint URL currently in use
     */
    public URL getEndpoint() { return ep; }

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
        return extractContent(getReplyFromCall(msg, actionURI));
    }

    /**
     * call the service and return the SOAPMessage response
     */
    protected SOAPMessage getReplyFromCall(SOAPMessage msg, String actionURI) 
         throws SOAPException
    {
        MimeHeaders mh = msg.getMimeHeaders();
        mh.setHeader("SOAPAction", actionURI);
        return conn.call(msg, ep);
    }

    /**
     * extract the content of the SOAP response.  The response is checked for
     * faults.  
     */
    protected Element extractContent(SOAPMessage resp) 
         throws RegistryServiceException, SOAPException
    {
        if (resp == null) 
            throw new RegistryServiceException("No response from registry");

        Node out = resp.getSOAPBody().getFirstChild();
        while (out != null && out.getNodeType() != Node.ELEMENT_NODE) {
            out = out.getNextSibling();
        }

        if (out == null) 
            throw new RegistryServiceException("Empty SOAP Envelope!");

        checkForFaults((Element) out);

        return ((Element) out);
    }

    /**
     * check the given element representing the contents of the SOAP body
     * to see if it is a SOAP Fault and throw the appropriate exception
     */
    public static void checkForFaults(Element bodyelement) 
         throws RegistryServiceException
    {
        if (bodyelement.getLocalName().equals("Fault") && 
            bodyelement.getNamespaceURI().equals(SOAP_NS)) 
        {
            Element fault = null;
            Element detail = getFirstChildElement(bodyelement, "detail");
            if (detail != null) fault = getFirstChildElement(detail, null);

            if (fault != null) {
              if (fault.getLocalName().equals(GENERAL_FAULT))
                  throw new RegistryServiceException(getMessage(fault));
              else if (fault.getLocalName().equals(NOT_FOUND_FAULT))
                  throw new IDNotFoundException(getMessage(fault));
              else if (fault.getLocalName().equals(UNSUPPORTED_OPERATION_FAULT))
                  throw new UnsupportedOperationException(getMessage(fault));
            }

            fault = getFirstChildElement(bodyelement, "faultstring");
            if (fault == null) {
                throw new RegistryServiceException("unknown server error " +
                                                   "(missing faultstring)");
            } 
            else {
                String msg = getText(fault);
                if (msg.length() == 0) 
                    msg = "unknown server error (empty faultstring)";
                throw new RegistryServiceException(msg);
            }
        }
        
    }

    private static Element getFirstChildElement(Element parent, String childtag)
    {
        Node child = parent.getFirstChild();
        while (child != null && 
               (child.getNodeType() != Node.ELEMENT_NODE ||
                (childtag != null && ! childtag.equals(child.getLocalName())) ))
            child = child.getNextSibling();

        return (Element) child;
    }

    /**
     * given the contents of registry interface fault element, return the 
     * enclosed error message.  The fault element is one defined in the 
     * registry interface search WSDL.
     */
    public static String getMessage(Element rifault) throws DOMException {
        NodeList nl = rifault.getElementsByTagName("errorMessage");
        Node msgel = null;
        for(int i=0; i < nl.getLength(); i++) {
            msgel = nl.item(i);
        }
        String msg = getText((Element) msgel);
        if (msg.length() == 0) msg = "Unknown error";
        return msg;
    }

    private static String getText(Element txtel) {
        StringBuffer sb = new StringBuffer();
        if (txtel != null) {
            Node msg = txtel.getFirstChild();
            while (msg != null) {
                if (msg.getNodeType() == Node.TEXT_NODE) 
                    sb.append(msg.getNodeValue());
                msg = msg.getNextSibling();
            }
        }
        return sb.toString().trim();
    }


}
