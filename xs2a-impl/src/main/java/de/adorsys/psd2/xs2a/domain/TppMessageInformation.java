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

package de.adorsys.psd2.xs2a.domain;

import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.exception.MessageCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static de.adorsys.psd2.xs2a.exception.MessageCategory.ERROR;

@Data
@EqualsAndHashCode
public class TppMessageInformation {
    private MessageCategory category;
    private MessageErrorCode messageErrorCode;
    private String path;
    private String[] textParameters;

    public static TppMessageInformation of(MessageErrorCode messageErrorCode) { //NOPMD
        return of(ERROR, messageErrorCode, null);
    }

    public static TppMessageInformation of(MessageErrorCode messageErrorCode, String... textParameters) { //NOPMD
        return of(ERROR, messageErrorCode, textParameters);
    }

    public static TppMessageInformation of(MessageCategory category, MessageErrorCode messageErrorCode) { //NOPMD
        return of(category, messageErrorCode, null, null);
    }

    public static TppMessageInformation of(MessageCategory category, MessageErrorCode messageErrorCode, String... textParameters) { //NOPMD
        return of(category, messageErrorCode, null, textParameters);
    }
    public static TppMessageInformation of(MessageCategory category, MessageErrorCode messageErrorCode, String path, String... textParameters) { //NOPMD
        return new TppMessageInformation(category, messageErrorCode, path, textParameters);
    }

    private TppMessageInformation(MessageCategory category, MessageErrorCode messageErrorCode, String path, String... textParameters) {
        this.category = category;
        this.messageErrorCode = messageErrorCode;
        this.path = path;
        this.textParameters = textParameters;
    }
}
