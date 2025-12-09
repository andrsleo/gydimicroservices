#!/bin/bash

# ==============================================================================
# 🚀 Fly.io Production Secrets Setup Script
# ==============================================================================
# This script generates and configures strong, random secrets for Fly.io deployment
#
# Prerequisites:
#   • Fly.io CLI installed: https://fly.io/docs/hands-on/install-flyctl/
#   • Logged in: fly auth login
#   • App created: fly launch --name gydi-backend
#   • Postgres attached: fly postgres attach gydi-postgres
#
# Usage:
#   chmod +x setup-secrets-flyio.sh
#   ./setup-secrets-flyio.sh
#
# ==============================================================================

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# ==============================================================================
# Pre-flight Checks
# ==============================================================================

echo "🚀 Fly.io Production Secrets Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if fly CLI is installed
if ! command -v fly &> /dev/null; then
    echo -e "${RED}❌ Fly.io CLI not found${NC}"
    echo "Install from: https://fly.io/docs/hands-on/install-flyctl/"
    exit 1
fi

echo -e "${GREEN}✓ Fly.io CLI installed${NC}"

# Check if logged in
if ! fly auth whoami &> /dev/null; then
    echo -e "${RED}❌ Not logged in to Fly.io${NC}"
    echo "Run: fly auth login"
    exit 1
fi

echo -e "${GREEN}✓ Logged in to Fly.io${NC}"
echo ""

# ==============================================================================
# Configuration
# ==============================================================================

echo -e "${YELLOW}Enter your Fly.io app name [default: gydi-backend]:${NC}"
read -r APP_NAME
APP_NAME="${APP_NAME:-gydi-backend}"

# Verify app exists
if ! fly apps list | grep -q "$APP_NAME"; then
    echo -e "${RED}❌ App '$APP_NAME' not found${NC}"
    echo "Create it first: fly launch --name $APP_NAME"
    exit 1
fi

echo -e "${GREEN}✓ App '$APP_NAME' found${NC}"
echo ""

# ==============================================================================
# Generate Secrets
# ==============================================================================

echo "📝 Generating strong random secrets..."
echo ""

# Database credentials
echo -e "${YELLOW}Enter database username [default: gydi_admin]:${NC}"
read -r DB_USERNAME
DB_USERNAME="${DB_USERNAME:-gydi_admin}"

DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n' | tr -d '/')

# JWT Secrets (64 bytes base64)
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
JWT_REFERRAL_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# AES Secret (32 bytes hex = 256 bits)
AES_SECRET_KEY=$(openssl rand -hex 32)

# Hashids Salt
URL_HASHIDS_SALT=$(openssl rand -base64 32 | tr -d '\n')

# Frontend URL
echo -e "${YELLOW}Enter frontend URL [default: https://gydi.vercel.app]:${NC}"
read -r FRONTEND_URL
FRONTEND_URL="${FRONTEND_URL:-https://gydi.vercel.app}"

# Cloudinary URL
echo ""
echo -e "${YELLOW}Enter Cloudinary URL:${NC}"
echo -e "${YELLOW}Format: cloudinary://api_key:api_secret@cloud_name${NC}"
echo -e "${YELLOW}Get from: https://cloudinary.com/console${NC}"
read -r CLOUDINARY_URL

if [ -z "$CLOUDINARY_URL" ]; then
    echo -e "${RED}❌ Cloudinary URL is required for production${NC}"
    exit 1
fi

# Resend API Key
echo ""
echo -e "${YELLOW}Enter Resend API key:${NC}"
echo -e "${YELLOW}Get from: https://resend.com/api-keys${NC}"
read -r RESEND_API_KEY

if [ -z "$RESEND_API_KEY" ]; then
    echo -e "${RED}❌ Resend API key is required for production${NC}"
    exit 1
fi

# Resend From Email
echo ""
echo -e "${YELLOW}Enter Resend verified email [default: noreply@gydi.com]:${NC}"
read -r RESEND_FROM_EMAIL
RESEND_FROM_EMAIL="${RESEND_FROM_EMAIL:-noreply@gydi.com}"

# Optional: Sentry DSN
echo ""
echo -e "${YELLOW}Enter Sentry DSN (optional, press Enter to skip):${NC}"
echo -e "${YELLOW}Get from: https://sentry.io/settings/projects/${NC}"
read -r SENTRY_DSN

echo ""
echo -e "${GREEN}✅ All secrets generated successfully!${NC}"
echo ""

