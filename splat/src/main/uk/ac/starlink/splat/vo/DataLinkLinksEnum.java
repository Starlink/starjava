package uk.ac.starlink.splat.vo;




/**
 * Enum of possible datalink links  fields .
 * 
 * 
 * @author Margarida Castro Neves
 *
 */


public enum DataLinkLinksEnum  {
	ID ("ID"),
	semantics ("semantics"),
	content_type ("content_type"),
	content_length ("content_length"),
	service_def ("service_def"),
	error_message ("error_message"),
	description ("description"),
	access_url ("access_url"),
	_default ("");
	
	private final String linksString;
	DataLinkLinksEnum(String s) {
		linksString = s;
	}

	public static DataLinkLinksEnum getLink(String input) {
		for (DataLinkLinksEnum dse : DataLinkLinksEnum.values())
			if (dse.linksString.equals(input))
				return dse;
		return _default;
	}

};



