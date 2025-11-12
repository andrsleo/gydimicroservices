# Auditoría de SQL Injection - GYDI 2.0

**Fecha**: 11 de Noviembre, 2025
**Auditor**: Security Team
**Nivel de Riesgo**: BAJO (Sin vulnerabilidades críticas encontradas)
**Estado**: ✅ APROBADO PARA PRODUCCIÓN

---

## Resumen Ejecutivo

Se realizó una auditoría exhaustiva del código fuente para identificar vulnerabilidades de SQL Injection en la aplicación GYDI 2.0.

**Resultado**: ✅ **NO se encontraron vulnerabilidades de SQL Injection**

La aplicación utiliza correctamente:
- ✅ Spring Data JPA con queries parametrizadas
- ✅ @Query con parámetros nombrados (@Param)
- ✅ Métodos de repositorio derivados (findBy*, existsBy*, etc.)
- ✅ JPQL/HQL en lugar de SQL nativo (donde es posible)
- ✅ PreparedStatements implícitos a través de JPA

---

## Metodología de Auditoría

### 1. Análisis Estático de Código

**Herramientas utilizadas**:
- Grep para búsqueda de patrones
- Revisión manual de código
- Análisis de repositories JPA

**Patrones buscados**:
```bash
# Buscar @Query annotations
grep -r "@Query" src/

# Buscar createQuery/createNativeQuery
grep -r "createNativeQuery\|createQuery" src/

# Buscar concatenación de strings en SQL
grep -r "+.*WHERE\|WHERE.*+\|\".*\"+.*\"" src/
```

### 2. Revisión de Repositories

**Archivos auditados**:
1. `UserJpaRepository.java`
2. `PasswordResetTokenJpaRepository.java`
3. `PasswordResetAuditJpaRepository.java`
4. `RefreshTokenJpaRepository.java`
5. `TokenBlacklistJpaRepository.java`
6. `PropertyJpaRepository.java`
7. `AmenityJpaRepository.java`
8. `BookingJpaRepository.java`
9. `ReorderPropertyImagesUseCaseImpl.java`

---

## Hallazgos Detallados

### ✅ SEGURO: Uso de @Query con Parámetros Nombrados

**Archivo**: `UserJpaRepository.java`

```java
// ✅ CORRECTO: Uso de parámetros nombrados
@Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
List<UserEntity> findByRoleName(@Param("roleName") String roleName);

@Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles")
List<UserEntity> findAllWithRoles();
```

**Análisis**:
- ✅ Usa `:roleName` como placeholder parametrizado
- ✅ Vincula el parámetro con `@Param("roleName")`
- ✅ JPA escapa automáticamente los valores
- ✅ NO es vulnerable a SQL injection

---

### ✅ SEGURO: Queries Complejas con Múltiples Parámetros

**Archivo**: `PasswordResetTokenJpaRepository.java`

```java
// ✅ CORRECTO: Query compleja con parámetros
@Query("""
        SELECT t FROM PasswordResetTokenEntity t
        WHERE t.userId = :userId
          AND t.used = false
          AND t.expiresAt > :currentTime
        ORDER BY t.createdAt DESC
        LIMIT 1
        """)
Optional<PasswordResetTokenEntity> findLatestValidTokenByUserId(
        @Param("userId") Long userId,
        @Param("currentTime") LocalDateTime currentTime
);
```

