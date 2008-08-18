package uk.ac.starlink.ttools.plot;

import java.awt.Rectangle;
import java.util.BitSet;

/**
 * Pixellator built on a bit vector.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2007
 */
public class BitSetPixellator implements Pixellator {

    private final Rectangle bounds_;
    private final BitSet mask_;
    private int iNext_ = -2;
    private int x_;
    private int y_;

    /**
     * Constructor.
     * Point <code>(x,y)</code> of the pixellator is represented by mask index
     * <code>(x-bounds.x) + bounds.width * (y-bounds.y)</code>.
     *
     * @param   bounds  bounds of this pixellator
     * @param   mask    bit vector with a point set for each filled in pixel
     */
    public BitSetPixellator( Rectangle bounds, BitSet mask ) {
        bounds_ = bounds;
        mask_ = mask;
    }

    public Rectangle getBounds() {
        return new Rectangle( bounds_ );
    }

    public void start() {
        iNext_ = -1;
    }

    public boolean next() {
        int in = mask_.nextSetBit( iNext_ + 1 );
        if ( in >= 0 ) {
            x_ = in % bounds_.width + bounds_.x;
            y_ = in / bounds_.width + bounds_.y;
            iNext_ = in;
            return true;
        }
        else {
            x_ = Integer.MIN_VALUE;
            y_ = Integer.MIN_VALUE;
            return false;
        }
    }

    public int getX() {
        return x_;
    }

    public int getY() {
        return y_;
    }

    /**
     * Creates a new pixellator with the same content as an existing one.
     * The bounds of the new one are derived from the actual limits of
     * the pixels in the input, so may differ from those reported by the
     * <code>getBounds</code> method of the input.
     *
     * @param   pixer   pixellator to copy
     * @return  clone
     */
    public static BitSetPixellator copy( Pixellator pixer ) {
        Rectangle bounds = pixer.getBounds();
        BitSet bitmap = new BitSet( bounds.width * bounds.height );
        for ( pixer.start(); pixer.next(); ) {
            int x = pixer.getX();
            int y = pixer.getY();
            bitmap.set( ( x - bounds.x ) + bounds.width * ( y - bounds.y ) );
        }
        return new BitSetPixellator( bounds, bitmap );
    }
}
