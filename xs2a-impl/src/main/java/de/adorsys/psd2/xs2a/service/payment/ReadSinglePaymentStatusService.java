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

package de.adorsys.psd2.xs2a.service.payment;

import de.adorsys.psd2.consent.api.pis.PisPayment;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.pis.ReadPaymentStatusResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.consent.PisAspspDataService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("status-payments")
@RequiredArgsConstructor
public class ReadSinglePaymentStatusService implements ReadPaymentStatusService {
    private final PisAspspDataService pisAspspDataService;
    private final SpiPaymentFactory spiPaymentFactory;
    private final SinglePaymentSpi singlePaymentSpi;
    private final SpiErrorMapper spiErrorMapper;
    private final RequestProviderService requestProviderService;

    @Override
    public ReadPaymentStatusResponse readPaymentStatus(List<PisPayment> pisPayments, String paymentProduct, SpiContextData spiContextData, AspspConsentData aspspConsentData) {
        Optional<SpiSinglePayment> spiSinglePaymentOptional = spiPaymentFactory.createSpiSinglePayment(pisPayments.get(0), paymentProduct);

        if (!spiSinglePaymentOptional.isPresent()) {
            return new ReadPaymentStatusResponse(
                ErrorHolder.builder(MessageErrorCode.RESOURCE_UNKNOWN_404)
                    .messages(Collections.singletonList("Payment not found"))
                    .build()
            );
        }

        SpiResponse<TransactionStatus> spiResponse = singlePaymentSpi.getPaymentStatusById(spiContextData, spiSinglePaymentOptional.get(), aspspConsentData);
        pisAspspDataService.updateAspspConsentData(spiResponse.getAspspConsentData());

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. READ SINGLE Payment STATUS failed. Can't get Payment status by id at SPI-level. Error msg: [{}]",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), spiSinglePaymentOptional.get().getPaymentId(), errorHolder);
            return new ReadPaymentStatusResponse(errorHolder);
        }

        return new ReadPaymentStatusResponse(spiResponse.getPayload());
    }
}
