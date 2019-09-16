/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.service.validator.ais.account.common;

import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.*;

@Component
@RequiredArgsConstructor
public class AccountConsentValidator {
    private final RequestProviderService requestProviderService;

    public ValidationResult validate(AccountConsent accountConsent, String requestUri) {
        if (LocalDate.now().compareTo(accountConsent.getValidUntil()) > 0) {
            return ValidationResult.invalid(AIS_401, of(CONSENT_EXPIRED));
        }

        ConsentStatus consentStatus = accountConsent.getConsentStatus();
        if (consentStatus != ConsentStatus.VALID) {
            return processConsentInvalidStatus(consentStatus);
        }

        if (isAccessExceeded(accountConsent, requestUri)) {
            return ValidationResult.invalid(AIS_429, of(ACCESS_EXCEEDED));
        }

        return ValidationResult.valid();
    }

    private ValidationResult processConsentInvalidStatus(ConsentStatus consentStatus) {
        if (consentStatus == ConsentStatus.REVOKED_BY_PSU) {
            return ValidationResult.invalid(AIS_401, of(CONSENT_INVALID_REVOKED));
        }
        MessageErrorCode messageErrorCode = consentStatus == ConsentStatus.RECEIVED ? CONSENT_INVALID : CONSENT_EXPIRED;
        return ValidationResult.invalid(AIS_401, of(messageErrorCode));
    }

    private boolean isAccessExceeded(AccountConsent accountConsent, String requestUri) {
        if (requestProviderService.isRequestFromPsu() && !accountConsent.isOneAccessType()) {
            return false;
        }

        if (!accountConsent.getUsageCounterMap().containsKey(requestUri)) {
            return false;
        }
        return accountConsent.getUsageCounterMap().get(requestUri) <= 0;
    }
}
