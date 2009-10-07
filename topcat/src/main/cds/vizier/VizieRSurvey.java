/*
 * Created on 14 Aug 2007
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.vizier;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/** 
 * 
 * @author Thomas Boch
 * @version 1.0 August 2007
 */
public class VizieRSurvey {
	private static final String VIZIER_BASE_URL = "http://vizier.u-strasbg.fr/viz-bin/votable?-out.add=_RAJ,_DEJ&-oc.form=dec&-out.meta=DhuL&-out.max=999999";
	
	private String description;
	private String smallName;
	// number of Krow (thousand of rows)
	private int nbKRow;
	
	public VizieRSurvey(String smallName, String description, int nbKRow) {
		this.smallName = smallName;
		this.description = description;
		this.nbKRow = nbKRow;
	}

	public String getDescription() {
		return description;
	}

	public int getNbKRow() {
		return nbKRow;
	}

	public String getSmallName() {
		return smallName;
	}
	
	/**
	 * Return the URL allowing to query this survey
	 * 
	 * @param target object name or coordinates
	 * @param radius search radius in decimal degrees
	 * @return the URL corresponding to this survey
	 */
	public URL getQueryUrl(String target, double radius) {
		URL url;
		
		try {
			url = new URL(VIZIER_BASE_URL+"&-source="+URLEncoder.encode(smallName, "UTF-8")+"&-c="+URLEncoder.encode(target, "UTF-8")+"&-c.rm="+radius*60.0);
		}
		catch(MalformedURLException mue) {
			mue.printStackTrace();
			return null;
		}
		catch(UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return null;
		}
		
		return url;
	}

}
