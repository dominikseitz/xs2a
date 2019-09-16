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

package de.adorsys.psd2.xs2a.service.validator.tpp;

import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import org.springframework.stereotype.Component;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_UNKNOWN_403;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.AIS_403;

@Component
public class AisConsentTppInfoValidator extends TppInfoValidator {
    public AisConsentTppInfoValidator(TppInfoCheckerService tppInfoCheckerService, RequestProviderService requestProviderService) {
        super(tppInfoCheckerService, requestProviderService);
    }

    @Override
    ErrorType getErrorType() {
        return AIS_403;
    }

    @Override
    TppMessageInformation getTppMessageInformation() {
        return TppMessageInformation.of(CONSENT_UNKNOWN_403, TPP_ERROR_MESSAGE);//TODO remove TppInfoValidator.TPP_ERROR_MESSAGE
    }
}
