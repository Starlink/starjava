package uk.ac.starlink.vo;

/**
 * Basic details of a registry resource record.
 * This is an intentionally rather flattened and truncated version 
 * of the information which can be stored in a registry record 
 * (at time of writing VOResource 1.0).
 *
 * @author   Mark Taylor
 * @since    17 Dec 2008
 */
public interface RegResource {

    /**
     * Returns the resource title.
     *
     * @return   title
     */
    String getTitle();

    /**
     * Returns the resource short name.
     *
     * @return   short name
     */
    String getShortName();

    /**
     * Returns the resource unique identifier.
     * This is a URI of the form <code>ivo://authority/path<code>.
     * 
     * @return  identifier
     */
    String getIdentifier();

    /**
     * Returns the resource publisher.
     *
     * @return  publisher
     */
    String getPublisher();

    /**
     * Returns the contact information for this resource.
     *
     * @return  contact info
     */
    String getContact();

    /**
     * Returns a list of subject areas relevant to this resource.
     *
     * @return   subject words
     */
    String[] getSubjects();

    /**
     * Returns the reference URL for this resource.
     *
     * @return   reference URL
     */
    String getReferenceUrl();

    /**
     * Returns an array of capability interfaces for this resource.
     * Very often there is only one (or zero) of these per resource,
     * but there can be many.
     *
     * @return  capabilities
     */
    RegCapabilityInterface[] getCapabilities();
}
  
