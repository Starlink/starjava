package uk.ac.starlink.treeview;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import uk.ac.starlink.datanode.nodes.IconFactory;

/**
 * Does symbolic preprocessing on HTML files.
 * This class is not used by the Treeview application, but is used as
 * a standalone utility for maintaining the Treeview web page.
 * It just does substitution so that IconFactory.XXXX is translated to
 * the right icon.
 */
class HTMLProcessor {

    /**
     * Takes an HTML file from one place and puts it, plus any icons that
     * it uses, into another.
     * <pre>
     *    Usage: HTMLProcessor input-file output-file-or-dir.
     * </pre>
     */
    public static void main( String[] args ) throws IOException {

        /* Get args. */
        File infile = new File( args[ 0 ] );
        File outfile = new File( args[ 1 ] );
        if ( outfile.isDirectory() ) {
            outfile = new File( outfile, infile.getName() );
        }

        /* Slurp in input file. */
        StringBuffer inbuf = new StringBuffer( (int) infile.length() );
        Reader inrdr = new BufferedReader( new FileReader( infile ) );
        for ( int c; ( c = inrdr.read() ) > -1; ) {
            inbuf.append( (char) c );
        }
        inrdr.close();

        /* Go through file looking for expressions to substitute. */
        Set symbols = new HashSet();
        Matcher match = Pattern.compile( 
            "(\\ssrc=[\"']?)IconFactory\\.([A-Z0-9_]+)([\"']?)" )
                               .matcher( inbuf );
        StringBuffer outbuf = new StringBuffer( inbuf.length() );
        while ( match.find() ) {
            String symbol = match.group( 2 );
            Icon ic = makeIcon( symbol );
            String dimspec = " width='" + ic.getIconWidth() + "'"
                           + " height='" + ic.getIconHeight() + "'";
            String replace = dimspec + " $1" 
                           + makeOutLocFromSymbol( symbol ) + "$3";
            match.appendReplacement( outbuf, replace );
            symbols.add( symbol );
        }
        match.appendTail( outbuf );

        /* Make sure that the output directory exists. */
        File outdir = outfile.getAbsoluteFile().getParentFile();
        outdir.mkdirs();
        
        /* Write the output file. */
        Writer outwr = new BufferedWriter( new FileWriter( outfile ) );
        for ( int i = 0; i < outbuf.length(); i++ ) {
            outwr.write( outbuf.charAt( i ) );
        }
        outwr.close();

        /* Make sure that all of the required icons are in place. */
        for ( Iterator it = symbols.iterator(); it.hasNext(); ) {
            String symbol = (String) it.next();
            InputStream istrm = makeIconInputStream( symbol );
            File oicon = new File( outdir, makeOutLocFromSymbol( symbol ) );
            File icondir = oicon.getParentFile();
            if ( ! icondir.exists() ) {
                icondir.mkdirs();
            }
            OutputStream ostrm = 
                new BufferedOutputStream( new FileOutputStream( oicon ) );
            for ( int b; ( ( b = istrm.read() ) > -1 ); ) {
                ostrm.write( b );
            }
            istrm.close();
            ostrm.close();
        }
    }

    private static String makeOutLocFromSymbol( String symbol ) {
        return "icons/" + symbol + ".gif";
    }

    private static InputStream makeIconInputStream( String symbol ) 
            throws IOException {
        try {
            Field field = IconFactory.class.getField( symbol );
            short id = field.getShort( null );
            URL iconURL = IconFactory.getIconURL( id );
            return iconURL.openStream();
        }
        catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( symbol );
        }
        catch ( IllegalAccessException e ) {
            throw new IllegalArgumentException( symbol );
        }
        catch ( Exception e ) {
            throw new RuntimeException( symbol, e );
        }
    }

    private static Icon makeIcon( String symbol ) {
        try {
            Field field = IconFactory.class.getField( symbol );
            short id = field.getShort( null );
            return IconFactory.getIcon( id );
        }
        catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( symbol );
        }
        catch ( IllegalAccessException e ) {
            throw new IllegalArgumentException( symbol );
        }
    }
}
