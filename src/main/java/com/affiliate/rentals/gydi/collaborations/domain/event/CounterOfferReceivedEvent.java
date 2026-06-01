package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a counter-offer is sent during negotiation.
 *
 * @param pitchId        pitch being negotiated
 * @param counterOfferId the new counter-offer record
 * @param roundNumber    which round (1-3)
 * @param offeredBy      HOST or CREATOR
 * @param recipientId    ID of the party who must now respond
 */
public record CounterOfferReceivedEvent(Long pitchId, Long counterOfferId, int roundNumber,
                                         String offeredBy, Long recipientId) {}
