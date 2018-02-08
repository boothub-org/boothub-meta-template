/*
 * Copyright 2018 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.boothub.template

import org.beryx.streamplify.product.CartesianProduct
import org.beryx.textio.TextIO
import org.beryx.textio.mock.MockTextTerminal
import org.boothub.GroovyClassDefiner
import org.boothub.Initializr
import org.boothub.Util
import org.boothub.Version
import org.boothub.context.ProjectContext
import org.boothub.context.StandardProjectContext
import org.boothub.context.TextIOConfigurator
import org.boothub.context.Versionable
import org.boothub.gradle.BuildChecker
import org.boothub.gradle.GradleBuildResult
import org.boothub.gradle.GradleTemplateBuilder
import org.boothub.gradle.OutputChecker
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class MetaTemplateSpec extends Specification {
    private static final String BASE_PATH = 'org/bizarre_soft'
    private static final String[] MODULES = ['weird_app', 'strange_lib', 'spooky_ui']
    private static final String APP_MODULE = MODULES[0]
    private static final APP_MAIN_CLASS = 'WeirdAppMain'

    private static final String[] GRADLE_FILES = [
            'gradle', 'build.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
            'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']

    private static String getPath(String resourcePath) {
        def resource = MetaTemplateSpec.class.getResource(resourcePath)
        assert resource : "Resource not available: $resourcePath"
        Paths.get(resource.toURI()).toAbsolutePath().toString()
    }

    static final String metaTemplateDir = 'skeleton/template'
    static Class<? extends StandardProjectContext> metaTemplateProjectContextClass
    static Class<? extends TextIOConfigurator> configuratorClass
    static Class<? extends Enum> modularityClass
    static Class<? extends Enum> projectTypeClass
    static List<? extends Enum> modularityValues
    static List<? extends Enum> projectTypeValues

    static {
        def definer = GroovyClassDefiner.ofTemplateDir(metaTemplateDir)
        definer.defineClasses()
        metaTemplateProjectContextClass = definer.forName('org.boothub.context.MetaTemplateProjectContext')
        configuratorClass = definer.forName('org.boothub.context.MetaInfo$Configurator')
        modularityClass = definer.forName('org.boothub.context.MetaInfo$Modularity')
        projectTypeClass = definer.forName('org.boothub.context.MetaInfo$ProjectType')
        modularityValues = modularityClass.getMethod('values').invoke(null) as List
        projectTypeValues = projectTypeClass.getMethod('values').invoke(null) as List

        Path.metaClass.allExist = { ... fileNames -> fileNames.every {f -> delegate.resolve(f).toFile().exists()} }
        Path.metaClass.noneExist = { ... fileNames -> fileNames.every {f -> !delegate.resolve(f).toFile().exists()} }
    }

    static StandardProjectContext createBaseMetaContext() {
        StandardProjectContext ctx = metaTemplateProjectContextClass.newInstance()
        ctx.with {
            boothubVersion = Version.BOOTHUB_CURRENT.toString()
            ghProjectId = 'my-project'
            projectName = 'My Project'
            projectPageUrl = 'https://my-project.org'
            ghApiUsed = false
            ghUserId = 'myUser'
            ghProjectOwner = 'myOrg'
            license = 'Apache-2.0'
            versionMajor = 3
            versionMinor = 4
            versionPatch = 5
            releaseBuild = true
        }
        ctx
    }

    static List<String> getNonMavenInputs(boolean licenseRequired, boolean versioningRequired) {
        [
                'org.example.my-project',   // templateId
                'This is my project.',      // templateCaption
                'N',                        // maven compatible artifacts
                licenseRequired ? 'Y' : 'N',
                versioningRequired ? 'Y' : 'N',
        ]
    }

    static List<String> getMavenCompatibleInputs(boolean gradleBased, boolean bintraySupport, Enum modularity, Enum projectType) {
        def inputs = [
                'org.example.my-project',   // templateId
                'This is my project.',      // templateCaption
                'Y',                        // maven compatible artifacts
                gradleBased ? 'Y' : 'N',
        ]
        if(gradleBased) {
            inputs << (bintraySupport ? 'Y' : 'N')
        }
        inputs += [
                (1 + modularity.ordinal()) as String,
                (1 + projectType.ordinal()) as String,
        ]
        inputs
    }

    static void configureLicenseAndVersion(StandardProjectContext metaCtx, ProjectContext ctx, String license, String version) {
        if(metaCtx.versionable) {
            def v = Version.fromString(version)
            ctx.versionMajor= v.major
            ctx.versionMinor = v.minor
            ctx.versionPatch = v.patch
            ctx.versionLabel = v.label
        }
        if(metaCtx.licensable) {
            ctx.license = license
        }
    }

    static void checkBasics(Path path, StandardProjectContext metaCtx, ProjectContext ctx) {
        assert path.allExist('.gitignore')
        def readmeText = path.resolve('README.md').text
        assert readmeText.contains(ctx.projectName)
        if(metaCtx.versionable) {
            Versionable v = ctx
            def version = new Version(v.versionMajor, v.versionMinor, v.versionPatch, v.versionLabel).toString()
            assert readmeText.contains("version: $version")
        } else {
            assert !readmeText.contains('version:')
        }
        if(metaCtx.licensable) {
            assert readmeText.contains("license: $ctx.license")
            assert path.allExist('LICENSE', 'license-header.txt')
        } else {
            assert !readmeText.contains('license:')
            assert path.noneExist('LICENSE', 'license-header.txt')
        }
        assert readmeText.contains("myExampleProperty: $ctx.myExampleProperty")
        true
    }

    @Unroll
    def "should generate a non-maven template with license=#licenseRequired and versioning=#versioningRequired"() {
        when:
        def metaCtx = createBaseMetaContext()
        def terminal = new MockTextTerminal()
        def textIO = new TextIO(terminal)
        terminal.inputs.addAll(getNonMavenInputs(licenseRequired, versioningRequired))
        configuratorClass.newInstance().configureWithTextIO(metaCtx, textIO)
        Path templatePath = new Initializr(metaTemplateDir).generateWithContext(metaCtx)
        println "templatePath: $templatePath"

        def initializr = new Initializr("$templatePath/skeleton/template")
        ProjectContext ctx = initializr.createContext(getPath('/base-context-non-maven.yml'))
        configureLicenseAndVersion(metaCtx, ctx, 'MIT', '6.7.8')
        Path path = initializr.generateWithContext(ctx)
        println "path: $path"

        then:
        checkBasics(path, metaCtx, ctx)
        path.noneExist(GRADLE_FILES)

        where:
        licenseRequired | versioningRequired
        false           | false
        false           | true
        true            | false
        true            | true

    }

    def getMavenNonGradleTemplates() {
        new CartesianProduct(modularityValues.size(), projectTypeValues.size())
                .stream()
                .map { prod ->
            Enum modularity = modularityValues.get(prod[0])
            Enum projectType = projectTypeValues.get(prod[1])
            createContextTuple(false, false, modularity, projectType)
        }
        .collect(Collectors.toList())
    }

    def getMavenGradleTemplates() {
        new CartesianProduct(2, modularityValues.size(), projectTypeValues.size())
                .stream()
                .map { prod ->
            boolean bintraySupport = prod[0] > 0
            Enum modularity = modularityValues.get(prod[1])
            Enum projectType = projectTypeValues.get(prod[2])
            createContextTuple(true, bintraySupport, modularity, projectType)
        }
        .collect(Collectors.toList())
    }

    def createContextTuple(boolean gradleBased, boolean bintraySupport, Enum modularity, Enum projectType) {
        def metaCtx = createBaseMetaContext()
        def terminal = new MockTextTerminal()
        def textIO = new TextIO(terminal)
        terminal.inputs.addAll(getMavenCompatibleInputs(gradleBased, bintraySupport, modularity, projectType))
        configuratorClass.newInstance().configureWithTextIO(metaCtx, textIO)
        Path templatePath = new Initializr(metaTemplateDir).generateWithContext(metaCtx)
        println "templatePath: $templatePath"
        [templatePath, metaCtx, bintraySupport, modularity, projectType]
    }

    @Unroll
    def "should generate a maven-compatible non-gradle-based template with modularity=#modularity, projectType=#projectType"() {
        when:
        def initializr = new Initializr("$templatePath/skeleton/template")
        ProjectContext ctx = initializr.createContext(getPath((modularity == 'SINGLE') ? '/base-context-single.yml' : '/base-context-multi.yml'))
        Path path = initializr.generateWithContext(ctx)
        println "path: $path"

        then:
        checkBasics(path, metaCtx, ctx)
        path.noneExist(GRADLE_FILES)

        where:
        t << getMavenNonGradleTemplates()
        templatePath = t[0]
        metaCtx = t[1]
        modularity = t[3].name()
        projectType = t[4].name()
    }

    @Unroll
    def "should generate a maven-compatible gradle-based template with bintraySupport=#bintraySupport, modularity=#modularity, projectType=#projectType"() {
        when:
        def ctxFile = getPath((modularity == 'SINGLE') ? '/base-context-single.yml' : '/base-context-multi.yml')
        def templateDir = templatePath.resolve('skeleton/template').toString()
        def builder = new GradleTemplateBuilder(templateDir).withContextFile(ctxFile)
        def buildResult = builder.runGradleBuild()
        Path path = buildResult.projectPath
        println "buildResult: $buildResult"

        then:
        checkBasics(path, metaCtx, builder.context)
        templatePath.allExist(GRADLE_FILES)
        templatePath.resolve('skeleton/template/files').allExist(GRADLE_FILES)
        path.allExist(GRADLE_FILES)
        if(modularity == 'SINGLE') {
            assert path.noneExist('settings.gradle')
            checkSingleArtifacts(buildResult)
        } else {
            assert path.allExist('settings.gradle')
            checkMultipleArtifacts(builder)
        }
        if(projectType != 'LIBRARY') {
            checkOutput(builder, (modularity == 'SINGLE') ? null : APP_MODULE.replaceAll('_', '-'))
        }

        where:
        t << getMavenGradleTemplates()
        templatePath = t[0]
        metaCtx = t[1]
        bintraySupport = t[2]
        modularity = t[3].name()
        projectType = t[4].name()
    }

    private static boolean checkSingleArtifacts(GradleBuildResult buildResult) {
        assert buildResult.artifacts['jar'].size() == 1
        assert buildResult.artifacts['sources'].size() == 1
        assert buildResult.artifacts['javadoc'].size() == 1
        true
    }

    private static boolean checkMultipleArtifacts(GradleTemplateBuilder builder) {
        MODULES.each { module ->
            def expectedFileNames = [getModuleFileName(module)]
            def checker = new BuildChecker(builder, module, "$BASE_PATH/$module")
            checker.checkClassesAndJars('sources', ['java'], expectedFileNames)
            checker.checkClassesAndJars('jar', ['class'], expectedFileNames)
        }
        true
    }

    private static boolean checkOutput(GradleTemplateBuilder builder, String moduleName) {
        def checker = new OutputChecker(builder.templateDir, builder.context, moduleName)
        checker.checkOutput("Hello from $builder.context.appMainClass!")
        true
    }

    private static String getModuleFileName(String module) {
        if(module == APP_MODULE) return APP_MAIN_CLASS
        return Util.asJavaClassName(module.replaceAll('_', '-')).capitalize() + "Util"
    }
}
