package uk.ac.starlink.hapi;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Hashtable;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Graphical time range selector component.
 * It can select a range between two supplied epochs, and consists of
 * two sliders, a linear one to choose the (sort of) central epoch,
 * and a logarithmic one to choose the duration.
 *
 * <p>Times are characterised as long integers giving the number
 * of seconds since the Unix epoch (1970-01-01).
 *
 * <p>This is intended for human-range timescales; if more than a few
 * centuries around 1970 are required it might need some adjustment.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2024
 */
public class SlideDateRanger extends JPanel {

    private final JSlider durationSlider_;
    private final JSlider epochSlider_;
    private final boolean smooth_;
    private Scaler durationScaler_;
    private Scaler epochScaler_;
    private long minSec_;
    private long maxSec_;
    private long[] range_;

    /** Name of property which gives the {startSec, stopSec} long[] array. */
    public static final String PROP_RANGE = "range";

    /**
     * Constructor.
     */
    public SlideDateRanger() {
        super( new BorderLayout() );
        epochSlider_ = new JSlider();
        durationSlider_ = new JSlider();
        smooth_ = false;

        Box sbox = Box.createHorizontalBox();
        sbox.add( epochSlider_ );
        sbox.add( Box.createHorizontalStrut( 10 ) );
        sbox.add( durationSlider_ );
        add( sbox, BorderLayout.NORTH );
        setLimits( 0, 0 );
        for ( JSlider slider :
              new JSlider[] { epochSlider_, durationSlider_ } ) {
            slider.setPaintLabels( true );
            slider.setPaintTicks( false );
            slider.addChangeListener( evt -> updateRange() );
        }
    }

    /**
     * Sets the minimum and maximum acceptable dates.
     *
     * @param  minSec  earliest permitted range start, in Unix seconds
     * @param  maxSec  latest permitted range end, in Unix seconds
     */
    public void setLimits( long minSec, long maxSec ) {
        if ( ! ( minSec < maxSec ) ) {
            minSec = 0;
            maxSec = System.currentTimeMillis() / 1000L;
        }
        minSec_ = minSec;
        maxSec_ = maxSec;
        epochScaler_ = createEpochScaler( minSec, maxSec );
        epochScaler_.configureSlider( epochSlider_ );
        durationScaler_ = createDurationScaler( maxSec - minSec );
        durationScaler_.configureSlider( durationSlider_ );
        durationSlider_.setValue( durationScaler_.secondsToScale( 3600 ) );
    }

    /**
     * Returns the currently selected range.
     *
     * @return   2-element array giving range (start, stop) in Unix seconds
     */
    public long[] getRange() {
        int durationValue = durationSlider_.getValue();
        int epochValue = epochSlider_.getValue();
        int epochMin = epochSlider_.getMinimum();
        int epochMax = epochSlider_.getMaximum();
        long durationSec = durationScaler_.scaleToSeconds( durationValue );
        long epochSec = epochScaler_.scaleToSeconds( epochValue );
        final long startSec;
        final long stopSec;
        if ( smooth_ ) {
            double epochFrac = 1.0 * ( epochValue - epochMin )
                             / ( epochMax - epochMin );
            startSec = epochSec - (long) ( epochFrac * durationSec );
            stopSec = epochSec + (long) ( ( 1. - epochFrac ) * durationSec );
        }
        else {
            if ( epochSec + durationSec <= maxSec_ ) {
                startSec = epochSec;
                stopSec = epochSec + durationSec;
            }
            else {
                stopSec = maxSec_;
                startSec = stopSec - durationSec;
            }
        }
        return new long[] { Math.max( minSec_, startSec ),
                            Math.min( maxSec_, stopSec ) };
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        durationSlider_.setEnabled( isEnabled );
        epochSlider_.setEnabled( isEnabled );
    }

