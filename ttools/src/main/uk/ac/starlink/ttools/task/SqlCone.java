package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.cone.JdbcConer;
import uk.ac.starlink.ttools.cone.SkyConeMatch2;

public class SqlCone extends SkyConeMatch2 {
    public SqlCone() {
        super( "Crossmatch between local table and table in SQL database",
               new JdbcConer() );
    }
}
