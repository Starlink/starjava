package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Convenience class to manipulate XML Sources. 
 * Methods are provided to do the useful things you might 
 * want done with from a {@link javax.xml.transform.Source}.
 * Depending on the type of the input source this may involve an
 * XML transformation or it may not; such a transformation is not 
 * performed if it is not required.
 * <p>
 * The transformer object used in the case that transformations are
 * required may be accessed or set to permit some customisation of
 * the way the transformation is done.  Some convenience methods are
 * provided for doing these settings as well.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SourceReader {

    private Transformer transformer;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.util" );

    /* Implicit no-arg constructor. */

    /**
     * Returns a reference to the Transformer object used for transformations
     * used by this object.  Its characteristics may be changed if required.
     * Note that in the case a transformation is
     * not required (e.g. in the case of getting a DOM node from a
     * source which is already a <code>DOMSource</code>) this transformer
     * will not be used.
     *
     * @return  the transformer object used when transformation is necessary
     */ 
    public Transformer getTransformer() {
        if ( transformer == null ) {

            try {
                /* Create a new transformer. */
                transformer = TransformerFactory.newInstance().newTransformer();
            }
            catch ( TransformerException e ) {
                throw new RuntimeException( "Unexpected configuration problem",
                                            e );
            }

            /* Configure some properties to generally useful values. */
            transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
            setIndent( -1 );
            setIncludeDeclaration( true );

            /* Configure the transformer's error listener to do sensible
             * things with errors. */
            transformer.setErrorListener( new ErrorListener() {
                public void warning( TransformerException e )
                        throws TransformerException {
                    log( e );
                }
                public void error( TransformerException e )
                        throws TransformerException {
                    log( e );
                }
                public void fatalError( TransformerException e ) 
                        throws TransformerException {
                    throw e;
                }
                private void log( TransformerException e ) {
                    logger.warning( e.toString() );
                }
            } );
        }
        return transformer;
    }

    /**
     * Sets the transformer object used for transformations.
     * Note that in the case a transformation is
     * not required (e.g. in the case of getting a DOM node from a
     * source which is already a <code>DOMSource</code>) this transformer
     * will not be used.
     *
     * @param trans  the transformer object to be used when transformation is
     *          necessary.  If <code>null</code> is supplied, a default
     *          transformer will be used.
     */
    public void setTransformer( Transformer trans ) {
        transformer = trans;
    }

    /**
     * Returns a DOM Node representing the given source.
     * Transformation errors are handled by this object's 
     * {@link javax.xml.transform.Transformer},
     * whose behaviour is in turn determined by its 
     * {@link javax.xml.transform.ErrorListener}.
     * By default, this <code>SourceReader</code> is installed as the 
     * <code>ErrorListener</code>.
     *
     * @param   src  the Source for which the DOM is required
     * @return  a DOM node (typically an <code>Element</code>) representing the
     *          XML data in <code>src</code>
     * @throws  TransformerException  if some error occurs in transformation
     *                                or I/O
     */
    public Node getDOM( Source src ) throws TransformerException {
        if ( src instanceof DOMSource ) {
            return ((DOMSource) src).getNode();
        }
        else {
            DOMResult res = new DOMResult();
            transform( src, res );
            return res.getNode();
        }
    }

    /**
     * Returns a DOM Element representing the given source.
     * This convenience method invokes {@link #getDOM} and then finds 
     * an element in the result - if the result is an element that is
     * returned, but if it is a Document then the top-level document 
     * element is returned.  Anything else throws an IllegalArgumentException.
     *
     * @param   src  the Source for which the DOM is required
     * @return  an Element representing the XML data in <code>src</code>
     * @throws  TransformerException  if some error occurs in transformation
     *                                or I/O
     * @throws  IllegalArgumentException if src does not represent a 
     *          Document or Element
     */
    public Element getElement( Source src ) throws TransformerException {
        Node node = getDOM( src );
        if ( node instanceof Element ) {
            return (Element) node;
        }
        else if ( node instanceof Document ) {
            return ((Document) node).getDocumentElement();
        }
        else {
            throw new IllegalArgumentException(
                "Source " + src + " is not an Element or Document" );
        }
    }

    /**
     * Writes the contents of a given Source into a given Writer.
     * Additional buffering will be performed on the writer if necessary.
     * The writer will be flushed, but not closed.
     * <p>
     * <i>Hmm, not sure if the encoding is handled correctly here for
     * SAXSources...</i>
     *
     * @param   src  the Source to be written
     * @param   wr   the destination for the content of <code>src</code>
     * @throws  TransformerException  if some error occurs in transformation
     *                                or I/O
     */
    public void writeSource( Source src, Writer wr ) 
            throws TransformerException {
        try {

            /* Make sure we've got a buffered writer for efficiency. */
            if ( ! ( wr instanceof BufferedWriter ) ) {
                wr = new BufferedWriter( wr );
            }

            /* If we can get a Reader directly from the source, copy chars
             * directly from that to the writer. */
            Reader rdr = getReader( src );
            if ( rdr != null ) {
                try {
                    if ( ! ( rdr instanceof BufferedReader ) ) {
                        rdr = new BufferedReader( rdr );
                    }
                    int c;
                    while ( ( c = rdr.read() ) > -1 ) {
                        wr.write( c );
                    }
                }
                finally {
                    rdr.close();
                }
            }

            /* Otherwise, do an XML transformation into a StreamResult based 
             * on our writer. */
            else {
                Result res = new StreamResult( wr );
                transform( src, res );
            }
            wr.flush();
        }
        catch ( IOException e ) {
            throw new TransformerException( e );
        }
    }

    /**
     * Writes the contents of a given Source into a given OutputStream.
     * Additional buffering will be performed on the stream if necessary.
     * The stream will be flushed, but not closed.
     *
     * @param   src   the Source to be written
     * @param   ostrm the destination for the content of <code>src</code>
     * @throws  TransformerException  if some error occurs in transformation
     *                                or I/O
     */
    public void writeSource( Source src, OutputStream ostrm )
            throws TransformerException {
        try {

            /* Make sure we've got a buffered output stream for efficiency. */
            if ( ! ( ostrm instanceof BufferedOutputStream ) ) {
                ostrm = new BufferedOutputStream( ostrm );
            }

            /* If we can get an InputStream directly from the source, copy 
             * bytes directly from that to the OutputStream. */
            InputStream istrm = getInputStream( src );
            if ( istrm != null ) {
                try {
                    if ( ! ( istrm instanceof BufferedInputStream ) ) {
                        istrm = new BufferedInputStream( istrm );
                    }
                    int b;
                    while ( ( b = istrm.read() ) > -1 ) {
                        ostrm.write( b );
                    }
                }
                finally {
                    istrm.close();
                }
            }

            /* Otherwise, do an XML transformation into a StreamResult based
             * on our OutputStream. */
            else {
                Result res = new StreamResult( ostrm );
                transform( src, res );
            }
            ostrm.flush();
        }
        catch ( IOException e ) {
            throw new TransformerException( e );
        }
    }

    /**
     * Returns an input stream from which the serialised XML text 
     * corresponding to a given Source can be read.
     *
     * @param  src  the Source to be read
     * @return  an InputStream which will supply the XML serialisation of
     *          <code>src</code>
     */
    public InputStream getXMLStream( final Source src ) {
        final PipedOutputStream ostrm = new PipedOutputStream();
        PipedInputStream istrm;
        try {
            istrm = new PipedInputStream( ostrm );
        }
        catch ( IOException e ) {
            throw new AssertionError( "What could go wrong?" );
        }
        new Thread() {
            public void run() {
                try {
                    writeSource( src, ostrm );
                }
                catch ( TransformerException e ) {
                    // May well catch an exception here if the reader stops
                    // reading.
                }
                finally {
                    try {
                        ostrm.close();
                    }
                    catch ( IOException e ) {
                    }
                }
            }
        }.start();
        return istrm;
    }

    /**
     * Tries to set the indent level used by the <code>writeSource</code>
     * methods.
     * This method modifies the output properties of the the 
     * current transformer to affect the way it does the transformation
     * (so will be undone by a subsequent <code>setTransformer</code>).
     * If the supplied <code>indent</code> value is &gt;=0 then the transformer 
     * may add whitespace when producing the XML output; it will be encouraged
     * to prettyprint the XML using <code>indent</code> spaces to indicate
     * element nesting, though whether this is actually done depends on
     * which parser is actually being used by JAXP.
     * If <code>indent&lt;0</code> then no whitespace will be added when
     * outputting XML.
     * <p>
     * By default, no whitespace is added.
     * <p>
     * For convenience the method returns this <code>SourceReader</code> 
     * is returned.
     *
     * @param  indent  indicates if and how whitespace should be added by
     *                 <code>writeSource</code> methods
     * @return  this <code>SourceReader</code>
     */
    public SourceReader setIndent( int indent ) {
        Transformer trans = getTransformer();
        if ( indent >= 0 ) {
            trans.setOutputProperty( OutputKeys.INDENT, "yes" );

            /* Attempt to set the indent; if we don't have an Apache
             * transformer this may have no effect, but at worst it is
             * harmless. */
            trans.setOutputProperty( 
                "{http://xml.apache.org/xslt}indent-amount", 
                Integer.toString( indent ) );
        }
        else {
            trans.setOutputProperty( OutputKeys.INDENT, "no" );
        }
        return this;
    }

    /**
     * Sets whether the <code>writeSource</code> methods will output an XML
     * declaration at the start of the XML output.
     * This method modifies the output properties of the the 
     * current transformer to affect the way it does the transformation
     * (so will be undone by a subsequent <code>setTransformer</code>).
     * <p>
     * By default, the declaration is included
     * <p>
     * For convenience the method returns this <code>SourceReader</code> 
     * is returned.
     *
     * @param  flag  <code>true</code> if the <code>writeSource</code> methods 
     *               are to output an XML declaration,
     *               <code>false</code> if they are not to
     * @return  this <code>SourceReader</code>
     */
    public SourceReader setIncludeDeclaration( boolean flag ) {
        Transformer trans = getTransformer();
        trans.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION,
                                 flag ? "no" : "yes" );
        return this;
    }

    /**
     * Performs the transformation.
     */
    private void transform( Source src, Result res ) 
            throws TransformerException {
        Transformer trans = getTransformer();
        trans.transform( src, res );
    }


    /**
     * Attempts to get a Reader object directly from a provided XML Source.
     * If none is available, null is returned.
     */
    private static Reader getReader( Source src ) {

        /* Try to get a Reader directly from a StreamSource. */
        if ( src instanceof StreamSource ) {
            StreamSource strmsrc = (StreamSource) src;

            Reader rdr = strmsrc.getReader();
            if ( rdr != null ) { 
                return rdr;
            }
        }

        /* Try to get a Reader directly from a SAXSource. */
        if ( src instanceof SAXSource ) {
            SAXSource saxsrc = (SAXSource) src;
            InputSource input = saxsrc.getInputSource();
            if ( input != null ) {
                Reader rdr = input.getCharacterStream();
                if ( rdr != null ) {
                    return rdr;
                }
            }
        }

        /* Try to get an InputStream directly and turn that into a Reader. */
        InputStream istrm = getInputStream( src );
        if ( istrm != null ) {
            return new InputStreamReader( istrm );
        }

        /* No luck. */
        return null;
    }

    /**
     * Attempts to get an InputStream directly from a provided XML Source.
     * If none is available, null is returned.
     */
    private static InputStream getInputStream( Source src ) {

        /* Try to get an InputStream directly from a StreamSource. */
        if ( src instanceof StreamSource ) {
            StreamSource strmsrc = (StreamSource) src;

            InputStream istrm = strmsrc.getInputStream();
            if ( istrm != null ) {
                return istrm;
            }

            String sysid = strmsrc.getSystemId();
            if ( sysid != null ) {
                try {
                    URL url = URLUtils.newURL( sysid );
                    return url.openStream();
                }
                catch ( MalformedURLException e ) {
                    // no action
                }
                catch ( IOException e ) {
                    // no action
                }
            }
        }

        /* Try to get an InputStream directly from a SAXSource. */
        if ( src instanceof SAXSource ) {
            SAXSource saxsrc = (SAXSource) src;
            InputSource input = saxsrc.getInputSource();
            if ( input != null ) {

                InputStream istrm = input.getByteStream();
                if ( istrm != null ) {
                    return istrm;
                }

                String sysid = saxsrc.getSystemId();
                if ( sysid != null ) {
                    try {
                        URL url = URLUtils.newURL( sysid );
                        return url.openStream();
                    }
                    catch ( MalformedURLException e ) {
                        // no action
                    }
                    catch ( IOException e ) {
                        // no action
                    }
                }
            }
        }

        /* No luck. */
        return null;
    }

}
