/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.text.*;
import java.util.*;

/** Observatory Data
 */
public class Observatory {
    private int NOBS = 83;
    private String Id, Name;
    private double Longitude, Latitude, Height;
    private char EW, NS;
    private int longdeg, longmin, latdeg, latmin;
    private double longsec, latsec;
    private static final double DAS2R  = Math.PI / ( 180 * 3600 );

/** Create Observatory
 *  @param id Code name for Observatory
 *  @param name Full name of Observatory
 *  @param ew Either 'E' or 'W'
 *  @param lo Longitude in degrees
 *  @param ns Either 'N' or 'S'
 *  @param lat Latitude in degrees
 *  @param ht Height in metres
 */
    Observatory ( String id, String name, char ew, double lo, 
                             char ns, double lat, double ht ) {
        Id = id; Name = name;
        EW = ew; Longitude = lo;
        NS = ns; Latitude = lat;
        Height = ht;
    }

/** Create Observatory
 *  @param i Identifying number for Observatory
 */
    Observatory ( int i ) {
        if ( i > NOBS ) i = NOBS+1;
        i--;
        Id = Obs[i][0];
        Name = Obs[i][1];
        if ( Name == null ) return;
        setLong( Obs[i][2] );
        setLat( Obs[i][3] );
        Height = Double.parseDouble( Obs[i][4] );
    }

/** Create Observatory
 *  @param id Code name for Observatory
 */
    Observatory ( String id ) {
        int i = 0;
        for ( ; ! Obs[i][0].equals ( null ); i++ ) {
            if ( id.equals( Obs[i][0] ) ) break;
        }
        Id = Obs[i][0];
        Name = Obs[i][1];
        if ( Name == null ) return;
        setLong( Obs[i][2] );
        setLat( Obs[i][3] );
        Height = Double.parseDouble( Obs[i][4] );
   }

/** Set Longitude from String
 *  @param lon String of form "E dd mm ss.f"
 */
    private void setLong( String lon ) {
        StringTokenizer st = new StringTokenizer ( lon );
        String s = st.nextToken();
        EW = s.charAt( 0 );
        longdeg = Integer.parseInt( st.nextToken() );
        longmin = Integer.parseInt( st.nextToken() );
        longsec = Double.parseDouble( st.nextToken() );
        if ( EW == 'W' )
            Longitude = DAS2R * ( (60.0 * ( (60.0 * longdeg) + longmin ) ) + longsec );
        else 
            Longitude = - DAS2R * ( (60.0 * ( (60.0 * longdeg) + longmin) ) + longsec );
    }

/** Set Latitude from String
 *  @param lat String of form "N dd mm ss.f"
 */
    private void setLat( String lat ) { 
        StringTokenizer st = new StringTokenizer ( lat );
        String s = st.nextToken();    
        NS = s.charAt( 0 );        
        latdeg = Integer.parseInt( st.nextToken() );
        latmin = Integer.parseInt( st.nextToken() );
        latsec = Double.parseDouble( st.nextToken() );
        if ( NS == 'N' ) 
            Latitude = DAS2R * ( (60.0 * ( (60.0 * latdeg) + latmin) ) + latsec );
        else 
            Latitude = - DAS2R * ( (60.0 * ( (60.0 * latdeg) + latmin ) ) + latsec );
    }

/** Get identifier
 *  @return Code name for Observatory
 */
    public String getId() { return Id; }

/** Get Name
 *  @return Full name for Observatory
 */
    public String getName() { return Name; }

/** Get Longitude
 *  @return Longitude of Observatory
 */
    public double getLong() { return Longitude; }

/** Get Latitude
 *  @return Latitude of Observatory
 */
    public double getLat() { return Latitude; }

/** Get Height
 *  @return Height of Observatory in metres
 */
    public double getHeight() { return Height; }

/** Get the longitude, latitude and height as a string
 *  @return Longitude, latitude and height as a string of the form 
 *          'E dd mm ss.ss N dd mm ss.ss (Height: nn.nn m)'
 */
    public String printPosition( ) { return printPosition( 2 ); }

/** Get the longitude, latitude and height as a string
 *  @param  ndp  Number of decimal places in answer
 *  @return Longitude, latitude and height as a string of the form 
 *          'E dd mm ss.f N dd mm ss.f (Height: nn.f m)'
 *          where f is to the required precision
 */
    public String printPosition( int ndp ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits( ndp );
        nf.setMinimumFractionDigits( ndp );
        NumberFormat nfi = NumberFormat.getNumberInstance();
        nfi.setMinimumIntegerDigits( 2 );
        return EW + " " + longdeg + " " + nfi.format(longmin) + " "
                  + nf.format(longsec) + "   " +
               NS + " " + latdeg + " " + nfi.format(latmin) + " "
                  + nf.format(latsec)
                  + " (Height: " + nf.format(Height) + "m)";
    };

/** Get the name of the Observatory as a string
 *  @return Name of the Observatory in the form 'fullname [id]'
 */
    public String toString() {
        return Name + " [" + Id +"]";
    };


/** Return the number of observatories available 
 *  @return the number of observatories available.
 */
    public static int getObservatoryCount() {
        return Obs.length - 1;
    }

/** Get the name of an observatory 
 *  @param i index of the observatory
 *  @return name of the observatory, null if off limits
 */
    public static String getObservatoryName( int i ) {
        if ( i < Obs.length ) {
            return Obs[i][1];
        }
        return null;
    }

/** Get the short identifier of an observatory 
 *  @param i index of the observatory
 *  @return id of the observatory, null if off limits
 */
    public static String getObservatoryID( int i ) {
        if ( i < Obs.length ) {
            return Obs[i][0];
        }
        return null;
    }

/* AAT ( Observer's Guide ) */
    private static String Obs[][] = {
            { "AAT", "Anglo-Australian 3.9m Telescope",
              "E 149 3 57.91", "S 31 16 37.34", "1164.0" },
/* WHT ( Gemini, April 1987 ) */
            { "LPO4.2", "William Herschel 4.2m Telescope",
              "W 17 52 53.9" , "N 28 45 38.1", "2332.0" },
/* INT ( Gemini, April 1987 ) */
            { "LPO2.5", "Isaac Newton 2.5m Telescope",
              "W 17 52 39.5", "N 28 45 43.2", "2336.0" },
/* JKT ( Gemini, April 1987 ) */
            { "LPO1", "Jacobus Kapteyn 1m Telescope",
              "W 17 52 41.2", "N 28 45 39.9", "2364.0" },
/* Lick 120" ( S.L.Allen, Private communication, 2002 ) */
            { "LICK120", "Lick 120 inch",
              "W 121 38 13.689", "N 37 20 34.931", "1286.0" },
/* MMT 6.5m conversion ( MMT Observatory website ) */
            { "MMT", "MMT 6.5m, Mt Hopkins",
              "W 110 53 4.4", "N 31 41 19.6", "2608.0" },
/* Victoria B.C. 1.85m ( 1984 Almanac ) */
            { "VICBC", "Victoria B.C. 1.85 metre",
              "W 123 25 1.18", "N 48 31 11.9", "238.0" },
/* Las Campanas ( 1983 Almanac ) */
            { "DUPONT", "Du Pont 2.5m Telescope, Las Campanas",
              "W7 0 42 9.0", "S 29 0 11.", "2280.0" },
/* Mt Hopkins 1.5m ( 1983 Almanac ) */
            { "MTHOP1.5", "Mt Hopkins 1.5 metre",
              "W 110 52 39.00", "N 31 40 51.4", "2344.0" },
/* Mt Stromlo 74" ( 1983 Almanac ) */
            { "STROMLO74", "Mount Stromlo 74 inch",
              "E 149 0 27.59", "S 35 19 14.3", "767.0" },
/* ANU 2.3m, SSO ( Gary Hovey ) */
            { "ANU2.3", "Siding Spring 2.3 metre",
              "E 149 3 40.3", "S 31 16 24.1", "1149.0" },
/* Greenbank 140' ( 1983 Almanac ) */
            { "GBVA140", "Greenbank 140 foot",
              "W 79 50 9.61", "N 38 26 15.4", "881.0" },
/* Cerro Tololo 4m ( 1982 Almanac ) */
            { "TOLOLO4M", "Cerro Tololo 4 metre",
              "W 70 48 53.6", "S 30 9 57.8", "2235.0" },
/* Cerro Tololo 1.5m ( 1982 Almanac ) */
            { "TOLOLO1.5M", "Cerro Tololo 1.5 metre",
              "W 70 48 54.5", "S 30 9 56.3", "2225.0" },
/* Tidbinbilla 64m ( 1982 Almanac ) */
            { "TIDBINBLA", "Tidbinbilla 64 metre",
              "E 148 58 48.20", "S 35 24 14.3", "670.0" },
/* Bloemfontein 1.52m ( 1981 Almanac ) */
            { "BLOEMF", "Bloemfontein 1.52 metre",
              "E 26 24 18.", "S 29 2 18.", "1387.0" },
/* Bosque Alegre 1.54m ( 1981 Almanac ) */
            { "BOSQALEGRE", "Bosque Alegre 1.54 metre",
              "W 64 32 48.0", "S 31 35 53.0", "1250.0" },
/* USNO 61" astrographic reflector, Flagstaff ( 1981 Almanac ) */
            { "FLAGSTF61", "USNO 61 inch astrograph, Flagstaff",
              "W 111 44 23.6", "N 35 11 2.5", "2316.0" },
/* Lowell 72" ( 1981 Almanac ) */
            { "LOWELL72", "Perkins 72 inch, Lowell",
              "W 111 32 9.3", "N 35 5 48.6", "2198.0" },
/* Harvard 1.55m ( 1981 Almanac ) */
            { "HARVARD", "Harvard College Observatory 1.55m",
              "W 71 33 29.32", "N 42 30 19.0", "185.0" },
/* Okayama 1.88m ( 1981 Almanac ) */
            { "OKAYAMA", "Okayama 1.88 metre",
              "E 133 35 47.29", "N 34 34 26.1", "372.0" },
/* Kitt Peak Mayall 4m ( 1981 Almanac ) */
            { "KPNO158", "Kitt Peak 158 inch",
              "W 111 35 57.61", "N 31 57 50.3", "2120.0" },
/* Kitt Peak 90 inch ( 1981 Almanac ) */
            { "KPNO90", "Kitt Peak 90 inch",
              "W 111 35 58.24", "N 31 57 46.9", "2071.0" },
/* Kitt Peak 84 inch ( 1981 Almanac ) */
            { "KPNO84", "Kitt Peak 84 inch",
              "W 111 35 51.56", "N 31 57 29.2", "2096.0" },
/* Kitt Peak 36 foot ( 1981 Almanac ) */
            { "KPNO36FT", "Kitt Peak 36 foot",
              "W 111 36 51.12", "N 31 57 12.1", "1939.0" },
/* Kottamia 74" ( 1981 Almanac ) */
            { "KOTTAMIA", "Kottamia 74 inch",
              "E 31 49 30.", "N 29 55 54.", "476.0" },
/* La Silla 3.6m ( 1981 Almanac ) */
            { "ESO3.6", "ESO 3.6 metre",
              "W 70 43 36.", "S 29 15 36.", "2428.0" },
/* Mauna Kea 88 inch
  ( IfA website, Richard Wainscoat ) */
            { "MAUNAK88", "Mauna Kea 88 inch",
              "W 155 28 9.96", "N 19 49 22.77", "4213.6" },
/* UKIRT
  ( Ifa website, Richard Wainscoat ) */
            { "UKIRT", "UK Infra Red Telescope",
              "W 155 28 13.18", "N 19 49 20.75", "4198.5" },
/* Quebec 1.6m ( 1981 Almanac ) */
            { "QUEBEC1.6", "Quebec 1.6 metre",
              "W 71 9 9.7", "N 45 27 20.6", "1114.0" },
/* Mt Ekar 1.82m ( 1981 Almanac ) */
            { "MTEKAR", "Mt Ekar 1.82 metre",
              "E 11 34 15.", "N 45 50 48.", "1365.0" },
/* Mt Lemmon 60" ( 1981 Almanac ) */
            { "MTLEMMON60", "Mt Lemmon 60 inch",
              "W 110 42 16.9", "N 32 26 33.9", "2790.0" },
/* Mt Locke 2.7m ( 1981 Almanac ) */
            { "MCDONLD2.7", "McDonald 2.7 metre",
              "W 104 1 17.60", "N 30 40 17.7", "2075.0" },
/* Mt Locke 2.1m ( 1981 Almanac ) */
            { "MCDONLD2.1", "McDonald 2.1 metre",
              "W 104 1 20.10", "N 30 40 17.7", "2075.0" },
/* Palomar 200" ( 1981 Almanac ) */
            { "PALOMAR200", "Palomar 200 inch",
              "W 116 51 50.", "N 33 21 22.", "1706.0" },
/* Palomar 60" ( 1981 Almanac ) */
            { "PALOMAR60", "Palomar 60 inch",
              "W 116 51 31.", "N 33 20 56.", "1706.0" },
/* David Dunlap 74" ( 1981 Almanac ) */
            { "DUNLAP74", "David Dunlap 74 inch",
              "W 79 25 20.", "N 43 51 46.", "244.0" },
/* Haute Provence 1.93m ( 1981 Almanac ) */
            { "HPROV1.93", "Haute Provence 1.93 metre",
              "E 5 42 46.75", "N 43 55 53.3", "665.0" },
/* Haute Provence 1.52m ( 1981 Almanac ) */
            { "HPROV1.52", "Haute Provence 1.52 metre",
              "E 5 42 43.82", "N 43 56 0.2", "667.0" },
/* San Pedro Martir 83" ( 1981 Almanac ) */
            { "SANPM83", "San Pedro Martir 83 inch",
              "W 115 27 47.", "N 31 2 38.", "2830.0" },
/* Sutherland 74" ( 1981 Almanac ) */
            { "SAAO74", "Sutherland 74 inch",
              "E 20 48 44.3", "S 32 22 43.4", "1771.0" },
/* Tautenburg 2m ( 1981 Almanac ) */
            { "TAUTNBG", "Tautenburg 2 metre",
              "E 11 42 45.", "N 50 58 51.", "331.0" },
/* Catalina 61" ( 1981 Almanac ) */
            { "CATALINA61", "Catalina 61 inch",
              "W 110 43 55.1", "N 32 25 0.7", "2510.0" },
/* Steward 90" ( 1981 Almanac ) */
            { "STEWARD90", "Steward 90 inch",
              "W 111 35 58.24", "N 31 57 46.9", "2071.0" },
/* Russian 6m ( 1981 Almanac ) */
            { "USSR6", "USSR 6 metre",
              "E 41 26 30.0", "N 43 39 12.", "2100.0" },
/* Arecibo 1000' ( 1981 Almanac ) */
            { "ARECIBO", "Arecibo 1000 foot",
              "W 66 45 11.1", "N 18 20 36.6", "496.0" },
/* Cambridge 5km ( 1981 Almanac ) */
            { "CAMB5KM", "Cambridge 5km",
              "E 0 2 37.23", "N 52 10 12.2", "17.0" },
/* Cambridge 1 mile ( 1981 Almanac ) */
            { "CAMB1MILE", "Cambridge 1 mile",
              "E 0 2 21.64", "N 52 9 47.3", "17.0" },
/* Bonn 100m ( 1981 Almanac ) */
            { "EFFELSBERG", "Effelsberg 100 metre",
              "E 6 53 1.5", "N 50 31 28.6", "366.0" },
/* Greenbank 300' ( 1981 Almanac - defunct ) */
            { "GBVA300", "Greenbank 300 foot",
              "W 79 50 56.36", "N 38 25 46.3", "894.0" },
/* Jodrell Bank Mk 1 ( 1981 Almanac ) */
            { "JODRELL1", "Jodrell Bank 250 foot",
              "W 2 18 25.", "N 53 14 10.5", "78.0" },
/* Australia Telescope Parkes Observatory
  ( Peter te Lintel Hekkert ) */
            { "PARKES", "Parkes 64 metre",
              "E 148 15 44.3591", "S 32 59 59.8657", "391.79" },
/* VLA ( 1981 Almanac ) */
            { "VLA", "Very Large Array",
              "W 107 37 3.82", "N 34 4 43.5", "2124.0" },
/* Sugar Grove 150' ( 1981 Almanac ) */
            { "SUGARGROVE", "Sugar Grove 150 foot",
              "W 79 16 23.", "N 38 31 14.", "705.0" },
/* Russian 600' ( 1981 Almanac ) */
            { "USSR600", "USSR 600 foot",
              "E 41 35 25.5", "N 43 49 32.", "973.0" },
/* Nobeyama 45 metre mm dish ( based on 1981 Almanac entry ) */
            { "NOBEYAMA", "Nobeyama 45 metre",
              "E 138 29 12.", "N 35 56 19.", "1350.0" },
/* James Clerk Maxwell 15 metre mm telescope, Mauna Kea
  ( IfA website, Richard Wainscoat, height from I.Coulson ) */
            { "JCMT", "JCMT 15 metre",
              "W 155 28 37.20", "N 19 49 22.11", "4111.0" },
/* ESO 3.5 metre NTT, La Silla ( K.Wirenstrand ) */
            { "ESONTT", "ESO 3.5 metre NTT",
              "W 70 43 7.", "S 29 15 30.", "2377.0" },
/* St Andrews University Observatory ( 1982 Almanac ) */
            { "ST.ANDREWS", "St Andrews",
              "W 2 48 52.5", "N 56 20 12.", "30.0" },
/* Apache Point 3.5 metre ( R.Owen ) */
            { "APO3.5", "Apache Point 3.5m",
              "W 105 49 11.56", "N 32 46 48.96", "2809.0" },
/* W.M.Keck Observatory, Telescope 1 ( site survey )
  ( William Lupton ) */
            { "KECK1", "Keck 10m Telescope #1",
              "W 155 28 28.99", "N 19 49 33.41", "4160.0" },
/* Tautenberg Schmidt ( 1983 Almanac ) */
            { "TAUTSCHM", "Tautenberg 1.34 metre Schmidt",
              "E 11 42 45.0", "N 50 58 51.0", "331.0" },
/* Palomar Schmidt ( 1981 Almanac ) */
            { "PALOMAR48", "Palomar 48-inch Schmidt",
              "W 116 51 32.0", "N 33 21 26.0", "1706.0" },
/* UK Schmidt, Siding Spring ( 1983 Almanac ) */
            { "UKST", "UK 1.2 metre Schmidt, Siding Spring",
              "E 149 4 12.8", "S 31 16 27.8", "1145.0" },
/* Kiso Schmidt, Japan ( 1981 Almanac ) */
            { "KISO", "Kiso 1.05 metre Schmidt, Japan",
              "E 137 37 42.2", "N 35 47 38.7", "1130.0" },
/* ESO Schmidt, La Silla ( 1981 Almanac ) */
            { "ESOSCHM", "ESO 1 metre Schmidt, La Silla",
              "W 70 43 46.5", "S 29 15 25.8", "2347.0" },
/* Australia Telescope Compact Array ( WGS84 coordinates of Station 35, 
  Mark Calabretta ) */
            { "ATCA", "Australia Telescope Compact Array",
              "E 149 33 0.500", "S 30 18 46.385", "236.9" },
/* Australia Telescope Mopra Observatory
  ( Peter te Lintel Hekkert ) */
            { "MOPRA", "ATNF Mopra Observatory",
              "E 149 5 58.732", "S 31 16 4.451", "850.0" },
/* Subaru telescope, Mauna Kea
  ( IfA website, Richard Wainscoat ) */
            { "SUBARU", "Subaru 8m telescope",
              "W 155 28 33.67", "N 19 49 31.81", "4163.0" },
/* Canada-France-Hawaii Telescope, Mauna Kea
  ( IfA website, Richard Wainscoat ) */
            { "CFHT", "Canada-France-Hawaii 3.6m Telescope",
              "W 155 28 7.95", "N 19 49 30.91", "4204.1" },
/* W.M.Keck Observatory, Telescope 2
  ( William Lupton ) */
            { "KECK2", "Keck 10m Telescope #2",
              "W 155 28 27.24", "N 19 49 35.62", "4159.6" },
/* Gemini North, Mauna Kea
  ( IfA website, Richard Wainscoat ) */
            { "GEMININ", "Gemini North 8-m telescope",
              "W 155 28 8.57", "N 19 49 25.69", "4213.4" },
/* Five College Radio Astronomy Observatory
  ( Tim Jenness ) */
            { "FCRAO", "Five College Radio Astronomy Obs",
              "W 72 20 42.0", "N 42 23 30.0", "314.0" },
/* NASA Infra Red Telescope Facility
  ( IfA website, Richard Wainscoat ) */
            { "IRTF", "NASA IR Telescope Facility, Mauna Kea",
              "W 155 28 19.20", "N 19 49 34.39", "4168.1" },
/* Caltech Submillimeter Observatory
  ( IfA website, Richard Wainscoat; height estimated ) */
            { "CSO", "Caltech Sub-mm Observatory, Mauna Kea",
              "W 155 28 31.79", "N 19 49 20.78", "4080.0" },
/* ESO VLT, UT1
  ( ESO website, VLT Whitebook Chapter 2 ) */
            { "VLT1", "ESO VLT, Paranal, Chile: UT1",
              "W 70 24 11.642", "S 24 37 33.117", "2635.43" },
/* ESO VLT, UT2
  ( ESO website, VLT Whitebook Chapter 2 ) */
            { "VLT2", "ESO VLT, Paranal, Chile: UT2",
              "W 70 24 10.855", "S 24 37 31.465", "2635.43" },
/* ESO VLT, UT3
  ( ESO website, VLT Whitebook Chapter 2 ) */
            { "VLT3", "ESO VLT, Paranal, Chile: UT3",
              "W 70 24 9.896", "S 24 37 30.300", "2635.43" },
/* ESO VLT, UT4
  ( ESO website, VLT Whitebook Chapter 2 ) */
            { "VLT4", "ESO VLT Paranal, Chile: UT4",
              "W 70 24 8.000", "S 24 37 31.000", "2635.43" },
/* Gemini South, Cerro Pachon
  ( GPS readings by Pat Wallace ) */
            { "GEMINIS", "Gemini South 8-m telescope",
              "W 70 44 11.5", "S 30 14 26.7", "2738.0" },
/* Cologne Observatory for Submillimeter Astronomy ( KOSMA )
  ( Holger Jakob ) */
            { "KOSMA3M", "KOSMA 3m telescope, Gornergrat",
              "E 7 47 3.48", "N 45 58 59.772", "3141.0" },
/* Magellan 1, 6.5m telescope at Las Campanas
  ( Skip Schaller ) */
            { "MAGELLAN1", "Magellan 1, 6.5m, Las Campanas",
              "W 70 41 31.9", "S 29 0 51.7", "2408.0" },
/* Magellan 2, 6.5m telescope at Las Campanas
  ( Skip Schaller ) */
            { "MAGELLAN2", "Magellan 2, 6.5m, Las Campanas",
              "W 70 41 33.5", "S 29 0 50.3", "2408.0" },
/* Null record to end list */
            { null, null, null, null , null }
    };
}
