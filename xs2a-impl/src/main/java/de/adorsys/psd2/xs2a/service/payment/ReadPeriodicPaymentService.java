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
import de.adorsys.psd2.xs2a.domain.pis.CommonPayment;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aPeriodicPaymentMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPeriodicPayment;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PeriodicPaymentSpi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("periodic-payments")
public class ReadPeriodicPaymentService extends AbstractReadPaymentService {

    private PeriodicPaymentSpi periodicPaymentSpi;
    private SpiToXs2aPeriodicPaymentMapper spiToXs2aPeriodicPaymentMapper;

    @Autowired
    public ReadPeriodicPaymentService(PeriodicPaymentSpi periodicPaymentSpi, SpiToXs2aPeriodicPaymentMapper spiToXs2aPeriodicPaymentMapper,
                                      SpiErrorMapper spiErrorMapper, SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory,
                                      RequestProviderService requestProviderService, Xs2aUpdatePaymentAfterSpiService updatePaymentStatusAfterSpiService,
                                      SpiContextDataProvider spiContextDataProvider, SpiPaymentFactory spiPaymentFactory) {
        super(spiErrorMapper, aspspConsentDataProviderFactory, requestProviderService,updatePaymentStatusAfterSpiService, spiContextDataProvider, spiPaymentFactory);
        this.periodicPaymentSpi = periodicPaymentSpi;
        this.spiToXs2aPeriodicPaymentMapper = spiToXs2aPeriodicPaymentMapper;
    }

    @Override
    public Optional<SpiPeriodicPayment> createSpiPayment(List<PisPayment> pisPayments, String paymentProduct) {
        return spiPaymentFactory.createSpiPeriodicPayment(pisPayments.get(0), paymentProduct);
    }

    @Override
    public SpiResponse<SpiPeriodicPayment> getSpiPaymentById(SpiContextData spiContextData, Object spiPayment, SpiAspspConsentDataProvider aspspConsentDataProvider) {
        return periodicPaymentSpi.getPaymentById(spiContextData, (SpiPeriodicPayment) spiPayment, aspspConsentDataProvider);
    }

    @Override
    public CommonPayment getXs2aPayment(SpiResponse spiResponse) {
        SpiPeriodicPayment spiPeriodicPayment = (SpiPeriodicPayment) spiResponse.getPayload();
        return spiToXs2aPeriodicPaymentMapper.mapToXs2aPeriodicPayment(spiPeriodicPayment);
    }
}
