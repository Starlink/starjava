package uk.ac.starlink.mirage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;

public class MirageTableWriter implements StarTableWriter {

    public void writeStarTable( StarTable startab, String location )
            throws IOException {
        if ( location.equals( "-" ) ) {
            MirageFormatter mf = new MirageFormatter( System.out );
            mf.writeMirageFormat( startab );
        }
        else {
            File file = new File( location );
            OutputStream ostrm = new FileOutputStream( file );
            PrintStream pstrm = new PrintStream( ostrm );
            MirageFormatter mf = new MirageFormatter( pstrm );
            mf.writeMirageFormat( startab );
            pstrm.close();
        }
    }

    public boolean looksLikeFile( String filename ) {
        return filename.endsWith( ".mirage" );
    }

    public String getFormatName() {
        return "mirage";
    }
}
