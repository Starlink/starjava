package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Identifier object for PlotLayers.
 * Two plot layers which have equal LayerIds will produce the same
 * plotting results.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2014
 */
@Equality
public class LayerId {

    private final Plotter plotter_;
    private final DataSpec dataSpec_;
    private final DataGeom dataGeom_;
    private final Style style_;

    /**
     * Constructor.
     *
     * @param  plotter  plotter
     * @param  dataSpec   data specification
     * @param  dataGeom   mapping to graphics space
     * @param  style    layer style
     */
    public LayerId( Plotter plotter, DataSpec dataSpec, DataGeom dataGeom,
                    Style style ) {
        plotter_ = plotter;
        dataSpec_ = dataSpec;
        dataGeom_ = dataGeom;
        style_ = style;
    }

    @Override
    public int hashCode() {
        int code = 9901;
        code = code * 23 + plotter_.hashCode();
        code = code * 23 + PlotUtil.hashCode( dataSpec_ );
        code = code * 23 + PlotUtil.hashCode( dataGeom_ );
        code = code * 23 + PlotUtil.hashCode( style_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LayerId ) {
            LayerId other = (LayerId) o;
            return this.plotter_.equals( other.plotter_ )
                && PlotUtil.equals( this.dataSpec_, other.dataSpec_ )
                && PlotUtil.equals( this.dataGeom_, other.dataGeom_ )
                && PlotUtil.equals( this.style_, other.style_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns a layerId characterising a given plot layer.
     *
     * @param  layer
     * @return  layer id
     */
    public static LayerId createLayerId( PlotLayer layer ) {
        return new LayerId( layer.getPlotter(), layer.getDataSpec(),
                            layer.getDataGeom(), layer.getStyle() );
    }
}
