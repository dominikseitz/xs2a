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

package de.adorsys.psd2.xs2a.web.aspect;

import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.message.MessageService;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.web.controller.PaymentController;
import de.adorsys.psd2.xs2a.web.link.UpdatePisPsuDataLinks;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UpdatePisPsuDataAspect extends AbstractLinkAspect<PaymentController> {
    private ScaApproachResolver scaApproachResolver;

    public UpdatePisPsuDataAspect(ScaApproachResolver scaApproachResolver, MessageService messageService, AspspProfileServiceWrapper aspspProfileServiceWrapper) {
        super(messageService, aspspProfileServiceWrapper);
        this.scaApproachResolver = scaApproachResolver;
    }

    @AfterReturning(pointcut = "execution(* de.adorsys.psd2.xs2a.service.PaymentAuthorisationService.updatePisCommonPaymentPsuData(..)) && args( request)", returning = "result", argNames = "result,request")
    public ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> updatePisAuthorizationAspect(ResponseObject<Xs2aUpdatePisCommonPaymentPsuDataResponse> result, Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        if (!result.hasError()) {
            Xs2aUpdatePisCommonPaymentPsuDataResponse body = result.getBody();
            body.setLinks(new UpdatePisPsuDataLinks(getHttpUrl(), scaApproachResolver, request, body.getScaStatus(),
                                                    body.getChosenScaMethod()));
            return result;
        }

        return enrichErrorTextMessage(result);
    }

}
