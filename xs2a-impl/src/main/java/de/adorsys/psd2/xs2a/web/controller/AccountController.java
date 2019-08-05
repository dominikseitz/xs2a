/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.psd2.xs2a.web.controller;

import de.adorsys.psd2.api.AccountApi;
import de.adorsys.psd2.xs2a.core.ais.BookingStatus;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.Transactions;
import de.adorsys.psd2.xs2a.domain.account.*;
import de.adorsys.psd2.xs2a.exception.MessageCategory;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.AccountService;
import de.adorsys.psd2.xs2a.service.TransactionService;
import de.adorsys.psd2.xs2a.service.mapper.AccountModelMapper;
import de.adorsys.psd2.xs2a.service.mapper.ResponseMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ResponseErrorMapper;
import de.adorsys.psd2.xs2a.web.filter.TppErrorMessage;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unchecked") // This class implements autogenerated interface without proper return values generated
@Slf4j
@RestController
@AllArgsConstructor
@Api(value = "v1", description = "Provides access to the account information", tags = {"Account Information Service (AIS)"})
public class AccountController implements AccountApi {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final ResponseMapper responseMapper;
    private final AccountModelMapper accountModelMapper;
    private final ResponseErrorMapper responseErrorMapper;

    @Override
    public ResponseEntity getAccountList(UUID xRequestID, String consentID, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, String psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Xs2aAccountListHolder> accountList = accountService.getAccountList(consentID, Optional.ofNullable(withBalance).orElse(false), trimEndingSlash(request.getRequestURI()));
        return accountList.hasError()
                   ? responseErrorMapper.generateErrorResponse(accountList.getError())
                   : responseMapper.ok(accountList, accountModelMapper::mapToAccountList);
    }

    @Override
    public ResponseEntity readAccountDetails(String accountId, UUID xRequestID, String consentID, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, String psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Xs2aAccountDetailsHolder> accountDetails = accountService.getAccountDetails(consentID, accountId, Optional.ofNullable(withBalance).orElse(false), trimEndingSlash(request.getRequestURI()));
        return accountDetails.hasError()
                   ? responseErrorMapper.generateErrorResponse(accountDetails.getError())
                   : responseMapper.ok(accountDetails, accountModelMapper::mapToInlineResponse200);
    }

    @Override
    public ResponseEntity getBalances(String accountId, UUID xRequestID, String consentID, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, String psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Xs2aBalancesReport> balancesReport = accountService.getBalancesReport(consentID, accountId, trimEndingSlash(request.getRequestURI()));
        return balancesReport.hasError()
                   ? responseErrorMapper.generateErrorResponse(balancesReport.getError())
                   : responseMapper.ok(balancesReport, accountModelMapper::mapToBalance);
    }

    @Override
    public ResponseEntity getTransactionList(String accountId, String bookingStatus, UUID xRequestID, String consentID, LocalDate dateFrom, LocalDate dateTo, String entryReferenceFrom, Boolean deltaList, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, String psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        Xs2aTransactionsReportByPeriodRequest xs2aTransactionsReportByPeriodRequest = new Xs2aTransactionsReportByPeriodRequest(consentID, accountId, request.getHeader("accept"), BooleanUtils.isTrue(withBalance), dateFrom, dateTo, BookingStatus.forValue(bookingStatus), trimEndingSlash(request.getRequestURI()), entryReferenceFrom, deltaList);
        ResponseObject<Xs2aTransactionsReport> transactionsReport = transactionService.getTransactionsReportByPeriod(xs2aTransactionsReportByPeriodRequest);

        if (transactionsReport.hasError()) {
            return responseErrorMapper.generateErrorResponse(transactionsReport.getError());
        } else if (transactionsReport.getBody().isResponseContentTypeJson()) {
            return responseMapper.ok(transactionsReport, accountModelMapper::mapToTransactionsResponse200Json);
        } else {
            return responseMapper.ok(transactionsReport, accountModelMapper::mapToTransactionsResponseRaw);
        }
    }

    @GetMapping(value = "/v1/accounts/{account-id}/transactions/download/{download-id}")
    public void downloadTransactions(@RequestHeader("X-Request-ID") UUID xRequestId,
                                     @RequestHeader("Consent-ID") String consentId,
                                     @PathVariable("account-id") String accountId,
                                     @PathVariable("download-id") String downloadId) {
        ResponseObject<Xs2aTransactionsDownloadResponse> downloadTransactionsResponse = transactionService.downloadTransactions(consentId, accountId, downloadId);

        if (downloadTransactionsResponse.hasError()) {
            MessageError error = downloadTransactionsResponse.getError();
            TppMessageInformation tppMessage = error.getTppMessage();
            response.setStatus(error.getErrorType().getErrorCode());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            flushResponseError(new TppErrorMessage(tppMessage.getCategory(), tppMessage.getMessageErrorCode(), tppMessage.getText()));
            return;
        }

        Xs2aTransactionsDownloadResponse responseBody = downloadTransactionsResponse.getBody();
        Integer dataSizeBytes = responseBody.getDataSizeBytes();
        String dataFileName = responseBody.getDataFileName();

        try (InputStream transactions = responseBody.getTransactionStream()) {
            IOUtils.copy(transactions, response.getOutputStream(), 4096);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.addHeader("Content-Disposition", resolveContentDisposition(dataFileName));
            if (dataSizeBytes != null) {
                response.setContentLength(dataSizeBytes);
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.info("X-Request-ID: [{}], Consent-ID: [{}], Account-ID: [{}]. Download-ID [{}]. Download transactions failed: IOException occurred in downloadTransactions controller.",
                     xRequestId, consentId, accountId, downloadId);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            flushResponseError(new TppErrorMessage(MessageCategory.ERROR, MessageErrorCode.INTERNAL_SERVER_ERROR, "Internal Server Error"));
        }
    }

    @Override
    public ResponseEntity getTransactionDetails(String accountId, String resourceId, UUID xRequestID, String consentID, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, String psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Transactions> transactionDetails = transactionService.getTransactionDetails(consentID, accountId, resourceId, trimEndingSlash(request.getRequestURI()));
        return transactionDetails.hasError()
                   ? responseErrorMapper.generateErrorResponse(transactionDetails.getError())
                   : responseMapper.ok(transactionDetails, accountModelMapper::mapToTransactionDetails);

    }

    private String trimEndingSlash(String input) {
        String result = input;

        while (StringUtils.endsWith(result, "/")) {
            result = StringUtils.removeEnd(result, "/");
        }
        return result;
    }

    private String resolveContentDisposition(String fileName) {
        return String.format("attachment; filename=%s", fileName == null ? System.currentTimeMillis() : fileName);
    }

    private void flushResponseError(TppErrorMessage errorMessage) {
        try {
            response.getWriter().println(errorMessage.toString());
            response.flushBuffer();
        } catch (IOException e) {
            log.info(" Writing to the httpServletResponse failed.");
        }
    }
}
