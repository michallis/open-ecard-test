/****************************************************************************
 * Copyright (C) 2012 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.ifd.protocol.pace.crypto;

import org.openecard.bouncycastle.crypto.digests.SHA256Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;


/**
 * Implements a Key Derivation Function (KDF).
 * See BSI-TR-03110, version 2.10, part 3, section A.2.3.
 *
 * @author Moritz Horsch
 */
public final class KDF {

    private static final Logger logger = LoggerFactory.getLogger(KDF.class);
    private SHA256Digest md;
    private int keyLength;

    /**
     * Create a new Key Derivation Function.
     *
     * @throws GeneralSecurityException
     */
    public KDF() throws GeneralSecurityException {
        md = new SHA256Digest();
        keyLength = 32;
    }

    /**
     * Key Derivation Function.
     *
     * @param md        MessageDigest
     * @param keyLength Key length
     */
    public KDF(SHA256Digest md, int keyLength) {
        this.md = md;
        this.keyLength = keyLength;
    }

    /**
     * Derive key for encryption.
     *
     * @param secret Secret
     * @return Key for message en/decryption (Key_PI)
     */
    public byte[] derivePI(byte[] secret) {
        return derive(secret, (byte) 3, null);
    }

    /**
     * Derive key for message authentication.
     *
     * @param secret Secret
     * @return Key for message authentication (Key_MAC)
     */
    public byte[] deriveMAC(byte[] secret) {
        return derive(secret, (byte) 2, null);
    }

    /**
     * Derive key for message authentication.
     *
     * @param secret Secret
     * @param nonce  Nonce
     * @return Key for message authentication (Key_MAC)
     */
    public byte[] deriveMAC(byte[] secret, byte[] nonce) {
        return derive(secret, (byte) 2, nonce);
    }

    /**
     * Derive key for message encryption.
     *
     * @param secret Secret
     * @return Key for message encryption (Key_ENC)
     */
    public byte[] deriveENC(byte[] secret) {
        return derive(secret, (byte) 1, null);
    }

    /**
     * Derive key for message encryption.
     *
     * @param secret Secret
     * @param nonce  Nonce
     * @return Key for message encryption (Key_ENC)
     */
    public byte[] deriveENC(byte[] secret, byte[] nonce) {
        return derive(secret, (byte) 1, nonce);
    }

    private byte[] derive(byte[] secret, byte counter, byte[] nonce) {
        byte[] c = {(byte) 0x00, (byte) 0x00, (byte) 0x00, counter};
        byte[] key = new byte[keyLength];

        final SHA256Digest sha256 = new SHA256Digest();
        sha256.update(secret, 0, secret.length);
        sha256.update(c, 0, c.length);
        sha256.doFinal(key, 0);

        return key;
    }

}
