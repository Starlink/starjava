package uk.ac.starlink.registry;

/**     
 * Interface for receiving resources.
 *
 * @author   Mark Taylor
 */     
public interface ResourceSink<R> {
    
    /**
     * Accept a newly discovered resource.
     *
     * @param   resource resource
     */
    abstract void addResource( R resource );
}
