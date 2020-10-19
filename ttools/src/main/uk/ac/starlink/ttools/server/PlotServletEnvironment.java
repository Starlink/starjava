package uk.ac.starlink.ttools.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.task.DataStoreParameter;
import uk.ac.starlink.ttools.plot2.task.PlotConfiguration;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.ttools.task.TableEnvironment;
import uk.ac.starlink.util.Pair;

/**
 * Execution environment for use with PlotServlet.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2020
 */
public class PlotServletEnvironment implements TableEnvironment {

    private final Map<String,List<String>> paramMap_;
    private final StarTableFactory tableFactory_;
    private final StarTableOutput tableOutput_;
    private final JDBCAuthenticator jdbcAuth_;
    private final DataStoreFactory dataStoreFactory_;
    private final PrintStream outStream_;
    private final ServerPainter painter_;
    private boolean strictVot_;
    private boolean debug_;
    private GraphicExporter exporter_;

    /**
     * Constructor.
     *
     * @param   response  response for servlet on behalf of which this
     *                    environment is operating
     * @param   nvPairs   ordered set of name-value pairs giving
     *                    general environment parameter value settings
     * @param   tableFactory  table factory
     * @param   tableOutput   table outputter
     * @param   jdbcAuth   JDBC authentication
     * @param   dataStoreFactory   generates DataStores
     */
    public PlotServletEnvironment( HttpServletResponse response,
                                   List<Pair<String>> nvPairs,
                                   StarTableFactory tableFactory,
                                   StarTableOutput tableOutput,
                                   JDBCAuthenticator jdbcAuth,
                                   DataStoreFactory dataStoreFactory )
            throws IOException {
        outStream_ = new PrintStream( response.getOutputStream() );
        tableFactory_ = tableFactory;
        tableOutput_ = tableOutput;
        jdbcAuth_ = jdbcAuth;
        dataStoreFactory_ = dataStoreFactory;
        painter_ = new ServerPainter();
        paramMap_ = new LinkedHashMap<>();
        for ( Pair<String> pair : nvPairs ) {
            String key = LineTableEnvironment.normaliseName( pair.getItem1() );
            String value = pair.getItem2();
            paramMap_.computeIfAbsent( key, k -> new ArrayList<String>() )
                     .add( value );
        }
    }

    public String[] getNames() {
        return paramMap_.keySet().toArray( new String[ 0 ] );
    }

    public void acquireValue( Parameter<?> param ) throws TaskException {

        /* Special behaviour for the paint mode.  Arrange for painted
         * output to get sent to the HTTP response object rather than
         * being sent to stdout. */
        if ( param instanceof PaintModeParameter ) {
            PaintModeParameter pmParam = (PaintModeParameter) param;
            ChoiceParameter<GraphicExporter> formatParam =
                pmParam.getFormatParameter();
            formatParam.setStringDefault( "jpeg" );
            exporter_ = formatParam.objectValue( this );
            pmParam.setValueFromPainter( this, painter_ );
        }

        /* Make sure we are using an appropriate storage option. */
        else if ( param instanceof DataStoreParameter ) {
            DataStoreParameter storageParam = (DataStoreParameter) param;
            storageParam.setValueFromObject( this, dataStoreFactory_ );
        }

        /* Otherwise use the supplied name-value pairs. */
        else {
            String pname =
                LineTableEnvironment.normaliseName( param.getName() );
            List<String> valList = paramMap_.get( pname );
            int nval = valList == null ? 0 : valList.size();
            final String stringVal;

            /* No value supplied: use parameter default. */
            if ( nval == 0 ) {
                stringVal = param.getStringDefault();
            }

            /* Multiple-valued parameter: concatenate different values. */
            else if ( param instanceof MultiParameter ) {
                char sep = ((MultiParameter) param).getValueSeparator();
                StringBuffer sbuf = new StringBuffer();
                for ( int iv = 0; iv < nval; iv++ ) {
                    if ( iv > 0 ) {
                        sbuf.append( sep );
                    }
                    sbuf.append( valList.get( iv ) );
                }
                stringVal = sbuf.toString();
            }

            /* Single-valued parameter: be lenient here, and permit multiple
             * values if only one of them is non-null; this may facilitate
             * form processing in some cases (SELECT or TEXT form element). */
            else {
                if ( nval > 1 ) {
                    int nv = 0;
                    String val = null;
                    for ( String value : valList ) {
                        if ( value != null && value.length() > 0 ) {
                            nv++;
                            val = value;
                        }
                    }
                    if ( nv == 0 ) {
                        stringVal = null;
                    }
                    else if ( nv == 1 ) {
                        stringVal = val;
                    }
                    else {
                        throw new ParameterValueException(
                            param,
                            "Multiple values supplied for " +
                            "single-valued  parameter" );
                    }
                }
                else {
                    stringVal = valList.get( 0 );
                }
            }

            /* We have the string value, now process it. */
            if ( stringVal == null || stringVal.length() == 0 ) {
                if ( param.isNullPermitted() ) {
                    param.setValueFromString( this, null );
                }
                else {
                    throw new ParameterValueException(
                                  param, "null value not allowed" );
                }
            }
            else {
                param.setValueFromString( this, stringVal );
            }
        }
    }

    /**
     * Returns the plot configuration.
     *
     * @return  config
     */
    public PlotConfiguration<?,?> getPlotConfiguration() {
        return painter_.getPlotConfiguration();
    }

    /**
     * Returns the graphic format identifier.
     *
     * @return  exporter
     */
    public GraphicExporter getGraphicExporter() {
        return exporter_;
    }

    public void clearValue( Parameter<?> param ) {
        synchronized ( paramMap_ ) {
            paramMap_.remove( LineTableEnvironment
                             .normaliseName( param.getName() ) );
        }
    }

    public PrintStream getOutputStream() {
        return outStream_;
    }

    public PrintStream getErrorStream() {
        return outStream_;
    }

    public StarTableFactory getTableFactory() {
        return tableFactory_;
    }

    public StarTableOutput getTableOutput() {
        return tableOutput_;
    }

    public JDBCAuthenticator getJdbcAuthenticator() {
        return jdbcAuth_;
    }

    public boolean isDebug() {
        return debug_;
    }

    public void setDebug( boolean debug ) {
        debug_ = debug;
    }

    public boolean isStrictVotable() {
        return strictVot_;
    }

    public void setStrictVotable( boolean strictVot ) {
        strictVot_ = strictVot;
    }
}
