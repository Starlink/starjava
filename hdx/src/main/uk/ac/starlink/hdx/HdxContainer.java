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

    /**
     * Returns a single object of the given type from the HDX.  If
     * there is more than one, this will return any one of them.  The
     * object returned will be of the Java type returned by {@link
     * HdxResourceType#getConstructedClass}, if that is non-null.
     *
     * @param type the type of object to return
     *
     * @return an object of the required type, or null if there is no
     * appropriate object in the HDX
     */
    public Object get(HdxResourceType type);

    /**
     * Obtains a DOM representing the HDX. 
     *
     * <p>The XML in general may contain URIs, for instance referencing the
     * array components of the NDX.  How these are written is determined
     * by the <code>base</code> parameter; URIs will be written as relative
     * URIs relative to <code>base</code> if this is possible (e.g. if they
     * share a part of their path).  If there is no common part of the
     * path, including the case in which <code>base</code> is <code>null</code>,
     * then an absolute reference will be written.
     *
     * @return an Element which is the document element of a DOM
     * representing the HDX
     */
    public org.w3c.dom.Element getDOM(java.net.URI base);

    /**
     * Obtains a {@link javax.xml.transform.Source} representing the
     * HDX.  The resulting <code>Source</code> is equivalent to the
     * DOM returned by {@link #getDOM}.
     *
     * <p>The XML in general may contain URIs, for instance referencing the
     * array components of the NDX.  How these are written is determined
     * by the <code>base</code> parameter; URIs will be written as relative
     * URIs relative to <code>base</code> if this is possible (e.g. if they
     * share a part of their path).  If there is no common part of the
     * path, including the case in which <code>base</code> is <code>null</code>,
     * then an absolute reference will be written.
     *
     * @return a Source which represents the HDX.
     */
    public javax.xml.transform.Source getSource(java.net.URI base);

//     /**
//      * Obtains the <code>HdxFactory</code> associated with this Hdx.
//      * This can be used to resolve URIs.
//      *
//      * @return the <code>HdxFactory</code> associated with this container
//      */
//     public HdxFactory getFactory();
}
