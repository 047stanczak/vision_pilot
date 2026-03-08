package com.visionpilot.android_app;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

/**
 * Activity principal — agora enxuta.
 *
 * Comentários:
 * - Esta Activity estende CameraActivity (do OpenCV) e implementa CvCameraViewListener2
 *   para receber callbacks da câmera (start/stop/frame).
 * - Ela só configura a view, trata permissões, ciclo de vida e delega o processamento
 *   de frames para FrameProcessor.
 */
public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    // referência para o componente que mostra o preview da câmera no layout (JavaCameraView)
    private CameraBridgeViewBase cameraBridgeViewBase;

    // classe que encapsula todo o processamento de visão computacional (detecção de cor, desenho)
    private FrameProcessor frameProcessor;

    // código de requisição de permissão (usado ao pedir permissão em runtime)
    private static final int CAMERA_PERMISSION_REQUEST = PermissionManager.REQUEST_CODE;

    // ---------- onCreate: inicialização da Activity ----------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // associa a Activity ao layout XML (activity_main.xml)
        setContentView(R.layout.activity_main);

        // obtém a view de câmera definida no layout (id = camera_view)
        cameraBridgeViewBase = findViewById(R.id.camera_view);

        // registra esta Activity como "ouvinte" da view da câmera: receberemos onCameraFrame, etc.
        cameraBridgeViewBase.setCvCameraViewListener(this);

        // cria o processador de frames (separa responsabilidades: MainActivity não faz processamento)
        frameProcessor = new FrameProcessor();

        // pede permissão de câmera em runtime caso ainda não tenha sido concedida
        // PermissionManager encapsula a checagem / pedido de permissão
        if (!PermissionManager.hasCameraPermission(this)) {
            PermissionManager.requestCameraPermission(this);
        }
    }

    // ---------- onResume: quando a Activity volta a ficar visível ----------
    @Override
    protected void onResume() {
        super.onResume();

        // se não temos permissão, não tentamos ligar a câmera — apenas registramos um aviso
        if (!PermissionManager.hasCameraPermission(this)) {
            Log.w(TAG, "Sem permissão de câmera — aguardando permissão do usuário.");
            return;
        }

        // inicializa o OpenCV de forma "debug" (initDebug carrega libs nativas embutidas).
        // Se der true, podemos habilitar a view da câmera; caso contrário registramos erro.
        if (OpenCVLoader.initDebug()) {
            // habilita o preview (começam a chegar frames e callbacks)
            cameraBridgeViewBase.enableView();
        } else {
            // se OpenCV não carregou, a câmera via OpenCV não funcionará corretamente
            Log.e(TAG, "OpenCV não carregou.");
        }
    }

    // ---------- onPause: Activity não está mais em primeiro plano ----------
    @Override
    protected void onPause() {
        super.onPause();
        // desliga a view da câmera para liberar recurso (camera) — importante!
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
        // também notifica o FrameProcessor para liberar mats nativos, etc.
        frameProcessor.onCameraViewStopped();
    }

    // ---------- onDestroy: limpeza final ----------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mesma limpeza defensiva que em onPause
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
        frameProcessor.onCameraViewStopped();
    }

    // Necessário para CameraActivity: lista das views de câmera usadas por esta Activity.
    // Aqui devolvemos uma lista contendo apenas a cameraBridgeViewBase.
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    // ---------- Callbacks CvCameraViewListener2 (delegam para FrameProcessor) ----------

    /**
     * Chamado quando a câmera inicia: recebe largura e altura do frame.
     * Delegamos para frameProcessor para possível inicialização relacionada ao tamanho.
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        frameProcessor.onCameraViewStarted(width, height);
    }

    /**
     * Chamado quando a câmera para. Delegamos para o frameProcessor.
     */
    @Override
    public void onCameraViewStopped() {
        frameProcessor.onCameraViewStopped();
    }

    /**
     * Chamado para cada frame da câmera.
     * Recebe um objeto inputFrame (fornecido pelo OpenCV) que permite extrair o frame em RGBA/BGR/etc.
     * Retorna a Mat que será exibida no preview — aqui pedimos ao frameProcessor para processar.
     *
     * Observação: o frame passado é gerenciado pelo OpenCV; não se deve chamar release() nele.
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();          // pega o frame atual em RGBA
        return frameProcessor.processFrame(rgba); // processa e devolve o frame para preview
    }

    // ---------- Resultado do pedido de permissão (quando o usuário responde) ----------
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // verificamos se é a requisição que emitimos (REQUEST_CODE)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            // grantResults contém os resultados; 0 = permitido
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                Log.i(TAG, "Permissão de câmera concedida pelo usuário.");
                // como permissões foram concedidas, podemos habilitar a view (ligar preview)
                if (cameraBridgeViewBase != null) cameraBridgeViewBase.enableView();
            } else {
                Log.w(TAG, "Permissão de câmera negada pelo usuário.");
            }
        }
    }
}