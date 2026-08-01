import { Capacitor } from '@capacitor/core';

export async function setupNativeChrome() {
  if (!Capacitor.isNativePlatform()) return;
  const { StatusBar, Style } = await import('@capacitor/status-bar');
  // Inset the WebView below the status bar instead of drawing under it —
  // Android 15+ (targetSdk 35+) enforces edge-to-edge by default, which
  // otherwise hides the sticky topbar behind the status bar.
  await StatusBar.setOverlaysWebView({ overlay: false });
  await StatusBar.setStyle({ style: Style.Light });
  await StatusBar.setBackgroundColor({ color: '#ffffff' });
}
