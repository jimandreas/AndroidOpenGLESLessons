package com.learnopengles.android.livewallpaper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.learnopengles.android.util.LoggerConfig;

import timber.log.Timber;

public abstract class GLWallpaperService extends WallpaperService {

	public class GLEngine extends Engine {
		class WallpaperGLSurfaceView extends GLSurfaceView {

			WallpaperGLSurfaceView(Context context) {
				super(context);

				if (LoggerConfig.ON) {
					Timber.d("WallpaperGLSurfaceView(%s)", context);
				}
			}

			@Override
			public SurfaceHolder getHolder() {
				if (LoggerConfig.ON) {
					Timber.d("getHolder(): returning %s", getSurfaceHolder());
				}

				return getSurfaceHolder();
			}

			public void onDestroy() {
				if (LoggerConfig.ON) {
					Timber.d("onDestroy()");
				}

				super.onDetachedFromWindow();
			}
		}

		private WallpaperGLSurfaceView glSurfaceView;
		private boolean rendererHasBeenSet;

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			if (LoggerConfig.ON) {
				Timber.d("onCreate(%s)", surfaceHolder);
			}

			super.onCreate(surfaceHolder);

			glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (LoggerConfig.ON) {
				Timber.d("onVisibilityChanged(%s)", visible);
			}

			super.onVisibilityChanged(visible);

			if (rendererHasBeenSet) {
				if (visible) {
					glSurfaceView.onResume();
				} else {
					glSurfaceView.onPause();
				}
			}
		}

		@Override
		public void onDestroy() {
			if (LoggerConfig.ON) {
				Timber.d("onDestroy()");
			}

			super.onDestroy();
			glSurfaceView.onDestroy();
		}

		protected void setRenderer(Renderer renderer) {
			if (LoggerConfig.ON) {
				Timber.d("setRenderer(%s)", renderer);
			}

			glSurfaceView.setRenderer(renderer);
			rendererHasBeenSet = true;
		}

		@SuppressLint("ObsoleteSdkInt")
		protected void setPreserveEGLContextOnPause(boolean preserve) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				if (LoggerConfig.ON) {
					Timber.d("setPreserveEGLContextOnPause(%s)", preserve);
				}

				glSurfaceView.setPreserveEGLContextOnPause(preserve);
			}
		}

		protected void setEGLContextClientVersion(int version) {
			if (LoggerConfig.ON) {
				Timber.d("setEGLContextClientVersion(%s)", version);
			}

			glSurfaceView.setEGLContextClientVersion(version);
		}
	}
}
