#!/bin/bash
#
# IDOR Penetration Testing Script
#
# Purpose: Automated security testing for IDOR vulnerabilities
# Usage: ./idor_penetration_test.sh
#
# SECURITY: Run only in development/staging environments!
#

set -e

# Configuration
BASE_URL="${API_BASE_URL:-http://localhost:8080}"
PASSED=0
FAILED=0
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
LOG_FILE="idor_test_results_${TIMESTAMP}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
  echo "" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"
  echo -e "${BLUE}$1${NC}" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"
}

print_test() {
  echo -e "${YELLOW}[TEST $1]${NC} $2" | tee -a "$LOG_FILE"
}

print_pass() {
  echo -e "${GREEN}✅ PASS:${NC} $1" | tee -a "$LOG_FILE"
  ((PASSED++))
}

print_fail() {
  echo -e "${RED}❌ FAIL:${NC} $1" | tee -a "$LOG_FILE"
  ((FAILED++))
}

print_info() {
  echo -e "ℹ️  $1" | tee -a "$LOG_FILE"
}

# Check prerequisites
check_prerequisites() {
  print_header "Checking Prerequisites"

  # Check if jq is installed
  if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed${NC}"
    echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
  fi
  print_info "jq found: $(which jq)"

  # Check if backend is running
  if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo -e "${RED}Error: Backend is not running at $BASE_URL${NC}"
    echo "Start the backend with: ./mvnw spring-boot:run"
    exit 1
  fi
  print_info "Backend is running at $BASE_URL"

  echo "" | tee -a "$LOG_FILE"
}

# Test setup
setup_test_users() {
  print_header "Test Setup: Creating Users"

  # Generate random emails to avoid conflicts
  RANDOM_ID=$(date +%s)
  EMAIL_A="userA_${RANDOM_ID}@test.com"
  EMAIL_B="userB_${RANDOM_ID}@test.com"
  EMAIL_ADMIN="admin_${RANDOM_ID}@test.com"

  print_test "1" "Creating User A..."
  RESPONSE_A=$(curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"User A\",\"email\":\"$EMAIL_A\",\"password\":\"SecurePass123!\"}")

  if echo "$RESPONSE_A" | jq -e '.token' > /dev/null 2>&1; then
    print_info "User A created: $EMAIL_A"
  else
    print_fail "Failed to create User A"
    echo "Response: $RESPONSE_A" | tee -a "$LOG_FILE"
    exit 1
  fi

  print_test "2" "Creating User B..."
  RESPONSE_B=$(curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"User B\",\"email\":\"$EMAIL_B\",\"password\":\"SecurePass456!\"}")

  if echo "$RESPONSE_B" | jq -e '.token' > /dev/null 2>&1; then
    print_info "User B created: $EMAIL_B"
  else
    print_fail "Failed to create User B"
    echo "Response: $RESPONSE_B" | tee -a "$LOG_FILE"
    exit 1
  fi

  echo "" | tee -a "$LOG_FILE"
}

# Authenticate users
authenticate_users() {
  print_header "Authentication"

  print_test "3" "Authenticating User A..."
  LOGIN_A=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL_A\",\"password\":\"SecurePass123!\"}")

  TOKEN_A=$(echo "$LOGIN_A" | jq -r '.token')
  if [ "$TOKEN_A" == "null" ] || [ -z "$TOKEN_A" ]; then
    print_fail "Failed to authenticate User A"
    echo "Response: $LOGIN_A" | tee -a "$LOG_FILE"
    exit 1
  fi
  print_info "User A authenticated (token: ${TOKEN_A:0:20}...)"

  print_test "4" "Authenticating User B..."
  LOGIN_B=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL_B\",\"password\":\"SecurePass456!\"}")

  TOKEN_B=$(echo "$LOGIN_B" | jq -r '.token')
  if [ "$TOKEN_B" == "null" ] || [ -z "$TOKEN_B" ]; then
    print_fail "Failed to authenticate User B"
    echo "Response: $LOGIN_B" | tee -a "$LOG_FILE"
    exit 1
  fi
  print_info "User B authenticated (token: ${TOKEN_B:0:20}...)"

  echo "" | tee -a "$LOG_FILE"
}

