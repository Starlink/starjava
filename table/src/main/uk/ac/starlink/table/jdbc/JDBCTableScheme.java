package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableScheme;

/**
 * TableScheme for intepreting JDBC-type URLs.
 * The full specification ("<code>jdbc:...</code>" - no leading colon)
 * is a <em>JDBC URL</em> as used by the JDBC system.
 *
 * <p>For historical reasons, the JDBCAuthenticator used by this
 * scheme is managed by an associated StarTableFactory,
 * which must be supplied at construction time.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2020
 */
public class JDBCTableScheme implements TableScheme, Documented {

    private final StarTableFactory tfact_;
    private final boolean wantRandom_;

    /**
     * Constructor.
     *
     * @param  tfact  table factory
     */
    public JDBCTableScheme( StarTableFactory tfact ) {
        tfact_ = tfact;
        wantRandom_ = false;
    }

    public String getSchemeName() {
        return "jdbc";
    }

    public String getSchemeUsage() {
        return "<jdbc-part>";
    }

    public String getExampleSpecification() {
        return null;
    }

    public String getXmlDescription() {
        String exampleUrl = 
            "jdbc:mysql://localhost/dbl#SELECT TOP 10 ra, dec FROM gsc";
        return String.join( "\n",
            "<p>Interacts with the JDBC system",
            "(JDBC sort-of stands for Java DataBase Connectivity)",
            "to execute an SQL query on a connected database.",
            "The <code>jdbc:...</code> specification is the JDBC URL.",
            "For historical compatibility reasons,",
            "specifications of this scheme",
            "may omit the leading colon character,",
            "so that the following are both legal, and are equivalent:",
            "<pre>",
            "   " + exampleUrl,
            "   :" + exampleUrl,
            "</pre>",
            "</p>",
            "<p>In order for this to work, you must have access to",
            "a suitable database with a JDBC driver,",
            "and some standard JDBC configuration",
            "is required to set the driver up.",
            "The following steps are necessary:",
            "<ol>",
            "<li>the driver class must be available on the runtime classpath",
            "    </li>",
            "<li>the <code>jdbc.drivers</code> system property must be set",
            "    to the driver classname",
            "    </li>",
            "</ol>",
            "</p>",
            "<p>More detailed information about how to set up the JDBC system",
            "to connect with an available database,",
            "and of how to construct JDBC URLs,",
            "is provided elsewhere in the documentation.",
            "</p>",
        "" );
    }

    public StarTable createTable( String spec ) throws IOException {
        String jdbcUrl = "jdbc:" + spec;
        return tfact_.getJDBCHandler().makeStarTable( jdbcUrl, wantRandom_ );
    }
}
