package uk.ac.starlink.ttools.jel;

import gnu.jel.CompiledExpression;
import uk.ac.starlink.table.ValueInfo;

/**
 * Aggregates a compiled expression and a metadata object.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2016
 */
public interface JELQuantity {

    /**
     * Returns the string expression from which this quantity was created.
     *
     * @return  original expression string
     */
    String getExpression();

    /**
     * Returns the compiled expression from which this quantity can be
     * evaluated.
     *
     * @return  compiled expression
     */
    CompiledExpression getCompiledExpression();

    /**
     * Returns a metadata object that describes this quantity.
     * It should have at least a name and a data type.
     * It may have other information like UCDs and descriptions if
     * they are available or can be determined.
     *
     * @return  metadata for quantity
     */
    ValueInfo getValueInfo();
}
