package uk.ac.starlink.ttools.plot2.config;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

/**
 * Double value specifier that uses a slider to choose a value in the
 * range betwen two given values.  Linear and logarithmic scaling are
 * available.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2014
 */
public class SliderSpecifier extends SpecifierPanel<Double> {

    private final double lo_;
    private final double hi_;
    private final boolean log_;
    private final JSlider slider_;
    private static final int MIN = 0;
    private static final int MAX = 10000;

    /**
     * Constructor.
     *
     * @param   lo   slider lower bound
     * @param   hi   slider upper bound
     * @param  log  true for logarithmic slider scale, false for linear
     */
    public SliderSpecifier( double lo, double hi, boolean log ) {
        super( true );
        lo_ = lo;
        hi_ = hi;
        log_ = log;
        slider_ = new JSlider( MIN, MAX );
    }

    protected JComponent createComponent() {
        slider_.addChangeListener( getChangeForwarder() );
        return slider_;
    }

    public Double getSpecifiedValue() {
        return scale( slider_.getValue() );
    }

    public void setSpecifiedValue( Double dval ) {
        slider_.setValue( unscale( dval.doubleValue() ) );
    }

    /**
     * Adds a change listener.
     *
     * @param  listener   listener to add
     */
    public void addChangeListener( ChangeListener listener ) {
        slider_.addChangeListener( listener );
    }

    /**
     * Removes a change listener.
     *
     * @param  listener  previously added listener
     */
    public void removeChangeListener( ChangeListener listener ) {
        slider_.removeChangeListener( listener );
    }

    /**
     * Turns a slider value into a specified value.
     *
     * @param  ival  slider position
     * @return  specifier value
     */
    private double scale( int ival ) {
        double f = ( ival - MIN ) / (double) ( MAX - MIN );
        return log_ ? lo_ * Math.pow( hi_ / lo_, f )
                    : lo_ + ( hi_ - lo_ ) * f;
    }

    /**
     * Turns a specified value into a slider value.
     *
     * @param  dval  specifier value
     * @return  slider position
     */
    private int unscale( double dval ) {
        double s = log_ ? Math.log( dval / lo_ ) / Math.log( hi_ / lo_ )
                        : ( dval - lo_ ) / ( hi_ - lo_ );
        return (int) Math.round( s * ( MAX - MIN ) ) + MIN;
    }
}
