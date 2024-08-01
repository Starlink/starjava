package uk.ac.starlink.topcat.plot;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Widget for selecting lower and upper percentiles for image display.
 * This is currently implemented as two adjacent horizontal sliders
 * with logarithmic scales, one going from near zero to 0.5, and the
 * other from 0.5 to near 1.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2005
 */
public class CutChooser extends JPanel {

    private final JSlider loSlider_;
    private final JSlider hiSlider_;
    private final List<ChangeListener> changeListeners_;
    private boolean percentileLabels_ = true;
    private double min_ = 0.001;
    private static final int SCALE = 10000;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public CutChooser() {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

        /* Construct sliders.  Each has a scale from (SCALE*log(min_))
         * to SCALE*0.5.  This will be interpreted to the outside world
         * as a logarithmic scale between min_ and 0.5. */
        loSlider_ = new JSlider( (int) ( Math.log( min_ ) * SCALE ),
                                 (int) ( Math.log( 0.5 ) * SCALE ) );
        hiSlider_ = new JSlider( (int) -( Math.log( 0.5 ) * SCALE ),
                                 (int) -( Math.log( min_ ) * SCALE ) );

        /* Construct a table of labels with which to annotate the sliders. */
        Hashtable<Integer,JLabel> loLabels = new Hashtable<Integer,JLabel>();
        Hashtable<Integer,JLabel> hiLabels = new Hashtable<Integer,JLabel>();
        double[] points = new double[] { .5, .1, .01, .001, };
        for ( int i = 0; i < points.length; i++ ) {
            double point = points[ i ];
            loLabels.put( Integer
                         .valueOf( (int) ( Math.log( point ) * SCALE ) ),
                          new JLabel( formatLabel( point ) ) );
            hiLabels.put( Integer
                         .valueOf( (int) ( -Math.log( point ) * SCALE ) ),
                          new JLabel( formatLabel( 1. - point ) ) );
        }
        loSlider_.setLabelTable( loLabels );
        hiSlider_.setLabelTable( hiLabels );

        /* Configure and place the sliders. */
        loSlider_.setPaintLabels( true );
        hiSlider_.setPaintLabels( true );
        loSlider_.setPaintTicks( false );
        hiSlider_.setPaintTicks( false );
        add( loSlider_ );
        add( Box.createHorizontalStrut( 5 ) );
        add( hiSlider_ );

        /* Arrange to notify listeners when one of the sliders has moved
         * (but not while it's moving). */
        changeListeners_ = new ArrayList<ChangeListener>();
        ChangeListener changer = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( ! ((JSlider) evt.getSource()).getValueIsAdjusting() ) {
                    ChangeEvent fevt = new ChangeEvent( CutChooser.this );
                    for ( ChangeListener l : changeListeners_ ) {
                        l.stateChanged( fevt );
                    }
                }
            }
        };
        loSlider_.addChangeListener( changer );
        hiSlider_.addChangeListener( changer );
    }

    /**
     * Sets the lower cut value.
     *
     * @param   val  value (coerced to between minimum and 0.5)
     */
    public void setLowValue( double val ) {
        val = Math.min( Math.max( val, min_ ), 0.5 );
        loSlider_.setValue( (int) ( Math.log( val ) * SCALE ) );
    }

    /**
     * Sets the upper cut value
     *
     * @param  val   value (coerced to between 0.5 and maximum)
     */
    public void setHighValue( double val ) {
        val = Math.min( Math.max( val, 0.5 ), 1.0 - min_ );
        hiSlider_.setValue( (int) ( -Math.log( 1.0 - val ) * SCALE ) );
    }

    /**
     * Adds a listener which will be notified when the cut levels have
     * changed (but not while they are changing).
     *
     * @param  listener  listener to add
     */
    public void addChangeListener( ChangeListener listener ) {
        changeListeners_.add( listener );
    }

    /**
     * Removes a listener added by <code>addChangeListener</code>.
     *
     * @param  listener  listener to remove
     */
    public void removeChangeListener( ChangeListener listener ) {
        changeListeners_.remove( listener );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        loSlider_.setEnabled( enabled );
        hiSlider_.setEnabled( enabled );
    }

    /**
     * Returns the lower value specified by this component.
     *
     * @return  low cut value; 0&lt;=val&lt;=1 and val&lt;=getHighValue()
     */
    public double getLowValue() {
        return Math.exp( loSlider_.getValue() / (double) SCALE );
    }

    /** 
     * Returns the upper value specified by this component.
     *
     * @return  high cut value; 0&lt;=val&lt;=1 and val&gt;=getLowValue()
     */
    public double getHighValue() {
        return 1.0 - Math.exp( - hiSlider_.getValue() / (double) SCALE );
    }

    /**
     * Returns a string suitable for labelling the sliders at a given value.
     *
     * @param  frac  value between 0 and 1
     */
    private String formatLabel( double frac ) {
        double perc = ( percentileLabels_ ? 100 : 1 ) * frac;
        if ( perc == (int) perc ) {
            return Integer.toString( (int) perc );
        }
        else {
            return Float.toString( (float) perc );
        }
    }
}
