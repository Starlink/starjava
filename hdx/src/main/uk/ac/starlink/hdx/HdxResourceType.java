package uk.ac.starlink.hdx;

import java.util.Collections;
import org.dom4j.QName;

/** Encapsulates the types of data contained within NDX.  Each of the
    public fields exposed by this class ({@link #HDX}, {@link #NDX}
    and so on) can be used as a type indicator, which additionally
    encapsulates the various properties that type has.
 
    @author Norman Gray (norman@astro.gla.ac.uk)
    @version $Id$
*/
public class HdxResourceType {
    // This is an example of the typesafe enum pattern.
    private final String name;

    private HdxResourceType(String name) { this.name = name; }

    /** The namespace for HDX */
    public static final String HDX_NAMESPACE
	    = "http://www.starlink.ac.uk/HDX";

    public static final HdxResourceType HDX
	    = new HdxResourceType("hdx");
    public static final HdxResourceType NDX
	    = new HdxResourceType("ndx");
    public static final HdxResourceType DATA
	    = new HdxResourceType("data");
    public static final HdxResourceType VARIANCE
	    = new HdxResourceType("variance");
    public static final HdxResourceType QUALITY
	    = new HdxResourceType("quality");
    public static final HdxResourceType TITLE
            = new HdxResourceType("title");
    public static final HdxResourceType BADBITS
            = new HdxResourceType("badbits");
    public static final HdxResourceType WCS
            = new HdxResourceType("wcs");
    public static final HdxResourceType OTHER
	    = new HdxResourceType("other"); // this is really an error return

    /** Returns a printable version of the resource type */
    public String toString() { return name; }

    /** Returns the name of the resource type as found in the 
	XML serialization. */
    public String xmlName() { return name; }

    /** Returns the property name corresponding to this type */
    public String propertyName() { return name; }

    /** Returns a QName referring to the object. */
    public QName qName() {
	// The namespace prefix is arbitrary
	return new QName (name,
			  new org.dom4j.Namespace("x", HDX_NAMESPACE));
    }

    /** Returns a HdxResourceType object which matches the specified QName.
     * @return one of the static resource type constants, or a `type'
     * OTHER if the string does not match anything.
     */
    static public HdxResourceType match(QName q) {
	// XXX bogus: should match namespace, too
	String s = q.getName();
	HdxResourceType result;
	if (s.equals(NDX.name))
	    result = NDX;
	else if (s.equals(DATA.name))
	    result = DATA;
	else if (s.equals(VARIANCE.name))
	    result = VARIANCE;
	else if (s.equals(QUALITY.name))
	    result = QUALITY;
	else if (s.equals(HDX.name))
	    result = HDX;
        else if (s.equals(TITLE.name))
            result = TITLE;
        else if (s.equals(BADBITS.name))
            result = BADBITS;
        else if (s.equals(WCS.name))
            result = WCS;
	else
	    result = OTHER;

	System.err.println("HdxResourceType.match(" + q.getName() + ")="
			   + result);
	return result;
    }

    /** Returns an iterator containing the HdxResourceType objects
	which are legitimate children of an NDX. */
    static public java.util.ListIterator getChildrenIterator() {
	java.util.ArrayList l = new java.util.ArrayList();
	l.add(DATA);
	l.add(VARIANCE);
	l.add(QUALITY);
        l.add(TITLE);
        l.add(BADBITS);
        l.add(WCS);
	return Collections.unmodifiableList( l ).listIterator();
    }

    /** Returns a HdxResourceType object which matches the specified QName.
     * @return one of the static resource type constants, or a `type'
     * OTHER if the string does not match anything.
     * @see #match(QName)
     */
    static public HdxResourceType match(String s) {
	return match(new QName(s));
    }
}
