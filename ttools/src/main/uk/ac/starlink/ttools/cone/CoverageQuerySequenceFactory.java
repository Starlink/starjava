package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Wrapper QuerySequenceFactory that filters out elements outside of
 * a given coverage object.
 * The {@link ConeQueryRowSequence#getIndex} values of this sequence
 * will be those of the underlying ConeQueryRowSequences, so will miss
 * out values for those elements that are excluded.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2014
 */
public class CoverageQuerySequenceFactory implements QuerySequenceFactory {

    private final QuerySequenceFactory baseFact_;
    private final Coverage coverage_;

    /**
     * Constructor.
     *
     * @param  baseFact  base QuerySequenceFactory
     * @param  coverage  coverage object
     */
    public CoverageQuerySequenceFactory( QuerySequenceFactory baseFact,
                                         Coverage coverage ) {
        baseFact_ = baseFact;
        coverage_ = coverage;
    }

    public ConeQueryRowSequence createQuerySequence( StarTable table )
            throws IOException {
        return new WrapperQuerySequence( baseFact_
                                        .createQuerySequence( table ) ) {
            @Override
            public boolean next() throws IOException {
                while ( super.next() ) {
                    if ( coverage_.discOverlaps( super.getRa(), super.getDec(),
                                                 super.getRadius() ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
