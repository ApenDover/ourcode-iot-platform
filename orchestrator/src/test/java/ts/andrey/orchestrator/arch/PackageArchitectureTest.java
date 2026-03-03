package ts.andrey.orchestrator.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Slf4j
@AnalyzeClasses(
        packages = "ts.andrey.orchestrator",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
class PackageArchitectureTest {

    @ArchTest
    static final ArchRule only_rest_controllers_in_adapter_in_package = classes()
            .that().resideInAPackage("..infrastructure.adapter.in..")
            .should().beAnnotatedWith(RestController.class)
            .andShould().haveSimpleNameEndingWith("Controller")
            .andShould().beTopLevelClasses()
            .andShould().bePublic()
            .because("В пакете infrastructure.adapter.in должны быть только публичные "
                    + "REST контроллеры верхнего уровня");

    @ArchTest
    static final ArchRule no_rest_controllers_outside_adapter_in = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..infrastructure.adapter.in..")
            .andShould().haveSimpleNameEndingWith("Controller")
            .because("REST контроллеры могут находиться только в infrastructure.adapter.in");

    @ArchTest
    static final ArchRule strict_port_rule = classes()
            .that().resideInAPackage("..application.port..")
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Port")
            .andShould().beTopLevelClasses()
            .andShould().bePublic()
            .because("В пакете application.port должны быть только публичные интерфейсы "
                    +
                    "верхнего уровня с окончанием Port");

    @ArchTest
    static final ArchRule strict_port_interface_rule = classes()
            .that().resideInAPackage("..port..")
            .should().beInterfaces()
            .andShould().beTopLevelClasses()
            .andShould().bePublic()
            .because("В пакете port должны быть только публичные интерфейсы");

    @ArchTest
    static final ArchRule strict_adapter_not_interface_rule = classes()
            .that().resideInAPackage("..adapter..")
            .should().notBeInterfaces()
            .andShould().beTopLevelClasses()
            .andShould().bePublic()
            .because("В пакете adapter не должно быть интерфейсов");

    @ArchTest
    static final ArchRule domain_exception_package_contains_exception_related_classes = classes()
            .that().resideInAPackage("..domain.exception..")
            .should().haveSimpleNameEndingWith("Exception")
            .orShould().haveSimpleNameEndingWith("ExceptionMessage")
            .orShould().haveSimpleNameEndingWith("ErrorCode")
            .because("В пакете domain.exception должны быть только классы, связанные с исключениями");

    @ArchTest
    static final ArchRule all_exception_related_classes_reside_in_domain_exception_package = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .or().haveSimpleNameEndingWith("ExceptionMessage")
            .or().haveSimpleNameEndingWith("ErrorCode")
            .should().resideInAPackage("..domain.exception..")
            .because("Все классы, связанные с исключениями, должны находиться в domain.exception");

    @ArchTest
    static final ArchRule all_configuration_classes_should_reside_in_infrastructure_config_package = classes()
            .that().areAnnotatedWith(Configuration.class)
            .should().resideInAPackage("..infrastructure.config..")
            .because("Все классы с аннотацией @Configuration должны находиться в пакете infrastructure.config");

    @ArchTest
    static final ArchRule infrastructure_handler_package_should_contain_only_advice_classes = classes()
            .that().resideInAPackage("..infrastructure.handler..")
            .should().beAnnotatedWith(RestControllerAdvice.class)
            .because("В пакете infrastructure.handler должны быть только классы с @RestControllerAdvice");

    @ArchTest
    static final ArchRule all_rest_controller_advice_reside_in_correct_package = classes()
            .that().areAnnotatedWith(RestControllerAdvice.class)
            .should().resideInAPackage("..infrastructure.handler..")
            .because("Все классы-обработчики исключений должны находиться в infrastructure.handler");

    @ArchTest
    static final ArchRule infrastructure_mapper_package_should_contain_only_mapper_classes = classes()
            .that().resideInAPackage("..infrastructure.mapper..")
            .and().areNotAnonymousClasses()
            .and().areNotNestedClasses()
            .should().haveSimpleNameContaining("Mapper")
            .because("В пакете infrasructure.mapper должны быть только классы-мапперы");

    @ArchTest
    static final ArchRule all_mapper_classes_reside_in_correct_package = classes()
            .that().haveSimpleNameContaining("Mapper")
            .and().areNotAnonymousClasses()
            .and().areNotNestedClasses()
            .should().resideInAPackage("..infrastructure.mapper..")
            .because("Все классы-мапперы должны находиться в infrastructure.mapper");

    @ArchTest
    static final ArchRule all_annotated_mapper_in_correct_package = classes()
            .that().areAnnotatedWith(Mapper.class)
            .should().resideInAPackage("..infrastructure.mapper..")
            .because("Все классы-мапперы с аннотацией Mapper в infrastructure.mapper");

    @ArchTest
    static final ArchRule all_utility_classes_reside_in_correct_package = classes()
            .that().haveSimpleNameContaining("Util")
            .or().haveSimpleNameContaining("Utility")
            .and().areTopLevelClasses()
            .and().resideOutsideOfPackage("..ts.andrey.orchestrator.api..")
            .should().resideInAPackage("..infrastructure.util..")
            .because("Все утилитные классы должны находиться в infrastructure.util");

    @ArchTest
    static final ArchRule all_feign_clients_should_reside_in_infrastructure_out_port_feign_package = classes()
            .that().areAnnotatedWith(FeignClient.class)
            .should().resideInAPackage("..infrastructure.out.port.feign..")
            .because("Все Feign клиенты должны находиться в пакете infrastructure.out.port.feign");

    @ArchTest
    static final ArchRule infrastructure_out_should_only_depend_on_adapter_out =
            classes()
                    .that().resideInAPackage("ts.andrey.orchestrator.infrastructure.out..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "ts.andrey.orchestrator.infrastructure.adapter.out..",
                            "ts.andrey.orchestrator.infrastructure.out.port.grpc..",
                            "ts.andrey.device..",
                            "ts.andrey.event..",
                            "ts.andrey.routermanager..",
                            "io.grpc..",
                            "feign..",
                            "java..", "javax..", "org..", "lombok.."
                    )
                    .because("Все классы из infrastructure.out могут обращаться только к infrastructure.adapter.out");


}
