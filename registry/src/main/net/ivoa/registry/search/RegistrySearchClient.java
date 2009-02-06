/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

// import net.ivoa.adql.convert.parser.v1_0.ADQLParser;

import net.ivoa.registry.RegistryServiceException;
import net.ivoa.registry.RegistryFormatException;
import net.ivoa.registry.RegistryCommException;
import net.ivoa.registry.RegistryAccessException;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * a registry search client class
 */
public class RegistrySearchClient {

    protected int resRecordBuffer = 500;
    protected int resIDBuffer = 10000;
    protected SOAPSearchClient service = null;
    protected short strictness = WARN_COMPLIANCE;

    /**
     * the default registry endpoint to connect to.  This can be overridden
     * via the system property "VORegsitry.search.defaultRegistry".  
     */
    public static URL defaultEndpoint= null;
    static {
        String hardcoded = "http://nvo.stsci.edu/vor10/ristandardservice.asmx";
        String defURL = System.getProperty("VORegsitry.search.defaultRegistry",
                                           hardcoded);
        try { defaultEndpoint = new URL(defURL); }
        catch (MalformedURLException ex) {
            System.err.println("Malformed default registry URL: " +
                               defURL);
            try { defaultEndpoint = new URL(hardcoded); }
            catch (MalformedURLException e) {
                throw new InternalError("programmer error: bad hardcoded URL: "+
                                        hardcoded);
            }
        }
    }

    /**
     * create a client configured to connect to the configured default 
     * registry
     */
    public RegistrySearchClient() {
        this(defaultEndpoint);
    }

    /**
     * create a client connected to a registry with the given endpoint URL
     */
    public RegistrySearchClient(URL endpointURL) {
        service = new SOAPSearchClient(endpointURL, strictness);
    }

    /** 
     * Adapt silently to any service compliance errors when possible.
     * See setCompliance().
     */
    public final static short LOOSE_COMPLIANCE = 0;

    /** 
     * Warn about any service compliance errors with a message to standard
     * error, but otherwise adapt when possible.
     * See setCompliance().
     */
    public final static short WARN_COMPLIANCE = 1;

    /** 
     * Throw an exception whenever the registry does not comply with the 
     * Registry Interface specification.
     * See setCompliance().
     */
    public final static short STRICT_COMPLIANCE = 2;

    /**
     * Set the tolerance toward non-compliance with the Registry Interface
     * standard.  
     * See fields LOOSE_COMPLIANCE, WARN_COMPLIANCE, and STRICT_COMPLIANCE
     * possible input values.
     */
    public void setCompliance(short level) { 
        strictness = level; 
        service.setCompliance(level);
    }

    /**
     * Set the tolerance toward non-compliance with the Registry Interface
     * standard.  
     * See fields LOOSE_COMPLIANCE, WARN_COMPLIANCE, and STRICT_COMPLIANCE
     * possible return values.
     */
    public short getCompliance() { return strictness; }

    /**
     * return true if the operation arguments are qualified in the request
     * SOAP request messages that are produced.
     */
    public boolean soapArgsAreQualified() { return service.argsAreQualified(); }

    /**
     * set whether operation arguments will be qualified in the request
     * SOAP request messages that are produced.
     */
    public void qualifySoapArgs(boolean yesno) { service.qualifyArgs(yesno); }

    /**
     * return the size of the record buffer.  The resource record search 
     * methods will buffer a maximum of this many records in its memory at
     * a time.  
     */
    public int getRecordBufferSize() { return resRecordBuffer; }

    /**
     * return the size of the record buffer.  The resource record search 
     * methods will buffer a maximum of this many records in its memory at
     * a time.  
     */
    public void setRecordBufferSize(int size) { 
        if (size < 1) size = 500;
        resRecordBuffer = size; 
    }

    /**
     * return the description of the registry.   <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     * @exception RegistryFormatException   if the response does not contain a
     *                           VOResource record.  
     */
    public VOResource getIdentity() throws RegistryAccessException {
        try {
            return new VOResource(getFirstChildElement(service.getIdentity()));
        }
        catch (SOAPException ex) {
            throw new RegistryCommException(ex);
        }
    }

