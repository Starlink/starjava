package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.xml.sax.SAXException;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.HeadBufferInputStream;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * TapQuery extension to read Datalink parameters from the resulting VOTable
 * 
 * @author   Margarida Castro Neves
 * @since    October 2017
 */


public class TapQueryWithDatalink extends TapQuery {

	private VOElement voDomElement=null;
	private DataLinkServices dlParams=null;
	
	public TapQueryWithDatalink(URL serviceUrl, String adql, Map<String, String> extraParams) {
		super(serviceUrl, adql, extraParams);
		
	}
	
	public StarTable executeSync( StoragePolicy storage, ContentCoding coding ) throws IOException {
		
		voDomElement = getTapDom( createSyncConnection( coding ), coding, storage );
		dlParams = DalResourceXMLFilter.getDalGetServiceElement(voDomElement, true);
		//dlParams = DalResourceXMLFilter.getDalGetServiceElement(voDomElement);
		return  DalResourceXMLFilter.getDalResultTable( voDomElement );
		
	}
	
	
	public DataLinkServices getDatalinkServices()  {		
		return dlParams; 				
	}

			
	
	public VOElement getTapDom(URLConnection conn, ContentCoding coding, StoragePolicy storage) throws IOException {
        /* Get input stream. */
        int headSize = 2048;
        HeadBufferInputStream in =
            new HeadBufferInputStream( getVOTableStream( conn, coding ),
                                       headSize );

        /* Read the result as a VOTable DOM. */
        VOElement voEl;
        try {
            voEl = new VOElementFactory( storage )
                  .makeVOElement( in, conn.getURL().toString() );
        }
        catch ( SAXException e ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "TAP response is not a VOTable" );
            byte[] buf = in.getHeadBuffer();
            int nb = Math.min( (int) in.getReadCount(), (int) buf.length );
            if ( nb > 0 ) {           
                sbuf.append( " - " ).append( new String( buf, 0, nb, "UTF-8" ) );
                if ( nb == buf.length ) {
                	sbuf.append( " ..." );
                }
            }	
            throw (IOException)
              new IOException( sbuf.toString() ).initCause( e );
        }
        finally {
        	in.close();
        }
	    return voEl;
	}
	


}
