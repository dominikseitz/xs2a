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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.model.*;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.HrefType;
import de.adorsys.psd2.xs2a.domain.Links;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.authorisation.AuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.*;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AuthorisationMapper1Impl.class, ScaMethodsMapperImpl.class,
    HrefLinkMapper.class, ObjectMapper.class, CoreObjectsMapperImpl.class})
public class AuthorisationMapper1Test {

    @Autowired
    private AuthorisationMapper1 mapper;
    @Autowired
    private CoreObjectsMapper coreObjectsMapper;
    @Autowired
    private HrefLinkMapper hrefLinkMapper;
    @Autowired
    private ScaMethodsMapper scaMethodsMapper;

    private AuthorisationMapper mapper2;
    private JsonReader jsonReader;

    @Before
    public void setUp() {
        mapper2 = new AuthorisationMapper(coreObjectsMapper, hrefLinkMapper, scaMethodsMapper);
        jsonReader = new JsonReader();
    }

    @Test
    public void mapToAuthorisations() {
        Xs2aAuthorisationSubResources xs2aAuthorisationSubResources = jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-Xs2aAutorisationSubResources.json", Xs2aAuthorisationSubResources.class);
        Authorisations actualAuthorisations = mapper2.mapToAuthorisations(xs2aAuthorisationSubResources);

        Authorisations expectedAuthorisations = jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-Authorisations.json", Authorisations.class);
        // TODO change yaml-generator to correct Authorisations.AuthorisationsList equals https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/871#
        assertArrayEquals(expectedAuthorisations.getAuthorisationIds().toArray(), actualAuthorisations.getAuthorisationIds().toArray());
    }

    @Test
    public void mapToAuthorisations_nullValue() {
        Authorisations actualAuthorisations = mapper.mapToAuthorisations(null);
        assertNull(actualAuthorisations);
    }

    @Test
    public void mapToPisCreateOrUpdateAuthorisationResponse_for_Xs2aCreatePisAuthorisationResponse() {
        // given
        Xs2aCreatePisAuthorisationResponse xs2aCreatePisAuthorisationResponse = jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-Xs2aCreatePisAuthorisationResponse.json", Xs2aCreatePisAuthorisationResponse.class);
        ResponseObject<Xs2aCreatePisAuthorisationResponse> responseObject = ResponseObject.<Xs2aCreatePisAuthorisationResponse>builder()
                                                                                .body(xs2aCreatePisAuthorisationResponse)
                                                                                .build();
        // when
        StartScaprocessResponse actualStartScaProcessResponse = (StartScaprocessResponse) mapper2.mapToPisCreateOrUpdateAuthorisationResponse(responseObject);

        StartScaprocessResponse expectedStartScaProcessResponse = jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-StartScaProcessResponse-expected.json", StartScaprocessResponse.class);

        assertLinks(expectedStartScaProcessResponse.getLinks(), actualStartScaProcessResponse.getLinks());

        expectedStartScaProcessResponse.setLinks(actualStartScaProcessResponse.getLinks());
        assertEquals(expectedStartScaProcessResponse, actualStartScaProcessResponse);
    }

    @Test
    public void mapToPisCreateOrUpdateAuthorisationResponse_for_Xs2aUpdatePisCommonPaymentPsuDataResponse() {
        // given
        Xs2aUpdatePisCommonPaymentPsuDataResponse xs2aUpdatePisCommonPaymentPsuDataResponse =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-Xs2aUpdatePisCommonPaymentPsuDataResponse-ResponseObject.json", Xs2aUpdatePisCommonPaymentPsuDataResponse.class);
        ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> responseObject = ResponseObject.<Xs2aUpdatePisCommonPaymentPsuDataResponse>builder()
                                                                                       .body(xs2aUpdatePisCommonPaymentPsuDataResponse)
                                                                                       .build();
        // when
        UpdatePsuAuthenticationResponse actualUpdatePsuAuthenticationResponse =
            (UpdatePsuAuthenticationResponse) mapper2.mapToPisCreateOrUpdateAuthorisationResponse(responseObject);

        UpdatePsuAuthenticationResponse expectedUpdatePsuAuthenticationResponse =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-UpdatePsuAuthenticationResponse-expected.json", UpdatePsuAuthenticationResponse.class);

        assertLinks(expectedUpdatePsuAuthenticationResponse.getLinks(), actualUpdatePsuAuthenticationResponse.getLinks());

        expectedUpdatePsuAuthenticationResponse.setLinks(actualUpdatePsuAuthenticationResponse.getLinks());
        assertEquals(expectedUpdatePsuAuthenticationResponse, actualUpdatePsuAuthenticationResponse);

        // TODO change yaml-generator to correct ChosenScaMethod equals https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/871#
        assertThatChosenScaMethodsEquals(expectedUpdatePsuAuthenticationResponse.getChosenScaMethod(), actualUpdatePsuAuthenticationResponse.getChosenScaMethod());
    }

    @Test
    public void mapToAisCreateOrUpdateAuthorisationResponse_for_CreateConsentAuthorizationResponse() {
        // Given
        CreateConsentAuthorizationResponse createConsentAuthorizationResponse =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-CreateConsentAuthorisationResponse.json", CreateConsentAuthorizationResponse.class);
        ResponseObject<AuthorisationResponse> responseObject = ResponseObject.<AuthorisationResponse>builder()
                                                                   .body(createConsentAuthorizationResponse)
                                                                   .build();
        // When
        StartScaprocessResponse actualStartScaProcessResponse =
            (StartScaprocessResponse) mapper2.mapToAisCreateOrUpdateAuthorisationResponse(responseObject);

        // Then
        StartScaprocessResponse expectedStartScaProcessResponse =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-start-scaprocess-response-expected.json", StartScaprocessResponse.class);

        assertLinks(expectedStartScaProcessResponse.getLinks(), actualStartScaProcessResponse.getLinks());

        expectedStartScaProcessResponse.setLinks(actualStartScaProcessResponse.getLinks());
        assertEquals(expectedStartScaProcessResponse, actualStartScaProcessResponse);
    }

