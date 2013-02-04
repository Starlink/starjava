package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

public class GetDataTable {
    
    private VOElement params;
  
    List<ParamElement> paramList; 
    // Logger.
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.vo.GetDataParameters" );


    public GetDataTable( TableElement votable) {
     
      
        paramList = new ArrayList<ParamElement>();
        VOElement[] voels = votable.getChildrenByName( "PARAM" );
        int i=0;
        while ( i < voels.length ) {
       
                ParamElement pel = (ParamElement) voels[i];
                paramList.add(pel);
      
            i++;
        }
    }
    
    /**
     * Returns list containing the PARAM elements. 
     *
     * @param voe - the VO Element containing the parameters
     * @return the PARAM elements which are children of this table
     */
    public ParamElement[] getParams() {
        if (paramList==null)
            return null;
        return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );
    }
    
    
/*
    public boolean checkGenerationParameters(URL url) {
        
        InputSource inSrc=null;
        VOElement voElement;
        paramList.clear();
        
        try {
             //inSrc = new InputSource( queryCon.getInputStream() );
             inSrc = new InputSource( url.openStream() );            
         } catch (IOException ioe) {
            
             logger.info( "RESPONSE Connection IOException from " + url + " "+ioe.getMessage() );
             return false;  
         } 
         SAXParserFactory spfact = SAXParserFactory.newInstance();
         try {  // don't load external validation dtds, avoiding so long parsing delays.
             spfact.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
         } catch (SAXNotRecognizedException e) {return false; 
         } catch (SAXNotSupportedException e) {return false; 
         } catch (ParserConfigurationException e) {return false; 
         }
         
         XMLReader streamReader = null;

         // configure reader and parser
         try {
             streamReader = spfact.newSAXParser().getXMLReader();       
             // don't load external validation dtds, avoiding so long parsing delays.
             streamReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
         } catch (SAXException se) {
           
             logger.info( "RESPONSE SAXException when configuring parser" + url );
             return false; //return null;
         } catch (ParserConfigurationException pce) {
           
             logger.info( "RESPONSE ParserConfigurationException when configuring " + url );
             return false; // return null;
         }

         Source xsrc = new SAXSource( streamReader, inSrc );

         VOElementFactory vofact = new VOElementFactory();

         // parse the VO Elements
         try {
             voElement = vofact.makeVOElement( xsrc );
             // parse and return the relevant elements 
             
             VOElement table = voElement.getChildByName("TABLE");
             if (table != null && table.getName().equalsIgnoreCase("GenerationParameters")) {
                 addParamsToList( table );
                 hasGetData = true;
                
                 return true;
             }
             
           
         } catch (SAXException se) {
            
             logger.info( "RESPONSE SAXException when parsing " + url );
             return false; // return null;
         } catch (IOException ioe2) {
            
             logger.info( "RESPONSE IOException when parsing " + url );          
             return false; //  return null;
         }
    
       return false;
    }
    // param list not empty
    public boolean hasParams() {
        return (paramList != null && paramList.size() > 0);
    }
  
 
    /**
     * Adds the PARAM elements for this table.
     *
     * @param voe - the VO Element containing the parameters
     * @return the PARAM elements which are children of this table
     *
    private void addParamsToList(VOElement voe) {
        paramList.clear();
        VOElement[] voels = voe.getChildrenByName( "PARAM" );
        int i=0;
        while ( i < voels.length ) {
                ParamElement pel = (ParamElement) voels[i];
                paramList.add(pel);
            i++;
        }
    }
    
    */
 
}
