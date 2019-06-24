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

package de.adorsys.psd2.xs2a.web.mapper;

import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.core.sca.OtpFormat;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CoreObjectsMapperImpl.class})
public class CoreObjectsMapperTest {

    private static final byte[] CHALLENGE_DATA_IMAGE = "image_source".getBytes();
    private static final String DATA = "some data";
    private static final String IMAGE_LINK = "localhost:8080/image.jpg";
    private static final String ADDITIONAL_INFO = "image additional info";

    @Autowired
    private CoreObjectsMapper coreObjectsMapper;

    @Test
    public void mapToModelScaStatus_success() {
        for (de.adorsys.psd2.xs2a.core.sca.ScaStatus scaStatus : de.adorsys.psd2.xs2a.core.sca.ScaStatus.values()) {
            // When
            de.adorsys.psd2.model.ScaStatus actual = coreObjectsMapper.mapToModelScaStatus(scaStatus);

            // Then
            assertEquals(de.adorsys.psd2.model.ScaStatus.fromValue(actual.toString()), actual);
        }
    }

    @Test
    public void mapToChallengeData_success() {
        // Given
        ChallengeData challengeData = new ChallengeData(CHALLENGE_DATA_IMAGE, DATA, IMAGE_LINK, 10, OtpFormat.CHARACTERS,
                                                        ADDITIONAL_INFO);

        // When
        de.adorsys.psd2.model.ChallengeData actual = coreObjectsMapper.mapToChallengeData(challengeData);

        // Then
        de.adorsys.psd2.model.ChallengeData expected = buildChallengeData();
        assertArrayEquals(expected.getImage(), actual.getImage());
        assertEquals(expected.getData(), actual.getData());
        assertEquals(expected.getImageLink(), actual.getImageLink());
        assertEquals(expected.getAdditionalInformation(), actual.getAdditionalInformation());
    }

    @Test
    public void mapToChallengeData_shouldReturnNull() {
        // When
        de.adorsys.psd2.model.ChallengeData actual = coreObjectsMapper.mapToChallengeData(null);

        // Then
        assertNull(actual);
    }

    @NotNull
    private de.adorsys.psd2.model.ChallengeData buildChallengeData() {
        de.adorsys.psd2.model.ChallengeData expected = new de.adorsys.psd2.model.ChallengeData();
        expected.setData(DATA);
        expected.setAdditionalInformation(ADDITIONAL_INFO);
        expected.setImage(CHALLENGE_DATA_IMAGE);
        expected.setImageLink(IMAGE_LINK);
        expected.setOtpMaxLength(10);
        expected.setOtpFormat(de.adorsys.psd2.model.ChallengeData.OtpFormatEnum.CHARACTERS);
        return expected;
    }
}
