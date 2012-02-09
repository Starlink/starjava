package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.cone.JdbcConer;
import uk.ac.starlink.ttools.cone.SkyConeMatch2;

/**
 * Multiple cone match task which works by doing SQL queries using JDBC.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2007
 */
public class SqlCone extends SkyConeMatch2 {
    public SqlCone() {

        /* Note that parallelism is not permitted.  Not only is it doubtful
         * whether it would provide advantages, but the JdbcConer 
         * implementation uses a single JDBC connection for all queries,
         * and this cannot be used to make multiple queries at once. */
        super( "Crossmatches table on sky position against SQL table",
               new JdbcConer(), false, 0 );
    }
}
