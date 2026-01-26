# Comandos para Desarrollo Local - Spring Boot

Este documento contiene todos los comandos necesarios para trabajar con el proyecto Spring Boot localmente.

## 📁 Navegación al Proyecto

```bash
cd "/Users/andresvargas/Documents/Project GYDI 2.0/GydiMicroservices"
```

---

## 🧪 Ejecutar Tests

### Tests Básicos

```bash
# Ejecutar TODOS los tests
./mvnw test

# Ejecutar tests con reporte de cobertura
./mvnw test jacoco:report

# Ejecutar una clase de test específica
./mvnw test -Dtest=UserServiceTest

# Ejecutar un método de test específico
./mvnw test -Dtest=UserServiceTest#shouldCreateUser

# Ejecutar tests de integración
./mvnw verify

# Ejecutar tests en paralelo (más rápido)
./mvnw -T 1C test
```

---

## 🔨 Compilar el Proyecto

### Compilación Básica

```bash
# Compilar sin ejecutar tests (más rápido)
./mvnw clean compile -DskipTests

# Compilar y ejecutar tests
./mvnw clean compile

# Compilar y empaquetar (genera JAR)
./mvnw clean package -DskipTests

# Compilar, ejecutar tests y empaquetar
./mvnw clean install
```

---

## 🚀 Levantar Spring Boot

### Métodos de Ejecución

```bash
# Método 1: Usando Maven (recomendado para desarrollo)
./mvnw spring-boot:run

# Método 2: Con perfil específico (dev, local, prod)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Método 3: Ejecutar el JAR generado
./mvnw clean package -DskipTests
java -jar target/rentals-0.0.1-SNAPSHOT.jar

# Método 4: Con perfil y variables de entorno
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# Método 5: Con debug habilitado (puerto 5005)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 🔄 Workflow Completo Recomendado

### Opción 1: Con Tests (Más Seguro)

```bash
cd "/Users/andresvargas/Documents/Project GYDI 2.0/GydiMicroservices"

# 1. Limpiar, compilar y ejecutar tests
./mvnw clean install

# 2. Si todo pasó correctamente, levantar la aplicación
./mvnw spring-boot:run
```

### Opción 2: Sin Tests (Más Rápido)

```bash
cd "/Users/andresvargas/Documents/Project GYDI 2.0/GydiMicroservices"

# 1. Limpiar y compilar sin tests
./mvnw clean install -DskipTests

# 2. Levantar la aplicación
./mvnw spring-boot:run
```

### Opción 3: Todo-en-Uno (Un Solo Comando)

```bash
# Con tests
cd "/Users/andresvargas/Documents/Project GYDI 2.0/GydiMicroservices" && \
./mvnw clean install && \
./mvnw spring-boot:run

# Sin tests (más rápido)
cd "/Users/andresvargas/Documents/Project GYDI 2.0/GydiMicroservices" && \
./mvnw clean install -DskipTests && \
./mvnw spring-boot:run
```

---

## 🌍 Variables de Entorno Locales

### Variables Requeridas

Antes de levantar, configura estas variables de entorno:

```bash
# Database
export DB_PASSWORD="tu_password_postgresql"
export DB_USERNAME="postgres"
export DATABASE_URL="jdbc:postgresql://localhost:5432/gydidb"

# JWT Security
export JWT_SECRET="tu_secret_jwt_generado"
export JWT_REFERRAL_SECRET="otro_secret_diferente"

# Encryption
export AES_SECRET_KEY="clave_aes_32_bytes_hex"
export URL_HASHIDS_SALT="salt_para_hashids"

# Cloudinary (File Storage)
export CLOUDINARY_URL="cloudinary://api_key:api_secret@cloud_name"
export STORAGE_PROVIDER="cloudinary"

# Email (opcional para local)
export RESEND_API_KEY="tu_resend_api_key"
export RESEND_FROM_EMAIL="noreply@gydi.com"

# Stripe (Payment Gateway)
export STRIPE_API_KEY="sk_test_..."
export STRIPE_PUBLISHABLE_KEY="pk_test_..."
export STRIPE_WEBHOOK_SECRET="whsec_..."

