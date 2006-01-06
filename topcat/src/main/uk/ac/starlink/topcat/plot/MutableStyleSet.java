package uk.ac.starlink.topcat.plot;

import java.util.HashMap;
import java.util.Map;

/**
 * StyleSet implementation which allows entries to be changed.
 * Its basic behaviour is given by a supplied base style set, but any
 * entry may be overwritten using the {@link #setStyle} method.
 *
 * @author   Mark Taylor
 * @since    6 Jan 2006
 */
public class MutableStyleSet implements StyleSet {

    private final StyleSet base_;
    private final Map modified_;

    /**
     * Constructs a new mutable style set based on a given one.
     *
     * @param   base  base style set
     */
    public MutableStyleSet( StyleSet base ) {
        base_ = base;
        modified_ = new HashMap();
    }

    public String getName() {
        return base_.getName();
    }

    /**
     * Overwrites one entry of this style set.
     *
     * @param   index  index of entry to overwrite
     * @param   style  new style for entry <code>index</code>
     */
    public void setStyle( int index, Style style ) {
        modified_.put( new Integer( index ), style );
    }

    public Style getStyle( int index ) {
        Integer key = new Integer( index );
        return modified_.containsKey( key )
             ? (Style) modified_.get( key )
             : base_.getStyle( index );
    }
}
