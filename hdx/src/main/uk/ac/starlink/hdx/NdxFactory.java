package uk.ac.starlink.hdx;

/** Factory returns objects implementing the {@link Ndx} interface.
 */
public class NdxFactory {

    /** Processes a single URL to extract a single NDX.
     * @return an Ndx object.  If there is more than one NDX in the
     *         resource referred to by the URL, then return only one
     *         of them (unspecified which one); if there is none in
     *         the resource referred to, it returns an empty one.
     * @throws HdxException if we can't read the URL, for
     *         whatever reason.
     */
    public static Ndx getNdx (java.net.URL url) throws HdxException {
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx = hdxf.readHdx (url);
        return hdx.getNdx();
    }

    /** Processes a URL to extract a list of the NDXs contained within
     * the resource it refers to.  If there are no NDXs in the HDX,
     * it returns an empty list.
     *
     * @throws HdxException if we can't read the URL, for whatever reason.
     */
    public static java.util.List getNdxList (java.net.URL url)
            throws HdxException {
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx = hdxf.readHdx (url);
        return hdx.getNdxList();
    }

    /** Extracts a single NDX from an org.w3c.dom.Element.
     *
     * @return an Ndx object.  If there is more than one NDX in the
     *         resource referred to by the URL, then return only one
     *         of them (unspecified which one); if there is none in
     *         the resource referred to, it returns an empty one.
     *
     * @throws HdxException if we can't read the URL, for whatever reason.
     */
    public static Ndx getNdx (org.w3c.dom.Element w3cElement)
            throws HdxException {
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx = hdxf.readHdx(w3cElement);
        return hdx.getNdx();
    }
}
