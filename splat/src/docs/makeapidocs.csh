#!/bin/csh -xv

#  Create the API documentation set. This is a restricted set
#  that is supposed to be useful for writing plugins and remote
#  control code, not the full set (no inheritance APIs shown).

if ( -e apidocs ) then
   \rm -r -f apidocs
endif
mkdir apidocs

javadoc -public -d apidocs \
   uk.ac.starlink.splat.iface.SplatBrowser \
   uk.ac.starlink.splat.util.RemoteUtilities \
   uk.ac.starlink.splat.iface.GlobalSpecPlotList \
   uk.ac.starlink.splat.data.SpecData \
   uk.ac.starlink.splat.data.MEMSpecDataImpl \
   uk.ac.starlink.splat.data.SpecDataFactory \
   uk.ac.starlink.splat.plot.PlotControl \
   uk.ac.starlink.splat.util.SpectralFileFilter

javadoc -noinherited -public -doclet doclet.Latex -output apidocs.tex \
   uk.ac.starlink.splat.iface.SplatBrowser \
   uk.ac.starlink.splat.util.RemoteUtilities \
   uk.ac.starlink.splat.iface.GlobalSpecPlotList \
   uk.ac.starlink.splat.data.SpecData \
   uk.ac.starlink.splat.data.MEMSpecDataImpl \
   uk.ac.starlink.splat.data.SpecDataFactory \
   uk.ac.starlink.splat.plot.PlotControl \
   uk.ac.starlink.splat.util.SpectralFileFilter



