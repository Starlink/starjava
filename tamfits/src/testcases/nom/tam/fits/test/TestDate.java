package nom.tam.fits.test;

import nom.tam.fits.FitsDate;
import java.util.Date;

public class TestDate {
    
    public static void main(String[] args) {
	
	System.out.println(FitsDate.getFitsDateString());
	System.out.println(FitsDate.getFitsDateString(new Date()));
	System.out.println(FitsDate.getFitsDateString(new Date(), true));
	System.out.println(FitsDate.getFitsDateString(new Date(), false));
    }
}
			   
