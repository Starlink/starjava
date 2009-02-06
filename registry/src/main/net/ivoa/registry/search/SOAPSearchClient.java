/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;
import net.ivoa.registry.RegistryFormatException;
import net.ivoa.registry.RegistryAccessException;

import java.net.URL;
import java.net.MalformedURLException;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.Name;
import javax.xml.soap.MimeHeaders;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

/**
 * A client for the IVOA Registry Search Interface.
 * <p>
 * This class provides a direct interface to the remote registry search 
 * service that matches the WSDL.  
 * <p>
 * Note that this class can support a deprecated form of the keywordSearch
 * operation that uses a <code>to</code> input parameter instead of 
 * <code>max</code>, depending on the strictness setting (set via 
 * {@link #setCompliance} (see {@link #getKeywordSearchVariant()} for more 
 * discussion).  
 */
public class SOAPSearchClient {

    protected URL endpoint = null;
    protected MessageFactory soapfactory = null;
    protected SOAPConnection conn = null;
    protected ServiceCaller caller = null;

    // these are used for backward compatibility with earlier working draft
    // of the standard interface
    protected Integer kwsVariant = null;
    int wsdlAttempt = 0;
    short strictness = 0;
    static final short LOOSE = 0;
    static final short WARN = 1;
    static final short STRICT = 2;
    boolean qualified = false; 

    public static final String GETIDENTITY_ACTION = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0#GetIdentity";

    public static final String GETRESOURCE_ACTION = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0#GetResource";

    public static final String SEARCH_ACTION = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0#Search";

    public static final String KEYWORDSEARCH_ACTION = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0#KeywordSearch";

    public static final String XQUERYSEARCH_ACTION = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0#XQuerySearch";

    public static final String WSDL_NS = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0";

    public static final String WSDL_PREFIX = "rs";


    protected SOAPSearchClient() {
        try {
            soapfactory = MessageFactory.newInstance();
        }
        catch (SOAPException ex) {
            throw new InternalError("installation/config error: " + 
                                    ex.getMessage());
        }
    }

    /**
     * Create a client configured to connect to a given registry
     */
    public SOAPSearchClient(URL endpointURL) { 
        this();
        endpoint = endpointURL; 
        caller = new DefaultServiceCaller(endpoint);
    }

    /**
     * Create a client configured to connect to a given registry
     */
    public SOAPSearchClient(URL endpointURL, short strictness) { 
        this(endpointURL);
        this.strictness = strictness;
    }

    /**
     * set the Caller implementation to use.  It will be initialized with 
     * the service endpoint.  If the input is null, the default caller will
     * be restored.  
     * @param c   the caller object
     */
    public void setCaller(ServiceCaller c) {
        if (c == null) {
            caller = new DefaultServiceCaller(endpoint);
        }
        else {
            caller = c;
            caller.setEndpoint(endpoint);
        }
    }

