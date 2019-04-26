package com.bitcoin.wallet.btc.utils;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.TextureView;
import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;
import java.util.*;

public class CameraManager {
    private static final int MIN_FRAME_SIZE = 240;
    private static final int MAX_FRAME_SIZE = 600;
    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
    private static final int MAX_PREVIEW_PIXELS = 1280 * 720;
    private static final Comparator<Camera.Size> numPixelComparator = new Comparator<Camera.Size>() {
        @Override
        public int compare(final Camera.Size size1, final Camera.Size size2) {
            final int pixels1 = size1.height * size1.width;
            final int pixels2 = size2.height * size2.width;

            if (pixels1 < pixels2)
                return 1;
            else if (pixels1 > pixels2)
                return -1;
            else
                return 0;
        }
    };
    private Camera camera;
    private Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    private Camera.Size cameraResolution;
    private Rect frame;
    private RectF framePreview;

    private static Camera.Size findBestPreviewSizeValue(final Camera.Parameters parameters, int width, int height) {
        if (height > width) {
            final int temp = width;
            width = height;
            height = temp;
        }

        final float screenAspectRatio = (float) width / (float) height;

        final List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null)
            return parameters.getPreviewSize();

        // sort by size, descending
        final List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewSizes, numPixelComparator);

        Camera.Size bestSize = null;
        float diff = Float.POSITIVE_INFINITY;

        for (final Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            final int realWidth = supportedPreviewSize.width;
            final int realHeight = supportedPreviewSize.height;
            final int realPixels = realWidth * realHeight;
            if (realPixels < MIN_PREVIEW_PIXELS || realPixels > MAX_PREVIEW_PIXELS)
                continue;

            final boolean isCandidatePortrait = realWidth < realHeight;
            final int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            final int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == width && maybeFlippedHeight == height)
                return supportedPreviewSize;

            final float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            final float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = supportedPreviewSize;
                diff = newDiff;
            }
        }

        if (bestSize != null)
            return bestSize;
        else
            return parameters.getPreviewSize();
    }

    @SuppressLint("InlinedApi")
    private static void setDesiredCameraParameters(final Camera camera, final Camera.Size cameraResolution) {
        final Camera.Parameters parameters = camera.getParameters();
        if (parameters == null)
            return;

        final List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        final String focusMode = findValue(supportedFocusModes, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, Camera.Parameters.FOCUS_MODE_AUTO,
                Camera.Parameters.FOCUS_MODE_MACRO);
        if (focusMode != null)
            parameters.setFocusMode(focusMode);

        parameters.setPreviewSize(cameraResolution.width, cameraResolution.height);

        camera.setParameters(parameters);
    }

    private static boolean getTorchEnabled(final Camera camera) {
        if (camera == null) {
            return false;
        }
        try {
            final Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                final String flashMode = camera.getParameters().getFlashMode();
                return flashMode != null && (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)
                        || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }

        } catch (RuntimeException ignored) {
        }
        return false;
    }

    private static void setTorchEnabled(final Camera camera, final boolean enabled) {
        final Camera.Parameters parameters = camera.getParameters();

        final List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes != null) {
            final String flashMode;
            if (enabled)
                flashMode = findValue(supportedFlashModes, Camera.Parameters.FLASH_MODE_TORCH,
                        Camera.Parameters.FLASH_MODE_ON);
            else
                flashMode = findValue(supportedFlashModes, Camera.Parameters.FLASH_MODE_OFF);

            if (flashMode != null) {
                camera.cancelAutoFocus(); // autofocus can cause conflict

                parameters.setFlashMode(flashMode);
                camera.setParameters(parameters);
            }
        }
    }

    private static String findValue(final Collection<String> values, final String... valuesToFind) {
        for (final String valueToFind : valuesToFind)
            if (values.contains(valueToFind))
                return valueToFind;

        return null;
    }

    public Rect getFrame() {
        return frame;
    }

    public RectF getFramePreview() {
        return framePreview;
    }

    public int getFacing() {
        return cameraInfo.facing;
    }

    public int getOrientation() {
        return cameraInfo.orientation;
    }

    public Camera open(final TextureView textureView, final int displayOrientation) throws IOException {
        final int cameraId = determineCameraId();
        Camera.getCameraInfo(cameraId, cameraInfo);

        camera = Camera.open(cameraId);

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            camera.setDisplayOrientation((720 - displayOrientation - cameraInfo.orientation) % 360);
        else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            camera.setDisplayOrientation((720 - displayOrientation + cameraInfo.orientation) % 360);
        else
            throw new IllegalStateException("facing: " + cameraInfo.facing);

        camera.setPreviewTexture(textureView.getSurfaceTexture());

        final Camera.Parameters parameters = camera.getParameters();

        cameraResolution = findBestPreviewSizeValue(parameters, textureView.getWidth(), textureView.getHeight());

        final int width = textureView.getWidth();
        final int height = textureView.getHeight();

        final int rawSize = Math.min(width * 2 / 3, height * 2 / 3);
        final int frameSize = Math.max(MIN_FRAME_SIZE, Math.min(MAX_FRAME_SIZE, rawSize));

        final int leftOffset = (width - frameSize) / 2;
        final int topOffset = (height - frameSize) / 2;
        frame = new Rect(leftOffset, topOffset, leftOffset + frameSize, topOffset + frameSize);
        if (width > height) { // landscape
            framePreview = new RectF(frame.left * cameraResolution.width / width,
                    frame.top * cameraResolution.height / height, frame.right * cameraResolution.width / width,
                    frame.bottom * cameraResolution.height / height);
        } else { // portrait
            framePreview = new RectF(frame.top * cameraResolution.width / height,
                    frame.left * cameraResolution.height / width, frame.bottom * cameraResolution.width / height,
                    frame.right * cameraResolution.height / width);
        }

        final String savedParameters = parameters == null ? null : parameters.flatten();

        try {
            setDesiredCameraParameters(camera, cameraResolution);
        } catch (final RuntimeException x) {
            if (savedParameters != null) {
                final Camera.Parameters parameters2 = camera.getParameters();
                parameters2.unflatten(savedParameters);
                try {
                    camera.setParameters(parameters2);
                    setDesiredCameraParameters(camera, cameraResolution);
                } catch (final RuntimeException x2) {

                }
            }
        }

        try {
            camera.startPreview();
            return camera;
        } catch (final RuntimeException x) {

            camera.release();
            throw x;
        }
    }

    private int determineCameraId() {
        final int cameraCount = Camera.getNumberOfCameras();
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        // prefer back-facing camera
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;
        }

        // fall back to front-facing camera
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return i;
        }

        return -1;
    }

    public void close() {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (final RuntimeException x) {

            }

            camera.release();
        }
    }

    public void requestPreviewFrame(final Camera.PreviewCallback callback) {
        try {
            camera.setOneShotPreviewCallback(callback);
        } catch (final RuntimeException x) {

        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(final byte[] data) {
        return new PlanarYUVLuminanceSource(data, cameraResolution.width, cameraResolution.height,
                (int) framePreview.left, (int) framePreview.top, (int) framePreview.width(),
                (int) framePreview.height(), false);
    }

    public void setTorch(final boolean enabled) {
        if (enabled != getTorchEnabled(camera))
            setTorchEnabled(camera, enabled);
    }
}
