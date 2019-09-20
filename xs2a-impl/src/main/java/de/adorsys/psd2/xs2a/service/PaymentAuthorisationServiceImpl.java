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

package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.event.core.model.EventType;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.authorisation.AuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthorisationSubResources;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationServiceResolver;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.pis.CommonPaymentObject;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.initiation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuthorisationServiceImpl implements PaymentAuthorisationService {
    private static final String PAYMENT_NOT_FOUND_MESSAGE = "Payment not found";

    private final Xs2aEventService xs2aEventService;
    private final PisScaAuthorisationServiceResolver pisScaAuthorisationServiceResolver;
    private final Xs2aPisCommonPaymentService pisCommonPaymentService;
    private final CreatePisAuthorisationValidator createPisAuthorisationValidator;
    private final UpdatePisCommonPaymentPsuDataValidator updatePisCommonPaymentPsuDataValidator;
    private final GetPaymentInitiationAuthorisationsValidator getPaymentAuthorisationsValidator;
    private final GetPaymentInitiationAuthorisationScaStatusValidator getPaymentAuthorisationScaStatusValidator;
    private final RequestProviderService requestProviderService;

    /**
     * Creates pis authorisation for payment. In case when psu data and password came then second step will be update psu data in created authorisation
     *
     * @param createRequest data container to create pis authorisation
     * @return ResponseObject it could contains data after create or update pis authorisation
     */
    @Override
    public ResponseObject<AuthorisationResponse> createPisAuthorisation(Xs2aCreatePisAuthorisationRequest createRequest) {
        ResponseObject<Xs2aCreatePisAuthorisationResponse> createPisAuthorisationResponse = createPisAuthorisation(createRequest.getPaymentId(), createRequest.getPaymentService(), createRequest.getPsuData());

        if (createPisAuthorisationResponse.hasError()) {
            return ResponseObject.<AuthorisationResponse>builder()
                       .fail(createPisAuthorisationResponse.getError())
                       .build();
        }

        if (createRequest.getPsuData().isEmpty()
                || StringUtils.isBlank(createRequest.getPassword())) {
            return ResponseObject.<AuthorisationResponse>builder()
                       .body(createPisAuthorisationResponse.getBody())
                       .build();
        }

        String authorisationId = createPisAuthorisationResponse.getBody().getAuthorisationId();
        Xs2aUpdatePisCommonPaymentPsuDataRequest updateRequest = new Xs2aUpdatePisCommonPaymentPsuDataRequest(createRequest, authorisationId);
        ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> updatePsuDataResponse = updatePisCommonPaymentPsuData(updateRequest);

        if (updatePsuDataResponse.hasError()) {
            return ResponseObject.<AuthorisationResponse>builder()
                       .fail(updatePsuDataResponse.getError())
                       .build();
        }

        return ResponseObject.<AuthorisationResponse>builder()
                   .body(updatePsuDataResponse.getBody())
                   .build();
    }

    /**
     * Update psu data for payment request if psu data and password are valid
     *
     * @param request update psu data request, which contains paymentId, authorisationId, psuData, password, authenticationMethodId, scaStatus, paymentService and scaAuthenticationData
     * @return Xs2aUpdatePisCommonPaymentPsuDataResponse that contains authorisationId, scaStatus, psuId and related links in case of success
     */
    @Override
    public ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> updatePisCommonPaymentPsuData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        xs2aEventService.recordPisTppRequest(request.getPaymentId(), EventType.UPDATE_PAYMENT_AUTHORISATION_PSU_DATA_REQUEST_RECEIVED, request);

        Optional<PisCommonPaymentResponse> pisCommonPaymentResponse = pisCommonPaymentService.getPisCommonPaymentById(request.getPaymentId());
        if (!pisCommonPaymentResponse.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Update PIS CommonPayment PSU data failed. PIS CommonPayment not found by id",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), request.getPaymentId());
            return ResponseObject.<Xs2aUpdatePisCommonPaymentPsuDataResponse>builder()
                       .fail(PIS_404, of(RESOURCE_UNKNOWN_404, PAYMENT_NOT_FOUND_MESSAGE))
                       .build();
        }

        ValidationResult validationResult = updatePisCommonPaymentPsuDataValidator.validate(new UpdatePisCommonPaymentPsuDataPO(pisCommonPaymentResponse.get(), request.getAuthorisationId(), request.getPsuData()));
        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Update PIS CommonPayment PSU data - validation failed: {}",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), request.getPaymentId(), validationResult.getMessageError());
            return ResponseObject.<Xs2aUpdatePisCommonPaymentPsuDataResponse>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        PisScaAuthorisationService pisScaAuthorisationService = pisScaAuthorisationServiceResolver.getServiceInitiation(request.getAuthorisationId());
        Xs2aUpdatePisCommonPaymentPsuDataResponse response = pisScaAuthorisationService.updateCommonPaymentPsuData(request);

        if (response.hasError()) {
            return ResponseObject.<Xs2aUpdatePisCommonPaymentPsuDataResponse>builder()
                       .fail(new MessageError(response.getErrorHolder()))
                       .build();
        }
        return ResponseObject.<Xs2aUpdatePisCommonPaymentPsuDataResponse>builder()
                   .body(response)
                   .build();
    }

    /**
     * Gets authorisations for current payment
     *
     * @param paymentId ASPSP identifier of the payment, associated with the authorisation
     * @return Response containing list of authorisations
     */
    @Override
    public ResponseObject<Xs2aAuthorisationSubResources> getPaymentInitiationAuthorisations(String paymentId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.GET_PAYMENT_AUTHORISATION_REQUEST_RECEIVED);

        Optional<PisCommonPaymentResponse> pisCommonPaymentResponse = pisCommonPaymentService.getPisCommonPaymentById(paymentId);
        if (!pisCommonPaymentResponse.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Get Payment authorisation failed. PIS CommonPayment not found by id",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId);
            return ResponseObject.<Xs2aAuthorisationSubResources>builder()
                       .fail(PIS_404, of(RESOURCE_UNKNOWN_404, PAYMENT_NOT_FOUND_MESSAGE))
                       .build();
        }

        ValidationResult validationResult = getPaymentAuthorisationsValidator.validate(new CommonPaymentObject(pisCommonPaymentResponse.get()));
        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Get payment initiation authorisation - validation failed: {}",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId, validationResult.getMessageError());
            return ResponseObject.<Xs2aAuthorisationSubResources>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        PisScaAuthorisationService pisScaAuthorisationService = pisScaAuthorisationServiceResolver.getService();
        return pisScaAuthorisationService.getAuthorisationSubResources(paymentId)
                   .map(resp -> ResponseObject.<Xs2aAuthorisationSubResources>builder().body(resp).build())
                   .orElseGet(() -> {
                       log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Get payment initiation authorisation has failed. Authorisation not found by payment id.",
                                requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId);
                       return ResponseObject.<Xs2aAuthorisationSubResources>builder()
                                  .fail(PIS_404, of(RESOURCE_UNKNOWN_404))
                                  .build();
                   });
    }

    /**
     * Gets SCA status of payment initiation authorisation
     *
     * @param paymentId       ASPSP identifier of the payment, associated with the authorisation
     * @param authorisationId authorisation identifier
     * @return Response containing SCA status of authorisation or corresponding error
     */
    @Override
    public ResponseObject<ScaStatus> getPaymentInitiationAuthorisationScaStatus(String paymentId, String authorisationId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.GET_PAYMENT_SCA_STATUS_REQUEST_RECEIVED);

        Optional<PisCommonPaymentResponse> pisCommonPaymentResponse = pisCommonPaymentService.getPisCommonPaymentById(paymentId);
        if (!pisCommonPaymentResponse.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Get SCA status payment initiation authorisation failed. PIS CommonPayment not found by id",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId);
            return ResponseObject.<ScaStatus>builder()
                       .fail(PIS_404, of(RESOURCE_UNKNOWN_404, PAYMENT_NOT_FOUND_MESSAGE))
                       .build();
        }

        ValidationResult validationResult = getPaymentAuthorisationScaStatusValidator.validate(new GetPaymentInitiationAuthorisationScaStatusPO(pisCommonPaymentResponse.get(), authorisationId));
        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Get SCA status payment initiation authorisation - validation failed: {}",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId, validationResult.getMessageError());
            return ResponseObject.<ScaStatus>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        PisScaAuthorisationService pisScaAuthorisationService = pisScaAuthorisationServiceResolver.getServiceInitiation(authorisationId);
        Optional<ScaStatus> scaStatus = pisScaAuthorisationService.getAuthorisationScaStatus(paymentId, authorisationId);

        if (!scaStatus.isPresent()) {
            return ResponseObject.<ScaStatus>builder()
                       .fail(PIS_403, of(RESOURCE_UNKNOWN_403))
                       .build();
        }

        return ResponseObject.<ScaStatus>builder()
                   .body(scaStatus.get())
                   .build();
    }

    private ResponseObject<Xs2aCreatePisAuthorisationResponse> createPisAuthorisation(String paymentId, String paymentService, PsuIdData psuData) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.START_PAYMENT_AUTHORISATION_REQUEST_RECEIVED);

        Optional<PisCommonPaymentResponse> pisCommonPaymentResponse = pisCommonPaymentService.getPisCommonPaymentById(paymentId);
        if (!pisCommonPaymentResponse.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Create PIS Authorisation failed. PIS CommonPayment not found by id",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId);
            return ResponseObject.<Xs2aCreatePisAuthorisationResponse>builder()
                       .fail(PIS_404, of(RESOURCE_UNKNOWN_404, PAYMENT_NOT_FOUND_MESSAGE))
                       .build();
        }

        ValidationResult validationResult = createPisAuthorisationValidator.validate(new CommonPaymentObject(pisCommonPaymentResponse.get()));
        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. Create PIS Authorisation - validation failed: {}",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId, validationResult.getMessageError());
            return ResponseObject.<Xs2aCreatePisAuthorisationResponse>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        PisScaAuthorisationService pisScaAuthorisationService = pisScaAuthorisationServiceResolver.getService();
        return pisScaAuthorisationService.createCommonPaymentAuthorisation(paymentId, PaymentType.getByValue(paymentService)
                                                                                          .orElseThrow(() -> new IllegalArgumentException("Unsupported payment service")), psuData)
                   .map(resp -> ResponseObject.<Xs2aCreatePisAuthorisationResponse>builder()
                                    .body(resp)
                                    .build())
                   .orElseGet(ResponseObject.<Xs2aCreatePisAuthorisationResponse>builder()
                                  .fail(PIS_400, of(PAYMENT_FAILED))
                                  ::build);
    }
}
