
/**
 * Include the default space template ZIPs in the WAR
 */
eventCreateWarStart = { name, stagingDir ->
    def resPath = "${stagingDir}/WEB-INF/classes/org/weceem/resources"
    ant.mkdir(dir:resPath)
    ant.copy(todir:resPath) {
        fileset(dir:"${weceemPluginDir}/src/groovy/org/weceem/resources", includes:"*.zip")
    }
}
