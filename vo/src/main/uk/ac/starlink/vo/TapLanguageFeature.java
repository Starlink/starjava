package uk.ac.starlink.vo;

/**
 * Describes a non-standard or optional feature of a declared TAP query
 * language.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2011
 */
public interface TapLanguageFeature {

    /**
     * Returns the form of this feature.
     *
     * @return   formal description
     */
    String getForm();

    /**
     * Returns a textual description of this feature.
     *
     * @return   free-text description
     */
    String getDescription();
}
