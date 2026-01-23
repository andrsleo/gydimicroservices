# Railway Environment Variables Configuration

Este documento especifica las variables de entorno requeridas para desplegar GYDI 2.0 en Railway con la nueva implementación de seguridad CSRF.

## 🔐 Variables de Seguridad CSRF (OBLIGATORIAS)

### SPRING_PROFILES_ACTIVE

**Descripción:** Define el perfil de Spring Boot activo, que determina qué archivo de configuración se carga (`application-{profile}.yml`) y las configuraciones específicas de seguridad.

**Valores permitidos:**
- `dev` o `local` - Ambiente de desarrollo (SameSite=Lax, CORS patterns permitidos)
- `prod` o `production` - Ambiente de producción (SameSite=None + CSRF tokens, CORS estricto)

**Configuración por ambiente:**

#### Railway Dev Environment
```bash
SPRING_PROFILES_ACTIVE=dev
```

#### Railway Production Environment
```bash
SPRING_PROFILES_ACTIVE=prod
```

**Impacto:**
- **Archivo de configuración:** Carga `application-dev.yml` o `application-prod.yml`
- **SameSite Cookies:** Perfiles `dev`/`local` usan `Lax`, perfiles `prod`/`production` usan `None`
- **CORS:** Development puede usar patterns, production requiere origins explícitos

---

## 🌐 Variables CORS (OBLIGATORIAS)

### CORS_ALLOWED_ORIGINS

**Descripción:** Lista de orígenes permitidos para requests cross-origin (separados por comas).

**IMPORTANTE:** En producción, NUNCA usar wildcards (`*`) con `allowCredentials=true`.

#### Railway Dev Environment
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000,https://gydi-front-next.vercel.app
```

#### Railway Production Environment
```bash
CORS_ALLOWED_ORIGINS=https://gydi-front-next.vercel.app
```

### CORS_ALLOWED_ORIGIN_PATTERNS (OPCIONAL)

**Descripción:** Patterns de orígenes permitidos (para preview deployments de Vercel).

**⚠️ SOLO en Development:**

#### Railway Dev Environment
```bash
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app
```

#### Railway Production Environment
```bash
# NO configurar en producción o dejar vacío
CORS_ALLOWED_ORIGIN_PATTERNS=
```

---

## 🗄️ Variables de Base de Datos (EXISTENTES)

Estas variables ya existen, solo se listan como referencia:

```bash
DATABASE_URL=jdbc:postgresql://host:5432/database?sslmode=require&prepareThreshold=0
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

---

## 🔑 Variables JWT (EXISTENTES)

```bash
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
JWT_REFERRAL_SECRET=your-referral-jwt-secret
JWT_REFERRAL_EXPIRATION=31536000000
```

---

## ☁️ Variables Cloudinary (EXISTENTES)

```bash
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
STORAGE_PROVIDER=cloudinary
```

---

## 📧 Variables Email (EXISTENTES)

```bash
RESEND_API_KEY=your-resend-api-key
RESEND_FROM_EMAIL=noreply@gydi.com
EMAIL_PROVIDER=resend
```

---

## 💳 Variables Stripe (EXISTENTES)

```bash
STRIPE_API_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_CURRENCY=USD
```

---

## 🔒 Variables URL Encryption (EXISTENTES)

```bash
URL_HASHIDS_SALT=your-hashids-salt
AES_SECRET_KEY=your-aes-secret-key-32-bytes-hex
```

---

## 🌍 Variables Frontend (EXISTENTES)

```bash
FRONTEND_URL=https://gydi-front-next.vercel.app
DEV_EMAIL_RECIPIENT=dev@gydi.local
```

---

## ✅ Checklist de Configuración

### Railway Dev Environment

- [ ] `SPRING_PROFILES_ACTIVE=dev` ⭐ **Define ambiente y SameSite policy**
- [ ] `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000,https://gydi-front-next.vercel.app`
- [ ] `CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app` (opcional)
- [ ] `CLOUDINARY_URL=cloudinary://...` (con valor correcto)
- [ ] `STORAGE_PROVIDER=cloudinary`
- [ ] `DATABASE_URL=jdbc:postgresql://...`
- [ ] `DB_USERNAME=...`
- [ ] `DB_PASSWORD=...`
- [ ] Todas las demás variables (JWT, Stripe, etc.)

### Railway Production Environment

- [ ] `SPRING_PROFILES_ACTIVE=prod` ⭐ **Define ambiente, archivo config, y SameSite=None**
- [ ] `CORS_ALLOWED_ORIGINS=https://gydi-front-next.vercel.app` ⭐ **SIN localhost**
- [ ] `CORS_ALLOWED_ORIGIN_PATTERNS=` (vacío o no configurado)
- [ ] `CLOUDINARY_URL=cloudinary://...` (con valor correcto)
- [ ] `STORAGE_PROVIDER=cloudinary`
- [ ] `DATABASE_URL=jdbc:postgresql://...`
- [ ] `DB_USERNAME=...`
- [ ] `DB_PASSWORD=...`
- [ ] Todas las demás variables (JWT, Stripe, etc.)

---

## 🔍 Verificación Post-Deploy

Después de configurar las variables y hacer deploy, verifica en los logs de Railway:

### ✅ Logs Esperados (Éxito)

```
✅ CORS configured with allowed origins: [https://gydi-front-next.vercel.app]
✅ Cloudinary configured successfully with cloud name: cloudestoragegydi
✅ No warnings "Token not found"
```

### ❌ Logs de Error (Problemas)

```
❌ SECURITY: Token not found - No authentication token found in request
   → Problema: Cookies no se están enviando (verificar CORS y SameSite)

❌ Invalid Cloudinary URL format
   → Problema: CLOUDINARY_URL mal configurado

❌ CORS configured with allowed origin patterns: [https://*.vercel.app]
   → Problema: Patterns activos en producción (solo dev)
```

---

## 🆘 Troubleshooting

### Problema: "Token not found" después de login

**Causa:** Cookies no se envían en requests subsecuentes.

**Solución:**
1. Verificar `SPRING_PROFILES_ACTIVE=prod` en Railway (esto activa SameSite=None)
2. Verificar `CORS_ALLOWED_ORIGINS` incluye el dominio exacto de Vercel
3. Verificar que frontend use `credentials: 'include'` en requests

### Problema: CSRF validation failed (403)

**Causa:** CSRF token no se está enviando o es inválido.

**Solución:**
1. Verificar que frontend llama `fetchCsrfToken()` en app load
2. Verificar que `X-XSRF-TOKEN` header se envía en POST/PUT/PATCH/DELETE
3. Verificar que cookie `XSRF-TOKEN` existe en el navegador

### Problema: CORS error en browser console

**Causa:** Origin no está en lista de permitidos.

**Solución:**
1. Verificar `CORS_ALLOWED_ORIGINS` en Railway
2. Verificar que el dominio coincide EXACTAMENTE (con/sin www, http/https)
3. NO usar `CORS_ALLOWED_ORIGIN_PATTERNS` en producción

---

## 📝 Notas Importantes

1. **NUNCA** commits variables sensibles al repositorio
2. **SIEMPRE** usa valores diferentes para dev y prod (especialmente JWT_SECRET)
3. **VERIFICA** que `SPRING_PROFILES_ACTIVE` está correcta en cada ambiente (dev vs prod)
4. **MONITOREA** logs de Railway después de cada deploy
5. **PRUEBA** login y acciones POST/PUT/DELETE después de deploy

---

**Última actualización:** Enero 2026
**Versión:** 2.0 (con CSRF protection)