# ==============================================================================
# Confirm Before Setting Secrets
# ==============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "⚠️  IMPORTANT: Review Configuration"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "App Name:          $APP_NAME"
echo "DB Username:       $DB_USERNAME"
echo "DB Password:       [hidden]"
echo "Frontend URL:      $FRONTEND_URL"
echo "Cloudinary:        [configured]"
echo "Resend Email:      $RESEND_FROM_EMAIL"
echo "Sentry:            ${SENTRY_DSN:-(not configured)}"
echo ""
echo -e "${YELLOW}This will set secrets in Fly.io app: $APP_NAME${NC}"
echo -e "${YELLOW}Existing secrets with the same name will be overwritten.${NC}"
echo ""
echo -e "${YELLOW}Continue? (yes/no):${NC}"
read -r CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ Aborted"
    exit 0
fi

# ==============================================================================
# Set Secrets in Fly.io
# ==============================================================================

echo ""
echo "🔐 Setting secrets in Fly.io..."
echo ""

# Build secrets command
SECRETS_CMD="fly secrets set \
  DB_USERNAME=\"$DB_USERNAME\" \
  DB_PASSWORD=\"$DB_PASSWORD\" \
  JWT_SECRET=\"$JWT_SECRET\" \
  JWT_REFERRAL_SECRET=\"$JWT_REFERRAL_SECRET\" \
  AES_SECRET_KEY=\"$AES_SECRET_KEY\" \
  URL_HASHIDS_SALT=\"$URL_HASHIDS_SALT\" \
  FRONTEND_URL=\"$FRONTEND_URL\" \
  CLOUDINARY_URL=\"$CLOUDINARY_URL\" \
  RESEND_API_KEY=\"$RESEND_API_KEY\" \
  RESEND_FROM_EMAIL=\"$RESEND_FROM_EMAIL\""

# Add optional secrets
if [ -n "$SENTRY_DSN" ]; then
    SECRETS_CMD="$SECRETS_CMD SENTRY_DSN=\"$SENTRY_DSN\""
fi

# Add app flag
SECRETS_CMD="$SECRETS_CMD --app $APP_NAME"

# Execute
eval $SECRETS_CMD

echo ""
echo -e "${GREEN}✅ Secrets configured successfully!${NC}"
echo ""

# ==============================================================================
# Save Secrets to Local File (Backup)
# ==============================================================================

BACKUP_FILE=".secrets-backup-$(date +%Y%m%d-%H%M%S).txt"

echo "💾 Saving secrets backup to: $BACKUP_FILE"
echo ""

cat > "$BACKUP_FILE" << EOF
# ==============================================================================
# GYDI Backend - Production Secrets Backup
# ==============================================================================
# Generated: $(date)
# App: $APP_NAME
#
# ⚠️  SECURITY WARNING:
# • Store this file in a secure password manager (1Password, Bitwarden, etc.)
# • DO NOT commit to git
# • Delete after storing securely
# ==============================================================================

DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD

JWT_SECRET=$JWT_SECRET
JWT_REFERRAL_SECRET=$JWT_REFERRAL_SECRET

AES_SECRET_KEY=$AES_SECRET_KEY
URL_HASHIDS_SALT=$URL_HASHIDS_SALT

FRONTEND_URL=$FRONTEND_URL

CLOUDINARY_URL=$CLOUDINARY_URL

RESEND_API_KEY=$RESEND_API_KEY
RESEND_FROM_EMAIL=$RESEND_FROM_EMAIL

EOF

if [ -n "$SENTRY_DSN" ]; then
    echo "SENTRY_DSN=$SENTRY_DSN" >> "$BACKUP_FILE"
fi

echo -e "${GREEN}✅ Backup saved to: $BACKUP_FILE${NC}"
echo ""

# ==============================================================================
# Display Next Steps
# ==============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📝 Next Steps:"
echo ""
echo "1. Verify secrets are set:"
echo -e "   ${GREEN}fly secrets list --app $APP_NAME${NC}"
echo ""
echo "2. Deploy your application:"
echo -e "   ${GREEN}fly deploy --app $APP_NAME${NC}"
echo ""
echo "3. Monitor deployment:"
echo -e "   ${GREEN}fly logs --app $APP_NAME${NC}"
echo ""
echo "4. Verify health:"
echo -e "   ${GREEN}fly status --app $APP_NAME${NC}"
echo -e "   ${GREEN}curl https://$APP_NAME.fly.dev/actuator/health${NC}"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔒 Security Reminder:"
echo "   • Store $BACKUP_FILE in a secure password manager"
echo "   • Delete the backup file after storing: rm $BACKUP_FILE"
echo "   • Never share secrets via email or Slack"
echo "   • Rotate secrets every 90 days"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
