package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * TableMapper which concatenates tables top to bottom.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public class CatMapper implements TableMapper {

    public Parameter[] getParameters() {
        return new Parameter[ 0 ];
    }

    public TableMapping createMapping( Environment env ) {
        return new TableMapping() {
            public void mapTables( StarTable[] inTables,
                                   TableConsumer[] consumers )
                    throws IOException {
                consumers[ 0 ].consume( new ConcatStarTable( inTables[ 0 ],
                                                             inTables ) );
            }
        };
     }
}
