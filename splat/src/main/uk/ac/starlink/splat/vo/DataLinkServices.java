/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import jsky.util.Logger;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.ValuesElement;

/**
 * Class for storing Datalink parameters, which can be defined either from
 * service resources starting with <resource ... utype=adhoc:service> 
 * or from a datalink votable containing links to different services or files.
 * 
 * @author Margarida Castro Neves
 *
 */


public class DataLinkServices {
    
    
	//private DataLinkResponse dlResponse=null;
    private ArrayList <DataLinkServiceResource> service=null ;
     
    /**
     * Constructs an empty instance. Information will be added later.
     *
     */
    public DataLinkServices( )  {
      
        service = new ArrayList <DataLinkServiceResource>();
 
    }
    
    public DataLinkServices(DataLinkServiceResource dlr )  {
        
        service = new ArrayList <DataLinkServiceResource>();
        service.add(dlr);
    }
    
    /**
     * Constructs the DataLink Parameters from a VOElement (<RESOURCE utype=adhoc:service >)
     * 
     */
    public DataLinkServices( VOElement voel) {
    	service = new ArrayList <DataLinkServiceResource>();
        service.add(new DataLinkServiceResource(voel));
  
    }
    
    public DataLinkServices(ArrayList <VOElement> voels) {
    	
    	service = new ArrayList <DataLinkServiceResource>();
		for (VOElement ve : voels) {
			service.add(new DataLinkServiceResource(ve));
		}
		
	}
    
    
   

    public String getIdSource() {
    	DataLinkServiceResource dl = getDataLinkService();
    	if (dl != null)
    		return dl.getFieldRefID();  
    	return null;
    
    }
    	 
    public String getServiceId() { 
    	DataLinkServiceResource soda = getSodaService();
    	if (soda != null)
    		return soda.getRefID();  
    	return null;
    }

    public boolean hasSodaService() {
        return (getSodaService() != null);    	
    }
   public boolean hasDataLinkService() {
        return (getDataLinkService() != null);    	

    }
    
    public  String getDataLinkLink() {    

    	DataLinkServiceResource dl = getDataLinkService();
    	if (dl != null)
    		return dl.getAccessURL();  
    	return null;


    }
    
   
    public int getServiceCount() {
        return service.size();
    }
 

    protected void setQueryParam( String paramName, String value ) {
       	DataLinkServiceResource srv = getSodaService();
    	if (srv != null)
       // for (DataLinkService srv : service)
            if (srv.hasQueryParam(paramName))
                srv.setQueryParam(paramName, value);        
   }
    

