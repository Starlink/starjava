package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.util.IntList;

/**
 * Adapts a Pixellator to generate Pixer instances.
 * This is intended as a temporary measure; in due course all the
 * pixel iteration will be written as Pixer objects from scratch,
 * but this class is useful to adapt the functionality from the classic
 * plot classes.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2013
 */
public class PixellatorPixerFactory {

    private final int[] xs_;
    private final int[] ys_;
    private final int np_;
    private final int xmin_;
    private final int xmax_;
    private final int ymin_;
    private final int ymax_;

    /**
     * Constructor.
     *
     * @param  pixellator  pixellator
     */
    public PixellatorPixerFactory( Pixellator pixellator ) {
        IntList xlist = new IntList();
        IntList ylist = new IntList();
        int np = 0;
        for ( pixellator.start(); pixellator.next(); np++ ) {
            xlist.add( pixellator.getX() );
            ylist.add( pixellator.getY() );
        }
        xs_ = xlist.toIntArray();
        ys_ = ylist.toIntArray();
        np_ = np;
        Rectangle bounds = pixellator.getBounds();
        xmin_ = bounds.x;
        xmax_ = bounds.x + bounds.width;
        ymin_ = bounds.y;
        ymax_ = bounds.y + bounds.height;
    }

    /**
     * Returns a pixel iterator for this factory intersected with a
     * given clip region.  May return null in the case of no intersection.
     *
     * @param  clip   clip region
     * @return  new pixel iterator, or null
     */
    public Pixer createPixer( Rectangle clip ) {
        return Pixers.clip( Pixers.createArrayPixer( xs_, ys_, np_ ), clip,
                            xmin_, xmax_, ymin_, ymax_ );
    }
}
