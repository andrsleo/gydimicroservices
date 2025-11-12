#!/bin/bash
#
# Rate Limiting Load Testing Script
#
# Purpose: Load testing for rate limiting mechanisms under concurrent attacks
# Usage: ./rate_limiting_load_test.sh [OPTIONS]
#
# SECURITY: Simulates brute force attacks to verify rate limiting effectiveness
#

set -e

# Configuration
BASE_URL="${API_BASE_URL:-http://localhost:8080}"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
LOG_FILE="rate_limiting_load_test_${TIMESTAMP}.log"
RESULTS_DIR="load_test_results_${TIMESTAMP}"
CONCURRENT_USERS="${CONCURRENT_USERS:-10}"
REQUESTS_PER_USER="${REQUESTS_PER_USER:-20}"
ATTACK_DURATION="${ATTACK_DURATION:-60}"  # seconds
PASSED=0
FAILED=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
  echo "" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"
  echo -e "${BLUE}$1${NC}" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"
}

print_section() {
  echo "" | tee -a "$LOG_FILE"
  echo -e "${CYAN}--- $1 ---${NC}" | tee -a "$LOG_FILE"
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

print_metric() {
  echo -e "${MAGENTA}📊 $1:${NC} $2" | tee -a "$LOG_FILE"
}

# Parse command line arguments
parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      --concurrent)
        CONCURRENT_USERS="$2"
        shift 2
        ;;
      --requests)
        REQUESTS_PER_USER="$2"
        shift 2
        ;;
      --duration)
        ATTACK_DURATION="$2"
        shift 2
        ;;
      --url)
        BASE_URL="$2"
        shift 2
        ;;
      --help)
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  --concurrent N     Number of concurrent attackers (default: 10)"
        echo "  --requests N       Requests per user (default: 20)"
        echo "  --duration N       Attack duration in seconds (default: 60)"
        echo "  --url URL          Base URL (default: http://localhost:8080)"
        echo "  --help             Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                                    # Default settings"
        echo "  $0 --concurrent 50 --requests 100    # Heavy load"
        echo "  $0 --duration 120                     # 2-minute attack"
        exit 0
        ;;
      *)
        echo "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
    esac
  done
}

# Check prerequisites
check_prerequisites() {
  print_header "Prerequisites Check"

  # Create results directory
  mkdir -p "$RESULTS_DIR"
  print_info "Results directory: $RESULTS_DIR"

  # Check if backend is running
  print_test "1" "Checking backend availability..."
  if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    print_fail "Backend is not running at $BASE_URL"
    echo "Start the backend with: ./mvnw spring-boot:run"
    exit 1
  fi
  print_pass "Backend is running at $BASE_URL"

  # Check if curl is available
  print_test "2" "Checking curl installation..."
  if ! command -v curl &> /dev/null; then
    print_fail "curl is not installed"
    exit 1
  fi
  print_pass "curl found: $(curl --version | head -1)"

  # Check if bc is available (for calculations)
  print_test "3" "Checking bc installation..."
  if ! command -v bc &> /dev/null; then
    print_fail "bc is not installed (required for calculations)"
    echo "Install with: brew install bc (macOS) or apt-get install bc (Linux)"
    exit 1
  fi
  print_pass "bc found"

  # Display test configuration
  print_section "Test Configuration"
  print_info "Base URL: $BASE_URL"
  print_info "Concurrent Users: $CONCURRENT_USERS"
  print_info "Requests per User: $REQUESTS_PER_USER"
  print_info "Attack Duration: ${ATTACK_DURATION}s"
  print_info "Total Requests: $((CONCURRENT_USERS * REQUESTS_PER_USER))"

  echo "" | tee -a "$LOG_FILE"
}

# Single attacker simulation
simulate_attacker() {
  local attacker_id=$1
  local ip_address=$2
  local output_file="$RESULTS_DIR/attacker_${attacker_id}.log"
  local success_count=0
  local blocked_count=0
  local error_count=0
  local total_time=0

  for i in $(seq 1 $REQUESTS_PER_USER); do
    start_time=$(date +%s%N)

    # Simulate login attempt
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -H "X-Forwarded-For: $ip_address" \
      -d "{\"email\":\"attacker${attacker_id}@evil.com\",\"password\":\"WrongPass${i}\"}" \
      2>&1)

    end_time=$(date +%s%N)
    elapsed=$((($end_time - $start_time) / 1000000))  # Convert to milliseconds
    total_time=$((total_time + elapsed))

    http_code=$(echo "$response" | tail -1)

    # Log the result
    echo "$i,$http_code,$elapsed" >> "$output_file"

    # Count results
    case $http_code in
      200|201)
        ((success_count++))
        ;;
      429)
        ((blocked_count++))
        ;;
      *)
        ((error_count++))
        ;;
    esac

    # Small delay to prevent overwhelming the system
    sleep 0.1
  done

  # Calculate average response time
  avg_time=$(echo "scale=2; $total_time / $REQUESTS_PER_USER" | bc)

  # Write summary
  echo "SUMMARY: Success=$success_count, Blocked=$blocked_count, Error=$error_count, AvgTime=${avg_time}ms" >> "$output_file"
}

