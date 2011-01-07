package uk.ac.starlink.ttools.plottask;

import uk.ac.starlink.task.ChoiceParameter;

/**
 * ChoiceParameter subclass suitable for use with plotting style configuration.
 * Adds a few useful customisations.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public class StyleParameter extends ChoiceParameter {

    private boolean usageSet_;

    /**
     * Constructs a StyleParameter with a given list of options.
     *
     * @param   name  parameter name
     * @param   options  list of options
     */
    public StyleParameter( String name, Object[] options ) {
        super( name, options );
    }

    /**
     * Constructs a StyleParameter without initialising any options.
     *
     * @param  name  parameter name
     */
    public StyleParameter( String name ) {
        super( name );
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
            String[] names = getOptionNames();
            int nopt = names.length;
            StringBuffer sbuf = new StringBuffer();
            if ( nopt > 4 ) {
                for ( int i = 0; i < 2; i++ ) {
                    sbuf.append( names[ i ] );
                    sbuf.append( '|' );
                }
                sbuf.append( "..." );
            }
            else {
                for ( int i = 0; i < nopt; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( '|' );
                    }
                    sbuf.append( names[ i ] );
                }
            }
            return sbuf.toString();
        }
    }

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
        String[] names = getOptionNames();
        for ( int i = 0; i < names.length; i++ ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<![CDATA[" )
                .append( names[ i ] )
                .append( "]]>" )
                .append( "</code>" )
                .append( "</li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    public String getName( Object option ) {
        return super.getName( option ).toLowerCase().replaceAll( " ", "_" );
    }
}
