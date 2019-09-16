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

package de.adorsys.psd2.xs2a.service.authorization.ais.stage.embedded;


import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.authorization.ais.AisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.ais.CommonDecoupledAisService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aAisConsentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aAuthenticationObjectMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.AisConsentSpi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AisScaStartAuthorisationStageTest {
    private static final String CONSENT_ID = "Test consentId";
    private static final String WRONG_CONSENT_ID = "wrong consent id";
    private static final String AUTHORISATION_ID = "Test authorisation id";
    private static final String PASSWORD = "Test password";
    private static final String PSU_ID = "Test psuId";
    private static final String TEST_AUTHENTICATION_METHOD_ID = "sms";
    private static final ScaStatus FAILED_SCA_STATUS = ScaStatus.FAILED;
    private static final ScaStatus AUTHENTICATED_SCA_STATUS = ScaStatus.PSUAUTHENTICATED;
    private static final ScaStatus METHOD_SELECTED_SCA_STATUS = ScaStatus.SCAMETHODSELECTED;
    private static final MessageErrorCode FORMAT_ERROR_CODE = MessageErrorCode.FORMAT_ERROR;
    private static final SpiPsuData SPI_PSU_DATA = new SpiPsuData(PSU_ID, null, null, null, null);
    private static final PsuIdData PSU_ID_DATA = new PsuIdData(PSU_ID, null, null, null);
    private static final List<SpiAuthenticationObject> MULTIPLE_SPI_SCA_METHODS = Arrays.asList(buildSpiSmsAuthenticationObject(), buildSpiPhotoAuthenticationObject());
    private static final List<Xs2aAuthenticationObject> MULTIPLE_CMS_SCA_METHODS = Arrays.asList(buildXs2aSmsAuthenticationObject(), buildXs2aPhotoAuthenticationObject());
    private static final List<SpiAuthenticationObject> ONE_SPI_SCA_METHOD = Collections.singletonList(buildSpiSmsAuthenticationObject());
    private static final List<SpiAuthenticationObject> NONE_SPI_SCA_METHOD = Collections.emptyList();
    private static final SpiContextData SPI_CONTEXT_DATA = new SpiContextData(SPI_PSU_DATA, new TppInfo(), UUID.randomUUID(), UUID.randomUUID());
    private final List<String> ERROR_MESSAGE_TEXT = Arrays.asList("message 1", "message 2", "message 3");

    private static final String PSU_SUCCESS_MESSAGE = "Test psuSuccessMessage";
    private static final String DECOUPLED_AUTHENTICATION_METHOD_ID = "decoupled method";

    @InjectMocks
    private AisScaReceivedAuthorisationStage scaReceivedAuthorisationStage;

    @Mock
    private Xs2aAisConsentService aisConsentService;
    @Mock
    private AisConsentSpi aisConsentSpi;
    @Mock
    private Xs2aAisConsentMapper aisConsentMapper;
    @Mock
    private Xs2aToSpiPsuDataMapper psuDataMapper;
    @Mock
    private UpdateConsentPsuDataReq request;
    @Mock
    private SpiAccountConsent spiAccountConsent;
    @Mock
    private AccountConsent accountConsent;
    @Mock
    private SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private CommonDecoupledAisService commonDecoupledAisService;
    @Mock
    private ScaApproachResolver scaApproachResolver;
    @Mock
    private AisScaAuthorisationService aisScaAuthorisationService;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;

    @Before
    public void setUp() {
        when(request.getConsentId())
            .thenReturn(CONSENT_ID);

        when(aisConsentService.getAccountConsentById(CONSENT_ID))
            .thenReturn(Optional.of(accountConsent));

        when(aisConsentService.getAccountConsentById(WRONG_CONSENT_ID))
            .thenReturn(Optional.empty());

        when(aisConsentMapper.mapToSpiAccountConsent(accountConsent))
            .thenReturn(spiAccountConsent);

        when(psuDataMapper.mapToSpiPsuData(any(PsuIdData.class)))
            .thenReturn(SPI_PSU_DATA);

        when(request.getPassword())
            .thenReturn(PASSWORD);

        when(spiToXs2aAuthenticationObjectMapper.mapToXs2aAuthenticationObject(buildSpiSmsAuthenticationObject())).thenReturn(buildXs2aSmsAuthenticationObject());

        when(spiToXs2aAuthenticationObjectMapper.mapToXs2aListAuthenticationObject(Arrays.asList(buildSpiSmsAuthenticationObject(), buildSpiPhotoAuthenticationObject()))).thenReturn(Arrays.asList(buildXs2aSmsAuthenticationObject(), buildXs2aPhotoAuthenticationObject()));

        when(request.getPsuData())
            .thenReturn(PSU_ID_DATA);

        when(spiContextDataProvider.provideWithPsuIdData(PSU_ID_DATA))
            .thenReturn(SPI_CONTEXT_DATA);
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)).thenReturn(spiAspspConsentDataProvider);

    }

    @Test
    public void apply_AllAvailableAccounts_Success() {
        //Given
        ArgumentCaptor<ConsentStatus> argumentCaptor = ArgumentCaptor.forClass(ConsentStatus.class);
        when(accountConsent.isConsentForAllAvailableAccounts()).thenReturn(true);
        when(aisScaAuthorisationService.isOneFactorAuthorisation(true, true)).thenReturn(true);
        when(accountConsent.isOneAccessType())
            .thenReturn(true);
        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));
        //When
        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);
        //Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.FINALISED);
        verify(aisConsentService, times(1)).updateConsentStatus(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(ConsentStatus.VALID);
    }

    @Test
    public void apply_AllAvailableAccounts_SuccessScaRequiredTrue() {
        //Given
        when(accountConsent.isOneAccessType())
            .thenReturn(true);
        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));
        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(ONE_SPI_SCA_METHOD));
        when(aisConsentSpi.requestAuthorisationCode(SPI_CONTEXT_DATA, TEST_AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorizationCodeResult()));

        //When
        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);
        //Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.SCAMETHODSELECTED);
        verify(aisConsentService, never()).updateConsentStatus(eq(CONSENT_ID), any(ConsentStatus.class));
    }

    @Test
    public void apply_Failure_AuthorisationStatusSpiResponseFailedWithoutBody() {
        String errorMessagesString = ERROR_MESSAGE_TEXT.toString().replace("[", "").replace("]", "");

        ErrorHolder errorHolder = ErrorHolder.builder(ErrorType.AIS_401)
                                      .tppMessages(TppMessageInformation.of(MessageErrorCode.UNAUTHORIZED, "message 1, message 2, message 3"))
                                      .build();

        when(spiErrorMapper.mapToErrorHolder(any(SpiResponse.class), eq(ServiceType.AIS)))
            .thenReturn(errorHolder);

        SpiResponse<SpiAuthorisationStatus> spiResponse = SpiResponse.<SpiAuthorisationStatus>builder()
                                                              .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                                                              .build();

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(spiResponse);

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_401);
        assertThat(actualResponse.getMessageError().getTppMessage().getText()).isEqualTo(errorMessagesString);
    }

    @Test
    public void apply_Failure_AuthorisationStatusSpiResponseFailedWithBody() {
        SpiResponse<SpiAuthorisationStatus> spiResponse = SpiResponse.<SpiAuthorisationStatus>builder()
                                                              .payload(SpiAuthorisationStatus.FAILURE)
                                                              .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                                                              .build();

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(spiResponse);
        when(aisConsentMapper.mapToSpiUpdateConsentPsuDataReq(any(UpdateConsentPsuDataResponse.class),
                                                              any(UpdateConsentPsuDataReq.class))).thenReturn(request);

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_401);

        verify(aisConsentService).updateConsentAuthorization(any(UpdateConsentPsuDataReq.class));
    }

    @Test
    public void apply_MultipleAvailableScaMethods_Success() {
        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));

        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(MULTIPLE_SPI_SCA_METHODS));

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getAvailableScaMethods()).isEqualTo(MULTIPLE_CMS_SCA_METHODS);
        assertThat(actualResponse.getScaStatus()).isEqualTo(AUTHENTICATED_SCA_STATUS);
    }

    @Test
    public void apply_OneAvailableScaMethod_Success() {
        when(request.getPsuData())
            .thenReturn(PSU_ID_DATA);

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));

        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(ONE_SPI_SCA_METHOD));

        SpiAuthenticationObject scaMethod = ONE_SPI_SCA_METHOD.get(0);

        when(aisConsentSpi.requestAuthorisationCode(SPI_CONTEXT_DATA, TEST_AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorizationCodeResult()));

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getChosenScaMethod()).isEqualTo(buildXs2aSmsAuthenticationObject());
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
    }

    @Test
    public void apply_OneAvailableScaMethod_DecoupledApproach() {
        when(request.getPsuData())
            .thenReturn(PSU_ID_DATA);

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));

        List<SpiAuthenticationObject> availableScaMethods = Collections.singletonList(buildDecoupledAuthenticationObject(DECOUPLED_AUTHENTICATION_METHOD_ID));
        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(availableScaMethods));

        when(commonDecoupledAisService.proceedDecoupledApproach(any(), any(), eq(DECOUPLED_AUTHENTICATION_METHOD_ID), any()))
            .thenReturn(buildUpdateConsentPsuDataResponse());

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getPsuMessage()).isEqualTo(PSU_SUCCESS_MESSAGE);
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
        verify(commonDecoupledAisService).proceedDecoupledApproach(eq(request), eq(spiAccountConsent), eq(DECOUPLED_AUTHENTICATION_METHOD_ID), any());
    }

    @Test
    public void apply_DecoupledApproach_ShouldChangeScaApproach() {
        when(request.getPsuData())
            .thenReturn(PSU_ID_DATA);

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));
        List<SpiAuthenticationObject> availableScaMethods = Collections.singletonList(buildDecoupledAuthenticationObject(DECOUPLED_AUTHENTICATION_METHOD_ID));
        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(availableScaMethods));

        scaReceivedAuthorisationStage.apply(request);

        verify(scaApproachResolver).forceDecoupledScaApproach();
    }

    @Test
    public void apply_OneAvailableScaMethod_Failure_ResponseWithError() {
        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));

        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(ONE_SPI_SCA_METHOD));

        when(aisConsentSpi.requestAuthorisationCode(SPI_CONTEXT_DATA, TEST_AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildErrorSpiResponse(new SpiAuthorizationCodeResult()));

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(new SpiAuthorizationCodeResult()), ServiceType.AIS))
            .thenReturn(ErrorHolder
                            .builder(ErrorType.AIS_400)
                            .tppMessages(TppMessageInformation.of(FORMAT_ERROR_CODE, ""))
                            .build());

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
    }

    @Test
    public void apply_NoneAvailableScaMethods_Failure_WrongScenarioAccordingToSpecification() {
        when(request.getPsuData())
            .thenReturn(PSU_ID_DATA);

        when(aisConsentSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(SpiAuthorisationStatus.SUCCESS));

        when(aisConsentSpi.requestAvailableScaMethods(SPI_CONTEXT_DATA, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(NONE_SPI_SCA_METHOD));

        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
    }

    @Test
    public void apply_Identification_Success() {
        //Given
        when(request.isUpdatePsuIdentification()).thenReturn(true);
        when(request.getPsuData()).thenReturn(PSU_ID_DATA);
        //When
        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);
        //Then
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.PSUIDENTIFIED);
    }

    @Test
    public void apply_Identification_Failure() {
        //Given
        when(request.isUpdatePsuIdentification()).thenReturn(true);
        when(request.getPsuData()).thenReturn(null);
        //When
        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);
        //Then
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.FAILED);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
        assertThat(actualResponse.getMessageError().getTppMessage().getMessageErrorCode()).isEqualTo(MessageErrorCode.FORMAT_ERROR_NO_PSU);
    }

    @Test
    public void apply_Identification_wrongId_Failure() {
        //Given
        when(request.getConsentId()).thenReturn(WRONG_CONSENT_ID);

        //When
        UpdateConsentPsuDataResponse actualResponse = scaReceivedAuthorisationStage.apply(request);

        //Then
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.FAILED);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
        assertThat(actualResponse.getMessageError().getTppMessage().getMessageErrorCode()).isEqualTo(MessageErrorCode.CONSENT_UNKNOWN_400);
    }

    private static SpiAuthenticationObject buildSpiSmsAuthenticationObject() {
        SpiAuthenticationObject spiAuthenticationObject = new SpiAuthenticationObject();
        spiAuthenticationObject.setAuthenticationMethodId("sms");
        spiAuthenticationObject.setAuthenticationType("SMS_OTP");
        return spiAuthenticationObject;
    }

    private static SpiAuthenticationObject buildSpiPhotoAuthenticationObject() {
        SpiAuthenticationObject spiAuthenticationObject = new SpiAuthenticationObject();
        spiAuthenticationObject.setAuthenticationMethodId("photo");
        spiAuthenticationObject.setAuthenticationType("PHOTO_OTP");
        return spiAuthenticationObject;
    }

    private static Xs2aAuthenticationObject buildXs2aSmsAuthenticationObject() {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId("sms");
        xs2aAuthenticationObject.setAuthenticationType("SMS_OTP");
        return xs2aAuthenticationObject;
    }

    private static Xs2aAuthenticationObject buildXs2aPhotoAuthenticationObject() {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId("photo");
        xs2aAuthenticationObject.setAuthenticationType("PHOTO_OTP");
        return xs2aAuthenticationObject;
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildSuccessSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildErrorSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                   .build();
    }

    private UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse() {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(ScaStatus.SCAMETHODSELECTED, CONSENT_ID, AUTHORISATION_ID);
        response.setPsuMessage(PSU_SUCCESS_MESSAGE);
        return response;
    }

    private static SpiAuthenticationObject buildDecoupledAuthenticationObject(String methodId) {
        SpiAuthenticationObject spiAuthenticationObject = new SpiAuthenticationObject();
        spiAuthenticationObject.setAuthenticationMethodId(methodId);
        spiAuthenticationObject.setDecoupled(true);
        return spiAuthenticationObject;
    }
}

