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

package de.adorsys.psd2.xs2a.web.validator;

import de.adorsys.psd2.xs2a.exception.MessageError;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

public class StringMaxLengthValidator implements ObjectValidator<StringMaxLengthValidator.MaxLengthRequirement> {
    @Override
    public void validate(MaxLengthRequirement object, MessageError messageError) {


        protected void checkOptionalFieldForMaxLength(String field, String fieldName, int maxLength, MessageError messageError) {
            if (StringUtils.isNotBlank(field)) {
                checkFieldForMaxLength(field, fieldName, maxLength, messageError);
            }
        }


        private void checkFieldForMaxLength(@NotNull String fieldToCheck, String fieldName, int maxLength, MessageError messageError) {
            if (fieldToCheck.length() > maxLength) {
                String text = String.format("Value '%s' should not be more than %s symbols", fieldName, maxLength);
                errorBuildingService.enrichMessageError(messageError, text);
            }
        }
    }

    @Value
    public class MaxLengthRequirement {
        private String field;
        private String fieldName;
        private int maxLength;
        private MessageError messageError;
    }
}
