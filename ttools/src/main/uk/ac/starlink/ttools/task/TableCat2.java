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
public class TableCat2 extends FixedMapperTask {

    public TableCat2() {
        super( new Cat2Mapper(), 2, new ChoiceMode(), true, true );
    }

    private static class Cat2Mapper implements TableMapper {

        public Cat2Mapper() {
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
            consumers[ 0 ].consume( new ConcatStarTable( inTables[ 0 ],
                                                         inTables ) );
        }
    }
}
