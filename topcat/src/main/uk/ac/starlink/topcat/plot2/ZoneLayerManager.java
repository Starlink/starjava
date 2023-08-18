package uk.ac.starlink.topcat.plot2;

import java.util.List;
import java.util.Map;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotLayer;

/**
 * Abstraction for behaviour of controls that may work with multiple
 * zones, form controls and styles.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public interface ZoneLayerManager {

    /**
     * Indicates whether this control will yield any layers in its
     * current state.
     * It returns true if {@link #getLayers getLayers}
     * will return a non-empty array.
     * False positives are best avoided, but permitted.
     *
     * @return  true if there is a non-zero number of layers
     */
    boolean hasLayers();

    /**
     * Returns the layers contributed by this control.
     *
     * @param   ganger  ganger within which layers will be used
     * @return  layers
     */
    TopcatLayer[] getLayers( Ganger<?,?> ganger );

    /**
     * Returns a map associating plot styles with RowSubsets
     * for the current configuration.
     *
     * @return   ordered RowSubset-&gt;Styles map
     */
    Map<RowSubset,List<Style>> getStylesBySubset();

    /**
     * Returns a map associating plot layers with form controls that are
     * associated with this object.
     *
     * @param  ganger   ganger
     * @return  FormControl-&gt;PlotLayers map
     */
    Map<FormControl,List<PlotLayer>> getLayersByControl( Ganger<?,?> ganger );
}
