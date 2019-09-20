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

package de.adorsys.psd2.xs2a.service.validator.ais.consent;

import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Validator to be used for validating get consent authorisation sca status request according to some business rules
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetConsentAuthorisationScaStatusValidator extends AbstractConsentTppValidator<GetConsentAuthorisationScaStatusPO> {
    private final AisAuthorisationValidator aisAuthorisationValidator;

    /**
     * Validates get consent authorisation sca status request
     *
     * @param consentObject consent information object
     * @return valid result if the consent is valid, invalid result with appropriate error otherwise
     */
    @NotNull
    @Override
    protected ValidationResult executeBusinessValidation(GetConsentAuthorisationScaStatusPO consentObject) {
        AccountConsent response = consentObject.getAccountConsent();
        String authorisationId = consentObject.getAuthorisationId();

        ValidationResult authorisationValidationResult = aisAuthorisationValidator.validate(authorisationId, response);
        if (authorisationValidationResult.isNotValid()) {
            return authorisationValidationResult;
        }

        return ValidationResult.valid();
    }
}
