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
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.domain.pis.BulkPayment;
import de.adorsys.psd2.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.psd2.xs2a.domain.pis.SinglePayment;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiBulkPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPeriodicPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiSinglePaymentMapper;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiBulkPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPeriodicPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory class to be used to get SpiPayment from PisPayment, PaymentProduct and PaymentType
 * or concrete SpiPayment (SINGLE/PERIODIC/BULK) from PisPayment and PaymentProduct
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpiPaymentFactory {
    private final CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper;
    private final Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper;
    private final Xs2aToSpiPeriodicPaymentMapper xs2aToSpiPeriodicPaymentMapper;
    private final Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper;
    private final RequestProviderService requestProviderService;

    /**
     * Creates Optional of SpiPayment from PisPayment, PaymentProduct and PaymentType. Should be used, when general SpiPayment type is needed.
     *
     * @param pisPayments    PisPayment
     * @param paymentProduct PaymentProduct
     * @param paymentType    PaymentType
     * @return Optional of SpiPayment subclass of requested payment type or throws IllegalArgumentException for unknown payment type
     */
    public Optional<? extends SpiPayment> createSpiPaymentByPaymentType(List<PisPayment> pisPayments, String paymentProduct, PaymentType paymentType) {
        switch (paymentType) {
            case SINGLE:
                return createSpiSinglePayment(pisPayments.get(0), paymentProduct);
            case PERIODIC:
                return createSpiPeriodicPayment(pisPayments.get(0), paymentProduct);
            case BULK:
                return createSpiBulkPayment(pisPayments, paymentProduct);
            default:
                log.info("InR-ID: [{}], X-Request-ID: [{}]. Unknown payment type: [{}]",
                         requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentType);
                throw new IllegalArgumentException("Unknown payment type");
        }
    }

    /**
     * Creates SpiSinglePayment from PisPayment and PaymentProduct. Should be used, when concrete SpiSinglePayment type is needed.
     *
     * @param pisPayment     PisPayment
     * @param paymentProduct PaymentProduct
     * @return Optional of SpiSinglePayment from PisPayment
     */
    public Optional<SpiSinglePayment> createSpiSinglePayment(PisPayment pisPayment, String paymentProduct) {
        SinglePayment singlePayment = cmsToXs2aPaymentMapper.mapToSinglePayment(pisPayment);

        if (singlePayment == null) {
            log.warn("InR-ID: [{}], X-Request-ID: [{}]. Can't map PIS Payment with paymentProduct [{}] to SINGLE payment.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentProduct);
            return Optional.empty();
        }

        return Optional.ofNullable(xs2aToSpiSinglePaymentMapper.mapToSpiSinglePayment(singlePayment, paymentProduct));
    }

    /**
     * Creates SpiPeriodicPayment from PisPayment and PaymentProduct. Should be used, when concrete SpiPeriodicPayment type is needed.
     *
     * @param pisPayment     PisPayment
     * @param paymentProduct PaymentProduct
     * @return Optional of SpiPeriodicPayment from PisPayment
     */
    public Optional<SpiPeriodicPayment> createSpiPeriodicPayment(PisPayment pisPayment, String paymentProduct) {
        PeriodicPayment periodicPayment = cmsToXs2aPaymentMapper.mapToPeriodicPayment(pisPayment);

        if (periodicPayment == null) {
            log.warn("InR-ID: [{}], X-Request-ID: [{}]. Can't map PIS Payment with paymentProduct [{}] to PERIODIC payment.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentProduct);
            return Optional.empty();
        }

        return Optional.ofNullable(xs2aToSpiPeriodicPaymentMapper.mapToSpiPeriodicPayment(periodicPayment, paymentProduct));
    }

    /**
     * Creates SpiBulkPayment from PisPayment and PaymentProduct. Should be used, when concrete SpiBulkPayment type is needed.
     *
     * @param pisPayments    List of PisPayments
     * @param paymentProduct PaymentProduct
     * @return Optional of SpiBulkPayment from PisPayment
     */
    public Optional<SpiBulkPayment> createSpiBulkPayment(List<PisPayment> pisPayments, String paymentProduct) {
        BulkPayment bulkPayment = cmsToXs2aPaymentMapper.mapToBulkPayment(pisPayments);

        if (bulkPayment == null) {
            log.warn("InR-ID: [{}], X-Request-ID: [{}]. Can't map list of PIS Payments with paymentProduct [{}] to BULK payment.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentProduct);
            return Optional.empty();
        }

        return Optional.ofNullable(xs2aToSpiBulkPaymentMapper.mapToSpiBulkPayment(bulkPayment, paymentProduct));
    }
}
