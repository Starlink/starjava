package uk.ac.starlink.ttools.plot2;

/**
 * Describes the result of a navigation request from the GUI.
 * Currently this aggregates two items, both optional:
 * the aspect object which describes the new
 * view of the plotting surface that should result from the action,
 * and a surface decoration for indicating to the user
 * the nature of the navigation in progress.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2014
 */
public class NavAction<A> {

    private final A aspect_;
    private final Decoration decoration_;

    /**
     * Constructor.
     *
     * @param   aspect   describes the new surface; null if no change
     * @param   decoration  decorates the surface to indicate navigation;
     *                      null if no decoration
     */
    public NavAction( A aspect, Decoration decoration ) {
        aspect_ = aspect;
        decoration_ = decoration;
    }

    /**
     * Returns the surface aspect describing the result of the navigation.
     *
     * @return   surface aspect, or null for no change
     */
    public A getAspect() {
        return aspect_;
    }

    /**
     * Returns a surface decoration giving a visual indication of
     * the nature of the navigation action in progress.
     *
     * @return  decoration, or null for no visual indication
     */
    public Decoration getDecoration() {
        return decoration_;
    }
}
