package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DataSource;

/**
 * A table builder which reads tables in simple text format.
 * The detailed format of input file which is understood is documented
 * fully in the {@link TextStarTable} class.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TextTableBuilder implements TableBuilder {

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc ) throws IOException {
        return new TextStarTable( datsrc.getInputStream() );
    }
}
