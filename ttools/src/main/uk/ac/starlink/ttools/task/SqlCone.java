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
        super( "Crossmatch between local table and table in SQL database",
               new JdbcConer() );
    }
}
