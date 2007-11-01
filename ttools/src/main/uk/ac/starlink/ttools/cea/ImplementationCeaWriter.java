package uk.ac.starlink.ttools.cea;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.SAXException;
import uk.ac.starlink.ttools.Stilts;

/**
 * CeaWriter implementation which writes an XML document giving 
 * CEA Implementation instance.  This is what goes in an
 * <code>app-description.xml</code> file local to a STILTS CEA installation
 * (I think).
 *
 * @author   Mark Taylor
 * @since    1 Nov 2007
 */
public class ImplementationCeaWriter extends CeaWriter {

    private static final String AG_SCHEMA_BASE =
        "http://www.astrogrid.org/schema/";
    private static final String CEAI_NS =
        AG_SCHEMA_BASE + "CEAImplementation/v1";
    private static final String CEAB_NS =
        AG_SCHEMA_BASE + "CommonExecutionArchitectureBase/v1";
    private static final String AGPD_NS =
        AG_SCHEMA_BASE + "AGParameterDefinition/v1";

    public static final String SCHEMA_LOCATION =
        "http://software.astrogrid.org/schema/cea/CEAImplementation/v1.0/"
      + "CEAImplementation.xsd";
    public static final String APPLICATION_ID = "ivo://uk.ac.starlink/stilts";

    private String appPath_;
    private final CeaTask[] tasks_;

    /**
     * Constructor.
     *
     * @param  out  output stream for XML
     * @param  tasks  list of tasks to be described by the output
     * @param  cmdline  command line string, used for logging within the
     *                  output only
     */
    public ImplementationCeaWriter( PrintStream out, CeaTask[] tasks,
                                    String cmdline ) {
        super( out, createImplementationConfig(), tasks, cmdline );
        tasks_ = tasks;
    }

    public String getSchemaLocation() {
        return SCHEMA_LOCATION;
    }

    public int configure( String usageBase, String[] args ) {
        String usage = usageBase
                     + " -path <stilts-path>"
                     + "\n";
        List argList = new ArrayList( Arrays.asList( args ) );
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( "-path".equals( arg ) && it.hasNext() && appPath_ == null ) {
                it.remove();
                appPath_ = (String) it.next();
                it.remove();
            }
        }
        if ( ! argList.isEmpty() || appPath_ == null ) {
            System.err.println( usage );
            return 1;
        }
        return 0;
    }

    protected void writeContent() throws SAXException {

        /* Application description. */
        startElement( "CommandLineExecutionControllerConfig",
              formatAttribute( "xmlns", CEAI_NS )
            + formatAttribute( "xmlns:ceai", CEAI_NS )
            + formatAttribute( "xmlns:ceab", CEAB_NS )
            + formatAttribute( "xmlns:agpd", AGPD_NS )
            + formatAttribute( "xmlns:xsi",
                               "http://www.w3.org/2001/XMLSchema-instance" )
            + formatAttribute( "xsi:schemaLocation",
                               CEAI_NS +
                               " ./src/workflow-objects/schema/" +
                               "CEAImplementation.xsd" ) );
        println( getIndent( getLevel() )
               + "<!-- Application name set from " + getClass().getName()
               + " command-line flag. -->" );
        startElement( "Application",
                      formatAttribute( "name", APPLICATION_ID )
                    + formatAttribute( "version", Stilts.getVersion() ) );

        /* Main task and parameter definitions. */
        writeParameters();
        writeInterfaces();

        /* Write the location of the executable. */
        println( getIndent( getLevel() )
               + "<!-- ExecutionPath set from " + getClass().getName()
               + " command-line flag. -->" );
        addElement( "ExecutionPath", "", appPath_.toString() );

        /* Write description matter.  This includes short text and a link
         * for each of the separate tasks (CEA interfaces). */
        addElement( "LongName", "",
                    "STILTS - "
                  + "Starlink Tables Infrastructure Library Tool Set" );
        addElement( "Version", "", Stilts.getVersion() );
        startElement( "Description" );
        println( "STILTS is a package which provides a number of table " +
                 "manipulation functions." );
        println( "The following tasks (profiles) are provided: " );
        for ( int i = 0; i < tasks_.length; i++ ) {
            CeaTask task = tasks_[ i ];
            println( "   " + task.getName() + ":" );
            println( "      " + task.getPurpose() );
        }
        endElement( "Description" );
        addElement( "ReferenceURL", "", getManualURL() );

        /* Outro. */
        endElement( "Application" );
        endElement( "CommandLineExecutionControllerConfig" );
    }

    /**
     * Constructs a CeaConfig object which encapsulates requirements
     * specific to this CeaWriter implementation.
     *
     * @return   suitable CeaConfig object
     */
    private static CeaConfig createImplementationConfig() {
        ElementDeclaration ifsDecl =
             ElementDeclaration
            .createNamespaceElement( "Interfaces", CEAB_NS );
        ElementDeclaration paramsDecl =
             new ElementDeclaration( "ceab:Parameters" );
        ElementDeclaration paramDecl =
            ElementDeclaration
           .createNamespaceElement( "ceai:CmdLineParameterDefn", AGPD_NS );
        return new CeaConfig( ifsDecl, paramsDecl, paramDecl );
    }
}
