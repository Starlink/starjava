package uk.ac.starlink.vo;

/**
 * Describes an element of the VOResource data model for use by
 * resource query interfaces.
 * A non-exhaustive selection of useful instances is provided as
 * static final members.
 *
 * @see  <a href="http://www.ivoa.net/Documents/latest/VOResource.html"
 *          >IVOA VOResource Recommendation</a>
 */
public class ResourceField {

    private final String label_;
    private final String xpath_;

    /** ShortName field. */
    public static final ResourceField SHORTNAME =
        new ResourceField( "Short Name", "shortName" );

    /** Title field. */
    public static final ResourceField TITLE =
        new ResourceField( "Title", "title" );

    /** Subjects field. */
    public static final ResourceField SUBJECTS =
        new ResourceField( "Subjects", "content/subject" );

    /** IVO ID field. */
    public static final ResourceField ID =
        new ResourceField( "ID", "identifier" );

    /** Publisher field. */
    public static final ResourceField PUBLISHER =
        new ResourceField( "Publisher", "curation/publisher" );

    /** Description field. */
    public static final ResourceField DESCRIPTION =
        new ResourceField( "Description", "content/description" );

    /**
     * Constructor.
     *
     * @param   label    user-directed short text label
     * @param   xpath    XPath into VOResource data model
     */
    public ResourceField( String label, String xpath ) {
        label_ = label;
        xpath_ = xpath;
    }

    /**
     * Returns a user-directed short text label for this field.
     *
     * @return  short label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the XPath into the VOResource data model for this field.
     *
     * @return  xpath
     */
    public String getXpath() {
        return xpath_;
    }
}
