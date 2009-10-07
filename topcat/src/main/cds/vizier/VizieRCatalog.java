/*
 * Created on 4 Sep 2007
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
 * Class representing a VizieR catalogue
 * 
 * @author Thomas Boch
 * @version 1.0 September 2007
 */
public class VizieRCatalog {
    private static final String VIZIER_BASE_URL = "http://vizier.u-strasbg.fr/viz-bin/votable?-out.add=_RAJ,_DEJ&-oc.form=dec&-out.meta=DhuL&-out.max=999999";
    
    private String name;
    private String desc;
    private String category; // eg : Radio, Optical
    private String density;
    
    public VizieRCatalog(String name, String desc, String category, String density) {
        this.name = name;
        this.desc = desc;
        this.category = category;
        this.density = density;
    }

    public static String getVIZIER_BASE_URL() {
        return VIZIER_BASE_URL;
    }

    public String getCategory() {
        return category;
    }

    public String getDensity() {
        return density;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }
    
    /**
     * Return the URL allowing to query this catalogue
     * 
     * @param target object name or coordinates
     * @param radius search radius in decimal degrees
     * @return the URL corresponding to this catalogue and those parameters
     */
    public URL getQueryUrl(String target, double radius) {
        URL url;
        
        try {
            url = new URL(VIZIER_BASE_URL+"&-source="+URLEncoder.encode(name, "UTF-8")+"&-c="+URLEncoder.encode(target, "UTF-8")+"&-c.rm="+radius*60.0);
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
