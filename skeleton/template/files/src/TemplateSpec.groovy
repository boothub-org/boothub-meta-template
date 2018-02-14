{{javaComment 'license-header.txt'~}}
package {{templatePackage}}

import java.nio.file.Paths
import org.boothub.Initializr
import org.boothub.gradle.*
import spock.lang.Specification
import spock.lang.Unroll

class {{projectNameAsJavaClass}}Spec extends Specification {

    private static final String TEMPLATE_DIR = getPath("/template")

{{#ifb (compare modularity '!=' 'SINGLE')~}}
    private static final String MODULE_STRANGE_LIB = 'strange_lib'
    private static final String MODULE_SPOOKY_UI = 'spooky_ui'
    private static final String MODULE_WEIRD_APP = 'weird_app'
    private static final String MODULE_WEIRD_NAME = MODULE_WEIRD_APP.replaceAll('_', '-')
{{~/ifb}}

{{#ifb (compare projectType '!=' LIBRARY)~}}
//    private static final String BASE_PATH = 'org/bizarre_soft/weird_app'
    private static final String BASE_PATH = 'org/bizarre_soft'
    private static final APP_MAIN_CLASS = 'WeirdAppMain'
//    private static final APP_MAIN_CLASS_PATH = "$BASE_PATH/${APP_MAIN_CLASS}.class"
    private static final APP_MAIN_CLASS_PATH = "$BASE_PATH/weird_app/${APP_MAIN_CLASS}.class"
{{~/ifb}}

{{#ifb (compare modularity '!=' 'MULTI')~}}
    private static final String CONTEXT_SINGLE = getPath("/base-context-single.yml")
{{~/ifb}}
{{#ifb (compare modularity '!=' 'SINGLE')~}}
    private static final String CONTEXT_MULTI = getPath("/base-context-multi.yml")
{{~/ifb}}

    private static String getPath(String resourcePath) {
        def resource = {{projectNameAsJavaClass}}Spec.class.getResource(resourcePath)
        assert resource : "Resource not available: $resourcePath"
        Paths.get(resource.toURI()).toAbsolutePath().toString()
    }

{{~#ifb (compare modularity '!=' 'SINGLE')}}
    private static boolean checkBuildArtifacts(GradleTemplateBuilder builder, String module,
                                               List<String> fileNames, List<String> forbiddenFileNames) {
        def checker = new BuildChecker(builder, module, "$BASE_PATH/$module")

        checker.checkClassesAndJars('sources', ['java'], fileNames, forbiddenFileNames)
        checker.checkClassesAndJars('jar', ['class'], fileNames, forbiddenFileNames)
        true
    }
{{~/ifb}}

{{#ifb (compare modularity '!=' 'MULTI')~}}
    def "should create a valid artifact using base-context-single.yml"() {
        when:
        def artifacts = new GradleTemplateBuilder(TEMPLATE_DIR)
                .withContextFile(CONTEXT_SINGLE)
                .runGradleBuild()
                .artifacts
        def jars = artifacts['jar']

        then:
        jars.size() == 1
        jars[0].getEntry(APP_MAIN_CLASS_PATH) != null
    }

{{#ifb (compare projectType '!=' LIBRARY)~}}
    def "should create a valid application using base-context-single.yml"() {
        when:
        def context = new Initializr(TEMPLATE_DIR).createContext(CONTEXT_SINGLE)

        then:
        new OutputChecker(TEMPLATE_DIR, context)
                .checkOutput("Hello from $context.appMainClass!")
    }
{{~/ifb}}
{{~/ifb}}

{{#ifb (compare modularity '!=' 'SINGLE')~}}
    @Unroll
    def "should build valid artifacts for module #module using base-context-multi.yml"() {
        given:
        def builder = new GradleTemplateBuilder(TEMPLATE_DIR)
                .withContextFile(CONTEXT_MULTI)

        expect:
        checkBuildArtifacts(builder, module, fileNames, forbiddenFileNames)

        where:
        module             | fileNames           | forbiddenFileNames                           
        MODULE_SPOOKY_UI   | ["SpookyUiUtil"]    | ["SpookyUiMain"]   
        MODULE_STRANGE_LIB | ["StrangeLibUtil"]  | ["StrangeLibMain"]
        MODULE_WEIRD_APP   | ["WeirdAppMain"]    | ["WeirdAppUtil"]                       
    }

{{#ifb (compare projectType '!=' LIBRARY)~}}
    def "should create a valid application using base-context-multi.yml"() {
        when:
        def context = new Initializr(TEMPLATE_DIR).createContext(CONTEXT_MULTI)

        then:
        new OutputChecker(TEMPLATE_DIR, context, MODULE_WEIRD_NAME)
                .checkOutput("Hello from $context.appMainClass!")
    }
{{~/ifb}}
{{~/ifb}}
}
