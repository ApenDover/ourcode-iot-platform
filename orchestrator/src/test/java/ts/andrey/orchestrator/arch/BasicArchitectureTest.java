package ts.andrey.orchestrator.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Slf4j
@AnalyzeClasses(packages = "ts.andrey.orchestrator.infrastructure.adapter.in")
class BasicArchitectureTest {

    private final JavaClasses importedClasses =
            new ClassFileImporter().importPackages("ts.andrey.orchestrator.infrastructure.adapter.in");

    @ArchTest
    void classes_should_have_meaningful_names() {
        classes()
                .that().resideInAPackage("..adapter.in..")
                .should().haveSimpleNameEndingWith("Controller")
                .check(importedClasses);
    }

    @ArchTest
    static final ArchRule domain_must_not_depend_on_adapters =
            noClasses().that().resideInAnyPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..adapter..", "..feign..", "..grpc..", "..config..");

    @ArchTest
    static final ArchRule controllers_call_domain_not_clients =
            classes().that().resideInAnyPackage("..adapter.in.web..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("..adapter.in.web..", "..domain..", "..common..", "java..", "jakarta..");

    @ArchTest
    static final ArchRule only_outbound_adapters_use_external_clients =
            noClasses().that().resideInAnyPackage("..domain..", "..adapter.in.web..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.cloud.openfeign", "io.grpc", "net.devh.boot.grpc.client");

    @ArchTest
    static final ArchRule clients_located_in_outbound_packages =
            classes().that().haveSimpleNameEndingWith("Client")
                    .should().resideInAnyPackage("..adapter.out.feign..", "..adapter.out.grpc..");


}
