package uk.ac.starlink.mirage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StreamStarTableWriter;

public class MirageTableWriter extends StreamStarTableWriter {

    public void writeStarTable( StarTable startab, OutputStream out )
            throws IOException {
        PrintStream pstrm = out instanceof PrintStream 
                          ? (PrintStream) out
                          : new PrintStream( out );
        MirageFormatter mf = new MirageFormatter( pstrm );
        mf.writeMirageFormat( startab );
        pstrm.flush();
    }

    public boolean looksLikeFile( String filename ) {
        return filename.endsWith( ".mirage" );
    }

    public String getFormatName() {
        return "mirage";
    }

    public String getMimeType() {
        return "text/plain";
    }
}
