package com.ionicframework.auth;

import com.bottlerocketstudios.vault.keys.storage.KeyStorage;

/*
 Extend the key storage interface to have a notion of locks
 */
public interface IonicKeyStorage extends KeyStorage {

    void lock();

    boolean isLocked();
}
