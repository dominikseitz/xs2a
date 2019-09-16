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

package de.adorsys.psd2.xs2a.service.payment.read;

import de.adorsys.psd2.consent.api.pis.CommonPaymentData;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.pis.CommonPayment;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInformationResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aPaymentInfoMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentInfoMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.CommonPaymentSpi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReadCommonPaymentServiceTest {
    private static final PsuIdData PSU_DATA = new PsuIdData(null, null, null, null);
    private static final String PRODUCT = "sepa-credit-transfers";
    private static final CommonPaymentData COMMON_PAYMENT = buildCommonPaymentData();
    private static final SpiPaymentInfo SPI_PAYMENT_INFO = new SpiPaymentInfo(PRODUCT);
    private static final SpiContextData SPI_CONTEXT_DATA = getSpiContextData();
    private static final CommonPayment PIS_PAYMENT_INFO = getCommonPayment();
    private static final String SOME_ENCRYPTED_PAYMENT_ID = "Encrypted Payment Id";
    private static final String PAYMENT_NOT_FOUND = "Payment not found";

    @InjectMocks
    private ReadCommonPaymentService readCommonPaymentService;

    @Mock
    private CommonPaymentSpi commonPaymentSpi;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private Xs2aToSpiPaymentInfoMapper xs2aToSpiPaymentInfoMapper;
    @Mock
    private SpiToXs2aPaymentInfoMapper spiToXs2aPaymentInfoMapper;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private RequestProviderService requestProviderService;

    @Before
    public void init() {
        when(xs2aToSpiPaymentInfoMapper.mapToSpiPaymentInfo(COMMON_PAYMENT)).thenReturn(SPI_PAYMENT_INFO);
        when(spiContextDataProvider.provideWithPsuIdData(PSU_DATA)).thenReturn(SPI_CONTEXT_DATA);
        when(spiToXs2aPaymentInfoMapper.mapToXs2aPaymentInfo(any())).thenReturn(PIS_PAYMENT_INFO);
        when(commonPaymentSpi.getPaymentById(SPI_CONTEXT_DATA, SPI_PAYMENT_INFO, spiAspspConsentDataProvider))
            .thenReturn(SpiResponse.<SpiPaymentInfo>builder()
                            .payload(SPI_PAYMENT_INFO)
                            .build());
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(anyString()))
            .thenReturn(spiAspspConsentDataProvider);
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void getPayment_success() {
        // When
        PaymentInformationResponse<CommonPayment> actualResponse = readCommonPaymentService.getPayment(COMMON_PAYMENT, PSU_DATA, SOME_ENCRYPTED_PAYMENT_ID);

        // Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse.getPayment()).isEqualTo(PIS_PAYMENT_INFO);
    }

    @Test
    public void getPayment_failed() {
        // Given
        ErrorHolder expectedError = ErrorHolder.builder(ErrorType.PIS_404)
                                        .tppMessages(TppMessageInformation.of(MessageErrorCode.RESOURCE_UNKNOWN_404, PAYMENT_NOT_FOUND))
                                        .build();
        SpiResponse<SpiPaymentInfo> failSpiResponse = SpiResponse.<SpiPaymentInfo>builder()
                                                          .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                                                          .build();

        when(commonPaymentSpi.getPaymentById(SPI_CONTEXT_DATA, SPI_PAYMENT_INFO, spiAspspConsentDataProvider)).thenReturn(failSpiResponse);
        when(spiErrorMapper.mapToErrorHolder(failSpiResponse, ServiceType.PIS))
            .thenReturn(expectedError);

        // When
        PaymentInformationResponse<CommonPayment> actualResponse = readCommonPaymentService.getPayment(COMMON_PAYMENT, PSU_DATA, SOME_ENCRYPTED_PAYMENT_ID);

        // Then
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getPayment()).isNull();
        assertThat(actualResponse.getErrorHolder()).isNotNull();
        assertThat(actualResponse.getErrorHolder()).isEqualToComparingFieldByField(expectedError);
    }

    private static CommonPaymentData buildCommonPaymentData() {
        PisCommonPaymentResponse request = new PisCommonPaymentResponse();
        request.setPaymentType(PaymentType.SINGLE);
        request.setPaymentProduct("sepa-credit-transfers");
        request.setPaymentData(new byte[16]);

        return request;
    }

    private static SpiContextData getSpiContextData() {
        return new SpiContextData(
            new SpiPsuData("", "", "", "", ""),
            new TppInfo(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static CommonPayment getCommonPayment() {
        return new CommonPayment();
    }
}
