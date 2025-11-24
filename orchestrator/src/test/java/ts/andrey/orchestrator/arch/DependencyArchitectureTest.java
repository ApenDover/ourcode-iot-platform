package ts.andrey.orchestrator.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.extern.slf4j.Slf4j;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Slf4j
@AnalyzeClasses(
        packages = "ts.andrey.orchestrator",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
class DependencyArchitectureTest {

    @ArchTest
    static final ArchRule application_should_only_access_allowed_packages = classes()
            .that().resideInAPackage("..application..")
            .should().onlyAccessClassesThat().resideOutsideOfPackages("..infrastructure..")
            .because("Application слой не должен обращаться к Infrastructure слою");

    @ArchTest
    static final ArchRule domain_should_only_access_allowed_packages = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyAccessClassesThat().resideOutsideOfPackages("..infrastructure..")
            .andShould().onlyAccessClassesThat().resideOutsideOfPackages("..application..")
            .because("domain слой не должен обращаться к Infrastructure слою или Application");

    @ArchTest
    static final ArchRule non_input_adapters_must_not_depend_on_application =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .and().resideOutsideOfPackage("..infrastructure.adapter..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .because("Только адаптеры могут вызывать application слой");


    @ArchTest
    static final ArchRule only_output_adapters_and_out_can_use_infrastructure_out =
            classes()
                    .that().resideInAPackage("..infrastructure.out..")
                    .should().onlyBeAccessed().byClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.out..",
                            "..infrastructure.adapter.out.."
                    )
                    .because("Классы infrastructure.out могут использоваться только самим пакетом и output адаптерами");

}
