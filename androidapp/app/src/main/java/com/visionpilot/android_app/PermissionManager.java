package com.visionpilot.android_app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Classe utilitária para centralizar verificação e pedido de permissão de câmera.
 *
 * Comentários:
 * - Static helper: fácil de testar e reusar.
 * - REQUEST_CODE é público para que Activities saibam qual código usar ao comparar resultados.
 */
public final class PermissionManager {

    // código usado ao pedir a permissão (deve ser consistente com onRequestPermissionsResult)
    public static final int REQUEST_CODE = 101;

    // construtor privado para evitar instanciação (classe utilitária)
    private PermissionManager() {}

    /**
     * Verifica se a permissão de câmera já foi concedida.
     * Retorna true se concedida; false caso contrário.
     */
    public static boolean hasCameraPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Pede a permissão de câmera ao usuário.
     * activity: Activity que receberá o resultado em onRequestPermissionsResult.
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{android.Manifest.permission.CAMERA},
                REQUEST_CODE);
    }
}