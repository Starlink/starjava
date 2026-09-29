package uk.ac.starlink.splat.vo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import jsky.util.Logger;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;

/*
 ****************************************************************************
 * DataLinkServiceResource
 * describes one datalink service
 * (identified in a VOTable as <RESOURCE type=”meta” utype=”adhoc:service”>)
 * 
 * @author Margarida Castro Neves
 *
 ****************************************************************************
 */


public class DataLinkServiceResource {

	private String refID=null;  		    // the reference in the  XML ID attribute of a service
	private String refFieldID=null;			// to reference the XML ID attribute of a FIELD
	private String accessURL=null;  		// URL to invoke the capability
	private String standardID=null; 		// URI for the capability
	private String resourceIdentifier=null; // IVOA registry identifier


	ArrayList <ParamElement> groupParams = null;
	private HashMap <String,String[]> groupParamMap= new HashMap<String,String[]>(); 
	private String format="";


	public DataLinkServiceResource() {  	   
		groupParams= new ArrayList<ParamElement>();
	}

	// adds element type "<service..."
	public DataLinkServiceResource(VOElement voel) {  

		groupParams= new ArrayList<ParamElement>();
		refID=voel.getAttribute("ID");
		Logger.info(this, "refID="+refID);
		addResource( voel );	

	}

	public void addResource(VOElement serviceEl)  // 
	{
		
		// handle the PARAM elements

		VOElement[] voels = serviceEl.getChildrenByName( "PARAM" ); // the param elements

		for ( VOElement el : voels ) {

			ParamElement pel = (ParamElement) el;
			String name = pel.getAttribute("name").toLowerCase();
			String value = pel.getAttribute("value");
			if ( "accessurl".equals(name)) {
				this.accessURL=value;
			} else if ("standardid".equals(name)) {
				this.standardID=value;
			} else if ("resourceidentifier".equals(name)) { 
				this.resourceIdentifier=value;				    		
			}
		}

		// handle the GROUP with name=InputParams element and its parameters
		VOElement[] grpels = serviceEl.getChildrenByName( "GROUP"/*, "name", "inputParams"*/ ); // the GROUP Element
		for (VOElement gel : grpels ) {

			String name = gel.getAttribute("name");

			if (name.equalsIgnoreCase("inputparams")) { // InputParams
				addGroupParams(gel);
			}

		}
	}

	private void addGroupParams( VOElement gel) {

		VOElement[] grpParams = gel.getChildrenByName( "PARAM" ); // the param elements
		// handle the group  PARAM elements
		int size=grpParams.length;
		for ( int j=0; j < size ; j++ ) {
			ParamElement pel = (ParamElement) grpParams[j];               
			if ( pel.getAttribute("name").equals("ID")) {
				refFieldID = pel.getAttribute("ref");  // the pubDID is a reference to a field in the results table
				if (refFieldID == null || refFieldID.isEmpty())  
					refFieldID=pel.getAttribute("value");   // the value is the pubDID
				Logger.info(this, "fieldrefID="+refFieldID);
			} else {
				addGroupParam(pel);

			}
		}

	}

	protected  void addGroupParam(ParamElement param) {

		groupParams.add(param);
		//ParamElement [] groupParam = groupParams.toArray(new ParamElement[]{});
		for (ParamElement p : groupParams ) {
			String xtype = p.getXtype();
			long[] arraysize = p.getArraysize();
			if ( xtype != null && xtype.equalsIgnoreCase("interval") && arraysize != null && arraysize[0]!=1) {
				groupParamMap.put(p.getName(), new String[] {"", ""} );
			} else {
				groupParamMap.put(p.getName(), new String[] {""} );
			}
		}
	}

	protected boolean isSodaService() {

		return (standardID != null && (standardID.toLowerCase().startsWith("ivo://ivoa.net/std/soda#")));

	}
	protected boolean isDataLink() {

		return (standardID != null && (standardID.toLowerCase().startsWith("ivo://ivoa.net/std/datalink#links")));

	}

	public String getRefID() {
		return refID;
	}
	
	public String getFieldRefID() {
		return refFieldID;
	}

	public String getAccessURL() {
		return accessURL;
	}

	public String getStandardID() {
		return standardID;
	}

	public String getResourceIdentifier() {
		return resourceIdentifier;
	}

	public ArrayList<ParamElement> getGroupParams() {
		return groupParams;
	}

	public HashMap<String, String[]> getGroupParamMap() {
		return groupParamMap;
	}

	 public ParamElement [] getQueryParams() {
		    
         return (ParamElement[]) groupParams.toArray(new ParamElement[]{});
     }
	 public String [] getQueryParamValue( String paramName ) {
         
         return groupParamMap.get(paramName);
         
     }
	 public void setDefaultFormat(String format) {
		 this.format=format;
	 }
	 public String getQueryFormat() {
		 return format;
	 }

	 protected boolean hasQueryParam(String name) {
		 return groupParamMap.containsKey(name);
	 }
	 
	 protected void setQueryParam( String paramName, String value ) {  
         groupParamMap.remove(paramName);
         groupParamMap.put(paramName, new String[] { value });
     }
     protected void setQueryParam( String paramName, String min, String max ) {
         groupParamMap.remove(paramName);
         groupParamMap.put(paramName, new String[] { min, max });
     }
     
     protected  Set<String> getQueryParamNames() {
         return  groupParamMap.keySet();
     }
     
     public String getDataLinkRequest() {
    	 if (!isSodaService())
    		 return "";
    	 
    	 String request="";
    	    for (String key : getQueryParamNames()) {
                String [] values = getQueryParamValue( key);
                if (values != null && values.length > 0) {
                    String value=values[0];
                    if (values.length==2 && (!values[0].isEmpty()||!values[1].isEmpty()) ) //if any of the values is not empty
                        value+=" "+values[1];
                    try {//
                        if (! key.equals("IDSource") && ! (key.equals("AccessURL"))) {
                          
                            if (!value.isEmpty())
                                request+="&"+key+"="+URLEncoder.encode(value, "UTF-8");
                        }

                    } catch (UnsupportedEncodingException e) {
                    	Logger.warn(this, e.getMessage());
                        return "";
                    }                                     
                }
            }
    	    return request;
     }
   

}
