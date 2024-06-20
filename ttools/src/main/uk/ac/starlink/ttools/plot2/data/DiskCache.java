package uk.ac.starlink.ttools.plot2.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a persistent cache based on named files in a given
 * directory of a filesystem.
 * This class does not provide all the required facilities for
 * cache management, but it provides some methods which will be
 * useful during such management.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2020
 */
public class DiskCache {

    private final File dir_;
    private final Map<File,Long> createdFiles_;
    private final Level logLevel_;
    private final Logger logger_;
    private long limit_;

    public static final String CACHE_DIRNAME = "stilts-cache";
    public static final String README_NAME = "README-cache.txt";
    private static final String UTF8 = "UTF-8";

    /**
     * Constructor.
     *
     * @param  dir  directory into which cache files will be written
     * @param  limit  cache size indicator;
     *                if positive, it's the maximum cache size in bytes;
     *                if negative, it's the amount of space on the
     *                disk that cache usage tries to keep free;
     *                if zero, it's something adaptive
     */
    public DiskCache( File dir, long limit ) {
        dir_ = dir;
        limit_ = limit;
        logger_ = Logger.getLogger( "uk.ac.starlink.ttools.plot2.data" );
        logLevel_ = Level.WARNING;
        createdFiles_ = new ConcurrentHashMap<File,Long>();
        Runtime.getRuntime()
               .addShutdownHook( new Thread( "Cache shutdown logging" ) {
            public void run() {
                // The logging system is not reliable during shutdown,
                // so we have to just print this.
                if ( createdFiles_.size() > 0 ) {
                    System.err.println( "NOTE: " + getWriteSummary() );
                }
            }
        } );
    }

    /**
     * Returns the directory associated with this cache.
     *
     * @return  cache directory
     */
    public File getDir() {
        return dir_;
    }

    /**
     * Records that a file has been added to the cache.
     * This record is used to report persistently added files at shutdown.
     * No check is made that the file in question actually was added.
     *
     * @param  f  file added
     */
    public void fileAdded( File f ) {
        createdFiles_.put( f, Long.valueOf( f.length() ) );
    }

    /**
     * Ensures that this cache's directory is ready for use.
     * Should be called before the cache is used.
     */
    public void ready() throws IOException {
        if ( mkdirs( dir_, false ) ) {
            logger_.log( logLevel_, "Created plot cache directory " + dir_ );
            initBaseCacheDir( getNamedAncestor( dir_, CACHE_DIRNAME ) ); 
        }
        if ( ! dir_.isDirectory() || ! dir_.canWrite() ) {
            throw new IOException( "Cache path " + dir_
                                 + " is not a writable directory" );
        }
    }

    /**
     * Logging utility function.
     * This just passes a message to the logging system, using the
     * logging level associated with this instance.
     *
     * @param  txt  message to log
     */
    public void log( String txt ) {
        logger_.log( logLevel_, txt );
    }

    /**
     * Removes files from this cache's directory if it contains more
     * data than the configured cache limit.
     */
    public void tidy() {
        if ( limit_ == 0 ) {
            limit_ = getDefaultCacheLimit( dir_ );
        }

        /* Find out how much free space there is on the filesystem. */
        long freeSpace = dir_.getUsableSpace();

        /* If we're being asked to maintain a certain amount of free space,
         * and there is more than that free, then no action is required. */
        if ( limit_ < 0 && freeSpace > -limit_ ) {
            logger_.log( logLevel_,
                         "Plot cache filesystem free space "
                       + formatByteSize( freeSpace ) + " > "
                       + formatByteSize( -limit_ ) );
            return;
        }

        /* Otherwise, there is a fixed limit on cache size.
         * Determine the current cache size. */
        File[] files = dir_.listFiles();
        List<FileMeta> fms = new ArrayList<>();
        long totsize = 0;
        for ( File f : files ) {
            FileMeta fm = new FileMeta( f );
            fms.add( fm );
            totsize += fm.size_;
        }

        /* Find out the effective maximum size. */
        long maxSize = limit_ > 0
                     ? limit_
                     : freeSpace + totsize + limit_;
        log( "Plot cache usage: " + formatByteSize( totsize )
                          + " / " + formatByteSize( maxSize ) );

        /* If actual usage exceeds maximum size, delete files oldest first
         * until we are back within the required limit.
         * Note there are probably smarter strategies than this,
         * like preferentially deleting very large old-ish files,
         * but for now keep it simple. */
        long overSize = totsize - maxSize;
        if ( overSize > 0 ) {
            Collections.sort( fms, FileMeta.BY_LAST_MODIFIED );
            for ( FileMeta fm : fms ) {
                if ( overSize <= 0 ) {
                    return;
                }
                File file = fm.file_;
                long size = fm.size_;
                if ( file.delete() ) {
                    overSize -= size;
                    log( "Drop plot cache file " + file
                       + " (" + formatByteSize( size ) + ")" );
                }
                else {
                    logger_.warning( "Failed to drop plot cache file " + file );
                }
            }
        }
    }

