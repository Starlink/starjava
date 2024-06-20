package uk.ac.starlink.topcat.plot;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * AxesSelector implementation which queries for spherical polar coordinates
 * and yields 3D Cartesian ones.
 *
 * @author   Mark Taylor
 * @since    31 May 2007
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class SphericalAxesSelector implements AxesSelector {

    private final JComponent colBox_;
    private final ColumnSelector phiSelector_;
    private final ColumnSelector thetaSelector_;
    private final AxisDataSelector rSelector_;
    private final JComponent rContainer_;
    private final ColumnSelector tanerrSelector_;
    private final ToggleButtonModel logToggler_;
    private final ToggleButtonModel tangentErrorToggler_;
    private final ErrorModeSelectionModel radialErrorModeModel_;
    private boolean radialVisible_;
    private TopcatModel tcModel_;

    /** Pattern for matching (probable) tangent error UCDs. */
    private static final Pattern TANERR_UCD_REGEX = Pattern.compile(
           "pos\\.angDistance"
        + "|pos\\.angResolution"
        + "|pos\\.errorEllipse"
        + "|(stat\\.(error|stdev);"
          + "pos\\.(eq|galactic|supergalactic|ecliptic|earth\\.l..)[.a-z]*)"
    );

    /** 
     * Constructor.
     *
     * @param  logToggler model for determining whether the radial coordinate
     *         is to be scaled logarithmically
     * @param  tangentErrorToggler  model indicating whether tangential
     *         errors will be drawn
     * @param  radialErrorModeModel   model indicating whether/how radial
     *         errors will be drawn
     */
    public SphericalAxesSelector( ToggleButtonModel logToggler,
                             ToggleButtonModel tangentErrorToggler,
                             ErrorModeSelectionModel radialErrorModeModel ) {
        logToggler_ = logToggler;
        tangentErrorToggler_ = tangentErrorToggler;
        radialErrorModeModel_ = radialErrorModeModel;

        /* Prepare column selection panel. */
        colBox_ = Box.createVerticalBox();
        String[] axisNames = new String[] { "Longitude", "Latitude" };
        JComponent[] selectors = new JComponent[ axisNames.length ];

        /* Selector for longitude column. */
        phiSelector_ = new ColumnSelector( Tables.RA_INFO, false );
        phiSelector_.setTable( null );
        phiSelector_.setEnabled( false );
        selectors[ 0 ] = phiSelector_;

        /* Selector for latitude column. */
        thetaSelector_ = new ColumnSelector( Tables.DEC_INFO, false );
        thetaSelector_.setTable( null );
        thetaSelector_.setEnabled( false );
        selectors[ 1 ] = thetaSelector_;

        /* Place longitude and latitude selectors. */
        Box tandatBox = Box.createVerticalBox();
        JLabel[] axLabels = new JLabel[ axisNames.length ];
        for ( int i = 0; i < axisNames.length; i++ ) {
            String aName = axisNames[ i ];
            JComponent cPanel = Box.createHorizontalBox();
            axLabels[ i ] = new JLabel( " " + aName + " Axis: " );
            cPanel.add( axLabels[ i ] );
            cPanel.add( new ShrinkWrapper( selectors[ i ] ) );
            cPanel.add( Box.createHorizontalStrut( 5 ) );
            cPanel.add( Box.createHorizontalGlue() );
            tandatBox.add( Box.createVerticalStrut( 5 ) );
            tandatBox.add( cPanel );
        }

        /* Place long/lat selectors alongside a container for a tangential
         * error column. */
        JComponent tanerrContainer = Box.createHorizontalBox();
        final Box tanerrBox = Box.createHorizontalBox();
        tanerrBox.add( Box.createVerticalGlue() );
        tanerrBox.add( tanerrContainer );
        tanerrBox.add( Box.createVerticalGlue() );

        /* Selector for tangential errors. */
        DefaultValueInfo sizeInfo =
            new DefaultValueInfo( "Angular Size", Number.class,
                                  "Angular size or error" );
        sizeInfo.setUnitString( "radians" );
        sizeInfo.setNullable( true );
        tanerrSelector_ = new ColumnSelector( sizeInfo, false );
        tanerrSelector_.setTable( null );
        tanerrSelector_.setEnabled( false );
        ChangeListener tanerrListener = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                tanerrBox.removeAll();
                if ( tangentErrorToggler_.isSelected() ) {
                    tanerrBox.add( new JLabel( " +/- " ) );
                    tanerrBox.add( new ShrinkWrapper( tanerrSelector_ ) );
                }
            }
        };
        tangentErrorToggler_.addChangeListener( tanerrListener );
        tanerrListener.stateChanged( null );

        /* Add long/lat business to main selector panel. */
        Box tanBox = Box.createHorizontalBox();
        tanBox.add( new ShrinkWrapper( tandatBox ) );
        tanBox.add( new ShrinkWrapper( tanerrBox ) );
        tanBox.add( Box.createHorizontalGlue() );
        colBox_.add( tanBox );

        /* Selector for radius column. */
        rSelector_ =
            new AxisDataSelector( "Radial", new String[] { "Log" },
                                  new ToggleButtonModel[] { logToggler } );
        rSelector_.setEnabled( false );
        rSelector_.setBorder( BorderFactory.createEmptyBorder( 5, 0, 5, 0 ) );
        rContainer_ = Box.createVerticalBox();
        colBox_.add( rContainer_ );
        radialErrorModeModel_.setEnabled( radialVisible_ );

        /* Align axis labels. */
        Dimension labelSize = new Dimension( 0, 0 );
        for ( int i = 0; i < axisNames.length; i++ ) {
            Dimension s = axLabels[ i ].getPreferredSize();
            labelSize.width = Math.max( labelSize.width, s.width );
            labelSize.height = Math.max( labelSize.height, s.height );
        }
        for ( int i = 0; i < axisNames.length; i++ ) {
            axLabels[ i ].setPreferredSize( labelSize );
        }

        /* Fix for changes to the error mode selections to modify the 
         * state of the axis data selectors as appropriate. */
        ActionListener radialErrorListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                rSelector_.setErrorMode( radialErrorModeModel_.getErrorMode() );
            }
        };
        radialErrorModeModel_.addActionListener( radialErrorListener );
        radialErrorListener.actionPerformed( null );
    }

    public JComponent getColumnSelectorPanel() {
        return colBox_;
    }

    public JComboBox[] getColumnSelectors() {
        return rSelector_.getSelectors();
    }

    public int getNdim() {
        return 3;
    }

    public boolean isReady() {
        return tcModel_ != null
            && getPhi() != null
            && getTheta() != null;
    }

    public StarTable getData() {
        return new SphericalPolarTable( tcModel_,
                                        getPhi(), getTheta(), getR() );
    }

    public StarTable getErrorData() {
        List<ColumnData> colList = new ArrayList<ColumnData>();
        boolean hasTanerr = tangentErrorToggler_.isSelected();
        ErrorMode radialMode = radialVisible_
                             ? radialErrorModeModel_.getErrorMode()
                             : ErrorMode.NONE;
        if ( hasTanerr ) {
            ColumnData tData = tanerrSelector_.getColumnData();
            colList.add( tData == null ? ConstantColumnData.ZERO : tData );
        }
        if ( radialMode != ErrorMode.NONE ) {
            JComboBox[] rErrorSelectors = rSelector_.getErrorSelectors();
            for ( int isel = 0; isel < rErrorSelectors.length; isel++ ) {
                Object rItem = rErrorSelectors[ isel ].getSelectedItem();
                ColumnData rData = rItem instanceof ColumnData
                                 ? (ColumnData) rItem
                                 : ConstantColumnData.ZERO;
                colList.add( rData );
            }
        }
        ColumnData[] eCols = colList.toArray( new ColumnData[ 0 ] );
        return new ColumnDataTable( tcModel_, eCols );
    }

    public StarTable getLabelData() {
        return null;
    }

    public PointStore createPointStore( int npoint ) {
        boolean hasTanerr = tangentErrorToggler_.isSelected();
        ErrorMode radialMode = radialVisible_
                             ? radialErrorModeModel_.getErrorMode()
                             : ErrorMode.NONE;
        boolean radialLog = logToggler_.isSelected();
        return new SphericalPolarPointStore( radialMode, hasTanerr, radialLog,
                                             npoint );
    }

    public ErrorMode[] getErrorModes() {
        ErrorMode[] modes = new ErrorMode[ 3 ];
        boolean hasTan = tangentErrorToggler_.isSelected();
        modes[ 0 ] = hasTan ? ErrorMode.SYMMETRIC : ErrorMode.NONE;
        modes[ 1 ] = hasTan ? ErrorMode.SYMMETRIC : ErrorMode.NONE;
        modes[ 2 ] = radialVisible_ ? radialErrorModeModel_.getErrorMode()
                                    : ErrorMode.NONE;
        return modes;
    }

    public AxisEditor[] createAxisEditors() {

        /* We only have one axis editor, that for the radial coordinate.
         * Override the default implementation of setAxis so that only
         * the upper bound can be set - the lower bound is always zero. */
        final AxisEditor ed = new AxisEditor( "Radial" ) {
            public void setAxis( ValueInfo axis ) {
                super.setAxis( axis );
                loField_.setText( "" );
                loField_.setEnabled( false );
            }
            protected double getHigh() {
                double hi = super.getHigh();
                return logToggler_.isSelected() ? Math.log( hi ) : hi;
            }
        };
        logToggler_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {

                /* For some reason I can't set the visible range from
                 * the value filled in the editor axis when the log toggle
                 * changes.  I can't work out why.  Until I manage to fix it,
                 * better to cear the editor bounds so they don't say the
                 * wrong thing. */
                ed.clearBounds();
            }
        } );

        rSelector_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                Object cItem = rSelector_.getMainSelector().getSelectedItem();
                ColumnInfo cInfo = cItem instanceof ColumnData
                                 ? ((ColumnData) cItem).getColumnInfo()
                                 : null;
                ed.setAxis( cInfo );
            }
        } );
        return new AxisEditor[] { ed };
    }

    public void setTable( TopcatModel tcModel ) {
        if ( tcModel == null ) {
            phiSelector_.getModel().getColumnModel().setSelectedItem( null );
            phiSelector_.getModel().getConverterModel().setSelectedItem( null );
            thetaSelector_.getModel().getColumnModel().setSelectedItem( null );
            thetaSelector_.getModel().getConverterModel()
                                     .setSelectedItem( null );
            tanerrSelector_.getModel().getColumnModel().setSelectedItem( null );
            tanerrSelector_.getModel().getConverterModel()
                                      .setSelectedItem( null );
        }
        else {
            phiSelector_.setTable( tcModel );
            thetaSelector_.setTable( tcModel );
            tanerrSelector_.setTable( tcModel );
            if ( tanerrSelector_.getColumnData() == null ) {
                ColumnData tanerr =
                    guessTanerrColumn( tcModel, tanerrSelector_
                                               .getModel().getColumnModel() );
                if ( tanerr != null ) {
                    tanerrSelector_.setColumnData( tanerr );
                }
            }
            if ( tanerrSelector_.getColumnData() == null ) {
                ColumnData tanerr = guessTanerrParam( tcModel );
                if ( tanerr != null ) {
                    tanerrSelector_.setColumnData( tanerr );
                }
            }
        }
        rSelector_.setTable( tcModel );
        phiSelector_.setEnabled( tcModel != null );
        thetaSelector_.setEnabled( tcModel != null );
        tanerrSelector_.setEnabled( tcModel != null );
        rSelector_.setEnabled( tcModel != null );
        tcModel_ = tcModel;
    }

    public void initialiseSelectors() {
    }

    public void addActionListener( ActionListener listener ) {
        thetaSelector_.addActionListener( listener );
        phiSelector_.addActionListener( listener );
        rSelector_.addActionListener( listener );
        tanerrSelector_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        thetaSelector_.removeActionListener( listener );
        phiSelector_.removeActionListener( listener );
        rSelector_.removeActionListener( listener );
        tanerrSelector_.removeActionListener( listener );
    }

    /**
     * Determines whether the radial axis control is visible.
     * If not, the radial axis is assumed always to have a value of unity.
     *
     * @param   vis  whether the radial control is visible
     */
    public void setRadialVisible( boolean vis ) {
        if ( vis != radialVisible_ ) {
            if ( vis ) {
                rContainer_.add( rSelector_ );
            }
            else {
                rContainer_.remove( rSelector_ );
            }
            radialVisible_ = vis;
            rContainer_.revalidate();
        }
        radialErrorModeModel_.setEnabled( radialVisible_ );
    }

    /**
     * Returns metadata describing the currently selected radial coordinate.
     * If no radial coordinate is selected (all points on the surface of
     * the sphere), <code>null</code> is returned.
     *
     * @return   radial column info
     */
    public ValueInfo getRadialInfo() {
        if ( ! radialVisible_ ) {
            return null;
        }
        Object citem = rSelector_.getMainSelector().getSelectedItem();
        if ( citem instanceof ColumnData ) {
            ColumnData cdata = (ColumnData) citem;
            return logToggler_.isSelected()
                 ? new LogColumnData( cdata ).getColumnInfo()
                 : cdata.getColumnInfo();
        }
        else {
            return null;
        }
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
        if ( ! radialVisible_ ) {
            return ConstantColumnData.ONE;
        }
        Object citem = rSelector_.getMainSelector().getSelectedItem();
        if ( citem instanceof ColumnData ) {
            ColumnData cdata = (ColumnData) citem;
            return logToggler_.isSelected() ? new LogColumnData( cdata )
                                            : cdata;
        }
        else {
            return ConstantColumnData.ONE;
        }
    }

    /**
     * Attempts to locate a column from a given list which corresponds to
     * a tangent-plane error.
     *
     * @param  tcModel  table for which the errors are required
     * @param  list of candidate ColumnData objects
     * @return  a ColumnData which probably represents tangent error, or null
     */
    private static ColumnData guessTanerrColumn( TopcatModel tcModel,
                                                 ListModel colModel ) {
        ColumnData bestCol = null;
        int bestScore = 0;
        for ( int icol = 0; icol < colModel.getSize(); icol++ ) {
            Object colItem = colModel.getElementAt( icol );
            if ( colItem instanceof ColumnData ) {
                ColumnData colData = (ColumnData) colItem;
                int score = getTanerrLikeness( colData.getColumnInfo() );
                if ( score > bestScore ) {
                    bestScore = score;
                    bestCol = colData;
                }
            }
        }
        return bestCol;
    }

    /**
     * Attempts to determine a parameter from a given table which corresponds
     * to tangent-plane error.
     *
     * @param  tcModel  table for which the errors are required
     * @return   a ColumnData which probably represents tangent error, or null
     */
    private static ColumnData guessTanerrParam( TopcatModel tcModel ) {
        ColumnData bestCol = null;
        int bestScore = 0;
        for ( DescribedValue dval : tcModel.getDataModel().getParameters() ) {
            ValueInfo info = dval.getInfo();
            Object value = dval.getValue();
            if ( value instanceof Number ) {
                int score = getTanerrLikeness( info );
                if ( score > bestScore ) {
                    bestScore = score;
                    bestCol = new ParameterColumnData( dval );
                }
            }
        }
        return bestCol;
    }

    /**
     * Returns a number indicating how much a ValueInfo looks like the
     * description of a tangent plane error.  This is the result of some
     * ad-hoc grubbing through UCDs, units etc.  A zero result means that
     * it doesn't look like a tangent plane error; higher results look
     * like progressively better bets.
     *
     * @param  info  metadata object describing value
     * @return  resemblance score (>=0)
     */
    private static int getTanerrLikeness( ValueInfo info ) {
        if ( ! Number.class.isAssignableFrom( info.getContentClass() ) ) {
            return 0;
        }
        int score = 0;
        String ucd = info.getUCD();
        String units = info.getUnitString();
        if ( ucd != null && TANERR_UCD_REGEX.matcher( ucd ).matches() ) {
            score += 3;
        }
        if ( units != null ) {
            if ( units.matches( "arcsec[a-z]*" ) ) {
                score += 2;
            }
            else if ( units.matches( "arcmin[a-z]*" ) ) {
                score += 1;
            }
        }
        return score;
    }

    /**
     * ColumnData implementation which represents the log() values of
     * a base ColumnData.
     * An intelligent implementation of equals() is provided.
     */
    private static class LogColumnData extends ColumnData {

        private final ColumnData base_;

        /**
         * Constructs a new LogColumnData.
         *
         * @param  base  (unlogged) base column data
         */
        LogColumnData( ColumnData base ) {
            base_ = base;
            ColumnInfo cinfo = new ColumnInfo( base.getColumnInfo() );
            String units = cinfo.getUnitString();
            if ( units != null && units.trim().length() > 0 ) {
                cinfo.setUnitString( "log(" + units + ")" );
            }
            cinfo.setName( "log(" + cinfo.getName() + ")" );
            cinfo.setContentClass( Double.class );
            setColumnInfo( cinfo );
        }

        public Object readValue( long irow ) throws IOException {
            Object val = base_.readValue( irow );
            if ( val instanceof Number ) {
                double dval = ((Number) val).doubleValue();
                return dval > 0 ? Double.valueOf( Math.log( dval ) )
                                : null;
            }
            else {
                return null;
            }
        }

        public boolean equals( Object o ) {
            return ( o instanceof LogColumnData )
                 ? this.base_.equals( ((LogColumnData) o).base_ )
                 : false;
        }

        public int hashCode() {
            return base_.hashCode() * 999;
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

                                row_[ 0 ] = Double.valueOf( x );
                                row_[ 1 ] = Double.valueOf( y );
                                row_[ 2 ] = Double.valueOf( z );
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

    /**
     * ColumnData which contains the (constant) value of a table parameter.
     * Implements <code>equals</code> and <code>hashCode</code> properly.
     */
    private static class ParameterColumnData extends ColumnData {

        private final String name_;
        private final Object value_;

        /**
         * Constructor.
         *
         * @param  dval   object giving parameter metadata and value
         */
        ParameterColumnData( DescribedValue dval ) {
            value_ = dval.getValue();
            ColumnInfo info = new ColumnInfo( dval.getInfo() );
            info.setContentClass( dval.getClass() );
            setColumnInfo( info );
            name_ = info.getName();
            assert value_ != null;
        }

        public Object readValue( long irow ) {
            return value_;
        }

        public String toString() {
            return StarTableJELRowReader.PARAM_PREFIX + name_;
        }

        public int hashCode() {
            return value_.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o instanceof ParameterColumnData ) {
                ParameterColumnData other = (ParameterColumnData) o;
                return other.value_.equals( this.value_ );
            }
            return false;
        }
    }
}
