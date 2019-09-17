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

package de.adorsys.psd2.xs2a.web.validator.body.payment.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.domain.Xs2aAmount;
import de.adorsys.psd2.xs2a.domain.address.Xs2aAddress;
import de.adorsys.psd2.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.psd2.xs2a.domain.pis.Remittance;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import de.adorsys.psd2.xs2a.web.mapper.PurposeCodeMapper;
import de.adorsys.psd2.xs2a.web.mapper.RemittanceMapper;
import de.adorsys.psd2.xs2a.web.validator.ErrorBuildingService;
import de.adorsys.psd2.xs2a.web.validator.body.AmountValidator;
import de.adorsys.psd2.xs2a.web.validator.body.payment.config.DefaultPaymentValidationConfigImpl;
import de.adorsys.psd2.xs2a.web.validator.body.payment.config.PaymentValidationConfig;
import de.adorsys.psd2.xs2a.web.validator.body.payment.mapper.PaymentMapper;
import de.adorsys.psd2.xs2a.web.validator.header.ErrorBuildingServiceMock;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeriodicPaymentTypeValidatorImplTest {

    private static final String VALUE_36_LENGHT = "QWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJK";
    private static final String VALUE_71_LENGHT = "QWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJKQWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJ";

    private PeriodicPaymentTypeValidatorImpl validator;
    private MessageError messageError;

    private PeriodicPayment periodicPayment;
    private AccountReference accountReference;
    private Xs2aAddress address;

    @Before
    public void setUp() {
        JsonReader jsonReader = new JsonReader();
        messageError = new MessageError();
        periodicPayment = jsonReader.getObjectFromFile("json/validation/periodic-payment.json", PeriodicPayment.class);
        periodicPayment.setStartDate(LocalDate.now().plusDays(1));
        periodicPayment.setEndDate(LocalDate.now().plusDays(5));

        accountReference = jsonReader.getObjectFromFile("json/validation/account_reference.json", AccountReference.class);
        address = jsonReader.getObjectFromFile("json/validation/address.json", Xs2aAddress.class);

        ObjectMapper objectMapper = new ObjectMapper();
        PurposeCodeMapper purposeCodeMapper = Mappers.getMapper(PurposeCodeMapper.class);
        RemittanceMapper remittanceMapper = Mappers.getMapper(RemittanceMapper.class);
        ErrorBuildingService errorBuildingServiceMock = new ErrorBuildingServiceMock(ErrorType.AIS_400);

        PaymentValidationConfig paymentValidationConfig = jsonReader.getObjectFromFile("json/validation/payment-validation-config.json",
                                                                                       DefaultPaymentValidationConfigImpl.class);

        validator = new PeriodicPaymentTypeValidatorImpl(errorBuildingServiceMock,
                                                         objectMapper,
                                                         new PaymentMapper(objectMapper, purposeCodeMapper, remittanceMapper),
                                                         new AmountValidator(errorBuildingServiceMock), paymentValidationConfig);
    }

    @Test
    public void getPaymentType() {
        assertEquals(PaymentType.PERIODIC, validator.getPaymentType());
    }

    @Test
    public void doValidation_success() {
        validator.doPeriodicValidation(periodicPayment, messageError);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void doValidation_endToEndIdentification_tooLong_error() {
        periodicPayment.setEndToEndIdentification(VALUE_36_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "endToEndIdentification", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_debtorAccount_null_error() {
        periodicPayment.setDebtorAccount(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'debtorAccount' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_instructedAmount_null_error() {
        periodicPayment.setInstructedAmount(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'instructedAmount' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_instructedAmount_currency_null_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setCurrency(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'currency' has wrong format", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_instructedAmount_amount_null_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setAmount(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'amount' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_instructedAmount_amount_wrong_format_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setAmount(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'amount' has wrong format", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_creditorAccount_null_error() {
        periodicPayment.setCreditorAccount(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'creditorAccount' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_creditorName_null_error() {
        periodicPayment.setCreditorName(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'creditorName' cannot be empty", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_creditorName_tooLong_error() {
        periodicPayment.setCreditorName(VALUE_71_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "creditorName", 70),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_requestedExecutionDate_error() {
        periodicPayment.setRequestedExecutionDate(LocalDate.now().minusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.EXECUTION_DATE_INVALID_IN_THE_PAST, messageError.getTppMessage().getMessageErrorCode());
    }

    @Test
    public void validateAccount_success() {
        validator.validateAccount(accountReference, messageError);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void validateAccount_iban_error() {
        accountReference.setIban("123");

        validator.validateAccount(accountReference, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Invalid IBAN format", messageError.getTppMessage().getText());
    }

    @Test
    public void validateAccount_bban_error() {
        accountReference.setBban("123");

        validator.validateAccount(accountReference, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Invalid BBAN format", messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_pan_tooLong_error() {
        accountReference.setPan(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "PAN", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_maskedPan_tooLong_error() {
        accountReference.setMaskedPan(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "Masked PAN", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_Msisdn_tooLong_error() {
        accountReference.setMsisdn(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "MSISDN", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_success() {
        validator.validateAddress(address, messageError);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void validatorAddress_street_tooLong_error() {
        address.setStreetName(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "streetName", 100),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_buildingNumber_tooLong_error() {
        address.setBuildingNumber(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "buildingNumber", 20),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_city_tooLong_error() {
        address.setTownName(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "townName", 100),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_postalCode_tooLong_error() {
        address.setPostCode(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "postCode", 5),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_country_null_error() {
        address.setCountry(null);

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'address.country' is required", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_country_codeBlank_error() {
        address.getCountry().setCode("");

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'address.country' is required", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_country_codeFormat_error() {
        address.getCountry().setCode("zz");

        validator.validateAddress(address, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'address.country' should be ISO 3166 ALPHA2 country code", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_startDate_null_error() {
        periodicPayment.setStartDate(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'startDate' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_startDate_beforeNow_error() {
        periodicPayment.setStartDate(LocalDate.now().minusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'startDate' should not be in the past", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_frequency_null_error() {
        periodicPayment.setFrequency(null);

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals("Value 'frequency' should not be null", messageError.getTppMessage().getText());
    }

    @Test
    public void validatorAddress_paymentPeriod_error() {
        periodicPayment.setStartDate(LocalDate.now().plusDays(2)); // Start date bigger than end date
        periodicPayment.setEndDate(LocalDate.now().plusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.PERIOD_INVALID_WRONG_ORDER, messageError.getTppMessage().getMessageErrorCode());
    }

    @Test
    public void doValidation_ultimate_debtor_error() {
        periodicPayment.setUltimateDebtor(VALUE_71_LENGHT);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "ultimateDebtor", 70),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_ultimate_creditor_error() {
        periodicPayment.setUltimateCreditor(VALUE_71_LENGHT);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "ultimateCreditor", 70),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_remittance_no_reference_error() {
        Remittance remittance = new Remittance();
        remittance.setReference(null);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' cannot be empty", "reference"),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_remittance_reference_error() {
        Remittance remittance = new Remittance();
        remittance.setReference(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "reference", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_remittance_reference_type_error() {
        Remittance remittance = new Remittance();
        remittance.setReference("reference");
        remittance.setReferenceType(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "referenceType", 35),
                     messageError.getTppMessage().getText());
    }

    @Test
    public void doValidation_remittance_reference_tissuer_error() {
        Remittance remittance = new Remittance();
        remittance.setReference("reference");
        remittance.setReferenceIssuer(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError);
        assertEquals(MessageErrorCode.FORMAT_ERROR, messageError.getTppMessage().getMessageErrorCode());
        assertEquals(String.format("Value '%s' should not be more than %s symbols", "referenceIssuer", 35),
                     messageError.getTppMessage().getText());
    }
}
