package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.plot.Range;

/**
 * Axis configuration editor component.
 * This provides boxes in which axis labels and data ranges can be entered.
 * The data ranges are either some fixed numeric value, or Double.NaN;
 * the latter means that the limit in question is to be assigned dynamically
 * by the plot component (presumably by assessing the range of the available
 * data).
 *
 * @author   Mark Taylor
 * @since    27 Jan 2006
 */
public class AxisEditor extends JPanel {

    private final JTextField labelField_;
    private final JComponent mainBox_;
    protected final JTextField loField_;
    protected final JTextField hiField_;
    private final ActionForwarder actionForwarder_;
    private final List ranges_;
    private ValueInfo axis_;

    /**
     * Constructor.
     *
     * @param   axname   name of the axis
     */
    AxisEditor( String axname ) {
        super( new BorderLayout() );
        JLabel[] labels = new JLabel[ 2 ];

        /* Set up data entry fields. */
        labelField_ = new JTextField();
        loField_ = new JTextField( 10 );
        hiField_ = new JTextField( 10 );

        /* Arrange for actions (data entry) on the fields to be forwarded to
         * interested parties. */
        ranges_ = new ArrayList();
        actionForwarder_ = new ActionForwarder();
        ActionListener axListener = new AxisListener();
        loField_.addActionListener( axListener );
        hiField_.addActionListener( axListener );

        /* Entry box for axis label. */
        JComponent labelBox = Box.createHorizontalBox();
        labelBox.add( labels[ 0 ] = new JLabel( "Label: " ) );
        labelBox.add( labelField_ );

        /* Entry box for axis lower and upper data bounds. */
        JComponent rangeBox = Box.createHorizontalBox();
        rangeBox.add( labels[ 1 ] = new JLabel( "Range: " ) );
        rangeBox.add( loField_ );
        rangeBox.add( new JLabel( "  \u2014  " ) );  // emdash
        rangeBox.add( hiField_ );

        /* Place the components in the center of this dialogue. */
        mainBox_ = Box.createVerticalBox();
        mainBox_.add( labelBox );
        mainBox_.add( Box.createVerticalStrut( 5 ) );
        mainBox_.add( rangeBox );
        setTitle( axname + " Axis" );
        add( mainBox_ );

        /* Align labels. */
        int wmax = 0;
        for ( int i = 0; i < labels.length; i++ ) {
            wmax = Math.max( wmax, labels[ i ].getPreferredSize().width );
        }
        for ( int i = 0; i < labels.length; i++ ) {
            Dimension size = labels[ i ].getPreferredSize();
            size.width = wmax;
            labels[ i ].setPreferredSize( size );
        }

        /* Initialise. */
        setAxis( null );
    }

    /**
     * Configures this component to edit the configuration of a given axis.
     * Some of the fields will be initialised only if the submitted 
     * <code>axis</code> differs from the last one which was submitted in
     * a call to this method.  Thus it is important that the 
     * <code>equals()</code> method of <code>axis</code> is implemented
     * properly.
     *
     * @param   axis   metadata of the axis to edit
     */
    public void setAxis( ValueInfo axis ) {

        /* Don't trigger actions while updating state. */
        labelField_.removeActionListener( actionForwarder_ );
        loField_.removeActionListener( actionForwarder_ );
        hiField_.removeActionListener( actionForwarder_ );

        /* Initialise empty if we're presenting data for no axis. */
        if ( axis == null ) {
            labelField_.setText( "" );
            loField_.setText( "" );
            hiField_.setText( "" );
        }

        /* Perform some initialisation setup if the axis is different from
         * last time. */
        else if ( ! axis.equals( axis_ ) ) {
            String name = axis.getName();
            String unit = axis.getUnitString();
            String txt = unit != null && unit.trim().length() > 0
                       ? name + " / " + unit
                       : name;
            labelField_.setText( txt );
            loField_.setText( "" );
            hiField_.setText( "" );
        }
        axis_ = axis;

        /* Fields enabled according to whether the axis is blank or not. */
        labelField_.setEnabled( axis != null );
        loField_.setEnabled( axis != null );
        hiField_.setEnabled( axis != null );

        /* Restore listeners. */
        labelField_.addActionListener( actionForwarder_ );
        loField_.addActionListener( actionForwarder_ );
        hiField_.addActionListener( actionForwarder_ );
    }

    /**
     * Returns the currently entered label for the axis in this editor.
     *
     * @return  axis label string
     */
    public String getLabel() {
        return labelField_.getText();
    }

