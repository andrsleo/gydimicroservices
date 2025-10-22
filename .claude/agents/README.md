# 🤖 Agentes IA - Backend (GydiMicroservices)

Agentes especializados para el proyecto backend de GYDI 2.0.

---

## 📋 Agentes Disponibles

| # | Agente | Rol | Stack Principal |
|---|--------|-----|-----------------|
| **2** | **Backend_AI** | Backend Lead | Java 21, Spring Boot 3.5.5, Hexagonal Architecture |
| **5** | **DevOps_AI** | DevOps Specialist | AWS, Kubernetes, Terraform, GitHub Actions |
| **6** | **QA_AI** | QA Specialist | JUnit, Mockito, TestContainers, REST Assured |

---

## 🚀 Inicio Rápido

### Para Tareas Backend

**Opción 1: Usar Orchestrator (Recomendado para tareas complejas)**
```bash
# Desde la raíz del proyecto
cat ../../.claude/agents/0_Orchestrator.md | pbcopy
```

**Opción 2: Usar Backend_AI directamente (Para tareas específicas)**
```bash
# Desde GydiMicroservices/
cat .claude/agents/2_Backend_AI.md | pbcopy
```

---

## 🎯 Cuándo Usar Cada Agente

### Backend_AI (#2)
**Úsalo para:**
- ✅ Implementar bounded contexts (domain/application/infrastructure)
- ✅ Crear REST endpoints
- ✅ Diseñar domain models y use cases
- ✅ Implementar puertos y adaptadores
- ✅ Crear Flyway migrations
- ✅ Configurar Spring Security

**Automáticamente invoca:**
- AIArchitect (#9) - Después de implementaciones
- AICodeMentor (#10) - Antes de marcar como completado

**Ejemplo:**
```
Usuario: "Implementa el bounded context de subscriptions"
Backend_AI: [Crea domain/, application/, infrastructure/, tests, migration]
→ AIArchitect valida arquitectura hexagonal
→ AICodeMentor revisa código
```

### DevOps_AI (#5)
**Úsalo para:**
- ✅ Configurar CI/CD pipelines (GitHub Actions)
- ✅ Crear infraestructura AWS (Terraform)
- ✅ Configurar Kubernetes/EKS
- ✅ Setup de monitoring (Prometheus, Grafana)
- ✅ Configurar bases de datos (RDS, ElastiCache)
- ✅ Manejo de secrets (AWS Secrets Manager)

**Ejemplo:**
```
Usuario: "Configura CI/CD para deployment en staging"
DevOps_AI: [Crea .github/workflows, configura AWS, K8s manifests]
```

### QA_AI (#6)
**Úsalo para:**
- ✅ Crear tests unitarios (JUnit + Mockito)
- ✅ Tests de integración (TestContainers)
- ✅ API contract testing (REST Assured)
- ✅ Performance testing (JMeter, k6)
- ✅ Estrategia de testing completa
- ✅ CI/CD test integration

**Ejemplo:**
```
Usuario: "Crea suite completa de tests para subscriptions BC"
QA_AI: [Tests unitarios, integración, API, performance]
```

---

## 🏗️ Arquitectura del Proyecto

```
GydiMicroservices/
├── src/main/java/com/affiliate/rentals/gydi/
│   ├── users/                    # Bounded Context: Usuarios
│   │   ├── domain/              # ← Backend_AI
│   │   ├── application/         # ← Backend_AI
│   │   └── infrastructure/      # ← Backend_AI
│   │
│   ├── properties/              # Bounded Context: Propiedades
│   ├── bookings/                # Bounded Context: Reservas
│   ├── subscriptions/           # Bounded Context: Suscripciones
│   │
│   └── shared/                  # Cross-cutting concerns
│       ├── config/              # ← DevOps_AI (configs)
│       └── security/            # ← Backend_AI (JWT, RBAC)
│
├── src/test/java/               # ← QA_AI (todos los tests)
│
├── .github/workflows/           # ← DevOps_AI (CI/CD)
│
└── infrastructure/              # ← DevOps_AI (Terraform, K8s)
    ├── terraform/
    └── k8s/
```

---

## ✅ Workflow Recomendado

### Implementar Nueva Feature Backend

```markdown
1. Orchestrator (#0) coordina
   ↓
2. Backend_AI (#2) implementa
   - Domain models (pure Java, no framework)
   - Ports (interfaces)
   - Use cases
   - Adapters (JPA, REST)
   - Flyway migration
   ↓
3. AIArchitect (#9) valida
   - Separación de capas
   - Arquitectura hexagonal >95%
   ↓
4. AICodeMentor (#10) revisa
   - Code smells
   - SOLID principles
   - Clean Code
   ↓
5. QA_AI (#6) crea tests
   - Unit tests (>80% coverage)
   - Integration tests
   - API contract tests
   ↓
6. DevOps_AI (#5) deployment
   - CI/CD pipeline
   - Deploy a staging
```

---

## 📐 Principios Arquitectónicos

### Arquitectura Hexagonal (Todos los Bounded Contexts)

```
domain/                    # ← CORE del negocio
  ├── model/              # Entities, Value Objects, Aggregates
  ├── ports/              # Interfaces (RepositoryPort, ServicePort)
  └── exception/          # Domain exceptions

application/               # ← Casos de uso
  ├── dto/                # DTOs (Java records)
  ├── usecase/            # Use case implementations
  └── mapper/             # Mappers (MapStruct)

infrastructure/            # ← Detalles técnicos
  ├── in/rest/           # REST controllers
  └── out/persistence/    # JPA adapters
```

**Reglas:**
- ❌ Domain NO puede importar Spring, JPA, ni nada de infrastructure
- ✅ Domain define ports, infrastructure los implementa
- ✅ DTOs son Java records inmutables
- ✅ Use cases orquestan domain logic

---

## 🔗 Agentes Globales

Para validaciones y coordinación, estos agentes están en `../../.claude/agents/`:

| # | Agente | Cuándo Invocar |
|---|--------|---------------|
| **0** | **Orchestrator** | Punto de entrada para tareas complejas |
| **8** | **PM_AI** | Definir user stories, OKRs |
| **9** | **AIArchitect** | Validar arquitectura (automático) |
| **10** | **AICodeMentor** | Code review (automático) |

---

## 💡 Tips

1. **Progressive Disclosure**: Backend_AI usa Glob → Grep → Read (nunca carga todo)
2. **Sub-Agentes**: Backend_AI invoca automáticamente #9 y #10
3. **Bounded Contexts**: Cada BC es autónomo, sin dependencias cruzadas
4. **Tests**: >80% coverage es obligatorio antes de merge

---

## 📚 Documentación Adicional

- **Proyecto General**: `../../CLAUDE.md`
- **Backend Específico**: `../../GydiMicroservices/CLAUDE.md`
- **Sistema de Agentes**: `../../.claude/agents/README.md`

---

**Última actualización:** Octubre 2025
**Versión:** 1.0 (Multi-Proyecto)