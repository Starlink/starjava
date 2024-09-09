package uk.ac.starlink.ttools.plot2;

/**
 * Aggregates a list of Ticks with the orientation with which their labels
 * should be drawn on the axis.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2024
 */
public class TickRun {

    private final Tick[] ticks_;
    private final Orientation orient_;

    /**
     * Constructor.
     *
     * @param  ticks  tick array
     * @param  orient   tick label orientation
     */
    public TickRun( Tick[] ticks, Orientation orient ) {
        ticks_ = ticks;
        orient_ = orient;
    }

    /**
     * Returns the tick array.
     *
     * @return  ticks
     */
    public Tick[] getTicks() {
        return ticks_;
    }
  
    /**
     * Returns the tick label orientation.
     *
     * @return  orientation
     */
    public Orientation getOrientation() {
        return orient_;
    }
}
