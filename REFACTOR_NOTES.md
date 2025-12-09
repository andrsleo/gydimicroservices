# Commission Refactoring Notes

## Changes Made (V48 Migration)

### Database Changes
1. ✅ Dropped `fraud_alerts` table completely
2. ✅ Refactored `commission_ledger` → `commission_booking`
3. ✅ Removed columns: `referral_link_id`, `affiliate_id`, `property_id`
4. ✅ Added column: `booking_id` (FK to `referrals.booking`)
5. ✅ Updated functions, triggers, and indexes

### Java Code Changes
1. ✅ Updated `Commission.java` domain model - now uses `bookingId`
2. ✅ Updated `CommissionJpaEntity.java` - mapped to `commission_booking` table
3. ✅ Updated `CommissionDto.java` - simplified fields
4. ✅ Updated `CommissionRepository.java` (port) - simplified methods
5. ✅ Updated `CommissionRepositoryAdapter.java` - simplified methods
6. ✅ Updated `CommissionJpaRepository.java` - simplified queries
7. ✅ Deleted `FraudAlertStatus.java` and `FraudSeverity.java`

## Use Cases Requiring Refactoring

The following use cases need to be refactored to work with the new schema:

### 1. GetEarningsUseCase.java
**Issues:**
- Uses `findByAffiliateId()` - no longer exists
- Uses `calculateTotalEarnings(affiliateId)` - no longer exists
- Uses `calculateEarningsByStatus(affiliateId, status)` - no longer exists  
- Uses `countByAffiliateIdAndStatus()` - no longer exists
- Uses `calculateMonthlyEarnings()` - no longer exists
- Accesses `getReferralLinkId()`, `getAffiliateId()`, `getPropertyId()` - no longer exist

**Solution:**
- Need to join with `booking` table to get `affiliateId` from `referral_link`
- May need to create new queries or use denormalized data from `referral_links` table
- Consider using the existing denormalized counters on `referral_links` table

### 2. RegisterConversionUseCase.java
**Issues:**
- Creates commission with old signature: `create(referralLinkId, affiliateId, propertyId, ...)`
- New signature is: `create(bookingId, commissionAmount, commissionRate, affiliatePlan)`

**Solution:**
- This use case should work with bookings now
- When a booking is created/confirmed, create the commission record with `booking_id`

### 3. GetReferralStatsUseCase.java
**Issues:**
- Uses `calculateTotalEarnings(affiliateId)`
- Uses `calculateEarningsByStatus(affiliateId, status)`
- Uses `calculateMonthlyEarnings(affiliateId, year)`

**Solution:**
- Similar to GetEarningsUseCase - need to use denormalized data or create new queries

## Recommended Approach

### Option 1: Use Denormalized Data (Recommended)
The `referral_links` table already has:
- `total_commission DECIMAL(12,2)` - total commission earned
- `conversions_count INTEGER` - number of conversions

Use these fields instead of querying `commission_booking` table.

### Option 2: Create New Queries with JOINs
Create queries that join `commission_booking` → `booking` → `referral_link` to get affiliate info.

Example:
```sql
SELECT SUM(cb.commission_amount)
FROM referrals.commission_booking cb
JOIN referrals.booking b ON b.booking_id = cb.booking_id
JOIN referrals.referral_links rl ON rl.id = b.referral_link_id
WHERE rl.affiliate_id = ?
```

## Next Steps

1. ✅ Migration applied and Java models updated
2. ⚠️ Use cases need refactoring (currently commented out for compilation)
3. TODO: Decide on approach (denormalized vs JOINs)
4. TODO: Refactor or remove affected use cases
5. TODO: Update tests
6. TODO: Update API controllers if needed

