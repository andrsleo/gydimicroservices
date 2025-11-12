# Rate Limiting Load Testing Guide

**Purpose**: Load testing and performance validation for rate limiting mechanisms under realistic attack scenarios.

**Date**: November 2025
**Security Level**: CRITICAL
**Test Type**: Performance & Load Testing

---

## Overview

This document provides comprehensive load testing procedures for the GYDI application's rate limiting system. The tests simulate various brute force attack scenarios to verify that the rate limiting mechanism effectively protects against unauthorized access attempts under high load conditions.

## Rate Limiting Configuration

### Current Implementation

**Technology**: Bucket4j 7.6.0 (Token Bucket Algorithm)

**Configuration**:
```java
// RateLimitService.java
private Bucket createAuthBucket() {
    Bandwidth limit = Bandwidth.classic(
        5,  // 5 tokens (attempts allowed)
        Refill.intervally(5, Duration.ofMinutes(15))
    );
    return Bucket.builder().addLimit(limit).build();
}
```

**Rate Limits**:
- **Authentication Endpoint**: 5 attempts per 15 minutes per IP
- **General API**: 100 requests per hour per IP
- **Response**: HTTP 429 (Too Many Requests)
- **Headers**: `X-RateLimit-Remaining`, `X-RateLimit-Retry-After`

**IP Detection**:
- Supports `X-Forwarded-For` header (proxy/load balancer)
- Extracts client IP from first entry in X-Forwarded-For chain
- Falls back to `HttpServletRequest.getRemoteAddr()`

---

## Test Suite Overview

The load testing script (`rate_limiting_load_test.sh`) includes 5 comprehensive tests:

| Test # | Name | Purpose | Duration | Requests |
|--------|------|---------|----------|----------|
| 1 | Single IP Attack | Verify rate limiting for single attacker | ~2s | 20 |
| 2 | Distributed Attack | Test independent rate limits per IP | ~30s | 200+ |
| 3 | Sustained Attack | Verify persistence over time | 60s | ~120 |
| 4 | Response Time | Measure performance under load | ~10s | 200 |
| 5 | Recovery Test | Verify token bucket refill | 40s | 15 |

**Total Test Duration**: ~2-3 minutes
**Total Requests**: 500-600+

---

## Prerequisites

### System Requirements

```bash
# Required tools
- curl (HTTP client)
- bc (calculator for metrics)
- bash 4.0+

# Install on macOS
brew install bash bc

# Install on Linux
apt-get install bash bc
```

### Backend Requirements

```bash
# 1. Backend must be running
cd GydiMicroservices
./mvnw spring-boot:run

# 2. Verify backend is accessible
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Performance Baseline

Before running load tests, ensure your system can handle:
- **CPU**: < 50% usage at idle
- **Memory**: > 2GB free
- **Network**: Stable connection
- **Disk**: > 1GB free (for logs)

---

## Running Load Tests

### Basic Usage

```bash
cd GydiMicroservices

# Run with default settings
./rate_limiting_load_test.sh

# Results will be saved to:
# - rate_limiting_load_test_YYYY-MM-DD_HH-MM-SS.log
# - load_test_results_YYYY-MM-DD_HH-MM-SS/
```

### Advanced Configuration

```bash
# Custom concurrent users and requests
./rate_limiting_load_test.sh --concurrent 50 --requests 100

# Custom attack duration
./rate_limiting_load_test.sh --duration 120

# Custom backend URL
./rate_limiting_load_test.sh --url http://production.gydi.com

# Full customization
./rate_limiting_load_test.sh \
  --concurrent 25 \
  --requests 50 \
  --duration 90 \
  --url http://staging.gydi.com