# Test 1: Single IP brute force attack
test_single_ip_attack() {
  print_header "Test 1: Single IP Brute Force Attack"

  print_info "Simulating rapid login attempts from single IP..."
  print_info "Expected: First 5 attempts succeed, rest are blocked (429)"

  local success=0
  local blocked=0
  local total=20

  for i in $(seq 1 $total); do
    response=$(curl -s -w "%{http_code}" -o /dev/null \
      -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"test@example.com\",\"password\":\"wrong${i}\"}")

    if [ "$response" == "429" ]; then
      ((blocked++))
    else
      ((success++))
    fi

    sleep 0.1
  done

  print_metric "Total Attempts" "$total"
  print_metric "Allowed" "$success"
  print_metric "Blocked (429)" "$blocked"
  print_metric "Block Rate" "$(echo "scale=1; ($blocked / $total) * 100" | bc)%"

  # Verify rate limiting is working
  if [ $blocked -ge 10 ]; then
    print_pass "Rate limiting is working (blocked $blocked/$total attempts)"
  else
    print_fail "Rate limiting ineffective (only blocked $blocked/$total attempts)"
  fi
}

# Test 2: Distributed attack from multiple IPs
test_distributed_attack() {
  print_header "Test 2: Distributed Attack (Multiple IPs)"

  print_info "Simulating attacks from $CONCURRENT_USERS different IPs..."
  print_info "Each IP should have independent rate limits"

  local pids=()
  local start_time=$(date +%s)

  # Launch concurrent attackers
  for attacker_id in $(seq 1 $CONCURRENT_USERS); do
    ip_address="10.0.0.$attacker_id"
    simulate_attacker "$attacker_id" "$ip_address" &
    pids+=($!)
  done

  # Wait for all attackers to finish
  print_info "Attack in progress... (${CONCURRENT_USERS} concurrent attackers)"
  for pid in "${pids[@]}"; do
    wait $pid
  done

  local end_time=$(date +%s)
  local duration=$((end_time - start_time))

  # Analyze results
  print_section "Attack Results Analysis"

  local total_success=0
  local total_blocked=0
  local total_errors=0

  for attacker_id in $(seq 1 $CONCURRENT_USERS); do
    output_file="$RESULTS_DIR/attacker_${attacker_id}.log"
    if [ -f "$output_file" ]; then
      summary=$(tail -1 "$output_file")
      success=$(echo "$summary" | grep -oP 'Success=\K\d+')
      blocked=$(echo "$summary" | grep -oP 'Blocked=\K\d+')
      error=$(echo "$summary" | grep -oP 'Error=\K\d+')

      total_success=$((total_success + success))
      total_blocked=$((total_blocked + blocked))
      total_errors=$((total_errors + error))
    fi
  done

  local total_requests=$((CONCURRENT_USERS * REQUESTS_PER_USER))
  local block_rate=$(echo "scale=1; ($total_blocked / $total_requests) * 100" | bc)

  print_metric "Attack Duration" "${duration}s"
  print_metric "Total Requests" "$total_requests"
  print_metric "Successful" "$total_success"
  print_metric "Blocked (429)" "$total_blocked"
  print_metric "Errors" "$total_errors"
  print_metric "Block Rate" "${block_rate}%"
  print_metric "Requests/Second" "$(echo "scale=2; $total_requests / $duration" | bc)"

  # Verify distributed rate limiting
  # Each IP should have ~5 successful requests (rate limit per IP)
  local expected_success=$((CONCURRENT_USERS * 5))
  local tolerance=20  # 20% tolerance
  local min_success=$(($expected_success - ($expected_success * $tolerance / 100)))
  local max_success=$(($expected_success + ($expected_success * $tolerance / 100)))

  if [ $total_success -ge $min_success ] && [ $total_success -le $max_success ]; then
    print_pass "Distributed rate limiting working correctly ($total_success ≈ $expected_success allowed)"
  else
    print_fail "Distributed rate limiting issue ($total_success allowed, expected ≈ $expected_success)"
  fi

  # Verify high block rate
  if (( $(echo "$block_rate > 50" | bc -l) )); then
    print_pass "High block rate achieved (${block_rate}%)"
  else
    print_fail "Block rate too low (${block_rate}%, expected > 50%)"
  fi
}

