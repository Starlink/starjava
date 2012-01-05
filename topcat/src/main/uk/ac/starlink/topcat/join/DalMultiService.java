package uk.ac.starlink.topcat.join;

import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.Footprint;
import uk.ac.starlink.vo.Capability;

/**
 * Defines service-type-specific aspects of how to do a multiple query 
 * against a positional (cone-like) DAL service.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2009
 */
public interface DalMultiService {

    /**
     * Returns the name of this service type.
     *
     * @return  short name
     */
    String getName();

    /**
     * Returns a short label for this service type.
     *
     * @return  short label - no spaces, just a few lower case characters
     */
    String getLabel();

    /**
     * Returns the capability defining this service type.
     *
     * @return  capapbility type
     */
    Capability getCapability();

    /**
     * Returns the voresource subtype for this service as used in
     * voresource.loadlist.* MTypes.
     *
     * @return   voresource MType subtype
     */
    String getResourceListType();

    /**
     * Returns metadata describing the search radius (or diameter, or whatever)
     * parameter used by this query.
     *
     * @return   search size metadata
     */
    ValueInfo getSizeInfo();

    /**
     * Configures the column selector representing search radius 
     * (or diameter, or whatever) to some sensible default value.
     *
     * @param  sizeSelector   search size value selector component
     */
    void setSizeDefault( ColumnSelector sizeSelector );

    /**
     * Returns a panel with custom controls specific to this service type.
     *
     * @return   custom component container, or null
     */
    JComponent getControlPanel();

    /**
     * Constructs a cone searcher object for this service type.
     *
     * @param   url  service URL
     * @param   tfact  table factory
     */
    ConeSearcher createSearcher( URL url, StarTableFactory tfact );

    /**
     * Indicates whether this service is capable of supplying footprint
     * information.
     *
     * @return   false if <code>getFootprint</code> will always return false
     */
    boolean hasFootprints();

    /**
     * Gets a coverage footprint for this service.
     *
     * @param    url   service URL
     * @return  coverage footprint object, or null
     */
    Footprint getFootprint( URL url );
}