    /**
     * Adds a range which will be modified in accordance with changes of
     * the state of this editor.  Note the converse does not apply:
     * changes to <code>range</code> will not be refelected by this 
     * component.
     *
     * @param   range  range to maintain
     */
    public void addMaintainedRange( Range range ) {
        ranges_.add( range );
    }

    /**
     * Removes a range previously added by {@link #addMaintainedRange}.
     * Note that object identity not equality is used for removal.
     *
     * @param   range to unmaintain
     */
    public void removeMaintainedRange( Range range ) {

        /* Note we want to use == not .equals(), so List.remove() is no good. */
        for ( Iterator it = ranges_.iterator(); it.hasNext(); ) {
            if ( it.next() == range ) {
                it.remove();
            }
        }
    }

    /**
     * Returns the currently requested data range.
     * The result is a 2-element array giving lower, then upper bounds
     * in that order.  Either or both elements may be Double.NaN, indicating
     * no preferred limit.
     *
     * @return  (lo,hi) array
     */
    public double[] getAxisBounds() {
        double low = getLow();
        double high = getHigh();
        if ( low > high ) {
            loField_.setText( "" );
            hiField_.setText( "" );
            return new double[] { Double.NaN, Double.NaN };
        }
        else {
            return new double[] { low, high };
        }
    }

    /**
     * Clears the upper and lower bounds in this editor.
     */
    public void clearBounds() {
        setAxisBounds( Double.NaN, Double.NaN );
    }

    /**
     * Set the requested lower and upper bounds for the axis being edited.
     * Either or both values may be Double.NaN, indicating no preferred limit.
     * No listeners are informed.
     *
     * @param   low  lower bound
     * @param   high  upper bound
     */
    private void setAxisBounds( double low, double high ) {
        loField_.removeActionListener( actionForwarder_ );
        hiField_.removeActionListener( actionForwarder_ );
        loField_.setText( Double.isNaN( low ) ? "" : Double.toString( low ) );
        hiField_.setText( Double.isNaN( high ) ? "" : Double.toString( high ) );
        loField_.addActionListener( actionForwarder_ );
        hiField_.addActionListener( actionForwarder_ );
    }

    /**
     * Registers a listener to be notified when the state of this component
     * changes.
     *
     * @param  listener   listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Unregisters a listener added by {@link #addActionListener}.
     *
     * @param  listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Sets the title of this editor.  It is used to label the component's
     * border.
     *
     * @param   title  title text
     */
    public void setTitle( String title ) {
        mainBox_.setBorder( AuxWindow.makeTitledBorder( title ) );
    }

    /**
     * Returns the current lower bound, fixing state if necessary.
     *
     * @return   lower bound, may be NaN
     */
    protected double getLow() {
        String txt = loField_.getText();
        if ( txt == null || txt.trim().length() == 0 ) {
            return Double.NaN;
        }
        else {
            try {
                return Double.parseDouble( txt );
            }
            catch ( NumberFormatException e ) {
                loField_.setText( "" );
                return Double.NaN;
            }
        }
    }

    /**
     * Returns the current upper bound, fixing state if necessary.
     *
     * @return  upper bound, may be NaN
     */
    protected double getHigh() {
        String txt = hiField_.getText();
        if ( txt == null || txt.trim().length() == 0 ) {
            return Double.NaN;
        }
        else {
            try {
                return Double.parseDouble( txt );
            }
            catch ( NumberFormatException e ) {
                hiField_.setText( "" );
                return Double.NaN;
            }
        }
    }

    /**
     * Updates all of the Ranges currently maintained by this editor
     * according to its current state.
     */
    public void updateRanges() {
        for ( Iterator it = ranges_.iterator(); it.hasNext(); ) {
            double[] bounds = getAxisBounds();
            ((Range) it.next()).setBounds( bounds[ 0 ], bounds[ 1 ] );
        }
    }

    /**
     * Listens on field entries, ensuring that this component is in a
     * consistent state.
     */
    private class AxisListener implements ActionListener {
        public void actionPerformed( ActionEvent evt ) {
            Object src = evt.getSource();
            if ( src == loField_ ) {
                if ( getLow() > getHigh() ) {
                    hiField_.setText( "" ); 
                }
            }
            else if ( src == hiField_ ) {
                if ( getHigh() < getLow() ) {
                    loField_.setText( "" );
                }
            }
            updateRanges();
        }
    }
}