**Análisis**:
- ✅ Múltiples parámetros nombrados (`:userId`, `:currentTime`)
- ✅ Todos los parámetros están correctamente vinculados
- ✅ Uso de text blocks (""") para queries legibles
- ✅ NO es vulnerable a SQL injection

---

### ✅ SEGURO: Queries de Actualización y Eliminación

**Archivo**: `PasswordResetTokenJpaRepository.java`

```java
// ✅ CORRECTO: UPDATE query parametrizada
@Modifying
@Query("""
        UPDATE PasswordResetTokenEntity t
        SET t.used = true, t.usedAt = :usedAt
        WHERE t.userId = :userId AND t.used = false
        """)
int invalidateAllTokensForUser(
        @Param("userId") Long userId,
        @Param("usedAt") LocalDateTime usedAt
);

// ✅ CORRECTO: DELETE query parametrizada
@Modifying
@Query("""
        DELETE FROM PasswordResetTokenEntity t
        WHERE t.expiresAt < :currentTime
        """)
int deleteByExpiresAtBefore(@Param("currentTime") LocalDateTime currentTime);
```

**Análisis**:
- ✅ Usa `@Modifying` para operaciones que modifican datos
- ✅ Todos los parámetros están parametrizados
- ✅ No hay concatenación de strings
- ✅ NO es vulnerable a SQL injection

---

### ✅ SEGURO: Métodos Derivados de Spring Data

**Archivo**: `UserJpaRepository.java`

```java
// ✅ CORRECTO: Métodos derivados (Spring Data JPA)
Optional<UserEntity> findByEmail(String email);
boolean existsByEmail(String email);
List<UserEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
```

**Análisis**:
- ✅ Spring Data JPA genera queries parametrizadas automáticamente
- ✅ No requiere @Query annotation
- ✅ Totalmente seguro contra SQL injection
- ✅ Implementación estándar de Spring Data

---

### ⚠️ REVISAR: Uso de Native Query

**Archivo**: `ReorderPropertyImagesUseCaseImpl.java` (línea 35)

```java
// ⚠️ Native query sin parámetros
entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
```

**Análisis**:
- ⚠️ Usa SQL nativo (createNativeQuery)
- ✅ **NO es vulnerable**: Query es fija, sin parámetros de usuario
- ✅ Solo ejecuta comando PostgreSQL de configuración
- ✅ No hay interpolación de variables
- ⚠️ **Recomendación**: Agregar comentario explicativo

**Recomendación de Mejora**:
```java
// SECURITY: Fixed SQL command for PostgreSQL constraint deferral
// No user input - safe from SQL injection
entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
```

---

## Buenas Prácticas Identificadas

### 1. Uso Consistente de JPA/JPQL

✅ **La aplicación usa JPA/JPQL en lugar de SQL nativo**

```java
// ✅ JPQL (Java Persistence Query Language)
@Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")

// ❌ Evita SQL nativo innecesario
// @Query(value = "SELECT * FROM users WHERE role_name = :roleName", nativeQuery = true)
```

**Beneficios**:
- Abstracción de base de datos
- Validación en tiempo de compilación
- Tipo seguro
- Protección automática contra SQL injection

### 2. Parámetros Nombrados (@Param)

✅ **Todos los parámetros usan nombres descriptivos**

```java
// ✅ CORRECTO
@Query("SELECT t FROM Token t WHERE t.userId = :userId")
Optional<Token> findByUserId(@Param("userId") Long userId);

// ❌ EVITAR: Parámetros posicionales (menos legibles)
// @Query("SELECT t FROM Token t WHERE t.userId = ?1")
// Optional<Token> findByUserId(Long userId);
```

### 3. Text Blocks para Queries Complejas

✅ **Uso de text blocks (""") para queries multilínea**

```java
// ✅ CORRECTO: Legible y mantenible
@Query("""
        SELECT t FROM PasswordResetTokenEntity t
        WHERE t.userId = :userId
          AND t.used = false
          AND t.expiresAt > :currentTime
        ORDER BY t.createdAt DESC
        LIMIT 1
        """)
```

### 4. Separation of Concerns

✅ **Lógica de negocio separada de acceso a datos**

```
domain/
  └─ model/          # Modelos de dominio (sin SQL)

application/
  └─ usecase/        # Casos de uso (orquestación)

infrastructure/
  └─ persistence/    # Repositories (SQL aquí)
```

---

## Verificación de Seguridad

### Checklist de Validación

- [x] **No hay concatenación de strings en queries SQL**
- [x] **Todos los @Query usan parámetros nombrados**
- [x] **No hay interpolación de variables en SQL**
- [x] **EntityManager.createNativeQuery solo para comandos fijos**
- [x] **PreparedStatements implícitos vía JPA**
- [x] **No hay ejecución de SQL dinámico**
- [x] **Validación de entrada en capa de aplicación**
- [x] **DTO validation con @Valid**

