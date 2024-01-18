package uk.ac.starlink.topcat.plot2;

import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.util.Bi;

/**
 * GUI component for obtaining data position coordinates.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public interface PositionCoordPanel extends CoordPanel {

    /**
     * Returns the position geometry that defines the mapping of input
     * to data coordinates.
     *
     * @return  data geom
     */
    DataGeom getDataGeom();

    /**
     * Returns definitions for additional tabs to add alongside the
     * main Position tab in the FormLayerControl.
     * In most cases an empty list will be returned.
     *
     * @return  list of (TabName,TabContent) pairs to add
     */
    default List<Bi<String,JComponent>> getExtraTabs() {
        return Collections.emptyList();
    }
}
