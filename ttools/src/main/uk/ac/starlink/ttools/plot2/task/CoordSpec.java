package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.table.DomainMapper;

/**
 * Characterises a coordinate specification in sufficient detail
 * to recreate it as part of a STILTS plotting command.
 *
 * @author   Mark Taylor
 * @since    8 May 2020
 */
public class CoordSpec {

    private final String inputName_;
    private final String valueExpr_;
    private final DomainMapper dm_;
    private final DomainMapper dfltDm_;

    /**
     * Constructor.
     *
     * @param   inputName  name of input quantity (coordinate name)
     * @param   valueExpr  expression giving the quantity's value,
     *                     to be evaluated in the context of the host table
     * @param   dm         domain mapper used to interpret the input value;
     *                     null if the quantity is not domain-sensitive
     * @param   dfltDm     domain mapper that would be used if none was given;
     *                     null if the quantity is not domain-sensitive
     *                     or if no obvious default suggests itself
     */
    public CoordSpec( String inputName, String valueExpr, DomainMapper dm,
                      DomainMapper dfltDm ) {
        inputName_ = inputName;
        valueExpr_ = valueExpr;
        dm_ = dm;
        dfltDm_ = dfltDm;
    }

    /**
     * Returns the name of the input quantity.
     *
     * @return  coordinate name
     */
    public String getInputName() {
        return inputName_;
    }

    /**
     * Returns the expression giving the quantity's value,
     * to be evaluated in the context of the host table.
     *
     * @return  value expression (column name or JEL expression)
     */
    public String getValueExpr() {
        return valueExpr_;
    }

    /**
     * Returns the domain mapper used to interpret the value,
     * or null if the quantity is not domain-sensitive.
     *
     * @return  domain mapper or null
     */
    public DomainMapper getDomainMapper() {
        return dm_;
    }

    /**
     * Returns the domain mapper that would be used if none was given,
     * or null if the quantity is not domain-sensitive
     * or if no obvious default suggests itself.
     *
     * @return  default domain mapper, or null
     */
    public DomainMapper getDefaultDomainMapper() {
        return dfltDm_;
    }
}
