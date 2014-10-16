package uk.ac.starlink.splat.vo;

import java.util.ArrayList;

import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;

public class GetDataTable {
    
  
    private ArrayList<ParamElement> paramList;
 
    // Logger.
    //private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.vo.GetDataParameters" );


    public GetDataTable( TableElement votable ) {
     
     
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
         return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );
    }
    
    

 
}
