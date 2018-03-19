package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.URL;

import org.xml.sax.SAXException;

import jsky.util.Logger;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOTableBuilder;

public class DataLinkResponse {
	
	
	private StarTable linksTable;
	private DataLinkServices services;

	int idIndex = -1;
	int semanticsIndex =-1;     // index of the semantics column
	int accessUrlIndex =-1;     // index of the access_url column
	int contentTypeIndex =-1;   // index of content_type column
	int contentLengthIndex =-1; // index of content_length column
	int serviceDefIndex =-1;    // index of service_def column
	int errorMessageIndex = -1; // index of error_message column
	int descriptionIndex = -1; // index of description column
//	private ImageIcon preview;
	
	public DataLinkResponse ( StarTable table ) {
		linksTable = table;
		getTableIndexes();
	}
	
	
 /**
  * Constructs the DataLink Parameters from an URL string pointing to a
  *  VOTable with DataLink information, resulted as a response from a query.
  *  The VOTABLE must contain only Datalink info and service resources
  * @throws IOException 
  * @throws SAXException 
  */
	public DataLinkResponse ( String dataLinksrc ) throws IOException, SAXException {
	
	
        URL dataLinkURL = new URL(dataLinksrc);
        DataSource  datsrc = new URLDataSource( dataLinkURL );

        VOElement votable =  new VOElementFactory( StoragePolicy.getDefaultPolicy() ).makeVOElement( datsrc.getInputStream(), datsrc.getURL().toString() );
  //      linksTable =
  //              new VOTableBuilder().makeStarTable( datsrc, true,
   //                     StoragePolicy.getDefaultPolicy() );
        linksTable = DalResourceXMLFilter.getDalResultTable(votable);
        
        getTableIndexes();
        	    
        services =  DalResourceXMLFilter.getDalGetServiceElement(votable); 
       
    }
    
 	
	
	private void getTableIndexes() {
		
	    
		for (int i=0;i<linksTable.getColumnCount();i++) {
			ColumnInfo ci =  linksTable.getColumnInfo(i);
			String colname = ci.getName().replaceAll( "\\s", "_" );
			switch (DataLinkLinksEnum.getLink(colname)) {
			case ID:
				idIndex=i;
				break;
			case semantics:
				semanticsIndex =i;  
				break;
			case content_type:
				contentTypeIndex =i;
				break;
			case content_length:
				contentLengthIndex =i;
				break; 
			case service_def:
				serviceDefIndex=i;
				break;
			case error_message:
				errorMessageIndex = i;
				break;
			case description:
				descriptionIndex=i;
				break;
			case access_url:
				accessUrlIndex =i;   

			}
		}
	}
	
	public String getAccessURL( String semantics ) throws IOException   {
		for (int i=0;i<linksTable.getRowCount();i++) {
			String value = (String) linksTable.getCell(i, semanticsIndex);
			
	
			if (value.equalsIgnoreCase(semantics)) {
				if (errorMessageIndex != -1 ) {
					String error = (String) linksTable.getCell(i, errorMessageIndex);
					if (error != null) {
						Logger.info(this,  error);
						return null;
					}
				}
				return	(String) linksTable.getCell(i, accessUrlIndex);
			}
		}
		return null;

	}
	
	public String getContentType( String semantics ) throws IOException   {
		for (int i=0;i<linksTable.getRowCount();i++) {
			String value = (String)  linksTable.getCell(i, semanticsIndex);
			
	
			if (value.equalsIgnoreCase(semantics)) {
				if (errorMessageIndex != -1 ) {
					String error = (String) linksTable.getCell(i, errorMessageIndex);
					if (error != null) {
						Logger.info(this,  error);
						return null;
					}
				}
				return	(String) linksTable.getCell(i, contentTypeIndex);
			}
		}
		return null;

	}
	
	public int getIdIndex() {
		return semanticsIndex;
	}
	public int getSemanticsIndex() {
		return semanticsIndex;
	}

	public int getAccessUrlIndex() {
		return accessUrlIndex;
	}

	public int getContentTypeIndex() {
		return contentTypeIndex;
	}

	public int getContentLengthIndex() {
		return contentLengthIndex;
	}

	public int getServiceDefIndex() {
		return serviceDefIndex;
	}

	public int getErrorMessageIndex() {
		return errorMessageIndex;
	}

	public int getDescriptionIndex() {
		return descriptionIndex;
	}

	public StarTable getLinksTable() {
		
		return linksTable;
	}

	
	public String getIDValue(int row)   {
		try {
			return (String) linksTable.getCell(row, idIndex);
		} catch (Exception e) {
			return null;

		}
	}
	public String getSemanticsValue(int row)   {
		try {
			return (String) linksTable.getCell(row, semanticsIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getAccessUrlValue( int row )   {
		try {
			return (String) linksTable.getCell(row, accessUrlIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getContentTypeValue( int row )   {
		try {
			return (String) linksTable.getCell(row, contentTypeIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getContentLengthValue( int row )   {
		
		try {
			return (String) linksTable.getCell(row, contentLengthIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getServiceDefValue( int row )   {
		
		try {
			return (String) linksTable.getCell(row, serviceDefIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getErrorMessageValue( int row )   {
		try {
			return (String) linksTable.getCell(row, errorMessageIndex);
		} catch (Exception e) {
			return null;

		}
	}

	public String getDescriptionValue( int row )  {
		try {
			return (String) linksTable.getCell(row, descriptionIndex);
		} catch (Exception e) {
			return null;

		}
	}
	public String getThisLink() throws SplatException {
		try {
			return getAccessURL("#this");
		} catch (IOException e) {
			
			throw new SplatException( "no #this link found");
		}
		
	}
	
	public String getThisContentType() throws SplatException {
	
		try {
			return getContentType("#this");
		} catch (IOException e) {
			
			throw new SplatException( "no #this link found");
		}
		
	}
	
	public String getLinkAccessURL( String semantics ) throws SplatException {
		
		try {
			return getContentType(semantics);
		} catch (IOException e) {
			
			throw new SplatException( "no #this link found");
		}
		
	}


	public DataLinkServiceResource getDataLinkService(String serviceDef) {
		
		return services.getDataLinkService(serviceDef);
	}


	public DataLinkServices getServices() {
		
		return services;
	}
	
}
