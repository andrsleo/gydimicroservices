#!/bin/bash

# ==============================================================================
# 🔐 Local Development Secrets Setup Script
# ==============================================================================
# This script generates strong, random secrets for local development
#
# Usage:
#   chmod +x setup-secrets-local.sh
#   ./setup-secrets-local.sh
#
# Output: .env file with all required secrets
# ==============================================================================

set -e  # Exit on error

echo "🔐 Generating secrets for local development..."
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==============================================================================
# Generate Secrets
# ==============================================================================

echo "📝 Generating strong random secrets..."

# Database credentials (use your local PostgreSQL credentials)
DB_USERNAME="${DB_USERNAME:-postgres}"
echo -e "${YELLOW}Enter your local PostgreSQL username [default: postgres]:${NC}"
read -r input_username
if [ -n "$input_username" ]; then
    DB_USERNAME="$input_username"
fi

echo -e "${YELLOW}Enter your local PostgreSQL password:${NC}"
read -rs DB_PASSWORD
echo ""

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Database password is required"
    exit 1
fi

# JWT Secrets (64 bytes base64)
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
JWT_REFERRAL_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# AES Secret (32 bytes hex = 256 bits)
AES_SECRET_KEY=$(openssl rand -hex 32)

# Hashids Salt
URL_HASHIDS_SALT=$(openssl rand -base64 32 | tr -d '\n')

# Optional: Cloudinary URL (for local testing)
echo -e "${YELLOW}Enter Cloudinary URL (optional, press Enter to skip):${NC}"
echo -e "${YELLOW}Format: cloudinary://api_key:api_secret@cloud_name${NC}"
read -r CLOUDINARY_URL

# Optional: Resend API Key (for local testing)
echo -e "${YELLOW}Enter Resend API key (optional, press Enter to skip):${NC}"
read -r RESEND_API_KEY

echo ""
echo "✅ All secrets generated successfully!"
echo ""

# ==============================================================================
# Create .env File
# ==============================================================================

ENV_FILE=".env"

echo "📄 Creating $ENV_FILE file..."

cat > "$ENV_FILE" << EOF
# ==============================================================================
# GYDI Backend - Local Development Environment Variables
# ==============================================================================
# Generated: $(date)
# DO NOT commit this file to git (.gitignore should already exclude it)
# ==============================================================================

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/gydidb
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD

# JWT Secrets
JWT_SECRET=$JWT_SECRET
JWT_REFERRAL_SECRET=$JWT_REFERRAL_SECRET

# Encryption Keys
AES_SECRET_KEY=$AES_SECRET_KEY
URL_HASHIDS_SALT=$URL_HASHIDS_SALT

# Frontend URL
FRONTEND_URL=http://localhost:3000

# Storage Provider (local or cloudinary)
STORAGE_PROVIDER=local

# Email Provider (local or resend)
EMAIL_PROVIDER=local

EOF

# Add optional services if provided
if [ -n "$CLOUDINARY_URL" ]; then
    echo "# Cloudinary (optional)" >> "$ENV_FILE"
    echo "CLOUDINARY_URL=$CLOUDINARY_URL" >> "$ENV_FILE"
    echo "" >> "$ENV_FILE"
fi

if [ -n "$RESEND_API_KEY" ]; then
    echo "# Resend (optional)" >> "$ENV_FILE"
    echo "RESEND_API_KEY=$RESEND_API_KEY" >> "$ENV_FILE"
    echo "RESEND_FROM_EMAIL=noreply@gydi.com" >> "$ENV_FILE"
    echo "" >> "$ENV_FILE"
fi

echo "✅ $ENV_FILE created successfully!"
echo ""

# ==============================================================================
# Display Instructions
# ==============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📝 Next Steps:"
echo ""
echo "1. Load environment variables:"
echo -e "   ${GREEN}export \$(cat .env | xargs)${NC}"
echo ""
echo "   Or add to your shell profile (~/.bashrc, ~/.zshrc):"
echo -e "   ${GREEN}source /path/to/GydiMicroservices/.env${NC}"
echo ""
echo "2. Run the application:"
echo -e "   ${GREEN}./mvnw spring-boot:run${NC}"
echo ""
echo "3. Verify secrets are loaded:"
echo "   Check logs for 'HikariPool-1 - Start completed' (DB connection)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔒 Security Notes:"
echo "   • .env file is excluded from git (.gitignore)"
echo "   • Never commit secrets to version control"
echo "   • Rotate secrets periodically"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
