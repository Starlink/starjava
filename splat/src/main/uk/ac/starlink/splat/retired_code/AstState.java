package uk.ac.starlink.splat.ast;

/**
 * Abstract base class for storing the state of an AST graphics item
 * and converting the current state into an AST option description.
 * It is expected that this class will be extended to provide concrete
 * implementations that describe the state of graphical items such as
 * a Plot title, X Axis label, the Tick Marks etc.
 *
 * @since $Date$
 * @since 01-NOV-2000
 * @version $Id$
 * @author Peter W. Draper
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
abstract public class AstState 
{
    /**
     * The name of the associated AST attribute.
     */
    protected String attribute = null;

    /**
     * The name of the attribute element (axis number etc.).
     */
    protected String element = null;

    /**
     * The "value" of the object.
     */
    protected Object value = null;

    /**
     * Create an instance.
     */
    public AstState( String attribute ) 
    {
        this.attribute = attribute;
    }

    /**
     * Create an instance.
     */
    public AstState( String attribute, String element ) 
    {
        this.attribute = attribute;
        this.element = element;
    }

    /**
     * Convert the current state into an AST options description.
     */
    public String getAsOption() 
    {
        if ( element == null ) {
            return attribute + "=" + value;
        } else {
            return attribute + "(" + element + ")=" + value;
        }
    }
}
