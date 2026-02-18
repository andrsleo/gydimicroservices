# E2E Integration Tests - Commission System

Este directorio contiene tests de integración E2E (End-to-End) para el sistema de comisiones de GYDI 2.0.

## Tests Disponibles

### 1. HostCommissionChargeFlowE2ETest ✅

**Objetivo:** Verificar que el cobro de comisión al HOST funciona correctamente cuando un booking finaliza.

**Escenario Completo:**
```
1. User con plan PRO (tiene payment method configurado)
2. User publica propiedad
3. Otro user hace booking de la propiedad
4. Booking pasa por estados: REQUESTED → RESERVED → IN_PROGRESS → FINISHED
5. Al finalizar (FINISHED):
   ✅ Se crea HostCommission automáticamente
   ✅ Se intenta cobrar inmediatamente vía Stripe
   ✅ Si éxito → status = CHARGED
   ✅ Si falla → status = FAILED, retry programado
```

**Test Cases:**

| Test | Descripción | Expected |
|------|-------------|----------|
| `shouldChargeHostCommissionWhenBookingFinishes()` | Happy path - cobro exitoso | Status = CHARGED, stripe_payment_intent_id guardado |
| `shouldHandlePaymentFailureGracefully()` | Stripe rechaza (insufficient funds) | Status = FAILED, failure_reason guardado, retry_attempts = 1 |
| `shouldNotChargeIfNoPaymentMethod()` | Host sin payment method | Status = FAILED, reason = "No payment method" |
| `shouldCalculateCorrectCommissionAmount()` | Cálculo correcto según plan | PRO = 20% → $1000 booking = $200 comisión |
| `shouldUpdateStatusAtomically()` | Update atómico PENDING → CHARGED | Estado consistente, sin race conditions |

**Mocks:**
- `PaymentGatewayPort` (Stripe) está mockeado para evitar llamadas reales
- Payment methods de test: `pm_test_123456789`

---

## Cómo Ejecutar

### Ejecutar todos los tests de integración

```bash
cd GydiMicroservices
./mvnw test -Dtest=*E2ETest
```

### Ejecutar solo HostCommissionChargeFlowE2ETest

```bash
./mvnw test -Dtest=HostCommissionChargeFlowE2ETest
```

### Ejecutar un test específico

```bash
./mvnw test -Dtest=HostCommissionChargeFlowE2ETest#shouldChargeHostCommissionWhenBookingFinishes
```

### Con debug logging

```bash
./mvnw test -Dtest=HostCommissionChargeFlowE2ETest -Dlogging.level.com.affiliate.rentals.gydi=DEBUG
```

---

## Configuración

### Base de Datos

Los tests usan **H2 in-memory** configurado en `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Cada test se ejecuta en una transacción que hace **rollback** automáticamente (`@Transactional`).

### Perfil de Test

Los tests usan el perfil `test` activado con `@ActiveProfiles("test")`.

---

## Estructura de un Test E2E

```java
@SpringBootTest                    // Carga contexto completo
@ActiveProfiles("test")            // Usa application-test.yml
@Transactional                     // Rollback automático
@DisplayName("E2E Test: ...")
class MyE2ETest {

    @Autowired
    private MyUseCase useCase;     // Inyección de dependencias reales

    @MockBean
    private ExternalService mock;  // Mock de servicios externos (Stripe)

    @BeforeEach
    void setUp() {
        // Preparar datos de test
    }

    @Test
    @DisplayName("Should do something when X happens")
    void shouldDoSomething() {
        // GIVEN: Setup
        // WHEN: Execute
        // THEN: Verify
    }
}
```

---

## Verificaciones en Database

### Ver Host Commissions después del test

```sql
SELECT * FROM commissions.host_commission
WHERE booking_id = ?;
```

**Expected Result:**
```
id | booking_id | host_id | amount_cents | status  | stripe_payment_intent_id | charged_at
1  | 123        | 456     | 20000        | CHARGED | pi_test_123456789        | 2025-10-20 14:30:00
```

### Ver Payment Methods

```sql
SELECT * FROM subscriptions.payment_methods
WHERE user_id = ?;
```

---

## Cobertura de Tests

Este test verifica:

✅ **Domain Logic:**
- Cálculo de comisiones según plan (FREE=25%, PRO=20%, ELITE=15%)
- Estados de comisión (PENDING → CHARGED/FAILED)
- Validaciones de negocio

✅ **Application Layer:**
- Use Cases: CreateCommissionsFromBookingUseCase, ChargeHostCommissionUseCase
- Event Handling: BookingFinishedEvent
- Transaction management

✅ **Infrastructure Layer:**
- Persistencia con JPA
- Integración con PaymentGatewayPort (mockeado)
- Repository operations

✅ **Integration:**
- Flujo completo desde Booking FINISHED hasta Commission CHARGED
- Coordinación entre bounded contexts (bookings → commissions)
- Manejo de errores y retry logic

---

## Próximos Tests

### 2. AffiliatePayoutFlowE2ETest (pendiente)

Verificará:
- Onboarding de affiliate con Stripe Connect
- Acumulación de comisiones (mínimo $50)
- Batch payout el día 1 y 15 del mes
- 7-day dispute period

### 3. FailedChargeRetryFlowE2ETest (pendiente)

Verificará:
- Retry automático (24h, 72h, 168h)
- Exponential backoff
- Property suspension después de 3 fallos
- Notificaciones al host

---

## Troubleshooting

### Error: "Table 'host_commission' not found"

**Causa:** Flyway migrations no se ejecutaron.

**Solución:**
```yaml
# application-test.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### Error: "Bean 'paymentGateway' not found"

**Causa:** PaymentGatewayPort no está mockeado.

**Solución:** Agregar `@MockBean` en la clase de test:
```java
@MockBean
private PaymentGatewayPort paymentGateway;
```

### Error: "Transaction marked as rollback-only"

**Causa:** Exception lanzada dentro de test transaccional.

**Solución:** Usar `assertThatThrownBy()` para verificar exceptions sin romper la transacción.

---

## Referencias

- **Plan de Tests**: `/Project GYDI 2.0/.claude/plans/jiggly-wiggling-salamander.md`
- **Verification E2E Section**: Líneas 870-945
- **Domain Models**: `GydiMicroservices/src/main/java/.../commissions/domain/model/`
- **Use Cases**: `GydiMicroservices/src/main/java/.../commissions/application/usecase/`

---

**Autor:** Backend AI Agent
**Fecha:** Febrero 2026
**Versión:** 1.0
