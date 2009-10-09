eventCreateWarStart = { name, stagingPath ->
    def resPath = "${stagingDir}/WEB-INF/classes/org/weceem/resources"
    ant.mkdir(dir:resPath)
    ant.copy(todir:resPath) {
        fileset(dir:"${weceemPluginDir}/src/groovy/org/weceem/resources", includes:"*.zip")
    }
}