    /**
     * Returns a string summarising total cache files written to date.
     *
     * @return  summary text
     */
    public String getWriteSummary() {
        long nbyte = 0;
        for ( Long fsize : createdFiles_.values() ) {
            nbyte += fsize.longValue();
        }
        int nfile = createdFiles_.size();
        return new StringBuffer()
           .append( "Persistent plot cache data written: " )
           .append( nfile )
           .append( " " )
           .append( nfile == 1 ? "file" : "files" )
           .append( ", " )
           .append( formatByteSize( nbyte ) )
           .append( " in directory " )
           .append( dir_ )
           .toString();
    }

    /**
     * Updates the lastModified timestamp for a given file.
     *
     * @param  file  file to touch
     */
    public void touch( File file ) {
        if ( ! file.setLastModified( System.currentTimeMillis() ) ) {
            logger_.warning( "Touch " + file + " failed" );
        }
    }

    /**
     * Intialises a cache directory by writing a readme file explaining
     * its status.
     *
     * @param   dir  directory to initialise; if null no action is taken
     */
    private void initBaseCacheDir( File dir ) {
        if ( dir == null ) {
            return;
        }
        File readmeFile = new File( dir, README_NAME );
        if ( readmeFile.exists() ) {
            return;
        }
        try {
            PrintStream out =
                new PrintStream( new FileOutputStream( readmeFile ) );
            out.println( "This directory holds persistent cached data" );
            out.println( "for use by the STILTS application." );
            out.println( "It's probably related to plotting operations." );
            out.println( "Deleting this directory or its contents" );
            out.println( "is not likely to break anything," );
            out.println( "but it may lead to slower startup of some plots" );
            out.println( "next time they are invoked." );
            out.close();
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Failed to write " + readmeFile, e );
        }
    }

    /**
     * Returns a default-length hash string corresponding to a given string.
     * This doesn't have to be cryptographically secure, but accidental
     * collisions are to be avoided.
     *
     * @param  txt  text to hash
     * @return  fixed-length hash string
     */
    public static String hashText( String txt ) {
        return hashText( txt, 16 );
    }

