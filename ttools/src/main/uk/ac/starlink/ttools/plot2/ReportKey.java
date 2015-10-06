package uk.ac.starlink.ttools.plot2;

/**
 * Typed key for use in a ReportMap.
 * Instances of this class identify an item of data generated when
 * plotting a layer.
 * 
 * @author   Mark Taylor
 * @since    9 Dec 2014
 */
public class ReportKey<T> {

    private final ReportMeta meta_;
    private final Class<T> clazz_;
    private final boolean isGeneralInterest_;

    /**
     * Constructor.
     *
     * @param   meta   metadata describing this key
     * @param   clazz  type of data item described by this key
     * @param   isGeneralInterest  indicates whether this key represents
     *          a general purpose report
     */
    public ReportKey( ReportMeta meta, Class<T> clazz,
                      boolean isGeneralInterest ) {
        meta_ = meta;
        clazz_ = clazz;
        isGeneralInterest_ = isGeneralInterest;
    }

    /**
     * Returns this key's metadata.
     *
     * @return  descriptive metadata
     */
    public ReportMeta getMeta() {
        return meta_;
    }

    /**
     * Returns the type of object identified by this key.
     *
     * @return   value class
     */
    public Class<T> getValueClass() {
        return clazz_;
    }

    /**
     * Indicates whether this key represents a key of general interest.
     * General interest reports can/should be presented to the user by a
     * general purpose UI as plot feedback and the corresponding values
     * should have a sensible toString implemenatation.
     * If the return value is false, the corresponding report is only
     * intended for plotter-specific code that understands what it's getting.
     *
     * @return   true  if general purpose code should present report items
     *                 to the user in their stringified form
     */
    public boolean isGeneralInterest() {
        return isGeneralInterest_;
    }
}
