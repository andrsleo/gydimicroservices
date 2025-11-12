# Pruebas de Seguridad - Actuator Endpoints

## Objetivo
Verificar que los endpoints de Spring Boot Actuator están correctamente protegidos.

## Configuración Actual

### Endpoints Públicos (sin autenticación):
- ✅ `/actuator/health` - Solo muestra estado básico
- ✅ `/actuator/info` - Información general de la aplicación

### Endpoints Protegidos (requieren rol ADMIN):
- 🔒 `/actuator/metrics` - Métricas de la aplicación
- 🔒 `/actuator/env` - Variables de entorno (valores ocultos sin autorización)

### Endpoints Deshabilitados:
- ❌ `/actuator/shutdown` - Apagar la aplicación
- ❌ `/actuator/threaddump` - Volcado de threads
- ❌ `/actuator/heapdump` - Volcado de memoria
- ❌ `/actuator/logfile` - Archivos de log

---

## Pruebas Manuales

### 1. Test de endpoint público /health

```bash
# Sin autenticación - debe funcionar
curl http://localhost:8080/actuator/health

# Respuesta esperada (sin detalles):
{
  "status": "UP"
}
```

### 2. Test de endpoint público /info

```bash
# Sin autenticación - debe funcionar
curl http://localhost:8080/actuator/info

# Respuesta esperada:
{
  "app": {
    "name": "GYDI Microservices",
    "version": "1.0.0"
  }
}
```

### 3. Test de endpoint protegido /metrics SIN token

```bash
# Sin autenticación - debe devolver 401 o 403
curl http://localhost:8080/actuator/metrics

# Respuesta esperada:
{
  "timestamp": "2024-11-07T...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/actuator/metrics"
}
```

### 4. Test de endpoint protegido /metrics CON token de usuario normal

```bash
# Primero obtener token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.accessToken')

# Intentar acceder con token de HOST (no ADMIN) - debe fallar
curl http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer $TOKEN"

# Respuesta esperada:
{
  "timestamp": "2024-11-07T...",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/actuator/metrics"
}
```

### 5. Test de endpoint protegido /metrics CON token de ADMIN

```bash
# Obtener token de ADMIN
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gydi.com","password":"admin123"}' \
  | jq -r '.accessToken')

# Acceder con token de ADMIN - debe funcionar
curl http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Respuesta esperada:
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "http.server.requests",
    ...
  ]
}
```

### 6. Test de endpoint deshabilitado /shutdown

```bash
# Intentar acceder al endpoint de shutdown - debe devolver 404
curl -X POST http://localhost:8080/actuator/shutdown \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Respuesta esperada:
{
  "timestamp": "2024-11-07T...",
  "status": 404,
  "error": "Not Found",
  "message": "No message available",
  "path": "/actuator/shutdown"
}
```

### 7. Test de endpoint deshabilitado /heapdump

```bash
# Intentar descargar heap dump - debe devolver 404
curl http://localhost:8080/actuator/heapdump \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Respuesta esperada:
{
  "timestamp": "2024-11-07T...",
  "status": 404,
  "error": "Not Found"
}
```

### 8. Test de endpoint /env con valores ocultos

```bash
# Sin autenticación - debe mostrar propiedades pero sin valores sensibles
curl http://localhost:8080/actuator/env

# Con token ADMIN - debe mostrar valores completos
curl http://localhost:8080/actuator/env \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Pruebas con Script Automatizado

Puedes ejecutar todas las pruebas con este script:

```bash
#!/bin/bash

echo "=== Pruebas de Seguridad - Actuator ==="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: /health público
echo -n "Test 1: /health sin auth... "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
if [ "$RESPONSE" -eq 200 ]; then
    echo -e "${GREEN}✓ PASS${NC} ($RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} ($RESPONSE)"
fi

# Test 2: /info público
echo -n "Test 2: /info sin auth... "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/info)
if [ "$RESPONSE" -eq 200 ]; then
    echo -e "${GREEN}✓ PASS${NC} ($RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} ($RESPONSE)"
fi

# Test 3: /metrics sin auth - debe fallar
echo -n "Test 3: /metrics sin auth... "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/metrics)
if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
    echo -e "${GREEN}✓ PASS${NC} (Bloqueado: $RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (Debería estar bloqueado: $RESPONSE)"
fi

# Test 4: /shutdown deshabilitado
echo -n "Test 4: /shutdown deshabilitado... "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/actuator/shutdown)
if [ "$RESPONSE" -eq 404 ]; then
    echo -e "${GREEN}✓ PASS${NC} (Deshabilitado: $RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (Debería estar deshabilitado: $RESPONSE)"
fi

# Test 5: /heapdump deshabilitado
echo -n "Test 5: /heapdump deshabilitado... "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/heapdump)
if [ "$RESPONSE" -eq 404 ]; then
    echo -e "${GREEN}✓ PASS${NC} (Deshabilitado: $RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (Debería estar deshabilitado: $RESPONSE)"
fi

echo ""
echo "=== Pruebas completadas ==="
```

Guarda esto como `test-actuator-security.sh` y ejecútalo:

```bash
chmod +x test-actuator-security.sh
./test-actuator-security.sh
```

---

## Resultados Esperados

### ✅ Todos los tests deben pasar:

1. `/actuator/health` → **200 OK** (público)
2. `/actuator/info` → **200 OK** (público)
3. `/actuator/metrics` sin auth → **401/403** (bloqueado)
4. `/actuator/shutdown` → **404 Not Found** (deshabilitado)
5. `/actuator/heapdump` → **404 Not Found** (deshabilitado)
6. `/actuator/metrics` con token ADMIN → **200 OK** (permitido)

---

## Configuración en Producción

Al desplegar en producción con `--spring.profiles.active=prod`:

- ✅ Solo `/health` y `/info` expuestos
- ✅ `/health` NO muestra detalles (solo UP/DOWN)
- ✅ Swagger UI completamente deshabilitado
- ✅ Mensajes de error genéricos (sin stack traces)
- ✅ Todos los endpoints peligrosos deshabilitados

---

## Troubleshooting

### Problema: Actuator devuelve 404 en todos los endpoints

**Causa:** El contexto de Actuator está deshabilitado o el puerto es diferente.

**Solución:**
```yaml
management:
  endpoints:
    web:
      base-path: /actuator
  server:
    port: 8080  # Usar el mismo puerto que la aplicación
```

### Problema: /metrics devuelve 200 sin autenticación

**Causa:** La configuración de seguridad no se aplicó correctamente.

**Solución:** Verifica que `SecurityConfig.java` tenga:
```java
.requestMatchers("/actuator/**").hasRole("ADMIN")
```

### Problema: ADMIN no puede acceder a /metrics

**Causa:** El usuario no tiene el rol ADMIN o el token es inválido.

**Solución:**
1. Verifica el rol del usuario en la base de datos
2. Regenera el token de autenticación
3. Verifica que el header Authorization esté bien formado

---

## Referencias

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Security Configuration](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
- [OWASP Spring Boot Security Guide](https://cheatsheetseries.owasp.org/cheatsheets/Spring_Security_Cheat_Sheet.html)
