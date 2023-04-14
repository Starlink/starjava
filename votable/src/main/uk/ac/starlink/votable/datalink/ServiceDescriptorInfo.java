package uk.ac.starlink.votable.datalink;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.VOStarTable;

/**
 * ValueInfo for service descriptor values.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2017
 */
public class ServiceDescriptorInfo extends DefaultValueInfo {

    final StarTable table_;

    /**
     * Constructor.
     *
     * @param  name   descriptor name, should not be null
     * @param  description   descriptor description, may be null
     *                       if no relevant information is available
     * @param  table   table to which the values of this info refer;
     *                 may be null if inapplicable or unknown
     */
    public ServiceDescriptorInfo( String name, String description,
                                  StarTable table ) {
        super( name, ServiceDescriptor.class,
               description == null ? "Datalink-style service descriptor"
                                   : description );
        table_ = table;
    }

    @Override
    public String formatValue( Object value, int maxLength ) {
        if ( value == null ) {
            return null;
        }
        else {
            String txt = formatDescriptor( (ServiceDescriptor) value, table_ );
            return txt.length() <= maxLength
                 ? txt
                 : txt.substring( 0, maxLength - 3 ) + "...";
        }
    }

    /**
     * Always returns null.
     */
    @Override
    public Object unformatString( String txt ) {
        return null;
    }

    /**
     * Returns a string detailing the state of a given ServiceDesciptor
     * in a human-readable form.
     *
     * @param   sd  service descriptor
     * @param  table   table to which the descriptor refers;
     *                 may be null if inapplicable or unknown
     * @return  multi-line description string
     */
    public static String formatDescriptor( ServiceDescriptor sd,
                                           StarTable table ) {
        StringBuffer sbuf = new StringBuffer()
            .append( formatItem( "ID", sd.getDescriptorId() ) )
            .append( formatItem( "accessURL", sd.getAccessUrl() ) )
            .append( formatItem( "standardId", sd.getStandardId() ) )
            .append( formatItem( "resourceIdentifier",
                                 sd.getResourceIdentifier() ) )
            .append( formatItem( "contentType", sd.getContentType() ) );
        for ( ExampleUrl example : sd.getExampleUrls() ) {
            sbuf.append( formatItem( "exampleURL", example.getUrl() ) );
        }
        sbuf.append( "Input parameters:" );
        for ( ServiceParam param : sd.getInputParams() ) {
            sbuf.append( "\n" )
                .append( "   " )
                .append( param.getName() )
                .append( " (" );
            String ref = param.getRef();
            String value = param.getValue();
            if ( ref != null ) {
                sbuf.append( "column=" );
                int icol = VOStarTable.getRefColumnIndex( ref, table );
                if ( icol >= 0 ) {
                    sbuf.append( table.getColumnInfo( icol ).getName() );
                }
                else {
                    sbuf.append( "ID:" )
                        .append( ref );
                }
            }
            else if ( value != null && value.length() > 0 ) {
                sbuf.append( "default=\"" )
                    .append( value )
                    .append( "\"" );
            }
            else {
                sbuf.append( "type=" )
                    .append( param.getDatatype() );
                int[] shape = param.getArraysize();
                if ( shape != null && shape.length > 0 ) {
                    sbuf.append( "[" )
                        .append( DefaultValueInfo.formatShape( shape ) )
                        .append( "]" );
                }
            }
            sbuf.append( ")" );
        }
        return sbuf.toString();
    }

    /**
     * Formats a heading and a value as a 2-line string.
     *
     * @param  name  heading
     * @param  value  value text
     * @return   multi-line string giving name-value pair
     */
    private static String formatItem( String name, String value ) {
        return ( value == null || value.trim().length() == 0 )
             ? ""
             : name + ":\n   " + value + "\n";
    }
}
