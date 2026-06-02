const jwt = require('jsonwebtoken');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'safari_sphere_fallback_master_jwt_secret_99482';

/**
 * Express Middleware to protect routes via JWT Bearer Token validation
 */
module.exports = (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authorization header is missing or improperly formatted (must be Bearer Token)' });
  }

  const token = authHeader.split(' ')[1];

  try {
    // If running in development with an empty or fallback key, let's sign/verify safely
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = {
      id: decoded.id,
      username: decoded.username,
      email: decoded.email,
      role: decoded.role || 'user'
    };
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Auth credentials have expired. Please refresh your session.', code: 'TOKEN_EXPIRED' });
    }
    return res.status(403).json({ error: 'Invalid or forged authorization token signature.', code: 'TOKEN_INVALID' });
  }
};
