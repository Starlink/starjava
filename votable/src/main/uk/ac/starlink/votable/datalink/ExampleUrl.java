package uk.ac.starlink.votable.datalink;

/**
 * Represents an example invocation from a Service Descriptor.
 * This is a representation of a PARAM element with @name="exampleURL".
 *
 * @author   Mark Taylor
 * @since    14 Apr 2023
 * @see  <a href="http://www.ivoa.net/documents/DataLink/"
 *          >DataLink 1.1, section 4.2</a>
 */
public interface ExampleUrl {

    /**
     * Returns the invocation URL for the example.
     * This is the value attribute of the PARAM.
     *
     * @return  invocation URL
     */
    String getUrl();

    /**
     * Returns a textual description of the example.
     * This is the content of the DESCRIPTION child of the PARAM.
     *
     * @return   description text, may be null
     */
    String getDescription();
}
