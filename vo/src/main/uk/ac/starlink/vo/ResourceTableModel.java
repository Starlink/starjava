package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;

/**
 * TableModel in which each row represents a {@link RegResource}.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2008
 */
public class ResourceTableModel extends ArrayTableModel<RegResource> {

    /**
     * Constructs a ResourceTableModel with no AccessRef column.
     */
    public ResourceTableModel() {
        this( false );
    }

    /**
     * Constructs a ResourceTableModel with an optional AccessRef column.
     * This is a bit problematic - there is not a formal 1:1 relationship
     * between RegResources, which is what are displayed per-row in
     * this table, and RegCapabilityInterfaces, which are what host
     * AccessRefs (a.k.a. Service URLs).  In many cases however, the
     * relationship is in fact 1:1.
     * If <code>includeAcref</code> is set true, a column is added for
     * this information, and it's populated only in the cases where
     * a 1:1 relationship does actually hold.
     *
     * @param   includeAcref  true if the access ref column is to be included
     */
    @SuppressWarnings("this-escape")
    public ResourceTableModel( boolean includeAcref ) {
        super( new RegResource[ 0 ] );
        List<StringColumn> colList = new ArrayList<StringColumn>();
        colList.add( new StringColumn( "Short Name" ) {
            public String getValue( RegResource res ) {
                return res.getShortName();
            }
        } );
        colList.add( new StringColumn( "Title" ) {
            public String getValue( RegResource res ) {
                return res.getTitle();
            }
        } );
        colList.add( new StringColumn( "Subjects" ) {
            public String getValue( RegResource res ) {
                return arrayToString( res.getSubjects() );
            }
        } );
        colList.add( new StringColumn( "Identifier" ) {
            public String getValue( RegResource res ) {
                return res.getIdentifier();
            }
        } );
        colList.add( new StringColumn( "Publisher" ) {
            public String getValue( RegResource res ) {
                return res.getPublisher();
            }
        } );
        colList.add( new StringColumn( "Contact" ) {
            public String getValue( RegResource res ) {
                return res.getContact();
            }
        } );
        colList.add( new StringColumn( "Reference URL" ) {
            public String getValue( RegResource res ) {
                return res.getReferenceUrl();
            }
        } );
        if ( includeAcref ) {
            colList.add( new StringColumn( "soleAccessURL" ) {
                public String getValue( RegResource res ) {
                    RegCapabilityInterface[] caps = res.getCapabilities();
                    return ( caps != null && caps.length == 1 )
                         ? caps[ 0 ].getAccessUrl()
                         : null;
                }
            } );
        }
        setColumns( colList );
    }

    /**
     * Sets the data for this table.
     *
     * @param  resources   resource array
     */
    public void setResources( RegResource[] resources ) {
        super.setItems( resources );
    }

    /**
     * Returns the data array for this table.
     *
     * @return  resource array
     */
    public RegResource[] getResources() {
        return super.getItems();
    }

    /**
     * Turns a string array into a readable string scalar.
     *
     * @param   values  array
     * @return   string
     */
    private static String arrayToString( String[] values ) {
        if ( values == null || values.length == 0 ) {
            return null;
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int iv = 0; iv < values.length; iv++ ) {
            String val = values[ iv ];
            if ( val != null && val.trim().length() > 0 ) {
                if ( sbuf.length() > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( val.trim() );
            }
        }
        return sbuf.toString();
    }

    /**
     * Utility class that provides string-yielding columns for use with
     * this model.
     */
    private static abstract class StringColumn
            extends ArrayTableColumn<RegResource,String> {

        /**
         * Constructor.
         *
         * @param  name  column name
         */
        StringColumn( String name ) {
            super( name, String.class );
        }
    }
}
