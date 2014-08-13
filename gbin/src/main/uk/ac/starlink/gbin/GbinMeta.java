package uk.ac.starlink.gbin;

public interface GbinMeta {
    String buildDescription( boolean showChunkBreakdown );
    Integer getGbinVersionNumber();
    long[] getSolutionIdList();
    Long getTotalElementCount();
    String toString();
}
