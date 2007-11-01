package uk.ac.starlink.ttools.cea;

/**
 * Class which defines schema-specific details of a CEA configuration file
 * to be written by a {@link CeaWriter} object.
 *
 * @author   Mark Taylor
 * @since    1 Nov 2007
 */
public class CeaConfig {

    private final ElementDeclaration interfacesEl_;
    private final ElementDeclaration parametersEl_;
    private final ElementDeclaration parameterEl_;

    /**
     * Constructor.
     *
     * @param  interfacesEl  declaration for Interfaces element
     * @param  parametersEl  declaration for Parameters element
     * @param  parameterEl   declaration for Parameter element
     */
    public CeaConfig( ElementDeclaration interfacesEl,
                      ElementDeclaration parametersEl,
                      ElementDeclaration parameterEl ) {
        interfacesEl_ = interfacesEl;
        parametersEl_ = parametersEl;
        parameterEl_ = parameterEl;
    }

    /**
     * Returns declaration to use for the Interfaces element.
     *
     * @return  element
     */
    public ElementDeclaration getInterfacesElement() {
        return interfacesEl_;
    }

    /**
     * Returns declaration to use for the Parameters element.
     *
     * @return  element
     */
    public ElementDeclaration getParametersElement() {
        return parametersEl_;
    }

    /**
     * Returns declaration to use for the Parameter element.
     *
     * @return  element
     */
    public ElementDeclaration getParameterElement() {
        return parameterEl_;
    }
}
