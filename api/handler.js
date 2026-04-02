const http = require('http');

// Import the server from the main file
const server = require('../server');

// Vercel serverless handler — forward requests to the http server
module.exports = (req, res) => {
  // Reconstruct the URL with /api prefix if needed
  server.emit('request', req, res);
};
