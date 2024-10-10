package uk.ac.starlink.ttools.task;

import java.util.logging.Logger;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.cone.ConeSearchConer;
import uk.ac.starlink.ttools.cone.ParallelResultRowSequence;
import uk.ac.starlink.ttools.cone.SkyConeMatch2;

/**
 * SkyConeMatch2 implementation which uses an external Cone Search service.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class MultiCone extends SkyConeMatch2 {
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public MultiCone() {
        super( "Crossmatches table on sky position against remote cone service",
               new ConeSearchConer(), 
               ParallelResultRowSequence.getMaxParallelism() );
    }

    @Override
    public ConeSearchConer getConer() {
        return (ConeSearchConer) super.getConer();
    }

    @Override
    public Executable createExecutable( Environment env ) throws TaskException {
        logger_.warning( "Multi-Cone is somewhat deprecated in favour of "
                       + "TAP or CDS X-Match" );
        logger_.warning( "There is probably a much faster way to do this" );
        return super.createExecutable( env );
    }
}
