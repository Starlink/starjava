// Copyright 1999-2001
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ResourceMap.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
//

package jsky.util;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * A ResourceMap provdes a map of resources such that frequently used
 * resources, such as images and icons can be reused.
 * <p>
 * Currently, this is a simple {@link HashMap}, but could
 * be evolved to use weak references.
 * <p>
 * Currently only Icon special routines are implemented.
 */
public final class ResourceMap {

    // Private variable to indicate the rough size of the map
    private static final int DEFAULT_CACHE_CAPACITY = 25;
    // The private HashMap
    private Map _map;

    /**
     * Create a new ResourceMap.
     */
    public ResourceMap() {
        this(DEFAULT_CACHE_CAPACITY);
    }

    /**
     * Create a new ResourceMap with a start up capacity.
     */
    public ResourceMap(int capacity) {
        _map = new HashMap(capacity);
    }

    /**
     * Checks and returns a cached {@link Icon}.
     *
     * @return the cached <code>Icon</code> or null if not present.
     */
    public Icon getIcon(String iconName) {
        Icon icon = (Icon) _map.get(iconName);
        return icon;
    }


    /**
     * Stores an Icon with the given resourceName.
     *
     */
    public void storeIcon(String iconName, Icon icon) {
        // If already present, it's overwritten
        _map.put(iconName, icon);
    }

}
