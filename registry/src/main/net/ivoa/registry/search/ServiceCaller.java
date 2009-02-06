/*
 * Created by Ray Plante for the US National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;

import java.net.URL;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import org.w3c.dom.Element;

/**
 * an interface for calling a SOAP service.  
 *
 * This interface allows a service client to swap in different 
 * implementations for accessing a service with a SOAP message.  
 * It was created for this package primarily for inserting a proxy 
 * class for testing.  
 *
 * This interface is intended for the so-called <em>wrapped document<em>
 * style of SOAP messaging in that the call() method is supposed to return 
 * the wrapping element from inside the SOAP body of the service response 
 * message.
 */
public interface ServiceCaller {

    /**
     * set the service endpoint URL
     */
    public void setEndpoint(URL endpoint);

    /**
     * get the service endpoint URL currently in use
     */
    public URL getEndpoint();

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
         throws RegistryServiceException, SOAPException;

}
