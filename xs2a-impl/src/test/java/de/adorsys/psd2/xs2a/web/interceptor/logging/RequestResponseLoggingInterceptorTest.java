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

package de.adorsys.psd2.xs2a.web.interceptor.logging;

import de.adorsys.psd2.xs2a.component.logger.request.RequestResponseLogMessage;
import de.adorsys.psd2.xs2a.component.logger.request.RequestResponseLogger;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestResponseLoggingInterceptorTest {
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private RequestResponseLogger requestResponseLogger;
    @Mock
    private RequestProviderService requestProviderService;
    @InjectMocks
    private RequestResponseLoggingInterceptor requestResponseLoggingInterceptor;

    @Test
    public void afterCompletion_shouldLogRequestAndResponse() {
        // Given
        UUID internalRequestId = UUID.fromString("b87028ad-6925-41fa-b892-88912606a2f4");
        when(requestProviderService.getInternalRequestId()).thenReturn(internalRequestId);

        RequestResponseLogMessage message = RequestResponseLogMessage.builder(httpServletRequest, httpServletResponse)
                                                .withInternalRequestId(internalRequestId)
                                                .withRequestUri()
                                                .withRequestHeaders()
                                                .withRequestPayload()
                                                .withResponseStatus()
                                                .withResponseHeaders()
                                                .withResponseBody()
                                                .build();

        // When
        requestResponseLoggingInterceptor.afterCompletion(httpServletRequest, httpServletResponse, null, null);

        // Then
        verify(requestResponseLogger).logMessage(message);
    }
}
