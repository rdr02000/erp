# GitHub Copilot Instructions: Java, Maven, Spring Boot & Spring Data JPA Standards

Always adhere to the following architectural, behavioral, Maven, Spring Boot, and Spring Data JPA persistence guidelines when generating, refactoring, or reviewing code in this workspace.

## 1. Developer Profile and Tech Stack Rules
* **Primary Backend: Java 21+, Spring Boot 3.x, Maven. 
* **Primary Frontend: Next.js (App Router), React, TypeScript, Tailwind CSS.

## 2. Null Safety & NullPointer Prevention
* **Compile-Time Enforcement:** Use standard Jakarta/Spring validation annotations (`@NonNull`, `@Nullable`, `@NotNull`) on method parameters, return types, and field boundaries to allow static analysis tools (like SpotBugs) to catch potential NPEs before compilation.
* **Input Validation & Guard Clauses:** Never assume incoming method parameters are non-null unless protected by a framework validator. Validate arguments immediately at the top of the method scope using guard clauses:
  * Prefer `Objects.requireNonNull(variable, "Error message")` for object references.
  * Use `StringUtils.hasText(string)` or `string.isBlank()` for String arguments within Spring Boot apps.
* **Return Typings (Optional API):** Never return a literal `null` from a method intended to represent the absence of a value—especially in Service or Repository layers. Return a `java.util.Optional<T>` instead.
* **Optional Consumption:** Avoid calling `.get()` directly on an `Optional` without checking `.isPresent()`. Instead, favor fluent functional methods such as `.map()`, `.flatMap()`, `.orElse()`, `.orElseGet()`, or `.orElseThrow()`.
* **Pattern Matching for Nulls (Java 21+):** When using modern `switch` expressions, explicitly handle the null case within the switch selector block using the new `case null -> ...` syntax rather than wrapping the entire statement in an external `if (obj != null)` check.
* **Collection Defensiveness:** Never return `null` for array or collection return types. Always return empty variants, such as `List.of()`, `Set.of()`, `Map.of()`, or `Collections.emptyList()`.


## 3. Spring Boot 3.x Architecture & Component Design
* **Layered Architecture:** Maintain strict separation of concerns across layers:
  * `Controller` / `RestController`: HTTP routing, request validation, and mapping payloads to/from DTOs.
  * `Service`: Business logic, transaction boundaries (`@Transactional`), and orchestration.
  * `Repository`: Data access and database query encapsulation.
* **Dependency Injection:** Always use constructor injection instead of field injection (`@Autowired` on fields). Constructor fields should be declared `final` to ensure immutability.
* **Configuration Properties:** Encapsulate configuration values using `@ConfigurationProperties` paired with immutable record definitions or standard classes annotated with `@ConfigurationPropertiesBinding`. Avoid messy, unvalidated `@Value` annotations scattered across business logic.
* **Stateless Operations:** Keep components stateless. Avoid keeping mutable request- or session-scoped state inside singleton beans.

## 4. Spring Data JPA & Persistence Best Practices
* **Entity Definition:** 
  * Use standard Jakarta Persistence annotations (`@Entity`, `@Table`, `@Id`). 
  * Avoid using raw Lombok `@Data` on entity classes to prevent broken `hashCode()`, `toString()`, and circular dependency loops in bi-directional relationships. Write targeted getters/setters or use explicit, safe Lombok annotations (`@Getter`, `@Setter`).
  * Always use a surrogate primary key (e.g., `UUID` or `GenerationType.IDENTITY`).
* **Relationship Mappings:**
  * Default to lazy loading for all associations (`FetchType.LAZY`). Never use `FetchType.EAGER` unless explicitly justified.
  * Properly manage bidirectional relationships using the `mappedBy` element on the non-owning side, and implement helper methods to keep both sides synchronized.
* **Repository Architecture:**
  * Inherit from `ListCrudRepository` or `ListPagingAndSortingRepository` instead of the legacy `JpaRepository` when List return types are preferred over Iterables.
  * Favor **Dynamic Projections (using Java Records)** for read-only queries to avoid fetching full managed entities into memory when only a subset of fields is required.
* **Query Performance & Optimization:**
  * Actively avoid the N+1 query problem. Use `@EntityGraph` or explicit `JOIN FETCH` JPQL queries within repositories when fetching related collections.
  * Annotate read-only service transactions with `@Transactional(readOnly = true)` to optimize dirty-checking behavior in the Hibernate session.

## 5. Maven Project Structure & Conventions
* **Directory Layout:** Strictly follow the Standard Maven Directory Layout (`src/main/java`, `src/main/resources`, `src/test/java`).
* **Dependency Management:** Define artifact versions inside the `<dependencyManagement>` section of the parent POM, or rely on Spring Boot Parent Starter properties. Keep dependency declarations in modules version-free.
* **Scope Discipline:** Maintain clean dependency scopes (`compile`, `provided`, `runtime`, `test`) to prevent fat-JAR bloat.

