package uk.ac.starlink.ttools.plottask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter subclass for selecting named options.
 * This resembles {@link uk.ac.starlink.task.ChoiceParameter} in that several
 * named choices are available.
 * However, it is also possible to select options which are not in the
 * known option list.  For this to work, a pair of methods
 * {@link #toString(java.lang.Object)} and {@link #fromString(java.lang.String)}
 * must be implemented as inverses of each other so that a string can be
 * turned into an object.
 * The supplied options do not need to have names which follow this scheme.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2008
 */
public abstract class NamedObjectParameter extends Parameter {

    private final List optList_;
    private Object objectValue_;
    private boolean usageSet_;

    /**
     * Constructs a new parameter with no named options.
     *
     * @param  name  parameter name
     */
    public NamedObjectParameter( String name ) {
        super( name );
        optList_ = new ArrayList();
    }

    /**
     * Adds an option with an associated name.
     * This name does not need to be <code>toString(option)</code>.
     *
     * @param  name  option alias
     * @param  option  option value object
     */
    public void addOption( String name, Object option ) {
        optList_.add( new NamedOption( name, option ) );
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        boolean done = false;
        if ( sval == null && isNullPermitted() ) {
            objectValue_ = null;
            done = true;
        }
        if ( ! done ) {
            for ( Iterator it = optList_.iterator(); it.hasNext() && ! done; ) {
                NamedOption opt = (NamedOption) it.next();
                if ( opt.name_.equalsIgnoreCase( sval ) ) {
                    objectValue_ = opt.option_;
                    done = true;
                }
            }
        }
        if ( ! done ) {
            try {
                objectValue_ = fromString( sval );
                done = true;
            }
            catch ( RuntimeException e ) {
                throw new ParameterValueException( this, "Bad format " + sval,
                                                   e );
            }
        }
        assert done;
        super.setValueFromString( env, sval );
    }

    /**
     * Returns the value of this parameter as an object.
     * The returned value will be one of:
     * <ul>
     * <li>an option previously added using {@link #addOption}</li>
     * <li>a value which has been returned from {@link #fromString}</li>
     * <li>a value which was set using {@link #setDefaultOption}</li>
     * <li><code>null</code>, if <code>isNullPermitted()</code></li>
     * </ul>
     *
     * @param  env  execution environment
     * @return   option value
     */
    public Object objectValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return objectValue_;
    }

    /**
     * Sets the default value of this parameter as an option value object. 
     * <code>option</code> must be either one of the values added using
     * {@link #addOption} or {@link #toString(java.lang.Object)} must be
     * able to translate it.  Or it could be null.
     *
     * @param  option  new default value as an object
     */
    public void setDefaultOption( Object option ) {
        if ( option == null ) {
            super.setDefault( null );
            return;
        }
        for ( Iterator it = optList_.iterator(); it.hasNext(); ) {
            NamedOption opt = (NamedOption) it.next();
            if ( opt.option_.equals( option ) ) {
                super.setDefault( opt.name_ );
                return;
            }
        }
        super.setDefault( toString( option ) );
    }

    /**
     * Translates a possible option value of this parameter into a string
     * which represents it as a string value.
     *
     * @param  option   object value
     * @return  corresponding string
     */
    public String toString( Object option ) {
        return option.toString();
    }

    /**
     * Translates a string value for this parameter into the object value
     * which it represents.  Must return a suitable object value for this
     * parameter, or throw an unchecked exception.
     *
     * <p>The implementation must be such that
     * <code>fromString(toString(o)).equals(o)</code>.
     *
     * @param   name   option name
     * @return   corresponding option value
     */
    public abstract Object fromString( String name );

    /**
     * Returns a formatted XML string giving an unordered list of the options
     * for this parameter.  Suitable for insertion into a parameter description.
     * Not enclosed in a &lt;p&gt; element.
     *
     * @return  option list XML string
     */
    public String getOptionList() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<ul>\n" );
        for ( Iterator it = optList_.iterator(); it.hasNext(); ) {
            NamedOption opt = (NamedOption) it.next();
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<![CDATA[" )
                .append( opt.name_ )
                .append( "]]>" )
                .append( "</code>" )
                .append( "</li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    /**
     * Returns the names of all the named options known for this parameter.
     *
     * @return  name list
     */
    public String[] getNames() {
        String[] names = new String[ optList_.size() ];
        for ( int i = 0; i < names.length; i++ ) {
            names[ i ] = ((NamedOption) optList_.get( i )).name_;
        }
        return names;
    }

    /**
     * Returns the option objects for all the named options known for this
     * parameter.
     *
     * @return   object list
     */
    public Object[] getOptions() {
        Object[] options = new String[ optList_.size() ];
        for ( int i = 0; i < options.length; i++ ) {
            options[ i ] = ((NamedOption) optList_.get( i )).option_;
        }
        return options;
    }

    public void setUsage( String usage ) {
        usageSet_ = true;
        super.setUsage( usage );
    }

    public String getUsage() {
        if ( usageSet_ ) {
            return super.getUsage();
        }
        else {

            /* Truncates usage message to a few items and an ellipsis if it
             * would otherwise be too long.  Styles typically have a
             * lot of options. */
            int nopt = optList_.size();
            StringBuffer sbuf = new StringBuffer();
            if ( nopt > 4 ) {
                for ( int i = 0; i < 2; i++ ) {
                    sbuf.append( ((NamedOption) optList_.get( i )).name_ );
                    sbuf.append( '|' );
                }
                sbuf.append( "..." );
            }
            else {
                for ( int i = 0; i < nopt; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( '|' );
                    }
                    sbuf.append( ((NamedOption) optList_.get( i )).name_ );
                }
            }
            return sbuf.toString();
        }
    }

    /**
     * Utility class which aggregates a name and an object.
     */
    private static class NamedOption {
        final String name_;
        final Object option_;
        NamedOption( String name, Object option ) {
            name_ = name;
            option_ = option;
        }
    }
}
