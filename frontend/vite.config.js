import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const devServerPort = Number(env.VITE_DEV_SERVER_PORT ?? 5173);
  const cloudflareWebAnalyticsToken = env.VITE_CLOUDFLARE_WEB_ANALYTICS_TOKEN?.trim();

  return {
    plugins: [vue(), cloudflareWebAnalyticsPlugin(cloudflareWebAnalyticsToken)],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: devServerPort,
    },
  };
});

function cloudflareWebAnalyticsPlugin(token) {
  return {
    name: 'weblinkpilot-cloudflare-web-analytics',
    transformIndexHtml() {
      if (!token) {
        return [];
      }

      return [
        {
          tag: 'script',
          injectTo: 'body',
          attrs: {
            defer: true,
            src: 'https://static.cloudflareinsights.com/beacon.min.js',
            'data-cf-beacon': JSON.stringify({ token }),
          },
        },
      ];
    },
  };
}
