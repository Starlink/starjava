package uk.ac.starlink.topcat.plot;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension of the StyleSet interface which allows entries to be changed.
 *
 * @author   Mark Taylor
 * @since    6 Jan 2006
 */
public interface MutableStyleSet extends StyleSet {

    /**
     * Overwrites one entry of this style set.
     *
     * @param   index  index of entry to overwrite
     * @param   style  new style for entry <code>index</code>
     */
    public void setStyle( int index, Style style );
}
