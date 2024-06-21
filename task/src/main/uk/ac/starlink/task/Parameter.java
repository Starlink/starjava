package uk.ac.starlink.task;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Comparator;

/**
 * A Parameter describes the function of one of a task's parameters.
 * An instance of the class encapsulates a parameter's name, value type,
 * prompt string, default value, position on the command line,
 * and so on.  It can also validate, hold and clear the value of the
 * parameter.  The parameter value is acquired from an associated
 * Environment in either String or typed form, and the Parameter
 * object configures its value from that.
 * <p>
 * This class must be subclassed according to the type of values it obtains.
 * Such concrete subclasses must implement the {@link #stringToObject}
 * method to perform mapping from an environment-supplied string value
 * to a typed parameter value, including validation.
 *
 * @author   Mark Taylor (Starlink)
 * @see Task
 * @see Environment
 */
public abstract class Parameter<T> {

    private final Class<T> clazz_;
    private boolean allowClassnameValue_;
    private String name_;
    private String prompt_;
    private String description_;
    private String usage_ = "<value>";
    private String stringDflt_;
    private int pos_;
    private boolean nullPermitted_;
    private boolean preferExplicit_;
    private String stringValue_;
    private T objectValue_;
    private boolean gotValue_;

    /** Compares parameters alphabetically by parameter name. */
    public static final Comparator<Parameter<?>> BY_NAME =
            new Comparator<Parameter<?>>() {
        public int compare( Parameter<?> p1, Parameter<?> p2 ) {
            return p1.getName().compareTo( p2.getName() );
        }
    };

    /**
     * Constructs a parameter with a given name.  This name should be unique
     * within a given Task.
     *
     * <p>If the <code>allowClassnameValue</code> flag is set,
     * then in addition to the options provided by
     * {@link #stringToObject stringToObject}, any supplied string value
     * equal to the classname of an available class matching
     * this parameter's value type which has a no-arg constructor
     * will cause a newly constructed instance of that class to be
     * used as a value.
     * 
     * @param  name  the identifying name of this parameter
     * @param  clazz  the class of this parameter's typed values
     * @param  allowClassnameValue  whether suitable classnames may be
     *         supplied as string values
     */
    public Parameter( String name, Class<T> clazz,
                      boolean allowClassnameValue ) {
        name_ = name;
        clazz_ = clazz;
        allowClassnameValue_ = allowClassnameValue;
    }

    /**
     * Takes a non-blank string, as supplied by the execution environment,
     * and turns it into a typed value for this parameter.
     * This method also performs validation, so if the string value
     * is unacceptable in any way, a ParameterValueException should
     * be thrown.
     *
     * <p>It is an error to supply a null or empty string value.
     *
     * <p>If this method fails (throws a ParameterValueException)
     * and if <code>allowClassnameValue</code> is set, then a subsequent
     * attempt will be made to interpret the <code>stringVal</code>
     * as the classname of a suitable class with a no-arg constructor.
     *
     * @param  env  execution environment; in most cases this is not required
     *              but for some purposes environment-specific characteristics
     *              may influence the result
     * @param  stringVal  non-null, non-empty string value
     * @return  typed value
     */
    public abstract T stringToObject( Environment env, String stringVal )
            throws TaskException;

    /**
     * Takes a typed value of this parameter and formats it as a string
     * which may be used for presentation to the user.
     * Ideally, round-tripping between this method and
     * {@link #stringToObject stringToObject} should be possible,
     * but that is not in general required/guaranteed.
     *
     * <p>The default implementation uses the value's toString method,
     * but subclasses can override this for smarter behaviour.
     *
     * @param  env  execution environment
     * @param  objectVal  typed parameter value
     * @return  string value for presentation
     */
    public String objectToString( Environment env, T objectVal )
            throws TaskException {
        return objectVal == null ? null : objectVal.toString();
    }

    /**
     * Sets the value of this parameter from a String.  This method is
     * called by the Environment to configure the value of this parameter.
     *
     * <p>A null or empty string is intercepted by this method and translated
     * to a null object value or triggers a ParameterValueException 
     * according to the value of isNullPermitted().
     *
     * @param  env  execution environment; in most cases this is not required
     *              but for some purposes environment-specific characteristics
     *              may influence the result
     * @param  stringval  string representation of value
     */
    public void setValueFromString( Environment env, String stringval ) 
            throws TaskException {
        final String sval;
        final T oval;

        /* Null or empty string: convert to a null value. */
        if ( stringval == null || stringval.trim().length() == 0 ) {
            if ( isNullPermitted() ) {
                sval = null;
                oval = null;
            }
            else {
                throw new ParameterValueException( this, "Null value "
                                                       + "not permitted" );
            }
        }

        /* Otherwise, use this parameter's custom string-to-object
         * conversion. */
        else {
            sval = stringval;
            T ov;
            try {
                ov = stringToObject( env, stringval );
            }

            /* If that fails, maybe try interpreting the string
             * as a classname. */
            catch ( TaskException e ) {
                if ( allowClassnameValue_ ) {
                    T instance = attemptGetClassInstance( stringval );
                    if ( instance != null ) {
                        ov = instance;
                    }
                    else {
                        throw e;
                    }
                }
                else {
                    throw e;
                }
            }
            oval = ov;
        }
        setValue( sval, oval );
    }

