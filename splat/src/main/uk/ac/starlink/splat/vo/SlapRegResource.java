package uk.ac.starlink.splat.vo;

public class SlapRegResource extends SSAPRegResource {
	
	//private SLAPRegCapability[] capabilities;

	public SlapRegResource() {
		super();
	}

	public SlapRegResource(SSAPRegResource resource) {
		super(resource);
		// redo capabilities to add species_url
        SSAPRegCapability[] rci = resource.getCapabilities();
        SLAPRegCapability[] slapCaps = new SLAPRegCapability[rci.length];
        for ( int i = 0; i < rci.length; i++ ) {
            slapCaps[i] = new SLAPRegCapability( rci[i] );
        }
     //   capabilities = slapCaps;
        setCapabilities( slapCaps );  // 
		
	}

	public SlapRegResource(String newShortName, String newTitle, String newDescription, String newAccessUrl, String newSpeciesUrl) {
		super(newShortName, newTitle, newDescription, newAccessUrl);
		  SLAPRegCapability[] caps = new SLAPRegCapability[1];
	      caps[0] = new SLAPRegCapability( newDescription, newAccessUrl, newSpeciesUrl );
	      setCapabilities( caps );
		
	}

	public SlapRegResource(String newShortName, String newTitle, String newDescription, String newAccessUrl,
			String[] newWaveBand, String newDataSource, String newSpeciesUrl) {
		super(newShortName, newTitle, newDescription, newAccessUrl, newWaveBand, newDataSource);
		SLAPRegCapability[] caps = new SLAPRegCapability[1];
		caps[0] = new SLAPRegCapability( newDescription, newAccessUrl, newSpeciesUrl );
	    setCapabilities( caps );
	}
	
	public SLAPRegCapability[] getCapabilities() {
		 return (SLAPRegCapability[]) super.getCapabilities();
	
	}
	
	public void setCapabilities(SLAPRegCapability[] caps) {
	
	     super.setCapabilities( caps ); 	
	}

}