    private static Element getFirstChildElement(Element parent) 
         throws RegistryFormatException 
    {
        Node child = parent.getFirstChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) 
            child = child.getNextSibling();
        if (child == null) 
            throw new RegistryFormatException("Empty response wrapper: " + 
                                              parent.getTagName());
        return (Element) child;
    }

    /**
     * return the Resource description for a given identifier.   <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @param ivoid   the IVOA Identifier to resolve
     * @exception IDNotFoundException  if the service cannot match the given
     *                           ID to a description
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public VOResource getResource(String ivoid) 
         throws IDNotFoundException, RegistryAccessException 
    {
        try {
            return 
               new VOResource(getFirstChildElement(service.getResource(ivoid)));
        } catch (SOAPException ex) {
            throw new RegistryCommException(ex);
        }
    }

    /**
     * search for resource descriptions containing specific keywords.   <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @param keywords    the list of keywords to search for
     * @param orThem      if true, return records that contains at least one 
     *                       keyword; otherwise, return those that have all 
     *                       keywords.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public Records searchByKeywords(String keywords, boolean orThem)  
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        return new Records(new KeywordSearch(service, keywords, orThem, 0,
                                             resRecordBuffer, false),
                           strictness);
    }

    /**
     * search for the resource identifiers whose descriptions contain specific 
     * keywords.  <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @param keywords    the list of keywords to search for
     * @param orThem      if true, return records that contains at least one 
     *                       keyword; otherwise, return those that have all 
     *                       keywords.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public Identifiers identifiersByKeywords(String keywords, boolean orThem)  
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        return new Identifiers(new KeywordSearch(service, keywords, orThem, 0,
                                                 resRecordBuffer, true),
                               strictness);
    }

    /**
     * search for the resource descriptions based on a detailed ADQL query
     * @param adqlWhere   the ADQL Where clause that constrains the search
     * @exception ADQLSyntaxException   if a syntax error is detected in the 
     *                           input ADQL string.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public Records searchByADQL(String adqlWhere) 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException, ADQLSyntaxException
    {
        return new Records(new Search(service, adqlWhere, 0, resRecordBuffer, 
                                      false),
                           strictness);
    }

    /**
     * search for the resource identifiers whose descriptions match an ADQL
     * query.   <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @param adqlWhere   the ADQL Where clause that constrains the search
     * @exception ADQLSyntaxException   if a syntax error is detected in the 
     *                           input ADQL string.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public Identifiers identifiersByADQL(String adqlWhere) 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException, ADQLSyntaxException
    {
        return new Identifiers(new Search(service, adqlWhere, 0, 
                                          resRecordBuffer, true),
                               strictness);
    }

    /**
     * search for resource description information using XQuery.    <p>
     *
     * Note that all of the checked exceptions thrown by this 
     * method inherit from a common base class, RegistryAccessException.  
     *
     * @param String   the XQuery stored as a string.
     * @exception RegistryServiceException  if the service encounters an error 
     *                           (i.e. on the server side).
     * @exception RegistryFormatException   if the XML response is non-compliant
     *                           in some way.
     * @exception RegistryCommException   if an unexpected protocol error is
     *                           encountered.
     */
    public Element searchByXQuery(String xquery) 
         throws RegistryServiceException, UnsupportedOperationException, 
                RegistryCommException
    {
        try {
            return service.xquerySearch(xquery);
        } catch (SOAPException ex) {
            throw new RegistryCommException(ex);
        }
    }

    /**
     * set the SOAP ServiceCaller implementation to use.  It will be 
     * initialized with the service endpoint.  If the input is null, the 
     * default caller will be restored.  
     * 
     * @param c   the caller object
     * @see ServiceCaller
     */
    public void setCaller(ServiceCaller c) {
        service.setCaller(c);
    }

    private static Method getSearchMethod() {
        try {
            return (SOAPSearchClient.class).getMethod("search", 
                                                      new Class[]
                                                           { Element.class, 
                                                             Integer.TYPE,
                                                             Integer.TYPE,
                                                             Boolean.TYPE });
        }
        catch (NoSuchMethodException ex) {
            throw new InternalError("programmer error: can't find " +
                                    "search method via reflection");
        }
    }

    Element convertWhere(String adqlwhere) throws ADQLSyntaxException {
        adqlwhere = "where " + adqlwhere.trim();
        Where2DOM p = new Where2DOM(new StringReader(adqlwhere));

        try {
            return p.parseWhere();
        } catch (ParseException ex) {
            throw new ADQLSyntaxException(ex);
        }
    }

    class Search extends Searcher {

        Search(SOAPSearchClient service, String adqlwhere, 
               int from, int max, boolean idsonly) 
             throws ADQLSyntaxException
        {
            super(service, getSearchMethod(), from, max);

            // convert ADQL string to XML
            Element where = convertWhere(adqlwhere);

            args = new Object[] { where, new Integer(from), 
                                  new Integer(max), new Boolean(idsonly) };
        }

        public void updateArgs() {
            args[1] = new Integer(getFrom());
            args[2] = new Integer(getMax());
        }
    }

    private static Method getKeywordSearchMethod() {
        try {
            return (SOAPSearchClient.class).getMethod("keywordSearch", 
                                                      new Class[]
                                                           { String.class, 
                                                             Boolean.TYPE,
                                                             Integer.TYPE,
                                                             Integer.TYPE,
                                                             Boolean.TYPE });

        }
        catch (NoSuchMethodException ex) {
            throw new InternalError("programmer error: can't find " +
                                    "keywordSearch method via reflection");
        }
    }
    
    class KeywordSearch extends Searcher {

        KeywordSearch(SOAPSearchClient service, String keywords, boolean orThem,
                      int from, int max, boolean idsonly) 
        {
            super(service, getKeywordSearchMethod(), from, max);
            args = new Object[] { keywords, new Boolean(orThem), 
                                  new Integer(from), new Integer(max), new 
                                  Boolean(idsonly) };
        }

        public void updateArgs() {
            args[2] = new Integer(getFrom());
            args[3] = new Integer(getMax());
        }
    }
    

}

 
