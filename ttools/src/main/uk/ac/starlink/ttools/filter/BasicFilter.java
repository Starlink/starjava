package uk.ac.starlink.ttools.filter;

import uk.ac.starlink.ttools.DocUtils;

/**
 * Basic implementation of ProcessingFilter methods.
 * Utility superclass for implementing concrete ProcessingFilter.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public abstract class BasicFilter implements ProcessingFilter {

    private final String name_;
    private final String usage_;

    /**
     * Constructor.
     *
     * @param  name  filter name
     * @param  usage  filter usage
     */
    protected BasicFilter( String name, String usage ) {
        name_ = name;
        usage_ = usage;
    }

    public String getName() {
        return name_;
    }

    public String getUsage() {
        return usage_;
    }

    public String getDescription() {
        return DocUtils.join( getDescriptionLines() );
    }

    protected abstract String[] getDescriptionLines();

    /**
     * Returns a snippet of XML explaining use of the syntax of some given
     * usage-type example arguments.
     * These may be one of the following:
     * <ul>
     * <li>expr</li>
     * <li>col-id</li>
     * <li>colid-list</li>
     * </ul>
     * either alone or with some other text pre- or ap-pended.
     *
     * @param  usages  array of formal arguments to be explained
     * @return XML explanation
     */
    public static String explainSyntax( String[] usages ) {
        int nUsage = usages.length;
        if ( nUsage == 0 ) {
            return "";
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>" )
            .append( "Syntax for the " );
        String[] refids = new String[ nUsage ];
        for ( int i = 0; i < nUsage; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ( i == nUsage - 1 ) ? " and " : ", " );
            }
            String usage = usages[ i ];
            String refid;
            if ( usage.contains( "expr" ) ||
                 usage.contains( "key-list" ) ) {
                refid = "jel";
            }
            else if ( usage.contains( "col-id" ) ) {
                refid = "col-id";
            }
            else if ( usage.contains( "colid-list" ) ) {
                refid = "colid-list";
            }
            else {
                throw new IllegalArgumentException(
                    "Unknown usage type " + usage );
            }
            refids[ i ] = refid;
            if ( nUsage > 1 ) {
                sbuf.append( "<ref id=\"" )
                    .append( refid )
                    .append( "\">" );
            }
            sbuf.append( "<code>" )
                .append( "&lt;" )
                .append( usage )
                .append( "&gt;" )
                .append( "</code>" );
            if ( nUsage > 1 ) {
                sbuf.append( "</ref>" );
            }
            sbuf.append( " " );
        }
        if ( nUsage == 1 ) {
            sbuf.append( "argument" )
                .append( " is described in " )
                .append( "<ref id=\"" )
                .append( refids[ 0 ] )
                .append( "\"/>" );
        }
        else {
            sbuf.append( "arguments" )
                .append( " is described in " )
                .append( "the manual" );
        }
        sbuf.append( ".\n" )
            .append( "</p>\n" );
        return sbuf.toString();
    }
}
