package uk.ac.starlink.ant.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import uk.ac.starlink.ant.types.JarDownload;

/**
 * Task for maintaining a set of downloaded files.
 * Files are downloaded into a "cache" directory as required,
 * from which they are never deleted by this task,
 * and at runtime those which are required as indicated by
 * nested elements within the updateDownloads element
 * are copied into a "destination" directory, from which they
 * can be used by other parts of the build.
 *
 * <p>This task has three attributes:
 * <ul>
 * <li><code>dest</code>: destination directory where usable files
 *     will be copied to (and removed from) according to the nested
 *     content of this element
 * <li><code>cache</code>: cache directory where files will be downloaded
 *     to once only
 * <li><code>md5</code>: if true, on execution a file "md5sums.txt" will
 *     be written to the dest directory, containing MD5 sums of all the
 *     downloaded files currently in that directory
 * </ul>
 *
 * <p>An example invocation might look like:
 * &lt;updatedownloads dest="${basedir}/dest" cache="${basedir}/cache"&gt;
 *    &lt;jardownload owner="uk/ac/starlink" name="stil" version="4.3.5"/&gt;
 * &lt;/updatedownloads&gt;
 * </p>
 * The nested &lt;jardownload&gt; type also has an
 * optional parameter <code>repo</code> which defaults to
 * "<code>https://repo1.maven.org/maven2</code>".
 *
 * @author  Mark Taylor
 * @since   29 Jul 2026
 */
public class UpdateDownloads extends Task {

    private String dest_;
    private String cache_;
    private boolean md5_;
    private final List<DownloadItem> downloadItems_;
    private static final int BUFSIZ = 64 * 1024;
    private static final String MD5_FILE = "md5sums.txt";

    public UpdateDownloads() {
        downloadItems_ = new ArrayList<DownloadItem>();
        md5_ = true;
    }

    public void setDest( String dest ) {
        dest_ = dest;
    }

    public void setCache( String cache ) {
        cache_ = cache;
    }

    public void setMd5( boolean md5 ) {
        md5_ = md5;
    }

    public void addConfiguredJardownload( JarDownload jd ) {
        downloadItems_.add( jd );
    }

    public void execute() throws BuildException {
        File copyDir = getExistingDirectory( dest_, "dest" );
        File cacheDir = getExistingDirectory( cache_, "cache" );
        Map<String,URL> urlMap =
            downloadItems_.stream()
           .collect( Collectors.toMap( DownloadItem::getFilename,
                                       d -> toUrl( d.getUrl() ) ) );
        List<String> copiedNames = Arrays.stream( copyDir.listFiles() )
                                  .filter( File::isFile )
                                  .map( File::getName )
                                  .collect( Collectors.toList() );
        List<String> cachedNames = Arrays.stream( cacheDir.listFiles() )
                                  .filter( File::isFile )
                                  .map( File::getName )
                                  .collect( Collectors.toList() );
        List<String> wantNames = downloadItems_.stream()
                                .map( DownloadItem::getFilename )
                                .collect( Collectors.toList() );
        List<String> uncopiedNames = new ArrayList<>( wantNames );
        uncopiedNames.removeAll( copiedNames );
        List<String> uncachedNames = new ArrayList<>( uncopiedNames );
        uncachedNames.removeAll( cachedNames );
        List<String> unwantedNames = new ArrayList<>( copiedNames );
        unwantedNames.removeAll( wantNames );
        unwantedNames.remove( MD5_FILE );

        /* Delete files from dest dir that are not required. */
        for ( String unwantedName : unwantedNames ) {
            boolean deleted = new File( copyDir, unwantedName ).delete();
            System.out.println( "Deleting " + unwantedName
                                 + ( deleted ? "" : " (failed)" ) );
        }
        try {

            /* Download required files that are not already in the cache. */
            for ( String cacheName : uncachedNames ) {
                URL url = urlMap.get( cacheName );
                File cacheFile = new File( cacheDir, cacheName );
                System.out.println( "Downloading " + cacheName
                                  + " from " + url );
                try ( InputStream in = url.openStream();
                      OutputStream out = new FileOutputStream( cacheFile ) ) {
                    copyStream( in, out );
                }
            }

            /* Make sure that copies of the required files are in the
             * dest directory by copying them from the cache if they are
             * missing. */
            for ( String copyName : uncopiedNames ) {
                File cacheFile = new File( cacheDir, copyName );
                File copyFile = new File( copyDir, copyName );
                System.out.println( "Copying " + copyName );
                try ( InputStream in = new FileInputStream( cacheFile );
                      OutputStream out = new FileOutputStream( copyFile ) ) {
                    copyStream( in, out );
                }
            }

            /* Write MD5 sums if required. */
            if ( md5_ ) {
                String[] sortedNames = wantNames.toArray( new String[ 0 ] );
                Arrays.sort( sortedNames );
                File md5file = new File( copyDir, MD5_FILE );
                try ( Writer out =
                          new OutputStreamWriter(
                              new FileOutputStream( md5file ),
                              StandardCharsets.UTF_8 ) ) {
                    int nmd5 = 0;
                    for ( String name : sortedNames ) {
                        File inFile = new File( copyDir, name );
                        try ( InputStream in = new FileInputStream( inFile ) ) {
                            String md5 = md5Sum( in );
                            out.write( md5 + " " + name + "\n" );
                            nmd5++;
                        }
                    }
                }
                catch ( IOException | NoSuchAlgorithmException e ) {
                    System.out.println( "Failed to write MD5 sums: " + e );
                }
            }
        }
        catch ( IOException e ) {
            throw new BuildException( "Download error: " + e, e );
        }
    }

