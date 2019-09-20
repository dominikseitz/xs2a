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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation.initiation;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.pis.PaymentAuthorisationType;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.PisEndpointAccessCheckerService;
import de.adorsys.psd2.xs2a.service.validator.PsuDataUpdateAuthorisationChecker;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.pis.AbstractPisTppValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.PisAuthorisationStatusValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.PisAuthorisationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;

/**
 * Validator to be used for validating update PSU Data in payment authorisation request according to some business rules
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdatePisCommonPaymentPsuDataValidator extends AbstractPisTppValidator<UpdatePisCommonPaymentPsuDataPO> {
    private static final String MESSAGE_ERROR_NO_PSU = "Please provide the PSU identification data";

    private final PisEndpointAccessCheckerService pisEndpointAccessCheckerService;
    private final RequestProviderService requestProviderService;
    private final PisAuthorisationValidator pisAuthorisationValidator;
    private final PisAuthorisationStatusValidator pisAuthorisationStatusValidator;
    private final PsuDataUpdateAuthorisationChecker psuDataUpdateAuthorisationChecker;

    /**
     * Validates update PSU Data in payment authorisation request by checking whether:
     * <ul>
     * <li>endpoint is accessible for given authorisation</li>
     * <li>payment is not expired</li>
     * </ul>
     *
     * @param paymentObject payment information object
     * @return valid result if the payment is valid, invalid result with appropriate error otherwise
     */
    @Override
    protected ValidationResult executeBusinessValidation(UpdatePisCommonPaymentPsuDataPO paymentObject) {
        String authorisationId = paymentObject.getAuthorisationId();
        if (!pisEndpointAccessCheckerService.isEndpointAccessible(authorisationId, PaymentAuthorisationType.CREATED)) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Authorisation ID: [{}]. Updating PIS initiation authorisation PSU Data  has failed: endpoint is not accessible for authorisation",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), authorisationId);
            return ValidationResult.invalid(ErrorType.PIS_403, SERVICE_BLOCKED);
        }

        PisCommonPaymentResponse pisCommonPaymentResponse = paymentObject.getPisCommonPaymentResponse();
        // TODO temporary solution: CMS should be refactored to return response objects instead of Strings, Enums, Booleans etc., so we should receive this error from CMS https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/581
        if (pisCommonPaymentResponse.getTransactionStatus() == TransactionStatus.RJCT) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Authorisation ID: [{}]. Updating PIS initiation authorisation PSU Data has failed: payment has been rejected",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), authorisationId);
            return ValidationResult.invalid(ErrorType.PIS_403, RESOURCE_EXPIRED_403);
        }

        ValidationResult authorisationValidationResult = pisAuthorisationValidator.validate(authorisationId, pisCommonPaymentResponse);
        if (authorisationValidationResult.isNotValid()) {
            return authorisationValidationResult;
        }

        Authorisation authorisation = pisCommonPaymentResponse.findAuthorisationInPayment(authorisationId).get();

        if (psuDataUpdateAuthorisationChecker.areBothPsusAbsent(paymentObject.getPsuIdData(), authorisation.getPsuData())) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment ID: [{}], Authorisation ID: [{}]. Updating PIS initiation authorisation PSU Data has failed: PSU from authorisation and PSU from request are absent",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), pisCommonPaymentResponse.getExternalId(), authorisationId);
            return ValidationResult.invalid(new MessageError(ErrorType.PIS_400, of(FORMAT_ERROR, MESSAGE_ERROR_NO_PSU)));
        }

        if (!psuDataUpdateAuthorisationChecker.canPsuUpdateAuthorisation(paymentObject.getPsuIdData(), authorisation.getPsuData())) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment ID: [{}], Authorisation ID: [{}]. Updating PIS initiation authorisation PSU Data has failed: PSU from authorisation and PSU from request are different",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), pisCommonPaymentResponse.getExternalId(), authorisationId);
            return ValidationResult.invalid(new MessageError(ErrorType.PIS_401, of(PSU_CREDENTIALS_INVALID)));
        }

        ValidationResult authorisationStatusValidationResult = pisAuthorisationStatusValidator.validate(authorisation.getScaStatus());
        if (authorisationStatusValidationResult.isNotValid()) {
            return authorisationStatusValidationResult;
        }

        return ValidationResult.valid();
    }
}
