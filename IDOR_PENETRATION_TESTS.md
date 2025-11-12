# IDOR Penetration Testing Guide

**Purpose**: Manual penetration testing procedures to verify IDOR (Insecure Direct Object Reference) prevention in the GYDI application.

**Date**: November 2025
**Security Level**: CRITICAL
**Tested By**: Security Team

---

## Overview

This document provides step-by-step instructions for conducting penetration tests on IDOR vulnerabilities in the GYDI application. These tests verify that users cannot access or manipulate resources belonging to other users.

## Prerequisites

1. **Running Application**: Backend must be running on `http://localhost:8080`
2. **Test Users**: Create at least 2 test users (User A and User B)
3. **Tools**: `curl`, Postman, or similar HTTP client
4. **JWT Tokens**: Valid authentication tokens for both users

---

## Test Setup

### Step 1: Create Test Users

```bash
# Create User A
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "User A",
    "email": "userA@test.com",
    "password": "SecurePass123!"
  }'

# Create User B
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "User B",
    "email": "userB@test.com",
    "password": "SecurePass456!"
  }'
```

**Expected Response**: `201 Created` for both users

### Step 2: Authenticate and Get JWT Tokens

```bash
# Login as User A
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "userA@test.com",
    "password": "SecurePass123!"
  }'

# Save the JWT token from response:
TOKEN_USER_A="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Login as User B
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "userB@test.com",
    "password": "SecurePass456!"
  }'

# Save the JWT token from response:
TOKEN_USER_B="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 3: Get User IDs

```bash
# Get User A's profile to obtain their ID
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN_USER_A"

# Note down User A's ID (e.g., 1)
USER_A_ID=1

# Get User B's profile to obtain their ID
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN_USER_B"

# Note down User B's ID (e.g., 2)
USER_B_ID=2
```

---

## Penetration Tests

### Test 1: IDOR - Read Other User's Profile

**Objective**: Verify that User A cannot read User B's profile

**Vulnerability**: If successful, attackers can access sensitive personal information of other users

**Test Steps**:

```bash
# User A attempts to read User B's profile (ID=2)
curl -X GET http://localhost:8080/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -v
```

**✅ Expected Result**: `403 Forbidden`

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You do not have permission to access this resource."
}
```

**❌ Failure Condition**: If the response is `200 OK` and returns User B's profile, the IDOR vulnerability exists.

**Security Log**: Check logs for IDOR detection:
```
WARN: SECURITY: IDOR attempt detected! User userA@test.com (ID: 1) tried to access resource owned by user ID: 2
```

---

### Test 2: IDOR - Update Other User's Profile

**Objective**: Verify that User A cannot update User B's profile

**Vulnerability**: If successful, attackers can modify other users' personal information, email addresses, or profile details

**Test Steps**:

```bash
# User A attempts to update User B's profile (ID=2)
curl -X PUT http://localhost:8080/api/users/$USER_B_ID/profile \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Hacked",
    "lastName": "User",
    "phoneNumber": "666-666-6666"
  }' \
  -v
```

**✅ Expected Result**: `403 Forbidden`

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You do not have permission to access this resource."
}
```

**❌ Failure Condition**: If the response is `200 OK` and User B's profile is updated, the IDOR vulnerability exists.

**Verify**: Login as User B and check profile has not been modified.

---

### Test 3: IDOR - Delete Other User's Account

**Objective**: Verify that User A cannot delete User B's account

**Vulnerability**: CRITICAL - If successful, attackers can delete other users' accounts, causing data loss and service disruption

**Test Steps**:

```bash
# User A attempts to delete User B's account (ID=2)
curl -X DELETE http://localhost:8080/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -v
```

**✅ Expected Result**: `403 Forbidden`

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You do not have permission to access this resource."
}
```

**❌ Failure Condition**: If the response is `200 OK` or `204 No Content` and User B's account is deleted, the IDOR vulnerability exists.

**Verify**: User B should still be able to login successfully.

