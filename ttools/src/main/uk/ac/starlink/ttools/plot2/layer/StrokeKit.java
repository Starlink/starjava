package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Stroke;

/**
 * Supplier for drawing strokes.
 * A consistent set of strokes with different detail characteristics
 * can be supplied.
 *
 * @author  Mark Taylor
 * @since   30 Sep 2021
 */
public class StrokeKit {

    private final Stroke round_;
    private final Stroke butt_;

    /** Single pixel thickness instance. */
    public static final StrokeKit DEFAULT = new StrokeKit( 1f );

    /**
     * Constructs a standard StrokeKit for a given line thickness.
     *
     * @param  strokeSize  line thickness in pixels
     */
    public StrokeKit( float strokeSize ) {
        this( new BasicStroke( strokeSize, BasicStroke.CAP_ROUND,
                               BasicStroke.JOIN_MITER ),
              new BasicStroke( strokeSize, BasicStroke.CAP_BUTT,
                               BasicStroke.JOIN_MITER ) );
    }

    /**
     * Constructs a StrokeKit given strokes.
     *
     * @param  round  stroke to use for rounded ends
     * @param  butt   stroke to use for square ends
     */
    public StrokeKit( Stroke round, Stroke butt ) {
        round_ = round;
        butt_ = butt;
    }

    /**
     * Returns a stroke with rounded ends.
     *
     * @return  stroke
     */
    public Stroke getRound() {
        return round_;
    }

    /**
     * Returns a stroke with butted ends.
     *
     * @return  stroke
     */
    public Stroke getButt() {
        return butt_;
    }
}
