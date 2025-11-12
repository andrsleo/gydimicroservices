#!/bin/bash
#
# AWS IAM Configuration Verification Script
#
# Purpose: Verify that AWS IAM roles and permissions are correctly configured
# Usage: ./verify_aws_iam_config.sh [--profile PROFILE_NAME]
#
# SECURITY: This script checks AWS configuration for the GYDI application
#

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
LOG_FILE="aws_iam_verification_${TIMESTAMP}.log"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE=""
PASSED=0
FAILED=0
WARNINGS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

print_check() {
  echo -e "${YELLOW}[CHECK $1]${NC} $2" | tee -a "$LOG_FILE"
}

print_pass() {
  echo -e "${GREEN}✅ PASS:${NC} $1" | tee -a "$LOG_FILE"
  ((PASSED++))
}

print_fail() {
  echo -e "${RED}❌ FAIL:${NC} $1" | tee -a "$LOG_FILE"
  ((FAILED++))
}

print_warn() {
  echo -e "${YELLOW}⚠️  WARN:${NC} $1" | tee -a "$LOG_FILE"
  ((WARNINGS++))
}

print_info() {
  echo -e "ℹ️  $1" | tee -a "$LOG_FILE"
}

# Parse command line arguments
parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      --profile)
        AWS_PROFILE="$2"
        shift 2
        ;;
      --region)
        AWS_REGION="$2"
        shift 2
        ;;
      --help)
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  --profile PROFILE    AWS profile to use (from ~/.aws/credentials)"
        echo "  --region REGION      AWS region (default: us-east-1)"
        echo "  --help               Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                           # Use default credentials"
        echo "  $0 --profile production      # Use 'production' profile"
        echo "  $0 --region us-west-2        # Use us-west-2 region"
        exit 0
        ;;
      *)
        echo "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
    esac
  done

  # Set AWS CLI options
  if [ ! -z "$AWS_PROFILE" ]; then
    AWS_CMD="aws --profile $AWS_PROFILE --region $AWS_REGION"
    print_info "Using AWS profile: $AWS_PROFILE"
  else
    AWS_CMD="aws --region $AWS_REGION"
    print_info "Using default AWS credentials"
  fi
}

# Check prerequisites
check_prerequisites() {
  print_header "Prerequisites Check"

  # Check if AWS CLI is installed
  print_check "1" "Checking AWS CLI installation..."
  if ! command -v aws &> /dev/null; then
    print_fail "AWS CLI is not installed"
    echo "Install with: brew install awscli (macOS) or pip install awscli"
    exit 1
  fi

  AWS_VERSION=$(aws --version 2>&1)
  print_pass "AWS CLI found: $AWS_VERSION"

  # Check if jq is installed
  print_check "2" "Checking jq installation..."
  if ! command -v jq &> /dev/null; then
    print_fail "jq is not installed"
    echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
  fi

  JQ_VERSION=$(jq --version)
  print_pass "jq found: $JQ_VERSION"

  # Test AWS credentials
  print_check "3" "Testing AWS credentials..."
  if ! $AWS_CMD sts get-caller-identity > /dev/null 2>&1; then
    print_fail "AWS credentials are not configured or invalid"
    echo ""
    echo "Configure AWS credentials:"
    echo "  1. Run: aws configure"
    echo "  2. Or set environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY"
    echo "  3. Or use IAM role (if running on EC2)"
    exit 1
  fi

  CALLER_IDENTITY=$($AWS_CMD sts get-caller-identity)
  ACCOUNT_ID=$(echo "$CALLER_IDENTITY" | jq -r '.Account')
  USER_ARN=$(echo "$CALLER_IDENTITY" | jq -r '.Arn')
  print_pass "AWS credentials valid"
  print_info "Account ID: $ACCOUNT_ID"
  print_info "User/Role: $USER_ARN"

  echo "" | tee -a "$LOG_FILE"
}

