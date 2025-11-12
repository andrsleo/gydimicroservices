# AWS IAM Roles Configuration Guide

## Overview

GYDI Microservices uses **AWS IAM roles** instead of hardcoded access keys for enhanced security. This document explains how to configure IAM roles for your deployment environment.

---

## Why IAM Roles?

### Security Benefits

1. **No Hardcoded Credentials**: Eliminates the risk of accidentally committing AWS credentials to source control
2. **Automatic Credential Rotation**: AWS automatically rotates temporary credentials
3. **Fine-Grained Permissions**: IAM policies can be precisely scoped to only required actions
4. **Audit Trail**: AWS CloudTrail logs all actions performed using the role
5. **No Manual Key Management**: No need to rotate, store, or distribute access keys

### How It Works

The AWS SDK's `DefaultCredentialsProvider` searches for credentials in this order:

1. **Environment variables**: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
2. **System properties**: `aws.accessKeyId`, `aws.secretAccessKey`
3. **IAM role** attached to EC2 instance, ECS task, or Lambda function
4. **AWS credentials file**: `~/.aws/credentials`

---

## Required AWS Permissions

### S3 Permissions (File Storage)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowS3Operations",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::gydi-property-images",
        "arn:aws:s3:::gydi-property-images/*"
      ]
    }
  ]
}
```

### SES Permissions (Email Sending)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowSESOperations",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Setup Instructions by Environment

### 1. EC2 Instance (Recommended for VMs)

#### Step 1: Create IAM Role

```bash
# Create trust policy for EC2
cat > trust-policy-ec2.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create the role
aws iam create-role \
  --role-name gydi-microservices-role \
  --assume-role-policy-document file://trust-policy-ec2.json

# Create and attach S3 policy
aws iam put-role-policy \
  --role-name gydi-microservices-role \
  --policy-name S3AccessPolicy \
  --policy-document file://s3-policy.json

# Create and attach SES policy
aws iam put-role-policy \
  --role-name gydi-microservices-role \
  --policy-name SESAccessPolicy \
  --policy-document file://ses-policy.json

# Create instance profile
aws iam create-instance-profile \
  --instance-profile-name gydi-microservices-profile

# Add role to instance profile
aws iam add-role-to-instance-profile \
  --instance-profile-name gydi-microservices-profile \
  --role-name gydi-microservices-role
```

#### Step 2: Attach Role to EC2 Instance

**For existing instance:**
```bash
aws ec2 associate-iam-instance-profile \
  --instance-id i-1234567890abcdef0 \
  --iam-instance-profile Name=gydi-microservices-profile
```

**For new instance (during launch):**
```bash
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --iam-instance-profile Name=gydi-microservices-profile \
  --key-name your-key-pair
```

**Via AWS Console:**
1. Go to EC2 Dashboard → Instances
2. Select your instance
3. Actions → Security → Modify IAM role
4. Select `gydi-microservices-profile`
5. Save

#### Step 3: Verify Role Attachment

SSH into your EC2 instance and verify:

```bash
# Check if role is attached
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/

# Should return: gydi-microservices-role

# Check temporary credentials (should return JSON with AccessKeyId, SecretAccessKey, Token)
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/gydi-microservices-role
```

---

### 2. ECS/Fargate (Recommended for Containers)

#### Step 1: Create IAM Role for ECS Task

```bash
# Create trust policy for ECS tasks
cat > trust-policy-ecs.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create the role
aws iam create-role \
  --role-name gydi-ecs-task-role \
  --assume-role-policy-document file://trust-policy-ecs.json

# Attach policies
aws iam put-role-policy \
  --role-name gydi-ecs-task-role \
  --policy-name S3AccessPolicy \
  --policy-document file://s3-policy.json

aws iam put-role-policy \
  --role-name gydi-ecs-task-role \
  --policy-name SESAccessPolicy \
  --policy-document file://ses-policy.json
```

#### Step 2: Update ECS Task Definition

```json
{
  "family": "gydi-microservices",
  "taskRoleArn": "arn:aws:iam::YOUR_ACCOUNT_ID:role/gydi-ecs-task-role",
  "containerDefinitions": [
    {
      "name": "gydi-app",
      "image": "your-ecr-repo/gydi-microservices:latest",
      "memory": 2048,
      "cpu": 1024,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "AWS_REGION",
          "value": "us-east-1"
        },
        {
          "name": "AWS_S3_BUCKET_NAME",
          "value": "gydi-property-images"
        },
        {
          "name": "AWS_S3_BASE_URL",
          "value": "https://gydi-property-images.s3.amazonaws.com"
        }
      ]
    }
  ]
}
```

---

### 3. AWS Lambda (For Serverless)

Lambda functions automatically have IAM roles attached.

```bash
# Create role with Lambda trust policy
cat > trust-policy-lambda.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role and attach policies
aws iam create-role \
  --role-name gydi-lambda-role \
  --assume-role-policy-document file://trust-policy-lambda.json

# Attach AWS managed policy for Lambda basic execution
aws iam attach-role-policy \
  --role-name gydi-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Attach custom policies
aws iam put-role-policy \
  --role-name gydi-lambda-role \
  --policy-name S3AccessPolicy \
  --policy-document file://s3-policy.json
