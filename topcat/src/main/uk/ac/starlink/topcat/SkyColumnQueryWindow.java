package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.convert.SkyUnits;

/**
 * Query window which allows the user to specify new sky coordinate columns
 * based on old ones.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2005
 */
public class SkyColumnQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private final InCoordSelector inSelector_;
    private final OutCoordSelector outSelector_;

    /**
     * Constructor.
     *
     * @param   tcModel  topcat model
     * @param   parent   parent component
     */
    public SkyColumnQueryWindow( TopcatModel tcModel, Component parent ) {
        super( "Sky Coordinate Columns", parent );
        tcModel_ = tcModel;

        inSelector_ = new InCoordSelector();
        outSelector_ = new OutCoordSelector();
        inSelector_.getComponent()
                   .setBorder( makeTitledBorder( "Input Coordinates" ) );
        outSelector_.getComponent()
                    .setBorder( makeTitledBorder( "Output Coordinates" ) );
 
        JComponent main = getMainArea();
        Box box = Box.createHorizontalBox();
        getMainArea().add( box );
        box.add( inSelector_.getComponent() );
        box.add( Box.createHorizontalStrut( 10 ) );
        box.add( new JLabel( ResourceIcon.FORWARD ) );
        box.add( Box.createHorizontalStrut( 10 ) );
        box.add( outSelector_.getComponent() );

        addHelp( "SkyColumnQueryWindow" );
    }

    /**
     * Returns the selected position at which to append the new columns.
     *
     * @return   append position of first column; -1 for the end
     */
    private int getAppendPosition() {
        return -1;
    }

    /**
     * Returns the epoch value associated with this conversion.
     *
     * @return   epoch value
     */
    private double getEpoch() {
        return 2000.0;
    }

    /**
     * Attempt to add the new columns.
     */
    protected boolean perform() {

        /* Collect values from window state. */
        StarTableColumn[] inCols = inSelector_.getCoordColumns();
        String[] outNames = outSelector_.getCoordNames();
        SkySystem inSys = inSelector_.getSystem();
        SkyUnits inUnits = inSelector_.getUnits();
        SkySystem outSys = outSelector_.getSystem();
        SkyUnits outUnits = outSelector_.getUnits();
        double epoch = getEpoch();

        /* Check we have everything that we need. */
        String msg;
        if ( inCols[ 0 ] == null || inCols[ 1 ] == null ) {
            msg = "Input coordinates not specified";
        }
        else if ( outNames[ 0 ] == null ||
                  outNames[ 1 ] == null ||
                  outNames[ 0 ].trim().length() == 0 ||
                  outNames[ 1 ].trim().length() == 0 ) {
            msg = "Output coordinates not specified";
        }
        else {
            msg = null;
        }
        if ( msg != null ) {
            JOptionPane.showMessageDialog( this, msg,
                                           "Coordinate Conversion Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }

        /* Create the new columns. */
        final StarTable dataModel = tcModel_.getDataModel();
        final int icol1 = inCols[ 0 ].getModelIndex();
        final int icol2 = inCols[ 1 ].getModelIndex();
        final CoordConverter conv = new CoordConverter( inSys, inUnits,
                                                        outSys, outUnits,
                                                        epoch ) {
            public Object[] getInCoords( long irow ) throws IOException {
                return new Object[] {
                    dataModel.getCell( irow, icol1 ),
                    dataModel.getCell( irow, icol2 ),
                };
            }
        };
        ColumnData[] outData = new ColumnData[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            DefaultValueInfo info = new DefaultValueInfo( outNames[ i ] );
            info.setContentClass( outUnits.getUnitTypes()[ i ] );
            info.setDescription( outSys.getCoordinateDescriptions()[ i ] );
            info.setUnitString( outUnits.getUnitStrings()[ i ] );
            final int i0 = i;
            outData[ i ] = new ColumnData( info ) {
                public Object readValue( long irow ) {
                    return conv.getOutCoords( irow )[ i0 ];
                }
            };
        }

        /* Add the new columns to the table. */
        int ipos = getAppendPosition();
        int ip1;
        int ip2;
        if ( ipos < 0 ) {
           ip1 = -1;
           ip2 = -1;
        }
        else {
           ip1 = ipos;
           ip2 = ipos + 1;
        }
        tcModel_.appendColumn( outData[ 0 ], ip1 );
        tcModel_.appendColumn( outData[ 1 ], ip2 );
        return true;
    }

    /**
     * Abstract class defining common behaviour for selecting a pair
     * of sky coordinates to form the input or output side of a conversion.
     */
    private abstract class CoordSelector {
        LabelledComponentStack stack_;
        JComboBox sysChooser_;
        JComboBox unitChooser_;
        JLabel c1label_;
        JLabel c2label_;

        /**
         * Returns the component which is to be presented to the user for
         * making selections.
         *
         * @return  chooser component
         */
        JComponent getComponent() {
            if ( stack_ == null ) {
                stack_ = createQueryStack();
            }
            return stack_;
        }

        /**
         * Returns the components used to select names for the two 
         * coordinates associated with this chooser.
         *
         * @return  2-element array of coordinate name choosers
         */
        abstract JComponent[] getCoordChoosers();

        /**
         * Returns the selected coordinate system object.
         *
         * @return  system
         */
        SkySystem getSystem() {
            return (SkySystem) sysChooser_.getSelectedItem();
        }

        /**
         * Returns the selected coordinate units object.
         *
         * @return  units
         */
        SkyUnits getUnits() {
            return (SkyUnits) unitChooser_.getSelectedItem();
        }

        /**
         * Invoked when a new system is selected.
         *
         * @param  sys  new system
         */
        void systemSelected( SkySystem sys ) {
            String[] cnames = sys.getCoordinateNames();
            c1label_.setText( cnames[ 0 ] + ":  " );
            c2label_.setText( cnames[ 1 ] + ":  " );
        }

        /**
         * Constructs the stack of components which form the body of
         * this selector.
         *
         * @return   new component stack
         */
        private LabelledComponentStack createQueryStack() {
            LabelledComponentStack stack = new LabelledComponentStack();
            sysChooser_ = new JComboBox( SkySystem.getKnownSystems() );
            unitChooser_ = new JComboBox( SkyUnits.getKnownUnits() );
            JComponent[] coordChoosers = getCoordChoosers();
            stack.addLine( "System", sysChooser_ );
            stack.addLine( "Units", unitChooser_ );
            stack.addLine( "Coord 1", coordChoosers[ 0 ] );
            stack.addLine( "Coord 2", coordChoosers[ 1 ] );

            JLabel[] labels = stack.getLabels();
            c1label_ = labels[ 2 ];
            c2label_ = labels[ 3 ];
            assert c1label_.getText().startsWith( "Coord 1" );
            assert c2label_.getText().startsWith( "Coord 2" );

            /* Fix for labels on the coordinate entry widgets to change
             * appropriately for the system. */
            sysChooser_.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent evt ) {
                    if ( evt.getStateChange() == evt.SELECTED ) {
                        systemSelected( (SkySystem) evt.getItem() );
                    }
                }
            } );

            /* Make sure it initializes to a default value. */
            SkySystem dsys = (SkySystem) sysChooser_.getSelectedItem();
            sysChooser_.setSelectedItem( null );
            sysChooser_.setSelectedItem( dsys );

            return stack;
        }
    }

    /**
     * CoordSelector implemenatation for input columns.
     * This one has combo boxes to choose columns from the existing set.
     */
    private class InCoordSelector extends CoordSelector {

        private JComboBox[] coordChoosers_;

        /**
         * Produce combo boxes for selecting coordinates.
         */
        private JComboBox[] createCoordChoosers() {
            final JComboBox[] coordChoosers = new JComboBox[ 2 ];
            for ( int i = 0; i < 2; i++ ) {
                JComboBox box = new JComboBox();
                box.setRenderer( new ColumnCellRenderer( box ) );
                coordChoosers[ i ] = box;
            }

            /* Make sure that the selections change appropriately with the
             * units.  If sexagesimal is chosen, then string-type columns
             * will be selected, otherwise numeric ones. */
            unitChooser_.addItemListener( new ItemListener() {
                private ComboBoxModel dummyModel = new DefaultComboBoxModel();
                public void itemStateChanged( ItemEvent evt ) {
                    int state = evt.getStateChange();
                    if ( state == evt.SELECTED ) {
                        ComboBoxModel[] models = getColumnModels();
                        for ( int i = 0; i < 2; i++ ) {
                            coordChoosers[ i ].setModel( models[ i ] );
                        }
                    }
                }
            } );

            /* Make sure it gets set up correctly for the default values. */
            SkyUnits dunit = (SkyUnits) unitChooser_.getSelectedItem();
            unitChooser_.setSelectedItem( null );
            unitChooser_.setSelectedItem( dunit );

            return coordChoosers;
        }

        /**
         * Returns appropriate combo box models for coodinate column selection
         * according to the current state of this component.
         */
        private ComboBoxModel[] getColumnModels() {
            SkyUnits units = (SkyUnits) unitChooser_.getSelectedItem();
            Class okClass = ( units == SkyUnits.SEXAGESIMAL ) ? String.class
                                                              : Number.class;
            ComboBoxModel[] models = new ComboBoxModel[ 2 ];
            for ( int i = 0; i < 2; i++ ) {
                models[ i ] = RestrictedColumnComboBoxModel
                             .makeClassColumnComboBoxModel( tcModel_
                                                           .getColumnModel(),
                                                           false, okClass );
            }
            return models;
        }

        /**
         * Returns the coordinate selection widgets; they are combo boxes.
         */
        JComponent[] getCoordChoosers() {
            if ( coordChoosers_ == null ) {
                coordChoosers_ = createCoordChoosers();
            }
            return coordChoosers_;
        }

        /**
         * Returns the columns selected for input coordinate values.
         *
         * @return  two-element array of columns giving selected coordinates
         */
        StarTableColumn[] getCoordColumns() {
            return new StarTableColumn[] {
                (StarTableColumn) coordChoosers_[ 0 ].getSelectedItem(),
                (StarTableColumn) coordChoosers_[ 1 ].getSelectedItem(),
            };
        }
    }

    /**
     * CoordSelector implementation for output columns.
     * This one has text input boxes to input column names.
     */
    private class OutCoordSelector extends CoordSelector {

        private final JTextField[] coordEntries_;

        OutCoordSelector() {
            coordEntries_ = new JTextField[] {
                new JTextField(),
                new JTextField(),
            };
        }

        JComponent[] getCoordChoosers() {
            return coordEntries_;
        }

        /**
         * Returns column names to use 
         */
        String[] getCoordNames() {
            return new String[] {
                coordEntries_[ 0 ].getText(),
                coordEntries_[ 1 ].getText(),
            };
        }

        void systemSelected( SkySystem sys ) {
            super.systemSelected( sys );
            String[] colnames = sys.getCoordinateColumnNames();
            for ( int i = 0; i < 2; i++ ) {
                String colname = TopcatUtils
                                .getDistinctName( tcModel_.getColumnList(),
                                                  colnames[ i ], "x" );
                coordEntries_[ i ].setText( colname );
            }
        }
    }

    /**
     * Helper class which performs the actual coordinate conversion.
     * This calculates both coordinate values at once; in most cases
     * any client will want both the output coordinates rather than 
     * just one, so this is likely to be more efficient than calculating
     * just one at a time, since most of the work has to be done each time.
     */
    private abstract static class CoordConverter {

        private final SkySystem inSys_;
        private final SkySystem outSys_;
        private final SkyUnits inUnits_;
        private final SkyUnits outUnits_;
        private final double epoch_;
        private long irow_ = -1L;
        private Object[] outCoords_;

        /**
         * Constructor.
         *
         * @param  inSys  input sky coordinate system
         * @param  inUnits  input units
         * @param  outSys  output sky coordinate system
         * @param  outUnits  output units
         * @param  epoch   epoch relating to the conversion 
         *                 (I'm not sure if it makes astronomical sense to
         *                 specify it like this)
         */
        CoordConverter( SkySystem inSys, SkyUnits inUnits,
                        SkySystem outSys, SkyUnits outUnits, double epoch ) {
            inSys_ = inSys;
            inUnits_ = inUnits;
            outSys_ = outSys;
            outUnits_ = outUnits;
            epoch_ = epoch;
        }

        /**
         * Obtains the two input coordinates for a particular row.
         *
         * @param   irow  index of the row at which the coordinates are required
         * @return  two-element array of coordinate values in the form
         *          determined by <code>inSys</code> and <code>inUnits</code>
         */
        abstract Object[] getInCoords( long irow ) throws IOException;

        /**
         * Returns the output coordinate objects, in the form determined
         * by <code>outSys</code> and <code>outUnits</code> at a given row.
         *
         * @param  irow  row index for calculation
         * @return  two-element array of coordinate values in the form
         *          determined by <code>outSys</code> and <code>outUnits</code>
         */
        synchronized Object[] getOutCoords( long irow ) {
            if ( irow != irow_ ) {
                Object[] in;
                try { 
                    in = getInCoords( irow );
                }
                catch ( IOException e ) {
                    in = new Object[ 2 ];
                }
                if ( Tables.isBlank( in[ 0 ] ) || Tables.isBlank( in[ 1 ] ) ) {
                    outCoords_ = new Object[ 2 ];
                }
                else {
                    try {
                        double[] inRads = inUnits_.decode( in[ 0 ], in[ 1 ] );
                        double[] fk5 =
                            inSys_.toFK5( inRads[ 0 ], inRads[ 1 ], epoch_ );
                        double[] outRads =
                            outSys_.fromFK5( fk5[ 0 ], fk5[ 1 ], epoch_ );
                        outCoords_ =
                            outUnits_.encode( outRads[ 0 ], outRads[ 1 ] );
                    }
                    catch ( RuntimeException e ) {
                        outCoords_ = new Object[ 2 ];
                    }
                }
                irow_ = irow;
            }
            return outCoords_;
        }
    }
}
