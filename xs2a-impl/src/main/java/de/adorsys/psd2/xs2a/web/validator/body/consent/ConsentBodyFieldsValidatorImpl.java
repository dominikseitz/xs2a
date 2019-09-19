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

package de.adorsys.psd2.xs2a.web.validator.body.consent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.model.AccountAccess;
import de.adorsys.psd2.model.Consents;
import de.adorsys.psd2.xs2a.component.JsonConverter;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.web.validator.ErrorBuildingService;
import de.adorsys.psd2.xs2a.web.validator.body.AbstractBodyValidatorImpl;
import de.adorsys.psd2.xs2a.web.validator.body.TppRedirectUriBodyValidatorImpl;
import de.adorsys.psd2.xs2a.web.validator.body.DateFieldValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aRequestBodyDateFields.AIS_CONSENT_DATE_FIELDS;

@Component
public class ConsentBodyFieldsValidatorImpl extends AbstractBodyValidatorImpl implements ConsentBodyValidator {
    private static final String ACCESS_FIELD_NAME = "access";
    private static final String ALL_PSD2_FIELD_NAME = "allPsd2";
    private static final String AVAILABLE_ACCOUNTS_FIELD_NAME = "availableAccounts";
    private static final String AVAILABLE_ACCOUNTS_WITH_BALANCES_FIELD_NAME = "availableAccountsWithBalance";

    private final JsonConverter jsonConverter;
    private TppRedirectUriBodyValidatorImpl tppRedirectUriBodyValidator;
    private DateFieldValidator dateFieldValidator;

    @Autowired
    public ConsentBodyFieldsValidatorImpl(ErrorBuildingService errorBuildingService,
                                          ObjectMapper objectMapper,
                                          JsonConverter jsonConverter, TppRedirectUriBodyValidatorImpl tppRedirectUriBodyValidator,
                                          DateFieldValidator dateFieldValidator) {
        super(errorBuildingService, objectMapper);
        this.dateFieldValidator = dateFieldValidator;
        this.jsonConverter = jsonConverter;
        this.tppRedirectUriBodyValidator = tppRedirectUriBodyValidator;
    }

    @Override
    public void validateBodyFields(HttpServletRequest request, MessageError messageError) {
        tppRedirectUriBodyValidator.validate(request, messageError);

        validateRawAccess(request, messageError);

        Optional<Consents> consentsOptional = mapBodyToInstance(request, messageError, Consents.class);

        // In case of wrong JSON - we don't proceed the inner fields validation.
        if (!consentsOptional.isPresent()) {
            return;
        }

        Consents consents = consentsOptional.get();

        if (Objects.isNull(consents.getRecurringIndicator())) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_NULL_VALUE, "recurringIndicator"));
        }

        if (Objects.isNull(consents.getValidUntil())) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_NULL_VALUE, "validUntil"));
        } else {
            validateValidUntil(consents.getValidUntil(), messageError);
        }

        if (Objects.isNull(consents.getFrequencyPerDay())) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_NULL_VALUE, "frequencyPerDay"));
        } else {
            validateFrequencyPerDay(consents.getFrequencyPerDay(), messageError);
        }
    }

    @Override
    public void validateRawData(HttpServletRequest request, MessageError messageError) {
        validateRawAccess(request, messageError);
        dateFieldValidator.validateDateFormat(request, AIS_CONSENT_DATE_FIELDS.getDateFields(), messageError);
    }

    private void validateValidUntil(LocalDate validUntil, MessageError messageError) {
        if (validUntil.isBefore(LocalDate.now())) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_VALID_UNTIL_IN_THE_PAST));
        }
    }

    private void validateFrequencyPerDay(Integer frequencyPerDay, MessageError messageError) {
        if (frequencyPerDay < 1) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_INVALID_FREQUENCY));
        }
    }

    private void validateRawAccess(HttpServletRequest request, MessageError messageError) {
        Map<String, Object> access = extractConsentAccessMap(request, messageError);

        Object allPsd2 = access.get(ALL_PSD2_FIELD_NAME);
        validateEnumValue(allPsd2, AccountAccess.AllPsd2Enum::fromValue,
                          messageError, TppMessageInformation.of(FORMAT_ERROR_WRONG_FORMAT_VALUE, ALL_PSD2_FIELD_NAME));

        Object availableAccounts = access.get(AVAILABLE_ACCOUNTS_FIELD_NAME);
        validateEnumValue(availableAccounts, AccountAccess.AvailableAccountsEnum::fromValue,
                          messageError, TppMessageInformation.of(FORMAT_ERROR_WRONG_FORMAT_VALUE, AVAILABLE_ACCOUNTS_FIELD_NAME));

        Object availableAccountsWithBalance = access.get(AVAILABLE_ACCOUNTS_WITH_BALANCES_FIELD_NAME);
        validateEnumValue(availableAccountsWithBalance, AccountAccess.AvailableAccountsWithBalanceEnum::fromValue,
                          messageError, TppMessageInformation.of(FORMAT_ERROR_WRONG_FORMAT_VALUE, AVAILABLE_ACCOUNTS_WITH_BALANCES_FIELD_NAME));
    }

    private void validateEnumValue(Object value, Function<String, Enum> mapperToEnum,
                                   MessageError messageError, TppMessageInformation tppMessageInformation) {
        if (value == null || isValidEnumValue(value, mapperToEnum)) {
            return;
        }

        errorBuildingService.enrichMessageError(messageError, tppMessageInformation);
    }

    private boolean isValidEnumValue(@NotNull Object value, Function<String, Enum> mapperToEnum) {
        return value instanceof String
                   && mapperToEnum.apply((String) value) != null;
    }

    private Map<String, Object> extractConsentAccessMap(HttpServletRequest request, MessageError messageError) {
        Optional<Map<String, Object>> access = Optional.empty();
        try {
            access = jsonConverter.toJsonField(request.getInputStream(), ACCESS_FIELD_NAME, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_DESERIALIZATION_FAIL));
        }

        return access.orElseGet(Collections::emptyMap);
    }
}
