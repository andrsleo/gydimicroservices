package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PlanEntity;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between Plan domain model and PlanEntity.
 *
 * <p>This mapper handles the bidirectional conversion between the domain layer's
 * Plan aggregate and the infrastructure layer's PlanEntity.</p>
 *
 * @author GYDI Development Team
 * @see Plan
 * @see PlanEntity
 */
@Component
public class PlanEntityMapper {

    /**
     * Converts a Plan domain model to a PlanEntity.
     *
     * @param plan the domain plan to convert
     * @return the corresponding PlanEntity
     */
    public PlanEntity toEntity(Plan plan) {
        if (plan == null) {
            return null;
        }

        PlanEntity entity = new PlanEntity();
        entity.setId(plan.id());
        entity.setPlanCode(plan.planCode());
        entity.setPlanName(plan.planName());
        entity.setPlanDescription(plan.description());
        entity.setMonthlyPrice(plan.monthlyPrice());
        entity.setCurrency(plan.currency());
        entity.setCommissionRate(plan.commissionRate());
        entity.setReferralLimitPerMonth(plan.referralLimitPerMonth());
        entity.setPropertyPublishLimit(plan.propertyPublishLimit());
        entity.setIsActive(plan.isActive());
        entity.setIsFeatured(plan.isFeatured());
        entity.setDisplayOrder(plan.displayOrder());
        entity.setStripePriceId(plan.stripePriceId());
        entity.setCreatedAt(plan.createdAt());
        entity.setUpdatedAt(plan.updatedAt());

        return entity;
    }

    /**
     * Converts a PlanEntity to a Plan domain model.
     *
     * @param entity the PlanEntity to convert
     * @return the corresponding Plan domain model
     */
    public Plan toDomain(PlanEntity entity) {
        if (entity == null) {
            return null;
        }

        return Plan.builder()
                .id(entity.getId())
                .planCode(entity.getPlanCode())
                .planName(entity.getPlanName())
                .description(entity.getPlanDescription())
                .monthlyPrice(entity.getMonthlyPrice())
                .currency(entity.getCurrency())
                .commissionRate(entity.getCommissionRate())
                .referralLimitPerMonth(entity.getReferralLimitPerMonth())
                .propertyPublishLimit(entity.getPropertyPublishLimit())
                .isActive(entity.getIsActive())
                .isFeatured(entity.getIsFeatured())
                .displayOrder(entity.getDisplayOrder())
                .stripePriceId(entity.getStripePriceId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
