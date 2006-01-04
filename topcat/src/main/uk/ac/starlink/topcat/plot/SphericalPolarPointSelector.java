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
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
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

    /**
     * Constructor.
     *
     * @param  styles  plotting style set
     */
    public SphericalPolarPointSelector( StyleSet styles ) {
        super( styles );

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
        StarTableColumn rcol = getR();
        return new SphericalPolarTable( getTable(), getPhi(), getTheta(),
                                        rcol == null ? -1
                                                     : rcol.getModelIndex() );
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
    private StarTableColumn getR() {
        return (StarTableColumn) rSelector_.getSelectedItem();
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
        private final int rColIndex_;

        /**
         * Constructor.
         *
         * @param   tcModel   table
         * @param   phiData   column of longitude-like values
         * @param   thetaData column of latitude-like values
         * @param   rColIndex  column index in table's data model of the
         *          column representing radius values.  May be -1 to indicate
         *          no radial data
         */
        public SphericalPolarTable( TopcatModel tcModel, ColumnData phiData,
                                    ColumnData thetaData, int rColIndex ) {
            tcModel_ = tcModel;
            phiData_ = phiData;
            thetaData_ = thetaData;
            rColIndex_ = rColIndex;
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
            if ( rColIndex_ >= 0 ) {
                info.setUnitString( tcModel_.getDataModel()
                                            .getColumnInfo( rColIndex_ )
                                            .getUnitString() );
            }
            return new ColumnInfo( info );
        }

        public RowSequence getRowSequence() {
            final StarTable baseTable = tcModel_.getDataModel();
            final long nrow = getRowCount();
            final Double ONE = new Double( 1.0 );
            return new RowSequence() {
                long lrow_ = 0;
                Object[] row_;
                public boolean next() throws IOException {
                    if ( lrow_ < nrow ) {
                        row_ = new Object[ 3 ];
                        Object oPhi = phiData_.readValue( lrow_ );
                        Object oTheta = thetaData_.readValue( lrow_ );
                        Object oR = rColIndex_ >= 0
                                  ? baseTable.getCell( lrow_, rColIndex_ )
                                  : ONE;
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
                    && rColIndex_ == other.rColIndex_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = rColIndex_;
            code = 23 * code + tcModel_.hashCode();
            code = 23 * code + phiData_.hashCode();
            code = 23 * code + thetaData_.hashCode();
            return code;
        }
    }
}
