/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;
import net.ivoa.registry.RegistryFormatException;
import net.ivoa.registry.RegistryCommException;

import javax.xml.soap.SOAPException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * this class provides iterator access to the results of a registry search
 */
public abstract class SearchResults {

    Searcher search = null;
    LinkedElement buffer = null;
    Element currentBatch = null;
    Node currentRecord = null;
    String elname = null;
    int count = 0, nextfetch = 0, prevfetch = 0, fetchesFromUnknown = 0;
    int defnum = 250, prevnum = defnum, nextnum = defnum;
    boolean haveLast = false, seenLast = false;
    short strictness = RegistrySearchClient.WARN_COMPLIANCE;

    int bufpos=0, bufnum=0, abspos=0;

    SearchResults(Searcher searcher, short strictness, String elementName) 
         throws RegistryServiceException, RegistryFormatException,
                RegistryCommException
    { 
        this.strictness = strictness;
        elname = elementName;
        search = searcher;
        prevnum = nextnum = defnum = searcher.getMax();

        retrieveNext();
        if (buffer != null) {
            bufnum = buffer.getRecordCount();
            currentBatch = buffer.element;
            currentRecord = null;
        }
    }

    /**
     * return the total number of records downloaded from the registry so
     * far.  This is not the same as the number of records iterated through.
     * If isCountFinal() is true, then the count returned by this method
     * is the total number the registry found.  
     */
    public int getRetrievedCount() { return count; }

    /**
     * return whether all of the matched records have be retrieved from the 
     * registry.  If true, then getRetrievedCount() returns the total number
     * of records matched by the query.
     */
    public boolean isCountFinal() { return seenLast; }

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

    int retrieveNext() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        search.setFrom(nextfetch);
        search.setMax(nextnum);
        if (buffer != null && buffer.size() > 1) buffer.dropFirst();
        Element result = null;
        int num = -1;

        try {
            result = search.exec();
        } catch (SOAPException ex) {
            throw new RegistryCommException(ex);
        }

        result = getFirstChildElement(result);

        String attval = result.getAttribute("more");
        if (attval == null || 
            (! attval.equalsIgnoreCase("true") && 
             ! attval.equalsIgnoreCase("false")))
            throw new RegistryFormatException("bad format for " +
                                              "more attribute: " + attval);
        boolean more = Boolean.valueOf(attval).booleanValue();
            

        try {
            num = Integer.parseInt(result.getAttribute("numberReturned"));
        }
        catch (NumberFormatException ex) {
            complianceError("VOResources element with illegal numberReturned: " +
                            result.getAttribute("numberReturned"));
            num = countRecords(result);
        }

        if (fetchesFromUnknown < 1) 
            count += num;
        else 
            fetchesFromUnknown--;
        if (! more) {
            seenLast = haveLast = true;
            if (num < nextnum) nextnum = num;
        }
        nextfetch += num;

