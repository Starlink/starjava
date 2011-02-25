package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.vo.TapQuery;

/**
 * Mapper that does the work for {@link TapQuerier}.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2011
 */
public class TapMapper implements TableMapper {

    private final URLParameter urlParam_;
    private final Parameter adqlParam_;
    private final TapResultReader resultReader_;
    private final Parameter[] params_;

    public TapMapper() {
        List<Parameter> paramList = new ArrayList<Parameter>();

        paramList.add( createUploadNameParameter( VariableTablesInput
                                                 .NUM_SUFFIX ) );

        urlParam_ = new URLParameter( "tapurl" );
        urlParam_.setPrompt( "Base URL of TAP service" );
        urlParam_.setDescription( new String[] {
            "<p>The base URL of a Table Access Protocol service.",
            "This is the bare URL without a trailing \"/async\".",
            "</p>",
        } );
        paramList.add( urlParam_ );

        adqlParam_ = new Parameter( "adql" );
        adqlParam_.setPrompt( "ADQL query text" );
        adqlParam_.setDescription( new String[] {
            "<p>Astronomical Data Query Language string specifying the",
            "TAP query to execute.",
            "ADQL/S resembles SQL, so this string will likely start with",
            "\"SELECT\".",
            "</p>",
        } );
        paramList.add( adqlParam_ );

        resultReader_ = new TapResultReader();
        paramList.addAll( Arrays.asList( resultReader_.getParameters() ) );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public TableMapping createMapping( Environment env, final int nup )
            throws TaskException {
        final URL url = urlParam_.urlValue( env );
        final String adql = adqlParam_.stringValue( env );
        final TapResultProducer resultProducer =
            resultReader_.createResultProducer( env );
        final boolean progress =
            resultReader_.getProgressParameter().booleanValue( env );
        final PrintStream errStream = env.getErrorStream();
        final String[] upnames = new String[ nup ];
        for ( int iu = 0; iu < nup; iu++ ) {
            upnames[ iu ] =
                createUploadNameParameter( Integer.toString( iu + 1 ) )
               .stringValue( env );
        }
        return new TableMapping() {
            public StarTable mapTables( InputTableSpec[] inSpecs )
                    throws TaskException, IOException {
                Map<String,StarTable> uploadMap =
                    new LinkedHashMap<String,StarTable>();
                for ( int iu = 0; iu < nup; iu++ ) {
                    uploadMap.put( upnames[ iu ],
                                   inSpecs[ iu ].getWrappedTable() );
                }
                if ( progress ) {
                    errStream.println( "SUBMITTED ..." );
                }
                TapQuery query =
                    TapQuery.createAdqlQuery( url, adql, uploadMap );
                if ( progress ) {
                    errStream.println( query.getUwsJob().getJobUrl() );
                }
                query.start();
                return resultProducer.waitForResult( query );
            }
        };
    }

    /**
     * Returns a parameter for acquiring the label under which one of the
     * uploaded tables should be presented to the TAP server.
     *
     * @param   label  parameter suffix 
     * @return   upload name parameter
     */
    private static Parameter createUploadNameParameter( String label ) {
        Parameter upnameParam = new Parameter( "upname" + label );
        upnameParam.setPrompt( "Label for uploaded table #" + label );
        upnameParam.setUsage( "<label>" );
        upnameParam.setDescription( new String[] {
            "<p>Identifier to use in server-side expressions for uploaded",
            "table #" + label + ".",
            "In ADQL expressions, the table should be referred to as",
            "\"<code>TAP_UPLOAD.&lt;label&gt;</code>\".",
            "</p>",
        } );
        upnameParam.setDefault( "up" + label );
        return upnameParam;
    }
}
