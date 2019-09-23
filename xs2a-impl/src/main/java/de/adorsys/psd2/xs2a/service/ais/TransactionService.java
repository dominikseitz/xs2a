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

package de.adorsys.psd2.xs2a.service.ais;

import de.adorsys.psd2.consent.api.TypeAccess;
import de.adorsys.psd2.event.core.model.EventType;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.Transactions;
import de.adorsys.psd2.xs2a.domain.account.Xs2aAccountReport;
import de.adorsys.psd2.xs2a.domain.account.Xs2aTransactionsDownloadResponse;
import de.adorsys.psd2.xs2a.domain.account.Xs2aTransactionsReport;
import de.adorsys.psd2.xs2a.domain.account.Xs2aTransactionsReportByPeriodRequest;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAccountAccess;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.TppService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aAisConsentService;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.ValueValidatorService;
import de.adorsys.psd2.xs2a.service.validator.ais.account.DownloadTransactionsReportValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.GetTransactionDetailsValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.GetTransactionsReportValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.CommonAccountTransactionsRequestObject;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.DownloadTransactionListRequestObject;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.TransactionsReportByPeriodObject;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiTransaction;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiTransactionReport;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiTransactionsDownloadResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.AccountSpi;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.AIS_400;

@Slf4j
@Service
@Validated
@AllArgsConstructor
public class TransactionService {

    private final AccountSpi accountSpi;

    private final SpiToXs2aBalanceMapper balanceMapper;
    private final SpiToXs2aAccountReferenceMapper referenceMapper;
    private final SpiTransactionListToXs2aAccountReportMapper transactionsToAccountReportMapper;
    private final SpiToXs2aTransactionMapper spiToXs2aTransactionMapper;
    private final SpiToXs2aDownloadTransactionsMapper spiToXs2aDownloadTransactionsMapper;

    private final ValueValidatorService validatorService;
    private final Xs2aAisConsentService aisConsentService;
    private final Xs2aAisConsentMapper consentMapper;
    private final TppService tppService;
    private final AspspProfileServiceWrapper aspspProfileService;
    private final Xs2aEventService xs2aEventService;
    private final SpiErrorMapper spiErrorMapper;

    private final GetTransactionsReportValidator getTransactionsReportValidator;
    private final DownloadTransactionsReportValidator downloadTransactionsReportValidator;
    private final GetTransactionDetailsValidator getTransactionDetailsValidator;
    private final RequestProviderService requestProviderService;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    private final AccountHelperService accountHelperService;

    /**
     * Read Transaction reports of a given account addressed by "account-id", depending on the steering parameter
     * "bookingStatus" together with balances.  For a given account, additional parameters are e.g. the attributes
     * "dateFrom" and "dateTo".  The ASPSP might add balance information, if transaction lists without balances are
     * not supported.
     *
     * @param request Xs2aTransactionsReportByPeriodRequest object which contains information for building Xs2aTransactionsReport
     * @return TransactionsReport filled with appropriate transaction arrays Booked and Pending. For v1.1 balances
     * sections is added
     */
    public ResponseObject<Xs2aTransactionsReport> getTransactionsReportByPeriod(Xs2aTransactionsReportByPeriodRequest request) {
        xs2aEventService.recordAisTppRequest(request.getConsentId(), EventType.READ_TRANSACTION_LIST_REQUEST_RECEIVED);

        Optional<AccountConsent> accountConsentOptional = aisConsentService.getAccountConsentById(request.getConsentId());

        UUID internalRequestId = requestProviderService.getInternalRequestId();
        UUID xRequestId = requestProviderService.getRequestId();

        if (!accountConsentOptional.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID [{}]. Get transactions report by period failed. Account consent not found by id",
                     internalRequestId, xRequestId, request.getAccountId(), request.getConsentId());
            return ResponseObject.<Xs2aTransactionsReport>builder()
                       .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                       .build();
        }

