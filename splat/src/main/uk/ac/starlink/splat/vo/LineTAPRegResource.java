package uk.ac.starlink.splat.vo;

import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;

public class LineTAPRegResource implements RegResource {
	private String tableName;
	private String shortName;
	private String accessURL;
	// private String serviceName;

	public LineTAPRegResource()
	{

	}

	public LineTAPRegResource( LineTAPRegResource resource )
	{
		shortName = resource.getShortName();
		tableName = resource.getTableName();
		accessURL = resource.getAccessURL();
		// serviceName = resource.get
	}


	public LineTAPRegResource(String tablename, String accessURL )
	{
		//  resource.setShortName();
		setTableName(tablename);
		setAccessURL(accessURL);
	}


	public String getShortName()
	{
		return shortName;
	}

	public void setShortName( String shortName )
	{
		this.shortName = shortName;
	}
	public String getTableName() 
	{
		return tableName;
	}

	public void setTableName( String tableName )
	{
		this.tableName = tableName;
	}

	private String getAccessURL() {
		// TODO Auto-generated method stub
		return accessURL;
	}
	private void setAccessURL(String accessurl) {
		this.accessURL= accessurl;

	}

	@Override
	public RegCapabilityInterface[] getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContact() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPublisher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getReferenceUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSubjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}




}
