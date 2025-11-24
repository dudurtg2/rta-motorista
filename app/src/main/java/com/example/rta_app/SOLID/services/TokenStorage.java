package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {

    private static final String TAG        = "ApiKeyStorage";

    // Mesmo nome que você já usava
    private static final String PREF_FILE  = "secure_prefs";
    private static final String KEY_APIKEY = "apiKey";

    // Nome do SharedPreferences interno onde o AndroidX guarda o keyset criptografado.
    // Se não existir, o deleteSharedPreferences simplesmente não faz nada.
    private static final String ANDROIDX_KEYSET_PREF = "__androidx_security_crypto_encrypted_prefs__";

    private final SharedPreferences securePrefs;

    public TokenStorage(Context context) {
        this.securePrefs = initEncryptedPrefs(context);
        Log.d(TAG, "EncryptedSharedPreferences inicializado para API Key");
    }

    private SharedPreferences initEncryptedPrefs(Context context) {
        try {
            Log.i(TAG, "Inicializando EncryptedSharedPreferences (primeira tentativa)");
            return createEncryptedPrefs(context);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao inicializar EncryptedSharedPreferences (provavelmente keyset corrompido). Tentando resetar.", e);

            // Apaga prefs criptografadas e o prefs interno do keyset
            try {
                Log.w(TAG, "Apagando SharedPreferences criptografados: " + PREF_FILE);
                context.deleteSharedPreferences(PREF_FILE);

                Log.w(TAG, "Apagando SharedPreferences do keyset interno: " + ANDROIDX_KEYSET_PREF);
                context.deleteSharedPreferences(ANDROIDX_KEYSET_PREF);
            } catch (Exception ex) {
                Log.e(TAG, "Erro ao apagar SharedPreferences/Keyset corrompidos.", ex);
            }

            // Tenta criar tudo de novo do zero
            try {
                Log.i(TAG, "Recriando EncryptedSharedPreferences após reset.");
                return createEncryptedPrefs(context);
            } catch (Exception e2) {
                Log.e(TAG, "Falha DEFINITIVA ao inicializar EncryptedSharedPreferences.", e2);
                throw new RuntimeException("Erro ao inicializar EncryptedSharedPreferences", e2);
            }
        }
    }

    private SharedPreferences createEncryptedPrefs(Context context)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    /** Salva a API Key. */
    public void saveApiKey(@Nullable String apiKey) {
        securePrefs.edit()
                .putString(KEY_APIKEY, apiKey)
                .apply();
        Log.d(TAG, "API Key salva com sucesso (nula? " + (apiKey == null) + ")");
    }

    /** Retorna a API Key (ou null se não existir). */
    @Nullable
    public String getApiKey() {
        String key = securePrefs.getString(KEY_APIKEY, null);
        Log.d(TAG, "getApiKey(): " + key);
        return key;
    }

    /** Remove a API Key do storage. */
    public void clear() {
        securePrefs.edit()
                .remove(KEY_APIKEY)
                .apply();
        Log.d(TAG, "API Key removida do storage");
    }
}
