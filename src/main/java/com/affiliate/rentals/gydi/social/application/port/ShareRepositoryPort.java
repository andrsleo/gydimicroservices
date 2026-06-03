package com.affiliate.rentals.gydi.social.application.port;

import com.affiliate.rentals.gydi.social.domain.model.Share;

public interface ShareRepositoryPort {
    Share save(Share share);
}