    /**
     * Returns a file object representing a directory that actually exists.
     *
     * @param  attValue   directory path
     * @param  attName    name of variable, used for error reporting
     * @return   directory file
     * @throws  BuildException  if the named directory doesn't exist
     */
    private File getExistingDirectory( String attValue, String attName )
            throws BuildException {
        if ( attValue == null || attValue.trim().length() == 0 ) {
            throw new BuildException( "No value specified for dir " + attName );
        }
        File dir = new File( attValue );
        if ( ! dir.isDirectory() ) {
            throw new BuildException( "Not such directory for " + attName
                                    + ": " + attValue );
        }
        return dir;
    }

    /**
     * Copies data from one stream to another.
     * Streams are not closed.
     *
     * @param  in  source stream
     * @param  out  destination stream
     */
    private static void copyStream( InputStream in, OutputStream out )
            throws IOException {
        byte[] buf = new byte[ BUFSIZ ];
        for ( int n; ( n = in.read( buf ) ) > 0; ) {
            out.write( buf, 0, n );
        }
    }

    /**
     * Calculates an MD5 sum from an input stream.
     * It is not closed.
     *
     * @param   in  input stream
     * @return   MD5 string
     */
    private static String md5Sum( InputStream in )
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance( "MD5" );
        byte[] buf = new byte[ BUFSIZ ];
        for ( int n; ( n = in.read( buf ) ) > 0; ) {
            digest.update( buf, 0, n );
        }
        BigInteger i = new BigInteger( 1, digest.digest() );
        return String.format( "%1$032x", i );
    }

    /**
     * Converts a string to a URL.
     *
     * @param   txt  string form of URLo
     * @return   URL object
     * @throws  BuildException  if there's a problem
     */
    private static URL toUrl( String txt ) throws BuildException {
        try {
            return new URI( txt ).toURL();
        }
        catch ( MalformedURLException | URISyntaxException e ) {
            throw new BuildException( "Bad URL: " + txt, (Throwable) e );
        }
    }

    /**
     * Item that can be downloaded.
     */
    public interface DownloadItem {

        /**
         * URL from which remote resource can be obtained.
         *
         * @return  URL string
         */
        String getUrl();

        /**
         * Local filename for copy of resource.
         *
         * @return  filename without path
         */
        String getFilename();
    }
}
