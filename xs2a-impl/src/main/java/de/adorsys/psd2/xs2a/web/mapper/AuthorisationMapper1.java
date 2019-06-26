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

package de.adorsys.psd2.xs2a.web.mapper;

import de.adorsys.psd2.model.Authorisations;
import de.adorsys.psd2.model.ScaStatusResponse;
import de.adorsys.psd2.model.StartScaprocessResponse;
import de.adorsys.psd2.model.UpdatePsuAuthenticationResponse;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.consent.*;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Mapper(componentModel = "spring",
    uses = {ScaMethodsMapper.class, CoreObjectsMapper.class})
public abstract class AuthorisationMapper1 {

    @Autowired
    protected HrefLinkMapper hrefLinkMapper;
    @Autowired
    protected CoreObjectsMapper coreObjectsMapper;


    abstract Authorisations mapToAuthorisations(Xs2aAuthorisationSubResources xs2AAuthorisationSubResources);

    @Mapping(target = "links", expression = "java(hrefLinkMapper.mapToLinksMap(responseObject.getLinks()))")
    abstract StartScaprocessResponse mapToPisCreateOrUpdateAuthorisationResponse(Xs2aCreatePisAuthorisationResponse responseObject);

    @Mapping(target = "links", expression = "java(hrefLinkMapper.mapToLinksMap(responseObject.getLinks()))")
    @Mapping(target = "scaMethods", source = "availableScaMethods")
    abstract UpdatePsuAuthenticationResponse mapToPisCreateOrUpdateAuthorisationResponse(Xs2aUpdatePisCommonPaymentPsuDataResponse responseObject);

    @Mapping(target = "links", expression = "java(hrefLinkMapper.mapToLinksMap(responseObject.getLinks()))")
    abstract StartScaprocessResponse mapToAisCreateOrUpdateAuthorisationResponse(CreateConsentAuthorizationResponse responseObject);

    @Mapping(target = "links", expression = "java(hrefLinkMapper.mapToLinksMap(responseObject.getLinks()))")
    @Mapping(target = "scaMethods", source = "availableScaMethods")
    abstract UpdatePsuAuthenticationResponse mapToAisCreateOrUpdateAuthorisationResponse(UpdateConsentPsuDataResponse responseObject);

    public @NotNull ScaStatusResponse mapToScaStatusResponse(@NotNull ScaStatus scaStatus) {
        return new ScaStatusResponse().scaStatus(coreObjectsMapper.mapToModelScaStatus(scaStatus));
    }

    public Xs2aCreatePisAuthorisationRequest mapToXs2aCreatePisAuthorisationRequest(PsuIdData psuData, String paymentId,
                                                                                    String paymentService, String paymentProduct, Map body) {
        return new Xs2aCreatePisAuthorisationRequest(
            paymentId,
            psuData,
            paymentProduct,
            paymentService,
            mapToPasswordFromBody(body));
    }

    private String mapToPasswordFromBody(Map body) {
        return Optional.ofNullable(body)
                   .filter(bdy -> !bdy.isEmpty())
                   .map(bdy -> bdy.get("psuData"))
                   .map(o -> (LinkedHashMap<String, String>) o)
                   .map(psuDataMap -> psuDataMap.get("password"))
                   .orElse(null);
    }
}
