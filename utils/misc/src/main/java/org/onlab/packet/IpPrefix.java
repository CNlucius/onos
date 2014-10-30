/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.packet;

import java.util.Objects;

/**
 * A class representing an IP prefix.
 * TODO: Add support for IPv6 as well.
 * <p/>
 * A prefix consists of an IP address and a subnet mask.
 * NOTE: The stored IP address in the result IP prefix is masked to
 * contain zeroes in all bits after the prefix length.
 */
public final class IpPrefix {
    // Maximum network mask length
    public static final int MAX_INET_MASK_LENGTH = IpAddress.INET_BIT_LENGTH;
    public static final int MAX_INET6_MASK_LENGTH = IpAddress.INET6_BIT_LENGTH;

    private final IpAddress address;
    private final short prefixLength;

    /**
     * Constructor for given IP address, and a prefix length.
     *
     * @param address the IP address
     * @param prefixLength the prefix length
     */
    private IpPrefix(IpAddress address, int prefixLength) {
        checkPrefixLength(prefixLength);
        this.address = IpAddress.makeMaskedAddress(address, prefixLength);
        this.prefixLength = (short) prefixLength;
    }

    /**
     * Checks whether the prefix length is valid.
     *
     * @param prefixLength the prefix length value to check
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    private static void checkPrefixLength(int prefixLength) {
        if ((prefixLength < 0) || (prefixLength > MAX_INET_MASK_LENGTH)) {
            String msg = "Invalid prefix length " + prefixLength + ". " +
                "The value must be in the interval [0, " +
                MAX_INET_MASK_LENGTH + "]";
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Converts an integer and a prefix length into an IPv4 prefix.
     *
     * @param address an integer representing the IPv4 address
     * @param prefixLength the prefix length
     * @return an IP prefix
     */
    public static IpPrefix valueOf(int address, int prefixLength) {
        return new IpPrefix(IpAddress.valueOf(address), prefixLength);
    }

    /**
     * Converts a byte array and a prefix length into an IP prefix.
     *
     * @param address the IP address value stored in network byte order
     * @param prefixLength the prefix length
     * @return an IP prefix
     */
    public static IpPrefix valueOf(byte[] address, int prefixLength) {
        return new IpPrefix(IpAddress.valueOf(address), prefixLength);
    }

    /**
     * Converts an IP address and a prefix length into IP prefix.
     *
     * @param address the IP address
     * @param prefixLength the prefix length
     * @return an IP prefix
     */
    public static IpPrefix valueOf(IpAddress address, int prefixLength) {
        return new IpPrefix(address, prefixLength);
    }

    /**
     * Converts a CIDR (slash) notation string (e.g., "10.1.0.0/16") into an
     * IP prefix.
     *
     * @param value an IP prefix in string form, e.g. "10.1.0.0/16"
     * @return an IP prefix
     */
    public static IpPrefix valueOf(String address) {
        final String[] parts = address.split("/");
        if (parts.length != 2) {
            String msg = "Malformed IP prefix string: " + address + "." +
                "Address must take form \"x.x.x.x/y\"";
            throw new IllegalArgumentException(msg);
        }
        IpAddress ipAddress = IpAddress.valueOf(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        return new IpPrefix(ipAddress, prefixLength);
    }

    /**
     * Returns the IP version of the prefix.
     *
     * @return the IP version of the prefix
     */
    public IpAddress.Version version() {
        return address.version();
    }

    /**
     * Returns the IP address value of the prefix.
     *
     * @return the IP address value of the prefix
     */
    public IpAddress address() {
        return address;
    }

    /**
     * Returns the IP address prefix length.
     *
     * @return the IP address prefix length
     */
    public int prefixLength() {
        return prefixLength;
    }

    /**
     * Determines whether a given IP prefix is contained within this prefix.
     *
     * @param other the IP prefix to test
     * @return true if the other IP prefix is contained in this prefix,
     * otherwise false
     */
    public boolean contains(IpPrefix other) {
        if (this.prefixLength > other.prefixLength) {
            return false;               // This prefix has smaller prefix size
        }

        //
        // Mask the other address with my prefix length.
        // If the other prefix is within this prefix, the masked address must
        // be same as the address of this prefix.
        //
        IpAddress maskedAddr =
            IpAddress.makeMaskedAddress(other.address, this.prefixLength);
        return this.address.equals(maskedAddr);
    }

    /**
     * Determines whether a given IP address is contained within this prefix.
     *
     * @param other the IP address to test
     * @return true if the IP address is contained in this prefix, otherwise
     * false
     */
    public boolean contains(IpAddress other) {
        //
        // Mask the other address with my prefix length.
        // If the other prefix is within this prefix, the masked address must
        // be same as the address of this prefix.
        //
        IpAddress maskedAddr =
            IpAddress.makeMaskedAddress(other, this.prefixLength);
        return this.address.equals(maskedAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        IpPrefix other = (IpPrefix) obj;
        return ((prefixLength == other.prefixLength) &&
                address.equals(other.address));
    }

    @Override
    /*
     * (non-Javadoc)
     * The format is "x.x.x.x/y" for IPv4 prefixes.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(address.toString());
        builder.append("/");
        builder.append(String.format("%d", prefixLength));
        return builder.toString();
    }
}
