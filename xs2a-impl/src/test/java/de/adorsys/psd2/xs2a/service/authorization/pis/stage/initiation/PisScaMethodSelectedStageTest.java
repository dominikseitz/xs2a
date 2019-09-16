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

package de.adorsys.psd2.xs2a.service.authorization.pis.stage.initiation;

import de.adorsys.psd2.consent.api.pis.PisPayment;
import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.consent.PisAspspDataService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aPisCommonPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiSinglePaymentMapper;
import de.adorsys.psd2.xs2a.service.payment.Xs2aUpdatePaymentAfterSpiService;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.FINALISED;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.PIS_400;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PisScaMethodSelectedStageTest {
    private final List<String> ERROR_MESSAGE_TEXT = Arrays.asList("message 1", "message 2", "message 3");
    private static final String AUTHENTICATION_METHOD_ID = "sms";
    private static final String PAYMENT_ID = "123456789";
    private static final String PSU_ID = "id";
    private static final SpiContextData CONTEXT_DATA = new SpiContextData(new SpiPsuData(null, null, null, null, null), new TppInfo(), UUID.randomUUID(), UUID.randomUUID());
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final TransactionStatus ACCP_TRANSACTION_STATUS = TransactionStatus.ACCP;
    private static final SpiPaymentExecutionResponse SPI_PAYMENT_EXECUTION_RESPONSE = new SpiPaymentExecutionResponse(ACCP_TRANSACTION_STATUS);

    @InjectMocks
    private PisScaMethodSelectedStage pisScaMethodSelectedStage;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private PisAspspDataService pisAspspDataService;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private SinglePaymentSpi singlePaymentSpi;
    @Mock
    private Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper;
    @Mock
    private Xs2aPisCommonPaymentMapper xs2aPisCommonPaymentMapper;
    @Mock
    private CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private Xs2aUpdatePaymentAfterSpiService updatePaymentAfterSpiService;
    @Mock
    private SpiAspspConsentDataProviderFactory spiAspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;
    @Mock
    private RequestProviderService requestProviderService;

    @Before
    public void setUp() {
        ErrorHolder errorHolder = ErrorHolder.builder(PIS_400)
                                      .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR, "message 1, message 2, message 3"))
                                      .build();

        when(spiErrorMapper.mapToErrorHolder(any(SpiResponse.class), eq(ServiceType.PIS)))
            .thenReturn(errorHolder);

        when(spiContextDataProvider.provideWithPsuIdData(any(PsuIdData.class))).thenReturn(CONTEXT_DATA);

        when((spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(anyString())))
            .thenReturn(spiAspspConsentDataProvider);

        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void apply_paymentSpi_verifyScaAuthorisationAndExecutePayment_fail() {
        String errorMessagesString = ERROR_MESSAGE_TEXT.toString().replace("[", "").replace("]", "");
        SpiResponse<SpiPaymentExecutionResponse> spiErrorMessage = SpiResponse.<SpiPaymentExecutionResponse>builder()
                                                                       .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                                                                       .build();
        when(pisAspspDataService.getInternalPaymentIdByEncryptedString(PAYMENT_ID)).thenReturn(any());
        when(applicationContext.getBean(SinglePaymentSpi.class))
            .thenReturn(singlePaymentSpi);

        // generate an error
        when(singlePaymentSpi.verifyScaAuthorisationAndExecutePayment(any(), any(), any(), any()))
            .thenReturn(spiErrorMessage);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisScaMethodSelectedStage.apply(buildRequest(), buildResponse());

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getErrorHolder().getErrorType().getErrorCode()).isEqualTo(MessageErrorCode.FORMAT_ERROR.getCode());
        assertThat(actualResponse.getErrorHolder().getTppMessageInformationList().iterator().next().getText()).isEqualTo(errorMessagesString);
    }

    @Test
    public void apply_Success() {
        when(pisAspspDataService.getInternalPaymentIdByEncryptedString(PAYMENT_ID)).thenReturn(any());
        when(applicationContext.getBean(SinglePaymentSpi.class))
            .thenReturn(singlePaymentSpi);

        when(singlePaymentSpi.verifyScaAuthorisationAndExecutePayment(any(), any(), any(), any()))
            .thenReturn(buildSuccessSpiResponse());

        when(updatePaymentAfterSpiService.updatePaymentStatus(PAYMENT_ID, ACCP_TRANSACTION_STATUS))
            .thenReturn(true);

        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisScaMethodSelectedStage.apply(buildRequest(), buildResponse());

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FINALISED);
    }

    private Xs2aUpdatePisCommonPaymentPsuDataRequest buildRequest() {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setAuthenticationMethodId(PisScaMethodSelectedStageTest.AUTHENTICATION_METHOD_ID);
        request.setPaymentId(PisScaMethodSelectedStageTest.PAYMENT_ID);
        request.setPsuData(buildPsuIdData());
        return request;
    }

    private GetPisAuthorisationResponse buildResponse() {
        GetPisAuthorisationResponse pisAuthorisationResponse = new GetPisAuthorisationResponse();
        pisAuthorisationResponse.setPaymentType(PaymentType.SINGLE);
        pisAuthorisationResponse.setPaymentProduct(PAYMENT_PRODUCT);
        PisPaymentInfo pisPaymentInfo = buildPisPaymentInfo(PisScaMethodSelectedStageTest.PAYMENT_ID);
        pisAuthorisationResponse.setPaymentInfo(pisPaymentInfo);
        pisAuthorisationResponse.setPayments(getPisPayment());
        return pisAuthorisationResponse;
    }

    private PisPaymentInfo buildPisPaymentInfo(String paymentId) {
        PisPaymentInfo pisPaymentInfo = new PisPaymentInfo();
        pisPaymentInfo.setPaymentProduct(PAYMENT_PRODUCT);
        pisPaymentInfo.setPaymentType(PaymentType.SINGLE);
        pisPaymentInfo.setPaymentId(paymentId);
        return pisPaymentInfo;
    }

    private PsuIdData buildPsuIdData() {
        return new PsuIdData(PSU_ID, "type", "corporate ID", "corporate type");
    }

    private List<PisPayment> getPisPayment() {
        PisPayment pisPayment = new PisPayment();
        pisPayment.setTransactionStatus(TransactionStatus.RCVD);
        return Collections.singletonList(pisPayment);
    }

    private SpiResponse<SpiPaymentExecutionResponse> buildSuccessSpiResponse() {
        return SpiResponse.<SpiPaymentExecutionResponse>builder()
                   .payload(SPI_PAYMENT_EXECUTION_RESPONSE)
                   .build();
    }
}
