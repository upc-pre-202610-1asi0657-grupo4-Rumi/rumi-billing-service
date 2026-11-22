package com.rumi.billing.application;

import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;

/**
 * A subscription with its status as of the time it was read: an active one past its expiry reads as expired.
 */
public record SubscriptionDetails(Subscription subscription, SubscriptionStatus status) {
}
