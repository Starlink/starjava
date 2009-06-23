package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * TableModel in which each row represents a {@link RegResource}.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2008
 */
public class ResourceTableModel extends AbstractTableModel {

    private final RegColumn[] columns_;
    private RegResource[] resources_;

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
    public ResourceTableModel( boolean includeAcref ) {
        List colList = new ArrayList( Arrays.asList( new RegColumn[] {
            new RegColumn( "shortName" ) {
                public String getValue( RegResource res ) {
                    return res.getShortName();
                } 
            },
            new RegColumn( "title" ) {
                public String getValue( RegResource res ) {
                    return res.getTitle();
                }
            },
            new RegColumn( "identifier" ) {
                public String getValue( RegResource res ) {
                    return res.getIdentifier();
                }
            },
            new RegColumn( "publisher" ) {
                public String getValue( RegResource res ) {
                    return res.getPublisher();
                }
            },
            new RegColumn( "contact" ) {
                public String getValue( RegResource res ) {
                    return res.getContact();
                }
            },
            new RegColumn( "refURL" ) {
                public String getValue( RegResource res ) {
                    return res.getReferenceUrl();
                }
            },
        } ) );
        if ( includeAcref ) {
            colList.add( new RegColumn( "soleAccessURL" ) {
                public String getValue( RegResource res ) {
                    RegCapabilityInterface[] caps = res.getCapabilities();
                    return ( caps != null && caps.length == 1 )
                         ? caps[ 0 ].getAccessUrl()
                         : null;
                }
            } );
        }
        columns_ = (RegColumn[]) colList.toArray( new RegColumn[ 0 ] );
    }

    public int getColumnCount() {
        return columns_.length;
    }

    public int getRowCount() {
        return resources_ == null ? 0 : resources_.length;
    }

    public Object getValueAt( int irow, int icol ) {
        return columns_[ icol ].getValue( resources_[ irow ] );
    }

    public String getColumnName( int icol ) {
        return columns_[ icol ].name_;
    }

    /**
     * Sets the data for this table.
     *
     * @param  resources   resource array
     */
    public void setResources( RegResource[] resources ) {
        resources_ = (RegResource[]) resources.clone();
        Arrays.sort( resources_, columns_[ 0 ] );
        fireTableDataChanged();
    }

    /**
     * Returns the data array for this table.
     *
     * @return  resource array
     */
    public RegResource[] getResources() {
        return resources_;
    }

    /**
     * Represents a table column.  Implements a Comparator which sorts
     * on the contents of the column.
     */
    private abstract class RegColumn implements Comparator {
        final String name_;

        /**
         * Constructor.
         *
         * @param  name   column name
         */
        RegColumn( String name ) {
            name_ = name;
        }

        /**
         * Returns the value for this column for a given resource.
         *
         * @param  resource   resource object
         */
        abstract String getValue( RegResource resource );

        public int compare( Object o1, Object o2 ) {
            String s1 = getValue( (RegResource) o1 );
            String s2 = getValue( (RegResource) o2 );
            if ( s1 == null && s2 == null ) {
                return 0;
            }
            else if ( s1 == null ) {
                return -1;
            }
            else if ( s2 == null ) {
                return +1;
            }
            else {
                return s1.compareTo( s2 );
            }
        }
    }
}
