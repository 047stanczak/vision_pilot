package com.visionpilot.android_app;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável por todo o processamento de imagem (visão computacional).
 *
 * Comentários gerais:
 * - Mantém Mats reutilizáveis (hsv, mask, hierarchy) para reduzir alocação por frame,
 *   o que melhora desempenho em tempo real.
 * - Encapsula conversão de cor, threshold (inRange), detecção de contornos e desenho
 *   (retângulo, mira e círculo).
 * - Possui um minContourArea para filtrar ruídos pequenos.
 */
public class FrameProcessor {

    private static final String TAG = "FrameProcessor";

    // Mats reaproveitáveis para evitar alocações por frame (são liberados em onCameraViewStopped)
    private Mat hsv;
    private Mat mask;
    private Mat hierarchy;

    // Limites HSV que representam "azul" por padrão.
    // Scalar(h, s, v). Esses valores definem um cubo no espaço HSV.
    // ATENÇÃO: dependendo do formato do frame (RGB vs BGR) pode ser necessário ajustar COLOR_* e limites.
    private final Scalar lowerBlue = new Scalar(100, 150, 50);
    private final Scalar upperBlue = new Scalar(140, 255, 255);

    // minimun area para considerar um contorno como "válido" (filtra ruído)
    private final double minContourArea = 200.0;

    // construtor: inicializa as Mats que serão reutilizadas
    public FrameProcessor() {
        hsv = new Mat();
        mask = new Mat();
        hierarchy = new Mat();
    }

    /**
     * Chamado quando a câmera inicia (passado width/height).
     * Use este método para inicializações dependentes de resolução, se necessário.
     */
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera started: " + width + "x" + height);
        // Por ora não precisamos usar width/height — mas é o lugar certo para preparar buffers.
    }

    /**
     * Chamado quando a câmera para ou a Activity é pausada/destroída.
     * Importante liberar as Mats nativas para evitar vazamentos de memória nativa.
     */
    public void onCameraViewStopped() {
        if (hsv != null) {
            hsv.release();  // libera memória nativa associada
            hsv = null;
        }
        if (mask != null) {
            mask.release();
            mask = null;
        }
        if (hierarchy != null) {
            hierarchy.release();
            hierarchy = null;
        }
    }

    /**
     * processFrame:
     * - Recebe um frame RGBA (Mat) vindo do callback da câmera.
     * - Converte para HSV e aplica inRange para isolar a cor azul.
     * - Encontra contornos na máscara resultante.
     * - Seleciona o maior contorno acima de minContourArea.
     * - Desenha retângulo, mira (linhas) e círculo no centro do maior contorno.
     * - Retorna o mesmo Mat (modificado) para ser exibido no preview.
     *
     * Observações importantes:
     * - Não liberar o Mat 'frame' aqui: ele é gerenciado pela pipeline de câmera do OpenCV.
     * - Reuso das Mats (hsv/mask/hierarchy) reduz alocações por frame.
     */
    public Mat processFrame(Mat frame) {
        // validações simples
        if (frame == null || frame.empty()) return frame;

        // converte o frame atual de RGB para HSV, armazenando em 'hsv' (reutilizável)
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_RGB2HSV);

        // cria uma máscara onde pixels dentro do intervalo lowerBlue..upperBlue ficam com valor 255
        Core.inRange(hsv, lowerBlue, upperBlue, mask);

        // encontra contornos na máscara (contours é criada por frame)
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // procura o maior contorno que exceda minContourArea
        double maxArea = 0;
        MatOfPoint largest = null;

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);  // calcula área do contorno
            // atualiza maior se maior que maxArea e acima do limiar minimo
            if (area > maxArea && area >= minContourArea) {
                maxArea = area;
                largest = c;
            }
        }

        // se encontramos um contorno válido, desenhamos no frame
        if (largest != null) {
            // boundingRect: retângulo que envolve o contorno
            Rect rect = Imgproc.boundingRect(largest);

            // calcula centro do retângulo
            int centerX = rect.x + rect.width / 2;
            int centerY = rect.y + rect.height / 2;
            Point center = new Point(centerX, centerY);

            // desenha um retângulo verde ao redor do objeto detectado
            Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0), 2);

            // desenha uma "mira" (crosshair) com linhas horizontais/verticais no centro
            Imgproc.line(frame, new Point(centerX - 40, centerY), new Point(centerX + 40, centerY), new Scalar(255, 0, 0), 3);
            Imgproc.line(frame, new Point(centerX, centerY - 40), new Point(centerX, centerY + 40), new Scalar(255, 0, 0), 3);

            // desenha um círculo no centro do objeto
            Imgproc.circle(frame, center, 10, new Scalar(255, 0, 0), 3);
        }

        // devolve o frame (modificado) para ser mostrado no preview
        return frame;
    }

    // Nota: os limites lowerBlue/upperBlue são 'final' neste exemplo.
    // Se quiser calibrar em runtime, transforme-os em campos mutáveis e exponha setters.
}