package uk.ac.starlink.gbin;

public interface GbinTableProfile {
    boolean isReadMeta();
    boolean isTestMagic();
    boolean isHierarchicalNames();
    String getNameSeparator();
    boolean isSortedMethods();
    String[] getIgnoreNames();
}
