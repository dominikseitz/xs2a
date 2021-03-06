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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation;

import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.authorisation.AuthorisationStatusValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class PisAuthorisationStatusValidator extends AuthorisationStatusValidator {
    public PisAuthorisationStatusValidator(RequestProviderService requestProviderService) {
        super(requestProviderService);
    }

    @Override
    protected @NotNull ErrorType getErrorType() {
        return ErrorType.PIS_409;
    }
}