```

---

### 4. Local Development (Fallback to Access Keys)

For local development, you can still use access keys:

#### Option A: Environment Variables

```bash
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
export AWS_REGION="us-east-1"

# Run application
./mvnw spring-boot:run
```

#### Option B: AWS Credentials File

```bash
# Configure AWS CLI
aws configure

# This creates ~/.aws/credentials:
# [default]
# aws_access_key_id = YOUR_ACCESS_KEY_ID
# aws_secret_access_key = YOUR_SECRET_ACCESS_KEY

# Run application (will automatically use credentials file)
./mvnw spring-boot:run
```

---

## Testing IAM Role Configuration

### 1. Verify Credentials Discovery

Add this test endpoint to your application:

```java
@RestController
@RequestMapping("/api/test")
public class AwsCredentialsTestController {

    @GetMapping("/aws-credentials-check")
    public ResponseEntity<String> checkAwsCredentials() {
        try {
            DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
            AwsCredentials credentials = credentialsProvider.resolveCredentials();

            // Don't log actual credentials!
            return ResponseEntity.ok("AWS credentials successfully loaded: " +
                credentials.getClass().getSimpleName());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Failed to load AWS credentials: " + e.getMessage());
        }
    }
}
```

Expected responses:
- **With IAM role**: `AwsSessionCredentials` (temporary credentials)
- **With access keys**: `AwsBasicCredentials` (static credentials)
- **No credentials**: Error message

### 2. Test S3 Upload

```bash
# Upload a test file
curl -X POST http://your-app.com/api/users/profile/picture \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test-image.jpg"

# Check application logs for:
# "S3StorageService initialized for bucket: gydi-property-images in region: us-east-1 using IAM credentials"
```

### 3. Verify S3 Bucket Access

```bash
# From your EC2/ECS instance, test AWS CLI
aws s3 ls s3://gydi-property-images/

# Should list bucket contents without requiring access keys
```

---

## Troubleshooting

### Error: "Unable to load credentials from any of the providers in the chain"

**Cause**: No credentials found (no IAM role, no environment variables, no credentials file)

**Solutions**:
1. **EC2**: Verify IAM role is attached: `curl http://169.254.169.254/latest/meta-data/iam/security-credentials/`
2. **ECS**: Verify `taskRoleArn` in task definition
3. **Local**: Set environment variables or configure `~/.aws/credentials`

### Error: "Access Denied" when uploading to S3

**Cause**: IAM role lacks required S3 permissions

**Solution**: Add `s3:PutObject` permission to IAM role policy:

```bash
# Get existing policy
aws iam get-role-policy --role-name gydi-microservices-role --policy-name S3AccessPolicy

# Update policy with s3:PutObject permission
aws iam put-role-policy \
  --role-name gydi-microservices-role \
  --policy-name S3AccessPolicy \
  --policy-document file://updated-s3-policy.json
```

### Error: "The security token included in the request is invalid"

**Cause**: Temporary credentials expired (shouldn't happen automatically)

**Solution**: Restart application. AWS SDK automatically refreshes credentials.

---

## Security Best Practices

### 1. Use IAM Roles in Production

✅ **DO**: Use IAM roles for EC2, ECS, Lambda
❌ **DON'T**: Use access keys in production

### 2. Least Privilege Principle

Only grant permissions your application needs:

```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:GetObject",
    "s3:DeleteObject"
  ],
  "Resource": "arn:aws:s3:::gydi-property-images/*"
}
```

❌ **DON'T** use wildcard permissions:
```json
{
  "Effect": "Allow",
  "Action": "s3:*",
  "Resource": "*"
}
```

### 3. Separate Roles by Environment

Create separate IAM roles for dev, staging, and production:

- `gydi-microservices-dev-role` → dev S3 bucket
- `gydi-microservices-staging-role` → staging S3 bucket
- `gydi-microservices-prod-role` → production S3 bucket

### 4. Monitor IAM Role Usage

Enable AWS CloudTrail to audit all actions:

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceName,AttributeValue=gydi-property-images \
  --max-results 50
```

### 5. Rotate Access Keys (If Used for Development)

```bash
# List access keys
aws iam list-access-keys --user-name your-user

# Create new key
aws iam create-access-key --user-name your-user

# Delete old key
aws iam delete-access-key --user-name your-user --access-key-id OLD_KEY_ID
```

---

## Migration Checklist

- [ ] Create IAM role with S3 and SES permissions
- [ ] Attach IAM role to EC2 instance / ECS task
- [ ] Remove `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` from environment variables
- [ ] Update `application-prod.yml` (already done by migration)
- [ ] Deploy updated application
- [ ] Test file upload functionality
- [ ] Verify logs show "using IAM credentials"
- [ ] Monitor CloudWatch for any authentication errors
- [ ] Delete old access keys from IAM user (if no longer needed)

---

## Additional Resources

- [AWS IAM Roles Documentation](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html)
- [AWS SDK Default Credentials Provider Chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [EC2 Instance Profiles](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html)

---

**Last Updated**: 2025-11-11
**Version**: 1.0
**Security Level**: HIGH PRIORITY IMPLEMENTED ✅