package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.WrapperRowSequence;

/**
 * ConeQueryRowSequence implementation which delegates all methods to
 * a supplied instance.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public class WrapperQuerySequence extends WrapperRowSequence
                                  implements ConeQueryRowSequence {
    private final ConeQueryRowSequence base_;

    /**
     * Constructor.
     *
     * @param  base  base sequence
     */
    public WrapperQuerySequence( ConeQueryRowSequence base ) {
        super( base );
        base_ = base;
    }

    public double getRa() throws IOException {
        return base_.getRa();
    }

    public double getDec() throws IOException {
        return base_.getDec();
    }

    public double getRadius() throws IOException {
        return base_.getRadius();
    }

    public long getIndex() throws IOException {
        return base_.getIndex();
    }
}