# Application
export SPRING_PROFILES_ACTIVE="local"
export FRONTEND_URL="http://localhost:3000"
```

### Formas de Configurar Variables (Spring Boot Nativo)

> **NOTA IMPORTANTE:** Spring Boot **NO lee archivos `.env` de forma nativa** (eso es un patrón de Node.js).
> Las variables de entorno se deben exportar en el shell o configurar en tu IDE.

#### Opción 1: Script de Variables (Recomendado para Local)

```bash
# 1. Crear un script FUERA del repositorio (para no commitear secretos)
# Ejemplo: ~/gydi-local-env.sh

#!/bin/bash
export SPRING_PROFILES_ACTIVE=local
export DB_USERNAME=andresvargas
export DB_PASSWORD=Gydi2025@
export DATABASE_URL=jdbc:postgresql://localhost:5432/gydidb

export JWT_SECRET=LjQx8OFV0448l3uEwdCQUw==
export JWT_REFERRAL_SECRET=HgFbV40cv1qE/oDxqc2DXA==

export AES_SECRET_KEY=b7e36bfca9c0b6dafded67f69ac929e5cd96bb19a266340ff05977e8d4594341
export URL_HASHIDS_SALT=Aap4piesmxrF+OK08e+UKLHgJiuhWwofxereBOF9bJk=

export STORAGE_PROVIDER=local
export EMAIL_PROVIDER=local
export FRONTEND_URL=http://localhost:3000
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000

# 2. Dar permisos de ejecución
chmod +x ~/gydi-local-env.sh

# 3. Cargar variables ANTES de ejecutar Spring Boot
source ~/gydi-local-env.sh
./mvnw spring-boot:run
```

#### Opción 2: Exportar Directamente en Terminal

```bash
# Exportar cada variable manualmente en tu terminal
export SPRING_PROFILES_ACTIVE=local
export DB_PASSWORD=Gydi2025@
export JWT_SECRET=LjQx8OFV0448l3uEwdCQUw==
export JWT_REFERRAL_SECRET=HgFbV40cv1qE/oDxqc2DXA==
export AES_SECRET_KEY=b7e36bfca9c0b6dafded67f69ac929e5cd96bb19a266340ff05977e8d4594341
export URL_HASHIDS_SALT=Aap4piesmxrF+OK08e+UKLHgJiuhWwofxereBOF9bJk=

# Luego ejecutar Spring Boot
./mvnw spring-boot:run
```

#### Opción 3: Variables Inline (Una Línea)

```bash
# Variables más importantes en una sola línea
SPRING_PROFILES_ACTIVE=local DB_PASSWORD=Gydi2025@ JWT_SECRET=LjQx8OFV0448l3uEwdCQUw== JWT_REFERRAL_SECRET=HgFbV40cv1qE/oDxqc2DXA== AES_SECRET_KEY=b7e36bfca9c0b6dafded67f69ac929e5cd96bb19a266340ff05977e8d4594341 URL_HASHIDS_SALT=Aap4piesmxrF+OK08e+UKLHgJiuhWwofxereBOF9bJk= ./mvnw spring-boot:run
```

#### Opción 4: Configuración del IDE (IntelliJ IDEA / VS Code)

**IntelliJ IDEA:**
1. Run > Edit Configurations...
2. Selecciona tu configuración de Spring Boot
3. En "Environment variables" agrega:
   ```
   SPRING_PROFILES_ACTIVE=local;DB_PASSWORD=Gydi2025@;JWT_SECRET=...
   ```

**VS Code (launch.json):**
```json
{
  "type": "java",
  "name": "Spring Boot",
  "request": "launch",
  "mainClass": "com.affiliate.rentals.gydi.GydiApplication",
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "DB_PASSWORD": "Gydi2025@",
    "JWT_SECRET": "LjQx8OFV0448l3uEwdCQUw=="
  }
}
```

---

## 🎯 Comandos Útiles Adicionales

### Gestión de Dependencias

```bash
# Ver árbol de dependencias
./mvnw dependency:tree

# Verificar actualizaciones disponibles
./mvnw versions:display-dependency-updates

# Analizar dependencias obsoletas
./mvnw versions:display-plugin-updates
```

### Gestión del Proyecto

```bash
# Limpiar archivos compilados (carpeta target)
./mvnw clean

# Validar configuración del POM
./mvnw validate

# Ver configuración efectiva del POM
./mvnw help:effective-pom