    /**
     * Sets the value of this parameter directly from a typed object.
     * In this case the reported string value is obtained by calling
     * {@link #objectToString objectToString}.
     *
     * @param  env  execution environment; in most cases this is not required
     *              but for some purposes environment-specific characteristics
     *              may influence the result
     * @param  objectValue  typed value
     */
    public void setValueFromObject( Environment env, T objectValue )
            throws TaskException {
        final String stringValue;
        if ( objectValue == null ) {
            if ( isNullPermitted() ) {
                stringValue = null;
            }
            else {
                throw new ParameterValueException( this, "Null value "
                                                       + "not permitted" );
            }
        }
        else {
            stringValue = objectToString( env, objectValue );
        }
        setValue( stringValue, objectValue );
    }

    /**
     * Gets the value of this parameter as a String.
     * The value is lazily acquired by the supplied environment object.
     *
     * <p>The returned value may be <code>null</code> 
     * only if the {@link #isNullPermitted} method returns true.
     *
     * @param   env  execution environment from which value is obtained
     * @return   the value of this parameter as a string, or <code>null</code>
     * @throws  AbortException  if during the course of trying to obtain
     *          a value the Environment determines that the task should
     *          not continue.
     */
    public final String stringValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return stringValue_;
    }

    /**
     * Gets the value of this parameter as a typed object.
     * The value is actually acquired by the supplied environment object.
     *
     * <p>The returned value will generally be null if the string value
     * that generated it is null or empty.  That is only possible
     * if the {@link #isNullPermitted} method returns true.
     *
     * @param   env  execution environment from which value is obtained
     * @return   the value of this parameter as a string, or <code>null</code>
     * @throws  AbortException  if during the course of trying to obtain
     *          a value the Environment determines that the task should
     *          not continue.
     */
    public final T objectValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return objectValue_;
    }

    /**
     * Clears the value of this parameter.  Subsequent retrievals of the
     * parameter value will trigger a request to the environment for a new
     * value.
     *
     * @param  env  execution environment within which value will be cleared
     */
    public void clearValue( Environment env ) {
        env.clearValue( this );
        stringValue_ = null;
        objectValue_ = null;
        gotValue_ = false;
    }

    /**
     * Sets the value of this parameter without any additional validation.
     * This is invoked by setValueFromString and setValueFromObject,
     * and should not normally be invoked directly.
     *
     * @param  stringValue  string representation of value
     * @param  objectValue  typed value
     */
    protected void setValue( String stringValue, T objectValue ) {
        stringValue_ = stringValue;
        objectValue_ = objectValue;
        gotValue_ = true;
    }

    /**
     * Returns the class of the typed values this parameter takes.
     *
     * @return  the class of this parameter's typed values
     */
    public Class<T> getValueClass() {
        return clazz_;
    }

    /**
     * Returns the name of this parameter.
     *
     * @return  name  the identifying name of this parameter
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets the name of this parameter.
     *
     * @param  name  identifying name of this parameter
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Gets the prompt string for this parameter, as displayed to the 
     * user when the value of the parameter is requested.
     * Should be short (&lt;40 characters?).
     *
     * @return  prompt string
     */
    public String getPrompt() {
        return prompt_;
    }

    /**
     * Sets the prompt string for this parameter, as displayed to the 
     * user when the value of the parameter is requested.
     * Should be short (&lt;40 characters?).
     *
     * @param   prompt  the prompt string
     */
    public void setPrompt( String prompt ) {
        prompt_ = prompt;
    }

    /**
     * Returns the textual description for this parameter.
     *
     * @return  description, if known
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Sets the textual description for this parameter.
     *
     * @param  description  description
     */
    public void setDescription( String description ) {
        description_ = description;
    }

    /**
     * Convenience method to set the description for this parameter by 
     * the result of joining an array of lines together.
     *
     * @param  descLines  lines of textual description
     * @see  #setDescription(java.lang.String)
     */
    public void setDescription( String[] descLines ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < descLines.length; i++ ) {
            sbuf.append( descLines[ i ] )
                .append( '\n' );
        }
        setDescription( sbuf.toString() );
    }

    /**
     * Sets a usage string for this parameter.  This should be terse
     * (in particular no newline characters) and conform to the 
     * following rules:
     * <ul>
     * <li>the parameter name is not included in the message
     * <li>placeholders are enclosed in angle brackets (&lt;&gt;)
     * <li>literals are not enclosed in angle brackets
     * <li>a disjunction is represented using the "|" character
     * </ul>
     * The <code>Parameter</code> class uses the string "&lt;value&gt;"
     * as the default usage string.
     *
     * @param   usage  usage string
     */
    public void setUsage( String usage ) {
        usage_ = usage;
    }

    /**
     * Returns the usage string for this parameter.
     *
     * @return  usage string
     * @see   #setUsage
     */
    public String getUsage() {
        return usage_;
    }

    /**
     * Set whether it is legal for this parameter's value to be blank.
     * By default it is not.
     * Note that null and blank string values are treated the same as each
     * other, and are translated to null object values.
     *
     * @param  permitted  whether null values are to be permitted for this
     *         parameter
     */
    public void setNullPermitted( boolean permitted ) {
        nullPermitted_ = permitted;
    }

    /**
     * Determine whether it is legal for this parameter's value to be blank.
     * By default it is not.
     *
     * @return   true if null values are permitted for this parameter
     */
    public boolean isNullPermitted() {
        return nullPermitted_;
    }

    /**
     * Determine whether an explict value is generally preferred to the
     * default value for this parameter.
     *
     * @return   true  if a default value should generally be avoided
     */
    public boolean getPreferExplicit() {
        return preferExplicit_;
    }

    /**
     * Set whether an explicit value is generally to be solicited from 
     * the user rather than taking the default.
     *
     * @param   prefer   true if you would like to encourage an explicit
     *                   value to be set for this parameter
     */
    public void setPreferExplicit( boolean prefer ) {
        preferExplicit_ = prefer;
    }

    /**
     * Gets the default string value for this parameter.
     *
     * @return  the default string value
     */
    public String getStringDefault() {
        return stringDflt_;
    }

    /**
     * Sets the default string value for this parameter.
     * Concrete subclasses may additionally supply type-specific
     * default value setter methods, but those ought to operate by invoking
     * this method.
     *
     * @param  stringDflt  the default string value
     */
    public void setStringDefault( String stringDflt ) {
        stringDflt_ = stringDflt;
    }

    /**
     * Gets the position of this parameter in a parameter list;
     * the first parameter is 1.  If the position is 0, the value can only 
     * be set by name. 
     *
     * @return  parameter position
     */
    public int getPosition() {
        return pos_;
    }

    /**
     * Sets the position of this parameter in a parameter list;
     * the first parameter is 1.  If the position is 0, the value can only 
     * be set by name. 
     *
     * @param  pos  parameter position
     */
    public void setPosition( int pos ) {
        pos_ = pos;
    }

    /**
     * Utility function to convert a list to an array, where the elements
     * are of the value class of this parameter.
     *
     * @param   collection  typed collection
     * @return  typed array with same contents
     */
    public T[] toArray( Collection<T> collection ) {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance( clazz_, collection.size() );
        return collection.toArray( array );
    }

    /**
     * Returns the name of this parameter.
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        return name_;
    }

    /**
     * Ensure that this object is configured with a valid value from the 
     * environment.  If not, the environment is queried so that we are.
     *
     * @param   env  execution environment which supplies value
     */
    private void checkGotValue( Environment env ) throws TaskException {
        if ( ! gotValue_ ) {
            env.acquireValue( this );
            assert gotValue_ : "Environment did not call setValue";
        }
    }

    /**
     * Attempts to interpret a string as a classname to turn it into
     * a typed object.  The class must exist, must extend this parameter's
     * value type, and must have a public no-arg constructor for this to work.
     * In most cases, failure just results in a null return, but if it
     * looks like a genuine attempt has been made to supply a classname,
     * a ParameterValueException with a helpful message is thrown.
     *
     * @param   cname  string which might just be the name of a suitable
     *                 class with a no-arg constructor
     * @return  typed value or null
     */
    private T attemptGetClassInstance( String cname )
            throws ParameterValueException {

        /* See if we have a classname.  Most likely the string value is not
         * intended to represent a class instance at all, so if it's not
         * a class just return null with no error. */
        final Class<?> vclazz;
        try {
            vclazz = Class.forName( cname );
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }

        /* We have a classname, so it's a fair bet that it is intended to
         * name a class whose instance should be the value of this parameter.
         * Anything that goes wrong from here generates an exception. */
        final Class<T> clazz = getValueClass();
        if ( clazz.isAssignableFrom( vclazz ) ) {
            Class<? extends T> vtclazz = vclazz.asSubclass( clazz );
            Constructor<? extends T> constructor;
            try {
                constructor = vtclazz.getConstructor();
            }
            catch ( NoSuchMethodException e ) {
                throw new ParameterValueException( this,
                                                   "No no-arg constructor for "
                                                 + vtclazz );
            }
            try {
                return constructor.newInstance( new Object[ 0 ] );
            }
            catch ( Throwable e ) {
                throw new ParameterValueException( this,
                                                   "Error constructing "
                                                 + vtclazz, e );
            }
        }
        else {
            throw new ParameterValueException( this,
                                               vclazz + " is not of type "
                                             + clazz );
        }
    }
}
