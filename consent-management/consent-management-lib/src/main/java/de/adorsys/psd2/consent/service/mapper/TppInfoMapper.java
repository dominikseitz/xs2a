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

package de.adorsys.psd2.consent.service.mapper;

import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.core.tpp.TppRedirectUri;
import de.adorsys.psd2.xs2a.core.tpp.TppRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TppInfoMapper {

    @Mapping(target = "redirectUri", source = "tppRedirectUri.uri")
    @Mapping(target = "nokRedirectUri", source = "tppRedirectUri.nokUri")
    @Mapping(target = "cancelRedirectUri", source = "cancelTppRedirectUri.uri")
    @Mapping(target = "cancelNokRedirectUri", source = "cancelTppRedirectUri.nokUri")
    TppInfoEntity mapToTppInfoEntity(TppInfo tppInfo);

    @Mapping(target = "tppRedirectUri", expression = "java(createTppRedirectUri(tppInfoEntity.getRedirectUri(), tppInfoEntity.getNokRedirectUri()))")
    @Mapping(target = "cancelTppRedirectUri", expression = "java(createTppRedirectUri(tppInfoEntity.getCancelRedirectUri(), tppInfoEntity.getCancelNokRedirectUri()))")
    TppInfo mapToTppInfo(TppInfoEntity tppInfoEntity);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @NotNull List<TppRole> copyTppRoles(@Nullable List<TppRole> tppRoles);

    default TppRedirectUri createTppRedirectUri(String redirectUri, String nokRedirectUri) {
        if (redirectUri != null) {
            return new TppRedirectUri(redirectUri, nokRedirectUri);
        }
        return null;
    }
}
