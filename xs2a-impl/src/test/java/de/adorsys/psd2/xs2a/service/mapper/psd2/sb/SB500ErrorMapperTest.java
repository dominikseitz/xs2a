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

package de.adorsys.psd2.xs2a.service.mapper.psd2.sb;

import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.exception.model.error500.Error500NGSB;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.message.MessageService;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SB500ErrorMapperTest {
    private static final String ERROR_JSON_PATH = "json/service/mapper/psd2/sb/Error500NGSB.json";
    private static final String ERROR_CUSTOM_TEXT_JSON_PATH = "json/service/mapper/psd2/sb/Error500NGSB-custom-text.json";
    private static final String CUSTOM_ERROR_TEXT = "Custom text";
    private static final MessageError MESSAGE_ERROR = new MessageError(ErrorType.SB_500,
                                                                       TppMessageInformation.of(INTERNAL_SERVER_ERROR, "text"));
    private static final MessageError MESSAGE_ERROR_WITHOUT_TEXT = new MessageError(ErrorType.SB_500,
                                                                                    TppMessageInformation.of(INTERNAL_SERVER_ERROR));

    private JsonReader jsonReader = new JsonReader();
    @Mock
    private MessageService messageService;
    @InjectMocks
    private SB500ErrorMapper sb500ErrorMapper;

    @Test
    public void getErrorStatus_shouldReturn500() {
        // When
        HttpStatus errorStatus = sb500ErrorMapper.getErrorStatus();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorStatus);
    }

    @Test
    public void getMapper_shouldReturnCorrectErrorMapper() {
        when(messageService.getMessage(INTERNAL_SERVER_ERROR.name()))
            .thenReturn("Some %s");

        // Given
        Error500NGSB expectedError = jsonReader.getObjectFromFile(ERROR_JSON_PATH, Error500NGSB.class);

        // When
        Function<MessageError, Error500NGSB> mapper = sb500ErrorMapper.getMapper();
        Error500NGSB actualError = mapper.apply(MESSAGE_ERROR);

        // Then
        assertEquals(expectedError, actualError);
    }

    @Test
    public void getMapper_withNoTextInTppMessage_shouldGetTextFromMessageService() {
        when(messageService.getMessage(INTERNAL_SERVER_ERROR.name()))
            .thenReturn(CUSTOM_ERROR_TEXT);

        // Given
        Error500NGSB expectedError = jsonReader.getObjectFromFile(ERROR_CUSTOM_TEXT_JSON_PATH, Error500NGSB.class);

        // When
        Function<MessageError, Error500NGSB> mapper = sb500ErrorMapper.getMapper();
        Error500NGSB actualError = mapper.apply(MESSAGE_ERROR_WITHOUT_TEXT);

        // Then
        assertEquals(expectedError, actualError);
        verify(messageService).getMessage(INTERNAL_SERVER_ERROR.name());
    }
}