```

### Configuration Options

| Option | Description | Default | Recommended |
|--------|-------------|---------|-------------|
| `--concurrent` | Number of concurrent attackers | 10 | 10-50 |
| `--requests` | Requests per attacker | 20 | 20-100 |
| `--duration` | Sustained attack duration (seconds) | 60 | 60-300 |
| `--url` | Backend base URL | http://localhost:8080 | - |

---

## Test Scenarios Explained

### Test 1: Single IP Brute Force Attack

**Objective**: Verify rate limiting works for a single attacker

**Attack Simulation**:
```
IP: 192.168.1.100
Attempts: 20 rapid login requests
Expected: First 5 succeed, next 15 blocked (429)
```

**Success Criteria**:
- ✅ Block rate > 50% (10+ requests blocked out of 20)
- ✅ Consistent 429 responses after 5th attempt
- ✅ Appropriate `Retry-After` header in 429 responses

**Example Output**:
```
📊 Total Attempts: 20
📊 Allowed: 5
📊 Blocked (429): 15
📊 Block Rate: 75.0%
✅ PASS: Rate limiting is working (blocked 15/20 attempts)
```

---

### Test 2: Distributed Attack (Multiple IPs)

**Objective**: Verify independent rate limits per IP address

**Attack Simulation**:
```
IPs: 10.0.0.1, 10.0.0.2, ..., 10.0.0.N
Concurrent Attackers: 10 (configurable)
Requests per IP: 20
Total Requests: 200
```

**Expected Behavior**:
- Each IP has independent rate limit bucket
- Each IP gets 5 successful attempts
- Total successful ≈ 5 × N concurrent users
- Total blocked ≈ 15 × N concurrent users

**Success Criteria**:
- ✅ Total successful ≈ 50 (5 per IP × 10 IPs)
- ✅ Block rate > 50%
- ✅ No interference between different IPs
- ✅ Requests/second metric calculated

**Example Output**:
```
📊 Attack Duration: 32s
📊 Total Requests: 200
📊 Successful: 48
📊 Blocked (429): 150
📊 Errors: 2
📊 Block Rate: 75.0%
📊 Requests/Second: 6.25
✅ PASS: Distributed rate limiting working correctly (48 ≈ 50 allowed)
✅ PASS: High block rate achieved (75.0%)
```

**Analysis**:
- Each attacker gets ~5 successful attempts (independent limits)
- Remaining attempts are blocked
- System handles concurrent load effectively

---

### Test 3: Sustained Attack Duration Test

**Objective**: Verify rate limiting persists over extended period

**Attack Simulation**:
```
IP: 203.0.113.1
Duration: 60 seconds (configurable)
Request Frequency: ~2 requests/second
Total Requests: ~120
```

**Expected Behavior**:
- Initial 5 attempts succeed
- All subsequent attempts blocked
- Block rate remains high throughout duration
- No degradation over time

**Success Criteria**:
- ✅ Block rate > 70% throughout entire duration
- ✅ Consistent 429 responses after initial 5 attempts
- ✅ No rate limit bypass after long duration

**Example Output**:
```
Progress: 60s / 60s - Requests: 118 - Blocked: 98

📊 Duration: 60s
📊 Total Requests: 118
📊 Blocked (429): 98
📊 Block Rate: 83.1%
📊 Avg Requests/Second: 1.97
✅ PASS: Rate limiting sustained over time (83.1% blocked)
```

**Graph (Conceptual)**:
```
Requests Over Time:
|
|  ✓✓✓✓✓ ✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗✗
|  ^^^^^  ^^^^^^^^^^^^^^^^^^^^
|  Allowed      Blocked
|
+----------------------------------------> Time (60s)
```

---

### Test 4: Response Time Under Load

**Objective**: Measure system performance under concurrent load

**Attack Simulation**:
```
Concurrent Threads: 20
Requests per Thread: 10
Total Requests: 200
Endpoint: /actuator/health (lightweight)
```

**Metrics Collected**:
- **Min Response Time**: Fastest request
- **Avg Response Time**: Average across all requests
- **P50 (Median)**: 50th percentile
- **P95**: 95th percentile (slower requests)
- **P99**: 99th percentile (slowest requests)
- **Max Response Time**: Slowest request

**Success Criteria**:
- ✅ Average response time < 500ms
- ✅ P95 response time < 1000ms
- ✅ No significant degradation under load

**Example Output**:
```
📊 Total Requests: 200
📊 Min Response Time: 12ms
📊 Avg Response Time: 145.32ms
📊 P50 (Median): 138ms
📊 P95: 285ms
📊 P99: 412ms
📊 Max Response Time: 523ms
✅ PASS: Average response time acceptable (145.32ms < 500ms)
✅ PASS: P95 response time acceptable (285ms < 1000ms)
```

**Performance Categories**:
- **Excellent**: Avg < 100ms, P95 < 200ms
- **Good**: Avg < 300ms, P95 < 500ms
- **Acceptable**: Avg < 500ms, P95 < 1000ms
- **Poor**: Avg > 500ms or P95 > 1000ms

---

### Test 5: Rate Limit Recovery Test

**Objective**: Verify token bucket refill mechanism

**Attack Simulation**:
```
Phase 1: Exhaust rate limit (10 attempts)
Phase 2: Wait 30 seconds
Phase 3: Retry 5 attempts
```

**Expected Behavior**:
- Phase 1: First 5 succeed, last 5 blocked
- Phase 2: Bucket starts refilling tokens
- Phase 3: Some attempts succeed (tokens refilled)

**Token Refill**:
- **Full Refill**: 15 minutes (5 tokens)
- **Partial Refill**: 3 minutes (1 token)
- **Test Wait**: 30 seconds (partial recovery)

**Success Criteria**:
- ✅ At least 1 successful attempt after waiting
- ✅ Tokens are being refilled over time
- ✅ Rate limit is not permanent

**Example Output**:
```
Phase 1: Exhausting Rate Limit
ℹ️  Blocked: 6/10 attempts

