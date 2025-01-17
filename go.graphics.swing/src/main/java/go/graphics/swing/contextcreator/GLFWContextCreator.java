/*******************************************************************************
 * Copyright (c) 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package go.graphics.swing.contextcreator;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import javax.swing.SwingUtilities;

import go.graphics.UIPoint;
import go.graphics.event.command.EModifier;
import go.graphics.event.interpreter.AbstractEventConverter;
import go.graphics.swing.ContextContainer;

public class GLFWContextCreator extends AsyncContextCreator {

	private final GLFWEventConverter event_converter;

	public GLFWContextCreator(ContextContainer container, boolean debug) {
		super(container, debug);
		event_converter = new GLFWEventConverter();
	}

	protected long glfw_wnd;
	private GLFWErrorCallback ec;

	public void async_init() {
		if(debug) {
			ec = GLFWErrorCallback.createPrint(System.err);
			GLFW.glfwSetErrorCallback(ec);
		}

		if(!GLFW.glfwInit()) throw new Error("glfwInit() failed!");

		configureWindow();
		synchronized (wnd_lock) {
			glfw_wnd = GLFW.glfwCreateWindow(width, height, "lwjgl-offscreen", 0, 0);
			setupContext();
			try {
				parent.resizeContext(width, height);
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}

		event_converter.registerCallbacks();
	}

	protected void configureWindow() {
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, debug ? GLFW.GLFW_TRUE : GLFW.GLFW_DONT_CARE);
		GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 1);
	}

	protected void setupContext() {
		GLFW.glfwMakeContextCurrent(glfw_wnd);
		GLFW.glfwSwapInterval(0);
		parent.wrapNewGLContext();
	}

	public void async_set_size(int width, int height) {
		GLFW.glfwSetWindowSize(glfw_wnd, width, height);
	}

	private long glfw_resize_time = -1;
	private int glfw_width, glfw_height;

	public void async_refresh() {
		if(glfw_resize_time != -1) {
			if(glfw_resize_time+10 <= System.currentTimeMillis()) {
				Window wnd = SwingUtilities.windowForComponent(canvas);
				int dw = wnd.getWidth()-canvas.getWidth();
				int dh = wnd.getHeight()-canvas.getHeight();

				wnd.setSize(glfw_width+dw, glfw_height+dh);

				glfw_resize_time = -1;

				change_res = true;
				async_resized = true;
				new_width = glfw_width+dw;
				new_height = glfw_height+dh;
			}
		}
		GLFW.glfwPollEvents();
	}

	public void async_swapbuffers() {
		GLFW.glfwSwapBuffers(glfw_wnd);
	}

	public void async_stop() {
		GLFW.glfwTerminate();
	}

	private static final HashMap<Integer, String> keys = new HashMap<>();
	private static final HashMap<Integer, EModifier> mods = new HashMap<>();

	static {
		keys.put(GLFW.GLFW_KEY_LEFT, "LEFT");
		keys.put(GLFW.GLFW_KEY_RIGHT, "RIGHT");
		keys.put(GLFW.GLFW_KEY_UP, "UP");
		keys.put(GLFW.GLFW_KEY_DOWN, "DOWN");
		keys.put(GLFW.GLFW_KEY_PAUSE, "PAUSE");

		keys.put(GLFW.GLFW_KEY_F1, "F1");
		keys.put(GLFW.GLFW_KEY_F2, "F2");
		keys.put(GLFW.GLFW_KEY_F3, "F3");
		keys.put(GLFW.GLFW_KEY_F4, "F4");
		keys.put(GLFW.GLFW_KEY_F5, "F5");
		keys.put(GLFW.GLFW_KEY_F6, "F6");
		keys.put(GLFW.GLFW_KEY_F7, "F7");
		keys.put(GLFW.GLFW_KEY_F8, "F8");
		keys.put(GLFW.GLFW_KEY_F9, "F9");
		keys.put(GLFW.GLFW_KEY_F10, "F10");
		keys.put(GLFW.GLFW_KEY_F11, "F11");
		keys.put(GLFW.GLFW_KEY_F12, "F12");

		keys.put(GLFW.GLFW_KEY_DELETE, "DELETE");
		keys.put(GLFW.GLFW_KEY_ESCAPE, "ESCAPE");
		keys.put(GLFW.GLFW_KEY_BACKSPACE, "BACK_SPACE");
		keys.put(GLFW.GLFW_KEY_SPACE, " ");

		mods.put(GLFW.GLFW_KEY_LEFT_SHIFT, EModifier.SHIFT);
		mods.put(GLFW.GLFW_KEY_RIGHT_SHIFT, EModifier.SHIFT);

		mods.put(GLFW.GLFW_KEY_LEFT_ALT, EModifier.ALT);
		mods.put(GLFW.GLFW_KEY_RIGHT_ALT, EModifier.ALT);

		mods.put(GLFW.GLFW_KEY_LEFT_CONTROL, EModifier.CTRL);
		mods.put(GLFW.GLFW_KEY_RIGHT_CONTROL, EModifier.CTRL);
	}

	private class GLFWEventConverter extends  AbstractEventConverter {

		private UIPoint last_point = new UIPoint(0, 0);

		private final EnumSet<EModifier> activeMods = EnumSet.noneOf(EModifier.class);

		private GLFWKeyCallbackI key_callback = (window, key, scancode, action, modsUNUSUED) -> {
			String name = GLFW.glfwGetKeyName(key, scancode);
			if(name == null) {
				name = keys.get(key);
			}

			EModifier mod = mods.get(key);

			if(mod != null) {
				synchronized (activeMods) {
					if(action == GLFW.GLFW_PRESS) {
						activeMods.add(mod);
					} else {
						activeMods.remove(mod);
					}
				}
			}

			if(action == GLFW.GLFW_PRESS) {
				startKeyEvent(name);
			} else if(action == GLFW.GLFW_RELEASE){
				endKeyEvent(name);
			}
		};

		private double presstime = -1;

		private GLFWMouseButtonCallbackI mouse_callback = (window, button, action, mods) -> {

			UIPoint point = last_point;

			if(action == GLFW.GLFW_PRESS) {
				switch(button) {
					case GLFW.GLFW_MOUSE_BUTTON_1:
						startDraw(point);
						break;
					case GLFW.GLFW_MOUSE_BUTTON_2:
						presstime = GLFW.glfwGetTime();
					case GLFW.GLFW_MOUSE_BUTTON_3:
						startPan(point);
						break;
				}
			} else {
				switch(button) {
					case GLFW.GLFW_MOUSE_BUTTON_1:
						endDraw(point);
						break;
					case GLFW.GLFW_MOUSE_BUTTON_2:
						presstime = -1;
					case GLFW.GLFW_MOUSE_BUTTON_3:
						endPan(point);
						break;
				}
			}
		};

		private GLFWCursorEnterCallbackI cursorenter_callback = (window, entered) -> {
			if(entered) {
				startHover(last_point);
			} else {
				endHover(last_point);
			}
		};

		private GLFWCursorPosCallbackI cursorpos_callback = (window, xpos, ypos) -> {
			last_point = new UIPoint(xpos, height - ypos);
			updateHoverPosition(last_point);

			if(presstime+0.1<GLFW.glfwGetTime()) {
				updatePanPosition(last_point);
				updateDrawPosition(last_point);
			}
		};

		private GLFWScrollCallbackI scroll_callback = (window, xoffset, yoffset) -> {
			float factor = (float) Math.exp(yoffset / 20.0);

			startZoom();
			endZoomEvent(factor, last_point);
		};

		private GLFWWindowSizeCallbackI size_callback = new GLFWWindowSizeCallbackI() {
			@Override
			public void invoke(long window, int width, int height) {
				if(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == 0) return;

				synchronized (wnd_lock) {
					if(GLFWContextCreator.this.width == width && GLFWContextCreator.this.height == height) {
						glfw_resize_time = -1;
						return;
					}

					glfw_resize_time = System.currentTimeMillis();
					glfw_width = width;
					glfw_height = height;
				}
			}
		};

		private GLFWWindowCloseCallbackI close_callback = window -> {
			Window wnd = SwingUtilities.windowForComponent(canvas);
			wnd.dispatchEvent(new WindowEvent(wnd, WindowEvent.WINDOW_CLOSING));
		};

		private GLFWEventConverter() {
			super(parent);

			addReplaceRule(new EventReplacementRule(ReplacableEvent.DRAW, Replacement.COMMAND_SELECT, 5, 10));
			addReplaceRule(new EventReplacementRule(ReplacableEvent.PAN, Replacement.COMMAND_ACTION, 5, 10));
		}

		@Override
		protected Set<EModifier> getCurrentModifiers() {
			synchronized(activeMods) {
				return activeMods.clone();
			}
		}

		private void registerCallbacks() {
			GLFW.glfwSetKeyCallback(glfw_wnd, key_callback);
			GLFW.glfwSetMouseButtonCallback(glfw_wnd, mouse_callback);
			GLFW.glfwSetScrollCallback(glfw_wnd, scroll_callback);
			GLFW.glfwSetCursorEnterCallback(glfw_wnd, cursorenter_callback);
			GLFW.glfwSetCursorPosCallback(glfw_wnd, cursorpos_callback);
			GLFW.glfwSetWindowSizeCallback(glfw_wnd, size_callback);
			GLFW.glfwSetWindowCloseCallback(glfw_wnd, close_callback);
		}
	}
}