---

### Test 4: IDOR - Sequential ID Enumeration Attack

**Objective**: Verify that attackers cannot enumerate users by sequential ID guessing

**Vulnerability**: If successful, attackers can discover all user accounts in the system

**Test Steps**:

```bash
# User A attempts to enumerate users by trying sequential IDs
for id in {1..100}; do
  echo "Testing ID: $id"
  curl -X GET http://localhost:8080/api/users/$id \
    -H "Authorization: Bearer $TOKEN_USER_A" \
    -w "\nStatus Code: %{http_code}\n" \
    -o /dev/null -s
done
```

**✅ Expected Result**:
- `200 OK` only for User A's own ID
- `403 Forbidden` for all other IDs

**❌ Failure Condition**: If multiple `200 OK` responses are returned, the system is vulnerable to user enumeration.

---

### Test 5: IDOR - Access Other User's Profile Data

**Objective**: Verify that User A cannot access User B's profile details endpoint

**Vulnerability**: If successful, attackers can access extended profile information (bio, social links, etc.)

**Test Steps**:

```bash
# User A attempts to read User B's profile details (ID=2)
curl -X GET http://localhost:8080/api/users/$USER_B_ID/profile \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -v
```

**✅ Expected Result**: `403 Forbidden`

**❌ Failure Condition**: `200 OK` with User B's profile data

---

### Test 6: IDOR - Parameter Tampering Attack

**Objective**: Verify that changing the user ID in request body doesn't bypass authorization

**Vulnerability**: If successful, attackers can manipulate their own requests to affect other users

**Test Steps**:

```bash
# User A (ID=1) attempts to update their profile but tampers with the user_id in the request
curl -X PUT http://localhost:8080/api/users/1/profile \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "firstName": "Hacked",
    "lastName": "User"
  }' \
  -v
```

**✅ Expected Result**:
- The system should ignore the `userId` field in the request body
- Only User A's profile (ID=1) should be updated
- If the endpoint tries to update User B (ID=2), it should return `403 Forbidden`

**❌ Failure Condition**: User B's profile is modified

---

### Test 7: IDOR - Unauthenticated Access Attempt

**Objective**: Verify that endpoints reject requests without authentication tokens

**Vulnerability**: If successful, attackers can access user data without authentication

**Test Steps**:

```bash
# Attempt to access User B's profile without authentication
curl -X GET http://localhost:8080/api/users/$USER_B_ID \
  -v
```

**✅ Expected Result**: `401 Unauthorized` or `403 Forbidden`

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

**❌ Failure Condition**: `200 OK` with user data returned

---

### Test 8: IDOR - Token Substitution Attack

**Objective**: Verify that using another user's token correctly grants access only to their resources

**Vulnerability**: Verify JWT token binding to user identity

**Test Steps**:

```bash
# User A uses their own token to access their profile - should work
curl -X GET http://localhost:8080/api/users/$USER_A_ID \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -v

# User A uses User B's token to access User B's profile - should work (but this verifies token is correctly bound)
curl -X GET http://localhost:8080/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -v

# User A uses their own token to access User B's profile - should fail
curl -X GET http://localhost:8080/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -v
```

**✅ Expected Result**:
1. First request: `200 OK` (User A accessing own profile with own token)
2. Second request: `200 OK` (User B's token accessing User B's profile)
3. Third request: `403 Forbidden` (User A's token trying to access User B's profile)

**❌ Failure Condition**: Third request returns `200 OK`

---

### Test 9: IDOR - ADMIN Privilege Verification

**Objective**: Verify that ADMIN users CAN access other users' resources

**Vulnerability**: Verify proper role-based access control (RBAC)

**Test Steps**:

```bash
# Create ADMIN user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@test.com",
    "password": "AdminPass123!",
    "roles": ["ADMIN"]
  }'

# Login as ADMIN
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "password": "AdminPass123!"
  }'

# Save ADMIN token
TOKEN_ADMIN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# ADMIN accesses User A's profile - should work
curl -X GET http://localhost:8080/api/users/$USER_A_ID \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -v
```

