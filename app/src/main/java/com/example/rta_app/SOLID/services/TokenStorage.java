package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {

    private static final String TAG = "ApiKeyStorage";
    private static final String PREF_FILE = "secure_prefs";
    private static final String KEY_APIKEY = "apiKey";
    private static final String ANDROIDX_KEYSET_PREF = "__androidx_security_crypto_encrypted_prefs__";
    private static final String FALLBACK_PREF_FILE = "secure_prefs_fallback";

    private static final Object LOCK = new Object();
    private static volatile SharedPreferences cachedPrefs;
    private static volatile boolean usingFallbackPrefs = false;

    private final Context appContext;

    public TokenStorage(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static void warmUpAsync(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Thread t = new Thread(() -> {
            try {
                new TokenStorage(appContext).getPrefs();
            } catch (Exception e) {
                Log.e(TAG, "warmUpAsync(): falha ao inicializar storage", e);
            }
        }, "token-storage-warmup");
        t.setDaemon(true);
        t.start();
    }

    private SharedPreferences getPrefs() {
        SharedPreferences local = cachedPrefs;
        if (local != null) {
            return local;
        }

        synchronized (LOCK) {
            local = cachedPrefs;
            if (local != null) {
                return local;
            }

            try {
                local = createEncryptedPrefs(appContext);
                usingFallbackPrefs = false;
                Log.i(TAG, "EncryptedSharedPreferences inicializado com sucesso");
            } catch (Exception firstError) {
                Log.e(TAG, "Falha ao inicializar EncryptedSharedPreferences. Tentando resetar o keyset.", firstError);
                resetEncryptedPrefs(appContext);

                try {
                    local = createEncryptedPrefs(appContext);
                    usingFallbackPrefs = false;
                    Log.i(TAG, "EncryptedSharedPreferences recriado com sucesso após reset");
                } catch (Exception secondError) {
                    Log.e(TAG, "Falha definitiva no storage criptografado. Usando fallback local.", secondError);
                    local = appContext.getSharedPreferences(FALLBACK_PREF_FILE, Context.MODE_PRIVATE);
                    usingFallbackPrefs = true;
                }
            }

            cachedPrefs = local;
            return local;
        }
    }

    private static void resetEncryptedPrefs(Context context) {
        try {
            context.deleteSharedPreferences(PREF_FILE);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao apagar prefs criptografados", e);
        }

        try {
            context.deleteSharedPreferences(ANDROIDX_KEYSET_PREF);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao apagar keyset interno", e);
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

    public void saveApiKey(@Nullable String apiKey) {
        String safeValue = apiKey == null ? "" : apiKey.trim();
        getPrefs().edit().putString(KEY_APIKEY, safeValue).apply();
        Log.d(TAG, "API Key salva (vazia? " + safeValue.isEmpty() + ", fallback? " + usingFallbackPrefs + ")");
    }

    @NonNull
    public String getApiKey() {
        String key = getPrefs().getString(KEY_APIKEY, "");
        return key == null ? "" : key;
    }

    public boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }

    public void clear() {
        getPrefs().edit().remove(KEY_APIKEY).apply();
        Log.d(TAG, "API Key removida do storage");
    }
}
