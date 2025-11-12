# IDOR Prevention Implementation Guide

## Overview

**IDOR (Insecure Direct Object Reference)** is a security vulnerability where a user can access resources belonging to other users by simply changing IDs in URLs or request parameters.

This document describes the IDOR prevention measures implemented in GYDI Microservices.

---

## What is IDOR?

### Example Vulnerability:

```
User A (ID=1) is authenticated
GET /api/users/1/profile   ← ✅ User A's own profile (allowed)
GET /api/users/2/profile   ← ❌ User B's profile (SHOULD BE BLOCKED!)
```

Without IDOR protection, User A could view, modify, or delete User B's data simply by changing the ID in the URL.

---

## Implemented Solution

We implemented a comprehensive IDOR prevention system consisting of:

1. **OwnershipValidator** - Service that validates resource ownership
2. **ForbiddenException** - Exception thrown when access is denied
3. **GlobalExceptionHandler** - Handles ForbiddenException with HTTP 403
4. **Use Case Integration** - All sensitive endpoints validate ownership

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  1. Controller receives request                      │
│     GET /api/users/profiles/user/2                   │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  2. Use Case validates ownership                     │
│     ownershipValidator.validateOwnership(userId=2)   │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  3. OwnershipValidator checks:                       │
│     - Is user authenticated?                         │
│     - Get authenticated user's ID from context       │
│     - Does authenticated ID match requested ID?      │
│     - Or is user ADMIN?                              │
└──────────────────┬──────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌───────────────┐    ┌─────────────────┐
│  Ownership    │    │  No Ownership   │
│  Validated ✅ │    │  Denied ❌      │
│               │    │                 │
│  Continue     │    │  Throw          │
│  Processing   │    │  ForbiddenExc   │
└───────────────┘    └────────┬────────┘
                              │
                              ▼
                     ┌─────────────────────┐
                     │  GlobalException    │
                     │  Handler catches    │
                     │  Returns HTTP 403   │
                     └─────────────────────┘
```

---

## Components

### 1. OwnershipValidator

Location: `src/main/java/com/affiliate/rentals/gydi/shared/security/OwnershipValidator.java`

**Purpose**: Validates that authenticated users can only access their own resources.

**Key Methods**:

```java
// Validates ownership - throws ForbiddenException if denied
void validateOwnership(Long resourceOwnerId);

// Gets authenticated user's ID
Long getAuthenticatedUserId();

// Checks if user is ADMIN
boolean isAdmin();

// Checks if user can access resource (doesn't throw)
boolean canAccess(Long resourceOwnerId);
```

**Logic**:
1. Get authenticated user from SecurityContext
2. If ADMIN → allow access
3. Get authenticated user's ID from database
4. Compare with resourceOwnerId
5. If match → allow, else → throw ForbiddenException

### 2. ForbiddenException

Location: `src/main/java/com/affiliate/rentals/gydi/shared/exception/ForbiddenException.java`

**Purpose**: Custom exception for access denied scenarios (IDOR attempts).

**HTTP Status**: 403 Forbidden

**Example**:
```java
throw new ForbiddenException(
    "Access denied. You do not have permission to access this resource."
);
```

### 3. GlobalExceptionHandler

Location: `src/main/java/com/affiliate/rentals/gydi/shared/exception/GlobalExceptionHandler.java`

**Added Handler**:
```java
@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<ErrorResponse> handleForbiddenException(
        ForbiddenException ex,
        HttpServletRequest request
) {
    logger.warn("SECURITY: Forbidden access attempt - {}", ex.getMessage());

    return buildResponse(
            HttpStatus.FORBIDDEN,
            "Access Denied",
            ex.getMessage(),
            request.getRequestURI()
    );
}
```

**Response Example**:
```json
{
  "timestamp": "2025-11-11T16:20:00Z",
  "status": 403,
  "error": "Access Denied",
  "message": "Access denied. You do not have permission to access this resource.",
  "path": "/api/users/profiles/user/2"
}
```

---

## Protected Endpoints

### User Profile Endpoints

#### 1. Update Profile
**Endpoint**: `PATCH /api/users/profiles/user/{userId}`

**Protection**:
```java
@Transactional
public UserProfileResponse execute(Long userId, UpdateUserProfileRequest request) {
    // SECURITY: Validate ownership to prevent IDOR
    ownershipValidator.validateOwnership(userId);

    // ... rest of update logic
}
```

**Result**: Users can only update their own profile (or ADMIN can update any)

#### 2. Delete Profile
**Endpoint**: `DELETE /api/users/profiles/user/{userId}`

**Protection**: Same ownership validation

#### 3. Get Profile
**Endpoint**: `GET /api/users/profiles/user/{userId}`

**Note**: Currently protected, but could be relaxed to allow viewing public profiles

### User Endpoints

#### 4. Update User
**Endpoint**: `PUT /api/users/{id}`

**Protection**:
```java
@Transactional
public UserResponse execute(Long id, UpdateUserRequest request) {
    // SECURITY: Validate ownership to prevent IDOR
    ownershipValidator.validateOwnership(id);

    // ... rest of update logic
}
```

**Note**: Changing roles should require ADMIN (not yet fully implemented)

#### 5. Delete User
**Endpoint**: `DELETE /api/users/{id}`

**Protection**: Ownership validation prevents users from deleting other accounts

#### 6. Get User
**Endpoint**: `GET /api/users/{id}`

**Note**: Currently not protected (may want to allow viewing public user info)

---

## Testing IDOR Protection

### Test Scenario 1: Unauthorized Profile Update

```bash
# 1. Login as User A (ID=1)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"userA@example.com","password":"password123"}'

