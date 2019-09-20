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

package de.adorsys.psd2.xs2a.service.validator.ais.consent;

import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsentAuthorization;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.PsuDataUpdateAuthorisationChecker;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.ais.consent.dto.UpdateConsentPsuDataRequestObject;
import de.adorsys.psd2.xs2a.service.validator.tpp.AisConsentTppInfoValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateConsentPsuDataValidatorTest {
    private static final TppInfo TPP_INFO = buildTppInfo("authorisation number");
    private static final TppInfo INVALID_TPP_INFO = buildTppInfo("invalid authorisation number");

    private static final PsuIdData PSU_ID_DATA = new PsuIdData("psu-id", null, null, null);

    private static final String AUTHORISATION_ID = "random";
    private static final String INVALID_AUTHORISATION_ID = "random but invalid";

    private static final MessageError AUTHORISATION_VALIDATION_ERROR =
        new MessageError(ErrorType.AIS_403, TppMessageInformation.of(RESOURCE_UNKNOWN_403));

    private static final MessageError STATUS_VALIDATION_ERROR =
        new MessageError(ErrorType.AIS_409, TppMessageInformation.of(STATUS_INVALID));

    private static final MessageError TPP_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_401, TppMessageInformation.of(UNAUTHORIZED));

    @Mock
    private AisConsentTppInfoValidator aisConsentTppInfoValidator;
    @Mock
    private AisAuthorisationValidator aisAuthorisationValidator;
    @Mock
    private AisAuthorisationStatusValidator aisAuthorisationStatusValidator;
    @Mock
    private PsuDataUpdateAuthorisationChecker psuDataUpdateAuthorisationChecker;

    @InjectMocks
    private UpdateConsentPsuDataValidator updateConsentPsuDataValidator;

    @Before
    public void setUp() {
        // Inject pisTppInfoValidator via setter
        updateConsentPsuDataValidator.setAisConsentTppInfoValidator(aisConsentTppInfoValidator);

        when(aisConsentTppInfoValidator.validateTpp(TPP_INFO))
            .thenReturn(ValidationResult.valid());
        when(aisConsentTppInfoValidator.validateTpp(INVALID_TPP_INFO))
            .thenReturn(ValidationResult.invalid(TPP_VALIDATION_ERROR));
        when(psuDataUpdateAuthorisationChecker.canPsuUpdateAuthorisation(PSU_ID_DATA, null))
            .thenReturn(true);
    }

    @Test
    public void validate_withValidConsentObjectAndValidId_shouldReturnValid() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO, ScaStatus.RECEIVED);

        when(aisAuthorisationValidator.validate(AUTHORISATION_ID, accountConsent))
            .thenReturn(ValidationResult.valid());
        when(aisAuthorisationStatusValidator.validate(ScaStatus.RECEIVED))
            .thenReturn(ValidationResult.valid());
        // When

        ValidationResult validationResult = updateConsentPsuDataValidator.validate(new UpdateConsentPsuDataRequestObject(accountConsent, AUTHORISATION_ID, PSU_ID_DATA));

        // Then
        verify(aisConsentTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.getMessageError());
    }

    @Test
    public void validate_withValidConsentObjectAndInvalidId_shouldReturnInvalid() {
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO, ScaStatus.RECEIVED);
        when(aisAuthorisationValidator.validate(INVALID_AUTHORISATION_ID, accountConsent))
            .thenReturn(ValidationResult.invalid(AUTHORISATION_VALIDATION_ERROR));

        ValidationResult validationResult = updateConsentPsuDataValidator.validate(new UpdateConsentPsuDataRequestObject(accountConsent, INVALID_AUTHORISATION_ID, PSU_ID_DATA));

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(AUTHORISATION_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidScaStatus_shouldReturnInvalid() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO, ScaStatus.FAILED);

        when(aisAuthorisationValidator.validate(AUTHORISATION_ID, accountConsent))
            .thenReturn(ValidationResult.valid());
        when(aisAuthorisationStatusValidator.validate(ScaStatus.FAILED))
            .thenReturn(ValidationResult.invalid(STATUS_VALIDATION_ERROR));
        // When
        ValidationResult validationResult = updateConsentPsuDataValidator.validate(new UpdateConsentPsuDataRequestObject(accountConsent, AUTHORISATION_ID, PSU_ID_DATA));

        // Then
        verify(aisConsentTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(STATUS_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidTppInConsent_shouldReturnTppValidationError() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(INVALID_TPP_INFO, ScaStatus.RECEIVED);
        // When
        ValidationResult validationResult = updateConsentPsuDataValidator.validate(new UpdateConsentPsuDataRequestObject(accountConsent, AUTHORISATION_ID, PSU_ID_DATA));

        // Then
        verify(aisConsentTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    private static TppInfo buildTppInfo(String authorisationNumber) {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber(authorisationNumber);
        return tppInfo;
    }

    private AccountConsent buildAccountConsent(TppInfo tppInfo, ScaStatus scaStatus) {
        return new AccountConsent("id", null, false, null, 0,
                                  null, null, false, false,
                                  Collections.emptyList(), tppInfo, null, false,
                                  buildAuthorization(scaStatus), null, Collections.emptyMap());
    }

    private List<AccountConsentAuthorization> buildAuthorization(ScaStatus scaStatus) {
        AccountConsentAuthorization authorization = new AccountConsentAuthorization();
        authorization.setId(AUTHORISATION_ID);
        authorization.setScaStatus(scaStatus);
        return Collections.singletonList(authorization);
    }
}
