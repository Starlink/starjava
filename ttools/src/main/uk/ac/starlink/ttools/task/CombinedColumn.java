package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.task.StringMultiParameter;

/**
 * Describes an output column formed by aggregation of values from an
 * input expression.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2021
 */
public class CombinedColumn {

    private final String expr_;
    private final Combiner combiner_;
    private final String name_;

    /**
     * Constructor.
     *
     * @param   expr   expression to be aggregated
     * @param   combiner  aggregation method, may be null to indicate default
     * @param   name   name of output column, may be null to indicate default
     */
    public CombinedColumn( String expr, Combiner combiner, String name ) {
        expr_ = expr;
        combiner_ = combiner;
        name_ = name;
    }

    /**
     * Returns the expression to be aggregated.
     *
     * @return   input column name or expression, not null
     */
    public String getExpression() {
        return expr_;
    }

    /**
     * Returns the aggregation method.
     *
     * @return  aggregation method, or null to indicate default
     */
    public Combiner getCombiner() {
        return combiner_;
    }

    /**
     * Returns the output column name.
     *
     * @return   output name, or null to indicate default
     */
    public String getName() {
        return name_;
    }

    /**
     * Parses an input expression to a CombinedColumn value.
     *
     * @param  txt  input expression
     * @param  txtParam   parameter supplying input value,
     *                    used for reference in thrown exceptions
     * @param  combinerParam  parameter supplying combiner values,
     *                        used to decode combiner specifications
     */
    public static CombinedColumn
            parseSpecification( String txt, Parameter<?> txtParam,
                                ChoiceParameter<Combiner> combinerParam )
            throws ParameterValueException {
        String[] fields = txt.split( ";", 3 );
        int nf = fields.length;
        assert nf > 0 && nf <= 3;
        String expr = fields[ 0 ];
        final Combiner combiner;
        if ( nf > 1 && fields[ 1 ].trim().length() > 0 ) {
            combiner = combinerParam.getOption( fields[ 1 ] );
            if ( combiner == null ) {
                throw new ParameterValueException( txtParam,
                                                   "No such combination method"
                                                 + " \"" + fields[ 1 ] + "\"" );
            }
        }
        else {
            combiner = null;
        }
        final String name = nf > 2 && fields[ 2 ].trim().length() > 0
                          ? fields[ 2 ]
                          : null;
        return new CombinedColumn( expr, combiner, name );
    }

    /**
     * Creates a parameter for specifying aggregate columns.
     * The parameter return value is an array of strings,
     * each of which can be parsed using the
     * {@link #parseSpecification parseSpecification} method.
     *
     * @param  name  parameter name
     * @param  dfltCombinerParam   parameter used for specifying default
     *                             aggregation method, used in documentation
     */
    public static StringMultiParameter createCombinedColumnsParameter(
                String name, Parameter<Combiner> dfltCombinerParam ) {
        StringMultiParameter param = new StringMultiParameter( name, ' ' );
        param.setPrompt( "Aggregate column definitions" );
        param.setUsage( "<expr>[;<combiner>[;<name>]] ..." );
        param.setDescription( new String[] {
            "<p>Defines the quantities to be calculated.",
            "The value is a space-separated list of items,",
            "one for each aggregated column in the output table.",
            "</p>",
            "<p>Each item is composed of one, two or three tokens,",
            "separated by semicolon (\"<code>;</code>\") characters:",
            "<ul>",
            "<li><code>&lt;expr&gt;</code>: <em>(required)</em>",
            "    column name or expression using the",
            "    <ref id='jel'>expression language</ref>",
            "    for the quantity to be aggregated.",
            "    </li>",
            "<li><code>&lt;combiner&gt;</code>: <em>(optional)</em>",
            "    combination method, using the same options as for the",
            "    <code>" + dfltCombinerParam.getName() + "</code> parameter.",
            "    If omitted, the value specified for that parameter",
            "    will be used.",
            "    </li>",
            "<li><code>&lt;name&gt;</code>: <em>(optional)</em>",
            "    name of output column; if omitted,",
            "    the <code>&lt;expr&gt;</code> value",
            "    (perhaps somewhat sanitised)",
            "    will be used.",
            "    </li>",
            "</ul>",
            "It is often sufficient just to supply a space-separated list",
            "of input table column names for this parameter,",
            "but the additional syntax may be required for instance if",
            "it's required to calculate both a sum and mean",
            "of the same input column.",
            "</p>",
        } );
        param.setNullPermitted( true );
        return param;
    }
}
