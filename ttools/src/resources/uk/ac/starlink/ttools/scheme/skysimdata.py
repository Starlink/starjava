import stilts;

in_cat = "gaia_source.colfits"
out_table = "skysimdata.fits"
level = 5

gsource = stilts.tread(in_cat)
quants = ["gmag", "rmag", "b_r"]

gsrc = gsource
gsrc = gsrc.cmd_colmeta("-name", "gmag", "phot_g_mean_mag")
gsrc = gsrc.cmd_colmeta("-name", "rmag", "phot_rp_mean_mag")
gsrc = gsrc.cmd_colmeta("-name", "b_r", "bp_rp")
gsrc = gsrc.cmd_keepcols("l b gmag rmag b_r")

def hpxname(level):
   return "hpx%d" % level

def tcombine(level, combine):
   tcomb = stilts.tskymap(in_=gsrc, lon="l", lat="b",
                          tiling=hpxname(level),
                          combine=combine, cols=' '.join(quants))
   for q in quants:
      tcomb = tcomb.cmd_replacecol(q, '(float)'+q)
   return tcomb

def tstats(level):
   tmeans = tcombine(level, "mean")
   tstdvs = tcombine(level, "stdev")
   tpair = stilts.tmatch2(in1=tmeans, in2=tstdvs,
                          find='best', fixcols='all', join='1and2',
                          matcher='exact',
                          values1=hpxname(level), values2=hpxname(level),
                          suffix1='', suffix2="_stdev",
                          progress='none')
   tpair = tpair.cmd_delcols("hpx5_stdev count_stdev")
   tpair = tpair.cmd_healpixmeta("-level", level, "-column", hpxname(level),
                                 "-nested", "-csys", "G")
   return tpair

tstats(level).write(out_table)