    /**
     * Returns a configurable-length hash string corresponding
     * to a given string.
     * This doesn't have to be cryptographically secure, but accidental
     * collisions are to be avoided.
     *
     * @param  txt  text to hash
     * @param  nchar  number of characters in output (currently up to 32)
     * @return  hash string
     */
    public static String hashText( String txt, int nchar ) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance( "MD5" );
        }
        catch ( NoSuchAlgorithmException e ) {
            assert false;
            return txt.substring( 0, Math.min( nchar, txt.length() ) );
        }
        byte[] txtBytes;
        try {
            txtBytes = txt.getBytes( UTF8 );
        }
        catch ( UnsupportedEncodingException e ) {
            assert false;
            txtBytes = txt.getBytes();
        }
        byte[] mdbytes = md.digest( txtBytes );
        int nbyte = Math.min( nchar / 2, mdbytes.length );
        Formatter formatter = new Formatter();
        for ( int i = 0; i < nbyte; i++ ) {
            formatter.format( "%02x", Byte.valueOf( mdbytes[ i ] ) );
        }
        return formatter.toString();
    }

    /**
     * Produces a working filename to be used as temporary workspace
     * when assembling a given destination filename.
     *
     * <p>We could just use one of the <code>File.createTempFile</code> methods,
     * but attempt to come up with a name that is (a) likely to be unique and
     * (b) bears some resemlance to the requested destination file,
     * for ease of debugging etc.
     *
     * @param  file  destination file
     * @return  workspace file
     */
    public static File toWorkFilename( File file ) {
        String parent = file.getParent();
        String name = file.getName();
        int dotIx = name.lastIndexOf( '.' );
        String workId =
            "W-" +
            String.format( "%08x", Integer.valueOf( (int) System.nanoTime() ) );
        String rename = dotIx >= 0 ? name.substring( 0, dotIx ) + "-" +
                                     workId + name.substring( dotIx )
                                   : workId + "-" + name;
        return new File( parent, rename );
    }

    /**
     * Formats a string representing a number of bytes for human consumption.
     *
     * @param  nbyte  byte count
     * @return   storage size string
     */
    public static String formatByteSize( long nbyte ) {
        long kfact = 1000;   // kB is 1000; kiB is 1024
        long kilo = kfact * 1;
        long mega = kfact * kilo;
        long giga = kfact * mega;
        long tera = kfact * giga;
        long absNbyte = Math.abs( nbyte );
        if ( absNbyte >= tera * 10 ) {
            return ( nbyte / tera ) + "TB";
        }
        else if ( absNbyte >= giga * 10 ) {
            return ( nbyte / giga ) + "GB";
        }
        else if ( absNbyte >= mega * 10 ) {
            return ( nbyte / mega ) + "MB";
        }
        else if ( absNbyte >= kilo * 10 ) {
            return ( nbyte / kilo ) + "kB";
        }
        else {
            return nbyte + "bytes";
        }
    }

    /**
     * Returns the default system scratch directory.
     * This is supplied from the <code>java.io.tmpdir</code> system property.
     *
     * @return  scratch directory
     */
    public static File getSystemTmpDir() {
        return new File( System.getProperty( "java.io.tmpdir" ) );
    }
    
    /**
     * Constructs a cache directory path given a base directory and a
     * purpose-specific label.
     *
     * @param   baseDir  base directory; if null, java.io.tmpdir is used
     * @param   label    purpose-specific label (used for subdirectory name)
     * @return  directory to which cache files can be written
     */
    public static File toCacheDir( File baseDir, String label ) {
        if ( baseDir == null ) {
            baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
        }
        return new File( new File( baseDir, CACHE_DIRNAME ), label );
    }

    /**
     * Creates a scratch directory and any missing parents,
     * assigning write privileges for all or owner-only.
     * This is like {@link java.io.File#mkdirs}, but it allows the option
     * of assigning global write privileges to any created directories.
     *
     * @param  dir  target directory
     * @param  ownerOnly   true to make created directories writable by
     *                     ownwer only, false to make them writable by all
     * @return  true if creation succeeded
     */
    public static boolean mkdirs( File dir, boolean ownerOnly ) {
        return dir.exists() ? false : createDirIfNecessary( dir, ownerOnly );
    }

    /**
     * Recursively checks existence of a directory, and attempts to
     * create it and any parents if it does not exist.
     * Write permissions are assigned as per
     * the <code>ownerOnly</code> parameter.
     *
     * @param  dir  target directory
     * @param  ownerOnly   true to make created directories writable by
     *                     ownwer only, false to make them writable by all
     * @return  true iff the directory exists on exit
     */
    private static boolean createDirIfNecessary( File dir, boolean ownerOnly ) {
        if ( dir.exists() ) {
            return true;
        }
        else {
            if ( createDirIfNecessary( dir.getParentFile(), ownerOnly ) ) {
                if ( dir.mkdir() ) {
                    dir.setWritable( true, ownerOnly );
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
    }

    /**
     * Returns a cacheLimit value that is suitable for general usage.
     * The return value is negative, meaning that it corresponds to
     * requiring a certain number of bytes free on the filesystem.
     *
     * @param  dir  cache directory
     */
    public static long getDefaultCacheLimit( File dir ) {
        long cacheSize = 0;
        for ( File f : dir.listFiles() ) {
            cacheSize += f.length();
        }
        long freeSize = dir.getUsableSpace();
        long availSize = freeSize + cacheSize;
        return - availSize / 2;
    }

    /**
     * Returns an ancestor of the given file with a given name, if there is one.
     * If no such ancestor exists, null is returned.
     *
     * @param  file  descendent file
     * @param  name  target name of ancestor
     * @param  ancestor of file with given name, or null
     */
    private static File getNamedAncestor( File file, String name ) {
        if ( file == null ) {
            return null;
        }
        else if ( name.equals( file.getName() ) ) {
            return file;
        }
        else {
            return getNamedAncestor( file.getParentFile(), name );
        }
    }

    /**
     * Aggregates required file metadata.
     */
    private static class FileMeta {
        final File file_;
        final long size_;
        final long lastModified_;
        static final Comparator<FileMeta> BY_LAST_MODIFIED =
            (fm1, fm2) -> Long.compare( fm1.lastModified_, fm2.lastModified_ );
        FileMeta( File file ) {
            file_ = file;
            size_ = file.length();
            lastModified_ = file.lastModified();
        }
        boolean isBad() {
            return size_ <= 0 || lastModified_ <= 0;
        }
    }
}
