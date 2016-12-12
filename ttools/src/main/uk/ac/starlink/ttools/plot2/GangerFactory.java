package uk.ac.starlink.ttools.plot2;

/**
 * Generates Ganger instances for a particular purpose,
 * for instance a particular type of plot,
 * taking account of supplied user preferences.
 * At present, the user preferences are just in the form of
 * required margins outside the plot area, but it may acquire more
 * detailed functions in the future.
 *
 * <p>You can find a basic single-zone implementation at
 * {@link SingleGanger#FACTORY}.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2016
 */
public interface GangerFactory<P,A> {

    /**
     * Indicates whether this ganger may generate multi-zone plots.
     *
     * @return  false if returned gangers are always single-zone
     */
    boolean isMultiZone();

    /**
     * Returns a ganger given user margin preferences.
     *
     * @param   padding  required padding around plot area
     */
    Ganger<P,A> createGanger( Padding padding );
}
