package uk.ac.starlink.tplot;

import java.io.IOException;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Describes a set of points and associated grouping and style information
 * selected for plotting.  Concrete implementations will have some
 * additional method which can obtain the points data itself.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2008
 */
public interface PointSelection {

    /**
     * Returns a list of subsets to be plotted.  The row indices used by these
     * subsets correspond to the row sequence associated with this object.
     *
     * @return  subset array
     */
    RowSubset[] getSubsets();

    /**
     * Returns a list of styles for subset plotting.
     * This corresponds to the subset list returned by
     * {@link #getSubsets}.
     *
     * @return  style array
     */
    public Style[] getStyles();
}
