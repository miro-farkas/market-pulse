package com.example.marketpulse.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.Set;

/**
 * Build-enforced architecture rules. See CLAUDE.md, ADR-0001 (layers) and ADR-0002 (blocking).
 * Do not weaken or delete rules to make code pass – fix the code or propose an ADR change.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT = "com.example.marketpulse";

    private static final String RUN_ON_VIRTUAL_THREAD = "io.smallrye.common.annotation.RunOnVirtualThread";
    private static final String BLOCKING = "io.smallrye.common.annotation.Blocking";

    // ---------------------------------------------------------------- layers (ADR-0001)

    @ArchTest
    static final ArchRule LAYERS = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain")
            .definedBy(ROOT + ".domain..")
            .layer("Application")
            .definedBy(ROOT + ".application..")
            .layer("AdaptersIn")
            .definedBy(ROOT + ".adapter.in..")
            .layer("AdaptersOut")
            .definedBy(ROOT + ".adapter.out..")
            .layer("Config")
            .definedBy(ROOT + ".config..")
            .whereLayer("AdaptersIn")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("AdaptersOut")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("AdaptersIn", "AdaptersOut", "Config")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "AdaptersIn", "AdaptersOut", "Config");

    @ArchTest
    static final ArchRule DOMAIN_IS_PLAIN_JAVA = noClasses()
            .that()
            .resideInAPackage(ROOT + ".domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "io.quarkus..",
                    "io.smallrye..",
                    "io.vertx..",
                    "jakarta..",
                    "org.eclipse.microprofile..",
                    "org.hibernate..",
                    "org.apache.kafka..",
                    "com.fasterxml..")
            .because("the domain must be framework-free and synchronously testable (ADR-0001)");

    @ArchTest
    static final ArchRule APPLICATION_HAS_NO_TRANSPORT_OR_PERSISTENCE_TYPES = noClasses()
            .that()
            .resideInAPackage(ROOT + ".application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "jakarta.ws.rs..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    // Panache entities/repositories; io.quarkus.hibernate.reactive.panache.common (@WithTransaction) is
                    // allowed
                    "io.quarkus.hibernate.reactive.panache",
                    "io.vertx..",
                    "org.apache.kafka..",
                    "io.smallrye.reactive.messaging..",
                    "org.eclipse.microprofile.reactive.messaging..",
                    "io.quarkus.websockets..",
                    "com.fasterxml..")
            .because("use cases talk to the outside world only through ports (ADR-0001)");

    @ArchTest
    static final ArchRule ENTITIES_ONLY_IN_PERSISTENCE_ADAPTER = classes()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideInAPackage(ROOT + ".adapter.out.persistence..")
            .because("entities must never leak into domain, use cases or REST (ADR-0003)");

    @ArchTest
    static final ArchRule REST_RESOURCES_LIVE_IN_REST_ADAPTER = classes()
            .that()
            .areAnnotatedWith("jakarta.ws.rs.Path")
            .should()
            .resideInAPackage(ROOT + ".adapter.in.rest..")
            .andShould()
            .haveSimpleNameEndingWith("Resource");

    @ArchTest
    static final ArchRule INCOMING_ONLY_IN_MESSAGING_ADAPTER = methods()
            .that()
            .areAnnotatedWith("org.eclipse.microprofile.reactive.messaging.Incoming")
            .should()
            .beDeclaredInClassesThat()
            .resideInAPackage(ROOT + ".adapter.in.messaging..");

    @ArchTest
    static final ArchRule OUTGOING_ONLY_IN_MESSAGING_ADAPTERS = methods()
            .that()
            .areAnnotatedWith("org.eclipse.microprofile.reactive.messaging.Outgoing")
            .should()
            .beDeclaredInClassesThat()
            .resideInAnyPackage(ROOT + ".adapter.in.messaging..", ROOT + ".adapter.out.messaging..");

    @ArchTest
    static final ArchRule WEBSOCKET_CLIENTS_ONLY_IN_EXCHANGE_ADAPTER = noClasses()
            .that()
            .resideOutsideOfPackages(ROOT + ".adapter.out.exchange..", ROOT + ".config..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("io.quarkus.websockets.next..");

    // ---------------------------------------------------------------- reactive rules (ADR-0002, ADR-0003)

    @ArchTest
    static final ArchRule NO_BLOCKING_OUTSIDE_MARKED_METHODS = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(ROOT + "..")
            .and()
            .areNotAnnotatedWith(RUN_ON_VIRTUAL_THREAD)
            .and()
            .areNotAnnotatedWith(BLOCKING)
            .should(notCallBlockingApis())
            .because("blocking is only allowed in @RunOnVirtualThread or @Blocking methods (ADR-0002)");

    @ArchTest
    static final ArchRule NO_JDBC = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAPackage("java.sql..")
            .because("database access is reactive; JDBC is used by Flyway only (ADR-0003)");

    @ArchTest
    static final ArchRule NO_FLOATING_POINT_IN_DOMAIN = noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(ROOT + ".domain..")
            .should()
            .haveRawType(double.class)
            .orShould()
            .haveRawType(Double.class)
            .orShould()
            .haveRawType(float.class)
            .orShould()
            .haveRawType(Float.class)
            .because("prices, quantities and values use BigDecimal (docs/domain.md)");

    // ---------------------------------------------------------------- general conventions

    @ArchTest
    static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule NO_JUL = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule NO_GENERIC_EXCEPTIONS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule NO_OLD_DATE_TIME = GeneralCodingRules.OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;

    // ---------------------------------------------------------------- helpers

    private static final Set<String> BLOCKING_OWNERS =
            Set.of("io.smallrye.mutiny.groups.UniAwait", "io.smallrye.mutiny.groups.UniAwaitOptional");

    private static boolean isBlockingCall(JavaMethodCall call) {
        String owner = call.getTargetOwner().getName();
        String name = call.getName();
        return BLOCKING_OWNERS.contains(owner)
                || (owner.equals("io.smallrye.mutiny.Uni") && name.equals("await"))
                || (owner.equals("io.smallrye.mutiny.groups.MultiSubscribe")
                        && (name.equals("asIterable") || name.equals("asStream")))
                || (owner.equals("java.lang.Thread") && name.equals("sleep"))
                || (owner.equals("java.util.concurrent.Future") && name.equals("get"))
                || (owner.equals("java.util.concurrent.CompletableFuture")
                        && (name.equals("get") || name.equals("join")));
    }

    private static ArchCondition<JavaMethod> notCallBlockingApis() {
        return new ArchCondition<>("not call blocking APIs (Uni.await, asIterable, Thread.sleep, Future.get/join)") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getMethodCallsFromSelf().stream()
                        .filter(ArchitectureTest::isBlockingCall)
                        .forEach(call -> events.add(SimpleConditionEvent.violated(
                                method,
                                call.getDescription()
                                        + " -> stay non-blocking, or annotate the method with @Blocking /"
                                        + " @RunOnVirtualThread")));
            }
        };
    }
}
