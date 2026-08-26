
const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8082,
    host: '0.0.0.0',
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8091',
        changeOrigin: true,
        ws: true,
        onProxyRes(proxyRes) {
          proxyRes.headers['cache-control'] = 'no-cache'
          proxyRes.headers['x-accel-buffering'] = 'no'
        }
      }
    }
  }
})