# Verify IAM role exists
verify_iam_role() {
  local role_name=$1
  local description=$2

  print_check "IAM" "Verifying IAM role: $role_name"

  if $AWS_CMD iam get-role --role-name "$role_name" > /dev/null 2>&1; then
    ROLE_INFO=$($AWS_CMD iam get-role --role-name "$role_name")
    ROLE_ARN=$(echo "$ROLE_INFO" | jq -r '.Role.Arn')
    print_pass "IAM role exists: $ROLE_ARN"

    # Check trust policy
    TRUST_POLICY=$(echo "$ROLE_INFO" | jq -r '.Role.AssumeRolePolicyDocument')
    print_info "Trust policy configured"

    # List attached policies
    ATTACHED_POLICIES=$($AWS_CMD iam list-attached-role-policies --role-name "$role_name" | jq -r '.AttachedPolicies[].PolicyName')
    print_info "Attached policies: $(echo $ATTACHED_POLICIES | tr '\n' ', ' | sed 's/,$//')"

    return 0
  else
    print_fail "IAM role not found: $role_name"
    print_info "Create this role using: AWS_IAM_SETUP.md"
    return 1
  fi
}

# Verify S3 bucket exists and permissions
verify_s3_bucket() {
  local bucket_name=$1

  print_check "S3" "Verifying S3 bucket: $bucket_name"

  # Check if bucket exists
  if $AWS_CMD s3 ls "s3://$bucket_name" > /dev/null 2>&1; then
    print_pass "S3 bucket exists and accessible: $bucket_name"

    # Check bucket region
    BUCKET_REGION=$($AWS_CMD s3api get-bucket-location --bucket "$bucket_name" | jq -r '.LocationConstraint // "us-east-1"')
    print_info "Bucket region: $BUCKET_REGION"

    # Check bucket versioning
    VERSIONING=$($AWS_CMD s3api get-bucket-versioning --bucket "$bucket_name" | jq -r '.Status // "Disabled"')
    if [ "$VERSIONING" == "Enabled" ]; then
      print_pass "Bucket versioning: Enabled"
    else
      print_warn "Bucket versioning: $VERSIONING (recommended: Enabled)"
    fi

    # Check bucket encryption
    if $AWS_CMD s3api get-bucket-encryption --bucket "$bucket_name" > /dev/null 2>&1; then
      ENCRYPTION=$($AWS_CMD s3api get-bucket-encryption --bucket "$bucket_name" | jq -r '.ServerSideEncryptionConfiguration.Rules[0].ApplyServerSideEncryptionByDefault.SSEAlgorithm')
      print_pass "Bucket encryption: $ENCRYPTION"
    else
      print_warn "Bucket encryption: Not configured (recommended: AES256 or aws:kms)"
    fi

    # Check public access block
    PUBLIC_ACCESS=$($AWS_CMD s3api get-public-access-block --bucket "$bucket_name" 2>/dev/null | jq -r '.PublicAccessBlockConfiguration.BlockPublicAcls // false')
    if [ "$PUBLIC_ACCESS" == "true" ]; then
      print_pass "Public access blocked"
    else
      print_fail "Public access NOT blocked (security risk!)"
    fi

    # Test write permission
    TEST_FILE="gydi-test-${TIMESTAMP}.txt"
    if echo "GYDI IAM Test" | $AWS_CMD s3 cp - "s3://$bucket_name/$TEST_FILE" > /dev/null 2>&1; then
      print_pass "Write permission verified"

      # Test read permission
      if $AWS_CMD s3 cp "s3://$bucket_name/$TEST_FILE" - > /dev/null 2>&1; then
        print_pass "Read permission verified"
      else
        print_fail "Read permission failed"
      fi

      # Cleanup test file
      $AWS_CMD s3 rm "s3://$bucket_name/$TEST_FILE" > /dev/null 2>&1

      # Test delete permission
      if $AWS_CMD s3 ls "s3://$bucket_name/$TEST_FILE" > /dev/null 2>&1; then
        print_fail "Delete permission failed"
      else
        print_pass "Delete permission verified"
      fi
    else
      print_fail "Write permission failed"
    fi

    return 0
  else
    print_fail "S3 bucket not accessible: $bucket_name"
    print_info "Possible reasons:"
    print_info "  1. Bucket does not exist"
    print_info "  2. IAM permissions insufficient"
    print_info "  3. Bucket is in a different region"
    return 1
  fi
}

