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

import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.authorization.ais.CommonDecoupledAisService;
import de.adorsys.psd2.xs2a.service.authorization.ais.stage.AisScaStage;
import de.adorsys.psd2.xs2a.service.consent.Xs2aAisConsentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aAuthenticationObjectMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.AisConsentSpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;

@Slf4j
@Service("AIS_PSUAUTHENTICATED")
public class AisScaMethodSelectedStage extends AisScaStage<UpdateConsentPsuDataReq, UpdateConsentPsuDataResponse> {
    private final SpiContextDataProvider spiContextDataProvider;
    private final CommonDecoupledAisService commonDecoupledAisService;
    private final RequestProviderService requestProviderService;

    public AisScaMethodSelectedStage(Xs2aAisConsentService aisConsentService,
                                     SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory,
                                     AisConsentSpi aisConsentSpi,
                                     Xs2aAisConsentMapper aisConsentMapper,
                                     Xs2aToSpiPsuDataMapper psuDataMapper,
                                     SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper,
                                     SpiContextDataProvider spiContextDataProvider,
                                     SpiErrorMapper spiErrorMapper,
                                     CommonDecoupledAisService commonDecoupledAisService,
                                     RequestProviderService requestProviderService) {
        super(aisConsentService, aspspConsentDataProviderFactory, aisConsentSpi, aisConsentMapper, psuDataMapper, spiToXs2aAuthenticationObjectMapper, spiErrorMapper);
        this.spiContextDataProvider = spiContextDataProvider;
        this.commonDecoupledAisService = commonDecoupledAisService;
        this.requestProviderService = requestProviderService;
    }

    /**
     * Stage for multiple available SCA methods only: request should contain chosen sca method,
     * that is used in a process of requesting authorisation code (returns response with error code in case of wrong code request)
     * and returns response with SCAMETHODSELECTED status.
     *
     * @param request UpdateConsentPsuDataReq with updating data
     * @return UpdateConsentPsuDataResponse as a result of updating process
     */
    @Override
    public UpdateConsentPsuDataResponse apply(UpdateConsentPsuDataReq request) {
        String consentId = request.getConsentId();
        Optional<AccountConsent> accountConsentOptional = aisConsentService.getAccountConsentById(consentId);
        if (!accountConsentOptional.isPresent()) {
            log.warn("InR-ID: [{}], X-Request-ID: [{}], Consent-ID [{}]. AIS_PSUAUTHENTICATED stage. Apply authorisation when update consent PSU data has failed. Consent not found by id.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), consentId);
            MessageError messageError = new MessageError(ErrorType.AIS_400, of(MessageErrorCode.CONSENT_UNKNOWN_400));
            return createFailedResponse(messageError, Collections.emptyList(), request);
        }

        PsuIdData psuData = extractPsuIdData(request);
        AccountConsent accountConsent = accountConsentOptional.get();

        SpiAccountConsent spiAccountConsent = aisConsentMapper.mapToSpiAccountConsent(accountConsent);

        String authenticationMethodId = request.getAuthenticationMethodId();
        if (isDecoupledApproach(request.getAuthorizationId(), authenticationMethodId)) {
            aisConsentService.updateScaApproach(request.getAuthorizationId(), ScaApproach.DECOUPLED);
            return commonDecoupledAisService.proceedDecoupledApproach(request, spiAccountConsent, authenticationMethodId, psuData);
        }

        return proceedEmbeddedApproach(request, spiAccountConsent, psuData);
    }

    private boolean isDecoupledApproach(String authorisationId, String authenticationMethodId) {
        return aisConsentService.isAuthenticationMethodDecoupled(authorisationId, authenticationMethodId);
    }

    private UpdateConsentPsuDataResponse proceedEmbeddedApproach(UpdateConsentPsuDataReq request, SpiAccountConsent spiAccountConsent, PsuIdData psuData) {
        String authenticationMethodId = request.getAuthenticationMethodId();
        SpiResponse<SpiAuthorizationCodeResult> spiResponse = aisConsentSpi.requestAuthorisationCode(spiContextDataProvider.provideWithPsuIdData(psuData), authenticationMethodId, spiAccountConsent, aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(request.getConsentId()));

        if (spiResponse.hasError()) {
            MessageError messageError = new MessageError(spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.AIS));
            log.warn("InR-ID: [{}], X-Request-ID: [{}], Consent-ID [{}], Authorisation-ID [{}], PSU-ID [{}], Authentication-Method-ID [{}]. AIS_PSUAUTHENTICATED stage. Proceed embedded approach when performs authorisation depending on selected SCA method has failed. Error msg: [{}].",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), request.getConsentId(), request.getAuthorizationId(), request.getPsuData().getPsuId(), authenticationMethodId, messageError);
            return createFailedResponse(messageError, spiResponse.getErrors(), request);
        }

        SpiAuthorizationCodeResult authorizationCodeResult = spiResponse.getPayload();

        SpiAuthenticationObject chosenScaMethod = authorizationCodeResult.getSelectedScaMethod();
        ChallengeData challengeData = authorizationCodeResult.getChallengeData();

        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(ScaStatus.SCAMETHODSELECTED, request.getConsentId(), request.getAuthorizationId());
        response.setChosenScaMethod(spiToXs2aAuthenticationObjectMapper.mapToXs2aAuthenticationObject(chosenScaMethod));
        response.setChallengeData(challengeData);
        return response;
    }
}
