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
class CeaTask {

    private List paramList_;
    private final String name_;
    private final String description_;

    /**
     * Constructs a CeaTask.
     *
     * @param   task  ttools task object
     * @param   name  public name of the task
     * @param   description   shortish description of the task
     */
    public CeaTask( Task task, String name, String description ) {
        paramList_  = new ArrayList();
        Parameter[] params = task.getParameters();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            if ( ! ( param instanceof OutputModeParameter ) ) {
                paramList_.add( new CeaParameter( param ) );
            }
        }
        name_ = name;
        description_ = description;
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
     * Returns task description.
     *
     * @return  task description
     */
    public String getDescription() {
        return description_;
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
     * Returns a list of the parameters associated with this task for
     * the purposes of CEA use.
     *
     * @return   parameter list
     */
    public CeaParameter[] getParameters() {
        return (CeaParameter[]) paramList_.toArray( new CeaParameter[ 0 ] );
    }
}
