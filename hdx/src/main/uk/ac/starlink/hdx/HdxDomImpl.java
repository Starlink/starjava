package uk.ac.starlink.hdx;

import org.dom4j.*;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/** Implements HDX objects in terms of a DOM.
 */
class HdxDomImpl implements HdxContainer {

    Element hdxDocelem;

    /** Processes a single URL to extract a single HDX.  At present,
	we detect which type of file this is by examining the `file
	extension'.  This isn't terribly robust, but should work in
	the short term.
    */
    HdxDomImpl (Element docelem) {
	hdxDocelem = docelem;
    }

    /** Returns a list of NDX objects from the HDX.
	@return an empty list if there is no NDX available
    */
    public List getNdxList() {
	List ndxList = hdxDocelem.selectNodes
	    ("//" + HdxResourceType.NDX.xmlName());
	ArrayList retval = new ArrayList(ndxList.size());
	if (!ndxList.isEmpty())
	{
	    try {
		for (ListIterator li = ndxList.listIterator();
		     li.hasNext();
		     ) {
                    NdxImpl impl = new DomNdxImpl((Element) li.next());
                    Ndx ndx = new BridgeNdx(impl);
		    retval.add(ndx);
                }
	    } catch (HdxException ex) {
		System.err.println("Can't getNdx: " + ex);
	    }
	}
	return retval;
    }

    /** Returns a single NDX from the HDX.  If there is more than one, 
	this will return any one of them (in this present
	implementation, it returns the first, but that may change).

	@return null if there is no NDX in the HDX.
    */
    public Ndx getNdx() {
	List nl = getNdxList();
	if (nl.isEmpty())
	    return null;
	else
	    return (Ndx) nl.get(0);
    }
}
