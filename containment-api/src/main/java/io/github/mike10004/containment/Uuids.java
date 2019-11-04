package io.github.mike10004.containment;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

import java.util.Random;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods relating to UUIDs.
 * @author mchaberski
 */
public class Uuids {

    private Uuids() {}

    /**
     * Generates a random UUID using a given random number generator.
     * <pre>
     *  Licensed to the Apache Software Foundation (ASF) under one or more
     *  contributor license agreements.  See the NOTICE file distributed with
     *  this work for additional information regarding copyright ownership.
     *  The ASF licenses this file to You under the Apache License, Version 2.0
     *  (the "License"); you may not use this file except in compliance with
     *  the License.  You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing, software
     *  distributed under the License is distributed on an "AS IS" BASIS,
     *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *  See the License for the specific language governing permissions and
     *  limitations under the License.
     * </pre>
     */
    public static UUID randomUuid(Random ng) {
        byte[] data = new byte[16];
        ng.nextBytes(data);
        long msb = (data[0] & 0xFFL) << 56;
        msb |= (data[1] & 0xFFL) << 48;
        msb |= (data[2] & 0xFFL) << 40;
        msb |= (data[3] & 0xFFL) << 32;
        msb |= (data[4] & 0xFFL) << 24;
        msb |= (data[5] & 0xFFL) << 16;
        msb |= (data[6] & 0x0FL) << 8;
        msb |= (0x4L << 12); // set the version to 4
        msb |= (data[7] & 0xFFL);

        long lsb = (data[8] & 0x3FL) << 56;
        lsb |= (0x2L << 62); // set the variant to bits 01
        lsb |= (data[9] & 0xFFL) << 48;
        lsb |= (data[10] & 0xFFL) << 40;
        lsb |= (data[11] & 0xFFL) << 32;
        lsb |= (data[12] & 0xFFL) << 24;
        lsb |= (data[13] & 0xFFL) << 16;
        lsb |= (data[14] & 0xFFL) << 8;
        lsb |= (data[15] & 0xFFL);
        return new UUID(msb, lsb);
    }

    private static String randomUuid(Random ng, java.util.function.Function<? super UUID, String> stringifier) {
        UUID uuid = randomUuid(ng);
        String uuidStr = stringifier.apply(uuid);
        return uuidStr;
    }

    public static String randomUuidString(Random ng) {
        return randomUuid(ng, alphanumericRetainer);
    }

    private static final CharMatcher ENGLISH_ALPHANUMERIC = CharMatcher.inRange('A', 'Z').or(CharMatcher.inRange('a', 'z')).or(CharMatcher.inRange('0', '9'));

    private static final java.util.function.Function<UUID, String> alphanumericRetainer = input -> ENGLISH_ALPHANUMERIC.retainFrom(input.toString());

    private static final String HEX = "[0-9A-Fa-f]";
    // 12345678-1234-5678-1234-567812345678
    private static final String UUID_HEX_STR_REGEX = Joiner.on('-').join(HEX + "{8}", HEX + "{4}", HEX + "{4}", HEX + "{4}", HEX + "{12}");

    public static boolean isStandardHexStringForm(String uuidStr) {
        checkNotNull(uuidStr, "uuidStr");
        return uuidStr.matches(UUID_HEX_STR_REGEX);
    }
}