### Tests de Seguridad Recomendados

```java
// Test de SQL Injection en búsqueda por email
@Test
void shouldNotBeVulnerableToSQLInjection() {
    String maliciousEmail = "test@example.com' OR '1'='1";

    // Intento de SQL injection
    Optional<UserEntity> user = userRepository.findByEmail(maliciousEmail);

    // Debe retornar Optional.empty() en lugar de todos los usuarios
    assertThat(user).isEmpty();
}
```

---

## Patrones Seguros Recomendados

### Pattern 1: Repository con @Query

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ SEGURO: Parámetros nombrados
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = :active")
    Optional<User> findByEmailAndActive(
        @Param("email") String email,
        @Param("active") boolean active
    );
}
```

### Pattern 2: Criteria API para Queries Dinámicas

```java
// Para queries dinámicas, usar Criteria API en lugar de concatenación
@Service
public class UserSearchService {

    @PersistenceContext
    private EntityManager em;

    public List<User> searchUsers(UserSearchCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getEmail() != null) {
            predicates.add(cb.equal(user.get("email"), criteria.getEmail()));
        }

        if (criteria.getActive() != null) {
            predicates.add(cb.equal(user.get("active"), criteria.getActive()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(query).getResultList();
    }
}
```

### Pattern 3: Specification Pattern

```java
// Usar Spring Data Specifications para queries complejas
public interface UserRepository extends JpaRepository<User, Long>,
                                         JpaSpecificationExecutor<User> {}

public class UserSpecifications {

    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }
}

// Uso seguro
List<User> users = userRepository.findAll(
    UserSpecifications.hasEmail(email)
        .and(UserSpecifications.isActive())
);
```

---

## Anti-Patrones a Evitar

### ❌ INSEGURO: Concatenación de Strings

```java
// ❌ NUNCA HACER ESTO
@Query(value = "SELECT * FROM users WHERE email = '" + email + "'", nativeQuery = true)
List<User> findByEmailUnsafe(String email);

// ❌ NUNCA HACER ESTO
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
em.createNativeQuery(sql).getResultList();
```

**Riesgo**: Permite SQL injection como `email = "' OR '1'='1"`

### ❌ INSEGURO: Interpolación de Strings

```java
// ❌ NUNCA HACER ESTO
@Query(value = "SELECT * FROM users WHERE email = " + "${email}", nativeQuery = true)
List<User> findByEmailUnsafe(@Param("email") String email);
```

### ❌ INSEGURO: Dynamic SQL sin Validación

```java
// ❌ NUNCA HACER ESTO
public List<User> searchUsers(String orderBy) {
    String sql = "SELECT * FROM users ORDER BY " + orderBy;
    return em.createNativeQuery(sql, User.class).getResultList();
}
```

**Riesgo**: `orderBy = "id; DROP TABLE users;--"`

---

## Recomendaciones Adicionales

### 1. Input Validation

```java
// Validar entrada antes de usarla en queries
public User findByEmail(String email) {
    // Validación de formato
    if (!EmailValidator.isValid(email)) {
        throw new IllegalArgumentException("Invalid email format");
    }

    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email));
}
```

### 2. Query Logging (Desarrollo)

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Beneficio**: Ver los valores de los parámetros en logs para verificar correcta parametrización.

### 3. Prepared Statement Cache

```yaml
# application.yml
spring:
  datasource:
    hikari:
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### 4. Database User Permissions

```sql
-- Principio de mínimo privilegio
-- Usuario de aplicación NO debe tener permisos de DROP, ALTER, etc.

CREATE USER gydi_app WITH PASSWORD 'secure_password';

-- Solo permisos necesarios
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA users TO gydi_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA users TO gydi_app;

-- NO otorgar
-- REVOKE CREATE, DROP, ALTER ON SCHEMA users FROM gydi_app;
```

---

## Plan de Tests de SQL Injection

### Test 1: Email Injection

