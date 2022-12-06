package uk.ac.starlink.datanode.factory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.JDBCDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
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

    public DataNode buildNode( Object obj ) throws NoSuchDataException {

        /* It should be a string. */
        String string = (String) obj;

        /* If a file by this name exists, delegate to the file handler. */
        File file = new File( string );
        if ( file.exists() ) {
            return fileBuilder.buildNode( file );
        }

        /* If it looks like a JDBC URL, pass it to StarTable. */
        if ( string.startsWith( "jdbc:" ) ) {
            try {
                return new JDBCDataNode( string );
            }
            catch ( NoSuchDataException e ) {
                // oh well.
            }
        }

        /* Try to turn it into a URL and use that as a data source. */
        try {
            URL url = new URL( string );
            DataSource datsrc = new URLDataSource( url );
            try {
                return sourceBuilder.buildNode( datsrc );
            }
            catch ( NoSuchDataException e ) {
                datsrc.close();
            }
        }
        catch ( MalformedURLException e ) {
            // so it's not a URL
        }

        /* No more ideas. */
        throw new NoSuchDataException( "Not obvious what kind of node" );
    }
          

    public String toString() {
        return "StringDataNodeBuilder(java.lang.String)";
    }

}
