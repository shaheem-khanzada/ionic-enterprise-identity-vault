package com.ionicframework.auth;

import com.bottlerocketstudios.vault.keys.storage.MemoryOnlyKeyStorage;

public class IonicMemoryOnlyKeyStorage extends MemoryOnlyKeyStorage implements IonicKeyStorage {

    public void lock() {
        this.clearKey(null);
    }

    public boolean isLocked() {
        return !this.hasKey(null);
    }
}
