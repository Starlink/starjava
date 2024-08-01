package uk.ac.starlink.hapi;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Graphical component for selecting a time range.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2024
 */
public class DateRangePanel extends JPanel {

    private final JTextField startField_;
    private final JTextField stopField_;
    private final JLabel minLabel_;
    private final JLabel maxLabel_;
    private final ButtonModel lockModel_;
    private final SlideDateRanger slideRanger_;
    private final DateFormat isoFormat_;
    private String isoMin_;
    private String isoMax_;
    private String isoStart_;
    private String isoStop_;
    public static final String PROP_ISOSTART = "isoStart";
    public static final String PROP_ISOSTOP = "isoStop";

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public DateRangePanel() {
        super( new BorderLayout() );
        isoFormat_ = Times.createDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        startField_ = new JTextField();
        startField_.addCaretListener( evt -> updateStart() );
        stopField_ = new JTextField();
        stopField_.addCaretListener( evt -> updateStop() );
        minLabel_ = new JLabel();
        maxLabel_ = new JLabel();
        JToggleButton lockButton =
            new JToggleButton( HapiTableLoadDialog.createIcon( "lock.png" ) );
        lockButton.setToolTipText( "Prevent auto-update of start/stop dates" );
        lockModel_ = lockButton.getModel();
        slideRanger_ = new SlideDateRanger();
        slideRanger_.addPropertyChangeListener( SlideDateRanger.PROP_RANGE,
                                                evt -> updateFromSlider() );
        lockModel_.addActionListener( evt -> {
            boolean isSlide = ! lockModel_.isSelected();
            slideRanger_.setEnabled( ! lockModel_.isSelected() );
            updateFromSlider();
        } );

        /* Lay out components. */
        Box startBox = Box.createHorizontalBox();
        startBox.add( startField_ );
        startBox.add( Box.createHorizontalStrut( 10 ) );
        startBox.add( minLabel_ );
        Box stopBox = Box.createHorizontalBox();
        stopBox.add( stopField_ );
        stopBox.add( Box.createHorizontalStrut( 10 ) );
        stopBox.add( maxLabel_ );
        LabelledComponentStack fieldStack = new LabelledComponentStack();
        fieldStack.addLine( "Start Date", null, startBox, true );
        fieldStack.addLine( "Stop Date", null, stopBox, true );
        lockButton.setMargin( new Insets( 0, 0, 0, 0 ) );
        Box fieldLine = Box.createHorizontalBox();
        fieldLine.add( Box.createHorizontalStrut( 5 ) );
        fieldLine.add( lockButton );
        fieldLine.add( Box.createHorizontalStrut( 5 ) );
        fieldLine.add( fieldStack );
        Box box = Box.createVerticalBox();
        box.add( fieldLine );
        box.add( slideRanger_ );
        add( box, BorderLayout.NORTH );

        /* Initialise state. */
        setIsoLimits( null, null );
    }

    /**
     * Sets the minimum and maximum acceptable epochs.
     *
     * @param  isoMin  earliest range epoch as ISO-8601, or null
     * @param  isoMax  latest range epoch as ISO-8601, or null
     */
    public void setIsoLimits( String isoMin, String isoMax ) {
        isoMin_ = isoMin;
        isoMax_ = isoMax;
        minLabel_.setText( isoMin == null ? "" : ">= " + isoMin + " " );
        maxLabel_.setText( isoMax == null ? "" : "<= " + isoMax + " " );
        double dmin = Times.isoToUnixSeconds( isoMin );
        double dmax = Times.isoToUnixSeconds( isoMax );
        long[] secLimits = dmin < dmax
                         ? new long[] { (long) dmin, (long) dmax }
                         : new long[] { 0, 0 };
        slideRanger_.setLimits( secLimits[ 0 ], secLimits[ 1 ] );
        revalidate();
    }

    /**
     * Returns the currently selected range start time.
     * It is supposed to be an ISO-8601 string,
     * but there is no guarantee that it will be valid.
     *
     * @return   range start time string
     */
    public String getIsoStart() {
        return isoStart_ == null ? "" : isoStart_;
    }

    /**
     * Returns the currently selected range stop time.
     * It is supposed to be an ISO-8601 string,
     * but there is no guarantee that it will be valid.
     *
     * @return   range stop time string
     */
    public String getIsoStop() {
        return isoStop_ == null ? "" : isoStop_;
    }

    /**
     * Sets the range start time.
     *
     * @param  isoStart  range start time, should be ISO-8601
     */
    public void setIsoStart( String isoStart ) {
        final String txt;
        if ( isoStart == null ) {
            txt = "";
        }
        else if ( Times.isoToUnixSeconds( isoStart ) <=
                  Times.isoToUnixSeconds( isoMin_ ) ) {
            txt = isoMin_;
        }
        else {
            txt = isoStart;
        }
        startField_.setText( txt );
        updateStart();
    }

    /**
     * Sets the range stop time.
     *
     * @param  isoStop  range stop time, should be ISO-8601
     */
    public void setIsoStop( String isoStop ) {
        final String txt;
        if ( isoStop == null ) {
            txt = "";
        }
        else if ( Times.isoToUnixSeconds( isoStop ) >=
                  Times.isoToUnixSeconds( isoMax_ ) ) {
            txt = isoMax_;
        }
        else {
            txt = isoStop;
        }
        stopField_.setText( txt );
        updateStop();
    }

    /**
     * Sets the state of this panel from the state of a template panel.
     * Only the lock status and (if appropriate) start/stop times
     * are copied.
     *
     * @param  other  template  panel
     */
    public void configureFromTemplate( DateRangePanel other ) {
        boolean isLock = other.lockModel_.isSelected();
        lockModel_.setSelected( isLock );
        if ( isLock ) {
            setIsoStart( other.getIsoStart() );
            setIsoStop( other.getIsoStop() );
        }
    }

    /**
     * Updates the state of this panel from the slider state.
     * Should be called if slider state changes.
     */
    private void updateFromSlider() {
        if ( ! lockModel_.isSelected() ) {
            long[] range = slideRanger_.getRange();
            setIsoStart( Times.formatUnixSeconds( range[ 0 ], isoFormat_ ) );
            setIsoStop( Times.formatUnixSeconds( range[ 1 ], isoFormat_ ) );
            updateStart();
            updateStop();
        }
    }

    /**
     * Call if start field content may have changed.
     */
    private void updateStart() {
        String oldStart = isoStart_;
        String newStart = startField_.getText().trim();
        if ( ! Objects.equals( oldStart, newStart ) ) {
            isoStart_ = newStart;
            firePropertyChange( PROP_ISOSTART, oldStart, newStart );
        }
    }

    /**
     * Call if stop field content may have changed.
     */
    private void updateStop() {
        String oldStop = isoStop_;
        String newStop = stopField_.getText().trim();
        if ( ! Objects.equals( oldStop, newStop ) ) {
            isoStop_ = newStop;
            firePropertyChange( PROP_ISOSTOP, oldStop, newStop );
        }
    }
}
