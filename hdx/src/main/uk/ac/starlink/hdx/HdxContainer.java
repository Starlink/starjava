package uk.ac.starlink.hdx;

/**
 * An <code>HdxContainer</code> is the Java representation of an HDX object.
 *
 * <p>This interface should be regarded as rather provisional at present.  
 */
public interface HdxContainer {

    /**
     * Returns a list of objects from the Hdx.  The objects returned will be
     * of the Java type returned by {@link
     * HdxResourceType#getConstructedClass}, if that is non-null.
     *
     * @param type the type of object to return
     *
     * @return a List of objects of the required type.  If there are no
     * appropriate objects available, it returns an empty list.
     */
    public java.util.List getList(HdxResourceType type);

    /** Returns a single object from the HDX.  If there is more than one, 
     * this will return any one of them.  The object returned will be
     * of the Java type returned by {@link
     * HdxResourceType#getConstructedClass}, if that is non-null.
     *
     * @param type the type of object to return
     *
     * @return an object of the required type, or null if there is no
     * appropriate object in the HDX
     */
    public Object get(HdxResourceType type);

    /** Obtains a DOM representing the HDX. 
     *
     * @return a DOM (what more can one say?)
     */
    public org.w3c.dom.Element getDOM();

    /** Obtains a {@link javax.xml.transform.Source} representing the HDX.
     *
     * @return a Source which represents the HDX.
     */
    public javax.xml.transform.Source getSource();
}
