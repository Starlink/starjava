package uk.ac.starlink.task;

/**
 * A Parameter describes the function of one of a task's parameters.
 * An instance of the class encapsulates a parameter's name,
 * prompt string, source, default values, position on the command line,
 * and so on.  It can also validate, hold and clear the value of the
 * parameter.  The parameter value is obtained in string form by an
 * associated Environment object, and the Parameter object configures
 * its value from this string.
 * <p>
 * This class can be subclassed to provide more specialised kinds of 
 * parameter.  Such subclasses should subclass the 
 * {@link #setValueFromString} method to perform specific setup 
 * and validation, and may supply methods additional to {@link #stringValue}
 * which provide the parameter value in other forms; for instance
 * an <tt>IntegerParameter</tt> class might provide a method returning
 * the result as an <tt>int</tt>.  <b>Note</b> that methods which
 * return the value must call {@link #checkGotValue} to ensure that
 * initialisation from a string has taken place.
 * <p>
 * Here is an implementation of an example subclass:
 * <pre>
 *     public class IntegerParameter extends Parameter {
 *
 *         private int intval;
 *
 *         public IntegerParameter( String name ) {
 *             super( name );
 *         }
 *
 *         public void setValueFromString( String stringval )
 *                 throws ParameterValueException {
 *             try {
 *                 intval = Integer.parseInt( stringval );
 *             }
 *             catch ( NumberFormatException e ) {
 *                 throw new ParameterValueException( this, e );
 *             }
 *             super.setValueFromString( stringval );
 *         }
 *
 *         public int intValue() 
 *                 throws ParameterValueException, AbortException {
 *             checkGotValue();
 *             return intval;
 *         }
 *     }
 * </pre>
 *
 * @author   Mark Taylor (Starlink)
 * @see Task
 * @see Environment
 */
public class Parameter {

    private final String name;
    private String prompt;
    private String def;
    private int pos;
    private Environment env;
    private String stringValue;
    private boolean gotValue;

    /**
     * Constructs a parameter with a given name.  This name should be unique
     * within a given Task.
     * 
     * @param  name  the identifying name of this parameter
     */
    public Parameter( String name ) {
        this.name = name;
    }

    /**
     * Returns the name of this parameter.
     *
     * @return  name  the identifying name of this parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the prompt string for this parameter, as displayed to the 
     * user when the value of the parameter is requested.
     * Should be short (&lt;40 characters?).
     *
     * @return  prompt string
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the prompt string for this parameter, as displayed to the 
     * user when the value of the parameter is requested.
     * Should be short (&lt;40 characters?).
     *
     * @param   prompt  the prompt string
     */
    public void setPrompt( String prompt ) {
        this.prompt = prompt;
    }

    /**
     * Gets the default string value for this parameter.
     *
     * @return  the default string value
     */
    public String getDefault() {
        return def;
    }

    /**
     * Sets the default string value for this parameter.
     *
     * @param   the default string value
     */
    public void setDefault( String def ) {
        this.def = def;
    }

    /**
     * Gets the position of this parameter in a parameter list;
     * the first parameter is 1.  If the position is 0, the value can only 
     * be set by name. 
     *
     * @return  parameter position
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Sets the position of this parameter in a parameter list;
     * the first parameter is 1.  If the position is 0, the value can only 
     * be set by name. 
     *
     * @param  parameter position
     */
    public void setPosition( int pos ) {
        this.pos = pos;
    }

    /**
     * Sets the environment which is used to obtain values for this parameter.
     * This must be set before the value can be obtained.
     * 
     * @param  env  the execution environment
     */
    public void setEnvironment( Environment env ) {
        this.env = env;
    }

    /**
     * Sets the value of this parameter from a String.  This method is
     * called by the Environment to configure the value of this parameter.
     * It should be overridden by subclasses to set up their internal
     * state given a string representing their value. 
     * Validation is performed by throwing a ParameterValueException if
     * <tt>stringval</tt> does not represent a legal value for this parameter.
     * <p>
     * Note that subclasses implementations must invoke their parent 
     * implementation using <tt>super.setValueFromString</tt> once
     * it is certain that the value is legal.
     *
     * @throws ParameterValueException   if <tt>stringval</tt> does not
     *     represent a legal value for this parameter
     */
    public void setValueFromString( String stringval ) 
            throws ParameterValueException {
         this.stringValue = stringval;
         setGotValue( true );
    }

    /**
     * Gets the value of this parameter as a String.
     * The value is actually obtained by the <tt>Environment</tt> 
     * associated with this parameter.
     * The returned value may be <tt>null</tt> 
     * if the parameter has a null value.
     *
     * @param  the value of this parameter as a string, or <tt>null</tt>
     * @throws  AbortException  if during the course of trying to obtain
     *          a value the Environment determines that the task should
     *          not continue.
     */
    public String stringValue() throws AbortException {
        try {
            checkGotValue();
        }
        catch ( ParameterValueException e ) {
            throw new AssertionError( "How did that happen?" );
        }
        return stringValue;
    }

    /**
     * Clears the value of this parameter.  Subsequent retrievals of the
     * parameter value will trigger a request to the environment for a new
     * value.
     */
    public void clearValue() {
        env.clearParameterValue( this );
        setGotValue( false );
    }

    /**
     * Sets the gotValue flag.  This determines whether the environment needs
     * to be queried before we can satisfy a request for the value of this
     * parameter.
     *
     * @param  gotValue  true iff this object is configured with a 
     *         valid parameter value from the environment
     */
    private void setGotValue( boolean gotValue ) {
        this.gotValue = gotValue;
    }

    /**
     * Ensure that this object is configured with a valid value from the 
     * environment.  If not, the environment is queried so that we are.
     */
    protected void checkGotValue()
            throws AbortException, ParameterValueException {
        if ( ! gotValue ) {
            env.setParameterValue( this );
            setGotValue( true );
        }
    }

    /**
     * Returns the name of this parameter.
     *
     * @return  string representation
     */
    public String toString() {
        return name;
    }

}
