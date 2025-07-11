package com.example.rta_app.SOLID.services;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {
    private static final String TAG           = "TokenStorage";
    private static final String PREF_FILE     = "secure_prefs";
    private static final String KEY_ACCESS    = "accessToken";
    private static final String KEY_REFRESH   = "refreshToken";

    private final SharedPreferences securePrefs;

    public TokenStorage(Context context){
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Inicializa o EncryptedSharedPreferences
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


        Log.d(TAG, "EncryptedSharedPreferences inicializado");
    }

    /**
     * Salva os dois tokens (substitui os valores anteriores).
     */
    public void saveTokens(String accessToken, String refreshToken) {
        securePrefs.edit()
                .putString(KEY_ACCESS,  accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .apply();

        Log.d(TAG, "Tokens salvos com sucesso");
    }

    /**
     * Retorna um JSONObject { "accessToken": ..., "refreshToken": ... }
     * ou lança Exception se não encontrar.
     */
    public JSONObject getTokens() throws Exception {
        String access  = securePrefs.getString(KEY_ACCESS, null);
        String refresh = securePrefs.getString(KEY_REFRESH, null);

        if (access == null || refresh == null) {
            throw new Exception("Tokens não encontrados");
        }

        return new JSONObject()
                .put("accessToken", access)
                .put("refreshToken", refresh);
    }

    /** Retorna só o accessToken (ou null). */
    public String getAccessToken() {
        return securePrefs.getString(KEY_ACCESS, null);
    }

    /** Retorna só o refreshToken (ou null). */
    public String getRefreshToken() {
        return securePrefs.getString(KEY_REFRESH, null);
    }

    /** Limpa ambos os tokens do storage. */
    public void clear() {
        securePrefs.edit().clear().apply();
        Log.d(TAG, "Tokens removidos do storage");
    }
}
