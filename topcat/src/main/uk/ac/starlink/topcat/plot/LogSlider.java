package uk.ac.starlink.topcat.plot;

import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JSlider;

/**
 * Simple logarithmic slider.  Goes from 1 to a specified maximum,
 * only returning integer values.
 *
 * @author   Mark Taylor
 * @since    19 Jan 2005
 */
public class LogSlider extends JSlider {

    private static final int SCALE = 10000000;

    /**
     * Constructor.
     *
     * @param   max   maximum value
     */
    @SuppressWarnings("this-escape")
    public LogSlider( int max ) {
        setMaximum1( max );
    }

    /**
     * Sets the maximum.
     *
     * @param  max  maximum value
     */
    public void setMaximum1( int max ) {
        super.setMaximum( (int) ( Math.log( max ) * SCALE ) );
        Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
        for ( int val = 1; val <= max; val *= 10 ) {
            labels.put( Integer.valueOf( (int) ( Math.log( val ) * SCALE ) ),
                        new JLabel( Integer.toString( val ) ) );
        }
        setLabelTable( labels );
        setPaintLabels( false );
        setMajorTickSpacing( (int) ( Math.log( 10 ) * SCALE ) );
        setPaintTicks( true );
    }

    /**
     * Returns the current value.
     *
     * @return   value
     */
    public int getValue1() {
        return
            (int) Math.round( Math.exp( super.getValue() / (double) SCALE ) );
    }

    /**
     * Sets the current value.
     *
     * @param  val   value
     */
    public void setValue1( int val ) {
        super.setValue( (int) Math.round( ( Math.log( val ) * SCALE ) ) );
    }

    /**
     * Returns the current maximum.
     *
     * @return max
     */
    public int getMaximum1() {
        return (int) Math.ceil( Math.exp( super.getValue() / (double) SCALE ) );
    }
}
