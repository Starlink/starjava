package uk.ac.starlink.topcat.activate;

/**
 * Defines a type of activation action that can be invoked on table rows.
 * An instance of this class documents its general behaviour,
 * and can produce a GUI component that acts as a factory for
 * Activator instances.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public interface ActivationType {

    /**
     * Name of this activation type.
     *
     * @return  type name
     */
    String getName();

    /**
     * Description of this activation type.
     *
     * @return   type description
     */
    String getDescription();

    /**
     * Returns a component that can be used to configure activators of
     * this type.
     *
     * @param  tinfo  information about topcat model for which the
     *                activation will take place
     * @return   new configurator to produce compatible Activators
     */
    ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo );

    /**
     * Indicates the applicability of this activation type to a given table.
     *
     * @param  tinfo  information about topcat model
     * @return   suitability code
     */
    Suitability getSuitability( TopcatModelInfo tinfo );
}
