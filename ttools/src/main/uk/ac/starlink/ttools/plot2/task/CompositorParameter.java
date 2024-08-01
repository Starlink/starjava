package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.paper.Compositor;

/**
 * Parameter for selecting a Compositor.
 * Currently only allows selection by string value of Boost compostors.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2014
 */
public class CompositorParameter extends Parameter<Compositor> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    @SuppressWarnings("this-escape")
    public CompositorParameter( String name ) {
        super( name, Compositor.class, true );
        setUsage( "0..1" );
        setPrompt( "Faint pixel boost value" );
        setDescription( new String[] {
            "<p>Defines how multiple overplotted partially transparent pixels",
            "are combined to form a resulting colour.",
            "The way this is used depends on the details of",
            "the specified plot.",
            "</p>",
            "<p>Currently, this parameter takes a \"boost\" value",
            "in the range 0..1.",
            "If the value is zero, saturation semantics are used:",
            "RGB colours are added in proporition",
            "to their associated alpha value until the total alpha",
            "is saturated (reaches 1), after which additional pixels",
            "have no further effect.",
            "For larger boost values, the effect is similar,",
            "but any non-zero alpha in the output is boosted to the",
            "given minimum value.",
            "The effect of this is that even very slightly populated pixels",
            "can be visually distinguished from unpopulated ones",
            "which may not be the case for saturation composition.",
            "</p>",
        } );
        setStringDefault( "0.05" );
        setNullPermitted( false );
    }

    public Compositor stringToObject( Environment env, String sval )
            throws TaskException {
        final float boost;
        try {
            boost = Float.parseFloat( sval.trim() );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, "Not a numeric value" );
        }
        if ( ! ( boost >= 0 && boost <= 1 ) ) {
            throw new ParameterValueException( this, "Out of range 0..1" );
        }
        else {
            return boost == 0 ? Compositor.SATURATION
                              : new Compositor.BoostCompositor( boost );
        }
    }

    public String objectToString( Environment env, Compositor compositor ) {
        if ( compositor == Compositor.SATURATION ) {
            return "0";
        }
        else if ( compositor instanceof Compositor.BoostCompositor ) {
            float boost = ((Compositor.BoostCompositor) compositor).getBoost();
            return Float.toString( boost );
        }
        else {
            return compositor.getClass().getName();
        }
    }
}
