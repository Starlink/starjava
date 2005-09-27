package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Concatenates two tables.  Currently very rudimentary.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2005
 */
public class TableCat extends MapperTask {

    public TableCat() {
        super( new Cat2Mapper(), false );
    }

    private static class Cat2Mapper implements TableMapper {

        public Cat2Mapper() {
        }

        public int getInCount() {
            return 2;
        }

        public Parameter[] getParameters() {
            return new Parameter[ 0 ];
        }

        public TableMapping createMapping( Environment env ) {
            return new Cat2Mapping();
        }
    }

    private static class Cat2Mapping implements TableMapping {
        public void mapTables( StarTable[] inTables,
                               TableConsumer[] consumers ) throws IOException {
            consumers[ 0 ].consume( new ConcatStarTable( inTables ) );
        }
    }
}
