package uk.ac.starlink.connect;

/**
 * Describes an item of authorization information required when connecting
 * to a remote service.  This will typically be something like username,
 * password, etc.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class AuthKey {

    private String name_;
    private String description_;
    private String dfault_;
    private boolean hidden_;
    private boolean required_;

    /**
     * Constructs a new key with a given name.
     *
     * @param   name   name
     */
    public AuthKey( String name ) {
        name_ = name;
    }

    /**
     * Sets the name of this key.
     *
     * @param   name  name
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Returns the name of this key.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets the description of this key.
     * May be used as a tooltip or similar.
     *
     * @param  description  description
     */
    public void setDescription( String description ) {
        description_ = description;
    }

    /**
     * Returns the description of this key.
     * May be used as a tooltip or similar.
     *
     * @return   description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Sets a default value for this key.
     *
     * @param   dfault  default
     */
    public void setDefault( String dfault ) {
        dfault_ = dfault;
    }

    /**
     * Returns the default value for this key.
     *
     * @return  default
     */
    public String getDefault() {
        return dfault_;
    }

    /**
     * Sets whether this key's value will be hidden.
     * Typically set true for password-type fields in which the display
     * should not echo characters that are typed in.
     *
     * @param  hidden  true for fields whose values should not be shown
     */
    public void setHidden( boolean hidden ) {
        hidden_ = hidden;
    }

    /**
     * Returns true if this key's value should be hidden.
     * False by default.
     *
     * @return   hidden attribute
     */
    public boolean isHidden() {
        return hidden_;
    }

    /**
     * Sets whether this key must have a non-null value for a connection
     * attempt to proceed.
     *
     * @param   required  true iff this key must have a value
     */
    public void setRequired( boolean required ) {
        required_ = required;
    }

    /**
     * Indicates whether this key must have a non-null value.
     * False by default.
     *
     * @return   required attribute
     */
    public boolean isRequired() {
        return required_;
    }
}
