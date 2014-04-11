package uk.ac.starlink.vo;

/**
 * Describes an element of the VOResource data model for use by
 * resource query interfaces.
 *
 * <p>At present the private constructor means that only the static
 * members of this class are available for use.  Others could be constructed,
 * but only those defined here are handled by
 * {@link RegTapRegistryQuery#getAdqlCondition}.
 *
 * @see  <a href="http://www.ivoa.net/Documents/latest/VOResource.html"
 *          >IVOA VOResource Recommendation</a>
 */
public class ResourceField {

    private final String label_;
    private final String xpath_;
    private final String rrName_;

    /** ShortName field. */
    public static final ResourceField SHORTNAME =
        new ResourceField( "Short Name", "shortName", "short_name" );

    /** Title field. */
    public static final ResourceField TITLE =
        new ResourceField( "Title", "title", "res_title" );

    /** Subjects field. */
    public static final ResourceField SUBJECTS =
        new ResourceField( "Subjects", "content/subject", "res_subject" );

    /** IVO ID field. */
    public static final ResourceField ID =
        new ResourceField( "ID", "identifier", "ivoid" );

    /** Publisher field. */
    public static final ResourceField PUBLISHER =
        new ResourceField( "Publisher", "curation/publisher", null );

    /** Description field. */
    public static final ResourceField DESCRIPTION =
        new ResourceField( "Description", "content/description",
                           "res_description" );

    /**
     * Constructor.
     *
     * @param   label    user-directed short text label
     * @param   xpath    XPath into VOResource data model
     * @param   rrName   column name in Relational Registry schema
     */
    private ResourceField( String label, String xpath, String rrName ) {
        label_ = label;
        xpath_ = xpath;
        rrName_ = rrName;
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

    /**
     * Returns the column name of this field in the Registry Relational Schema.
     *
     * @return  relational registry name
     */
    public String getRelationalName() {
        return rrName_;
    }
}