    /**
     * submit a keyword search
     * @param keywords   space-delimited words to search on
     * @param orThem     if true, return results that contain any of the 
     *                      keywords
     * @param from       the position of the first match to return
     * @param max        the maximum number of matches to return.
     * @param identifiersOnly  if true, return only identifiers; otherwise,
     *                   return the entire VOResource record for each match.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element keywordSearch(String keywords, boolean orThem, 
                                 int from, int max, 
                                 boolean identifiersOnly) 
         throws RegistryServiceException, SOAPException
    {
        SOAPMessage msg = makeSOAPMessage();
        SOAPBody body = msg.getSOAPBody();
        SOAPElement sel = body.addBodyElement(makeRSName(msg, "KeywordSearch"));

        // add the keywords argument
        SOAPElement arg = sel.addChildElement(makeArgName(msg, "keywords"));
        arg.addTextNode(keywords.trim());

        // add the orValues argument
        arg = sel.addChildElement(makeArgName(msg, "orValues"));
        arg.addTextNode(Boolean.toString(orThem));

        // add the from argument, if necessary
        if (from > 0) {
            arg = sel.addChildElement(makeArgName(msg, "from"));
            arg.addTextNode(Integer.toString(from));
        }

        // add the max argument, if necessary.  Use "to" instead if we 
        // are playing loose and it is supported by the service.
        if (max > 0) {
            if (useKwsTo()) {
                if (strictness > WARN) 
                    throw new RegistryServiceException(KwsComplianceMsg);
                if (strictness > LOOSE)
                    System.err.println(KwsComplianceMsg);

                // backward compatability
                arg = sel.addChildElement(makeArgName(msg, "to"));
                max += from;
            }
            else {
                arg = sel.addChildElement(makeArgName(msg, "max"));
            }
            arg.addTextNode(Integer.toString(max));
        }
        arg = sel.addChildElement(makeArgName(msg, "identifiersOnly"));
        arg.addTextNode(Boolean.toString(identifiersOnly));

        Element res = call(msg, KEYWORDSEARCH_ACTION);
        return res;
    }

    /**
     * submit a constraint-based search
     * @param adqlWhere  the search constraints in the form of a ADQL Where 
     *                      clause.  The element's name should be "Where", and
     *                      its contents should comply with the ADQL schema's
     *                      "WhereType".  
     * @param from       the position of the first match to return
     * @param max        the maximum number of matches to return.
     * @param identifiersOnly  if true, return only identifiers; otherwise,
     *                   return the entire VOResource record for each match.
     * @exception DOMException if the adqlWhere Element object does not allow
     *                           itself to be imported or otherwise its 
     *                           implementation is defective.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element search(Element adqlWhere, int from, int max, 
                          boolean identifiersOnly) 
         throws RegistryServiceException, SOAPException, DOMException
    {
        SearchQuery query = new SearchQuery();
        Element wparent = query.getWhereParent();

        query.setWhere(adqlWhere);
        query.setFrom(from);
        query.setMax(max);
        query.setIdentifiersOnly(identifiersOnly);

        return search(query);
    }

    /**
     * return a SearchQuery object that can be used to attach an ADQL 
     * query to.
     * @exception SOAPException if an error is encountered while creating 
     *                           the SOAP request.  
     */
    public SearchQuery createSearchQuery() throws SOAPException {
        return new SearchQuery(); 
    }

    /**
     * submit a constraint-based search as a SearchQuery object.  Submitting
     * as a SearchQuery object is slightly more efficient(?) way to submit
     * as it provides a way to attach an ADQL query that doesn't ultimately 
     * require a cloning of the where element.
     * @param query   the search constraints as a SearchQuery object.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element search(SearchQuery query) 
         throws RegistryServiceException, SOAPException
    {
        return call(query.getSearchSOAPMessage(), SEARCH_ACTION);
    }

    /**
     * return the Registry description
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element getIdentity() 
         throws RegistryServiceException, SOAPException
    {
        SOAPMessage msg = makeSOAPMessage();

        SOAPBody body = msg.getSOAPBody();
        SOAPElement sel = body.addBodyElement(makeRSName(msg, "GetIdentity"));
        return call(msg, GETIDENTITY_ACTION);
    }

    /**
     * return the Resource description for a given identifier
     * @param ivoid   the IVOA Identifier to resolve
     * @param query   the search constraints as a SearchQuery object.
     * @exception IDNotFoundException  if the service cannot match the given
     *                           ID to a description
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element getResource(String ivoid) 
         throws RegistryServiceException, IDNotFoundException, SOAPException
    {
        SOAPMessage msg = makeSOAPMessage();

        SOAPBody body = msg.getSOAPBody();
        SOAPElement sel = body.addBodyElement(makeRSName(msg, "GetResource"));
        SOAPElement id = sel.addChildElement(makeArgName(msg, "identifier"));
        id.addTextNode(ivoid.trim());

        Element res = call(msg, GETRESOURCE_ACTION);
        return res;
    }

    protected Name makeRSName(SOAPMessage msg, String elname) 
         throws SOAPException
    {
        return msg.getSOAPPart().getEnvelope().createName(elname, WSDL_PREFIX,
                                                          WSDL_NS);
    }

    protected Name makeArgName(SOAPMessage msg, String elname) 
         throws SOAPException
    {
        if (qualified) 
            return makeRSName(msg, elname);
        else
            return msg.getSOAPPart().getEnvelope().createName(elname);
    }

    /**
     * return the result of an XQuery search
     * @exception UnsupportedOperationException  if the service does not support
     *                          an XQuery-based search
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while creating or
     *                           submitting the SOAP request or while processing
     *                           the SOAP response.  
     */
    public Element xquerySearch(String xquery) 
         throws RegistryServiceException, UnsupportedOperationException, 
                SOAPException
    {
        SOAPMessage msg = makeSOAPMessage();
        SOAPEnvelope env = msg.getSOAPPart().getEnvelope();

        SOAPBody body = msg.getSOAPBody();
        SOAPElement sel = body.addBodyElement(makeRSName(msg, "XQuerySearch"));
        SOAPElement id = sel.addChildElement(makeArgName(msg, "xquery"));
        id.addTextNode(xquery.trim());

        Element res = call(msg, XQUERYSEARCH_ACTION);
        return res;
    }
     