## 6. Naming & Formatting Conventions
* **Case Styles:** `UpperCamelCase` for classes/interfaces/records/entities, `lowerCamelCase` for methods/variables/parameters, and `UPPER_SNAKE_CASE` for static constants.
* **Indentation & Formatting:** Use 4 spaces per indent level. Never use tabs. Always use braces `{}` for control structures (`if`, `while`, `for`), even for single-line blocks.

## 7. Modern Java Idioms (Java 21+)
* **Data Transfer Objects:** Prefer shallow immutability by using `record` classes for DTOs, API payloads, configuration blocks, and query projections.
* **Control Flow:** Favor modern `switch` expressions that return values directly and utilize pattern matching over nested `if-else` blocks.
* **Functional Streams:** Process collections declaratively using Streams and Lambda expressions. Favor method references (e.g., `User::isActive`) over full functional expressions (`user -> user.isActive()`).

## 8. Resource & Exception Handling
* **Resource Closures:** Always use `try-with-resources` statements for any resource implementing `AutoCloseable` (e.g., File I/O, network streams).
* **Exception Catching & Logging:** Never catch generic `Exception` or `Throwable`. Catch specific checked/unchecked subclasses. Never leave a `catch` block empty—always log the exact stack trace using an SLF4J logger or wrap and rethrow it within a custom business exception.
* **Global Error Handling:** Manage REST responses cleanly using a `@RestControllerAdvice` class intercepting specific domain exceptions, transforming them into standardized error payloads (e.g., RFC 7807 Problem Details).

## 9. Unit & Integration Testing Best Practices
* **Testing Stack:** Natively use JUnit 5 (`org.junit.jupiter`), AssertJ (`org.assertj.core`), and Mockito (`org.mockito`). Avoid legacy JUnit 4 or standard JUnit assertions.
* **AAA Architecture:** Structure every test case following the **Arrange-Act-Assert** pattern, separating each block cleanly with a single blank line.
* **Test Isolation & Slicing:**
  * For pure business logic (Services/Components), use crisp unit tests with `@ExtendWith(MockitoExtension.class)` and `@Mock`. Do not boot up a Spring context.
  * For database verification, use `@DataJpaTest` to configure an in-memory or test database slice instead of bootstrapping the full `@SpringBootTest`.
  * For web boundaries, use `@WebMvcTest` combined with `MockMvc` to target individual controller layers.
* **Mocking Discipline:** Never mock the object under test (the System Under Test, or SUT). Mock only its external downstream dependencies. Avoid using `@Spy` unless testing legacy code or complex abstract workflows.
* **Assertions:** Favor AssertJ's fluent api (`assertThat(...)`) over alternative options to maximize readability. Always verify explicit behaviors (e.g., checking specific field values or exception types) rather than generic `assertTrue(result != null)`.
* **Test Data Cleanliness:** Rely on dedicated factory methods, builder patterns, or Java records to generate mock data structures. Keep tests clear of heavy, inline instantiation blocks.

## 10. Architecture & Clean Code
* **Single Responsibility Principle:** Keep classes, services, and repositories short, focused, and limited to a single logical responsibility.
* **Guard Clauses:** Avoid deeply nested code blocks. Validate method inputs immediately and exit early using guard clauses at the top of the scope.
* **Magic Values:** Eliminate inline literal values. Replace them with type-safe `enums` or descriptively named constant fields.

## 11. Next.js & Frontend Standards
* **Enforce strict TypeScript typing. Do not accept or suggest `any`.
* **Prioritize Next.js Server Components (RSC) by default. Only use `'use client'` when hooks or state are required.
* **Optimize rendering performance by suggesting `useMemo` or `useCallback` only when computing heavy operational values.

## 12. Strict Context Boundaries
* **NEVER suggest Java syntax, patterns, or annotations within directory paths targeting `src/main/frontend`, `apps/`, or web-facing UI components.
* **NEVER mix Node.js package recommendations into Java build files (`pom.xml` or `build.gradle`).

## Application Lifecycle & Deployment Workflow
* **When asked to "redeploy", "compile", or "restart the app", prioritize telling the user to use the **Spring Boot Dashboard** extension or the configured VS Code task rather than rewriting execution scripts.
* **If asked to write automated terminal scripts for compilation, always prefer using the Maven Wrapper (`./mvnw`) over global `mvn`.
* **Ensure all compilation prompts default to bypassing tests via `-DskipTests` for local iterative development unless explicitly asked to verify regressions.