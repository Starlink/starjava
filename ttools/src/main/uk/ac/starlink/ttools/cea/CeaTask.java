package uk.ac.starlink.ttools.cea;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.task.OutputModeParameter;

/**
 * Represents a task suitable for use in the CEA interface of STILTS.
 * Corresponds, but perhaps not exactly, to a STILTS task.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2006
 */
public class CeaTask {

    private List paramList_;
    private final String name_;
    private final String purpose_;

    /**
     * Constructs a CeaTask.
     *
     * @param   task  ttools task object
     * @param   name  public name of the task
     */
    public CeaTask( Task task, String name ) {
        paramList_  = new ArrayList();
        Parameter[] params = task.getParameters();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];

            /* We're not going to try to muck about with output modes 
             * other than "out" for the purposes of CEA; other modes 
             * are generally intended for interactive/client-side use 
             * of one sort or another, and the business of dynamically
             * determining supplementary parameters would be extremely
             * messy within CEA. */
            if ( ! ( param instanceof OutputModeParameter ) ) {
                paramList_.add( new CeaParameter( param ) );
            }
        }
        name_ = name;
        purpose_ = task.getPurpose();
    }

    /**
     * Returns public task name.
     *
     * @return  task name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns task purpose.
     *
     * @return  task purpose
     */
    public String getPurpose() {
        return purpose_;
    }

    /**
     * Removes a named parameter from this task's parameter list.
     *
     * @param   name  name of the parameter to remove
     */
    public void removeParameter( String name ) {
        for ( Iterator it = paramList_.iterator(); it.hasNext(); ) {
            CeaParameter param = (CeaParameter) it.next();
            if ( name.equals( param.getName() ) ) {
                it.remove();
                return;
            }
        }
        throw new IllegalArgumentException( "No such parameter: " + name );
    }

    /**
     * Returns a named parameter of this task.
     *
     * @param   name of the parameter
     * @return  parameter
     */
    public CeaParameter getParameter( String name ) {
        for ( Iterator it = paramList_.iterator(); it.hasNext(); ) {
            CeaParameter param = (CeaParameter) it.next();
            if ( name.equals( param.getName() ) ) {
                return param;
            }
        }
        throw new IllegalArgumentException( "No such parameter: " + name );
    }

    /**
     * Returns a list of the parameters associated with this task for
     * the purposes of CEA use.
     *
     * @return   parameter list
     */
    public CeaParameter[] getParameters() {
        return (CeaParameter[]) paramList_.toArray( new CeaParameter[ 0 ] );
    }
}