# Generar documentación del proyecto
./mvnw javadoc:javadoc
```

### Flyway (Migraciones de Base de Datos)

```bash
# Ver información de migraciones
./mvnw flyway:info

# Validar migraciones
./mvnw flyway:validate

# Limpiar base de datos (⚠️ CUIDADO - Borra todo)
./mvnw flyway:clean

# Migrar base de datos
./mvnw flyway:migrate
```

---

## ✅ Verificar que la Aplicación Está Corriendo

### 1. Verificar en Consola

La aplicación debe mostrar:

```
Started GydiApplication in X.XXX seconds (process running for Y.YYY)
Tomcat started on port 8080 (http) with context path '/'
```

### 2. Verificar con curl

```bash
# Health check
curl http://localhost:8080/actuator/health

# Info endpoint
curl http://localhost:8080/actuator/info
```

### 3. Abrir en Navegador

```bash
# Swagger UI (Documentación API)
open http://localhost:8080/swagger-ui.html

# Health endpoint
open http://localhost:8080/actuator/health
```

### 4. Verificar Base de Datos

```bash
# Conectar a PostgreSQL
psql -U postgres -d gydidb

# Listar tablas
\dt users.*;
\dt properties.*;
\dt subscriptions.*;

# Ver migraciones aplicadas
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
```

---

## 🛑 Detener la Aplicación

### Si Corriste con Maven

```bash
# Presionar en la terminal:
Ctrl + C
```

### Si Ejecutaste el JAR en Background

```bash
# Encontrar el proceso Java
ps aux | grep java

# Matar el proceso (reemplaza <PID> con el número del proceso)
kill -9 <PID>

# O encontrar y matar en un comando
pkill -f "rentals-0.0.1-SNAPSHOT.jar"
```

---

## 🐛 Debugging

### Ejecutar con Debug Remoto

```bash
# Iniciar con debugger en puerto 5005
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# En IntelliJ IDEA o VS Code:
# 1. Crear configuración "Remote JVM Debug"
# 2. Host: localhost
# 3. Port: 5005
# 4. Conectar debugger
```

### Ver Logs con Más Detalle

```bash
# Ejecutar con nivel de log DEBUG
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.affiliate.rentals.gydi=DEBUG"

# Ver logs de SQL queries
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.org.hibernate.SQL=DEBUG"

# Logs de Spring Security
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.org.springframework.security=DEBUG"
```

---

## 🔥 Comandos Rápidos (Cheatsheet)

```bash
# Desarrollo rápido (sin tests)
./mvnw clean install -DskipTests && ./mvnw spring-boot:run

# Workflow completo (con tests)
./mvnw clean install && ./mvnw spring-boot:run

# Solo tests
./mvnw test

# Solo compilar
./mvnw clean compile -DskipTests

# Limpiar todo
./mvnw clean

# Ver dependencias
./mvnw dependency:tree

# Health check
curl http://localhost:8080/actuator/health

# Detener aplicación
Ctrl + C
```

---

## 📝 Notas Importantes

1. **PostgreSQL debe estar corriendo** antes de iniciar la aplicación
   ```bash
   # Verificar si PostgreSQL está corriendo
   pg_isready

   # Iniciar PostgreSQL (macOS con Homebrew)
   brew services start postgresql@16
   ```

2. **Base de datos `gydidb` debe existir**
   ```bash
   # Crear base de datos si no existe
   psql -U postgres -c "CREATE DATABASE gydidb;"
   ```

3. **Variables de entorno son OBLIGATORIAS**
   - Especialmente: `DB_PASSWORD`, `JWT_SECRET`, `JWT_REFERRAL_SECRET`
   - Sin ellas, la aplicación NO iniciará

4. **Puerto 8080 debe estar libre**
   ```bash
   # Ver qué proceso usa el puerto 8080
   lsof -i :8080

   # Matar proceso en puerto 8080
   kill -9 $(lsof -t -i:8080)
   ```

5. **Flyway ejecuta migraciones automáticamente**
   - Al iniciar, Flyway verifica y aplica migraciones pendientes
   - Ver aplicadas: `SELECT * FROM flyway_schema_history;`

---

**Última Actualización:** Enero 2026
**Versión:** 1.0
**Stack:** Spring Boot 3.5.5 + Java 21 + PostgreSQL 16
