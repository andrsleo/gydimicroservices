#!/bin/bash

# ============================================
# GYDI Microservices - Development Startup Script
# ============================================
# This script loads environment variables from .env and starts Spring Boot
# Usage: ./start-dev.sh
# ============================================

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 GYDI Microservices - Starting Development Server${NC}"
echo ""

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo -e "${RED}❌ Error: .env file not found${NC}"
    echo ""
    echo "Please create a .env file with required environment variables."
    echo "You can run: ./setup-secrets-local.sh"
    echo ""
    echo "Or manually create .env with these variables:"
    echo "  - DB_PASSWORD"
    echo "  - JWT_SECRET"
    echo "  - JWT_REFERRAL_SECRET"
    echo "  - URL_HASHIDS_SALT"
    echo "  - AES_SECRET_KEY"
    exit 1
fi

echo -e "${GREEN}✓ Found .env file${NC}"

# Check if PostgreSQL is running
if ! nc -z localhost 5432 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Warning: PostgreSQL does not appear to be running on localhost:5432${NC}"
    echo "   Make sure PostgreSQL is started before continuing."
    read -p "   Press Enter to continue anyway, or Ctrl+C to cancel..."
fi

# Load environment variables
echo -e "${GREEN}✓ Loading environment variables from .env${NC}"
set -a
source .env
set +a

# Check if Maven wrapper exists
if [ ! -f "./mvnw" ]; then
    echo -e "${RED}❌ Error: Maven wrapper (mvnw) not found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven wrapper found${NC}"
echo ""
echo -e "${GREEN}🔧 Starting Spring Boot application...${NC}"
echo -e "${YELLOW}   (Press Ctrl+C to stop)${NC}"
echo ""

# Start Spring Boot
./mvnw spring-boot:run