        if (buffer == null) {
            buffer = new LinkedElement(result, num);
        }
        else {
            buffer.addLast(result, num);
        }
        return num;
    }

    int countRecords(Element parent) throws RegistryFormatException {
        count = 0;
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) count++;
            child = child.getNextSibling();
        }

        return count;
    }

    int retrievePrevious() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        search.setFrom(prevfetch);
        search.setMax(prevnum);
        if (buffer != null && buffer.size() > 1) {
            buffer.dropLast();
            if (haveLast) haveLast = false;
            fetchesFromUnknown++;
        }
        Element result = null;
        int num = -1;

        try {
            result = search.exec();
        } catch (SOAPException ex) {
            throw new RegistryCommException(ex);
        }

        String attval = result.getAttribute("more");
        if (attval == null || 
            (! attval.equalsIgnoreCase("true") && 
             ! attval.equalsIgnoreCase("false")))
            throw new RegistryFormatException("bad format for " +
                                              "more attribute: " + attval);
        boolean more = Boolean.valueOf(attval).booleanValue();
            
        try {
            num = Integer.parseInt(result.getAttribute("numberReturned"));
        }
        catch (NumberFormatException ex) {
            complianceError("VOResources element with illegal numberReturned: " +
                            result.getAttribute("numberReturned"));
            num = countRecords(result);
        }

        if (prevfetch == 1) 
            prevnum = nextnum;
        else {
            prevnum = num;
            prevfetch -= prevnum;
            if (prevfetch < 1) {
                prevnum = prevnum + prevfetch - 1;
                prevfetch = 1;
            }
        }

        buffer.addLast(result, num);
        return num;
    }

    /**
     * return true if next() will return a result
     */
    public boolean hasNext() {
        return (bufpos < bufnum || ! haveLast);
    }

    /**
     * return true if previous() will return a result
     */
    public boolean hasPrevious() {
        return (abspos > 1);
    }

    private void complianceError(String msg) 
         throws RegistryFormatException
    {
        if (strictness > RegistrySearchClient.WARN_COMPLIANCE)
            throw new RegistryFormatException(msg);
        if (strictness == RegistrySearchClient.WARN_COMPLIANCE)
            System.err.println(msg);
    }

    /**
     * return the next resource description in the list of results
     */
    Element nextElement() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        if (! hasNext()) return null;

        if (++bufpos > bufnum) {
            if (buffer.next==null) retrieveNext();
            buffer = buffer.next;
            bufnum = buffer.getRecordCount();
            bufpos = 0;
            currentBatch = buffer.element;
            currentRecord = null;
        }
        if (bufpos > bufnum) {
            if (! hasNext()) return null;
            complianceError("VOResources element with numReturned=\"0\" but " +
                            "more=\"false\" (record no.=" + abspos + ").");
            return nextElement();
        }

        String underflow = "VOResources element has fewer records (" + bufpos +
            ") than expected (" + bufnum + ").";

        if (currentRecord == null) 
            currentRecord = currentBatch.getFirstChild();
        else 
            currentRecord = currentRecord.getNextSibling();
        if (currentRecord == null) {
            if (! hasNext()) return null;
            complianceError(underflow);
            return nextElement();
        }

        while (currentRecord.getNodeType() != Node.ELEMENT_NODE ||
               currentRecord.getLocalName() != elname) 
        {
            currentRecord = currentRecord.getNextSibling();
            if (currentRecord == null)  {
                if (! hasNext()) return null;
                complianceError(underflow);
                return nextElement();
            }
        }

        abspos++;
        return ((Element) currentRecord);
    }

    /**
     * return the previous resource description
     */
    Element previousElement() 
         throws RegistryServiceException, RegistryFormatException, 
                RegistryCommException
    {
        if (! hasPrevious()) return null;

        if (--bufpos <= 0) {
            if (buffer.prev==null) retrievePrevious();
            buffer = buffer.prev;
            bufnum = buffer.getRecordCount();
            bufpos = bufnum-1;
            currentBatch = buffer.element;
            currentRecord = null;
        }
        if (bufpos < 0) {
            if (! hasPrevious()) return null;
            complianceError("VOResources element with numReturned=\"0\" on " +
                            "previous records (record no.="+ abspos + ").");
            return previousElement();
        }

        String underflow = "VOResources element has fewer records (" + bufpos +
            ") than expected (" + bufnum + ").";

        if (currentRecord == null) 
            currentRecord = currentBatch.getLastChild();
        else 
            currentRecord = currentRecord.getPreviousSibling();
        if (currentRecord == null) return previousElement();

        while (currentRecord.getNodeType() != Node.ELEMENT_NODE ||
               currentRecord.getLocalName() != elname) 
        {
            currentRecord = currentRecord.getPreviousSibling();
            if (currentRecord == null) return previousElement();
        }
        
        abspos--;
        return ((Element) currentRecord);
    }

    class LinkedElement {
        public Element element = null;
        public LinkedElement next = null;
        public LinkedElement prev = null;
        int[] linkcount = { 1 };
        int reccount = 0;

        public LinkedElement(Element el) {
            this(el, 1);
        }

        public LinkedElement(Element el, int count) {
            element = el;
            reccount = count;
        }

        public int size() { return linkcount[0]; }

        public int getRecordCount() { return reccount; }

        public void addFirst(Element el, int count) {
            LinkedElement first = this;
            while (first.prev != null) first = first.prev;
            first.prev = new LinkedElement(el, count);
            first.prev.linkcount = linkcount;
            linkcount[0]++;
            first.prev.next = first;
        }

        public void addLast(Element el, int count) {
            LinkedElement last = this;
            while (last.next != null) last = last.next;
            last.next = new LinkedElement(el, count);
            last.next.linkcount = linkcount;
            linkcount[0]++;
            last.next.prev = last;
        }

        public void dropFirst() {
            LinkedElement first = this;
            while (first.prev != null) first = first.prev;
            if (first != this) {
                first.next.prev = null;
                first.next = null;
                linkcount[0]--;
            }
        }

        public void dropLast() {
            LinkedElement last = this;
            while (last.prev != null) last = last.prev;
            if (last != this) {
                last.prev.next = null;
                last.prev = null;
                linkcount[0]--;
            }
        }


    }
}
