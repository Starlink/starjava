package uk.ac.starlink.vo;

/**
 * Represents a TAPRegExt output format.
 * This is a declaration of support by a TAP service for a given TAP
 * result format.
 *
 * @author   Mark Taylor
 * @since    6 May 2015
 * @see  <a href="http://www.ivoa.net/documents/TAPRegExt/index.html"
 *          >TAPRegExt v1.0, section 2.4</a>
 */
public interface OutputFormat {

    /**
     * Returns the MIME type associated with this format.
     * According to TAPRegExt this ought not to be null,
     * but implementations may not enforce that.
     *
     * @return   format MIME type, perhaps qualified by parameters
     */
    String getMime();

    /**
     * Returns a list of zero or more aliases for this format.
     *
     * @return  alias list
     */
    String[] getAliases();

    /**
     * Returns an optional IVO-ID associated with this format.
     *
     * @return  ivoi-id, or null
     */
    Ivoid getIvoid();
}
