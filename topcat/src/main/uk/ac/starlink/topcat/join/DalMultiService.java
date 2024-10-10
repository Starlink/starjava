package uk.ac.starlink.topcat.join;

import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeVerbosity;
import uk.ac.starlink.vo.RegistryPanel;

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
     * @return  capability type
     */
    Capability getCapability();

    /**
     * Returns the type of cone search service used.
     *
     * @return   service type
     */
    ConeServiceType getServiceType();

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
     * Indicates whether a blank value is permissible for the search
     * radius (or diameter, or whatever).
     *
     * @return  true iff null size values make sense for this service type
     */
    boolean allowNullSize();

    /**
     * Returns a panel with custom controls specific to this service type.
     *
     * @return   custom component container, or null
     */
    JComponent getControlPanel();

    /**
     * Returns a panel used for protocol version selection, if available.
     *
     * @return  version selector component, or null
     */
    JComponent getVersionComponent();

    /**
     * Performs any required initialisation based on the registry panel
     * that will be associated with this service.
     *
     * @param   regPanel  panel used for service selection
     */
    void init( RegistryPanel regPanel );

    /**
     * Constructs a cone searcher object for this service type.
     *
     * @param   url  service URL
     * @param   tfact  table factory
     * @param   coding  controls HTTP-level byte stream compression;
     *                  this hint may be ignored by implementations
     * @return   cone searcher object
     */
    ConeSearcher createSearcher( URL url, StarTableFactory tfact,
                                 ContentCoding coding );

    /**
     * Indicates whether this service is capable of supplying coverage
     * information.
     *
     * @return   false if <code>getCoverage</code> will always return false
     */
    boolean hasCoverages();

    /**
     * Gets a coverage description for this service.
     *
     * @param    url   service URL
     * @return  coverage coverage object, or null
     */
    Coverage getCoverage( URL url );

    /**
     * Returns the verbosity for this service.
     *
     * @return  verbosity level
     */
    ConeVerbosity getVerbosity();

    /**
     * Adds a listener that will be updated if the service characteristics
     * specified by this object may have changed.
     *
     * @param  l   listener to add
     */
    void addActionListener( ActionListener l );

    /**
     * Removes a previously added listener.
     *
     * @param  l  listener to remove
     */
    void removeActionListener( ActionListener l );
}