```java
@Test
@DisplayName("Should prevent SQL injection via email parameter")
void testEmailSQLInjection() {
    String[] maliciousEmails = {
        "admin' OR '1'='1",
        "admin'--",
        "admin' OR '1'='1' --",
        "admin'; DROP TABLE users;--",
        "' UNION SELECT * FROM users--"
    };

    for (String maliciousEmail : maliciousEmails) {
        Optional<User> result = userRepository.findByEmail(maliciousEmail);

        // Debe retornar empty, no todos los usuarios
        assertThat(result).isEmpty();
    }
}
```

### Test 2: Order By Injection

```java
@Test
@DisplayName("Should prevent SQL injection via dynamic ordering")
void testOrderBySQLInjection() {
    String maliciousOrderBy = "id; DROP TABLE users;--";

    // Si tu aplicación permite ordenamiento dinámico, debe validarlo
    assertThatThrownBy(() ->
        userService.findAllOrdered(maliciousOrderBy)
    ).isInstanceOf(IllegalArgumentException.class)
     .hasMessageContaining("Invalid order by field");
}
```

### Test 3: Search Parameter Injection

```java
@Test
@DisplayName("Should prevent SQL injection via search parameters")
void testSearchSQLInjection() {
    String maliciousSearch = "test%' OR '1'='1";

    List<User> results = userRepository.findByNameContaining(maliciousSearch);

    // Debe buscar literalmente la cadena, no ejecutar SQL
    // Si no existe un usuario con ese nombre exacto, debe retornar lista vacía
    assertThat(results).isEmpty();
}
```

---

## Herramientas de Análisis Estático

### 1. FindBugs/SpotBugs

```xml
<!-- pom.xml -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.0</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.12.0</version>
            </plugin>
        </plugins>
    </configuration>
</plugin>
```

**Ejecutar**:
```bash
./mvnw spotbugs:check
```

### 2. OWASP Dependency Check

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

**Ejecutar**:
```bash
./mvnw dependency-check:check
```

### 3. SonarQube

```bash
./mvnw sonar:sonar \
  -Dsonar.projectKey=gydi \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

---

## Cumplimiento de Estándares

### OWASP Top 10 2021

- ✅ **A03:2021 - Injection**: No se encontraron vulnerabilidades de SQL Injection
- ✅ Uso correcto de prepared statements vía JPA
- ✅ No hay concatenación de input de usuario en SQL
- ✅ Validación de entrada implementada

### CWE

- ✅ **CWE-89**: SQL Injection - MITIGADO
- ✅ **CWE-564**: SQL Injection: Hibernate - MITIGADO
- ✅ **CWE-943**: Improper Neutralization of Special Elements in Data Query Logic - MITIGADO

### PCI DSS

- ✅ **Requirement 6.5.1**: Injection flaws, particularly SQL injection
- ✅ Aplicación usa consultas parametrizadas
- ✅ No hay construcción dinámica de SQL con entrada de usuario

---

## Conclusiones

### Fortalezas

✅ **Excelente uso de Spring Data JPA**
✅ **Todas las queries usan parámetros nombrados**
✅ **No se encontró concatenación de strings en SQL**
✅ **Arquitectura hexagonal separa lógica de persistencia**
✅ **Validación de DTOs con @Valid**

### Riesgos Identificados

⚠️ **BAJO**: Un uso de createNativeQuery (pero es seguro - query fija)

### Recomendaciones

1. ✅ **Continuar usando Spring Data JPA** - Es la mejor práctica
2. ✅ **Agregar tests de SQL injection** - Para verificación continua
3. ✅ **Implementar análisis estático** - FindSecBugs, SonarQube
4. ✅ **Documentar queries nativas** - Explicar por qué son seguras
5. ✅ **Capacitación del equipo** - Mantener conciencia de seguridad

---

## Estado Final

🎉 **APROBADO PARA PRODUCCIÓN**

**Nivel de Seguridad SQL Injection**: 🟢 **ALTO**

La aplicación GYDI 2.0 sigue todas las mejores prácticas para prevención de SQL Injection y está lista para despliegue en producción.

---

**Próxima Auditoría**: Trimestral (Febrero 2026)
**Responsable**: Security Team
**Aprobado por**: CTO

**Fecha**: 11 de Noviembre, 2025
**Versión del Documento**: 1.0