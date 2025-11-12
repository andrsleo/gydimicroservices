# Pruebas de Rate Limiting - GYDI Microservices

## Configuración Implementada

### Rate Limits por Endpoint

| Endpoint | Límite | Ventana de Tiempo | Propósito |
|----------|--------|-------------------|-----------|
| `/api/v1/auth/login` | 5 intentos | 15 minutos | Prevenir brute force |
| `/api/v1/auth/register` | 5 intentos | 15 minutos | Prevenir spam de cuentas |
| `/api/v1/auth/forgot-password` | 3 intentos | 1 hora | Prevenir abuso de reset |
| General API | 100 requests | 1 minuto | Prevenir DoS |

### Algoritmo: Token Bucket (Bucket4j)

- Cada IP obtiene su propio "bucket" (cubeta) de tokens
- Cada request consume 1 token
- Los tokens se rellenan automáticamente después del tiempo especificado
- Si no hay tokens disponibles → `429 Too Many Requests`

---

## Pruebas Manuales

### Test 1: Verificar límite de login (5 intentos / 15 min)

```bash
#!/bin/bash

echo "=== Test 1: Rate Limiting en /login ==="
echo ""

# Intentar login 7 veces (límite es 5)
for i in {1..7}; do
  echo -n "Intento $i: "

  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong-password"}')

  if [ "$RESPONSE" -eq 429 ]; then
    echo "❌ Rate limited (429) - Esperado después del intento 5"
  elif [ "$RESPONSE" -eq 401 ]; then
    echo "✅ Unauthorized (401) - Intentando autenticar"
  else
    echo "⚠️  Unexpected: $RESPONSE"
  fi

  sleep 1
done

echo ""
echo "Resultado esperado:"
echo "  Intentos 1-5: 401 Unauthorized"
echo "  Intentos 6-7: 429 Too Many Requests"
```

**Salida esperada:**
```
=== Test 1: Rate Limiting en /login ===

Intento 1: ✅ Unauthorized (401) - Intentando autenticar
Intento 2: ✅ Unauthorized (401) - Intentando autenticar
Intento 3: ✅ Unauthorized (401) - Intentando autenticar
Intento 4: ✅ Unauthorized (401) - Intentando autenticar
Intento 5: ✅ Unauthorized (401) - Intentando autenticar
Intento 6: ❌ Rate limited (429) - Esperado después del intento 5
Intento 7: ❌ Rate limited (429) - Esperado después del intento 5
```

---

### Test 2: Verificar headers de rate limit

```bash
# Hacer request y ver headers
curl -v -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}' \
  2>&1 | grep -i "x-ratelimit"

# Después de exceder el límite
curl -v -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}' \
  2>&1 | grep -i "x-ratelimit"
```

**Headers esperados:**
```
X-RateLimit-Remaining: 4   # Después del 1er intento
X-RateLimit-Remaining: 3   # Después del 2do intento
X-RateLimit-Remaining: 0   # Después del 5to intento
X-RateLimit-Retry-After: 900  # 15 minutos en segundos
```

---

### Test 3: Verificar que diferentes IPs tienen límites independientes

```bash
# IP 1 (default)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}'

# IP 2 (simulada con X-Forwarded-For)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -d '{"email":"test@example.com","password":"wrong"}'

# IP 3 (otra IP simulada)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 192.168.1.200" \
  -d '{"email":"test@example.com","password":"wrong"}'
```

**Resultado esperado:**
Cada IP tiene su propio límite independiente. Si una IP se queda sin tokens, otra IP aún puede hacer requests.

---

### Test 4: Verificar recuperación de tokens después de 15 minutos

```bash
#!/bin/bash

echo "=== Test 4: Recuperación de tokens ==="
echo ""

# Agotar el límite (5 intentos)
for i in {1..5}; do
  curl -s -o /dev/null -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}'
done

echo "Límite agotado. Esperando 15 minutos..."
echo "(En producción. Para testing, puedes reducir el tiempo en RateLimitService)"

# Esperar 15 minutos
sleep 900

# Intentar de nuevo
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}'
)

if [ "$RESPONSE" -eq 401 ]; then
  echo "✅ Tokens recuperados - Request permitido (401 Unauthorized)"
else
  echo "❌ Aún bloqueado - $RESPONSE"
fi
```

---

## Pruebas Automatizadas (JUnit)

### Crear test de integración

```java
// src/test/java/com/affiliate/rentals/gydi/users/infrastructure/in/rest/AuthControllerRateLimitTest.java

package com.affiliate.rentals.gydi.users.infrastructure.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowFirstFiveLoginAttempts() throws Exception {
        String loginPayload = """
            {
                "email": "test@example.com",
                "password": "wrong-password"
            }
            """;

        // First 5 attempts should be allowed (but fail with 401)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                    .andExpect(status().isUnauthorized()); // 401
        }
    }

    @Test
    void shouldBlockSixthLoginAttempt() throws Exception {
        String loginPayload = """
            {
                "email": "test@example.com",
                "password": "wrong-password"
            }
            """;

        // Exhaust the 5 attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isTooManyRequests()) // 429
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Retry-After"));
    }

    @Test
    void shouldHaveIndependentLimitsPerIp() throws Exception {
        String loginPayload = """
            {
                "email": "test@example.com",
                "password": "wrong-password"
            }
            """;

        // Exhaust limit for IP 1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "192.168.1.1")
                    .content(loginPayload))
                    .andExpect(status().isUnauthorized());
        }

        // IP 1 should be blocked
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "192.168.1.1")
                .content(loginPayload))
                .andExpect(status().isTooManyRequests());

        // IP 2 should still be allowed
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "192.168.1.2")
                .content(loginPayload))
                .andExpect(status().isUnauthorized()); // Not rate limited
    }
}
```

