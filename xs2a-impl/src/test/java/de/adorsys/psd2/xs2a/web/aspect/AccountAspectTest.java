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

package de.adorsys.psd2.xs2a.web.aspect;

import de.adorsys.psd2.aspsp.profile.domain.AspspSettings;
import de.adorsys.psd2.aspsp.profile.service.AspspProfileService;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountDetails;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountDetailsHolder;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountListHolder;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisCancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.service.message.MessageService;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import de.adorsys.psd2.xs2a.web.link.AccountDetailsLinks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_UNKNOWN_400;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.AIS_400;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccountAspectTest {

    private static final String CONSENT_ID = "some consent id";
    private static final String ACCOUNT_ID = "some account id";
    private static final String REQUEST_URI = "/v1/accounts";
    private static final String ERROR_TEXT = "Error occurred while processing";

    @Mock
    private AspspProfileService aspspProfileService;
    @Mock
    private MessageService messageService;

    private Xs2aAccountDetails accountDetails;
    private AccountConsent accountConsent;
    private AspspSettings aspspSettings;
    private ResponseObject responseObject;
    private JsonReader jsonReader = new JsonReader();
    private AccountAspect aspect;

    @Before
    public void setUp() {
        aspect = new AccountAspect(aspspProfileService);
        aspspSettings = jsonReader.getObjectFromFile("json/aspect/aspsp-settings.json", AspspSettings.class);
        accountConsent = jsonReader.getObjectFromFile("json/aspect/account_consent.json", AccountConsent.class);
        accountDetails = jsonReader.getObjectFromFile("json/aspect/account_details.json", Xs2aAccountDetails.class);
    }

    @Test
    public void getAccountDetailsAspect_success() {
        when(aspspProfileService.getAspspSettings()).thenReturn(aspspSettings);

        responseObject = ResponseObject.<Xs2aAccountDetailsHolder>builder()
                             .body(new Xs2aAccountDetailsHolder(accountDetails, accountConsent))
                             .build();
        ResponseObject actualResponse = aspect.getAccountDetailsAspect(responseObject, CONSENT_ID, ACCOUNT_ID, true, REQUEST_URI);

        verify(aspspProfileService, times(1)).getAspspSettings();
        assertNotNull(accountDetails.getLinks());
        assertTrue(accountDetails.getLinks() instanceof AccountDetailsLinks);

        assertFalse(actualResponse.hasError());
    }

    @Test
    public void getAccountDetailsAspect_withError_shouldAddTextErrorMessage() {
        when(messageService.getMessage(any())).thenReturn(ERROR_TEXT);

        responseObject = ResponseObject.<Xs2aCreatePisCancellationAuthorisationResponse>builder()
                             .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                             .build();
        ResponseObject actualResponse = aspect.getAccountDetailsAspect(responseObject, CONSENT_ID, ACCOUNT_ID, true, REQUEST_URI);

        assertTrue(actualResponse.hasError());
        assertEquals(ERROR_TEXT, actualResponse.getError().getTppMessage().getText());
    }

    @Test
    public void getAccountDetailsListAspect_success() {
        when(aspspProfileService.getAspspSettings()).thenReturn(aspspSettings);

        responseObject = ResponseObject.<Xs2aAccountListHolder>builder()
                             .body(new Xs2aAccountListHolder(Collections.singletonList(accountDetails), accountConsent))
                             .build();
        ResponseObject actualResponse = aspect.getAccountDetailsListAspect(responseObject, CONSENT_ID, true, REQUEST_URI);

        verify(aspspProfileService, times(1)).getAspspSettings();
        assertNotNull(accountDetails.getLinks());
        assertTrue(accountDetails.getLinks() instanceof AccountDetailsLinks);

        assertFalse(actualResponse.hasError());
    }

    @Test
    public void getAccountDetailsListAspect_withError_shouldAddTextErrorMessage() {
        when(messageService.getMessage(any())).thenReturn(ERROR_TEXT);

        responseObject = ResponseObject.<Xs2aCreatePisCancellationAuthorisationResponse>builder()
                             .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                             .build();
        ResponseObject actualResponse = aspect.getAccountDetailsListAspect(responseObject, CONSENT_ID, true, REQUEST_URI);

        assertTrue(actualResponse.hasError());
        assertEquals(ERROR_TEXT, actualResponse.getError().getTppMessage().getText());
    }
}
