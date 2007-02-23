package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    private final ActionForwarder actionForwarder_;
    private ErrorMode errorMode_ = ErrorMode.NONE;

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
        
        /* Place the main column selector. */
        placeSelector( this, axisName + " Axis", atSelector_ );
        int ntog = toggleNames != null ? toggleNames.length : 0;

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

        /* Prepare a container to contain the auxiliary column selectors. */
        extrasBox_ = Box.createHorizontalBox();
        add( extrasBox_ );
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
        if ( ErrorMode.SYMMETRIC.equals( errorMode ) ) {
            placeSelector( extrasBox_, "+/-", lhSelector_ );
        }
        else if ( ErrorMode.LOWER.equals( errorMode ) ) {
            placeSelector( extrasBox_, "-", loSelector_ );
        }
        else if ( ErrorMode.UPPER.equals( errorMode ) ) {
            placeSelector( extrasBox_, "+", hiSelector_ );
        }
        else if ( ErrorMode.BOTH.equals( errorMode ) ) {
            placeSelector( extrasBox_, "-", loSelector_ );
            placeSelector( extrasBox_, "+", hiSelector_ );
        }
        else {
            assert ErrorMode.NONE.equals( errorMode );
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
        box.add( new JLabel( label + ": " ) );
        box.add( new ShrinkWrapper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
        box.add( new ComboBoxBumper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
    }
}