    @Test
    public void mapToAisCreateOrUpdateAuthorisationResponse_for_UpdateConsentPsuDataResponse() {
        // given
        UpdateConsentPsuDataResponse updateConsentPsuDataResponse = buildUpdateConsentPsuDataResponse();


        ResponseObject<AuthorisationResponse> responseObject = ResponseObject.<AuthorisationResponse>builder()
                                                                   .body(updateConsentPsuDataResponse)
                                                                   .build();
        // when
        UpdatePsuAuthenticationResponse actualUpdatePsuAuthenticationResponse =
            (UpdatePsuAuthenticationResponse) mapper2.mapToAisCreateOrUpdateAuthorisationResponse(responseObject);

        UpdatePsuAuthenticationResponse expectedUpdatePsuAuthenticationResponse =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-UpdatePsuAuthenticationResponse-expected.json", UpdatePsuAuthenticationResponse.class);
        assertLinks(expectedUpdatePsuAuthenticationResponse.getLinks(), actualUpdatePsuAuthenticationResponse.getLinks());

        expectedUpdatePsuAuthenticationResponse.setLinks(actualUpdatePsuAuthenticationResponse.getLinks());
        assertEquals(expectedUpdatePsuAuthenticationResponse, actualUpdatePsuAuthenticationResponse);

        // TODO change yaml-generator to correct ChosenScaMethod equals https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/871#
        assertThatChosenScaMethodsEquals(expectedUpdatePsuAuthenticationResponse.getChosenScaMethod(), actualUpdatePsuAuthenticationResponse.getChosenScaMethod());
    }

    @Test
    public void mapToScaStatusResponse() {
        for (ScaStatus status : ScaStatus.values()) {
            ScaStatusResponse scaStatusResponse = mapper.mapToScaStatusResponse(status);
            assertEquals(status.getValue(), scaStatusResponse.getScaStatus().toString());
        }
    }

    @Test
    public void mapToXs2aCreatePisAuthorisationRequest() {
        PsuIdData psuIdData = jsonReader.getObjectFromFile("json/service/mapper/psu-id-data.json", PsuIdData.class);
        Map<Object, Object> body = new HashMap<>();
        LinkedHashMap<String, String> value = new LinkedHashMap<>();
        value.put("password", "123456");
        body.put("psuData", value);

        Xs2aCreatePisAuthorisationRequest actualXs2aCreatePisAuthorisationRequest = mapper2.mapToXs2aCreatePisAuthorisationRequest(psuIdData, "payment id", "payment service", "payment product", body);

        Xs2aCreatePisAuthorisationRequest expectedXs2aCreatePisAuthorisationRequest =
            jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-Xs2aCreatePisAuthorisationRequest.json", Xs2aCreatePisAuthorisationRequest.class);
        assertEquals(expectedXs2aCreatePisAuthorisationRequest, actualXs2aCreatePisAuthorisationRequest);
    }

    private void assertLinks(Map expectedLinks, Map actualLinks) {
        assertNotNull(actualLinks);
        assertFalse(actualLinks.isEmpty());
        assertEquals(expectedLinks.size(), actualLinks.size());
        for (Object linkKey : actualLinks.keySet()) {
            HrefType actualHrefType = (HrefType) actualLinks.get(linkKey);
            assertEquals(String.valueOf(((Map) expectedLinks.get(linkKey)).get("href")), actualHrefType.getHref());
        }
    }

    private void assertThatChosenScaMethodsEquals(ChosenScaMethod expectedChosenScaMethod, ChosenScaMethod actualChosenScaMethod) {
        assertEquals(expectedChosenScaMethod.getAuthenticationMethodId(), actualChosenScaMethod.getAuthenticationMethodId());
        assertEquals(expectedChosenScaMethod.getAuthenticationType(), actualChosenScaMethod.getAuthenticationType());
        assertEquals(expectedChosenScaMethod.getName(), actualChosenScaMethod.getName());
        assertEquals(expectedChosenScaMethod.getExplanation(), actualChosenScaMethod.getExplanation());
        assertEquals(expectedChosenScaMethod.getAuthenticationVersion(), actualChosenScaMethod.getAuthenticationVersion());
    }

    private UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse() {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(ScaStatus.RECEIVED, "consent ID", "authorisation ID");
        response.setAuthenticationMethodId("authenticationMethod Id");
        response.setScaAuthenticationData("sca authentication data");
        response.setPsuMessage("some message");
        response.setLinks(jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-links.json", Links.class));
        response.setAvailableScaMethods(jsonReader.getListFromFile("json/service/mapper/AuthorisationMapper-availableScaMethods.json", Xs2aAuthenticationObject.class));
        response.setChosenScaMethod(jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-chosenScaMethod.json", Xs2aAuthenticationObject.class));
        response.setChallengeData(jsonReader.getObjectFromFile("json/service/mapper/AuthorisationMapper-challengeData.json", ChallengeData.class));
        return response;
    }
}
