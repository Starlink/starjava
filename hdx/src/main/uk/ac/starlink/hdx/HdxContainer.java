package uk.ac.starlink.hdx;

public interface HdxContainer {

    /** Returns a list of NDX objects from the HDX.  If there is no
     * NDX available, it returns an empty list.
     */
    public java.util.List getNdxList();

    /** Returns a single NDX from the HDX.  If there is more than one, 
     * this will return any one of them (in this present
     * implementation, it returns the first, but that may change).
     * 
     *<p>If there is no NDX in the HDX, it returns an empty NDX.
     */
    public Ndx getNdx();
}