# Test 3: Sustained attack over time
test_sustained_attack() {
  print_header "Test 3: Sustained Attack Duration Test"

  print_info "Simulating sustained attack for ${ATTACK_DURATION}s..."
  print_info "Testing rate limit persistence and recovery"

  local output_file="$RESULTS_DIR/sustained_attack.log"
  local start_time=$(date +%s)
  local request_count=0
  local blocked_count=0

  echo "Timestamp,HTTP_Code,Response_Time_ms" > "$output_file"

  while [ $(($(date +%s) - start_time)) -lt $ATTACK_DURATION ]; do
    timestamp=$(date +%s)

    start_req=$(date +%s%N)
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"sustained@attack.com\",\"password\":\"wrong${request_count}\"}" \
      2>&1)
    end_req=$(date +%s%N)

    elapsed=$((($end_req - $start_req) / 1000000))
    http_code=$(echo "$response" | tail -1)

    echo "$timestamp,$http_code,$elapsed" >> "$output_file"

    ((request_count++))
    if [ "$http_code" == "429" ]; then
      ((blocked_count++))
    fi

    # Show progress every 10 requests
    if [ $((request_count % 10)) -eq 0 ]; then
      elapsed_time=$(($(date +%s) - start_time))
      echo -ne "\rProgress: ${elapsed_time}s / ${ATTACK_DURATION}s - Requests: $request_count - Blocked: $blocked_count" | tee -a "$LOG_FILE"
    fi

    sleep 0.5
  done

  echo "" | tee -a "$LOG_FILE"

  # Analysis
  print_section "Sustained Attack Analysis"

  local block_rate=$(echo "scale=1; ($blocked_count / $request_count) * 100" | bc)
  local avg_rps=$(echo "scale=2; $request_count / $ATTACK_DURATION" | bc)

  print_metric "Duration" "${ATTACK_DURATION}s"
  print_metric "Total Requests" "$request_count"
  print_metric "Blocked (429)" "$blocked_count"
  print_metric "Block Rate" "${block_rate}%"
  print_metric "Avg Requests/Second" "$avg_rps"

  # Verify consistent blocking
  if (( $(echo "$block_rate > 70" | bc -l) )); then
    print_pass "Rate limiting sustained over time (${block_rate}% blocked)"
  else
    print_fail "Rate limiting degraded over time (only ${block_rate}% blocked)"
  fi
}

# Test 4: Response time under load
test_response_time_under_load() {
  print_header "Test 4: Response Time Under Load"

  print_info "Testing response times with concurrent requests..."

  local output_file="$RESULTS_DIR/response_times.log"
  local concurrent=20
  local requests_each=10
  local pids=()

  # Launch concurrent requests
  for i in $(seq 1 $concurrent); do
    (
      for j in $(seq 1 $requests_each); do
        start=$(date +%s%N)
        curl -s -o /dev/null "$BASE_URL/actuator/health"
        end=$(date +%s%N)
        elapsed=$((($end - $start) / 1000000))
        echo "$elapsed" >> "${output_file}_${i}"
      done
    ) &
    pids+=($!)
  done

  # Wait for completion
  for pid in "${pids[@]}"; do
    wait $pid
  done

  # Aggregate results
  cat ${output_file}_* > "$output_file"
  rm ${output_file}_*

  # Calculate statistics
  local total_requests=$(cat "$output_file" | wc -l)
  local avg=$(awk '{ sum += $1; count++ } END { print sum/count }' "$output_file")
  local min=$(sort -n "$output_file" | head -1)
  local max=$(sort -n "$output_file" | tail -1)
  local p50=$(sort -n "$output_file" | awk '{all[NR] = $0} END{print all[int(NR*0.50)]}')
  local p95=$(sort -n "$output_file" | awk '{all[NR] = $0} END{print all[int(NR*0.95)]}')
  local p99=$(sort -n "$output_file" | awk '{all[NR] = $0} END{print all[int(NR*0.99)]}')

  print_metric "Total Requests" "$total_requests"
  print_metric "Min Response Time" "${min}ms"
  print_metric "Avg Response Time" "$(printf "%.2f" $avg)ms"
  print_metric "P50 (Median)" "${p50}ms"
  print_metric "P95" "${p95}ms"
  print_metric "P99" "${p99}ms"
  print_metric "Max Response Time" "${max}ms"

  # Verify acceptable performance
  if (( $(echo "$avg < 500" | bc -l) )); then
    print_pass "Average response time acceptable ($(printf "%.2f" $avg)ms < 500ms)"
  else
    print_fail "Average response time too high ($(printf "%.2f" $avg)ms)"
  fi

  if (( $(echo "$p95 < 1000" | bc -l) )); then
    print_pass "P95 response time acceptable (${p95}ms < 1000ms)"
  else
    print_fail "P95 response time too high (${p95}ms)"
  fi
}

