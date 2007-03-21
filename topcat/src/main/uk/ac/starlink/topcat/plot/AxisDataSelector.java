package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Graphical component allowing selection of one or more columns 
 * required for plotting data along one axis of a graph.  
 * The principal column is the one which defines the centre of the
 * plotted points along the axis in question, but additional
 * ones may be required according to whether error bars will be drawn.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2007
 */
public class AxisDataSelector extends JPanel {

    private final JComponent extrasBox_;
    private final JComboBox atSelector_;
    private final JComboBox loSelector_;
    private final JComboBox hiSelector_;
    private final JComboBox lhSelector_;
    private final Map extentMap_;
    private final ActionForwarder actionForwarder_;
    private ErrorMode errorMode_ = ErrorMode.NONE;
    private static ColumnSelectionTracker colSelectionTracker_ =
        new ColumnSelectionTracker();

    /** Selector type denoting the main axis selector. */
    private static final SelectorType AT = new SelectorType( "At" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.atSelector_;
        }
    };

    /** Selector type denoting the lower bound axis selector. */
    private static final SelectorType LO = new SelectorType( "Lower" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.loSelector_;
        }
    };

    /** Selector type denoting the upper bound axis selector. */
    private static final SelectorType HI = new SelectorType( "Upper" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.hiSelector_;
        }
    };

    /** Selector type denoting the symmetric lower/upper bound axis selector. */
    private static final SelectorType LH = new SelectorType( "LowerUpper" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.lhSelector_;
        }
    };

    /**
     * Constructor.
     *
     * @param  axisName   name of the axis, used for user labels
     * @param  toggleNames  names of an array of toggle buttons to be
     *                      displayed in this component
     * @param  toggleModels toggle button models to be displayed in this
     *                      component (same length as <code>toggleNames</code>)
     */
    public AxisDataSelector( String axisName, String[] toggleNames,
                             ToggleButtonModel[] toggleModels ) {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        actionForwarder_ = new ActionForwarder();

        /* Construct all the column selectors which may be needed
         * (though possibly some will never be used). */
        atSelector_ = ColumnDataComboBoxModel.createComboBox();
        loSelector_ = ColumnDataComboBoxModel.createComboBox();
        hiSelector_ = ColumnDataComboBoxModel.createComboBox();
        lhSelector_ = ColumnDataComboBoxModel.createComboBox();
        JComboBox[] selectors = getSelectors();
        assert selectors.length == 4;
        for ( int i = 0; i < selectors.length; i++ ) {
            selectors[ i ].addActionListener( actionForwarder_ );
        }
        atSelector_.addActionListener( new ColumnSelectionListener( AT ) );
        loSelector_.addActionListener( new ColumnSelectionListener( LO ) );
        hiSelector_.addActionListener( new ColumnSelectionListener( HI ) );
        lhSelector_.addActionListener( new ColumnSelectionListener( LH ) );

        /* Cache information about which selectors represent which extents. */
        extentMap_ = new HashMap();
        extentMap_.put( ErrorMode.LOWER_EXTENT, loSelector_ );
        extentMap_.put( ErrorMode.UPPER_EXTENT, hiSelector_ );
        extentMap_.put( ErrorMode.BOTH_EXTENT, lhSelector_ );

        /* Place the main column selector. */
        placeSelector( this, axisName + " Axis:", atSelector_ );
        int ntog = toggleNames != null ? toggleNames.length : 0;

        /* Prepare a container to contain the auxiliary column selectors. */
        extrasBox_ = Box.createHorizontalBox();
        add( extrasBox_ );

        /* Place the toggle buttons. */
        for ( int itog = 0; itog < ntog; itog++ ) {
            String name = toggleNames[ itog ];
            ToggleButtonModel model = toggleModels[ itog ];
            if ( name != null && model != null ) {
                JCheckBox checkBox = model.createCheckBox();
                checkBox.setText( name );
                add( Box.createHorizontalStrut( 5 ) );
                add( checkBox );
            }
        }

        /* Pad. */
        add( Box.createHorizontalGlue() );
    }

    /**
     * Adds an action listener.
     *
     * @param   listener  action listener
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addListener( listener );
    }

    /**
     * Removes an action listener.
     *
     * @param  listener  action listener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeListener( listener );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        JComponent[] selectors = getSelectors();
        for ( int i = 0; i < selectors.length; i++ ) {
            selectors[ i ].setEnabled( enabled );
        }
    }

    /**
     * Returns the main column selector associated with this selector.
     * This is the one which defines the centre of the plotted points.
     *
     * @return  principal column selector
     */
    public JComboBox getMainSelector() {
        return atSelector_;
    }

    /**
     * Returns the currently displayed column selectors which provide
     * error information.  Which these are will depend on the current 
     * error mode.
     *
     * @return   displayed selectors apart from the main one
     */
    public JComboBox[] getErrorSelectors() {
        ErrorMode.Extent[] extents = errorMode_.getExtents();
        int nex = extents.length;
        JComboBox[] selectors = new JComboBox[ nex ];
        for ( int i = 0; i < nex; i++ ) {
            selectors[ i ] = (JComboBox) extentMap_.get( extents[ i ] );
        }
        return selectors;
    }

    /**
     * Sets the error mode.  This controls which column selectors are
     * displayed.
     *
     * @param  errorMode  error mode
     */
    public void setErrorMode( ErrorMode errorMode ) {
        if ( errorMode.equals( errorMode_ ) ) {
            return;
        }
        extrasBox_.removeAll();
        ErrorMode.Extent[] extents = errorMode.getExtents();
        for ( int i = 0; i < extents.length; i++ ) {
            ErrorMode.Extent extent = extents[ i ];
            placeSelector( extrasBox_, extent.getLabel(),
                           (JComboBox) extentMap_.get( extent ) );
        }
        errorMode_ = errorMode;
        revalidate();
    }

    /**
     * Configures this component for a given table, populating the 
     * column selectors accordingly.
     *
     * @param  tcModel  new table (may be null)
     */
    public void setTable( TopcatModel tcModel ) {
        JComboBox[] selectors = getSelectors();
        for ( int i = 0; i < selectors.length; i++ ) {
            JComboBox s = selectors[ i ];
            if ( tcModel == null ) {
                s.setSelectedItem( null );
                s.setEnabled( false );
            }
            else {
                s.setModel( new ColumnDataComboBoxModel( tcModel, Number.class,
                                                         true ) );
                s.setEnabled( true );
            }
        }
    }

    /**
     * Returns an array of all the column selectors which may be displayed
     * by this component.
     *
     * @return   array of combo boxes
     */
    private JComboBox[] getSelectors() {
        return new JComboBox[] {
            atSelector_, loSelector_, hiSelector_, lhSelector_,
        };
    }

    /**
     * Places a column selector within a container.
     *
     * @param  box  container
     * @param  label  text label describing the selector
     * @param  selector  combo box to place
     */
    private void placeSelector( JComponent box, String label, 
                                JComboBox selector ) {
        box.add( Box.createHorizontalStrut( 5 ) );
        box.add( new JLabel( label + " " ) );
        box.add( new ShrinkWrapper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
        box.add( new ComboBoxBumper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
    }

    /**
     * Makes a guess at the best error column to use for a given main column.
     *
     * @param   mainCol  column whose errors are to be determined
     * @param   type   error type
     * @param   selector  the combo box containing the possible columns for
     *          the main column (which is assumed to be the list of possible
     *          selections for the error column)
     * @return  guess for error column, or null if nothing looks suitable
     */
    private static ColumnData guessColumn( ColumnData mainCol,
                                           SelectorType type,
                                           JComboBox selector ) {
        return null; // not implemented yet.
    }

    /**
     * Action listener which converts events on individual column selectors 
     * into suitable calls to the Column Selection Tracker object.
     */
    private class ColumnSelectionListener implements ActionListener {

        private final SelectorType selectorType_;
        private ColumnData lastCol_;

        /**
         * Constructor.
         *
         * @param   selectorType  indicates the type of selector this 
         *          object is listening to
         */
        public ColumnSelectionListener( SelectorType selectorType ) {
            selectorType_ = selectorType;
        }

        public void actionPerformed( ActionEvent evt ) {
            JComboBox src = (JComboBox) evt.getSource();
            ColumnData col = (ColumnData) src.getSelectedItem();
            if ( col == lastCol_ ) {
                return;
            }
            lastCol_ = col;
            AxisDataSelector axSel = AxisDataSelector.this;
            if ( selectorType_ == AT ) {
                colSelectionTracker_.mainSelected( axSel, col );
            }
            else {
                colSelectionTracker_.auxSelected( axSel, selectorType_, col );
            }
        }
    }

    /**
     * Enumeration class which describes the type of individual column 
     * selectors within this axis data selector.
     */
    private static abstract class SelectorType {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  selector name
         */
        private SelectorType( String name ) {
            name_ = name;
        }

        /**
         * Returns the combo box corresponding to this selector type 
         * for a given AxisDataSelector object.
         *
         * @param  axSel   axis data selector
         * @return  combo box in <code>axSel</code> for this type
         */
        public abstract JComboBox getSelector( AxisDataSelector axSel );

        /**
         * Returns selector type name.
         */
        public String toString() {
            return name_;
        }
    }

    /**
     * Keeps track of which columns serve as error values for which other
     * columns.  There could be one of these per table, but it is currently
     * more convenient to have one application-wide instance.
     */
    private static class ColumnSelectionTracker {

        /**
         * Structure containing the state of this object.
         * It is a map from (main ColumnData, SelectorType) -> aux ColumnData
         * (see {@link #getKey}).  Entries exist where the value of an
         * auxiliary (error) selector is known for a given main selector value.
         * A null entry means the value should be blank; this differs from
         * an absent entry which means it is unknown.
         */
        private final Map errColMap_ = new HashMap();

        /**
         * Called when the main selector in an AxisDataSelector has a new
         * column chosen.  This may cause changes to the selections of the
         * auxiliary selectors.
         *
         * @param  axSel  the axis data selector which this event concerns
         * @param  col    the newly-selected column
         */
        public void mainSelected( final AxisDataSelector axSel,
                                  ColumnData mainCol ) {
            if ( mainCol == null ) {
                axSel.loSelector_.setSelectedItem( null );
                axSel.hiSelector_.setSelectedItem( null );
                axSel.lhSelector_.setSelectedItem( null );
            }
            else {
                for ( Iterator it =
                      Arrays.asList( new Object[] { LO, HI, LH } ).iterator();
                      it.hasNext(); ) {
                    SelectorType type = (SelectorType) it.next();
                    Object key = getKey( mainCol, type );
                    JComboBox selector = type.getSelector( axSel );
                    ColumnData col = errColMap_.containsKey( key )
                                   ? (ColumnData) errColMap_.get( key )
                                   : guessColumn( mainCol, type, selector );
                    selector.setSelectedItem( col );
                }
            }
        }

        /**
         * Called when an auxiliary selector in an AxisDataSelector has
         * a new column chosen.  This will not affect the selections in
         * any other selectors, but the selection may be remembered for
         * re-use later.
         *
         * @param  axSel  the axis data selector which this event concerns
         * @param  type   the type of selector whose selection has changed
         * @param  col    the new value for the selector
         */
        public void auxSelected( AxisDataSelector axSel, SelectorType type,
                                 ColumnData col ) {
            ColumnData mainCol =
                (ColumnData) axSel.atSelector_.getSelectedItem();
            if ( mainCol != null ) {
                Object key = getKey( mainCol, type );
                if ( col != null || errColMap_.containsKey( key ) ) {
                    errColMap_.put( key, col );
                }
            }
        }

        /**
         * Returns a key for use in this object's <code>errColMap_</code>.
         *
         * @param   mainCol   main column
         * @param   type      selector type
         * @return  opaque key object
         */
        private static Object getKey( ColumnData mainCol, SelectorType type ) {
            return Arrays.asList( new Object[] { mainCol, type, } );
        }
    }
}