# Verify SES configuration
verify_ses_configuration() {
  local sender_email=$1

  print_check "SES" "Verifying SES configuration for: $sender_email"

  # Check if SES is available in region
  if ! $AWS_CMD ses get-account-sending-enabled > /dev/null 2>&1; then
    print_fail "SES is not available or not configured in region $AWS_REGION"
    return 1
  fi

  # Check account sending status
  SENDING_ENABLED=$($AWS_CMD ses get-account-sending-enabled | jq -r '.Enabled')
  if [ "$SENDING_ENABLED" == "true" ]; then
    print_pass "SES sending enabled"
  else
    print_fail "SES sending disabled"
    return 1
  fi

  # Check if email is verified
  VERIFIED_EMAILS=$($AWS_CMD ses list-verified-email-addresses | jq -r '.VerifiedEmailAddresses[]')
  if echo "$VERIFIED_EMAILS" | grep -q "^$sender_email$"; then
    print_pass "Email verified: $sender_email"
  else
    print_warn "Email NOT verified: $sender_email"
    print_info "Verify email with: aws ses verify-email-identity --email-address $sender_email"
  fi

  # Check sending quota
  QUOTA=$($AWS_CMD ses get-send-quota)
  MAX_24_HOUR=$(echo "$QUOTA" | jq -r '.Max24HourSend')
  MAX_RATE=$(echo "$QUOTA" | jq -r '.MaxSendRate')
  SENT_LAST_24=$(echo "$QUOTA" | jq -r '.SentLast24Hours')

  print_info "SES Quota:"
  print_info "  - Max 24 Hour: $MAX_24_HOUR emails"
  print_info "  - Max Send Rate: $MAX_RATE emails/second"
  print_info "  - Sent Last 24h: $SENT_LAST_24 emails"

  if (( $(echo "$SENT_LAST_24 > $MAX_24_HOUR * 0.8" | bc -l) )); then
    print_warn "SES quota usage > 80% ($SENT_LAST_24/$MAX_24_HOUR)"
  fi

  # Test send permission
  print_check "SES" "Testing SES send permission..."
  if $AWS_CMD ses send-email \
    --from "$sender_email" \
    --destination "ToAddresses=$sender_email" \
    --message "Subject={Data='GYDI IAM Test'},Body={Text={Data='IAM verification test'}}" \
    > /dev/null 2>&1; then
    print_pass "SES send permission verified"
  else
    print_fail "SES send permission failed"
  fi
}

