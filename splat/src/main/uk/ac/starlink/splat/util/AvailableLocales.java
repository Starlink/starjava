package uk.ac.starlink.splat.util;

import java.text.DateFormat;
import java.util.Locale;

public class AvailableLocales {
    static public void main(String[] args) {
        System.out.println( "Default locale = " + Locale.getDefault() );
        Locale list[] = DateFormat.getAvailableLocales();
        for (int i = 0; i < list.length; i++) {
            System.out.print(list[i] + ":");
            System.out.println(list[i].getDisplayName());
        }
    }
}
