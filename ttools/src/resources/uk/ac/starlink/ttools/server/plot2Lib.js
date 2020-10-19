// JavaScript library for working with the STILTS PlotServlet.
// Names exported into the plot2 namespace can be seen at the end of the plot2
// function definition; the main one is the plot2.createPlotNode function.
//
// This library can be used as it stands, or customised to taste,
// or used as a template to write alternative JavaScript clients
// for the PlotServlet HTTP API.  It is certainly capable of
// improvement by a competent JavaScript/HTML5/CSS author.
//
// The behaviour of this library may change in future versions.

// Namespace for exported functions.
var plot2 = (function() {

   // Define boolean keys that may be supplied to the createPlotNode function,
   // to modify the appearance of the plot display element.
   var MSG = "msg";           // option to display info about replot requests
   var RATE = "rate";         // option to add a frame rate indicator
   var RESET = "reset";       // option to add a plot reset button
   var HELP = "help";         // option to add a navigation help button
   var DOWNLOAD = "download"; // option to add an image download button

   // Minimum size allowed for drag resize.
   var MIN_SIZE = 100;

   // Default plot2* xpix/ypix values (total image element dimensions).
   var DFLT_XPIX = 500;
   var DFLT_YPIX = 400;

   // Timeout for transient navigation decorations.
   var DECORATION_AUTO_MILLIS = 500;

   // Initialise plot
   var initPlot = function(servletUrl, plotTxt, sessionId, imgNode,
                           parentNode, options) {

      var plotLoading = false;
      var queued;
      var dragState;
      var isMouseDown = false;
      var transientExpired = false;
      var iseq = 0;
      var loadStart = new Date().getTime();
      var padNode;

      var createSvgNode = function() {
         return document.createElementNS("http://www.w3.org/2000/svg", "svg");
      };
      var transientSvgNode = createSvgNode();
      var staticSvgNode = createSvgNode();
      parentNode.appendChild(transientSvgNode);
      parentNode.appendChild(staticSvgNode);

      var panelNode = document.createElement("DIV");
      panelNode.style = "margin-top: 10px";
      parentNode.appendChild(panelNode);

      if (options[HELP]) {
         var helpUrl =
            servletUrl + "/" + plotTxt.split("&")[0] + "Navigation.html";
         var helpButt = createPopupButton("Nav Help");
         panelNode.appendChild(helpButt);
         panelNode.appendChild(createPadNode());
         var xhr = new XMLHttpRequest();
         xhr.onreadystatechange = function() {
            var txt;
            if (this.readyState == 4) {
                if (this.status == 200) {
                   txt = this.responseText;
                }
                else {
                   txt = "<em>Error downloading help from "
                       + "<a href='" + helpUrl +"'>" + helpUrl + "</a></em>";
                }
                helpButt.txtNode.innerHTML = txt;
            }
         };
         helpButt.txtNode.innerHTML = "<em>... downloading help...</em>";
         xhr.open("GET", helpUrl, true);
         xhr.send();
      }

      var resetNode;
      if (options[RESET]) {
         resetNode = document.createElement("BUTTON");
         panelNode.appendChild(resetNode);
         panelNode.appendChild(createPadNode());
         resetNode.setAttribute("type", "button");
         resetNode.innerHTML = "Reset";
         resetNode.onclick = function() { replot(navTxt("reset")); };
      }

      var dlNode;
      if (options[DOWNLOAD]) {
         // The HTML/CSS/js here could certainly be improved.
         var dlmenuHtml = ""
         var fmts = ['png', 'gif', 'jpeg', 'pdf', 'eps', 'svg'];
         var i;
         for (i in fmts) {
            dlmenuHtml += "<option>" + fmts[i] + "</option>\n";
         }
         dlNode = document.createElement("SPAN");
         var dlButton = document.createElement("BUTTON");
         dlButton.innerHTML = "Download";
         var dlMenu = document.createElement("SELECT");
         dlMenu.innerHTML = dlmenuHtml;
         dlNode.appendChild(dlButton);
         dlNode.appendChild(dlMenu);
         dlButton.onclick = function() {
            var fmt = dlMenu.options[dlMenu.selectedIndex];
            var words = [];
            words.push("sessionId=" + sessionId);
            words.push("format=" + fmt.value);
            var query = words.join("&");
            var dlUrl = servletUrl + "/img/" + plotTxt + "?" + query;
            window.open(dlUrl);
         };
         panelNode.appendChild(dlNode);
         panelNode.appendChild(createPadNode());
      }

      var rateNode;
      var setRate = function(rateTxt) {
         if (rateNode) {
            rateNode.innerHTML = " <b>Frame Time:</b> " + rateTxt + "ms";
         }
      };
      if (options[RATE]) {
         rateNode = document.createElement("SPAN");
         rateNode.style =
            "display: inline-block; width: 12em; font-family: sans-serif";
         panelNode.appendChild(rateNode);
         setRate("");
      }

      var msgNode;
      if (options[MSG]) {
         msgNode = document.createElement("SPAN");
         panelNode.appendChild(msgNode);
      }

      var message = function(txt) {
         if (msgNode) {
            msgNode.innerHTML = "";
            msgNode.appendChild(document.createTextNode(txt));
         }
      };

      var transientTimeout = function() {
         if (isMouseDown) {
            transientExpired = true;
         }
         else {
            overSvg(null, true);
         }
      };

      var overSvg = function(svg, isTransient) {
         var svgTimer;
         var svgNode = isTransient ? transientSvgNode : staticSvgNode;
         var width = 0;
         var height = 0;
         if (svg) {
            width = imgNode.naturalWidth;
            height = imgNode.naturalHeight;
         }
         svgNode.setAttribute("width", width );
         svgNode.setAttribute("height", height );
         svgNode.style.display = "block";
         svgNode.style.position = "absolute";
         svgNode.style.top = imgNode.offsetTop + "px";
         svgNode.style.left = imgNode.offsetLeft + "px";
         svgNode.innerHTML = svg;
         if (isTransient) {
            transientExpired = false;
            if (svgTimer) {
               window.clearTimeout(svgTimer);
            }
            if (svg) {
               svgTimer = window.setTimeout(transientTimeout,
                                            DECORATION_AUTO_MILLIS);
            }
         }
      };

      var replot = function(txt) {
         if (!plotLoading) {
            var words = [];
            words.push("sessionId=" + sessionId);

            // The purpose of the iseq parameter is to force subsequent URLs
            // to be different; otherwise navigation actions with the same
            // gesture can have the same URL so the IMG doesn't get updated.
            words.push("iseq=" + ++iseq);
            if (txt) {
                words.push(txt);
            }
            var query = words.join("&");
            message(query);
            loadStart = new Date().getTime();
            plotLoading = true;

            // We have two ways to update the displayed image.
            if (false) {

               // The first is to reset the src attribute of the IMG element
               // directly to a server URL that will give the latest image.
               // It's straightforward but primitive.
               var imgUrl = servletUrl + "/imgsrc/" + plotTxt + "?" + query;
               imgNode.setAttribute("src", imgUrl);
            }
            else {

               // The other option is to request a JSON data structure which
               // contains the image data embedded as a data-scheme URI.
               // This gives us more control: we can acquire additional
               // things like image bound information and decorations
               // alongside the image itself, and we can also benefit
               // from server knowledge of whether the image has changed
               // (if it's the same as last time, no update inline data
               // is sent).  One downside is that the image data is
               // base64-encoded, so a bit more data is sent over the wire.
               var jsonUrl = servletUrl + "/state/" + plotTxt + "?" + query;
               var result;
               var xhr = new XMLHttpRequest();
               xhr.onreadystatechange = function() {
                  var ctype;
                  if (this.readyState == 4) {
                     ctype = this.getResponseHeader("Content-Type");
                     if (this.status == 200) {
                         result = JSON.parse(this.responseText);
                         if (result.imgSrc) {
                            imgNode.setAttribute("src", result.imgSrc);
                            imgNode.setAttribute("alt",
                                                 "Plot generated by STILTS");
                            if (result.bounds &&
                                typeof parentNode.onbounds == "function") {
                               parentNode.onbounds(result.bounds);
                            }
                         }
                         else {
                            plotLoading = false;
                         }
                         overSvg(result.staticSvg, false);
                         overSvg(result.transientSvg, true);
                     }
                     else {
                        imgNode.setAttribute("src", null);
                        imgNode.setAttribute("alt",
                                             ctype == "text/plain"
                                                ? this.responseText
                                                : "Error: " + this.status );
                        overSvg(null, true);
                        plotLoading = false;
                     }
                  }
               };
               xhr.open("GET", jsonUrl, true);
               xhr.send();
            }
         }
      };

      var queueReplot = function(txt) {
         if (!plotLoading) {
            replot(txt);
         }
         else {
            queued = txt;
         }
      };

      var replotted = function() {
         var elapsed;
         if (loadStart) {
            setRate(new Date().getTime() - loadStart);
            loadStart = null;
         }
         plotLoading = false;
         
         imgNode.setAttribute("width", undefined);
         imgNode.setAttribute("height", undefined);
         if (queued !== undefined) {
            replot(queued);
            queued = undefined;
         }
         if (typeof parentNode.onreplot == "function") {
            parentNode.onreplot();
         }
      };

      var mousedown = function(event) {
         isMouseDown = true;
         var ibutt = eventToIbutton(event);
         var pos = eventToPos(event);
         dragState = {
            "origin": pos,
            "ibutt": ibutt
         };
         var resizeOpts = posToResizeOpts(pos);
         if (ibutt == 1 && (resizeOpts[0] || resizeOpts[1])) {
             dragState.resizeOpts = resizeOpts; 
             dragState.size0 = [imgNode.width, imgNode.height];
             updateResizeCursor(resizeOpts);
         }
         document.addEventListener("mousemove", mousedrag, true);
         document.addEventListener("mouseup", mouseup, true);
         event.stopPropagation();
         event.preventDefault();
      };

      var mousedrag = function(event) {
         var pos = eventToPos(event);
         var dragOrigin = dragState.origin;
         if (dragState.resizeOpts) {
            var sizeX = dragState.size0[0];
            var sizeY = dragState.size0[1];
            if (dragState.resizeOpts[0]) {
               sizeX += pos[0] - dragOrigin[0];
            }
            if (dragState.resizeOpts[1]) {
               sizeY += pos[1] - dragOrigin[1];
            }
            replot(joinArgs([navTxt("resize"),
                             sizeTxt(Math.max(MIN_SIZE, sizeX),
                                     Math.max(MIN_SIZE, sizeY))]));
         }
         else {
            replot(joinArgs([navTxt("drag"),
                             ibuttonTxt(dragState.ibutt),
                             originTxt(dragOrigin),
                             posTxt(pos)]));
         }
         event.stopPropagation();
         event.preventDefault();
      };

      var mousemove = function(event) {
         updateResizeCursor(posToResizeOpts(eventToPos(event)));
      };

      var mouseup = function(event) {
         isMouseDown = false;
         if (transientExpired) {
            transientExpired = false;
            overSvg(null, true);
         }
         if (dragState) {
             var pos = eventToPos(event);
             if (samePosition(dragState.origin, pos)) {
                 if (dragState.ibutt == 1) {
                    if (typeof parentNode.onrow == "function") {
                       clickPoint(pos, parentNode.onrow);
                    }
                 }
                 else {
                    replot(joinArgs([navTxt("click"),
                                    ibuttonTxt(dragState.ibutt), posTxt(pos)]));
                 }                  
             }
             else {
                 queueReplot(joinArgs([navTxt("drag"),
                                       ibuttonTxt(dragState.ibutt),
                                       originTxt(dragState.origin),
                                       posTxt(pos),
                                       "end=true"]));
             }
             dragState = undefined;
             event.stopPropagation();
             event.preventDefault();
         }
         updateResizeCursor();
         document.removeEventListener("mouseup", mouseup, true);
         document.removeEventListener("mousemove", mousedrag, true);
      };

      var wheel = function(event) {
         event.stopPropagation();
         event.preventDefault();

         // Ignore the wheel rotation value, just take its signum;
         // there should be one event each time the wheel moves,
         // and some browsers can return very large values (100).
         var wheelrot = sgn(eventToRot(event));
         if ( wheelrot != 0 ) {
             replot(joinArgs([navTxt("wheel"), posTxt(eventToPos(event)),
                              wheelrotTxt(wheelrot)]));
             return false;
         }
      };

      var sgn = function(value) {
         if (value > 0) {
            return 1;
         }
         else if (value < 0) {
            return -1;
         }
         else {
            return 0;
         }
      }

      var posToResizeOpts = function(pos) {
          return [Math.abs(pos[0] - imgNode.width) < 5,
                  Math.abs(pos[1] - imgNode.height) < 5];
      };

      var updateResizeCursor = function(opts) {
         var cursor = "auto";
         if (opts) {
            if (opts[0] && opts[1]) {
               cursor = "nw-resize";
            }
            else if (opts[0]) {
               cursor = "ew-resize";
            }
            else if (opts[1]) {
               cursor = "ns-resize";
            }
         }
         document.body.style.cursor = cursor;
      };

      var clickPoint = function(pos, onrow) {
         var words = [];
         words.push("sessionId=" + sessionId);
         words.push(posTxt(pos));
         words.push("highlight=true");
         var query = words.join("&");
         var rowUrl = servletUrl + "/row/" + plotTxt + "?" + query;
         var xhr = new XMLHttpRequest();
         var result;
         xhr.onreadystatechange = function() {
            if (this.readyState == 4) {
               if (this.status == 200) {
                  result = JSON.parse(this.responseText);
                  onrow(result);
                  replot(navTxt("none"));
               }
               else {
                  onrow("error: " + this.status);
               }
            }
         };
         xhr.open("GET", rowUrl, true);
         xhr.send();
      };

      var countPoints = function(countConsumer) {
         var words = [];
         words.push("sessionId=" + sessionId);
         var query = words.join("&");
         var countUrl = servletUrl + "/count/" + plotTxt + "?" + query;
         var xhr = new XMLHttpRequest();
         xhr.onreadystatechange = function() {
            if (this.readyState == 4) {
               if (this.status == 200) {
                  countConsumer(this.responseText);
               }
               else {
                  countConsumer("error");
               }
            }
         };
         xhr.open("GET", countUrl, true);
         xhr.send();
      };

      var getPixelLength = function(lengTxt) {
          var result = lengTxt.match(/^(-?[0-9]+)(px|)$/);
          return result ? result[1] : 0;
      };

      var eventToPos = function(event) {
         var plotRect = imgNode.getBoundingClientRect();
         var style = getComputedStyle(imgNode, null);
         var borderLeft = getPixelLength(style.borderLeftWidth);
         var borderTop = getPixelLength(style.borderTopWidth);
         var px = event.clientX - plotRect.left - borderLeft;
         var py = event.clientY - plotRect.top - borderTop;
         return [Math.round(px), Math.round(py)];
      };

      var eventToIbutton = function(event) {
         var jsbut;
         jsbut = event.button;
         if (jsbut == 0) {
            if (event.ctrlKey) {
               return 3;
            }
            else if (event.shiftKey) {
               return 2;
            }
            else {
               return 1;
            }
         }
         else {
            return jsbut + 1;
         }
      };

      var eventToRot = function(event) {
         var mx = Math.abs(event.deltaX);
         var my = Math.abs(event.deltaY);
         var mz = Math.abs(event.deltaZ);
         var mabs = Math.max(mx,my,mz);
         if ( mx == mabs ) {
            return event.deltaX;
         }
         else if ( my == mabs ) {
            return event.deltaY;
         }
         else {
            return event.deltaZ;
         }
      };
      var navTxt = function(cmd) {
         return "navigate=" + cmd;
      };
      var posTxt = function(pos) {
         return "pos=" + pos[0] + "," + pos[1];
      };
      var ibuttonTxt = function(ibutt) {
         return "ibutton=" + ibutt;
      };
      var originTxt = function(pos) {
         return "origin=" + pos[0] + "," + pos[1];
      };
      var wheelrotTxt = function(rot) {
         return "wheelrot=" + rot;
      };
      var sizeTxt = function(x,y) {
         return "size=" + x + "," + y;
      };
      var samePosition = function(pos1, pos2) {
         var thresh = 2;
         var dx = pos1[0] - pos2[0];
         var dy = pos1[1] - pos2[1];
         return dx*dx + dy*dy < thresh*thresh;
      };
      var joinArgs = function(args) {
         return args.join("&");
      };

      var stopper = function(event) {
         event.preventDefault();
      };

      // Add event handlers.  I'm adding them to all the elements that may
      // be uppermost in the image region, which looks like a terrible
      // way to do it, but other things I've tried haven't worked.
      // Actions like clicking and mouse wheel operation are intercepted
      // and prevented from propagating over the image region, since they
      // are used for the navigation.
      var nodes = [imgNode, transientSvgNode, staticSvgNode];
      var i;
      var n;
      for (i in nodes) {
         n = nodes[i];
         n.addEventListener("mousedown", mousedown, true);
         n.addEventListener("mousemove", mousemove, true);
         n.addEventListener("mouseleave", updateResizeCursor, true);
         n.addEventListener("wheel", wheel, true);
         n.addEventListener("load", replotted, true);
         n.addEventListener("click", stopper, true);
         n.addEventListener("contextmenu", stopper, true);
      }
      parentNode.countPoints = countPoints;
      replot(navTxt("reset"));
      message("started");
   };

   var createPadNode = function() {
       var node = document.createElement("SPAN");
       node.style = "display: inline-block; width: 2em";
       return node;
   };

   var createPopupButton = function(buttTxt, docTxt) {
      var isVis = false;
      var popStyle = ""
         + "position: absolute; "
         + "z-index: 1; "
         + "width: 60%; "
         + "background-color: #ccc; "
         + "border-radius: 6px; "
         + "padding: 8px 0; "
         + "";
      var node = document.createElement("DIV");
      node.style = "display: inline-block; font-family: sans-serif";
      var buttNode = document.createElement("BUTTON");
      buttNode.type = "button";
      buttNode.innerHTML = buttTxt;
      var txtNode = document.createElement("DIV");
      txtNode.innerHTML = docTxt;
      var txtVisible = function(flag) {
         txtNode.style =
            "visibility: " + (flag ? "visible" : "hidden") + "; " + popStyle;
      };
      txtVisible(isVis);
      buttNode.onclick = function() { isVis = !isVis; txtVisible(isVis); };
      node.appendChild(buttNode);
      node.appendChild(txtNode);
      node.txtNode = txtNode;
      return node;
   };

   var createCellNode = function(txt) {
      var txtNode = document.createTextNode(txt);
      if (typeof txt == 'string' && txt.match("^https?://")) {
         var anode = document.createElement("A");
         anode.setAttribute("href", txt);
         anode.appendChild(txtNode);
         return anode;
      }
      else {
         return txtNode;
      }
   };

   // Utility function that creates a TABLE element suitable for
   // displaying row data in conjunction with the plotNode onrow event.
   var createRowDisplayTable = function(isHorizontal) {
      var tableNode = document.createElement("TABLE");
      var theadNode = document.createElement("THEAD");
      var tbodyNode = document.createElement("TBODY");
      tableNode.appendChild(theadNode);
      tableNode.appendChild(tbodyNode);
      var initRow;
      var addPair;
      if (isHorizontal) {
         var tr0 = document.createElement("TR");
         var tr1 = document.createElement("TR");
         theadNode.appendChild(tr0);
         tbodyNode.appendChild(tr1);
         initRow = function() {
            tr0.innerHTML = '';
            tr1.innerHTML = '';
         };
         addPair = function(th, td) {
            tr0.appendChild(th);
            tr1.appendChild(td);
         };
      }
      else {
         var initRow = function() {
            tbodyNode.innerHTML = '';
         };
         var addPair = function(th, td) {
            var tr = document.createElement("TR");
            tr.appendChild(th);
            tr.appendChild(td);
            tbodyNode.appendChild(tr);
         };
      }
      tableNode.initRow = initRow;
      tableNode.addPair = addPair;
      tableNode.displayRow = function(rowData) {
         initRow();
         var row = (typeof rowData[0] == 'object') ? rowData[0] : rowData;
         var td;
         var th;
         if (typeof row == 'object') {
            for (colname in row) {
               if (row.hasOwnProperty(colname)) {
                  th = document.createElement("TH");
                  th.style.textAlign = 'left';
                  th.appendChild(document.createTextNode(colname));
                  td = document.createElement("TD");
                  th.style.textAlign = 'left';
                  td.appendChild(createCellNode(row[colname]));
                  addPair(th, td)
               }
            }
         }
      };
      return tableNode;
   };

   // Encode plain text for use as part of a stilts parameter
   // specification in a URL.
   // This uses the standard encodeURIComponent function which mostly
   // does the right thing, but it also fixes it so that "/" characters
   // are represented literally and not percent-encoded (%2F).
   // Tomcat rejects any URL whose path contains a %2F out of hand,
   // returning a 400 Bad Request.  This behaviour is apparently related
   // to the vulnerability CVE-2007-0450 and can be turned off using
   // Tomcat option ALLOW_ENCODED_SLASH, but better to avoid it altogether.
   // Replace %20 by "+" as well, since it's more compact and works here.
   var urlEncode = function(txt) {
      txt = encodeURIComponent(txt);
      txt = txt.replace(/%2[Ff]/g, "/");
      txt = txt.replace(/%20/g, "+");
      return txt;
   };

   // Extracts the value of a given argument from a plotTxt string.
   var getArg = function(plotTxt, argName) {
      var pairs = plotTxt.split("&");
      var i;
      var pair;
      var prefix = argName + "=";
      var plen = prefix.length;
      for (i = 0; i < pairs.length; i++) {
         pair = pairs[i];
         if (pair.substr(0, plen) == prefix) {
            return decodeURIComponent(pair.substr(plen));
         }
      }
   };

   // Utility method to assemble a plotTxt string from a taskName
   // and an array of unencoded [name,value] pairs.
   // Example:
   //    pairsToPlotTxt("plot2plane", [
   //       ["in","data.fits"],
   //       ["layer1","histogram"],
   //       ["x1", "redshift"],
   //   ])
   var pairsToPlotTxt = function(taskName, pairs) {
      var words = [taskName];
      var np = pairs.length;
      var i;
      var pair;
      for (i = 0; i < np; i++) {
         pair = pairs[i];
         words.push(urlEncode(pair[0]) + "=" + urlEncode(pair[1]));
      }
      return words.join("&");
   };

   // Utility method to assemble a plotTxt string from a list of words;
   // first word is the task name.
   // Example:
   //    wordsToPlotTxt(["plot2plane", "in=data.fits",
   //                    "layer1=histogram", "x1=redshift"])
   var wordsToPlotTxt = function(words) {
      var encWords = [];
      var nw = words.length;
      var word;
      var encWord;
      var i;
      var ieq;
      for (i = 0; i < nw; i++) {
         word = words[i];
         ieq = word.indexOf("=");
         encWords.push(ieq >= 0
                         ? (urlEncode(word.substr(0, ieq)) +
                            "=" +
                            urlEncode(word.substr(ieq+1)))
                         : urlEncode(word));
      }
      return encWords.join("&");
   }

   // Creates a DOM node containing a navigable IMG component,
   // by supplying a STILTS server URL and a string defining the
   // plot to be drawn.
   //
   // Arguments:
   //    serverUrl:
   //       base URL of the PlotServlet
   //    plotTxt:
   //       a plot specification like a STILTS plot command,
   //       of the form "taskName&arg1=val1&arg2=val2...",
   //       where the args and vals may be application/x-www-form-encoded.
   //    options (optional):
   //       an object with name/boolean entries giving options for the plot
   //
   // Example:
   //
   //    var serverUrl = "http://localhost:2112/stilts/plot";
   //    var plotSpec = "plot2plane&in=data.fits&layer1=histogram&x1=redshift";
   //    var opts = {}
   //    opts[plot2.RATE] = true;
   //    var plotNode = plot2.createPlotNode(serverUrl, plotSpec, opts);
   //
   // The "countPoints(countConsumer)" member of the returned object
   // can be used to count the points actually plotted in the most recent plot.
   // Note this may be an expensive operation for large plots.
   //
   // If the "onreplot" member of the returned object is set to a function,
   // it will be invoked every time the plot image changes.
   //
   // If the "onbounds" member of the returned object is set to a function,
   // it will be invoked when the plot data bounds become available;
   // this only happens for planar and cubic plots.
   // The argument of the onbounds function will be the content of the
   // "bounds" member returned from the json request, i.e. a
   // 2- or 3-element array of (lower,upper) data bound pairs giving
   // the extent of the plot.
   //
   // If the "onrow" member of the returned object is set to a function,
   // that function will be invoked when the users clicks on or near a point.
   // The single rowData argument will be an array of objects, one element
   // for each table represented in the plot, with each element being a
   // column-name:column-value map for the row indicated
   // (the createRowDisplayTable function provides an example element
   // for displaying such row information).
   var createPlotNode = function(servletUrl, plotTxt, options) {
      var sessionId = Math.random().toString().substr(2);
      var xpix = getArg(plotTxt, "xpix");
      var ypix = getArg(plotTxt, "ypix");

      var imgNode = document.createElement("IMG");
      imgNode.setAttribute("id", sessionId);
      imgNode.setAttribute("width", xpix || DFLT_XPIX);
      imgNode.setAttribute("height", ypix || DFLT_YPIX);
      imgNode.setAttribute("alt", "Plot generated by STILTS");
      imgNode.style.borderColor = "grey";
      imgNode.style.borderWidth = "1px";
      imgNode.style.borderStyle = "solid";
      imgNode.style.position = "relative";

      var containerNode = document.createElement("DIV");
      containerNode.style.position = "relative";
      containerNode.appendChild(imgNode);
      initPlot(servletUrl, plotTxt, sessionId, imgNode, containerNode,
               options ? options : {});
      return containerNode;
   }

   var plot2exports = {};
   plot2exports.createPlotNode = createPlotNode;
   plot2exports.wordsToPlotTxt = wordsToPlotTxt;
   plot2exports.pairsToPlotTxt = pairsToPlotTxt;
   plot2exports.urlEncode = urlEncode;
   plot2exports.MSG = MSG;
   plot2exports.RATE = RATE;
   plot2exports.RESET = RESET;
   plot2exports.HELP = HELP;
   plot2exports.DOWNLOAD = DOWNLOAD;
   plot2exports.createRowDisplayTable = createRowDisplayTable;
   return plot2exports;
})();
