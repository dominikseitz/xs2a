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
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.authorisation.AuthorisationResponseType;
import de.adorsys.psd2.xs2a.domain.authorisation.CancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisCancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aPaymentCancellationAuthorisationSubResource;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationServiceResolver;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.pis.CommonPaymentObject;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.cancellation.GetPaymentCancellationAuthorisationScaStatusValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.cancellation.GetPaymentCancellationAuthorisationsValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.cancellation.UpdatePisCancellationPsuDataPO;
import de.adorsys.psd2.xs2a.service.validator.pis.authorisation.cancellation.UpdatePisCancellationPsuDataValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PaymentCancellationAuthorisationServiceTest {
    private static final String CORRECT_PSU_ID = "123456789";
    private static final String PAYMENT_ID = "594ef79c-d785-41ec-9b14-2ea3a7ae2c7b";
    private static final String WRONG_PAYMENT_ID = "wrong payment id";
    private static final String NOT_EXISTING_PAYMENT_ID = "not existing payment id";
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String INVALID_AUTHORISATION_ID = "invalid authorisation id";
    private static final PsuIdData PSU_ID_DATA = new PsuIdData(CORRECT_PSU_ID, null, null, null);
    private static final String WRONG_CANCELLATION_AUTHORISATION_ID = "wrong cancellation authorisation id";
    private static final String CANCELLATION_AUTHORISATION_ID = "dd5d766f-eeb7-4efe-b730-24d5ed53f537";

    private static final PisCommonPaymentResponse PIS_COMMON_PAYMENT_RESPONSE = buildPisCommonPaymentResponse();
    private static final PisCommonPaymentResponse INVALID_PIS_COMMON_PAYMENT_RESPONSE = buildInvalidPisCommonPaymentResponse();

    private static final MessageError VALIDATION_ERROR = new MessageError(ErrorType.PIS_401, TppMessageInformation.of(UNAUTHORIZED));
    private static final MessageError AUTHORISATION_SERVICE_ERROR = new MessageError(ErrorType.PIS_404, TppMessageInformation.of(RESOURCE_UNKNOWN_404));
    private static final MessageError UNKNOWN_PAYMENT_ERROR = new MessageError(ErrorType.PIS_404, TppMessageInformation.of(RESOURCE_UNKNOWN_404_NO_PAYMENT));

    @InjectMocks
    private PaymentCancellationAuthorisationServiceImpl paymentCancellationAuthorisationService;

    @Mock
    private Xs2aEventService xs2aEventService;
    @Mock
    private PisScaAuthorisationService pisScaAuthorisationService;
    @Mock
    private PisScaAuthorisationServiceResolver pisScaAuthorisationServiceResolver;
    @Mock
    private Xs2aPisCommonPaymentService xs2aPisCommonPaymentService;
    @Mock
    private UpdatePisCancellationPsuDataValidator updatePisCancellationPsuDataValidator;
    @Mock
    private GetPaymentCancellationAuthorisationsValidator getPaymentCancellationAuthorisationsValidator;
    @Mock
    private GetPaymentCancellationAuthorisationScaStatusValidator getPaymentCancellationAuthorisationScaStatusValidator;
    @Mock
    private RequestProviderService requestProviderService;

    @Before
    public void setUp() {
        when(pisScaAuthorisationService.getCancellationAuthorisationScaStatus(PAYMENT_ID, CANCELLATION_AUTHORISATION_ID))
            .thenReturn(Optional.of(ScaStatus.RECEIVED));
        when(pisScaAuthorisationService.getCancellationAuthorisationScaStatus(PAYMENT_ID, WRONG_CANCELLATION_AUTHORISATION_ID))
            .thenReturn(Optional.empty());
        when(pisScaAuthorisationServiceResolver.getService())
            .thenReturn(pisScaAuthorisationService);
        when(pisScaAuthorisationServiceResolver.getServiceCancellation(CANCELLATION_AUTHORISATION_ID))
            .thenReturn(pisScaAuthorisationService);

        when(xs2aPisCommonPaymentService.getPisCommonPaymentById(PAYMENT_ID))
            .thenReturn(Optional.of(PIS_COMMON_PAYMENT_RESPONSE));
        when(xs2aPisCommonPaymentService.getPisCommonPaymentById(WRONG_PAYMENT_ID))
            .thenReturn(Optional.of(INVALID_PIS_COMMON_PAYMENT_RESPONSE));
        when(xs2aPisCommonPaymentService.getPisCommonPaymentById(NOT_EXISTING_PAYMENT_ID))
            .thenReturn(Optional.empty());

        when(updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(buildPisCommonPaymentResponse(), CANCELLATION_AUTHORISATION_ID)))
            .thenReturn(ValidationResult.valid());
        when(getPaymentCancellationAuthorisationsValidator.validate(new CommonPaymentObject(buildPisCommonPaymentResponse())))
            .thenReturn(ValidationResult.valid());
        when(getPaymentCancellationAuthorisationScaStatusValidator.validate(new CommonPaymentObject(buildPisCommonPaymentResponse())))
            .thenReturn(ValidationResult.valid());
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void createPisCancellationAuthorisation_Success_ShouldRecordEvent() {
        when(pisScaAuthorisationService.createCommonPaymentCancellationAuthorisation(anyString(), any(), any()))
            .thenReturn(Optional.of(new Xs2aCreatePisCancellationAuthorisationResponse(CANCELLATION_AUTHORISATION_ID, null, null)));

        // Given:
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, PaymentType.SINGLE.getValue(), null));

        // Then
        verify(xs2aEventService, times(1)).recordPisTppRequest(eq(PAYMENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.START_PAYMENT_CANCELLATION_AUTHORISATION_REQUEST_RECEIVED);
    }

    @Test
    public void createPisCancellationAuthorisation_success() {
        // Given
        ScaStatus scaStatus = ScaStatus.RECEIVED;
        PaymentType paymentType = PaymentType.SINGLE;

        when(pisScaAuthorisationService.createCommonPaymentCancellationAuthorisation(PAYMENT_ID, paymentType, PSU_ID_DATA))
            .thenReturn(Optional.of(new Xs2aCreatePisCancellationAuthorisationResponse(CANCELLATION_AUTHORISATION_ID, scaStatus, paymentType)));

        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, paymentType.getValue(), null));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isFalse();

        CancellationAuthorisationResponse responseBody = pisCancellationAuthorisation.getBody();
        assertThat(responseBody.getAuthorisationResponseType()).isEqualTo(AuthorisationResponseType.START);
        assertThat(responseBody.getCancellationId()).isEqualTo(CANCELLATION_AUTHORISATION_ID);
        assertThat(responseBody.getScaStatus()).isEqualTo(scaStatus);

        Xs2aCreatePisCancellationAuthorisationResponse concreteResponseBody = (Xs2aCreatePisCancellationAuthorisationResponse) responseBody;
        assertThat(concreteResponseBody.getPaymentType()).isEqualTo(paymentType);
    }

    @Test
    public void createPisCancellationAuthorisation_withNotExistingPaymentId_shouldReturnError() {
        // Given
        PaymentType paymentType = PaymentType.SINGLE;

        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(NOT_EXISTING_PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, paymentType.getValue(), null));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isTrue();
        assertThat(pisCancellationAuthorisation.getError()).isEqualTo(UNKNOWN_PAYMENT_ERROR);

        verify(pisScaAuthorisationServiceResolver, never()).getServiceCancellation(anyString());
        verify(pisScaAuthorisationService, never()).createCommonPaymentCancellationAuthorisation(anyString(), any(PaymentType.class), any(PsuIdData.class));
    }

    @Test
    public void createPisCancellationAuthorisation_withUpdatePsuData_success() {
        // Given
        when(pisScaAuthorisationService.updateCommonPaymentCancellationPsuData(any()))
            .thenReturn(new Xs2aUpdatePisCommonPaymentPsuDataResponse(ScaStatus.RECEIVED, PAYMENT_ID, CANCELLATION_AUTHORISATION_ID, PSU_ID_DATA));

        ScaStatus scaStatus = ScaStatus.RECEIVED;
        PaymentType paymentType = PaymentType.SINGLE;

        when(pisScaAuthorisationService.createCommonPaymentCancellationAuthorisation(anyString(), any(), any()))
            .thenReturn(Optional.of(new Xs2aCreatePisCancellationAuthorisationResponse(CANCELLATION_AUTHORISATION_ID, scaStatus, paymentType)));

        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, PaymentType.SINGLE.getValue(), "123"));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isFalse();

        CancellationAuthorisationResponse responseBody = pisCancellationAuthorisation.getBody();
        assertThat(responseBody.getAuthorisationResponseType()).isEqualTo(AuthorisationResponseType.UPDATE);
        assertThat(responseBody.getCancellationId()).isEqualTo(CANCELLATION_AUTHORISATION_ID);
        assertThat(responseBody.getScaStatus()).isEqualTo(scaStatus);

        Xs2aUpdatePisCommonPaymentPsuDataResponse concreteResponseBody = (Xs2aUpdatePisCommonPaymentPsuDataResponse) responseBody;
        assertThat(concreteResponseBody.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(concreteResponseBody.getAuthorisationId()).isEqualTo(CANCELLATION_AUTHORISATION_ID);
        assertThat(concreteResponseBody.getScaStatus()).isEqualTo(scaStatus);
        assertThat(concreteResponseBody.getPsuData()).isEqualTo(PSU_ID_DATA);
    }

    @Test
    public void createPisCancellationAuthorisation_withUpdatePsuDataAndNotExistingPaymentId_shouldReturnError() {
        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(NOT_EXISTING_PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, PaymentType.SINGLE.getValue(), "123"));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isTrue();
        assertThat(pisCancellationAuthorisation.getError()).isEqualTo(UNKNOWN_PAYMENT_ERROR);

        verify(pisScaAuthorisationServiceResolver, never()).getServiceCancellation(anyString());
        verify(pisScaAuthorisationService, never()).updateCommonPaymentCancellationPsuData(any(Xs2aUpdatePisCommonPaymentPsuDataRequest.class));
    }

    @Test
    public void createPisCancellationAuthorisation_withUpdatePsuDataAndUpdateValidationError_shouldReturnError() {
        // Given
        when(updatePisCancellationPsuDataValidator.validate(any(UpdatePisCancellationPsuDataPO.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        ScaStatus scaStatus = ScaStatus.RECEIVED;
        PaymentType paymentType = PaymentType.SINGLE;

        when(pisScaAuthorisationService.createCommonPaymentCancellationAuthorisation(anyString(), any(), any()))
            .thenReturn(Optional.of(new Xs2aCreatePisCancellationAuthorisationResponse(CANCELLATION_AUTHORISATION_ID, scaStatus, paymentType)));

        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, PaymentType.SINGLE.getValue(), "123"));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isTrue();
        assertThat(pisCancellationAuthorisation.getError()).isEqualTo(VALIDATION_ERROR);

        verify(pisScaAuthorisationServiceResolver, never()).getServiceCancellation(anyString());
        verify(pisScaAuthorisationService, never()).updateCommonPaymentCancellationPsuData(any(Xs2aUpdatePisCommonPaymentPsuDataRequest.class));
    }

    @Test
    public void createPisCancellationAuthorisation_withUpdatePsuDataAndAuthorisationServiceError_shouldReturnError() {
        // Given
        when(pisScaAuthorisationService.updateCommonPaymentCancellationPsuData(any()))
            .thenReturn(new Xs2aUpdatePisCommonPaymentPsuDataResponse(ScaStatus.RECEIVED, PAYMENT_ID, CANCELLATION_AUTHORISATION_ID, PSU_ID_DATA));
        ErrorHolder errorHolder = ErrorHolder.builder(AUTHORISATION_SERVICE_ERROR.getErrorType())
                                      .tppMessages(AUTHORISATION_SERVICE_ERROR.getTppMessage())
                                      .build();
        when(pisScaAuthorisationService.updateCommonPaymentCancellationPsuData(any(Xs2aUpdatePisCommonPaymentPsuDataRequest.class)))
            .thenReturn(new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, PAYMENT_ID, CANCELLATION_AUTHORISATION_ID, PSU_ID_DATA));

        ScaStatus scaStatus = ScaStatus.RECEIVED;
        PaymentType paymentType = PaymentType.SINGLE;

        when(pisScaAuthorisationService.createCommonPaymentCancellationAuthorisation(anyString(), any(), any()))
            .thenReturn(Optional.of(new Xs2aCreatePisCancellationAuthorisationResponse(CANCELLATION_AUTHORISATION_ID, scaStatus, paymentType)));

        // When
        ResponseObject<CancellationAuthorisationResponse> pisCancellationAuthorisation = paymentCancellationAuthorisationService.createPisCancellationAuthorisation(new Xs2aCreatePisAuthorisationRequest(PAYMENT_ID, PSU_ID_DATA, PAYMENT_PRODUCT, PaymentType.SINGLE.getValue(), "123"));

        // Then
        assertThat(pisCancellationAuthorisation.hasError()).isTrue();
        assertThat(pisCancellationAuthorisation.getError()).isEqualTo(AUTHORISATION_SERVICE_ERROR);
    }

    @Test
    public void updatePisCancellationPsuData_Success_ShouldRecordEvent() {
        when(pisScaAuthorisationService.updateCommonPaymentCancellationPsuData(any()))
            .thenReturn(new Xs2aUpdatePisCommonPaymentPsuDataResponse(ScaStatus.RECEIVED, PAYMENT_ID, CANCELLATION_AUTHORISATION_ID, PSU_ID_DATA));

        // Given:
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = buildXs2aUpdatePisPsuDataRequest();
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        paymentCancellationAuthorisationService.updatePisCancellationPsuData(request);

        // Then
        verify(xs2aEventService, times(1)).recordPisTppRequest(eq(PAYMENT_ID), argumentCaptor.capture(), any());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.UPDATE_PAYMENT_CANCELLATION_PSU_DATA_REQUEST_RECEIVED);
    }

    @Test
    public void updatePisCancellationPsuData_withInvalidPayment_shouldReturnValidationError() {
        // Given
        Xs2aUpdatePisCommonPaymentPsuDataRequest invalidUpdatePisPsuDataRequest = buildInvalidXs2aUpdatePisPsuDataRequest();
        when(updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(INVALID_PIS_COMMON_PAYMENT_RESPONSE, INVALID_AUTHORISATION_ID)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> actualResponse =
            paymentCancellationAuthorisationService.updatePisCancellationPsuData(invalidUpdatePisPsuDataRequest);

        // Then
        verify(updatePisCancellationPsuDataValidator).validate(new UpdatePisCancellationPsuDataPO(INVALID_PIS_COMMON_PAYMENT_RESPONSE, INVALID_AUTHORISATION_ID));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getPaymentInitiationCancellationAuthorisationInformation_Success_ShouldRecordEvent() {
        // Given:
        when(pisScaAuthorisationService.getCancellationAuthorisationSubResources(anyString()))
            .thenReturn(Optional.of(new Xs2aPaymentCancellationAuthorisationSubResource(Collections.emptyList())));
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        paymentCancellationAuthorisationService.getPaymentInitiationCancellationAuthorisationInformation(PAYMENT_ID);

        // Then
        verify(xs2aEventService, times(1)).recordPisTppRequest(eq(PAYMENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.GET_PAYMENT_CANCELLATION_AUTHORISATION_REQUEST_RECEIVED);
    }

    @Test
    public void getPaymentInitiationCancellationAuthorisationInformation_withInvalidPayment_shouldReturnValidationError() {
        // Given:
        when(getPaymentCancellationAuthorisationsValidator.validate(new CommonPaymentObject(INVALID_PIS_COMMON_PAYMENT_RESPONSE)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aPaymentCancellationAuthorisationSubResource> actualResponse =
            paymentCancellationAuthorisationService.getPaymentInitiationCancellationAuthorisationInformation(WRONG_PAYMENT_ID);

        // Then
        verify(getPaymentCancellationAuthorisationsValidator).validate(new CommonPaymentObject(INVALID_PIS_COMMON_PAYMENT_RESPONSE));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getPaymentCancellationAuthorisationScaStatus_success() {
        // Given
        when(pisScaAuthorisationServiceResolver.getServiceCancellation(CANCELLATION_AUTHORISATION_ID))
            .thenReturn(pisScaAuthorisationService);

        // When
        ResponseObject<ScaStatus> actual =
            paymentCancellationAuthorisationService.getPaymentCancellationAuthorisationScaStatus(PAYMENT_ID,
                                                                                                 CANCELLATION_AUTHORISATION_ID);

        // Then
        assertFalse(actual.hasError());
        assertEquals(ScaStatus.RECEIVED, actual.getBody());
    }

    @Test
    public void getPaymentCancellationAuthorisationScaStatus_success_shouldRecordEvent() {
        // Given:
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);
        when(pisScaAuthorisationServiceResolver.getServiceCancellation(CANCELLATION_AUTHORISATION_ID))
            .thenReturn(pisScaAuthorisationService);

        // When
        paymentCancellationAuthorisationService.getPaymentCancellationAuthorisationScaStatus(PAYMENT_ID,
                                                                                             CANCELLATION_AUTHORISATION_ID);

        // Then
        verify(xs2aEventService, times(1))
            .recordPisTppRequest(eq(PAYMENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.GET_PAYMENT_CANCELLATION_SCA_STATUS_REQUEST_RECEIVED);
    }

    @Test
    public void getPaymentCancellationAuthorisationScaStatus_failure_wrongIds() {
        // Given
        when(pisScaAuthorisationServiceResolver.getServiceCancellation(WRONG_CANCELLATION_AUTHORISATION_ID))
            .thenReturn(pisScaAuthorisationService);

        // When
        ResponseObject<ScaStatus> actual =
            paymentCancellationAuthorisationService.getPaymentCancellationAuthorisationScaStatus(PAYMENT_ID,
                                                                                                 WRONG_CANCELLATION_AUTHORISATION_ID);

        // Then
        verify(pisScaAuthorisationService).getCancellationAuthorisationScaStatus(PAYMENT_ID, WRONG_CANCELLATION_AUTHORISATION_ID);
        assertTrue(actual.hasError());
        assertNull(actual.getBody());
    }

    @Test
    public void getPaymentCancellationAuthorisationScaStatus_withInvalidPayment_shouldReturnValidationError() {
        // Given
        when(getPaymentCancellationAuthorisationScaStatusValidator.validate(new CommonPaymentObject(INVALID_PIS_COMMON_PAYMENT_RESPONSE)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<ScaStatus> actualResponse =
            paymentCancellationAuthorisationService.getPaymentCancellationAuthorisationScaStatus(WRONG_PAYMENT_ID,
                                                                                                 WRONG_CANCELLATION_AUTHORISATION_ID);

        // Then
        verify(getPaymentCancellationAuthorisationScaStatusValidator).validate(new CommonPaymentObject(INVALID_PIS_COMMON_PAYMENT_RESPONSE));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    private static PisCommonPaymentResponse buildPisCommonPaymentResponse() {
        PisCommonPaymentResponse response = new PisCommonPaymentResponse();
        response.setTransactionStatus(TransactionStatus.RCVD);
        return response;
    }

    private static PisCommonPaymentResponse buildInvalidPisCommonPaymentResponse() {
        PisCommonPaymentResponse response = new PisCommonPaymentResponse();
        response.setTppInfo(new TppInfo());
        return response;
    }

    private Xs2aUpdatePisCommonPaymentPsuDataRequest buildXs2aUpdatePisPsuDataRequest() {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setAuthorisationId(CANCELLATION_AUTHORISATION_ID);
        request.setPaymentId(PAYMENT_ID);
        return request;
    }

    private Xs2aUpdatePisCommonPaymentPsuDataRequest buildInvalidXs2aUpdatePisPsuDataRequest() {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setAuthorisationId(INVALID_AUTHORISATION_ID);
        request.setPaymentId(WRONG_PAYMENT_ID);
        return request;
    }
}
