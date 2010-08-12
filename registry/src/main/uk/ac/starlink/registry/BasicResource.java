package uk.ac.starlink.registry;

/**
 * Basic details of a registry resource record.
 * This is an intentionally rather flattened and truncated version
 * of the information which can be stored in a registry record
 * (at time of writing VOResource 1.0).
 *
 * @author   Mark Taylor
 */
public class BasicResource {
    String title_;
    String identifier_;
    String shortName_;
    String publisher_;
    String contact_;
    String[] subjects_;
    String referenceUrl_;
    BasicCapability[] capabilities_;

    /**
     * Returns the resource title.
     *
     * @return   title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns the resource short name.
     *
     * @return   short name
     */
    public String getShortName() {
        return shortName_;
    }

    /**
     * Returns the resource unique identifier.
     * This is a URI of the form <code>ivo://authority/path<code>.
     *
     * @return  identifier
     */
    public String getIdentifier() {
        return identifier_;
    }

    /**
     * Returns the resource publisher.
     *
     * @return  publisher
     */
    public String getPublisher() {
        return publisher_;
    }

    /**
     * Returns the contact information for this resource.
     *
     * @return  contact info
     */
    public String getContact() {
        return contact_;
    }

    /**
     * Returns an array of subject strings for this resource.
     *
     * @return  subject array
     */
    public String[] getSubjects() {
        return subjects_;
    }

    /**
     * Returns the reference URL for this resource.
     *
     * @return   reference URL
     */
    public String getReferenceUrl() {
        return referenceUrl_;
    }

    /**
     * Returns an array of capability interfaces for this resource.
     * Very often there is only one (or zero) of these per resource,
     * but there can be many.
     *
     * @return  capabilities
     */
    public BasicCapability[] getCapabilities() {
        return capabilities_;
    }
}
