package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.StatsFilter;
import uk.ac.starlink.ttools.plot2.layer.Combiner;

/**
 * Parameter for specifying Combiner values.
 * As well as the static options defined in the Combiner class,
 * the "Q.nnn" syntax for specifying arbitrary quantiles is supported.
 *
 * @author   Mark Taylor
 * @since    16 May 2024
 */
public class CombinerParameter extends ChoiceParameter<Combiner> {

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public CombinerParameter( String name ) {
        super( name, Combiner.getKnownCombiners() );
        setPrompt( "Combination method" );
    }

    @Override
    public Combiner stringToObject( Environment env, String sval )
            throws TaskException {
        double quant = StatsFilter.parseQuantileSpecifier( sval );
        return Double.isNaN( quant )
             ? super.stringToObject( env, sval )
             : Combiner.createQuantileCombiner( sval, null, quant );
    }

    /**
     * Returns an XML element listing the possible options
     * for this parameter with their descriptions.
     *
     * @return  options description in as a &lt;ul&gt; element
     */
    public String getOptionsDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<ul>\n" );
        for ( Combiner combiner : getOptions() ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( combiner.getName() )
                .append( "</code>: " )
                .append( combiner.getDescription() )
                .append( "</li>\n" );
        }
        sbuf.append( "<li><code>Q.nnn</code>: " )
            .append( "quantile nnn (e.g. Q.05 is the fifth percentile)" )
            .append( "</li>\n" );
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }
}
