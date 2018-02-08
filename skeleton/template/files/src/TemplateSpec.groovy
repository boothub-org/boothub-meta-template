{{javaComment 'license-header.txt'~}}
package {{templatePackage}}

import org.boothub.Initializr
import org.boothub.gradle.GradleTemplateBuilder
import org.boothub.gradle.OutputChecker
import spock.lang.Specification

import java.nio.file.Paths

class {{projectNameAsJavaClass}}Spec extends Specification {
    def "TODO"() {
        expect:
        1 + 1 == 2
    }

/*
    private static String getPath(String resourcePath) {
        def resource = SimpleJavaSpec.class.getResource(resourcePath)
        assert resource : "Resource not available: $resourcePath"
        Paths.get(resource.toURI()).toAbsolutePath().toString()
    }

    def "should create a valid artifact"() {
        when:
        def artifacts = new GradleTemplateBuilder(TEMPLATE_DIR)
                .withContextFile(SAMPLE_CONTEXT)
                .runGradleBuild()
                .artifacts
        def jars = artifacts['jar']

        then:
        jars.size() == 1
        jars[0].getEntry(APP_MAIN_CLASS_PATH) != null
    }

    def "should create a valid application"() {
        when:
        def context = new Initializr(TEMPLATE_DIR).createContext(SAMPLE_CONTEXT)

        then:
        new OutputChecker(TEMPLATE_DIR, context)
                .checkOutput("Hello from $context.appMainClass!")
    }
*(
}
