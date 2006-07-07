package uk.ac.starlink.votable.soap;

import org.apache.axis.Version;

public class AxisOK {

    /**
     * Indicates whether it's OK to attempt axis calls.  For certain
     * combinations of Axis and the JRE (1.1 and 1.5 respectively)
     * it's known to fail, so there's no point doing the tests here.
     * Problem should go away at Axis 1.2.
     */
    public static boolean isOK() {
        String axisVersion = Version.getVersion()
                            .replaceFirst( ".*(1\\.[0-9]).*\\s*.*", "$1" );
        String jvmVersion = System.getProperty( "java.runtime.version" )
                           .replaceFirst( ".*(1\\.[0-9]\\.[0-9]).*", "$1" );
        if ( axisVersion.startsWith( "1.1" ) && 
             jvmVersion.startsWith( "1.5" ) ) {
            System.out.println( "AXIS 1.1 won't work with J2SE 1.5 - " +
                                "skipping test" );
            return false;
        }
        return true;
    }

}