# Test 5: Rate limit recovery
test_rate_limit_recovery() {
  print_header "Test 5: Rate Limit Recovery Test"

  print_info "Testing rate limit bucket refill after waiting..."

  # First, exhaust the rate limit
  print_section "Phase 1: Exhausting Rate Limit"
  local blocked=0
  for i in {1..10}; do
    response=$(curl -s -w "%{http_code}" -o /dev/null \
      -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"recovery@test.com\",\"password\":\"wrong${i}\"}")

    if [ "$response" == "429" ]; then
      ((blocked++))
    fi
  done

  print_info "Blocked: $blocked/10 attempts"

  # Wait for rate limit to reset (15 minutes in real scenario, but we'll test partial recovery)
  print_section "Phase 2: Waiting for Partial Recovery (30 seconds)"
  for i in {1..30}; do
    echo -ne "\rWaiting... ${i}/30s"
    sleep 1
  done
  echo ""

  # Try again
  print_section "Phase 3: Testing Recovery"
  local success_after=0
  for i in {1..5}; do
    response=$(curl -s -w "%{http_code}" -o /dev/null \
      -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"recovery@test.com\",\"password\":\"attempt${i}\"}")

    if [ "$response" != "429" ]; then
      ((success_after++))
    fi
    sleep 1
  done

  print_metric "Successful After Recovery" "$success_after/5"

  if [ $success_after -gt 0 ]; then
    print_pass "Rate limit recovery working (tokens refilled)"
  else
    print_info "Rate limit still in effect (15-minute window not expired)"
  fi
}

# Generate performance report
generate_performance_report() {
  print_header "Performance Report"

  local report_file="$RESULTS_DIR/performance_report.txt"

  {
    echo "========================================"
    echo "Rate Limiting Load Test Report"
    echo "========================================"
    echo ""
    echo "Test Date: $(date)"
    echo "Base URL: $BASE_URL"
    echo ""
    echo "Configuration:"
    echo "  - Concurrent Users: $CONCURRENT_USERS"
    echo "  - Requests per User: $REQUESTS_PER_USER"
    echo "  - Attack Duration: ${ATTACK_DURATION}s"
    echo ""
    echo "Results Directory: $RESULTS_DIR"
    echo ""
    echo "See individual test logs for detailed results."
    echo "========================================"
  } | tee "$report_file"

  print_info "Performance report saved: $report_file"
}

# Generate final report
generate_report() {
  print_header "Test Results Summary"

  local total=$((PASSED + FAILED))
  local success_rate=$(echo "scale=1; ($PASSED / $total) * 100" | bc)

  echo "Test Date: $(date)" | tee -a "$LOG_FILE"
  echo "Base URL: $BASE_URL" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"
  echo -e "${GREEN}Passed:${NC} $PASSED" | tee -a "$LOG_FILE"
  echo -e "${RED}Failed:${NC} $FAILED" | tee -a "$LOG_FILE"
  echo "Total: $total" | tee -a "$LOG_FILE"
  echo "Success Rate: ${success_rate}%" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All rate limiting load tests passed!${NC}" | tee -a "$LOG_FILE"
    echo -e "${GREEN}✅ Rate limiting is effective under load${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=0
  else
    echo -e "${RED}⚠️ Some tests failed!${NC}" | tee -a "$LOG_FILE"
    echo -e "${RED}❌ Review failed tests and adjust rate limiting configuration${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=1
  fi

  echo "" | tee -a "$LOG_FILE"
  echo "Results saved to: $RESULTS_DIR/" | tee -a "$LOG_FILE"
  echo "Full test log: $LOG_FILE" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"

  return $EXIT_CODE
}

# Main execution
main() {
  clear
  print_header "Rate Limiting Load Testing Suite"
  echo "GYDI Application - Rate Limiting Performance Test" | tee -a "$LOG_FILE"
  echo "Target: $BASE_URL" | tee -a "$LOG_FILE"
  echo "Date: $(date)" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  parse_args "$@"
  check_prerequisites

  # Run all tests
  test_single_ip_attack
  test_distributed_attack
  test_sustained_attack
  test_response_time_under_load
  test_rate_limit_recovery

  # Generate reports
  generate_performance_report
  generate_report

  exit $?
}

# Run the tests
main "$@"