# Get user IDs
get_user_ids() {
  print_header "Getting User IDs"

  print_test "5" "Getting User A's ID..."
  PROFILE_A=$(curl -s -X GET "$BASE_URL/api/users/me" \
    -H "Authorization: Bearer $TOKEN_A")

  USER_A_ID=$(echo "$PROFILE_A" | jq -r '.id')
  if [ "$USER_A_ID" == "null" ] || [ -z "$USER_A_ID" ]; then
    print_fail "Failed to get User A's ID"
    echo "Response: $PROFILE_A" | tee -a "$LOG_FILE"
    exit 1
  fi
  print_info "User A ID: $USER_A_ID"

  print_test "6" "Getting User B's ID..."
  PROFILE_B=$(curl -s -X GET "$BASE_URL/api/users/me" \
    -H "Authorization: Bearer $TOKEN_B")

  USER_B_ID=$(echo "$PROFILE_B" | jq -r '.id')
  if [ "$USER_B_ID" == "null" ] || [ -z "$USER_B_ID" ]; then
    print_fail "Failed to get User B's ID"
    echo "Response: $PROFILE_B" | tee -a "$LOG_FILE"
    exit 1
  fi
  print_info "User B ID: $USER_B_ID"

  echo "" | tee -a "$LOG_FILE"
}

# Run IDOR penetration tests
run_idor_tests() {
  print_header "IDOR Penetration Tests"

  # Test 7: Read other user's profile
  print_test "7" "IDOR Attack: User A attempts to read User B's profile..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/users/$USER_B_ID" \
    -H "Authorization: Bearer $TOKEN_A")

  if [ "$STATUS" == "403" ]; then
    print_pass "IDOR attack blocked (HTTP $STATUS)"
  else
    print_fail "IDOR vulnerability! User A accessed User B's profile (HTTP $STATUS)"
  fi

  # Test 8: Update other user's profile
  print_test "8" "IDOR Attack: User A attempts to update User B's profile..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PUT "$BASE_URL/api/users/$USER_B_ID/profile" \
    -H "Authorization: Bearer $TOKEN_A" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Hacked","lastName":"User","bio":"Pwned!"}')

  if [ "$STATUS" == "403" ]; then
    print_pass "IDOR attack blocked (HTTP $STATUS)"
  else
    print_fail "IDOR vulnerability! User A modified User B's profile (HTTP $STATUS)"
  fi

  # Test 9: Delete other user's account
  print_test "9" "IDOR Attack: User A attempts to delete User B's account..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X DELETE "$BASE_URL/api/users/$USER_B_ID" \
    -H "Authorization: Bearer $TOKEN_A")

  if [ "$STATUS" == "403" ]; then
    print_pass "IDOR attack blocked (HTTP $STATUS)"
  else
    print_fail "CRITICAL! User A deleted User B's account (HTTP $STATUS)"
  fi

  # Test 10: Verify User B still exists
  print_test "10" "Verifying User B account integrity..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/users/me" \
    -H "Authorization: Bearer $TOKEN_B")

  if [ "$STATUS" == "200" ]; then
    print_pass "User B account is intact (HTTP $STATUS)"
  else
    print_fail "User B account was compromised! (HTTP $STATUS)"
  fi

  # Test 11: Access other user's profile details
  print_test "11" "IDOR Attack: User A attempts to read User B's profile details..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/users/$USER_B_ID/profile" \
    -H "Authorization: Bearer $TOKEN_A")

  if [ "$STATUS" == "403" ]; then
    print_pass "IDOR attack blocked (HTTP $STATUS)"
  else
    print_fail "IDOR vulnerability! User A accessed User B's profile details (HTTP $STATUS)"
  fi

  # Test 12: Unauthenticated access
  print_test "12" "IDOR Attack: Unauthenticated access to User B's profile..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/users/$USER_B_ID")

  if [ "$STATUS" == "401" ] || [ "$STATUS" == "403" ]; then
    print_pass "Unauthenticated access blocked (HTTP $STATUS)"
  else
    print_fail "Authentication bypass! Unauthenticated access succeeded (HTTP $STATUS)"
  fi

  # Test 13: User enumeration attack
  print_test "13" "IDOR Attack: User enumeration via sequential ID guessing..."
  FOUND_COUNT=0
  for id in $(seq $((USER_A_ID - 5)) $((USER_A_ID + 5))); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
      -X GET "$BASE_URL/api/users/$id" \
      -H "Authorization: Bearer $TOKEN_A")

    if [ "$STATUS" == "200" ]; then
      ((FOUND_COUNT++))
    fi
  done

  if [ "$FOUND_COUNT" -le 1 ]; then
    print_pass "User enumeration prevented (only $FOUND_COUNT user accessible)"
  else
    print_fail "User enumeration possible! Found $FOUND_COUNT accessible users"
  fi

  echo "" | tee -a "$LOG_FILE"
}

