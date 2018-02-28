stilts tpipe \
    in='http://vizier.u-strasbg.fr/viz-bin/votable?-source=J%2fApJ%2f833%2f119&-oc.form=dec&-out.meta=DhuL&-out.all&-out.max=500#2' \
    ifmt=votable \
    cmd='colmeta -name ra _RA.icrs' \
    cmd='colmeta -name dec _DE.icrs' \
    cmd='colmeta -name parallax varpi' \
    cmd='colmeta -name parallax_error e_varpi' \
    cmd='colmeta -name source_id Source' \
    cmd='keepcols "source_id ra dec parallax parallax_error rCep sigmaRCep* r*Exp1 r*Exp2"' \
    ofmt=votable-binary

