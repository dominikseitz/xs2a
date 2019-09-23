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

package de.adorsys.psd2.xs2a.service.mapper;

import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class SpiErrorMapperTest {
    @InjectMocks
    private SpiErrorMapper spiErrorMapper;

    @Test
    public void mapToErrorHolder_WithCustomError() {
        // Given
        String message = "error message";
        ErrorType errorType = ErrorType.PIS_401;
        SpiResponse spiResponse = SpiResponse.builder()
                                      .error(new TppMessage(PSU_CREDENTIALS_INVALID, message))
                                      .build();
        TppMessageInformation expectedTppMessage = TppMessageInformation.buildWithCustomError(PSU_CREDENTIALS_INVALID, message);

        // When
        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);

        // Then
        assertNotNull(errorHolder);
        assertEquals(Collections.singletonList(expectedTppMessage), errorHolder.getTppMessageInformationList());
        assertEquals(errorType, errorHolder.getErrorType());
    }

    @Test
    public void mapToErrorHolder_WithBundleError() {
        // Given
        ErrorType errorType = ErrorType.PIS_401;
        SpiResponse spiResponse = SpiResponse.builder()
                                      .error(new TppMessage(PSU_CREDENTIALS_INVALID))
                                      .build();
        TppMessageInformation expectedTppMessage = TppMessageInformation.of(PSU_CREDENTIALS_INVALID);

        // When
        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);

        // Then
        assertNotNull(errorHolder);
        assertEquals(Collections.singletonList(expectedTppMessage), errorHolder.getTppMessageInformationList());
        assertEquals(errorType, errorHolder.getErrorType());
    }

    @Test
    public void mapToErrorHolder_withoutExplicitErrorsInSpiResponse() {
        // Given
        ErrorType errorType = ErrorType.PIS_401;
        SpiResponse spiResponse = buildSpiResponseTransactionStatus();

        // When
        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);

        // Then
        assertNotNull(errorHolder);
        assertEquals(PSU_CREDENTIALS_INVALID, errorHolder.getTppMessageInformationList().iterator().next().getMessageErrorCode());
        assertEquals(errorType, errorHolder.getErrorType());
    }

    @Test
    public void mapToErrorHolder_withMultipleErrorsInResponse() {
        // Given
        ErrorType firstErrorType = ErrorType.PIS_400;
        TppMessage firstError = new TppMessage(FORMAT_ERROR);


        TppMessage secondError = new TppMessage(CANCELLATION_INVALID);

        SpiResponse spiResponse = SpiResponse.builder()
                                      .error(Arrays.asList(firstError, secondError))
                                      .build();

        TppMessageInformation expectedFirstTppMessage = TppMessageInformation.of(FORMAT_ERROR);
        TppMessageInformation expectedSecondTppMessage = TppMessageInformation.of(CANCELLATION_INVALID);

        // When
        ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);

        // Then
        assertNotNull(errorHolder);
        assertEquals(Arrays.asList(expectedFirstTppMessage, expectedSecondTppMessage), errorHolder.getTppMessageInformationList());
        assertEquals(firstErrorType, errorHolder.getErrorType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToErrorHolder_withoutErrors_shouldThrowIllegalArgumentException() {
        // Given
        SpiResponse spiResponse = SpiResponse.<String>builder().payload("some payload").build();

        // When
        spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
    }

    private static SpiResponse<Void> buildSpiResponseTransactionStatus() {
        return SpiResponse.<Void>builder()
                   .error(new TppMessage(PSU_CREDENTIALS_INVALID))
                   .build();
    }

}
