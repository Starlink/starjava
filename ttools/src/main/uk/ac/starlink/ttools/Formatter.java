package uk.ac.starlink.ttools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Assists in formatting text for output on the terminal.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2005
 */
public class Formatter {

    private final DocumentBuilder db_;
    private String manualName_ = "SUN/256";

    /**
     * Constructor.
     */
    public Formatter() {
        try {
            db_ = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    /**
     * Returns a string which is a formatted version of an XML string.
     * The result is suitable for output on the terminal.
     * A few elements, such as p, code, ul, ref etc may be treated specially.
     *
     * @param   xml  XML text
     * @param   indent  number of spaces to indent every line
     */
    public String formatXML( String xml, int indent ) throws SAXException {
        return formatDOM( readDOM( xml ), indent );
    }

    /**
     * Sets the text used to refer in formatted output to the STILTS manual.
     *
     * @param  name  manual reference name
     */
    public void setManualName( String name ) {
        manualName_ = name;
    }
   
    /**
     * Returns the text used to refer in formatted output to the STILTS manual.
     *
     * @return  manual reference name
     */
    public String getManualName() {
        return manualName_;
    }

    /**
     * Returns a string which is a formatted version of a DOM.
     * The result is suitable for output on the terminal.
     * A few elements, such as p, code, ul, ref etc may be treated specially.
     *
     * @param  doc  document
     * @param   indent  number of spaces to indent every line
     */
    private String formatDOM( Document doc, int indent ) {
        Result result = new Result( indent );
        appendChildren( result, doc );
        return result.getText();
    }

    /**
     * Recursive routine which adds the children of a given node to the
     * given result.
     *
     * @param  result  object which holds the final text for output
     * @param  node   node whose children will be processed
     */
    private void appendChildren( Result result, Node node ) {
        for ( Node child = node.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element el = (Element) child;
                String tag = el.getTagName();
                if ( tag.equals( "p" ) ) {
                    appendChildren( result, el );
                    result.newLine();
                    result.newLine();
                }
                else if ( tag.equals( "ul" ) ) {
                    result.newLine();
                    appendChildren( result, el );
                    result.newLine();
                    result.newLine();
                }
                else if ( tag.equals( "li" ) ) {
                    result.newLine();
                    result.appendText( " * " );
                    result.incLevel();
                    result.incLevel();
                    appendChildren( result, el );
                    result.decLevel();
                    result.decLevel();
                }
                else if ( tag.equals( "dl" ) ) {
                    appendChildren( result, el );
                    result.newLine();
                }
                else if ( tag.equals( "dt" ) ) {
                    result.incLevel();
                    result.newLine();
                    appendChildren( result, el );
                    result.decLevel();
                }
                else if ( tag.equals( "dd" ) ) {
                    result.incLevel();
                    result.incLevel();
                    result.newLine();
                    appendChildren( result, el );
                    result.newLine();
                    result.newLine();
                    result.decLevel();
                    result.decLevel();
                }
                else if ( tag.equals( "ref" ) ) {
                    if ( el.getFirstChild() != null ) {
                        appendChildren( result, el );
                    }
                    else {
                        result.appendWords( getManualName() );
                    }
                }
                else {
                    appendChildren( result, child );
                }
            }
            else if ( child instanceof Text ) {
                result.appendWords( ((Text) child).getData() );
            }
            else {
                throw new IllegalArgumentException( "Can't serialize node " + 
                                                    child.getClass() );
            }
        }
    }

    /**
     * Turns a string containing XML text into a DOM.  The submitted string
     * does not need to consist of a single top-level element.
     *
     * @param  xml  XML text
     * @return  DOM
     */
    private Document readDOM( String xml ) throws SAXException {
        String dxml = "<DOC>" + xml + "</DOC>";
        try {
            InputStream in = new ByteArrayInputStream( dxml.getBytes() );
            Document doc = db_.parse( in );
            return doc;
        }
        catch ( IOException e ) {
            throw new RuntimeException( e.getMessage(), e ); // shouldn't happen
        }
    }

    /**
     * Helper class which keeps track of a screed of output text, taking
     * care of things like indenting, line breaking etc.
     */
    private static class Result {

        StringBuffer sbuf_ = new StringBuffer();
        StringBuffer line_;
        int leng_ = 75;
        int level_;
        int indent_;
        String pad1_ = "   ";

        /**
         * Constructor.
         *
         * @param   indent  number of spaces to indent every line
         */
        Result( int indent ) {
            indent_ = indent;
            newLine();
        }

        /**
         * Appends the words in a given string to this buffer.
         * <code>words</code> is split into words using whitespace.
         * All whitespace is equivalent.
         *
         * @param   words  string of words to append
         */
        void appendWords( String words ) {
            if ( words.length() == 0 ) {
                return;
            }
            boolean spaceStart = isWhitespace( words.charAt( 0 ) );
            boolean spaceEnd = 
                isWhitespace( words.charAt( words.length() - 1 ) );
            for ( StringTokenizer st = new StringTokenizer( words );
                  st.hasMoreTokens(); ) {
                String word = st.nextToken();
                if ( line_.length() + word.length() > leng_ ) {
                    newLine();
                }
                else if ( line_.length() > 0 && 
                          spaceStart &&
                          ! isWhitespace( line_
                                         .charAt( line_.length() - 1 ) ) ) {
                    line_.append( ' ' );
                }
                line_.append( word );
                if ( ! st.hasMoreTokens() && spaceEnd && 
                     ! isWhitespace( line_.charAt( line_.length() - 1 ) ) ) {
                    line_.append( ' ' );
                }
                spaceStart = true;
            }
        }

        /**
         * Appends the given string to this buffer.
         * It's appended literally, no line breaks etc.
         *
         * @param   text  string to be appended
         */
        void appendText( String text ) {
            line_.append( text );
        }

        /**
         * Increments the indent level by one.
         */
        void incLevel() {
            level_++;
        }

        /**
         * Decrements the indent level by one.
         */
        void decLevel() {
            level_--;
        }

        /**
         * Adds a new line to this buffer.
         */
        void newLine() {
            if ( line_ != null ) {
                if ( line_.toString().trim().length() > 0 ) {
                    sbuf_.append( line_ );
                }
                sbuf_.append( '\n' );
            }
            line_ = new StringBuffer();
            for ( int i = 0; i < indent_; i++ ) {
                line_.append( ' ' );
            }
            for ( int i = 0; i < level_; i++ ) {
                line_.append( pad1_ );
            }
        }

        /**
         * Returns the text stored in this buffer.
         * This will be suitable for output to the terminal.
         *
         * @return  formatted text
         */
        String getText() {
            if ( line_ != null && line_.toString().trim().length() > 0 ) {
                sbuf_.append( line_ );
            }
            while ( sbuf_.length() > 0 &&
                    isWhitespace( sbuf_.charAt( sbuf_.length() - 1 ) ) ) {
                sbuf_.setLength( sbuf_.length() - 1 );
            }
            return sbuf_.toString()
                        .replaceAll( " *\n", "\n" )
                        .replaceAll( "\n{3,}", "\n\n" );
        }

        /**
         * Determines whether a character counts as whitespace.
         *
         * @param   c  character
         * @return  true iff <code>c</code> is whitespace
         */
        boolean isWhitespace( char c ) {
            switch ( c ) {
                case '\n':
                case '\t':
                case ' ':
                    return true;
                default:
                    return false;
            }
        }
    }
}