        AccountConsent accountConsent = accountConsentOptional.get();
        ValidationResult validationResult = getValidationResultForTransactionsReportByPeriod(request, accountConsent);

        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID [{}], WithBalance [{}], RequestUri [{}]. Get transactions report by period - validation failed: {}",
                     internalRequestId, xRequestId, request.getAccountId(), request.getConsentId(), request.isWithBalance(),
                     request.getRequestUri(), validationResult.getMessageError());
            return ResponseObject.<Xs2aTransactionsReport>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        SpiResponse<SpiTransactionReport> spiResponse = getSpiResponseSpiTransactionReport(request, accountConsent);

        if (spiResponse.hasError()) {
            return checkSpiResponseForTransactionsReport(request, spiResponse);
        }

        return getXs2aTransactionsReportResponseObject(request, accountConsent, spiResponse.getPayload());
    }

    /**
     * Gets transaction details by transaction ID
     *
     * @param consentId     String representing an AccountConsent identification
     * @param accountId     String representing a PSU`s Account at ASPSP
     * @param transactionId String representing the ASPSP identification of transaction
     * @param requestUri    the URI of incoming request
     * @return Transactions based on transaction ID.
     */
    public ResponseObject<Transactions> getTransactionDetails(String consentId, String accountId, String transactionId, String requestUri) {
        xs2aEventService.recordAisTppRequest(consentId, EventType.READ_TRANSACTION_DETAILS_REQUEST_RECEIVED);

        Optional<AccountConsent> accountConsentOptional = aisConsentService.getAccountConsentById(consentId);

        UUID internalRequestId = requestProviderService.getInternalRequestId();
        UUID xRequestId = requestProviderService.getRequestId();

        if (!accountConsentOptional.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID [{}]. Get transaction details failed. Account consent not found by ID",
                     internalRequestId, xRequestId, accountId, consentId);
            return ResponseObject.<Transactions>builder()
                       .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                       .build();
        }

        AccountConsent accountConsent = accountConsentOptional.get();
        ValidationResult validationResult = getValidationResultForCommonAccountTransactions(accountId, requestUri, accountConsent);

        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID [{}], RequestUri [{}]. Get transaction details - validation failed: {}",
                     internalRequestId, xRequestId, accountId, consentId, requestUri, validationResult.getMessageError());
            return ResponseObject.<Transactions>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        SpiResponse<SpiTransaction> spiResponse = getSpiResponseSpiTransaction(accountConsent, consentId, accountId, transactionId);

        if (spiResponse.hasError()) {
            return checkSpiResponseForTransactions(consentId, accountId, spiResponse);
        }

        return getTransactionsResponseObject(consentId, requestUri, accountConsent, spiResponse.getPayload());
    }

    /**
     * Gets stream with transaction list by consent ID, account ID and download ID
     *
     * @param consentId  String representing an AccountConsent identification
     * @param accountId  String representing a PSU`s Account at ASPSP
     * @param downloadId String representing the download identifier
     * @return Response with transaction list stream.
     */
    public ResponseObject<Xs2aTransactionsDownloadResponse> downloadTransactions(String consentId, String accountId, String downloadId) {
        xs2aEventService.recordAisTppRequest(consentId, EventType.DOWNLOAD_TRANSACTION_LIST_REQUEST_RECEIVED);

        Optional<AccountConsent> accountConsentOptional = aisConsentService.getAccountConsentById(consentId);

        UUID internalRequestId = requestProviderService.getInternalRequestId();
        UUID xRequestId = requestProviderService.getRequestId();

        if (!accountConsentOptional.isPresent()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Consent-ID [{}], Account-ID: [{}], Download-ID: [{}]. Download transactions failed. Account consent not found by ID",
                     internalRequestId, xRequestId, consentId, accountId, downloadId);
            return ResponseObject.<Xs2aTransactionsDownloadResponse>builder()
                       .fail(AIS_400, of(CONSENT_UNKNOWN_400))
                       .build();
        }

        AccountConsent accountConsent = accountConsentOptional.get();
        ValidationResult validationResult = getValidationResultForDownloadTransactionRequest(accountConsent);

        if (validationResult.isNotValid()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Consent-ID [{}], Account-ID: [{}], Download-ID: [{}]. Download transactions - validation failed: {}",
                     internalRequestId, xRequestId, consentId, accountId, downloadId, validationResult.getMessageError());
            return ResponseObject.<Xs2aTransactionsDownloadResponse>builder()
                       .fail(validationResult.getMessageError())
                       .build();
        }

        SpiResponse<SpiTransactionsDownloadResponse> spiResponse = getSpiResponseSpiTransactionsDownloadResponse(accountConsent, consentId, downloadId);

        if (spiResponse.hasError()) {
            return checkSpiResponseForTransactionDownloadResponse(consentId, accountId, downloadId, spiResponse);
        }

        return getXs2aTransactionsDownloadResponseResponseObject(spiResponse.getPayload());
    }

    private ValidationResult getValidationResultForTransactionsReportByPeriod(Xs2aTransactionsReportByPeriodRequest request,
                                                                              AccountConsent accountConsent) {
        TransactionsReportByPeriodObject validatorObject = new TransactionsReportByPeriodObject(accountConsent,
                                                                                                request.getAccountId(),
                                                                                                request.isWithBalance(),
                                                                                                request.getRequestUri(),
                                                                                                request.getEntryReferenceFrom(),
                                                                                                request.getDeltaList(),
                                                                                                request.getAcceptHeader(),
                                                                                                request.getBookingStatus());
        return getTransactionsReportValidator.validate(validatorObject);
    }

    private ValidationResult getValidationResultForCommonAccountTransactions(String accountId, String requestUri,
                                                                             AccountConsent accountConsent) {
        CommonAccountTransactionsRequestObject validatorObject = new CommonAccountTransactionsRequestObject(accountConsent,
                                                                                                            accountId,
                                                                                                            requestUri);
        return getTransactionDetailsValidator.validate(validatorObject);
    }

    private ValidationResult getValidationResultForDownloadTransactionRequest(AccountConsent accountConsent) {
        DownloadTransactionListRequestObject validatorObject = new DownloadTransactionListRequestObject(accountConsent);
        return downloadTransactionsReportValidator.validate(validatorObject);
    }

    @NotNull
    private SpiResponse<SpiTransactionReport> getSpiResponseSpiTransactionReport(Xs2aTransactionsReportByPeriodRequest request,
                                                                                 AccountConsent accountConsent) {
        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateToChecked = Optional.ofNullable(request.getDateTo()).orElseGet(LocalDate::now);

        validatorService.validateAccountIdPeriod(request.getAccountId(), dateFrom, dateToChecked);

        boolean isTransactionsShouldContainBalances =
            !aspspProfileService.isTransactionsWithoutBalancesSupported() || request.isWithBalance();

        return accountSpi.requestTransactionsForAccount(accountHelperService.getSpiContextData(),
                                                        request.getAcceptHeader(),
                                                        isTransactionsShouldContainBalances,
                                                        dateFrom,
                                                        dateToChecked,
                                                        request.getBookingStatus(),
                                                        getRequestedAccountReference(accountConsent, request.getAccountId()),
                                                        consentMapper.mapToSpiAccountConsent(accountConsent),
                                                        aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(request.getConsentId()));
    }

    private SpiAccountReference getRequestedAccountReference(AccountConsent accountConsent, String accountId) {
        Xs2aAccountAccess access = accountConsent.getAccess();
        return accountHelperService.findAccountReference(access.getTransactions(), accountId);
    }

    private ResponseObject<Xs2aTransactionsReport> checkSpiResponseForTransactionsReport(Xs2aTransactionsReportByPeriodRequest request,
                                                                                         SpiResponse<SpiTransactionReport> spiResponse) {
        UUID internalRequestId = requestProviderService.getInternalRequestId();
        UUID xRequestId = requestProviderService.getRequestId();

        // in this particular call we use NOT_SUPPORTED to indicate that requested Content-type is not ok for us
        if (spiResponse.getErrors().get(0).getErrorCode() == SERVICE_NOT_SUPPORTED) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID: [{}]. Get transactions report by period failed: requested content-type not json or text.",
                     internalRequestId, xRequestId, request.getAccountId(), request.getConsentId());
            return ResponseObject.<Xs2aTransactionsReport>builder()
                       .fail(ErrorType.AIS_406, of(REQUESTED_FORMATS_INVALID))
                       .build();
        }

        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.AIS);
        log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID: [{}]. Get transactions report by period failed: Request transactions for account fail at SPI level: {}",
                 internalRequestId, xRequestId, request.getAccountId(), request.getConsentId(), errorHolder);
        return ResponseObject.<Xs2aTransactionsReport>builder()
                   .fail(errorHolder)
                   .build();
    }

    @NotNull
    private SpiResponse<SpiTransaction> getSpiResponseSpiTransaction(AccountConsent accountConsent, String consentId,
                                                                     String accountId, String transactionId) {
        validatorService.validateAccountIdTransactionId(accountId, transactionId);

        return accountSpi.requestTransactionForAccountByTransactionId(accountHelperService.getSpiContextData(),
                                                                      transactionId,
                                                                      getRequestedAccountReference(accountConsent, accountId),
                                                                      consentMapper.mapToSpiAccountConsent(accountConsent),
                                                                      aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(consentId));
    }

    private ResponseObject<Transactions> checkSpiResponseForTransactions(String consentId, String accountId,
                                                                         SpiResponse<SpiTransaction> spiResponse) {
        UUID internalRequestId = requestProviderService.getInternalRequestId();
        UUID xRequestId = requestProviderService.getRequestId();

        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.AIS);
        log.info("InR-ID: [{}], X-Request-ID: [{}], Account-ID [{}], Consent-ID: [{}]. Get transaction details failed: Request transactions for account fail at SPI level: {}",
                 internalRequestId, xRequestId, accountId, consentId, errorHolder);
        return ResponseObject.<Transactions>builder()
                   .fail(new MessageError(errorHolder))
                   .build();
    }

    @NotNull
    private SpiResponse<SpiTransactionsDownloadResponse> getSpiResponseSpiTransactionsDownloadResponse(AccountConsent accountConsent,
                                                                                                       String consentId,
                                                                                                       String downloadId) {
        String decodedDownloadId = new String(Base64.getUrlDecoder().decode(downloadId));
        return accountSpi.requestTransactionsByDownloadLink(accountHelperService.getSpiContextData(),
                                                            consentMapper.mapToSpiAccountConsent(accountConsent),
                                                            decodedDownloadId,
                                                            aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(consentId));
    }

    private ResponseObject<Xs2aTransactionsDownloadResponse> checkSpiResponseForTransactionDownloadResponse(String consentId,
                                                                                                            String accountId,
                                                                                                            String downloadId,
                                                                                                            SpiResponse<SpiTransactionsDownloadResponse> spiResponse) {
        UUID xRequestId = requestProviderService.getRequestId();

        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.AIS);
        log.info("X-Request-ID: [{}], Consent-ID [{}], Account-ID: [{}], Download-ID: [{}]. Download transactions failed: couldn't get download transactions stream by link.",
                 xRequestId, consentId, accountId, downloadId);
        return ResponseObject.<Xs2aTransactionsDownloadResponse>builder()
                   .fail(new MessageError(errorHolder))
                   .build();
    }

    @NotNull
    private ResponseObject<Xs2aTransactionsReport> getXs2aTransactionsReportResponseObject(Xs2aTransactionsReportByPeriodRequest request,
                                                                                           AccountConsent accountConsent,
                                                                                           SpiTransactionReport spiTransactionReport) {
        Xs2aTransactionsReport transactionsReport = mapToTransactionsReport(request, accountConsent, spiTransactionReport);
        ResponseObject<Xs2aTransactionsReport> response = ResponseObject.<Xs2aTransactionsReport>builder()
                                                              .body(transactionsReport)
                                                              .build();

        aisConsentService.consentActionLog(tppService.getTppId(),
                                           request.getConsentId(),
                                           accountHelperService.createActionStatus(request.isWithBalance(), TypeAccess.TRANSACTION, response),
                                           request.getRequestUri(),
                                           accountHelperService.needsToUpdateUsage(accountConsent));
        return response;
    }

    @NotNull
    private Xs2aTransactionsReport mapToTransactionsReport(Xs2aTransactionsReportByPeriodRequest request,
                                                           AccountConsent accountConsent,
                                                           SpiTransactionReport spiTransactionReport) {
        Xs2aAccountReport report = transactionsToAccountReportMapper
                                       .mapToXs2aAccountReport(request.getBookingStatus(),
                                                               spiTransactionReport.getTransactions(),
                                                               spiTransactionReport.getTransactionsRaw())
                                       .orElse(null);

        Xs2aTransactionsReport transactionsReport = getXs2aTransactionsReport(report,
                                                                              getRequestedAccountReference(accountConsent, request.getAccountId()),
                                                                              spiTransactionReport);
        if (spiTransactionReport.getDownloadId() != null) {
            String encodedDownloadId = Base64.getUrlEncoder().encodeToString(spiTransactionReport.getDownloadId().getBytes());
            transactionsReport.setDownloadId(encodedDownloadId);
        }
        return transactionsReport;
    }

    private Xs2aTransactionsReport getXs2aTransactionsReport(Xs2aAccountReport report, SpiAccountReference requestedAccountReference,
                                                             SpiTransactionReport spiTransactionReport) {
        Xs2aTransactionsReport transactionsReport = new Xs2aTransactionsReport();
        transactionsReport.setAccountReport(report);
        transactionsReport.setAccountReference(referenceMapper.mapToXs2aAccountReference(requestedAccountReference));
        transactionsReport.setBalances(balanceMapper.mapToXs2aBalanceList(spiTransactionReport.getBalances()));
        transactionsReport.setResponseContentType(spiTransactionReport.getResponseContentType());
        return transactionsReport;
    }

    @NotNull
    private ResponseObject<Transactions> getTransactionsResponseObject(String consentId, String requestUri, AccountConsent accountConsent, SpiTransaction spiTransaction) {
        Transactions transactions = spiToXs2aTransactionMapper.mapToXs2aTransaction(spiTransaction);

        ResponseObject<Transactions> response = ResponseObject.<Transactions>builder()
                                                    .body(transactions)
                                                    .build();

        aisConsentService.consentActionLog(tppService.getTppId(), consentId,
                                           accountHelperService.createActionStatus(false, TypeAccess.TRANSACTION, response),
                                           requestUri,
                                           accountHelperService.needsToUpdateUsage(accountConsent));
        return response;
    }

    private ResponseObject<Xs2aTransactionsDownloadResponse> getXs2aTransactionsDownloadResponseResponseObject(SpiTransactionsDownloadResponse spiTransactionsDownloadResponse) {
        Xs2aTransactionsDownloadResponse transactionsDownloadResponse = spiToXs2aDownloadTransactionsMapper.mapToXs2aTransactionsDownloadResponse(spiTransactionsDownloadResponse);

        return ResponseObject.<Xs2aTransactionsDownloadResponse>builder()
                   .body(transactionsDownloadResponse)
                   .build();
    }
}