# Response: { "accessToken": "eyJhbGc..." }

# 2. Try to update User B's profile (ID=2)
curl -X PATCH http://localhost:8080/api/v1/users/profiles/user/2 \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Hacked","bio":"I hacked this!"}'

# Expected Response: HTTP 403 Forbidden
# {
#   "status": 403,
#   "error": "Access Denied",
#   "message": "Access denied. You do not have permission to access this resource.",
#   "path": "/api/v1/users/profiles/user/2"
# }
```

### Test Scenario 2: Authorized Profile Update

```bash
# 1. Login as User A (ID=1)
# ... (same as above)

# 2. Update own profile (ID=1)
curl -X PATCH http://localhost:8080/api/v1/users/profiles/user/1 \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Updated","bio":"My new bio"}'

# Expected Response: HTTP 200 OK
# {
#   "id": "...",
#   "userId": 1,
#   "firstName": "Updated",
#   "bio": "My new bio",
#   ...
# }
```

### Test Scenario 3: ADMIN Bypass

```bash
# 1. Login as ADMIN
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# 2. Update any user's profile
curl -X PATCH http://localhost:8080/api/v1/users/profiles/user/2 \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Admin Updated"}'

# Expected Response: HTTP 200 OK (ADMIN can access any resource)
```

---

## Security Logs

When an IDOR attempt is detected, the following logs are generated:

```
WARN  c.a.r.g.s.s.OwnershipValidator - SECURITY: IDOR attempt detected!
      User userA@example.com (ID: 1) tried to access resource owned by user ID: 2

WARN  c.a.r.g.s.e.GlobalExceptionHandler - SECURITY: Forbidden access attempt -
      Access denied. You do not have permission to access this resource.
```

These logs can be monitored for security incidents and potential attacks.

---

## Future Enhancements

### 1. Fine-Grained Permissions
Instead of simple ownership validation, implement more granular permissions:
- View-only access to public profiles
- Share access with specific users
- Team/organization-level access

### 2. Audit Trail
Log all access attempts (successful and failed) for security auditing:
```java
@Service
public class SecurityAuditService {
    public void logAccess(Long userId, Long resourceId, boolean granted) {
        // Log to database or audit service
    }
}
```

### 3. Rate Limiting for IDOR Attempts
Implement rate limiting specifically for failed ownership validations to prevent brute-force IDOR attacks.

### 4. Custom Error Messages
Provide more context-specific error messages based on the type of resource:
```java
throw new ForbiddenException(
    "You do not have permission to modify this profile. " +
    "Only the profile owner or administrators can make changes."
);
```

---

## Integration Checklist

When adding new endpoints that access user-specific resources:

- [ ] Identify if the endpoint accesses resources owned by users
- [ ] Inject `OwnershipValidator` into the Use Case
- [ ] Call `ownershipValidator.validateOwnership(resourceOwnerId)` at the beginning of the method
- [ ] Document the IDOR protection in JavaDoc
- [ ] Test with multiple users to verify access control
- [ ] Check logs for security warnings
- [ ] Add integration tests for both authorized and unauthorized access

---

## Related Documentation

- [SECURITY_SETUP.md](./SECURITY_SETUP.md) - General security configuration
- [AWS_IAM_SETUP.md](./AWS_IAM_SETUP.md) - AWS IAM roles configuration
- [RATE_LIMITING_TESTS.md](./RATE_LIMITING_TESTS.md) - Rate limiting testing
- [OWASP Top 10 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)

---

**Last Updated**: 2025-11-11
**Security Level**: MEDIUM PRIORITY IMPLEMENTED ✅
**IDOR Protection**: ACTIVE on all Update/Delete user endpoints