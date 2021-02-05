/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.embed.swing.oldimpl;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.embed.swing.DisposerRecord;
import com.sun.javafx.embed.swing.FXDnD;
import com.sun.javafx.embed.swing.SwingCursors;
import com.sun.javafx.embed.swing.SwingNodeHelper;
import com.sun.javafx.embed.swing.SwingNodeInterop;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.stage.WindowHelper;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javax.swing.JComponent;
import sun.awt.UngrabEvent;
import sun.swing.JLightweightFrame;
import sun.swing.LightweightContent;

public class SwingNodeInteropO extends SwingNodeInterop {

    private volatile JLightweightFrame lwFrame;

    /**
     * Calls LightweightFrameWrapper.notifyDisplayChanged.
     * Must be called on EDT only.
     */
    private static OptionalMethod<JLightweightFrame> jlfNotifyDisplayChanged;
    private static OptionalMethod<JLightweightFrame> jlfOverrideNativeWindowHandle;

    static {
        jlfNotifyDisplayChanged = new OptionalMethod<>(JLightweightFrame.class,
                "notifyDisplayChanged", Double.TYPE, Double.TYPE);
        if (!jlfNotifyDisplayChanged.isSupported()) {
            jlfNotifyDisplayChanged = new OptionalMethod<>(
                  JLightweightFrame.class,"notifyDisplayChanged", Integer.TYPE);
        }

        jlfOverrideNativeWindowHandle = new OptionalMethod<>(JLightweightFrame.class,
            "overrideNativeWindowHandle", Long.TYPE, Runnable.class);
    }

    public Object createLightweightFrame() {
        lwFrame = new JLightweightFrame();
        return lwFrame;
    }

    public JLightweightFrame getLightweightFrame() { return lwFrame; }

