package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {
    private static final String TAG        = "ApiKeyStorage";
    private static final String PREF_FILE  = "secure_prefs";
    private static final String KEY_APIKEY = "apiKey";

    private final SharedPreferences securePrefs;

    public TokenStorage(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Erro ao inicializar EncryptedSharedPreferences", e);
        }

        Log.d(TAG, "EncryptedSharedPreferences inicializado para API Key");
    }

    /** Salva a API Key. */
    public void saveApiKey(String apiKey) {
        securePrefs.edit()
                .putString(KEY_APIKEY, apiKey)
                .apply();
        Log.d(TAG, "API Key salva com sucesso");
    }

    /** Retorna a API Key (ou null se não existir). */
    public String getApiKey() {
        return securePrefs.getString(KEY_APIKEY, null);
    }

    /** Remove a API Key do storage. */
    public void clear() {
        securePrefs.edit().remove(KEY_APIKEY).apply();
        Log.d(TAG, "API Key removida do storage");
    }
}
