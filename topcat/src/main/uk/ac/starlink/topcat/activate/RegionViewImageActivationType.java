package uk.ac.starlink.topcat.activate;

/**
 * ActivationType for displaying an image with a highlighted region
 * in an internal viewer.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2018
 */
public class RegionViewImageActivationType
        extends GenericViewImageActivationType {
    public RegionViewImageActivationType() {
        super( true );
    }
}
