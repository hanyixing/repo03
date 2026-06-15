const CompressionWebpackPlugin = require('compression-webpack-plugin')

const isProd = process.env.NODE_ENV === 'production'

module.exports = {
  // Served from the root of Spring Boot's static directory in production.
  publicPath: '/',
  outputDir: 'dist',
  // Emit assets under dist/static/** so the layout matches what Spring Boot serves.
  assetsDir: 'static',
  // No source maps in production builds (smaller, faster, no source leakage).
  productionSourceMap: false,

  devServer: {
    port: 8080,
    open: false,
    proxy: {
      // Forward API calls to the backend during local development.
      '/': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },

  configureWebpack: config => {
    if (isProd) {
      // Pre-compress text assets so the server (or a CDN) can ship .gz files.
      config.plugins.push(
        new CompressionWebpackPlugin({
          test: /\.(js|css|html|svg)$/,
          threshold: 10240,
          minRatio: 0.8
        })
      )
    }
  },

  chainWebpack: config => {
    // Split vendor and shared code into long-term cacheable chunks.
    config.optimization.splitChunks({
      chunks: 'all',
      cacheGroups: {
        vendors: {
          name: 'chunk-vendors',
          test: /[\\/]node_modules[\\/]/,
          priority: -10,
          chunks: 'initial'
        },
        common: {
          name: 'chunk-common',
          minChunks: 2,
          priority: -20,
          chunks: 'initial',
          reuseExistingChunk: true
        }
      }
    })
  }
}