# Test rate limiting on authentication endpoint
test_rate_limiting() {
  print_header "Rate Limiting Tests"

  print_test "14" "Brute Force Attack: Testing rate limiting on login endpoint..."
  BLOCKED_COUNT=0

  for i in {1..10}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"attacker@evil.com\",\"password\":\"WrongPass$i\"}")

    if [ "$STATUS" == "429" ]; then
      ((BLOCKED_COUNT++))
    fi
  done

  if [ "$BLOCKED_COUNT" -ge 5 ]; then
    print_pass "Rate limiting working (blocked $BLOCKED_COUNT/10 attempts)"
  elif [ "$BLOCKED_COUNT" -gt 0 ]; then
    print_info "⚠️ Partial rate limiting ($BLOCKED_COUNT/10 blocked)"
  else
    print_fail "No rate limiting detected! All attempts succeeded"
  fi

  echo "" | tee -a "$LOG_FILE"
}

# Generate test report
generate_report() {
  print_header "Test Results Summary"

  TOTAL=$((PASSED + FAILED))
  SUCCESS_RATE=$(awk "BEGIN {printf \"%.1f\", ($PASSED/$TOTAL)*100}")

  echo "Test Date: $(date)" | tee -a "$LOG_FILE"
  echo "Base URL: $BASE_URL" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"
  echo -e "${GREEN}Passed:${NC} $PASSED" | tee -a "$LOG_FILE"
  echo -e "${RED}Failed:${NC} $FAILED" | tee -a "$LOG_FILE"
  echo "Total: $TOTAL" | tee -a "$LOG_FILE"
  echo "Success Rate: ${SUCCESS_RATE}%" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All IDOR penetration tests passed!${NC}" | tee -a "$LOG_FILE"
    echo -e "${GREEN}✅ Application is secure against IDOR attacks${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=0
  else
    echo -e "${RED}⚠️ SECURITY VULNERABILITIES DETECTED!${NC}" | tee -a "$LOG_FILE"
    echo -e "${RED}❌ Review failed tests and implement fixes immediately${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=1
  fi

  echo "" | tee -a "$LOG_FILE"
  echo "Full test log saved to: $LOG_FILE" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"

  return $EXIT_CODE
}

# Main execution
main() {
  clear
  print_header "IDOR Penetration Testing Suite"
  echo "Security Testing for GYDI Application" | tee -a "$LOG_FILE"
  echo "Target: $BASE_URL" | tee -a "$LOG_FILE"
  echo "Date: $(date)" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  check_prerequisites
  setup_test_users
  authenticate_users
  get_user_ids
  run_idor_tests
  test_rate_limiting
  generate_report

  exit $?
}

# Run the tests
main