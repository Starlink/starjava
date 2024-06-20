package uk.ac.starlink.topcat.plot;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.StyleSet;

/**
 * StyleSet which obtains styles from a base StyleSet, but 
 * only dispenses ones which are not already used.  A global list
 * of used indices, which is shared with other instances of this class,
 * ensures that markers are not shared between them.
 * Since this also implements MutableStyleSet, individual styles can
 * be overwritten.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class PoolStyleSet implements MutableStyleSet {

    private final StyleSet base_;
    private final BitSet used_;

    /**
     * Map which keeps track of what markers are used by what indices.
     * The keys of the map are <code>Integer</code>s giving the mark style
     * index.  The values may be either an <code>Integer</code>, which 
     * indicates an index into the base style set list, or a 
     * <code>Style</code> which is a literal style to use.
     */
    private final Map<Integer,Object> map_;

    /**
     * Constructs a new StyleSet.
     *
     * @param   base  style set which supplies the actual symbols
     * @param   used  a bit vector, shared between a group of 
     *          PoolStyleSet, which keeps track of which
     *          styles (indices into <code>base</code>) are currently in use
     */
    public PoolStyleSet( StyleSet base, BitSet used ) {
        base_ = base;
        used_ = used;
        map_ = new HashMap<Integer,Object>();
    }

    public String getName() {
        return base_.getName();
    }

    public Style getStyle( int index ) {
        Object value = map_.get( Integer.valueOf( index ) );
        if ( value instanceof Integer ) {
            return base_.getStyle( ((Integer) value).intValue() );
        }
        else if ( value instanceof Style ) {
            return (Style) value;
        }
        else if ( value == null ) {
            int ibase = used_.nextClearBit( 0 );
            used_.set( ibase );
            map_.put( Integer.valueOf( index ), Integer.valueOf( ibase ) );
            return base_.getStyle( ibase );
        }
        else {
            throw new AssertionError();
        }
    }

    /**
     * Explicitly sets the style at a given index to be a specified one.
     *
     * @param   index  style index
     * @param   style  style to use
     */
    public void setStyle( int index, Style style ) {

        /* If the style previously at the reset index was previously 
         * using one from the base set, it is returned to the unused pool. */
        // Object value = map_.get( Integer.valueOf( index ) );
        // if ( value instanceof Integer ) {
        //     used_.clear( ((Integer) value).intValue() );
        // }

        map_.put( Integer.valueOf( index ), style );
    }

    /**
     * Resets all the symbols to be ones from the base set.
     * This also has the effect of returning any styles owned by this
     * set to the pool.
     */
    public void reset() {
        for ( Iterator<?> it = map_.values().iterator(); it.hasNext(); ) {
            Object value = it.next();
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
