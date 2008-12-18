package uk.ac.starlink.vo;

import java.util.Arrays;
import java.util.Comparator;
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
     * Constructor.
     */
    public ResourceTableModel() {
        columns_ = new RegColumn[] {
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
        };
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
