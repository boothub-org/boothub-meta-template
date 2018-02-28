package org.boothub.context

import groovy.transform.SelfType
import org.beryx.textio.TextIO
import org.boothub.Util

@SelfType(ProjectContext)
@ConfiguredBy(MetaInfo.Configurator)
trait MetaInfo {
    String projectNameAsJavaClass
    String templateId
    String templatePackage
    String templateCaption
    Class<? extends ProjectContext> baseContextClass
    List<Class> additionalTraits = []
    String ancestry

    boolean licensable
    boolean versionable
    boolean mavenCompatible
    boolean gradleBased
    boolean bintraySupport

    String modularity = Modularity.SINGLE.name()
    String projectType = ProjectType.LIBRARY.name()

    static enum Modularity {
        SINGLE('Only single-module projects'),
        MULTI('Only multi-module projects'),
        BOTH('Both single-module and multi-module projects');

        private final String description

        Modularity(String description) {
            this.description = description
        }

        @Override
        String toString() {
            description
        }
    }

    static enum ProjectType {
        APP('Only applications (the generated projects will provide a main class)'),
        LIBRARY('Only libraries (the generated projects will not provide a main class)'),
        BOTH('Both applications and libraries (the generated projects may or may not provide a main class)');

        private final String description

        ProjectType(String description) {
            this.description = description
        }

        @Override
        String toString() {
            description
        }
    }

    static class Configurator extends TextIOConfigurator  {
        @Override
        void configureWithTextIO(ProjectContext context, TextIO textIO) {
            def ctx = context as MetaInfo
            ctx.projectNameAsJavaClass = Util.asJavaId(ctx.projectName).capitalize()
            ctx.templateId = textIO.newStringInputReader()
                    .withDefaultValue("com.github.$ctx.ghProjectId" as String)
                    .withValueChecker(Util.mavenIdChecker)
                    .read("Template ID")

            ctx.templatePackage = Util.asPackageFragment(ctx.templateId, true)

            ctx.templateCaption = textIO.newStringInputReader()
                    .read("Short description")

            ctx.mavenCompatible = textIO.newBooleanInputReader()
                    .withDefaultValue(true)
                    .read("Is your template intended to be used for generating projects that produce maven compatible artifacts?")

            if(!ctx.mavenCompatible) {
                ctx.licensable = textIO.newBooleanInputReader()
                        .withDefaultValue(true)
                        .read("Should your template require a license for the generated projects?")

                ctx.versionable = textIO.newBooleanInputReader()
                        .withDefaultValue(true)
                        .read("Should your template require project versioning?")

                if(ctx.licensable & ctx.versionable) {
                    ctx.baseContextClass = StandardProjectContext
                } else {
                    ctx.baseContextClass = ProjectContext
                    if(ctx.licensable) ctx.additionalTraits << Licensable
                    if(ctx.versionable) ctx.additionalTraits << Versionable
                }
            } else {
                ctx.licensable = true
                ctx.versionable = true
                ctx.gradleBased = textIO.newBooleanInputReader()
                        .withDefaultValue(true)
                        .read("Is your template intended to be used for generating gradle-based projects?")
                if(ctx.gradleBased) {
                    ctx.bintraySupport = textIO.newBooleanInputReader()
                            .withDefaultValue(true)
                            .read("Should the gradle script of the projects generated by your template provide support for publishing artifacts to Bintray?")
                }
                Modularity mod = textIO.newEnumInputReader(Modularity)
                        .read("What kind of structure should have the projects generated by your template?")

                ProjectType prjType = textIO.newEnumInputReader(ProjectType)
                        .read("What type of projects should your template generate?")

                ctx.baseContextClass = getBaseContextClass(mod, prjType)
                ctx.modularity = mod.name()
                ctx.projectType = prjType.name()
            }
            def traits = ['InfoContext'] + ctx.additionalTraits.collect {it.canonicalName}
            ctx.ancestry = "extends $ctx.baseContextClass.canonicalName implements ${traits.join(', ')}"
        }

        static Class<? extends ProjectContext> getBaseContextClass(Modularity mod, ProjectType prjType) {
            switch (mod) {
                case Modularity.SINGLE:
                    switch (prjType) {
                        case ProjectType.APP: return StandardProjectContext.AppSingle
                        case ProjectType.LIBRARY: return StandardProjectContext.LibSingle
                        case ProjectType.BOTH: return StandardProjectContext.Single
                    }
                case Modularity.MULTI:
                    switch (prjType) {
                        case ProjectType.APP: return StandardProjectContext.AppMulti
                        case ProjectType.LIBRARY: return StandardProjectContext.LibMulti
                        case ProjectType.BOTH: return StandardProjectContext.Multi
                    }
                case Modularity.BOTH:
                    switch (prjType) {
                        case ProjectType.APP: return StandardProjectContext.App
                        case ProjectType.LIBRARY: return StandardProjectContext.Lib
                        case ProjectType.BOTH: return StandardProjectContext.Generic
                    }
            }
            throw new IllegalArgumentException("Unsupported options:\n\tmodularity: $mod\n\ttype: $prjType")
        }
    }
}