    /**
     * create an empty SOAP message.  This can provide a DOM Document with which
     * an element containing an ADQL query can be created and inserted directly.
     * @exception SOAPException if an error is encountered while creating 
     *                           the SOAP request.  
     */
    public SOAPMessage makeSOAPMessage() throws SOAPException {
        return soapfactory.createMessage();
    }

    /**
     * submit the soap message
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception SOAPException if an error is encountered while submitting 
     *                           the SOAP request or while processing
     *                           the SOAP response.  
     */
    protected Element call(SOAPMessage msg, String actionURI) 
         throws RegistryServiceException, SOAPException
    {
        return caller.call(msg, actionURI);
    }

    /**
     * an updatable search query
     */
    public class SearchQuery {

        SOAPMessage msg = null;
        int from=0, max=-1;
        boolean identifiersOnly = false;
        SOAPElement whereParent = null;

        SearchQuery() throws SOAPException {
            msg = makeSOAPMessage();
            SOAPBody body = msg.getSOAPBody();

            // this ensures that the owner document is set.  
            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();

            whereParent = body.addBodyElement(makeRSName(msg, "Search"));
            whereParent.setAttribute("xmlns", WSDL_NS);
        }

        public void setWhere(Element where) throws SOAPException {
            SOAPElement wparent = getWhereParent();
            where = (Element)wparent.getOwnerDocument().importNode(where, true);

            if (WSDL_NS.equals(where.getNamespaceURI())) {
                wparent.appendChild(where);
            }
            else {
                // need to change the namespace
                SOAPElement newwhere = 
                    wparent.addChildElement(makeRSName(msg, "Where"));
                
                // copy over all the attributes
                Attr attr = null;
                NamedNodeMap attrs = where.getAttributes();
                while (attrs.getLength() > 0) {
                    attr = (Attr) attrs.item(0);
                    if (attr == null) break;
                    // where.removeAttributeNode(attr);
                    newwhere.setAttributeNode(attr);
                }

                // copy over all children
                Node node = where.getFirstChild();
                while (node != null) {
                    // where.removeChild(node);
                    newwhere.appendChild(node);
                    node = where.getFirstChild();
                }
            }
        }

        /**
         * return a SOAP Message that is ready for submission
         */
        public SOAPMessage getSearchSOAPMessage() throws SOAPException {
            SOAPElement child = null;

            // this ensures that the owner document is set.  
            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();

            // check that we have an ADQL
//             if (! whereParent.getChildElements("Where").hasNext())
//                 throw new IllegalStateException("Missing ADQL Where clause");

            if (from > 0) {
                child = whereParent.addChildElement(makeArgName(msg, "from"));
                child.addTextNode(Integer.toString(from));
            }
            if (max > 0) {
                child = whereParent.addChildElement(makeArgName(msg, "max"));
                child.addTextNode(Integer.toString(max));
            }
            child = whereParent.addChildElement(
                                    makeArgName(msg, "identifiersOnly"));
            child.addTextNode(Boolean.toString(identifiersOnly));

            return msg;
        }