    /**
     * Updates the range value according to the current graphical state
     * and informs listeners if there has been a change.
     * Should be called if the graphical state has changed.
     */
    private void updateRange() {
        long[] oldRange = range_;
        long[] newRange = getRange();
        if ( ! Arrays.equals( oldRange, newRange ) ) {
            range_ = newRange;
            firePropertyChange( PROP_RANGE, oldRange, newRange );
        }
    }

    /**
     * Creates a scaler for the epoch slider.
     * It is linear and annotated in years.
     *
     * @param  minSec  earliest permitted epoch in unix seconds
     * @param  maxSec  latest permitted epoch in unix seconds
     * @return  new scaler
     */
    private static Scaler createEpochScaler( long minSec, long maxSec ) {
        Scaler scaler = new Scaler( minSec, maxSec ) {
            public long scaleToSeconds( int scale ) {
                return (long) scale * 1000L;
            }
            public int secondsToScale( long sec ) {
                return (int) ( sec / 1000 );
            }
        };
        int minYear = Times.secToYear( minSec );
        int maxYear = Times.secToYear( maxSec );
        int ystep = Math.max( 1, ( maxYear - minYear ) / 4 );
        for ( int iy = minYear; iy <= maxYear; iy++ ) {
            if ( iy % ystep == 0 ) {
                long sec = Times.yearToSec( iy );
                if ( sec >= minSec && sec <= maxSec ) {
                    scaler.addTick( sec, Integer.toString( iy ) );
                }
            }
        }
        if ( scaler.labelTable_.isEmpty() ) {
            assert minYear == maxYear;
            scaler.addTick( ( maxSec + minSec ) / 2,
                            Integer.toString( minYear ) );
        }
        return scaler;
    }

    /**
     * Creates a scalar for the duration slider.
     * It is logarithmic.
     *
     * @param   maximum permitted duration in unix seconds
     * @return   new scaler
     */
    private static Scaler createDurationScaler( long maxDuration ) {
        Scaler scaler = new Scaler( 1, maxDuration ) {
            public long scaleToSeconds( int scale ) {
                return (long) Math.pow( 10, 0.01 * scale );
            }
            public int secondsToScale( long seconds ) {
                return (int) ( Math.log10( seconds ) * 100 );
            }
        };
        scaler.addTick( 1, "1sec" );
        scaler.addTick( 60, "1min" );
        scaler.addTick( 3600, "1hr" );
        scaler.addTick( 3600 * 24, "1day" );
        scaler.addTick( 3600 * 24 * 365 / 12, "1m" );
        scaler.addTick( 3600 * (long) (24 * 365.25), "1yr" );
        return scaler;
    }

    /**
     * Maps values in unix seconds to values suitable for use with a JSlider.
     */
    private static abstract class Scaler {
        final long secMin_;
        final long secMax_;
        final Hashtable<Integer,JComponent> labelTable_;

        /**
         * Constructor.
         *
         * @param  secMin  slider lower bound in seconds
         * @param  secMax  slider upper bound in seconds
         */
        Scaler( long secMin, long secMax ) {
            secMin_ = secMin;
            secMax_ = secMax;
            labelTable_ = new Hashtable<>();
        }

        /**
         * Maps a value in seconds to a slider value.
         *
         * @param  seconds  value in seconds
         * @return  value in slider range
         */
        abstract int secondsToScale( long seconds );

        /**
         * Maps a slider value to a value in seconds.
         *
         * @param  scale  value in slider range
         * @return  value in seconds
         */
        abstract long scaleToSeconds( int scale );

        /**
         * Adds an axis annotation.
         *
         * @param  seconds  value in seconds to annotate
         * @param  txt   axis annotation text
         */
        void addTick( long seconds, String text ) {
            labelTable_.put( secondsToScale( seconds ), new JLabel( text ) );
        }

        /**
         * Configures a slider for use with this scaling.
         *
         * @param  slider  slider
         */
        void configureSlider( JSlider slider ) {
            slider.setMinimum( secondsToScale( secMin_ ) );
            slider.setMaximum( secondsToScale( secMax_ ) );
            slider.setLabelTable( labelTable_ );
        }
    }
}
