{{javaComment 'license-header.txt'~}}
package {{templatePackage}}.context

import groovy.transform.SelfType
import org.boothub.context.ConfiguredBy
import org.boothub.context.ProjectContext
import org.boothub.context.TextIOConfigurator
import org.beryx.textio.TextIO

@SelfType(ProjectContext)
@ConfiguredBy(InfoContext.Configurator)
trait InfoContext {
    String myExampleProperty = 'dummy'

    static class Configurator extends TextIOConfigurator  {
        @Override
        void configureWithTextIO(ProjectContext context, TextIO textIO) {
            def ctx = context as InfoContext
            ctx.myExampleProperty = textIO.newStringInputReader()
                    .withDefaultValue(ctx.myExampleProperty)
                    .read('myExampleProperty')
        }
    }
}