        /**
         * return a parent element that the Where clause can be appended to.
         */
        public SOAPElement getWhereParent() {
            whereParent.removeContents();
            return whereParent;
        }

        /**
         * return the position of the first record to return
         */
        public int getFrom() { return from; } 

        /**
         * set the position of the first record to return
         */
        public void setFrom(int pos) { from = pos; }

        /**
         * return the maximum number of records to return
         */
        public int getMax() { return max; }

        /**
         * set the maximum number of records to return
         */
        public void setMax(int count) { max = count; }

        /**
         * return whether idenitifiers only should be returned
         */
        public boolean isIdentifiersOnly() { return identifiersOnly; }

        /**
         * set whether idenitifiers only should be returned.  The 
         * default value is false. 
         */
        public void setIdentifiersOnly(boolean yes) { identifiersOnly = yes; }

    }

    /**
     * determine the variant of the keywordSearch interface supported
     * by the service.  This method will return an integer representing
     * a bit mask of the variants that are supported, where the bits include
     * {@link net.ivoa.registry.search.AssessWSDL#VARIANT_ACCEPTS_MAX AssessWSDL.VARIANT_ACCEPTS_MAX} 
     * (the "max" input parameter is supported) and 
     * {@link net.ivoa.registry.search.AssessWSDL#VARIANT_ACCEPTS_TO AssessWSDL.VARIANT_ACCEPTS_TO} 
     * (the "to" input parameter is supported).  This determination involves
     * accessing the remote WSDL and examining the interface description.  
     * <p>
     * This check became necessary due to an error in the official (working 
     * draft) standard WSDL published on the IVOA web site, against several 
     * registries were built.  To provide backward compatibility with these
     * pre-Recommendation implementations, this class can adjust its keyword
     * search message accordingly.  
     * @see net.ivoa.registry.search.AssessWSDL 
     */
    public int getKeywordSearchVariant() {
        if (kwsVariant == null) {
            try {
                URL wsdl = new URL(endpoint.toString() + "?wsdl");
                int var = AssessWSDL.keywordSearchVariant(wsdl);
                kwsVariant = new Integer(var);
            }
            catch (MalformedURLException ex) {
                // the endpoint URL is probably screwy, in which case any
                // query call will probably fail.  We'll call this unrecognized.
                kwsVariant = new Integer(0);
            }
            catch (RegistryFormatException ex) {
                // something appears to be fatal syntax error in the WSDL
                kwsVariant = new Integer(0);
            }
            catch (RegistryAccessException ex) {
                // an I/O or network error occurred.  This could be temporary.
                // We'll allow three attempts and then assume unrecognized. 
                if (++wsdlAttempt < 3) return 0;
                kwsVariant = new Integer(0);
            }
        }
                
        return kwsVariant.intValue();
    }

    boolean useKwsTo() {
        int var = getKeywordSearchVariant();
        return (var > 0 && (var & AssessWSDL.VARIANT_ACCEPTS_MAX) == 0);
    }
    static String KwsComplianceMsg = 
        "Compliance issue: service uses deprecated \"to\" parameter";

    /**
     * Set the tolerance toward non-compliance with the Registry Interface
     * standard.  The possible values are the same as for 
     * {@link net.ivoa.registry.search.RegistrySearchClient}.  If set to
     * STRICT, qualifyArgs(false) will be called.
     */
    public void setCompliance(short level) { 
        strictness = level; 
        if (strictness >= STRICT) qualifyArgs(false);
    }

    /**
     * Set the tolerance toward non-compliance with the Registry Interface
     * standard.  The possible values are the same as for 
     * {@link net.ivoa.registry.search.RegistrySearchClient}.  
     */
    public short getCompliance() { return strictness; }

    /**
     * return true if the operation arguments are qualified in the request
     * SOAP request messages that are produced.
     */
    public boolean argsAreQualified() { return qualified; }

    /**
     * set whether operation arguments will be qualified in the request
     * SOAP request messages that are produced.
     */
    public void qualifyArgs(boolean yesno) { qualified = yesno; }

}
