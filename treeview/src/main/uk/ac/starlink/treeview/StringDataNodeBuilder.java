package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

public class StringDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static StringDataNodeBuilder instance = new StringDataNodeBuilder();

    private DataNodeBuilder fileBuilder = FileDataNodeBuilder.getInstance();
    private DataNodeBuilder sourceBuilder = SourceDataNodeBuilder.getInstance();

    /**
     * Obtains the singleton instance of this class.
     */
    public static StringDataNodeBuilder getInstance() {
        return instance;
    }

    /**
     * Private sole constructor. 
     */
    private StringDataNodeBuilder() {
    }

    public boolean suitable( Class objClass ) {
        return String.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) {

        /* It should be a string. */
        if ( ! ( obj instanceof String ) ) {
            return null;
        }
        String string = (String) obj;

        /* If a file by this name exists, delegate to the file handler. */
        File file = new File( string );
        DataNode dn;
        if ( file.exists() ) {
            dn = fileBuilder.buildNode( file );
            if ( dn != null ) {
                return dn;
            }
        }

        /* Try to turn it into a URL and use that as a data source. */
        try {
            URL url = new URL( string );
            DataSource datsrc = new URLDataSource( url );
            dn = sourceBuilder.buildNode( datsrc );
            if ( dn != null ) {
                return dn;
            }
            try {
                datsrc.close();
            }
            catch ( IOException e ) {
            }
        }
        catch ( MalformedURLException e ) {
            // so it's not a URL
        }

        /* No more ideas - return null. */
        return null;
    }
          

    public String toString() {
        return "StringDataNodeBuilder(java.lang.String)";
    }

}
