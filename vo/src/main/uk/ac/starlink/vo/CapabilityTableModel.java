package uk.ac.starlink.vo;

import javax.swing.table.AbstractTableModel;

/**
 * TableModel in which each row is a {@link RegCapabilityInterface}.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2008
 */
public class CapabilityTableModel extends AbstractTableModel {

    private RegCapabilityInterface[] caps_;
    private CapColumn[] columns_;

    /**
     * Constructor.
     */
    public CapabilityTableModel() {
        columns_ = new CapColumn[] {
            new CapColumn( "AccessURL" ) {
                public String getValue( RegCapabilityInterface cap ) {
                    return cap.getAccessUrl();
                }
            },
            new CapColumn( "Description" ) {
                public String getValue( RegCapabilityInterface cap ) {
                    return cap.getDescription();
                }
            },
            new CapColumn( "Version" ) {
                public String getValue( RegCapabilityInterface cap ) {
                    return cap.getVersion();  
                }
            },
        };
    }

    public int getColumnCount() {
        return columns_.length;
    }

    public int getRowCount() {
        return caps_ == null ? 0 : caps_.length;
    }

    public Object getValueAt( int irow, int icol ) {
        return columns_[ icol ].getValue( caps_[ irow ] );
    }

    public String getColumnName( int icol ) {
        return columns_[ icol ].name_;
    }

    /**
     * Sets the data for this table.
     *
     * @param  caps  capability array
     */
    public void setCapabilities( RegCapabilityInterface[] caps ) {
        caps_ = caps;
        fireTableDataChanged();
    }

    /**
     * Returns the data array for this table.
     *
     * @return   capability array
     */
    public RegCapabilityInterface[] getCapabilities() {
        return caps_;
    }

    /**
     * Represents a table column.
     */
    private static abstract class CapColumn {
        final String name_;

        /**
         * Constructor.
         *
         * @param   name   column name
         */
        CapColumn( String name ) {
            name_ = name;
        }

        /**
         * Returns the value for this column for a given capability.
         *
         * @param   cap  capaility object
         */
        abstract String getValue( RegCapabilityInterface cap );
    }
}