**Ejecutar tests:**
```bash
./mvnw test -Dtest=AuthControllerRateLimitTest
```

---

## Configuración para Diferentes Entornos

### Development (testing más rápido)

Para testing más ágil en desarrollo, puedes reducir temporalmente los límites:

```java
// RateLimitService.java - Para TESTING solamente
private Bucket createAuthBucket() {
    Bandwidth limit = Bandwidth.classic(
        3,  // 3 tokens (en lugar de 5)
        Refill.intervally(3, Duration.ofMinutes(1))  // 1 minuto (en lugar de 15)
    );
    return Bucket.builder().addLimit(limit).build();
}
```

### Production (configuración actual)

```java
private Bucket createAuthBucket() {
    Bandwidth limit = Bandwidth.classic(
        5,  // 5 attempts
        Refill.intervally(5, Duration.ofMinutes(15))  // 15 minutes
    );
    return Bucket.builder().addLimit(limit).build();
}
```

---

## Monitoreo en Producción

### Logs a buscar

```bash
# Ver cuando un IP es bloqueado
grep "Rate limit exceeded" /var/log/gydi/application.log

# Ejemplo de salida:
# 2025-11-10 10:30:45 - WARN - Rate limit exceeded for authentication endpoint. IP: 192.168.1.100
```

### Métricas importantes

1. **Número de requests bloqueados por hora**
   - Si es muy alto → Puede ser un ataque
   - Si es moderado → Usuarios legítimos con problemas

2. **IPs únicas bloqueadas**
   - Muchas IPs diferentes → Ataque distribuido
   - Una sola IP → Usuario con problemas o atacante individual

3. **Patrón temporal**
   - Picos nocturnos → Posible bot
   - Horario laboral → Usuarios legítimos

### Comandos útiles de administración

```bash
# Ver IPs bloqueadas actualmente (desde logs)
grep "Rate limit exceeded" /var/log/gydi/application.log | \
  awk '{print $NF}' | sort | uniq -c | sort -rn

# Output ejemplo:
#  15 192.168.1.100  # Esta IP ha sido bloqueada 15 veces
#   8 192.168.1.200
#   3 192.168.1.150
```

---

## Troubleshooting

### Problema: Un usuario legítimo está bloqueado

**Solución temporal (admin):**
```java
// Crear un endpoint de admin para limpiar rate limit de una IP específica
@PostMapping("/admin/rate-limit/clear/{ip}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> clearRateLimitForIp(@PathVariable String ip) {
    rateLimitService.clearRateLimitForIp(ip);
    return ResponseEntity.ok().build();
}
```

```bash
# Uso
curl -X POST http://localhost:8080/api/v1/admin/rate-limit/clear/192.168.1.100 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Problema: Rate limiting no funciona

**Verificar:**
1. Bucket4j está en el classpath: `./mvnw dependency:tree | grep bucket4j`
2. RateLimitService está siendo inyectado: Ver logs de Spring Boot
3. El código de rate limiting se está ejecutando: Agregar logs

**Debug:**
```java
// Agregar logs temporales en AuthController
log.info("Checking rate limit for IP: {}", getClientIp(httpRequest));
if (!rateLimitService.tryConsumeAuth(httpRequest)) {
    log.warn("RATE LIMIT BLOCKED");
    // ...
}
log.info("Rate limit check passed");
```

### Problema: Los headers X-RateLimit no aparecen

**Causa:** CORS puede estar bloqueando headers custom.

**Solución:**
```java
// SecurityConfig.java - Agregar headers permitidos
configuration.setExposedHeaders(Arrays.asList(
    "Authorization",
    "X-RateLimit-Remaining",
    "X-RateLimit-Retry-After"
));
```

---

## Mejoras Futuras

### 1. Rate Limiting basado en usuario (no solo IP)

```java
// Combinar IP + email para un límite más granular
private String getRateLimitKey(HttpServletRequest request, String email) {
    String ip = getClientIp(request);
    return email != null ? ip + ":" + email : ip;
}
```

### 2. Rate Limiting distribuido (Redis)

Para múltiples instancias de la aplicación:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>7.6.0</version>
</dependency>
```

```java
// Usar Redis para compartir buckets entre instancias
@Bean
public ProxyManager<String> proxyManager(RedissonClient redisson) {
    return new RedissonBasedProxyManager<>(redisson);
}
```

### 3. Rate limiting dinámico basado en riesgo

- Usuario con buena reputación → Más requests permitidos
- Usuario nuevo → Límite estricto
- IP sospechosa → Límite muy bajo

---

## Referencias

- [Bucket4j Documentation](https://bucket4j.com/)
- [OWASP Rate Limiting Guide](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html)
- [RFC 6585 - HTTP Status Code 429](https://tools.ietf.org/html/rfc6585)

---

**Última actualización:** Noviembre 2025
**Versión:** 1.0
**Librería:** Bucket4j 7.6.0
