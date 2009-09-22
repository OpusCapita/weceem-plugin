dojo.provide("weceem.LoadingViewLock");

dojo.require("dijit.Dialog");

/*
  author: Sergei Shushkevich
*/
dojo.declare("weceem.LoadingViewLock", [dijit._Widget, dijit._Templated], {

  id: "",
  message: "",
  templateString: '<div id="${id}" class="loadingViewLock" style="display: none;">${message}</div>',

  duration: 200,

  _underlay: null,
  _fadeIn: null,
  _fadeOut: null,

  postCreate: function() {
    var self = this;
    this._underlay = new dijit.DialogUnderlay({id: "loadingViewLock_underlay"});
    this._fadeIn = dojo.fx.combine([
      dojo.fadeIn({
        node: this._underlay.domNode,
        duration: this.duration,
        onBegin: dojo.hitch(this._underlay, "show")}),
      dojo.fadeIn({
        node: this.domNode,
        duration: this.duration,
        onBegin: function() {
          self.setPosition();
          self.domNode.style.display = "";
        }
      })
    ]);
    this._fadeOut = dojo.fx.combine([
      dojo.fadeOut({
        node: this._underlay.domNode,
        duration: this.duration,
        onEnd: dojo.hitch(this._underlay, "hide")}),
      dojo.fadeOut({
        node: this.domNode,
        duration: this.duration,
        onEnd: function() {
          self.domNode.style.display = "none";
        }
      })
    ]);
  },

  setPosition: function() {
    var viewport = dijit.getViewport();
    var mb = dojo.marginBox(this.domNode);
    var style = this.domNode.style;
    style.left = Math.floor((viewport.l + (viewport.w - mb.w)/2)) + "px";
    style.top = Math.floor((viewport.t + (viewport.h - mb.h)/2)) + "px";
  },

  lockView: function() {
    this._fadeIn.play();
  },

  unlockView: function() {
    this._fadeIn.stop();
    this._fadeOut.play();
  }
});