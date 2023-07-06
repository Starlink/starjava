package uk.ac.starlink.vo;

import java.util.Map;

/**
 * Describes a query language as declared by a TAP capabilities record.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2011
 */
public interface TapLanguage {

    /**
     * Returns the language name.
     *
     * @return  name with no version suffix
     */
    String getName();

    /**
     * Returns version strings.
     *
     * @return  array of supported version names, same length as 
     *          <code>getVersionIds</code> array
     */
    String[] getVersions();

    /**
     * Returns version IVO-IDs.
     *
     * @return   array of IVO-IDs associated with supported versions,
     *           same length as <code>getVersions</code> array
     */
    Ivoid[] getVersionIds();

    /**
     * Returns a textual description of this language.
     *
     * @return  description string
     */
    String getDescription();

    /**
     * Returns a map of language features for this language.
     * Map keys are the language feature "type" IVOIDs, and the
     * values are arrays of features with that type.
     *
     * @return   type-&gt;feature list map
     */
    Map<Ivoid,TapLanguageFeature[]> getFeaturesMap( );
}
