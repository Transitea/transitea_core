package com.transitea.service;

import com.transitea.dto.response.SuiviPublicReponse;

public interface TrackingService {
    SuiviPublicReponse suivreParCode(String codeTracking);
}
