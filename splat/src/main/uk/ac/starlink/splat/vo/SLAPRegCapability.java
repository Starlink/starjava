/**
 * 
 */
package uk.ac.starlink.splat.vo;

/**
 * 
 */
public class SLAPRegCapability extends SSAPRegCapability {
	
//	private String speciesURL;

	/**
	 * 
	 */
	public SLAPRegCapability() {
		
	}

	/**
	 * @param rci
	 */
	public SLAPRegCapability(SSAPRegCapability rci) {
		super(rci);
		
		
	}
	
	public SLAPRegCapability( SLAPRegCapability rci ) {
	    super( rci );
	    this.speciesURL = rci.getSpeciesURL();
	}

	/**
	 * @param newDescription
	 * @param newAccessUrl
	 */
	public SLAPRegCapability(String newDescription, String newAccessUrl) {
		super(newDescription, newAccessUrl);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param newDescription
	 * @param newAccessUrl
	 * @param newDataSource
	 */
	public SLAPRegCapability(String newDescription, String newAccessUrl, String newDataSource) {
		super(newDescription, newAccessUrl, newDataSource);
		// TODO Auto-generated constructor stub
	}

	public String getSpeciesURL() {
		return speciesURL;
	}

	public void setSpeciesURL(String speciesURL) {
		this.speciesURL = speciesURL;
	}
	
	

}
