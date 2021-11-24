package uk.ac.starlink.pds4;

import java.net.URL;

/**
 * Returns the result of parsing a PDS4 label XML file.
 * This contains only items of interest for looking at table data.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public interface Label {

    /**
     * Returns the parent URL in which this label was located.
     * It can be used to resolve relative data locations.
     *
     * @return   returns parent URL of this label
     */
    URL getContextUrl();

    /**
     * Returns any Table items that are associated with this label.
     *
     * @return  parsed table array
     */
    Table[] getTables();
}
