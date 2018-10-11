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

package de.adorsys.aspsp.xs2a.service.payment;

import de.adorsys.aspsp.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.aspsp.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aPeriodicPaymentMapper;
import de.adorsys.aspsp.xs2a.spi.domain.SpiResponse;
import de.adorsys.aspsp.xs2a.spi.domain.payment.SpiPaymentProduct;
import de.adorsys.aspsp.xs2a.spi.domain.v2.SpiPeriodicPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("periodic-payments")
@RequiredArgsConstructor
public class ReadPeriodicPayment extends ReadPayment<PeriodicPayment> {
    private final SpiToXs2aPeriodicPaymentMapper xs2aPeriodicPaymentMapper;

    @Override
    public PeriodicPayment getPayment(String paymentId, String paymentProduct) {
        SpiPeriodicPayment request = new SpiPeriodicPayment(SpiPaymentProduct.getByValue(paymentProduct));
        request.setPaymentId(paymentId);
        SpiResponse<SpiPeriodicPayment> spiResponse = periodicPaymentSpi.getPaymentById(request, pisConsentDataService.getAspspConsentDataByPaymentId(paymentId));
        pisConsentDataService.updateAspspConsentData(spiResponse.getAspspConsentData());
        SpiPeriodicPayment periodicPayment = spiResponse.getPayload();

        return xs2aPeriodicPaymentMapper.mapToXs2aPeriodicPayment(periodicPayment);
    }
}