    protected void setQueryParam( String paramName, String min, String max ) {
       	DataLinkServiceResource srv = getSodaService();
    	if (srv != null)
     //   for (DataLinkService srv : service)
            if (srv.hasQueryParam(paramName))
                srv.setQueryParam(paramName, min, max);        
   }
    

	
    
    
 /*  
   
    public void writeParamToFile( BufferedWriter writer, String sname ) throws IOException {
        //writer.write( "<RESOURCE type=\"meta\" utype=\"adhoc:service\" name=\""+name+"\">" );
        // 

        for (int i=0; i<service.size(); i++) {
            ParamElement[] pel = service.get(i).getQuerySodaParams();
            if (pel.length >0) {
                writer.write( "<RESOURCE utype=\"adhoc:service\" name=\""+sname+"\">" );
                writer.newLine();
                writer.write( "<GROUP name=\"inputparams\">");
                writer.newLine();
                writer.write( "<PARAM arraysize=\"*\" datatype=\"char\" name=\"ID\" ref=\"ssa_pubDID\" ucd=\"meta.id;meta.main\" value=\""+getSodaIdSource()+"\"></PARAM>");
                writer.newLine();
                
                for (int j=0;j<pel.length; j++) {
                    String arraysize=pel[j].getAttribute("arraysize");
                    String datatype=pel[j].getAttribute("datatype");
                    String name=pel[j].getAttribute("name");
                    String ucd=pel[j].getAttribute("ucd");
                    String utype=pel[j].getAttribute("utype");
                    String attrvalue=pel[j].getAttribute("value");

                    ValuesElement values = (ValuesElement) pel[j].getChildByName("VALUES");
                    if (values != null ) {
                        String [] options = values.getOptions(); 
                        if (options != null && options.length >0) {                   
                            writer.write("<PARAM arraysize=\""+arraysize+ "\" datatype=\""+datatype+"\" name=\""+name+ 
                                    "\" ucd=\""+ucd+ "\" utype=\""+utype+ "\" value=\""+attrvalue+"\">");
                            writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                            writer.newLine();
                            writer.write("<VALUES>" );
                            writer.newLine();
                            for (int k=0;k<options.length; k++) {
                                writer.write("<OPTION value=\""+options[k]+ "\"></OPTION>" );
                            }
                            writer.newLine();
                            writer.write("</VALUES>");
                            writer.newLine();
                            writer.write("</PARAM>");
                            writer.newLine();
                        } else {
                            writer.write("<PARAM ID=\"id"+j+"\" datatype=\""+datatype+"\" name=\""+name+ 
                                    "\" ucd=\""+ucd+ "\" utype=\""+utype+"\" unit=\""+pel[j].getUnit()+ "\" value=\""+attrvalue+"\">");
                            writer.newLine();
                            writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                            writer.newLine();
                            String max = values.getMaximum();
                            String min=values.getMinimum();
                            if (! max.isEmpty() || !min.isEmpty() ) {
                                writer.write("<VALUES>" );
                                writer.newLine();
                                if (!min.isEmpty())
                                    writer.write("<MIN value=\""+min+"\" ></MIN>" );
                                writer.newLine();
                                if (!max.isEmpty())
                                    writer.write("<MAX value=\""+max+"\" ></MAX>" );
                                writer.newLine();
                                writer.write("</VALUES>");
                                writer.newLine();
                                writer.write("</PARAM>");
                                writer.newLine();
                            }
                        }
                    } else {
                        writer.write("<PARAM ID=x datatype=\""+datatype+"\" name=\""+name+ 
                                "\" ucd=\""+ucd+ "\" utype=\""+utype+"\" unit=\""+pel[j].getUnit()+ "\" value=\""+attrvalue+"\"></PARAM>");
                        writer.newLine();
                        writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                        writer.newLine();
                        writer.write("</PARAM>");
                        writer.newLine();
                    }
                } // for j
                writer.write("</GROUP>");
                writer.newLine();
       //         writer.write("<GROUP name=\"inputParams\">");
       //         writer.newLine();
       ////         writer.write("<PARAM arraysize=\"*\" datatype=\"char\" name=\"ID\" ref=\"ssa_pubDID\" ucd=\"meta.id;meta.main\" value=\"\">");
        //        writer.newLine();
        //        writer.write("<LINK content-role=\"ddl:id-source\" value=\"#"+id_source+"\"></LINK></PARAM>");
        //        writer.newLine();
         //       writer.write("</GROUP>");
        //        writer.newLine();
                writer.write("<PARAM arraysize=\"*\" datatype=\"char\" name=\"accessURL\" ucd=\"meta.ref.url\" value=\""+getQueryAccessURL(i)+"\"></PARAM>");
                writer.newLine();
                writer.write("</RESOURCE>");
                writer.newLine();
            }
        } // for i       
      
    }

*/
	public DataLinkServiceResource getDataLinkService(String serviceDef) {
		
		for (DataLinkServiceResource s : service) {
			if (((String) s.getRefID()).equals(serviceDef))
				return s;
		}
		return null;
	}
	public DataLinkServiceResource getDataLinkService() {
		
		for (DataLinkServiceResource s : service) {
			if ( s.isDataLink() )
				return s;
		}
		return null;
	}

	public DataLinkServiceResource getSodaService() {
		for (DataLinkServiceResource s : service) {
			if (s.isSodaService())
				return s;
		}
		return null;
	}




    
}
