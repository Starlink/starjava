package uk.ac.starlink.vo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Enum for categorising the stage of UWS processing.
 * This effectively subdivides the various phases defined by UWS.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2011
 */
public enum UwsStage {

    /** Job will not complete of its own accord; need to post RUN phase. */
    UNSTARTED( new String[] { "PENDING", "HELD", } ),

    /** Job is progressing; should reach FINISHED stage eventually. */
    RUNNING( new String[] { "QUEUED", "EXECUTING", "SUSPENDED", } ),

    /** Job has finished successfully or otherwise; will not progress further.*/
    FINISHED( new String[] { "COMPLETED", "ERROR", "ABORTED", } ),

    /** UNKNOWN phase; may change in future. */
    UNKNOWN( new String[] { "UNKNOWN", } ),

    /** Not a defined phase string. */
    ILLEGAL( new String[ 0 ] );

    private final Collection<String> phaseList_;

    /**
     * Constructor.
     *
     * @param   phases  UWS phases corresponding to this stage
     */
    private UwsStage( String[] phases ) {
        phaseList_ = Collections
                    .unmodifiableSet( new LinkedHashSet( Arrays
                                                        .asList( phases ) ) );
    }

    /**
     * Returns the phases which correspond to this stage.
     *
     * @return  unmodifiable collection of defined phase strings
     */
    public Collection<String> getPhaseList() {
        return phaseList_;
    }

    /**
     * Returns the stage corresponding to a given UWS phase.
     * If the given phase is not one defined by UWS, ILLEGAL is returned.
     *
     * @param  phase   UWS phase string
     * @return   corresponding stage
     */
    public static UwsStage forPhase( String phase ) {
        UwsStage[] stages = UwsStage.values();
        for ( int i = 0; i < stages.length; i++ ) {
            if ( stages[ i ].phaseList_.contains( phase ) ) {
                return stages[ i ];
            }
        }
        return ILLEGAL;
    }
}
