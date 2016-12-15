package nl.xs4all.pebbe.globe;


import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;

    public MyGLSurfaceView(Context context, Bundle savedInstanceState) {
        super(context);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer();
        mRenderer.restoreInstanceState(savedInstanceState);
        mRenderer.setContext(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void saveInstanceState(Bundle outState) {
        mRenderer.saveInstanceState(outState);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                if (Math.abs(dx) > Math.abs(dy)) {
                    mRenderer.setAngleH(
                            mRenderer.getAngleH() -
                                    dx * TOUCH_SCALE_FACTOR);  // = 180.0f / 320

                } else {
                    float a = mRenderer.getAngleV() + dy * TOUCH_SCALE_FACTOR;
                    if (a > 89.99f) {
                        a = 89.99f;
                    } else if (a < -89.99f) {
                        a = -89.99f;
                    }
                    mRenderer.setAngleV(a);
                }

                requestRender();
                break;

        }
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

}
