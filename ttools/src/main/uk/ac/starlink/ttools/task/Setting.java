package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Defines a key-value pair for use in a stilts command line.
 * This representation focuses on the text values; there is not
 * much in the way of type safety, which should be enforced if
 * possible by earlier processing.
 *
 * <p>The equality constraint is required to aid factorisation.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2017
 */
@Equality
public class Setting {

    private final String key_;
    private final String strValue_;
    private final String strDflt_;
    private Object objValue_;
    private Credibility credibility_;

    /**
     * Constructs a setting.  The attributes set here are immutable
     * and constitute the items used to assess equality.
     *
     * @param  key       stilts parameter name
     * @param  strValue  string representation of stilts parameter value;
     *                   corresponds to actual value
     * @param  strDflt   string representation of stilts parameter default
     */
    public Setting( String key, String strValue, String strDflt ) {
        key_ = key;
        strValue_ = strValue;
        strDflt_ = strDflt;
        credibility_ = Credibility.YES;
    }

    /**
     * Returns this setting's parameter name.
     *
     * @return  key string
     */
    public String getKey() {
        return key_;
    }

    /**
     * Returns the string representation of this setting's value.
     *
     * @return  string value
     */
    public String getStringValue() {
        return strValue_;
    }

    /**
     * Returns the string representation of the default value for
     * this setting.
     *
     * @return  default value
     */
    public String getStringDefault() {
        return strDflt_;
    }

    /**
     * Indicates whether this setting's value is the same as the
     * default value.  If so, for most purposes, specifying it
     * has no effect.
     *
     * @return  true iff this setting's value and default are equivalent
     */
    public boolean isDefaultValue() {
        return ( strValue_ == null || strValue_.trim().length() == 0 )
             ? ( strDflt_ == null || strDflt_.trim().length() == 0 )
             : strValue_.equals( strDflt_ );
    }

    /**
     * Sets the typed value associated with this setting.
     * This is optional, but if present gives to the typed value
     * that the parameter would assume, corresponding to the string value.
     *
     * @param  objValue    typed value for setting
     */
    public void setObjectValue( Object objValue ) {
        objValue_ = objValue;
    }

    /**
     * Returns the typed value for this setting, if known.
     *
     * @return  setting value as a typed object, or null if not known
     */
    public Object getObjectValue() {
        return objValue_;
    }

    /**
     * Sets the credibility of this setting.
     * The default value is {@link Credibility#YES}.
     *
     * @param  cred  credibility level
     */
    public void setCredibility( Credibility cred ) {
        credibility_ = cred;
    }

    /** 
     * Returns the credibility of this setting.
     * The default value is {@link Credibility#YES}.
     *
     * @return  credibility level
     */
    public Credibility getCredibility() {
        return credibility_;
    }

    /**
     * Creates a new setting object which resembles this one,
     * but whose parameter name is modified by addition of a given
     * trailing string.
     *
     * @param  suffix   string to append to parameter name
     * @return   new setting object with suffix
     */
    public Setting appendSuffix( String suffix ) {
        Setting s = new Setting( key_ + suffix, strValue_, strDflt_ );
        s.setObjectValue( this.getObjectValue() );
        s.setCredibility( this.getCredibility() );
        return s;
    }

    /**
     * Creates a new setting object which resembles this one,
     * except it has a different default value.
     *
     * @param  strDflt  new default string
     * @return  new setting object with adjusted default
     */
    public Setting resetDefault( String strDflt ) {
        Setting s = new Setting( key_, strValue_, strDflt );
        s.setObjectValue( this.getObjectValue() );
        s.setCredibility( this.getCredibility() );
        return s;
    }

    @Override
    public int hashCode() {
        int code = 442990;
        code = 23 * code + key_.hashCode();
        code = 23 * code + PlotUtil.hashCode( strValue_ );
        code = 23 * code + PlotUtil.hashCode( strDflt_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Setting ) {
            Setting other = (Setting) o;
            return this.key_.equals( other.key_ )
                && PlotUtil.equals( this.strValue_, other.strValue_ )
                && PlotUtil.equals( this.strDflt_, other.strDflt_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer()
           .append( key_ )
           .append( '=' )
           .append( strValue_ );
        if ( isDefaultValue() ) {
            sbuf.append( " (dflt)" );
        }
        return sbuf.toString();
    }
}
