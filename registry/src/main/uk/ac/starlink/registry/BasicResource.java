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
    private String title_;
    private String identifier_;
    private String shortName_;
    private String publisher_;
    private String contact_;
    private String[] subjects_;
    private String referenceUrl_;
    private BasicCapability[] capabilities_;

    /**
     * Sets the resource title.
     *
     * @param  title  title
     */
    public void setTitle( String title ) {
        title_ = title;
    }

    /**
     * Returns the resource title.
     *
     * @return   title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Sets the resource short name.
     *
     * @param  shortName  short name
     */
    public void setShortName( String shortName ) {
        shortName_ = shortName;
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
     * Sets the resource unique identifier.
     *
     * @param  identifier   identifier URI
     */
    public void setIdentifier( String identifier ) {
        identifier_ = identifier;
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
     * Sets the resource publisher.
     *
     * @param  publisher  publisher
     */
    public void setPublisher( String publisher ) {
        publisher_ = publisher;
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
     * Sets the contact information.
     *
     * @param  contact  contact string
     */
    public void setContact( String contact ) {
        contact_ = contact;
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
     * Sets the subject strings.
     *
     * @param  subjects  array of subject strings
     */
    public void setSubjects( String[] subjects ) {
        subjects_ = subjects;
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
     * Sets the reference URL.
     *
     * @param  referenceUrl   reference URL
     */
    public void setReferenceUrl( String referenceUrl ) {
        referenceUrl_ = referenceUrl;
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
     * Sets the capability interface array.
     *
     * @param  capabilities   capability array
     */
    public void setCapabilities( BasicCapability[] capabilities ) {
        capabilities_ = capabilities;
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
