# Weceem CMS Plugin for Grails

This is the code for the free Open Source [Weceem](http://weceem.org) plugin for [Grails](http://grails.org).

This plugin is the full implementation of a pure-Grails Content Management System that you can
embed into your own Grails applications. You can create customized CMS functionality or a hybrid
application/CMS to provide user-editable content in production.

Security is completely decoupled so that you can plug in whatever security mechanism you are using, 
and you can customize the look and feel of the user interface to suit your application.

Full documentation is available on the [Weceem site](http://weceem.org)

To install the plugin into an existing Grails application, you can simply run:

    grails install-plugin weceem
    
See also the [Weceem Spring Security Plugin](http://github.com/jCatalog/weceem-spring-security) which provides a
simple bridge to your Spring Security domain classes, and [Weceem App](http://github.com/jCatalog/weceem-app) for
a standalone fully-functioning CMS from which you can build a WAR without coding.