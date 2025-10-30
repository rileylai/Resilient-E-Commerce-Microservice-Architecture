const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Proxy requests to /api/bank to Bank service (port 8084)
  app.use(
    '/api/bank',
    createProxyMiddleware({
      target: 'http://localhost:8084',
      changeOrigin: true,
      pathRewrite: {
        '^/api/bank': '/api'
      }
    })
  );
};
