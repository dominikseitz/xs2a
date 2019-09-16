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

package de.adorsys.psd2.xs2a.service.authorization.pis;

import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aDecoupledUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import de.adorsys.psd2.xs2a.spi.service.PaymentCancellationSpi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.SCAMETHODSELECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PisCommonDecoupledServiceTest {
    private static final String PRODUCT = "sepa-credit-transfers";
    private static final String PAYMENT_ID = "c713a32c-15ff-4f90-afa0-34a500359844";
    private static final String AUTHORISATION_ID = "ad746cb3-a01b-4196-a6b9-40b0e4cd2350";
    private static final String AUTHENTICATION_METHOD_ID = "dd5d766f-eeb7-4efe-b730-24d5ed53f537";
    private static final String DECOUPLED_PSU_MESSAGE = "Please use your BankApp for transaction Authorisation";
    private static final SpiContextData SPI_CONTEXT_DATA = getSpiContextData();
    private static final SpiSinglePayment SPI_SINGLE_PAYMENT = new SpiSinglePayment(PRODUCT);
    private static final SpiResponse<SpiAuthorisationDecoupledScaResponse> AUTH_DECOUPLED_RESPONSE = buildSpiResponse();
    private static final SpiResponse<SpiAuthorisationDecoupledScaResponse> AUTH_DECOUPLED_RESPONSE_FAIL = buildSpiResponseFail();
    private static final Xs2aUpdatePisCommonPaymentPsuDataRequest UPDATE_PIS_COMMON_PAYMENT_REQUEST = buildUpdatePisCommonPaymentPsuDataRequest(null);
    private static final Xs2aUpdatePisCommonPaymentPsuDataRequest UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID = buildUpdatePisCommonPaymentPsuDataRequest(AUTHENTICATION_METHOD_ID);
    private static final PsuIdData PSU_DATA = buildPsuIdData();
    private static final Xs2aUpdatePisCommonPaymentPsuDataResponse UPDATE_PIS_COMMON_PAYMENT_RESPONSE = buildUpdatePisCommonPaymentPsuDataResponse(UPDATE_PIS_COMMON_PAYMENT_REQUEST);
    private static final Xs2aUpdatePisCommonPaymentPsuDataResponse UPDATE_PIS_COMMON_PAYMENT_RESPONSE_AUTH_METHOD_ID = buildUpdatePisCommonPaymentPsuDataResponse(UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID);
    private static final ErrorHolder EXPECTED_ERROR = ErrorHolder.builder(ErrorType.PIS_404)
                                                                 .tppMessages(TppMessageInformation.of(MessageErrorCode.RESOURCE_UNKNOWN_404, "Payment not found"))
                                                                 .build();
    @InjectMocks
    private PisCommonDecoupledService pisCommonDecoupledService;

    @Mock
    private PaymentAuthorisationSpi paymentAuthorisationSpi;
    @Mock
    private PaymentCancellationSpi paymentCancellationSpi;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    RequestProviderService requestProviderService;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;

    @Before
    public void init() {
        when(spiContextDataProvider.provideWithPsuIdData(PSU_DATA))
            .thenReturn(SPI_CONTEXT_DATA);
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(PAYMENT_ID)).thenReturn(spiAspspConsentDataProvider);
    }

    @Test
    public void proceedDecoupledInitiation_success() {
        // Given
        when(paymentAuthorisationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, null, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledInitiation(UPDATE_PIS_COMMON_PAYMENT_REQUEST, SPI_SINGLE_PAYMENT);

        // Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse).isEqualTo(UPDATE_PIS_COMMON_PAYMENT_RESPONSE);
    }

    @Test
    public void proceedDecoupledInitiation_failed() {
        // Given
        when(paymentAuthorisationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, null, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE_FAIL);
        when(spiErrorMapper.mapToErrorHolder(AUTH_DECOUPLED_RESPONSE_FAIL, ServiceType.PIS))
            .thenReturn(EXPECTED_ERROR);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledInitiation(UPDATE_PIS_COMMON_PAYMENT_REQUEST, SPI_SINGLE_PAYMENT);

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getErrorHolder()).isEqualToComparingFieldByField(EXPECTED_ERROR);
    }

    @Test
    public void proceedDecoupledInitiation_authenticationMethodId_success() {
        // Given
        when(paymentAuthorisationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledInitiation(UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID, SPI_SINGLE_PAYMENT, AUTHENTICATION_METHOD_ID);

        // Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse).isEqualTo(UPDATE_PIS_COMMON_PAYMENT_RESPONSE_AUTH_METHOD_ID);
    }

    @Test
    public void proceedDecoupledInitiation_authenticationMethodId_failed() {
        // Given
        when(paymentAuthorisationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE_FAIL);
        when(spiErrorMapper.mapToErrorHolder(AUTH_DECOUPLED_RESPONSE_FAIL, ServiceType.PIS))
            .thenReturn(EXPECTED_ERROR);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledInitiation(UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID, SPI_SINGLE_PAYMENT, AUTHENTICATION_METHOD_ID);

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getErrorHolder()).isEqualToComparingFieldByField(EXPECTED_ERROR);
    }

    @Test
    public void proceedDecoupledCancellation_success() {
        // Given
        when(paymentCancellationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, null, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledCancellation(UPDATE_PIS_COMMON_PAYMENT_REQUEST, SPI_SINGLE_PAYMENT);

        // Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse).isEqualTo(UPDATE_PIS_COMMON_PAYMENT_RESPONSE);
    }

    @Test
    public void proceedDecoupledCancellation_failed() {
        // Given
        when(paymentCancellationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, null, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE_FAIL);
        when(spiErrorMapper.mapToErrorHolder(AUTH_DECOUPLED_RESPONSE_FAIL, ServiceType.PIS))
            .thenReturn(EXPECTED_ERROR);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledCancellation(UPDATE_PIS_COMMON_PAYMENT_REQUEST, SPI_SINGLE_PAYMENT);

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getErrorHolder()).isEqualToComparingFieldByField(EXPECTED_ERROR);
    }

    @Test
    public void proceedDecoupledCancellation_authenticationMethodId_success() {
        // Given
        when(paymentCancellationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledCancellation(UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID, SPI_SINGLE_PAYMENT, AUTHENTICATION_METHOD_ID);

        // Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse).isEqualTo(UPDATE_PIS_COMMON_PAYMENT_RESPONSE_AUTH_METHOD_ID);
    }

    @Test
    public void proceedDecoupledCancellation_authenticationMethodId_failed() {
        // Given
        when(paymentCancellationSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, SPI_SINGLE_PAYMENT, spiAspspConsentDataProvider))
            .thenReturn(AUTH_DECOUPLED_RESPONSE_FAIL);
        when(spiErrorMapper.mapToErrorHolder(AUTH_DECOUPLED_RESPONSE_FAIL, ServiceType.PIS))
            .thenReturn(EXPECTED_ERROR);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCommonDecoupledService.proceedDecoupledCancellation(UPDATE_PIS_COMMON_PAYMENT_REQUEST_AUTH_METHOD_ID, SPI_SINGLE_PAYMENT, AUTHENTICATION_METHOD_ID);

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getErrorHolder()).isEqualToComparingFieldByField(EXPECTED_ERROR);
    }

    private static PsuIdData buildPsuIdData() {
        return new PsuIdData("psuId", "psuIdType", "psuCorporateId", "psuCorporateIdType");
    }

    private static Xs2aUpdatePisCommonPaymentPsuDataRequest buildUpdatePisCommonPaymentPsuDataRequest(String authenticationMethodId) {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setPaymentId(PAYMENT_ID);
        request.setAuthorisationId(AUTHORISATION_ID);
        request.setAuthenticationMethodId(authenticationMethodId);
        request.setPsuData(buildPsuIdData());
        return request;
    }

    private static SpiContextData getSpiContextData() {
        return new SpiContextData(
            new SpiPsuData("psuId", "psuIdType", "psuCorporateId", "psuCorporateIdType", "psuIpAddress"),
            new TppInfo(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static SpiResponse<SpiAuthorisationDecoupledScaResponse> buildSpiResponse() {
        SpiAuthorisationDecoupledScaResponse response = new SpiAuthorisationDecoupledScaResponse(DECOUPLED_PSU_MESSAGE);
        return SpiResponse.<SpiAuthorisationDecoupledScaResponse>builder()
                   .payload(response)
                   .build();
    }

    private static SpiResponse<SpiAuthorisationDecoupledScaResponse> buildSpiResponseFail() {
        return SpiResponse.<SpiAuthorisationDecoupledScaResponse>builder()
                   .error(new TppMessage(MessageErrorCode.SERVICE_NOT_SUPPORTED))
                   .build();
    }

    private static Xs2aUpdatePisCommonPaymentPsuDataResponse buildUpdatePisCommonPaymentPsuDataResponse(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aDecoupledUpdatePisCommonPaymentPsuDataResponse(SCAMETHODSELECTED, PAYMENT_ID, AUTHORISATION_ID, PSU_DATA);
        response.setPsuMessage(AUTH_DECOUPLED_RESPONSE.getPayload().getPsuMessage());
        response.setChosenScaMethod(buildXs2aAuthenticationObjectForDecoupledApproach(request.getAuthenticationMethodId()));
        return response;
    }

    private static Xs2aAuthenticationObject buildXs2aAuthenticationObjectForDecoupledApproach(String authenticationMethodId) {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId(authenticationMethodId);
        return xs2aAuthenticationObject;
    }

}