**✅ Expected Result**: `200 OK` (ADMIN can access any user's resources)

**❌ Failure Condition**: `403 Forbidden` (ADMIN should have elevated privileges)

---

## Attack Scenarios

### Scenario 1: Mass User Data Scraping

**Attack**: Automated script attempts to scrape all user profiles

```bash
#!/bin/bash
# scrape_users.sh - Simulated attack script

TOKEN="$TOKEN_USER_A"

for id in {1..1000}; do
  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET http://localhost:8080/api/users/$id \
    -H "Authorization: Bearer $TOKEN")

  if [ "$response" == "200" ]; then
    echo "✓ Found user ID: $id"
    # Attacker would save this data
  fi
done
```

**✅ Expected Result**: Only 1 successful response (the authenticated user's own ID)

**❌ Failure Condition**: Multiple `200 OK` responses indicate IDOR vulnerability

---

### Scenario 2: Account Takeover via Profile Update

**Attack**: User A attempts to change User B's email to gain control of their account

```bash
# User A attempts to update User B's email address
curl -X PUT http://localhost:8080/api/users/$USER_B_ID/profile \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "attacker@evil.com",
    "firstName": "User",
    "lastName": "B"
  }' \
  -v
```

**✅ Expected Result**: `403 Forbidden`

**❌ Failure Condition**: `200 OK` and email is changed (attacker can now reset password and take over account)

---

### Scenario 3: Data Breach via Bulk Deletion

**Attack**: Attacker attempts to delete multiple user accounts

```bash
#!/bin/bash
# delete_users.sh - Simulated mass deletion attack

TOKEN="$TOKEN_USER_A"

for id in {1..100}; do
  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -X DELETE http://localhost:8080/api/users/$id \
    -H "Authorization: Bearer $TOKEN")

  if [ "$response" == "200" ] || [ "$response" == "204" ]; then
    echo "⚠️ Deleted user ID: $id"
  fi
done
```

**✅ Expected Result**: No users deleted (all responses are `403 Forbidden`)

**❌ Failure Condition**: Any user is successfully deleted

---

## Verification Checklist

After running all tests, verify:

- [ ] All IDOR attempts return `403 Forbidden`
- [ ] Security logs show IDOR detection warnings
- [ ] Users can only access their own resources
- [ ] ADMIN users can access all resources
- [ ] No user enumeration is possible
- [ ] JWT tokens are properly validated
- [ ] No parameter tampering bypasses authorization
- [ ] Unauthenticated requests are rejected

---

## Expected Log Output

When IDOR attempts are made, the application should log:

```
WARN: SECURITY: IDOR attempt detected! User userA@test.com (ID: 1) tried to access resource owned by user ID: 2
```

**Check logs**:
```bash
tail -f /Users/andresvargas/Documents/Project\ GYDI\ 2.0/GydiMicroservices/logs/application.log | grep "IDOR"
```

---

## Automated Testing Script

For convenience, here's a complete automated penetration test script:

```bash
#!/bin/bash
# idor_penetration_test.sh

set -e

BASE_URL="http://localhost:8080"
PASSED=0
FAILED=0

echo "=================================================="
echo "IDOR Penetration Testing - GYDI Application"
echo "=================================================="
echo ""

# Test 1: Setup
echo "[TEST 1] Creating test users..."
USER_A=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"User A","email":"userA@test.com","password":"SecurePass123!"}')

USER_B=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"User B","email":"userB@test.com","password":"SecurePass456!"}')

echo "✓ Test users created"
echo ""

# Test 2: Authentication
echo "[TEST 2] Authenticating users..."
TOKEN_A=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"userA@test.com","password":"SecurePass123!"}' | jq -r '.token')

TOKEN_B=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"userB@test.com","password":"SecurePass456!"}' | jq -r '.token')

echo "✓ Users authenticated"
echo ""

# Test 3: Get User IDs
echo "[TEST 3] Getting user IDs..."
USER_A_ID=$(curl -s -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $TOKEN_A" | jq -r '.id')

USER_B_ID=$(curl -s -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $TOKEN_B" | jq -r '.id')

echo "✓ User A ID: $USER_A_ID"
echo "✓ User B ID: $USER_B_ID"
echo ""

# Test 4: IDOR Attack - Read Profile
echo "[TEST 4] IDOR Attack: User A reads User B's profile..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X GET $BASE_URL/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_A")

if [ "$STATUS" == "403" ]; then
  echo "✅ PASS: IDOR attack blocked (403 Forbidden)"
  ((PASSED++))
else
  echo "❌ FAIL: IDOR vulnerability detected! Status: $STATUS"
  ((FAILED++))
fi
echo ""

# Test 5: IDOR Attack - Update Profile
echo "[TEST 5] IDOR Attack: User A updates User B's profile..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X PUT $BASE_URL/api/users/$USER_B_ID/profile \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Hacked","lastName":"User"}')

if [ "$STATUS" == "403" ]; then
  echo "✅ PASS: IDOR attack blocked (403 Forbidden)"
  ((PASSED++))
else
  echo "❌ FAIL: IDOR vulnerability detected! Status: $STATUS"
  ((FAILED++))
fi
echo ""

# Test 6: IDOR Attack - Delete Account
echo "[TEST 6] IDOR Attack: User A deletes User B's account..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X DELETE $BASE_URL/api/users/$USER_B_ID \
  -H "Authorization: Bearer $TOKEN_A")

if [ "$STATUS" == "403" ]; then
  echo "✅ PASS: IDOR attack blocked (403 Forbidden)"
  ((PASSED++))
else
  echo "❌ FAIL: IDOR vulnerability detected! Status: $STATUS"
  ((FAILED++))
fi
echo ""

# Test 7: Verify User B still exists
echo "[TEST 7] Verifying User B account integrity..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $TOKEN_B")

if [ "$STATUS" == "200" ]; then
  echo "✅ PASS: User B account still intact"
  ((PASSED++))
else
  echo "❌ FAIL: User B account was compromised! Status: $STATUS"
  ((FAILED++))
fi
echo ""

# Summary
echo "=================================================="
echo "Test Results:"
echo "✅ PASSED: $PASSED"
echo "❌ FAILED: $FAILED"
echo "=================================================="

if [ $FAILED -eq 0 ]; then
  echo "🎉 All IDOR penetration tests passed!"
  exit 0
else
  echo "⚠️ IDOR vulnerabilities detected! Review failed tests."
  exit 1
fi
```

**Run the script**:
```bash
chmod +x idor_penetration_test.sh
./idor_penetration_test.sh
```

---

## Remediation

If any test fails, the following fixes should be implemented:

1. **Add OwnershipValidator** to all user-resource endpoints
2. **Verify user ID** from JWT token matches resource owner ID
3. **Implement RBAC** for ADMIN users
4. **Log all IDOR attempts** for security monitoring
5. **Use UUIDs** instead of sequential IDs to prevent enumeration
6. **Rate limiting** on user endpoints to prevent mass scraping

**Reference Implementation**: See `OwnershipValidator.java` in `shared/security/`

---

## Security Compliance

These penetration tests verify compliance with:

- **OWASP Top 10 2021**: A01:2021 – Broken Access Control
- **CWE-639**: Authorization Bypass Through User-Controlled Key
- **NIST 800-53**: AC-3 (Access Enforcement)
- **PCI DSS**: Requirement 6.5.8 (Improper Access Control)

---

## Notes

- Run these tests in a **development/staging environment** only
- Never run penetration tests on production without authorization
- Document all findings and remediation steps
- Re-test after implementing fixes

---

**Last Updated**: November 2025
**Next Review**: After any authorization code changes