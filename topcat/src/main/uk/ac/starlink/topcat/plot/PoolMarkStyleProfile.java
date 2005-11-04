package uk.ac.starlink.topcat.plot;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * MarkStyleProfile which obtains symbols from a base profile, but 
 * only dispenses ones which are not already used.  A global list
 * of used indices, which is shared with other instances of this class,
 * ensures that markers are not shared between them.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class PoolMarkStyleProfile implements MarkStyleProfile {

    private final MarkStyleProfile base_;
    private final BitSet used_;

    /**
     * Map which keeps track of what markers are used by what indices.
     * The keys of the map are <code>Integer</code>s giving the mark style
     * index.  The values may be either an <code>Integer</code>, which 
     * indicates an index into the base profiles list, or a 
     * <code>MarkStyle</code> which is a literal style to use.
     */
    private final Map map_;

    /**
     * Constructs a new profile.
     *
     * @param   base  marker profile which supplies the actual symbols
     * @param   used  a bit vector, shared between a group of 
     *          PoolMarkStyleProfiles, which keeps track of which
     *          styles (indices into <code>base</code>) are currently in use
     */
    public PoolMarkStyleProfile( MarkStyleProfile base, BitSet used ) {
        base_ = base;
        used_ = used;
        map_ = new HashMap();
    }

    public String getName() {
        return base_.getName();
    }

    public MarkStyle getStyle( int index ) {
        Object value = map_.get( new Integer( index ) );
        if ( value instanceof Integer ) {
            return base_.getStyle( ((Integer) value).intValue() );
        }
        else if ( value instanceof MarkStyle ) {
            return (MarkStyle) value;
        }
        else if ( value == null ) {
            int ibase = used_.nextClearBit( 0 );
            used_.set( ibase );
            map_.put( new Integer( index ), new Integer( ibase ) );
            return base_.getStyle( ibase );
        }
        else {
            throw new AssertionError();
        }
    }

    /**
     * Explicitly sets the style at a given index to be a specified one.
     * If that index was previously using one from the base profile,
     * it is returned to the unused pool.
     *
     * @param   index  style index
     * @param   style  style to use
     */
    public void setStyle( int index, MarkStyle style ) {
        Object value = map_.get( new Integer( index ) );
        if ( value instanceof Integer ) {
            used_.clear( ((Integer) value).intValue() );
        }
        map_.put( new Integer( index ), style );
    }

    /**
     * Resets all the symbols to be ones from the base profile.
     * This also has the effect of returning any styles owned by this
     * profile to the pool.
     */
    public void reset() {
        for ( Iterator it = map_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Object value = entry.getValue();
            if ( value instanceof Integer ) {
                int ibase = ((Integer) value).intValue();
                assert used_.get( ibase );
                used_.clear( ibase );
            }
            it.remove();
        }
        assert map_.isEmpty();
    }
}
