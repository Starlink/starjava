package uk.ac.starlink.treeview;

import uk.ac.starlink.util.DataSource;

/**
 * Extends the <tt>DataSource</tt> class to provide a full path name.
 * This is used by some treeview classes when presenting the path to 
 * the user.  This class should be extended in preference to plain
 * <tt>DataSource</tt> if a full path can be supplied.
 */
public abstract class PathedDataSource extends DataSource {

    /**
     * Returns a pathname, preferably absolute, for this DataSource.
     *
     * @return  the path
     */
    public abstract String getPath();
}
