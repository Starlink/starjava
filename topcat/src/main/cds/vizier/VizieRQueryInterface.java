/*
 * Created on 14 Aug 2007
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.vizier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;


import cds.vizier.tools.CDSConstants;
import cds.vizier.tools.Util;

/**
 * A set of methods which wrap methods of VizieRQuery in a more O-O way
 * 
 * @author Thomas Boch [CDS]
 * @version 0.1 August 2007
 */
public class VizieRQueryInterface {

	private VizieRQuery query;
	
	// true if the metadata query has already been done
	private boolean mdQueryDone = false;
	
	/** Constructor
	 */
	public VizieRQueryInterface() {
		query = new VizieRQuery();
	}
	
	/**
	 * 
	 * @return list of Wavelength keywords (type:String) for VizieR
	 */
	public List getWavelengthKW() {
		if( ! mdQueryDone ) doMetadataQuery();
		
		Vector v = (Vector)query.gethKey().get("-kw.Wavelength");
		
        return new ArrayList(v);
	}
    
    /**
     * 
     * @return list of Mission keywords (type:String) for VizieR
     */
    public List getMissionKW() {
        if( ! mdQueryDone ) doMetadataQuery();
        
        Vector v = (Vector)query.gethKey().get("-kw.Mission");
        
        return new ArrayList(v);
    }
    
    /**
     * 
     * @return list of Mission keywords (type:String) for VizieR
     */
    public List getAstronomyKW() {
        if( ! mdQueryDone ) doMetadataQuery();
        
        Vector v = (Vector)query.gethKey().get("-kw.Astronomy");
        
        return new ArrayList(v);
    }
	
	/**
	 * Return list of VizieRMission, retrieved from a metadata query to VizieR
	 * 
	 * @return list of available VizieRMission
	 */
	public List getMissions() {
		if( ! mdQueryDone ) doMetadataQuery();
		
		Vector v = query.getvArchives();
		
		ArrayList missions = new ArrayList();
		
		String s;
		String[] parts;
		Enumeration e = v.elements();
		String name, desc;
		int nbKRow;
		while( e.hasMoreElements() ) {
			s = (String)e.nextElement();
			
			// split the string in 3 parts : name, description and nb of rows
			parts = Util.split(s, "\t");
			if( parts.length<3 ) {
				System.out.println(parts[0]);
				System.out.println("problem");
				continue;
			}
			name = parts[0];
			desc = parts[1];
			try {
				nbKRow = Integer.parseInt(parts[2]);
			}
			catch(NumberFormatException nfe) {
				nbKRow = 0;
			}
			
			missions.add(new VizieRMission(name, desc, nbKRow));
		}
		
		return missions;
	}
	
	/**
	 * Return list of VizieRSurvey, retrieved from a metadata query to VizieR
	 * 
	 * @return list of available VizieRSurvey
	 */
	public List getSurveys() {
		if( ! mdQueryDone ) doMetadataQuery();
		
		Vector v = query.getvSurveys();
		
		ArrayList surveys = new ArrayList();
		
		String s;
		String[] parts;
		Enumeration e = v.elements();
		String name, desc;
		int nbKRow;
		while( e.hasMoreElements() ) {
			s = (String)e.nextElement();
			
			// split the string in 3 parts : name, description and nb of rows
			parts = Util.split(s, "\t");
			if( parts.length<3 ) {
				System.out.println(parts[0]);
				System.out.println("problem");
				continue;
			}
			name = parts[0];
			desc = parts[1];
			try {
				nbKRow = Integer.parseInt(parts[2]);
			}
			catch(NumberFormatException nfe) {
				nbKRow = 0;
			}
			
			surveys.add(new VizieRSurvey(name, desc, nbKRow));
		}
		
		return surveys;
	}
    
    public List queryVizieR(String target, String radius, String unit, String tauthor, String extra) {
        if( ! mdQueryDone ) doMetadataQuery();
        List results = new ArrayList();
        // TODO : there is a problem with the first retrieved catalogue, some infos are missing
        query.submit(target, radius, unit, tauthor, extra, CDSConstants.FRAME, results);
        
        List vizCats = new ArrayList();
        Iterator it = results.iterator();
        String s;
        String name, desc, category, density;
        StringTokenizer st;
        while( it.hasNext() ) {
            name = category = desc = "";
            density = "0";
            
            s = (String)it.next();
            
            st = new StringTokenizer(s, ";");
            try {
                name = st.nextToken();
                category = st.nextToken();
                density = st.nextToken();
                desc = st.nextToken();
            }
            catch(NoSuchElementException e) {
                e.printStackTrace();
            }
            
            vizCats.add(new VizieRCatalog(name, desc, category, density));
        }
        
        return vizCats;
    }
	
	// TODO : gestion IOException
	private void doMetadataQuery() {
		if( !query.metaDataQuery() ) {
		    // TODO : à tester quand on n'est pas connecté
		}
	}
	
	public static void main(String[] args) {
		VizieRQueryInterface vqi = new VizieRQueryInterface();
		Iterator itMissions = vqi.getMissions().iterator();
		
		VizieRMission mission;
		while( itMissions.hasNext() ) {
			mission = (VizieRMission)itMissions.next();
			System.out.println(mission.getSmallName());
		}
	}
	
}
