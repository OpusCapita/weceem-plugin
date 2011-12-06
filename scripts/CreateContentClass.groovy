includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new Weceem content class") {
    depends(checkVersion, parseArguments)

    promptForName(type: "Content class")

    for ( name in argsMap["params"] ) {
        createArtifact(name: name, suffix: "", type: "WeceemContentClass", path: "grails-app/domain")
        createUnitTest(name: name, suffix: "")
    }
}
