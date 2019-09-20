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

package de.adorsys.psd2.xs2a.web.validator.body.raw;

import com.fasterxml.jackson.core.type.TypeReference;
import de.adorsys.psd2.xs2a.component.JsonConverter;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.web.validator.ErrorBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.FORMAT_ERROR_DESERIALIZATION_FAIL;

@Component
@RequiredArgsConstructor
public class FieldExtractor {

    private final ErrorBuildingService errorBuildingService;
    private final JsonConverter jsonConverter;

    public Optional<String> extractField(HttpServletRequest request, String fieldName, MessageError messageError) {
        Optional<String> fieldOptional = Optional.empty();
        try {
            // TODO: create common class with Jackson's functionality instead of two: JsonConverter and ObjectMapper.
            //  https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/870
            fieldOptional = jsonConverter.toJsonField(request.getInputStream(), fieldName, new TypeReference<String>() {
            });
        } catch (IOException e) {
            errorBuildingService.enrichMessageError(messageError, TppMessageInformation.of(FORMAT_ERROR_DESERIALIZATION_FAIL));
        }

        return fieldOptional;
    }
}
