package uk.ac.starlink.votable;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.Source;

/**
 * Object representing a LINK element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Link extends VOElement {

    public Link( Source xsrc ) {
        super( xsrc, "LINK" );
    }

    /**
     * Returns the value of this Link's 'href' attribute as a URL.
     * It is resolved against the base URL of this VOTable if it 
     * represents a relative URL.  
     *
     * @return  the URL represented by this Link's 'href' attribute, 
     *          or <tt>null</tt> if it has none
     * @throws  MalformedURLException in the case of a badly-formed URL
     */
    public URL getHref() throws MalformedURLException {
        if ( hasAttribute( "href" ) ) {
            return new URL( getContext(), getAttribute( "href" ) );
        }
        else {
            return null;
        }
    }
}
