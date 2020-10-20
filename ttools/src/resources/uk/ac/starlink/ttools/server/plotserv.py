# Defines a plot function that implements the magic for making interactive
# plots appear in Jupyter notebook output cells.

import IPython.core.display as disp
import json

server_url = "%PLOTSERV_URL%"

options = {
    'rate': True,
    'reset': True,
    'help': False,
    'msg': False,
}

_plot_counter = 0

def _plot_html(server_url, words, options):
    global _plot_counter
    _plot_counter += 1
    container_id = "plot-node-%d" % _plot_counter
    return (
        ( '<div id="%s"></div>\n' % container_id )
      + ( '<script>\n' )
      + ( 'requirejs(["plot2"], function(p2) {\n' )
      + ( '   var pNode = plot2.createPlotNode(%s, plot2.wordsToPlotTxt(%s), %s)\n'
            % (json.dumps(server_url), json.dumps(words), json.dumps(options)) )
      + ( '   document.getElementById("%s").appendChild(pNode)\n' % container_id )
      + ( '})\n' )
      + ( '</script>\n' )
    )

def show_plot(server_url, words, options):
    display(disp.HTML(_plot_html(server_url, words, options)))

def plot(words):
    show_plot(server_url, words, options)

display(disp.Javascript('requirejs.config({paths: {"plot2": "%s/plot2Lib"}})' % server_url))