Phase 2: Waiting for Partial Recovery (30 seconds)
Waiting... 30/30s

Phase 3: Testing Recovery
📊 Successful After Recovery: 2/5
✅ PASS: Rate limit recovery working (tokens refilled)
```

**Note**: Full recovery requires 15 minutes. This test only verifies partial recovery after 30 seconds.

---

## Interpreting Results

### Success Indicators

✅ **Rate Limiting Working**:
- Block rate > 70% in all tests
- 429 responses after 5th attempt per IP
- Independent limits per IP address
- Consistent blocking over time
- Token bucket refill working

✅ **Performance Acceptable**:
- Average response time < 500ms
- P95 < 1000ms
- No degradation under load
- Handles concurrent requests effectively

### Failure Indicators

❌ **Rate Limiting Issues**:
- Block rate < 50%
- Unlimited attempts from single IP
- Rate limits shared across different IPs
- Blocking degrades over time
- No token refill after waiting

❌ **Performance Issues**:
- Average response time > 500ms
- P95 > 1000ms
- Timeouts or connection errors
- High error rate
- System becomes unresponsive

---

## Results Analysis

### Generated Files

After running tests, you'll find:

```
load_test_results_YYYY-MM-DD_HH-MM-SS/
├── attacker_1.log          # Individual attacker logs
├── attacker_2.log
├── ...
├── attacker_N.log
├── sustained_attack.log    # Sustained attack data
├── response_times.log      # Response time measurements
└── performance_report.txt  # Summary report
```

### Log File Format

**Attacker Logs** (`attacker_N.log`):
```csv
Request,HTTP_Code,Response_Time_ms
1,401,145
2,401,138
3,401,142
4,401,151
5,401,139
6,429,85
7,429,82
...
SUMMARY: Success=0, Blocked=15, Error=5, AvgTime=125.43ms
```

**Sustained Attack Log** (`sustained_attack.log`):
```csv
Timestamp,HTTP_Code,Response_Time_ms
1699715400,401,145
1699715401,401,138
1699715402,429,82
...
```

**Response Times Log** (`response_times.log`):
```
145
138
142
151
...
```

### Visualization (Manual)

You can import logs into Excel/Google Sheets for visualization:

```bash
# Convert to CSV and import
cat load_test_results_*/sustained_attack.log | \
  sed '1d' | \
  awk -F',' '{print $1, $2, $3}' > sustained_attack.csv
```

**Recommended Graphs**:
1. **HTTP Status Codes Over Time**: Line graph showing 200 vs 429 responses
2. **Response Time Distribution**: Histogram of response times
3. **Block Rate by IP**: Bar chart comparing block rates across attackers

---

## Troubleshooting

### Issue: All requests succeed (no blocking)

**Possible Causes**:
1. Rate limiting not enabled
2. IP detection not working
3. Bucket configuration incorrect

**Solutions**:
```bash
# 1. Check RateLimitService bean is loaded
curl http://localhost:8080/actuator/beans | jq '.beans.rateLimitService'

# 2. Check application logs
tail -f logs/application.log | grep "Rate limit"

