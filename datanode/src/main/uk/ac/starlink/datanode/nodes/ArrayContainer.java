package uk.ac.starlink.datanode.nodes;

import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.ast.FrameSet;

/**
 * Struct-type class to contain an array and optionally a corresponding 
 * WCS frameset
 */
public class ArrayContainer {

    private final NDArray nda_;
    private final FrameSet wcs_;
 
    /**
     * Constructs a new array container.
     *
     * @param  nda  array
     * @param  wcs  frameset describing <tt>nda</tt>.  May be null.
     */
    public ArrayContainer( NDArray nda, FrameSet wcs ) {
        nda_ = nda;
        wcs_ = wcs;
    }

    /**
     * Returns the array.
     *
     * @return  array
     */
    public NDArray getArray() {
        return nda_;
    }

    /**
     * Returns the WCS frameset.
     *
     * @return  WCS frameset if there is one, or <tt>null</tt>
     */
    public FrameSet getWCS() {
        return wcs_;
    }
}
