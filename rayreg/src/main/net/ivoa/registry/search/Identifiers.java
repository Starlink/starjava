/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;
import net.ivoa.registry.RegistryFormatException;
import net.ivoa.registry.RegistryCommException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * this class provides iterator access to a list of identifiers returned
 * as part of a registry search.
 */
public class Identifiers extends SearchResults {

    Searcher search = null;

    Identifiers(Searcher searcher, short strictness) 
         throws RegistryServiceException, RegistryFormatException,
                RegistryCommException
    {
        super(searcher, strictness, "identifier");
    }

    /**
     * return the next identifier in the list of results.  This call
     * may recontact the registry to get the next set of results; thus,
     * some exceptions may be thrown.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     */
    public String next() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        Element out = nextElement();
        return ((out == null) ? null : getValue(out));
    }

    /**
     * return the previous identifier.  This call may recontact the registry 
     * to re-fetch the previous set of results; thus, some exceptions may be
     * thrown.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     */
    public String previous() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        Element out = previousElement();
        return ((out == null) ? null : getValue(out));
    }

    String getValue(Element txtel) {
        StringBuffer out = new StringBuffer();
        Node child = txtel.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) 
                out.append(child.getNodeValue());
            child = child.getNextSibling();
        }
        return out.toString().trim();
    }
}
