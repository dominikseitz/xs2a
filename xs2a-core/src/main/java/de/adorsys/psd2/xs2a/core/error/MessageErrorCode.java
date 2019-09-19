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

package de.adorsys.psd2.xs2a.core.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MessageErrorCode {
    SERVICE_NOT_SUPPORTED(406), // Requested service or it's part is not supported by ASPSP
    CERTIFICATE_INVALID(401),  // The contents of the signature/corporate seal certificate are not matching PSD2 general PSD2 or attribute requirements

    // TPP certificate doesn’t match the initial request
    CERTIFICATE_INVALID_TPP(401) {
        @Override
        public String getName() {
            return CERTIFICATE_INVALID_NAME;
        }
    },
    // You don't have access to this resource
    CERTIFICATE_INVALID_NO_ACCESS(401) {
        @Override
        public String getName() {
            return CERTIFICATE_INVALID_NAME;
        }
    },
    CERTIFICATE_EXPIRED(401),  // Signature/corporate seal certificate is expired
    CERTIFICATE_BLOCKED(401),  // Signature/corporate seal certificate has been blocked by the ASPSP
    CERTIFICATE_REVOKED(401),  // Signature/corporate seal certificate has been revoked by QSTP
    CERTIFICATE_MISSING(401),  // Signature/corporate seal certificate was not available in the request but is mandated for the corresponding
    SIGNATURE_INVALID(401),  // Application layer eIDAS Signature for TPP authentication is not correct
    SIGNATURE_MISSING(401),  // Application layer eIDAS Signature for TPP authentication is mandated by the ASPSP but is missing
    FORMAT_ERROR(400),  // Format of certain request fields are not matching the XS2A requirements

    // Please provide the PSU identification data
    FORMAT_ERROR_NO_PSU(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // PSU-ID is missing in request
    FORMAT_ERROR_NO_PSU_ID(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // PSU-ID should not be blank
    FORMAT_ERROR_PSU_ID_BLANK(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Only one account reference parameter is allowed
    FORMAT_ERROR_MULTIPLE_ACCOUNT_REFERENCES(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Attribute %s is not supported by the ASPSP
    FORMAT_ERROR_ATTRIBUTE_NOT_SUPPORTED(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Only one delta report query parameter can be present in request
    FORMAT_ERROR_MULTIPLE_DELTA_REPORT(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Header '%s' is missing in request
    FORMAT_ERROR_ABSENT_HEADER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Header '%s' should not be null
    FORMAT_ERROR_NULL_HEADER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Header '%s' should not be blank
    FORMAT_ERROR_BLANK_HEADER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Header 'psu-ip-address' has to be correct v.4 or v.6 IP address
    FORMAT_ERROR_WRONG_IP_ADDRESS(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // URIs don't comply with domain from certificate
    FORMAT_ERROR_INVALID_DOMAIN(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Header 'x-request-id' has to be represented by standard 36-char UUID representation
    FORMAT_ERROR_WRONG_HEADER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Payment not found
    FORMAT_ERROR_PAYMENT_NOT_FOUND(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value 'dayOfExecution' should be a number of day in month
    FORMAT_ERROR_INVALID_DAY_OF_EXECUTION(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Query parameter '%s' is missing in request
    FORMAT_ERROR_ABSENT_PARAMETER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Query parameter '%s' has invalid value
    FORMAT_ERROR_INVALID_PARAMETER_VALUE(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Query parameter '%s' should not be blank
    FORMAT_ERROR_BLANK_PARAMETER(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Invalid %s format
    FORMAT_ERROR_INVALID_FIELD(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // The field '%s' is not expected in the request
    FORMAT_ERROR_EXTRA_FIELD(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value '%s' cannot be empty
    FORMAT_ERROR_EMPTY_FIELD(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value '%s' should not be more than %s symbols
    FORMAT_ERROR_OVERSIZE_FIELD(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Cannot deserialize the request body
    FORMAT_ERROR_DESERIALIZATION_FAIL(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value '%s' should not be null
    FORMAT_ERROR_NULL_VALUE(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value '%s' has wrong format
    FORMAT_ERROR_WRONG_FORMAT_VALUE(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Wrong format for '%s': value should be %s '%s' format
    FORMAT_ERROR_WRONG_FORMAT_DATE_FIELD(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Consent object can not contain both list of accounts and the flag allPsd2 or availableAccounts
    FORMAT_ERROR_CONSENT_INCORRECT(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value 'validUntil' should not be in the past
    FORMAT_ERROR_VALID_UNTIL_IN_THE_PAST(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },
    // Value 'frequencyPerDay' should not be lower than 1
    FORMAT_ERROR_INVALID_FREQUENCY(400) {
        @Override
        public String getName() {
            return FORMAT_ERROR_NAME;
        }
    },



    RESOURCE_BLOCKED(400), // The addressed resource is not addressable by this request, since it is blocked e.g. by a grouping in a signing basket
    PSU_CREDENTIALS_INVALID(401),  // The PSU-ID cannot be matched by the addressed ASPSP or is blocked, or a password resp. OTP was not correct. Additional information might be added
    CORPORATE_ID_INVALID(401),  // The PSU-Corporate-ID cannot be matched by the addressed ASPSP
    CONSENT_INVALID(401),  // The consent was created by this TPP but is not valid for the addressed service/resource

    // Consent was revoked by PSU
    CONSENT_INVALID_REVOKED(401) {
        @Override
        public String getName() {
            return "CONSENT_INVALID";
        }
    },

    CONSENT_EXPIRED(401),  // The consent was created by this TPP but has expired and needs to be renewed
    TOKEN_UNKNOWN(401),  // The OAuth2 token cannot be matched by the ASPSP relative to the TPP
    TOKEN_INVALID(401),  // The OAuth2 token is associated to the TPP but is not valid for the addressed service/resource
    TOKEN_EXPIRED(401),  // The OAuth2 token is associated to the TPP but has expired and needs to be renewed

    TIMESTAMP_INVALID(400),  // Timestamp not in accepted time period
    PERIOD_INVALID(400),  // Requested time period out of bound

    // Date values has wrong order
    PERIOD_INVALID_WRONG_ORDER(400) {
        @Override
        public String getName() {
            return "PERIOD_INVALID";
        }
    },

    SCA_METHOD_UNKNOWN(400),  // Addressed SCA method in the AuthenticationObject Method Select Request is unknown or cannot be matched by the ASPSP with the PSU
    TRANSACTION_ID_INVALID(400),  // The TPP-Transaction-ID is not matching the temporary resource

    // PIS specific error codes
    PRODUCT_INVALID(403),  // The addressed payment product is not available for the PSU

    // Payment product invalid for addressed payment
    PRODUCT_INVALID_FOR_PAYMENT(403) {
        @Override
        public String getName() {
            return "PRODUCT_INVALID";
        }
    },

    PRODUCT_UNKNOWN(404),  // The addressed payment product is not supported by the ASPSP

    // Wrong payment product: %s
    PRODUCT_UNKNOWN_WRONG_PAYMENT_PRODUCT(404) {
        @Override
        public String getName() {
            return "PRODUCT_UNKNOWN";
        }
    },

    PAYMENT_FAILED(400),  // The payment initiation POST request failed during the initial process
    REQUIRED_KID_MISSING(401),  // The payment initiation has failed due to a missing KID
    EXECUTION_DATE_INVALID(400), // The requested execution date is not a valid execution date for the ASPSP

    // Value 'requestedExecutionDate' should not be in the past
    EXECUTION_DATE_INVALID_IN_THE_PAST(400) {
        @Override
        public String getName() {
            return "EXECUTION_DATE_INVALID";
        }
    },

    CARD_INVALID(400), // Addressed card number is unknown to the ASPSP or not associated to the PSU
    NO_PIIS_ACTIVATION(400), // The PSU has not activated the addressed account for the usage of the PIIS associated with the TPP

    // AIS specific error code
    SESSIONS_NOT_SUPPORTED(400),  // Sessions are not supported by ASPSP
    ACCESS_EXCEEDED(429),  // The access on the account has been exceeding the consented multiplicity per day
    REQUESTED_FORMATS_INVALID(406),  // The requested formats in the Accept header entry are not matching the formats offered by the ASPSP

    // 400 - The addressed service is not valid for the addressed resources or the submitted data because of payload
    SERVICE_INVALID_400(400) {
        @Override
        public String getName() {
            return SERVICE_INVALID_NAME;
        }
    },
    // 400 - Service invalid for addressed payment
    SERVICE_INVALID_400_FOR_PAYMENT(400) {
        @Override
        public String getName() {
            return SERVICE_INVALID_NAME;
        }
    },
    // 400 - Global Consent is not supported by ASPSP
    SERVICE_INVALID_400_FOR_GLOBAL_CONSENT(400) {
        @Override
        public String getName() {
            return SERVICE_INVALID_NAME;
        }
    },
    // 405 - The addressed service is not valid for the addressed resources or the submitted data because of http method
    SERVICE_INVALID_405(405) {
        @Override
        public String getName() {
            return SERVICE_INVALID_NAME;
        }
    },
    // 405 - HTTP method '%s' is not supported
    SERVICE_INVALID_405_METHOD_NOT_SUPPORTED(405) {
        @Override
        public String getName() {
            return SERVICE_INVALID_NAME;
        }
    },

    SERVICE_BLOCKED(403),  // This service is not reachable for the addressed PSU due to a channel independent blocking by the ASPSP

    // The consent-ID cannot be matched by the ASPSP relative to the TPP because of path
    CONSENT_UNKNOWN_403(403) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // TPP certificate doesn’t match the initial request
    CONSENT_UNKNOWN_403_INCORRECT_CERTIFICATE(403) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // The consent-ID cannot be matched by the ASPSP relative to the TPP because of payload
    CONSENT_UNKNOWN_400(400) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // TPP certificate doesn’t match the initial request
    CONSENT_UNKNOWN_400_INCORRECT_CERTIFICATE(400) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // Unknown TPP access type: %s
    CONSENT_UNKNOWN_400_UNKNOWN_ACCESS_TYPE(400) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // TPP access type should not be null
    CONSENT_UNKNOWN_400_NULL_ACCESS_TYPE(400) {
        @Override
        public String getName() {
            return CONSENT_UNKNOWN_NAME;
        }
    },
    // The addressed resource is unknown relative to the TPP because of account-id in path
    RESOURCE_UNKNOWN_404(404) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // Payment not found
    RESOURCE_UNKNOWN_404_NO_PAYMENT(404) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // PIS authorisation is not found
    RESOURCE_UNKNOWN_404_NO_AUTHORISATION(404) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // PIS cancellation authorisation is not found
    RESOURCE_UNKNOWN_404_NO_CANS_AUTHORISATION(404) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // The addressed resource is unknown relative to the TPP because of other resource in path
    RESOURCE_UNKNOWN_403(403) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // The addressed resource is unknown relative to the TPP because of payload
    RESOURCE_UNKNOWN_400(400) {
        @Override
        public String getName() {
            return RESOURCE_UNKNOWN_NAME;
        }
    },
    // The addressed resource is associated with the TPP but has expired, not addressable anymore because of path
    RESOURCE_EXPIRED_403(403) {
        @Override
        public String getName() {
            return RESOURCE_EXPIRED_NAME;
        }
    },
    // The addressed resource is associated with the TPP but has expired, not addressable anymore because of payload
    RESOURCE_EXPIRED_400(400) {
        @Override
        public String getName() {
            return RESOURCE_EXPIRED_NAME;
        }
    },

    PARAMETER_NOT_SUPPORTED(400), // The parameter is not supported by the API provider

    // bookingStatus '%s' is not supported by ASPSP
    PARAMETER_NOT_SUPPORTED_BOOKING_STATUS(400) {
        @Override
        public String getName() {
            return PARAMETER_NOT_SUPPORTED_STRING;
        }
    },
    // Parameter 'entryReferenceFrom' is not supported by ASPSP
    PARAMETER_NOT_SUPPORTED_ENTRY_REFERENCE_FROM(400) {
        @Override
        public String getName() {
            return PARAMETER_NOT_SUPPORTED_STRING;
        }
    },
    // Parameter 'deltaList' is not supported by ASPSP
    PARAMETER_NOT_SUPPORTED_DELTA_LIST(400) {
        @Override
        public String getName() {
            return PARAMETER_NOT_SUPPORTED_STRING;
        }
    },
    // Wrong payment type: %s
    PARAMETER_NOT_SUPPORTED_WRONG_PAYMENT_TYPE(400) {
        @Override
        public String getName() {
            return PARAMETER_NOT_SUPPORTED_STRING;
        }
    },

    BEARER_TOKEN_EMPTY(400), // Token must not be empty
    INTERNAL_SERVER_ERROR(500), // Internal Server Error
    UNAUTHORIZED(401), // The TPP or the PSU is not correctly authorized to perform the request
    CONTENT_TYPE_NOT_SUPPORTED(406), // The required response content-type is not supported by ASPSP
    UNSUPPORTED_MEDIA_TYPE(415), // Unsupported Media Type
    CANCELLATION_INVALID(405), // Payment initiation cannot be cancelled due to legal or other operational reasons
    SERVICE_UNAVAILABLE(503), // Service is unavailable
    STATUS_INVALID(409); // The addressed resource does not allow addtitional authorisation

    private static final String CERTIFICATE_INVALID_NAME = "CERTIFICATE_INVALID";
    private static final String FORMAT_ERROR_NAME = "FORMAT_ERROR";
    private static final String SERVICE_INVALID_NAME = "SERVICE_INVALID";
    private static final String CONSENT_UNKNOWN_NAME = "CONSENT_UNKNOWN";
    private static final String RESOURCE_UNKNOWN_NAME = "RESOURCE_UNKNOWN";
    private static final String RESOURCE_EXPIRED_NAME = "RESOURCE_EXPIRED";
    private static final String PARAMETER_NOT_SUPPORTED_STRING = "PARAMETER_NOT_SUPPORTED";
    private static Map<String, MessageErrorCode> container = new HashMap<>();

    static {
        Arrays.stream(values())
            .forEach(errorCode -> container.put(errorCode.getName(), errorCode));
    }

    @ApiModelProperty(value = "code", example = "400")
    private int code;

    @JsonCreator
    MessageErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    @JsonIgnore
    public static Optional<MessageErrorCode> getByName(String name) {
        return Optional.ofNullable(container.get(name));
    }
}
