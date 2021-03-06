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

package de.adorsys.psd2.xs2a.service.validator.pis;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Common payment object that contains necessary information for validating payment
 */
@Value
public class CommonPaymentObject implements PaymentTypeAndInfoProvider {
    //
    // TODO: this validation should be implemented not only for the pisCommonPaymentResponse, but for all methods in the
    //  payment controller including authorisation, cancellation etc. To do that we need to pass the PaymentType and PaymentProduct
    //  from the controller to all those methods.
    //  https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/849
    //

    @NotNull
    private PisCommonPaymentResponse pisCommonPaymentResponse;

    @Override
    public TppInfo getTppInfo() {
        return pisCommonPaymentResponse.getTppInfo();
    }

    @Override
    public PaymentType getPaymentType() {
        return pisCommonPaymentResponse.getPaymentType();
    }

    @Override
    public String getPaymentProduct() {
        return pisCommonPaymentResponse.getPaymentProduct();
    }
}
