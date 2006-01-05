package uk.ac.starlink.topcat.plot;

import java.awt.Dimension;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.ColumnComboBoxModel;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * PointSelector implementation which queries for spherical polar
 * coordinates and yields 3D Cartesian ones.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class SphericalPolarPointSelector extends PointSelector {

    private final JComponent colBox_;
    private final ColumnSelector phiSelector_;
    private final ColumnSelector thetaSelector_;
    private final JComboBox rSelector_;
    private final ToggleButtonModel logToggler_;

    /**
     * Constructor.
     *
     * @param  styles  plotting style set
     * @param  logToggler model for determining whether the radial coordinate
     *         is to be scaled logarithmically
     */
    public SphericalPolarPointSelector( StyleSet styles, 
                                        ToggleButtonModel logToggler ) {
        super( styles );
        logToggler_ = logToggler;

        /* Prepare column selection panel. */
        colBox_ = Box.createVerticalBox();
        String[] axisNames = new String[] { "Longitude", "Latitude", "Radius" };
        JComponent[] selectors = new JComponent[ 3 ];

        /* Selector for longitude column. */
        phiSelector_ = new ColumnSelector( Tables.RA_INFO, false );
        phiSelector_.addActionListener( actionForwarder_ );
        phiSelector_.setTable( null );
        phiSelector_.setEnabled( false );
        selectors[ 0 ] = phiSelector_;

        /* Selector for latitude column. */
        thetaSelector_ = new ColumnSelector( Tables.DEC_INFO, false );
        thetaSelector_.addActionListener( actionForwarder_ );
        thetaSelector_.setTable( null );
        thetaSelector_.setEnabled( false );
        selectors[ 1 ] = thetaSelector_;

        /* Selector for radius column. */
        rSelector_ = new JComboBox();
        rSelector_.setRenderer( new ColumnCellRenderer( rSelector_ ) );
        rSelector_.addActionListener( actionForwarder_ );
        rSelector_.setEnabled( false );
        Box rBox = Box.createHorizontalBox();
        rBox.add( new ShrinkWrapper( rSelector_ ) );
        rBox.add( Box.createHorizontalStrut( 5 ) );
        rBox.add( new ComboBoxBumper( rSelector_ ) );
        rBox.add( Box.createHorizontalStrut( 5 ) );
        rBox.add( logToggler_.createCheckBox() );
        selectors[ 2 ] = rBox;

        /* Place selectors. */
        JLabel[] axLabels = new JLabel[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            String aName = axisNames[ i ];
            JComponent cPanel = Box.createHorizontalBox();
            axLabels[ i ] = new JLabel( " " + aName + " Axis: " );
            cPanel.add( axLabels[ i ] );
            colBox_.add( Box.createVerticalStrut( 5 ) );
            colBox_.add( cPanel );
            cPanel.add( selectors[ i ] );
            cPanel.add( Box.createHorizontalStrut( 5 ) );
            cPanel.add( Box.createHorizontalGlue() );
        }

        /* Align axis labels. */
        Dimension labelSize = new Dimension( 0, 0 );
        for ( int i = 0; i < 3; i++ ) {
            Dimension s = axLabels[ i ].getPreferredSize();
            labelSize.width = Math.max( labelSize.width, s.width );
            labelSize.height = Math.max( labelSize.height, s.height );
        }
        for ( int i = 0; i < 3; i++ ) {
            axLabels[ i ].setPreferredSize( labelSize );
        }
    }

    protected JComponent getColumnSelectorPanel() {
        return colBox_;
    }

    public int getNdim() {
        return 3;
    }

    public boolean isValid() {
        return getTable() != null
            && getPhi() != null
            && getTheta() != null;
    }

    public StarTable getData() {
        return new SphericalPolarTable( getTable(),
                                        getPhi(), getTheta(), getR() );
    }

    protected void configureSelectors( TopcatModel tcModel ) {
        if ( tcModel == null ) {
            phiSelector_.getModel().getColumnModel().setSelectedItem( null );
            phiSelector_.getModel().getConverterModel().setSelectedItem( null );
            thetaSelector_.getModel().getColumnModel().setSelectedItem( null );
            thetaSelector_.getModel().getConverterModel()
                                     .setSelectedItem( null );
            rSelector_.setSelectedItem( null );
        }
        else {
            phiSelector_.setTable( tcModel );
            thetaSelector_.setTable( tcModel );
            rSelector_.setModel( RestrictedColumnComboBoxModel
                                .makeClassColumnComboBoxModel( tcModel
                                                              .getColumnModel(),
                                                               true,
                                                               Number.class ) );
        }
        phiSelector_.setEnabled( tcModel != null );
        thetaSelector_.setEnabled( tcModel != null );
        rSelector_.setEnabled( tcModel != null );
    }

    protected void initialiseSelectors() {
    }

    /**
     * Return the column of longitude-type values currently selected.
     *
     * @return  phi column data
     */
    private ColumnData getPhi() {
        return phiSelector_.getColumnData();
    }

    /**
     * Return the column of latitude-type values currently selected.
     *
     * @return   theta column data
     */
    private ColumnData getTheta() {
        return thetaSelector_.getColumnData();
    }

    /**
     * Return the column of radius values currently selected.
     * May legitimately be null if you want everything on the surface of
     * a sphere.
     *
     * @return   radius column
     */
    private ColumnData getR() {
        StarTableColumn tcol = (StarTableColumn) rSelector_.getSelectedItem();
        int icol = tcol == null ? -1 : tcol.getModelIndex();
        return icol >= 0
             ? (ColumnData) new SelectedColumnData( getTable(),
                                                    tcol.getModelIndex(),
                                                    logToggler_.isSelected() )
             : (ColumnData) UnitColumnData.INSTANCE;
    }

    /**
     * Returns metadata describing the currently selected radial coordinate.
     * If no radial coordinate is selected (all points on the surface of
     * the sphere), <code>null</code> is returned.
     *
     * @return   radial column info
     */
    public ValueInfo getRadialInfo() {
        StarTableColumn tcol = (StarTableColumn) rSelector_.getSelectedItem();
        return ( tcol == null || tcol == ColumnComboBoxModel.NO_COLUMN )
             ? null
             : tcol.getColumnInfo();
    }

    /**
     * ColumnData implementation which returns unity for every entry.
     */
    private static class UnitColumnData extends ColumnData {
        final static UnitColumnData INSTANCE = new UnitColumnData();
        private final Double ONE = new Double( 1.0 );
        private UnitColumnData() {
            super( new DefaultValueInfo( "Unit", Double.class, "Unit value" ) );
        }
        public Object readValue( long irow ) {
            return ONE;
        }
    }

    /**
     * ColumnData implementation which picks data from a given column of
     * a base table.  An intelligent implementation of equals() is provided.
     */
    private static class SelectedColumnData extends ColumnData {

        private final TopcatModel tcModel_;
        private final int icol_;
        private final boolean logFlag_;
        private final StarTable baseTable_;

        /**
         * Constructs a new SelectedColumnData.
         *
         * @param  tcModel  topcat model containing the table data
         * @param  icol    column index (in tcModel's data model StarTable)
         *                 of the column data to copy
         * @param  logFlag  true iff you want the logarithms of the
         *                  selected column
         */
        SelectedColumnData( TopcatModel tcModel, int icol, boolean logFlag ) {
            tcModel_ = tcModel;
            icol_ = icol;
            logFlag_ = logFlag;
            baseTable_ = tcModel.getDataModel();
            ColumnInfo cinfo = 
                new ColumnInfo( baseTable_.getColumnInfo( icol_ ) );
            if ( logFlag_ ) {
                String units = cinfo.getUnitString();
                if ( units != null && units.trim().length() > 0 ) {
                    cinfo.setUnitString( "log(" + units + ")" );
                }
                cinfo.setName( "log(" + cinfo.getName() + ")" );
                cinfo.setContentClass( Double.class );
            }
            setColumnInfo( cinfo );
        }

        public Object readValue( long irow ) throws IOException {
            Object val = baseTable_.getCell( irow, icol_ );
            if ( logFlag_ ) {
                if ( val instanceof Number ) {
                    double dval = ((Number) val).doubleValue();
                    return dval > 0 ? new Double( Math.log( dval ) )
                                    : null;
                }
                else {
                    return null;
                }
            }
            else {
                return val;
            }
        }

        public boolean equals( Object o ) {
            if ( o instanceof SelectedColumnData ) {
                SelectedColumnData other = (SelectedColumnData) o;
                return this.tcModel_ == other.tcModel_
                    && this.icol_ == other.icol_
                    && this.logFlag_ == other.logFlag_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 555;
            code = 23 * code + tcModel_.hashCode();
            code = 23 * code + icol_;
            code = 23 * code + ( logFlag_ ? 1 : 0 );
            return code;
        }
    
    }

    /**
     * StarTable implementation which returns a table with X, Y, Z columns
     * based on the TopcatModel columns selected in this component.
     * This involves a coordinate transformation (spherical polar to
     * Cartesian).
     *
     * <p>Provides a non-trivial implementation of equals().
     *
     * <p>The table is not random-access - it could be made so without 
     * too much effort, but random access is not expected to be required.
     */
    private static class SphericalPolarTable extends AbstractStarTable {

        private final TopcatModel tcModel_;
        private final ColumnData phiData_;
        private final ColumnData thetaData_;
        private final ColumnData rData_;

        /**
         * Constructor.
         *
         * @param   tcModel   table
         * @param   phiData   column of longitude-like values
         * @param   thetaData column of latitude-like values
         * @param   rData     column of radius-like values
         */
        public SphericalPolarTable( TopcatModel tcModel, ColumnData phiData,
                                    ColumnData thetaData, ColumnData rData ) {
            tcModel_ = tcModel;
            phiData_ = phiData;
            thetaData_ = thetaData;
            rData_ = rData;
        }

        public int getColumnCount() {
            return 3;
        }

        public long getRowCount() {
            return tcModel_.getDataModel().getRowCount();
        }

        public ColumnInfo getColumnInfo( int icol ) {
            DefaultValueInfo info =
                new DefaultValueInfo( new String[] { "X", "Y", "Z" }[ icol ],
                                      Double.class,
                                      "Cartesian coordinate " + ( icol + 1 ) );
            info.setUnitString( rData_.getColumnInfo().getUnitString() );
            return new ColumnInfo( info );
        }

        public RowSequence getRowSequence() {
            final StarTable baseTable = tcModel_.getDataModel();
            final long nrow = getRowCount();
            return new RowSequence() {
                long lrow_ = 0;
                Object[] row_;
                public boolean next() throws IOException {
                    if ( lrow_ < nrow ) {
                        row_ = new Object[ 3 ];
                        Object oPhi = phiData_.readValue( lrow_ );
                        Object oTheta = thetaData_.readValue( lrow_ );
                        Object oR = rData_.readValue( lrow_ );
                        if ( oPhi instanceof Number &&
                             oTheta instanceof Number &&
                             oR instanceof Number ) {
                            double r = ((Number) oR).doubleValue(); 
                            if ( r > 0 ) {
                                double phi = ((Number) oPhi).doubleValue();
                                double theta = ((Number) oTheta).doubleValue();

                                double sinTheta = Math.sin( theta );
                                double cosTheta = Math.cos( theta );
                                double sinPhi = Math.sin( phi );
                                double cosPhi = Math.cos( phi );

                                double x = r * cosTheta * cosPhi;
                                double y = r * cosTheta * sinPhi;
                                double z = r * sinTheta;

                                row_[ 0 ] = new Double( x );
                                row_[ 1 ] = new Double( y );
                                row_[ 2 ] = new Double( z );
                            }
                        }
                        lrow_++;
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                public Object[] getRow() {
                    return row_;
                }
                public Object getCell( int icol ) {
                    return row_[ icol ];
                }
                public void close() {
                }
            };
        }

        public boolean equals( Object o ) {
            if ( o instanceof SphericalPolarTable ) {
                SphericalPolarTable other = (SphericalPolarTable) o;
                return other.tcModel_.equals( this.tcModel_ )
                    && other.phiData_.equals( this.phiData_ )
                    && other.thetaData_.equals( this.thetaData_ )
                    && other.rData_.equals( this.rData_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 999;
            code = 23 * code + tcModel_.hashCode();
            code = 23 * code + phiData_.hashCode();
            code = 23 * code + thetaData_.hashCode();
            code = 23 * code + rData_.hashCode();
            return code;
        }
    }
}
