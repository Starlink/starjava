package uk.ac.starlink.ttools.plot2.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ConfigKey that allows named choices from a given list,
 * and optionally provides other ways of specifying values
 * from string descriptions.
 *
 * @author   Mark Taylor
 * @since    10 Sep 2014
 */
public abstract class ChoiceConfigKey<T> extends ConfigKey<T> {

    private final boolean nullPermitted_;
    private final Map<String,T> optMap_;

    /**
     * Constructor.
     *
     * @param  meta   metadata
     * @param  clazz   value class
     * @param  dflt   default value
     * @param  nullPermitted  true iff null is a permitted value
     */
    public ChoiceConfigKey( ConfigMeta meta, Class<T> clazz, T dflt,
                            boolean nullPermitted ) {
        super( meta, clazz, dflt );
        nullPermitted_ = nullPermitted;
        optMap_ = new LinkedHashMap<String,T>();
    }

    /**
     * Adds an option to the permitted list.
     * Its name is obtained using the {@link #stringifyValue} method,
     * which must return a non-null value.
     *
     * <p>For more flexibility, you can manipulate the return value of
     * {@link #getOptionMap} directly.
     *
     * @param  option  option to add
     */
    public void addOption( T option ) {
        String sval = stringifyValue( option );
        if ( sval == null ) {
            throw new IllegalArgumentException( "Can't stringify " + option );
        }
        optMap_.put( sval, option );
    }

    /**
     * Returns a mutable map giving the currently available known options
     * and their string values.
     *
     * @return   current name-&gt;value map of known options
     */
    public Map<String,T> getOptionMap() {
        return optMap_;
    }

    /**
     * Takes a string, and attempts to turn it into an object which may
     * be a value for this key.
     * If the string is not of a recognised form, null is returned.
     *
     * <p>This method should be the opposite of {@link #stringifyValue},
     * but does not need to be consistent with 
     * {@link #stringToValue stringToValue} or
     * {@link #valueToString valueToString}.
     *
     * @param  sval    string representation
     * @return   typed object represented by sval, or null
     */
    public abstract T decodeString( String sval );

    /**
     * Takes an object which may be a value of this key,
     * and attempts to turn it into a string for reporting purposes.
     *
     * <p>This method should if possible
     * be the opposite of {@link #decodeString},
     * but does not need to be consistent with 
     * {@link #stringToValue stringToValue} or
     * {@link #valueToString valueToString}.
     * If no round-trippable value is available, null should be returned.
     *
     * @param  value  typed object
     * @return   string representing object, or null
     */
    public abstract String stringifyValue( T value );

    public T stringToValue( String sval ) throws ConfigException {
        if ( sval == null || sval.length() == 0 ) {
            if ( nullPermitted_ ) {
                return null; 
            }
            else {
                throw new ConfigException( this, "null not permitted" );
            }
        }
        T mapVal = optMap_.get( sval );
        if ( mapVal != null ) {
            return mapVal;
        }
        T decodeVal = decodeString( sval );
        if ( decodeVal != null ) {
            return decodeVal;
        }
        throw new ConfigException( this, "Unknown value \"" + sval + "\"" );
    }

    public String valueToString( T value ) {
        if ( value == null ) {
            return null;
        }
        for ( Map.Entry<String,T> entry : optMap_.entrySet() ) {
            if ( entry.getValue().equals( value ) ) {
                return entry.getKey();
            }
        }
        String sval = stringifyValue( value );
        if ( sval != null ) {
            return sval;
        }
        return sval.toString();
    }
}
