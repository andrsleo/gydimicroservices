# Environment Setup Guide

## Quick Start (Development)

```bash
# Option 1: Use the helper script (recommended)
./start-dev.sh

# Option 2: Manual start
set -a && source .env && set +a && ./mvnw spring-boot:run
```

## Problem Solved

**Error**: `Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"`

**Cause**: After removing hardcoded secrets from `application.yml`, the application requires environment variables to be set.

**Solution**: Created a `.env` file with all required environment variables for local development.

## Required Environment Variables

The following environment variables are **required** for the application to start:

| Variable | Description | Example (Dev Only) |
|----------|-------------|-------------------|
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `JWT_SECRET` | Secret for JWT tokens | `dev-secret-key-change-in-production...` |
| `JWT_REFERRAL_SECRET` | Secret for referral tokens | `dev-referral-secret-change-in-production...` |
| `URL_HASHIDS_SALT` | Salt for Hashids | `dev-hashids-salt-for-local-development` |
| `AES_SECRET_KEY` | AES-256 key (64 hex chars) | `0123456789abcdef...` (64 chars) |

## .env File

A `.env` file has been created in the project root with **development-only** values. This file:

- ✅ Contains weak secrets suitable for local development only
- ✅ Is ignored by git (listed in `.gitignore`)
- ❌ Should **NEVER** be used in production
- ❌ Should **NEVER** be committed to git

### Example `.env` structure:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/gydidb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=dev-secret-key-change-in-production...
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Referral JWT
JWT_REFERRAL_SECRET=dev-referral-secret-change-in-production...
JWT_REFERRAL_EXPIRATION=2592000000

# URL Security
URL_HASHIDS_SALT=dev-hashids-salt-for-local-development
AES_SECRET_KEY=0123456789abcdef... (exactly 64 hex characters)

# Frontend
FRONTEND_URL=http://localhost:3000

# Storage & Email (local for dev)
STORAGE_PROVIDER=local
EMAIL_PROVIDER=local
```

## Setup Scripts

Two scripts are available for environment configuration:

### 1. `start-dev.sh` (Development Startup)

**Purpose**: Quick start with environment variables loaded

**Usage**:
```bash
./start-dev.sh
```

**What it does**:
- Checks if `.env` exists
- Loads environment variables
- Checks if PostgreSQL is running
- Starts Spring Boot

### 2. `setup-secrets-local.sh` (Interactive Setup)

**Purpose**: Interactive generation of strong secrets for local development

**Usage**:
```bash
./setup-secrets-local.sh
```

**What it does**:
- Prompts for database credentials
- Generates strong random secrets
- Creates `.env` file
- Provides instructions

### 3. `setup-secrets-flyio.sh` (Production Setup)

**Purpose**: Configure secrets in Fly.io for production deployment

**Usage**:
```bash
./setup-secrets-flyio.sh
```

**What it does**:
- Checks Fly CLI installation
- Generates production-grade secrets
- Sets Fly Secrets via CLI
- Creates backup file

## Development Workflow

### First Time Setup

1. **Clone repository**:
   ```bash
   git clone <repo-url>
   cd GydiMicroservices
   ```

2. **Start PostgreSQL**:
   ```bash
   # macOS (Homebrew)
   brew services start postgresql@16

   # Linux (systemd)
   sudo systemctl start postgresql

   # Docker
   docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16
   ```

3. **Create database**:
   ```bash
   psql -U postgres -c "CREATE DATABASE gydidb;"
   ```

4. **Setup environment** (choose one):

   **Option A: Use existing .env** (already created):
   ```bash
   # .env already exists with dev values
   # Just start the server
   ./start-dev.sh
   ```

   **Option B: Generate new secrets**:
   ```bash
   # Run interactive setup
   ./setup-secrets-local.sh

   # Then start server
   ./start-dev.sh
   ```

5. **Verify server is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   # Expected: {"status":"UP"}
   ```

### Daily Development

```bash
# Start server (loads .env automatically)
./start-dev.sh

# Or manually
set -a && source .env && set +a && ./mvnw spring-boot:run
```

## Production Deployment

**IMPORTANT**: Never use the development `.env` file in production!

### For Fly.io Deployment:

1. **Generate production secrets**:
   ```bash
   ./setup-secrets-flyio.sh
   ```

2. **Deploy**:
   ```bash
   fly deploy
   ```

### For Other Platforms:

1. Set environment variables in your platform's configuration:
   - **Heroku**: `heroku config:set JWT_SECRET=...`
   - **AWS**: Use AWS Secrets Manager or Parameter Store
   - **Docker**: Use `.env` file or `docker run -e`
   - **Kubernetes**: Use Secrets or ConfigMaps

2. Generate strong secrets:
   ```bash
   # JWT Secret (base64, 64 bytes)
   openssl rand -base64 64

   # AES Key (hex, 32 bytes = 64 characters)
   openssl rand -hex 32

   # Hashids Salt (base64, 32 bytes)
   openssl rand -base64 32
   ```

## Troubleshooting

### Error: "Could not resolve placeholder"

**Symptom**: Application fails to start with message about unresolved placeholders

**Solution**:
1. Verify `.env` file exists: `ls -la .env`
2. Check all required variables are set: `cat .env`
3. Load variables before starting: `set -a && source .env && set +a`
4. Or use the helper script: `./start-dev.sh`

### Error: "Unable to acquire JDBC Connection"

**Symptom**: PostgreSQL connection fails

**Solution**:
1. Check PostgreSQL is running: `pg_isready`
2. Verify database exists: `psql -U postgres -l | grep gydidb`
3. Check credentials in `.env` match PostgreSQL
4. Verify port 5432 is open: `nc -zv localhost 5432`

### Warning: "AES key must be exactly 32 bytes"

**Symptom**: Warning about AES key length

**Solution**:
- This is **not critical** for development (key is auto-adjusted)
- To fix: Ensure `AES_SECRET_KEY` is exactly 64 hex characters
- Generate correct key: `openssl rand -hex 32`

### Error: ".env file not found"

**Symptom**: `start-dev.sh` reports missing `.env`

**Solution**:
1. Run interactive setup: `./setup-secrets-local.sh`
2. Or manually create `.env` with required variables (see example above)

## Security Notes

### Development (.env file)

- ✅ Weak secrets are acceptable for local development
- ✅ File is ignored by git (`.gitignore`)
- ❌ Never commit actual `.env` to repository
- ❌ Never use these secrets in production

### Production (Fly.io / Cloud)

- ✅ Use platform-provided secret management
- ✅ Generate strong, unique secrets per environment
- ✅ Rotate secrets regularly (quarterly)
- ❌ Never hardcode secrets in code or config files

### Secret Strength Guidelines

**Development** (Local):
- JWT Secret: Any string (e.g., "dev-secret-key")
- AES Key: Any 64 hex characters
- Passwords: "postgres" is fine

**Production**:
- JWT Secret: Minimum 256 bits of entropy (`openssl rand -base64 64`)
- AES Key: Exactly 32 bytes (256 bits) as hex (`openssl rand -hex 32`)
- Passwords: Strong, unique, managed by platform

## Related Documentation

- **Security Fixes**: `docs/security/SECURITY_FIXES_VERCEL_FLYIO.md`
- **Deployment Plan**: `docs/setup/DEPLOYMENT_PLAN_VERCEL_FLYIO.md`
- **Password Validation**: `docs/security/STRONG_PASSWORD_VALIDATION.md`

---

**Last Updated**: 2025-12-04
**Version**: 1.0
**Status**: ✅ Working
