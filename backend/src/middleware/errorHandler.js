const errorHandler = (err, req, res, next) => {
  const statusCode = err.status || err.statusCode || 500;
  console.error(`[EXPRESS_EXCEPTION] [${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  console.error(err.stack || err.message || err);
  
  res.status(statusCode).json({
    success: false,
    timestamp: new Date().toISOString(),
    error: err.message || 'A critical server exception occurred inside the Safari Sphere network.',
    path: req.originalUrl,
    method: req.method,
    status: statusCode
  });
};

module.exports = errorHandler;