# 3. Verify IP extraction
# Test with X-Forwarded-For header
curl -X POST http://localhost:8080/api/auth/login \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}'
```

### Issue: All requests blocked (too aggressive)

**Possible Causes**:
1. Bucket size too small
2. Refill rate too slow
3. Shared bucket across requests

**Solutions**:
```java
// Adjust RateLimitService.java
private Bucket createAuthBucket() {
    Bandwidth limit = Bandwidth.classic(
        10,  // Increase from 5 to 10
        Refill.intervally(10, Duration.ofMinutes(15))
    );
    return Bucket.builder().addLimit(limit).build();
}
```

### Issue: Performance degradation

**Possible Causes**:
1. Bucket map growing too large
2. Memory leak
3. Inefficient IP extraction

**Solutions**:
```bash
# 1. Monitor memory usage
jcmd <PID> GC.heap_info

# 2. Clear old buckets periodically
# Add scheduled cleanup to RateLimitService

# 3. Profile with JProfiler/YourKit
```

### Issue: Test script fails

**Error**: "Backend is not running"
```bash
# Solution: Start backend
cd GydiMicroservices
./mvnw spring-boot:run
```

**Error**: "bc: command not found"
```bash
# Solution: Install bc
brew install bc  # macOS
apt-get install bc  # Linux
```

**Error**: "Permission denied"
```bash
# Solution: Make script executable
chmod +x rate_limiting_load_test.sh
```

---

## Performance Tuning

### Recommended Settings by Environment

**Development**:
```java
Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)))
```

**Staging**:
```java
Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(10)))
```

**Production**:
```java
Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)))
// Stricter limits for production
```

### Scaling Considerations

**Single Server**:
- In-memory bucket storage works well
- No coordination needed

**Multiple Servers (Load Balanced)**:
- Consider Redis-backed Bucket4j
- Shared rate limit state across instances
- Coordinated blocking

**Example Redis Integration**:
```java
// Using Bucket4j with Redis
@Bean
public ProxyManager<String> proxyManager(RedissonClient redisson) {
    return new RedissonProxyManager<>(redisson);
}
```

---

## Continuous Monitoring

### Production Monitoring

**Metrics to Track**:
1. **Rate Limit Hit Rate**: % of requests blocked
2. **Response Times**: P50, P95, P99
3. **Error Rate**: Failed requests
4. **Bucket Size**: Number of active buckets
5. **Memory Usage**: RateLimitService memory footprint

**Alerting Thresholds**:
- Alert if block rate > 20% (potential attack)
- Alert if P95 response time > 1000ms
- Alert if error rate > 5%

**Example Prometheus Metrics**:
```java
@Timed(value = "rate.limit.check", percentiles = {0.5, 0.95, 0.99})
public boolean tryConsumeAuth(HttpServletRequest request) {
    // ... rate limiting logic
}

@Counter(value = "rate.limit.blocked")
private void recordBlocked() {
    // Increment when request is blocked
}
```

---

## Security Compliance

These load tests verify compliance with:

- **OWASP ASVS 4.0**: V2.2 - Anti-automation
- **NIST 800-63B**: Account lockout mechanisms
- **PCI DSS**: Requirement 8.1.6 - Limit repeated access attempts

---

## Next Steps

After running load tests:

1. ✅ **Review Results**: Check all tests passed
2. ✅ **Analyze Metrics**: Review response times and block rates
3. ✅ **Tune Configuration**: Adjust limits if needed
4. ✅ **Document Baseline**: Save results for comparison
5. ✅ **Schedule Regular Tests**: Run weekly/monthly
6. ✅ **Monitor Production**: Set up alerting

---

## Appendix: Quick Reference

### Run Tests
```bash
./rate_limiting_load_test.sh
```

### Custom Configuration
```bash
./rate_limiting_load_test.sh --concurrent 50 --requests 100 --duration 120
```

### Check Results
```bash
cd load_test_results_*
cat performance_report.txt
```

### View Logs
```bash
tail -f rate_limiting_load_test_*.log
```

### Clean Up
```bash
rm -rf load_test_results_*
rm rate_limiting_load_test_*.log
```

---

**Last Updated**: November 2025
**Test Version**: 1.0
**Next Review**: Monthly