# Verify EC2 instance profile (if running on EC2)
verify_ec2_instance_profile() {
  print_check "EC2" "Checking if running on EC2 instance..."

  # Try to get instance metadata
  INSTANCE_ID=$(curl -s http://169.254.169.254/latest/meta-data/instance-id 2>/dev/null || echo "")

  if [ ! -z "$INSTANCE_ID" ]; then
    print_pass "Running on EC2 instance: $INSTANCE_ID"

    # Get instance profile
    INSTANCE_PROFILE=$(curl -s http://169.254.169.254/latest/meta-data/iam/security-credentials/ 2>/dev/null || echo "")

    if [ ! -z "$INSTANCE_PROFILE" ]; then
      print_pass "Instance profile attached: $INSTANCE_PROFILE"

      # Get temporary credentials
      CREDS=$(curl -s "http://169.254.169.254/latest/meta-data/iam/security-credentials/$INSTANCE_PROFILE" 2>/dev/null)
      EXPIRATION=$(echo "$CREDS" | jq -r '.Expiration')
      print_info "Credentials expire: $EXPIRATION"

      # Check if credentials are from instance profile
      CREDENTIALS_SOURCE=$($AWS_CMD configure list | grep "credentials" | awk '{print $4}')
      print_info "Credentials source: $CREDENTIALS_SOURCE"
    else
      print_warn "No instance profile attached to EC2 instance"
    fi
  else
    print_info "Not running on EC2 (or metadata service not accessible)"
  fi
}

# Verify environment variables in application.yml
verify_application_config() {
  print_check "APP" "Verifying application.yml configuration..."

  local app_yml="$SCRIPT_DIR/src/main/resources/application.yml"

  if [ ! -f "$app_yml" ]; then
    print_fail "application.yml not found at: $app_yml"
    return 1
  fi

  print_pass "application.yml found"

  # Check if AWS configuration uses DefaultCredentialsProvider
  if grep -q "DefaultCredentialsProvider" "$SCRIPT_DIR/src/main/java"/**/*.java 2>/dev/null; then
    print_pass "Using DefaultCredentialsProvider (IAM role support)"
  else
    print_warn "DefaultCredentialsProvider not found in code"
  fi

  # Check for hardcoded AWS credentials (security risk!)
  print_check "APP" "Scanning for hardcoded AWS credentials..."

  if grep -r "AKIA" "$SCRIPT_DIR/src" 2>/dev/null | grep -v ".git" | grep -v "target" > /dev/null; then
    print_fail "SECURITY: Hardcoded AWS Access Key found in source code!"
  else
    print_pass "No hardcoded AWS credentials found"
  fi

  # Check for AWS secret key patterns
  if grep -r "aws_secret_access_key" "$app_yml" 2>/dev/null > /dev/null; then
    print_fail "SECURITY: AWS secret key configuration found in application.yml!"
  else
    print_pass "No AWS secret keys in application.yml"
  fi
}

# Verify CloudWatch Logs configuration (optional)
verify_cloudwatch_logs() {
  print_check "CW" "Verifying CloudWatch Logs configuration..."

  LOG_GROUP="/aws/gydi/application"

  # Check if log group exists
  if $AWS_CMD logs describe-log-groups --log-group-name-prefix "$LOG_GROUP" | jq -e '.logGroups[] | select(.logGroupName=="'$LOG_GROUP'")' > /dev/null 2>&1; then
    print_pass "CloudWatch log group exists: $LOG_GROUP"

    # Check retention period
    RETENTION=$($AWS_CMD logs describe-log-groups --log-group-name-prefix "$LOG_GROUP" | jq -r '.logGroups[] | select(.logGroupName=="'$LOG_GROUP'") | .retentionInDays // "Never expires"')
    print_info "Log retention: $RETENTION days"

    # Test write permission
    if $AWS_CMD logs put-log-events \
      --log-group-name "$LOG_GROUP" \
      --log-stream-name "iam-test-${TIMESTAMP}" \
      --log-events timestamp=$(date +%s000),message="IAM verification test" \
      > /dev/null 2>&1; then
      print_pass "CloudWatch Logs write permission verified"
    else
      print_warn "CloudWatch Logs write permission failed (optional)"
    fi
  else
    print_info "CloudWatch log group not found (optional): $LOG_GROUP"
  fi
}

# Generate recommendation report
generate_recommendations() {
  print_header "Recommendations"

  if [ $FAILED -gt 0 ]; then
    echo -e "${RED}⚠️  CRITICAL ISSUES FOUND${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo "The following issues must be resolved before deployment:" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"

    if grep -q "IAM role not found" "$LOG_FILE"; then
      echo "1. Create required IAM roles:" | tee -a "$LOG_FILE"
      echo "   - Follow: AWS_IAM_SETUP.md" | tee -a "$LOG_FILE"
      echo "   - Required roles: GydiApplicationRole, GydiS3AccessRole, GydiSESAccessRole" | tee -a "$LOG_FILE"
      echo "" | tee -a "$LOG_FILE"
    fi

    if grep -q "S3 bucket not accessible" "$LOG_FILE"; then
      echo "2. Configure S3 bucket:" | tee -a "$LOG_FILE"
      echo "   - Create bucket: aws s3 mb s3://gydi-uploads-\$AWS_REGION" | tee -a "$LOG_FILE"
      echo "   - Enable encryption: aws s3api put-bucket-encryption ..." | tee -a "$LOG_FILE"
      echo "   - Block public access: aws s3api put-public-access-block ..." | tee -a "$LOG_FILE"
      echo "" | tee -a "$LOG_FILE"
    fi

    if grep -q "SES" "$LOG_FILE" && grep -q "FAIL" "$LOG_FILE"; then
      echo "3. Configure SES:" | tee -a "$LOG_FILE"
      echo "   - Verify email: aws ses verify-email-identity --email-address noreply@gydi.com" | tee -a "$LOG_FILE"
      echo "   - Request production access (if in sandbox)" | tee -a "$LOG_FILE"
      echo "" | tee -a "$LOG_FILE"
    fi

    if grep -q "SECURITY: Hardcoded AWS" "$LOG_FILE"; then
      echo "4. SECURITY: Remove hardcoded credentials!" | tee -a "$LOG_FILE"
      echo "   - Delete all hardcoded AWS keys from source code" | tee -a "$LOG_FILE"
      echo "   - Use IAM roles or environment variables" | tee -a "$LOG_FILE"
      echo "   - Rotate compromised credentials immediately" | tee -a "$LOG_FILE"
      echo "" | tee -a "$LOG_FILE"
    fi
  fi

  if [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠️  WARNINGS (Recommended Improvements)${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"

    if grep -q "Bucket versioning: Disabled" "$LOG_FILE"; then
      echo "- Enable S3 bucket versioning for data recovery" | tee -a "$LOG_FILE"
    fi

    if grep -q "Bucket encryption: Not configured" "$LOG_FILE"; then
      echo "- Enable S3 bucket encryption (AES256 or KMS)" | tee -a "$LOG_FILE"
    fi

    if grep -q "Email NOT verified" "$LOG_FILE"; then
      echo "- Verify SES email address" | tee -a "$LOG_FILE"
    fi

    echo "" | tee -a "$LOG_FILE"
  fi

  if [ $FAILED -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! AWS IAM configuration is correct.${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo "Your application is ready to use AWS services with IAM roles." | tee -a "$LOG_FILE"
  fi
}

# Generate final report
generate_report() {
  print_header "Verification Results Summary"

  TOTAL=$((PASSED + FAILED + WARNINGS))

  echo "Verification Date: $(date)" | tee -a "$LOG_FILE"
  echo "AWS Region: $AWS_REGION" | tee -a "$LOG_FILE"
  echo "AWS Account: $ACCOUNT_ID" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"
  echo -e "${GREEN}Passed:${NC} $PASSED" | tee -a "$LOG_FILE"
  echo -e "${RED}Failed:${NC} $FAILED" | tee -a "$LOG_FILE"
  echo -e "${YELLOW}Warnings:${NC} $WARNINGS" | tee -a "$LOG_FILE"
  echo "Total Checks: $TOTAL" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 AWS IAM Configuration Verification: PASSED${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=0
  else
    echo -e "${RED}❌ AWS IAM Configuration Verification: FAILED${NC}" | tee -a "$LOG_FILE"
    EXIT_CODE=1
  fi

  echo "" | tee -a "$LOG_FILE"
  echo "Full verification log saved to: $LOG_FILE" | tee -a "$LOG_FILE"
  echo "==================================================" | tee -a "$LOG_FILE"

  return $EXIT_CODE
}

# Main execution
main() {
  clear
  print_header "AWS IAM Configuration Verification"
  echo "GYDI Application - AWS Security Check" | tee -a "$LOG_FILE"
  echo "Date: $(date)" | tee -a "$LOG_FILE"
  echo "" | tee -a "$LOG_FILE"

  parse_args "$@"
  check_prerequisites

  # Verify IAM roles
  print_section "IAM Roles Verification"
  verify_iam_role "GydiApplicationRole" "Main application role"
  verify_iam_role "GydiS3AccessRole" "S3 access role"
  verify_iam_role "GydiSESAccessRole" "SES access role"

  # Verify S3 buckets
  print_section "S3 Buckets Verification"
  verify_s3_bucket "gydi-uploads-${AWS_REGION}"
  verify_s3_bucket "gydi-backups-${AWS_REGION}"

  # Verify SES configuration
  print_section "SES Configuration Verification"
  verify_ses_configuration "noreply@gydi.com"

  # Verify EC2 instance profile (if applicable)
  print_section "EC2 Instance Profile Verification"
  verify_ec2_instance_profile

  # Verify application configuration
  print_section "Application Configuration Verification"
  verify_application_config

  # Verify CloudWatch Logs (optional)
  print_section "CloudWatch Logs Verification"
  verify_cloudwatch_logs

  # Generate recommendations and report
  generate_recommendations
  generate_report

  exit $?
}

# Run the verification
main "$@"