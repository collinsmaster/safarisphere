const requestLogger = (req, res, next) => {
  console.log(`[Request] ${new Date().toISOString()} - ${req.method} ${req.url}`);
  if (Object.keys(req.body).length > 0) {
    console.log(`[Request Body]:`, JSON.stringify(req.body, null, 2));
  }
  next();
};

module.exports = requestLogger;
