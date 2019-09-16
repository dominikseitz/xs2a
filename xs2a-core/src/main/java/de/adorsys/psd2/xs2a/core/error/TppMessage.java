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

package de.adorsys.psd2.xs2a.core.error;

import lombok.Value;

@Value
public class TppMessage {
    private final MessageErrorCode errorCode;
    private final String messageText;
    private final Object[] messageTextArgs;

    public TppMessage(MessageErrorCode errorCode, String messageText, Object... messageTextArgs) {
        this.errorCode = errorCode;
        this.messageText = messageText;
        this.messageTextArgs = messageTextArgs;
    }

    public TppMessage(MessageErrorCode errorCode, Object... messageTextArgs) {
        this(errorCode, "", messageTextArgs);
    }
}
