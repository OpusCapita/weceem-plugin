# Weceem CMS Plugin for Grails

This is the code for the free Open Source [Weceem](http://weceem.org) plugin for [Grails](http://grails.org).

This is now compatible with Grails 2 and higher.

This plugin is the full implementation of a pure-Grails Content Management System that you can embed into your own Grails applications. You can create customized CMS functionality or a hybrid application/CMS to provide user-editable content in production.

Security is completely decoupled so that you can plug in whatever security mechanism you are using, and you can customize the look and feel of the user interface to suit your application.

Full documentation is available on the [Weceem site](http://weceem.org).

## Installing the plugin into your Grails application

To install the plugin into an existing Grails application, you can simply run `grails install-plugin weceem` or better, add the plugin as a dependency in `BuildConfig.groovy`:

    runtime ':weceem:1.1.3-SNAPSHOT'
   
You then must configure a couple of things for it to operate correctly:

* Set `grails.mime.file.extensions = false` in Config.groovy
* Remove any URL mappings that are promiscuous i.e. `/$controller/$action` or similar as they will clash with Weceem's URL mappings for content unless...
* You should set `weceem.content.prefix` in Config.groovy to a URL prefix that demarcates your content if you want to use your own controllers with Weceem
* Uninstall the `Resources` plugin or keep it but set `grails.resources.adhoc.excludes` to exclude everything served by Weceem (see above note on setting a content URL prefix) because Weceem must serve this content and Resources assumes that `/css` and so on are static resources

See also the [Weceem Spring Security Plugin](http://github.com/jCatalog/weceem-spring-security) which provides a
simple bridge to your Spring Security domain classes, and [Weceem App](http://github.com/jCatalog/weceem-app) for
a standalone fully-functioning CMS from which you can build a WAR without coding.
