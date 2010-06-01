package uk.ac.starlink.registry;

/**
 * Aggregates the information required to make a SOAP request.
 *
 * @author   Mark Taylor
 */
public interface SoapRequest {

    /**
     * Returns the unquoted SOAPAction HTTP header to be sent for the 
     * request.  If null, no SOAPAction is sent.
     *
     * @return   SOAPAction header value
     */
    String getAction();

    /**
     * Returns the content of the &lt;soapenv:Body&gt; element to be sent.
     *
     * @return  SOAP body
     */
    String getBody();
}
