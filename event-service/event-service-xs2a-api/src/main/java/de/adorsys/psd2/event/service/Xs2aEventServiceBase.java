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

package de.adorsys.psd2.event.service;

import de.adorsys.psd2.event.service.model.EventBO;
import org.jetbrains.annotations.NotNull;

public interface Xs2aEventServiceBase {
    /**
     * Records new Event in the CMS
     *
     * @param event Event to be recorded
     * @return <code>true</code> if the event was recorded. <code>false</code> otherwise.
     */
    boolean recordEvent(@NotNull EventBO event);
}