    public MouseEvent createMouseEvent(Object frame,
                            int swingID, long swingWhen, int swingModifiers,
                            int relX, int relY, int absX, int absY,
                            int clickCount, boolean swingPopupTrigger,
                            int swingButton) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        return new java.awt.event.MouseEvent(lwFrame, swingID,
                swingWhen, swingModifiers, relX, relY, absX, absY,
                clickCount, swingPopupTrigger, swingButton);
    }

    public MouseWheelEvent createMouseWheelEvent(Object frame,
                            int swingModifiers, int x, int y, int wheelRotation) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        return new MouseWheelEvent(lwFrame, java.awt.event.MouseEvent.MOUSE_WHEEL,
                       System.currentTimeMillis(), swingModifiers,
                   x, y, 0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
                   1, wheelRotation);
    }

    public KeyEvent createKeyEvent(Object frame,
                                   int swingID, long swingWhen,
                                   int swingModifiers,
                                   int swingKeyCode, char swingChar) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        return new java.awt.event.KeyEvent(lwFrame, swingID,
                    swingWhen, swingModifiers, swingKeyCode,
                    swingChar);
    }

    public AWTEvent createUngrabEvent(Object frame) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        return new UngrabEvent(lwFrame);
    }

    public void overrideNativeWindowHandle(Object frame, long handle, Runnable closeWindow) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        jlfOverrideNativeWindowHandle.invoke(lwFrame, handle, closeWindow);
    }

    public void notifyDisplayChanged(Object frame, double scaleX, double scaleY) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        if (jlfNotifyDisplayChanged.isIntegerApi()) {
            jlfNotifyDisplayChanged.invoke(lwFrame, (int) scaleX);
        } else {
            jlfNotifyDisplayChanged.invoke(lwFrame, scaleX, scaleY);
        }
    }

    public void setHostBounds(Object frame, int windowX, int windowY, int windowW, int windowH) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        jlfSetHostBounds.invoke(lwFrame, windowX, windowY, windowW, windowH);
    }

    public void setContent(Object frame, Object cnt) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        LightweightContent content = (LightweightContent)cnt;
        lwFrame.setContent(content);
    }

    public void setVisible(Object frame, boolean visible) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        lwFrame.setVisible(visible);
    }

    public void setBounds(Object frame, int frameX, int frameY, int frameW, int frameH) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        lwFrame.setBounds(frameX, frameY, frameW, frameH);
    }

    public SwingNodeContent createSwingNodeContent(JComponent content, SwingNode node) {
        return new SwingNodeContent(content, node);
    }

    public DisposerRecord createSwingNodeDisposer(Object frame) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        return new SwingNodeDisposer(lwFrame);
    }

    private static final class OptionalMethod<T> {
        private final Method method;
        private final boolean isIntegerAPI;

        OptionalMethod(Class<T> cls, String name, Class<?>... args) {
            Method m;
            try {
                m = cls.getMethod(name, args);
            } catch (NoSuchMethodException ignored) {
                // This means we're running with older JDK, simply skip the call
                m = null;
            } catch (Throwable ex) {
                throw new RuntimeException("Error when calling " + cls.getName() + ".getMethod('" + name + "').", ex);
            }
            method = m;
            isIntegerAPI = args != null && args.length > 0 &&
                                                        args[0] == Integer.TYPE;
        }

        public boolean isSupported() {
            return method != null;
        }

        public boolean isIntegerApi() {
            return isIntegerAPI;
        }

        public Object invoke(T object, Object... args) {
            if (method != null) {
                try {
                    return method.invoke(object, args);
                } catch (Throwable ex) {
                    throw new RuntimeException("Error when calling " + object.getClass().getName() + "." + method.getName() + "().", ex);
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Calls LightweightFrameWrapper.setHostBounds.
     * Must be called on EDT only.
     */
    private static final OptionalMethod<JLightweightFrame> jlfSetHostBounds =
        new OptionalMethod<>(JLightweightFrame.class, "setHostBounds",
                Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);

    public void emulateActivation(Object frame, boolean activate) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        lwFrame.emulateActivation(activate);
    }

    public void disposeFrame(Object frame) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        lwFrame.dispose();
    }

    public void addWindowFocusListener(Object frame, WindowFocusListener l) {
        JLightweightFrame lwFrame = (JLightweightFrame)frame;
        lwFrame.addWindowFocusListener(l);
    }

    private static class SwingNodeDisposer implements DisposerRecord {
         JLightweightFrame lwFrame;

         SwingNodeDisposer(JLightweightFrame ref) {
             this.lwFrame = ref;
         }
         public void dispose() {
             if (lwFrame != null) {
                 lwFrame.dispose();
                 lwFrame = null;
             }
         }
    }

    private static class SwingNodeContent implements LightweightContent {
        private JComponent comp;
        private volatile FXDnD dnd;
        private WeakReference<SwingNode> swingNodeRef;

        SwingNodeContent(JComponent comp, SwingNode swingNode) {
            this.comp = comp;
            this.swingNodeRef = new WeakReference<SwingNode>(swingNode);
        }
        @Override
        public JComponent getComponent() {
            return comp;
        }
        @Override
        public void paintLock() {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.getPaintLock(swingNode).lock();
            }
        }
        @Override
        public void paintUnlock() {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.getPaintLock(swingNode).unlock();
            }
        }

        @Override
        public void imageBufferReset(int[] data, int x, int y, int width, int height, int linestride) {
            imageBufferReset(data, x, y, width, height, linestride, 1);
        }
        //@Override
        public void imageBufferReset(int[] data, int x, int y, int width, int height, int linestride, int scale) {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.setImageBuffer(swingNode, data, x, y, width, height, linestride, scale, scale);
            }
        }
        @Override
        public void imageBufferReset(int[] data, int x, int y, int width, int height, int linestride, double scaleX, double scaleY) {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.setImageBuffer(swingNode, data, x, y, width, height, linestride, scaleX, scaleY);
            }
        }
        @Override
        public void imageReshaped(int x, int y, int width, int height) {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.setImageBounds(swingNode, x, y, width, height);
            }
        }
        @Override
        public void imageUpdated(int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight) {
            SwingNode swingNode = swingNodeRef.get();
            if (swingNode != null) {
                SwingNodeHelper.repaintDirtyRegion(swingNode, dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            }
        }
        @Override
        public void focusGrabbed() {
            SwingNodeHelper.runOnFxThread(() -> {
                // On X11 grab is limited to a single XDisplay connection,
                // so we can't delegate it to another GUI toolkit.
                if (PlatformUtil.isLinux()) return;

                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    Scene scene = swingNode.getScene();
                    if (scene != null &&
                            scene.getWindow() != null &&
                            WindowHelper.getPeer(scene.getWindow()) != null) {
                        WindowHelper.getPeer(scene.getWindow()).grabFocus();
                        SwingNodeHelper.setGrabbed(swingNode, true);
                    }
                }
            });
        }
        @Override
        public void focusUngrabbed() {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    Scene scene = swingNode.getScene();
                    SwingNodeHelper.ungrabFocus(swingNode, false);
                }
            });
        }
        @Override
        public void preferredSizeChanged(final int width, final int height) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    SwingNodeHelper.setSwingPrefWidth(swingNode, width);
                    SwingNodeHelper.setSwingPrefHeight(swingNode, height);
                    NodeHelper.notifyLayoutBoundsChanged(swingNode);
                }
            });
        }
        @Override
        public void maximumSizeChanged(final int width, final int height) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    SwingNodeHelper.setSwingMaxWidth(swingNode, width);
                    SwingNodeHelper.setSwingMaxHeight(swingNode, height);
                    NodeHelper.notifyLayoutBoundsChanged(swingNode);
                }
            });
        }
        @Override
        public void minimumSizeChanged(final int width, final int height) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    SwingNodeHelper.setSwingMinWidth(swingNode, width);
                    SwingNodeHelper.setSwingMinHeight(swingNode, height);
                    NodeHelper.notifyLayoutBoundsChanged(swingNode);
                }
            });
        }

        //@Override
        public void setCursor(Cursor cursor) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    swingNode.setCursor(SwingCursors.embedCursorToCursor(cursor));
                }
            });
        }

        private void initDnD() {
            // This is a part of AWT API, so the method may be invoked on any thread
            synchronized (SwingNodeContent.this) {
                if (this.dnd == null) {
                    SwingNode swingNode = swingNodeRef.get();
                    if (swingNode != null) {
                        this.dnd = new FXDnD(swingNode);
                    }
                }
            }
        }

        //@Override
        public synchronized <T extends DragGestureRecognizer> T createDragGestureRecognizer(
                Class<T> abstractRecognizerClass,
                DragSource ds, Component c, int srcActions,
                DragGestureListener dgl)
        {
            initDnD();
            return dnd.createDragGestureRecognizer(abstractRecognizerClass, ds, c, srcActions, dgl);
        }

        //@Override
        public DragSourceContextPeer createDragSourceContext(DragGestureEvent dge) throws InvalidDnDOperationException
        {
            initDnD();
            return (DragSourceContextPeer)dnd.createDragSourceContext(dge);
        }

        //@Override
        public void addDropTarget(DropTarget dt) {
            initDnD();
            dnd.addDropTarget(dt);
        }

        //@Override
        public void removeDropTarget(DropTarget dt) {
            initDnD();
            dnd.removeDropTarget(dt);
        }
    }

}
