package uk.ac.starlink.topcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.starlink.util.DOMUtils;

/**
 * Turns a JavaHelp XML file into an HTML table of contents frame.
 * This class is not used by the TOPCAT application itself, but
 * can be used as a utility to turn TOPCAT's help files into a free-standing
 * set of HTML help pages.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HelpTransformer {

    private DocumentBuilder parser;
    private Map targetMap = new HashMap();
    private PrintStream out;
    private Element helpEl;
    private File baseDir;
    private String tframe;

    /**
     * Constructs a new HelpTransformer which can transform a given
     * helpset file.
     *
     * @param  helpset  the file containing the 'helpset' XML document
     */
    public HelpTransformer( File helpset ) {
        Document helpDoc = parse( helpset.toString(), "helpset" );
        baseDir = helpset.getParentFile();
        helpEl = helpDoc.getDocumentElement();
    }

    /**
     * Sets the frame name into which which links will open new pages.
     *
     * @param  tframe the target frame name
     */
    public void setTargetFrame( String tframe ) {
        this.tframe = tframe;
    }

    /**
     * Writes a table of contents frame based on this transformer's 
     * help files to the given output stream.  The output will be an
     * HTML file suitable for use as a table of contents for the 
     * HTML pages constituting the help system.
     *
     * @param   out  the stream to which the HTML should be directed
     */
    public void write( PrintStream out ) {
        this.out = out;

        /* Get the helpset title. */
        Element titleEl = DOMUtils.getChildElementByName( helpEl, "title" );
        String title = titleEl == null
                     ? null 
                     : DOMUtils.getTextContent( titleEl ).trim();

        /* Process target maps. */
        Element maps = DOMUtils.getChildElementByName( helpEl, "maps" );
        NodeList maprefs = maps.getElementsByTagName( "mapref" );
        for ( int i = 0; i < maprefs.getLength(); i++ ) {
            Element maprefEl = (Element) maprefs.item( i );
            String maprefLoc = maprefEl.getAttribute( "location" ).trim();
            if ( maprefLoc.length() > 0 ) {
                processMap( maprefLoc );
            }
        }

        /* Get a table of contents. */
        String tocData = null;
        NodeList views = helpEl.getElementsByTagName( "view" );
        for ( int i = 0; i < views.getLength(); i++ ) {
            Element view = (Element) views.item( i );
            Element typeEl = DOMUtils.getChildElementByName( view, "type" );
            if ( typeEl != null ) {
                String type = DOMUtils.getTextContent( typeEl ).trim();
                if ( type.equals( "javax.help.TOCView" ) ) {
                    Element dataEl = DOMUtils
                                    .getChildElementByName( view, "data" );
                    tocData = DOMUtils.getTextContent( dataEl ).trim();
                    break;
                }
            }
        } 

        /* Write an HTML table of contents. */
        Document tocDoc = parse( tocData, "toc" );
        writeHeader( title );
        processTocItems( tocDoc.getDocumentElement(), 0 );
        writeFooter();
    }

    /**
     * Writes the HTML heading text.
     *
     * @param  title string
     */
    public void writeHeader( String title ) {
        out.println( "<!DOCTYPE HTML PUBLIC " +
                     "'-//W3C//DTD HTML 4.01 Frameset//EN'>" );
        out.println( "<head>" );
        out.println( "<title>" + title + "</title>" );
        if ( tframe != null ) {
            out.println( "<base target='" + tframe + "'>" );
        }
        out.println( "</head>" );
        out.println( "<body>" );
    }

    /**
     * Writes the HTML fotter text.
     */
    public void writeFooter() {
        out.println( "</body>" );
        out.println( "</html>" );
    }

    /**
     * Reads an XML 'map' document and remembers the target->URL mappings
     * therein.
     *
     * @param  mapFile  file containing an XML javahelp map document
     */
    public void processMap( String mapFile ) {
        Document doc = parse( mapFile, "map" );
        Element mapEl = doc.getDocumentElement();
        for ( Node child = mapEl.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element && 
                 ((Element) child).getTagName().equals( "mapID" ) ) {
                Element el = (Element) child;
                String target = el.getAttribute( "target" );
                String url = el.getAttribute( "url" );
                if ( target.length() > 0 && url.length() > 0 ) {
                    targetMap.put( target, url );
                }
            }
        }
    }

    /**
     * Recursively writes table of contents entries for <tt>tocitem</tt>
     * children of a given element.
     * 
     * @param  tocParent  the element whose tocitem children will be processed
     * @param  level  level of recursion 
     */
    public void processTocItems( Element tocParent, int level ) {
        boolean first = true;
        for ( Node child = tocParent.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element &&
                 ((Element) child).getTagName().equals( "tocitem" ) ) {
                Element item = (Element) child;
                String target = item.getAttribute( "target" );
                String text = item.getAttribute( "text" );
                String image = item.getAttribute( "image" );
                String loc = (String) targetMap.get( target );
                if ( first ) { 
                    listStart( level );
                    first = false;
                }
                listItem( level, "<a href='" + loc + "'>" + text + "</a>" );
                processTocItems( item, level + 1 );
            }
        }
        if ( ! first ) {
            listEnd( level );
        }
    }

    /**
     * Output any preample to a list.
     *
     * @param  level  level of recursion 
     */
    private void listStart( int level ) {
        // out.println( makePadding( level ) + "<ul>" ); 
    }

    /**
     * Output any postamble to a list.
     *
     * @param  level  level of recursion 
     */
    private void listEnd( int level ) {
        // out.println( makepadding( level ) + "</ul>" );
    }

    /**
     * Output a list item.
     *
     * @param  level  level of recursion 
     * @param  text   HTML text of the item
     */
    private void listItem( int level, String text ) {
        out.print( makePadding( level ) );
        for ( int i = 0; i < level; i++ ) {
            out.print( "&nbsp;&nbsp;&nbsp;&nbsp;" );
        }
        out.println( text + "</br>" );
    }

    /**
     * Returns a padding string for use at the start of a line at a 
     * given level of recursion.
     *
     * @param  level  level of recursion 
     * @return   padding string
     */
    private static String makePadding( int level ) {
        int npad = level * 2;
        StringBuffer sbuf = new StringBuffer( npad );
        for ( int i = 0; i < npad; i++ ) {
            sbuf.append( ' ' );
        }
        return sbuf.toString();
    }

    /**
     * Does XML parsing of a given file, returning a document which is 
     * guaranteed to have a top-level element with a given tagname.
     * If this can't be done (parsing error, wrong tagname), some kind
     * of RuntimeException is thrown.
     *
     * @param  file  filename of the file to be parsed
     * @param  topEl  tagname required for the top-level element of the 
     *                document at <tt>file</tt>
     * @return a DOM document with a top-level element that has a tagname
     *               of <tt>topEl</tt>
     */
    public Document parse( String file, String topEl ) {
        File f = new File( baseDir, file );
        try {
            if ( parser == null ) {
                    parser = DocumentBuilderFactory.newInstance()
                                                   .newDocumentBuilder();
            }
            Document doc = parser.parse( f );
            String doctag = doc.getDocumentElement().getTagName();
            if ( ! doctag.equals( topEl ) ) {
                throw new IllegalArgumentException( 
                    "Document " + f + " top element is <" + doctag + "> not " +
                    topEl + ">" );
            }
            return doc;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Error parsing file " + f + ": " + e );
        }
    }

    /**
     * Produces an HTML table of contents from a &lt;helpset&gt; document.
     * <blockquote>
     * Usage: HelpTransformer [-help] [-target frameName] hsfile
     * </blockquote>
     *
     * @param  args  command-line arguments
     */
    public static void main( String[] args ) throws IOException {
        String usage = "HelpTransformer [-help] [-target frame] hsfile";
        List arglist = new ArrayList( Arrays.asList( args ) );
        String tframe = null;
        for ( Iterator it = arglist.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-target" ) ) {
                it.remove();
                tframe = (String) it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-" ) ) {
                System.err.println( usage );
                System.exit( 0 );
            }
            else {
                break;
            }
        }
 
        if ( arglist.size() != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        File hsfile = new File( (String) arglist.get( 0 ) );
        if ( ! hsfile.canRead() ) {
            throw new FileNotFoundException( "Can't read file " + hsfile );
        }
        HelpTransformer htrans = new HelpTransformer( hsfile );
        htrans.setTargetFrame( tframe );
        htrans.write( System.out );
    }

}
