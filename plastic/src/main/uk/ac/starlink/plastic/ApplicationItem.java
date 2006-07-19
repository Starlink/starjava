package uk.ac.starlink.plastic;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an application which has been registered with a PLASTIC hub.
 * Instances of this class are just designed to hold information, 
 * you can't actually invoke their methods.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2006
 */
public class ApplicationItem {

    private final URI id_;
    private final String name_;
    private final List messages_;
    private String tag_;

    /**
     * Constructor.
     *
     * @param   id   application ID
     * @param   name  application type name
     * @param   supportedMessages  list of URIs representing messages which 
     *          are understood by this application
     */
    public ApplicationItem( URI id, String name, List supportedMessages ) {
        id_ = id;
        name_ = name;
        messages_ = new ArrayList( supportedMessages );
        tag_ = name;
    }

    /**
     * Returns the unique registration ID for this application.
     *
     * @return  registration ID
     */
    public URI getId() {
        return id_;
    }

    /**
     * Returns the application name.
     *
     * @return   application name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the list of messages supported by this application
     *
     * @return  list of message id URIs
     */
    public List getSupportedMessages() {
        return Collections.unmodifiableList( messages_ );
    }

    /**
     * Sets the tag for this object.  This is intended to be a human-readable
     * but unique string.  
     * It may be used as the return value of {@link #toString}.
     * 
     * @param   tag  short tag string
     */
    public void setTag( String tag ) {
        tag_ = tag;
    }

    /**
     * Returns the tag string for this object.
     *
     * @return  tag  short tag string
     */
    public String getTag() {
        return tag_;
    }

    public String toString() {
        return getTag();
    }

    /**
     * Equality is assessed only on the value of the ID attribute.
     */
    public boolean equals( Object other ) {
        return other instanceof ApplicationItem 
            && ((ApplicationItem) other).id_.equals( id_ );
    }

    public int hashCode() {
        return id_.hashCode();
    }
}
