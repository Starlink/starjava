package uk.ac.starlink.topcat.vizier;

/**
 * Defines a Vizier catalogue which may be searched or downloaded.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public interface Queryable {

    /**
     * Returns the source name of the catalogue, as presented to the
     * VizieR server's <code>-source</code> parameter.
     *
     * @return  source string
     */
    String getQuerySource();

    /**
     * Returns a short name suitable for presentation to the user to
     * represent the name of the catalogue.
     *
     * @return   identifier string
     */
    String getQueryId();
}
