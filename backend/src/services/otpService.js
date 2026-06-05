const twilio = require('twilio');
const https = require('https');

// Environment variables to be set in Koyeb settings
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const serviceId = process.env.VERIFICATION_SERVICE_ID || 'verification_eahp6k8';

// EmailJS credentials from environment
const emailjsServiceId = process.env.EMAILJS_SERVICE_ID || process.env.SERVICE_ID;
const emailjsTemplateId = process.env.EMAILJS_TEMPLATE_ID || process.env.TEMPLATE_ID;
const emailjsPublicKey = process.env.EMAILJS_PUBLIC_KEY || process.env.PUBLIC_KEY;
const emailjsPrivateKey = process.env.EMAILJS_PRIVATE_KEY || process.env.PRIVATE_KEY;

// In-memory store for OTP codes when Twilio/EmailJS is not fully configured (fallback development mode)
const fallbackOtpStore = {};

// Cache for persistent OTP codes valid for 30 minutes
const activeOtpStore = {};

const isTwilioConfigured = !!(accountSid && authToken && serviceId);
const isEmailJSConfigured = !!(emailjsServiceId && emailjsTemplateId && emailjsPublicKey && emailjsPrivateKey);

console.log(`[OTP Service] Twilio configure status: ${isTwilioConfigured ? 'LIVE (Real Twilio Verify)' : 'NOT CONFIGURED'}`);
console.log(`[OTP Service] EmailJS configure status: ${isEmailJSConfigured ? 'LIVE (Real EmailJS)' : 'NOT CONFIGURED'}`);

if (isTwilioConfigured) {
  console.log(`[OTP Service] Utilizing Verify Service ID: ${serviceId}`);
}

/**
 * Helper to make robust HTTP request to EmailJS REST API
 */
function sendEmailJSRecord(serviceId, templateId, publicKey, privateKey, destination, otpCode) {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify({
      service_id: serviceId,
      template_id: templateId,
      user_id: publicKey,
      accessToken: privateKey,
      template_params: {
        to_email: destination,
        email: destination,
        otp_code: otpCode,
        otp: otpCode,
        code: otpCode,
        message: `Your Safari Sphere verification OTP is: ${otpCode}`
      }
    });

    const options = {
      hostname: 'api.emailjs.com',
      port: 443,
      path: '/api/v1.0/email/send',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(data);
        } else {
          reject(new Error(`Status ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('error', (e) => {
      reject(e);
    });

    req.write(postData);
    req.end();
  });
}

/**
 * Send an OTP code to an email address (or mobile number if standard phone format matches)
 * @param {string} destination - Email or Phone number
 * @returns {Promise<{success: boolean, message: string, channel: string}>}
 */
async function sendOTP(destination) {
  const isEmail = destination.includes('@');
  const channel = isEmail ? 'email' : 'sms';

  const now = Date.now();
  let otpCode;
  if (activeOtpStore[destination] && activeOtpStore[destination].expiresAt > now) {
    otpCode = activeOtpStore[destination].code;
    console.log(`[OTP Service] Reusing valid OTP ${otpCode} (expires in ${Math.round((activeOtpStore[destination].expiresAt - now) / 1000)}s) for ${destination}`);
  } else {
    otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    activeOtpStore[destination] = {
      code: otpCode,
      expiresAt: now + 30 * 60 * 1000 // 30 minutes
    };
    console.log(`[OTP Service] Generated fresh 30-min OTP ${otpCode} for ${destination}`);
  }

  // Try EmailJS first for Email destinations if configured
  if (isEmail && isEmailJSConfigured) {
    try {
      fallbackOtpStore[destination] = otpCode;

      console.log(`[OTP Service] Sending EmailJS OTP code ${otpCode} to ${destination}...`);
      await sendEmailJSRecord(
        emailjsServiceId,
        emailjsTemplateId,
        emailjsPublicKey,
        emailjsPrivateKey,
        destination,
        otpCode
      );

      console.log(`[OTP Service] EmailJS OTP dispatched successfully to ${destination}`);
      return { 
        success: true, 
        message: `Verification code successfully sent via email!`, 
        channel,
        debugOtp: otpCode
      };
    } catch (err) {
      console.error('[OTP Service] EmailJS Error sending verification. Falling back:', err.message);
    }
  }

  if (isTwilioConfigured) {
    try {
      const client = twilio(accountSid, authToken);
      const verification = await client.verify.v2.services(serviceId)
        .verifications
        .create({ to: destination, channel: channel });
      
      console.log(`[OTP Service] Sent real OTP to ${destination} via Twilio status: ${verification.status}`);
      return { success: true, message: `Verification code successfully sent via Twilio (${channel})!`, channel };
    } catch (err) {
      console.error('[OTP Service] Twilio Error sending verification. Falling back to mock OTP:', err.message);
      // Fallback below
    }
  }

  // Fallback / Development mode OTP
  fallbackOtpStore[destination] = otpCode;
  
  console.log(`==========================================`);
  console.log(`[DEVELOPMENT OTP BYPASS]`);
  console.log(`Destination: ${destination}`);
  console.log(`Verification Code: ${otpCode}`);
  console.log(`Please use code ${otpCode} to complete authorization.`);
  console.log(`==========================================`);

  return { 
    success: true, 
    message: `[Fallback Mode] Code of '${otpCode}' generated and printed to server logs for ${destination}!`,
    channel,
    debugOtp: otpCode
  };
}

/**
 * Verify an OTP code for an email address (or mobile number)
 * @param {string} destination - Email or Phone number
 * @param {string} code - User input OTP code
 * @returns {Promise<boolean>}
 */
async function verifyOTP(destination, code) {
  if (!code) return false;

  if (isTwilioConfigured) {
    try {
      const client = twilio(accountSid, authToken);
      const check = await client.verify.v2.services(serviceId)
        .verificationChecks
        .create({ to: destination, code: code });
      
      console.log(`[OTP Service] Twilio check status for ${destination}: ${check.status}`);
      return check.status === 'approved';
    } catch (err) {
      console.error('[OTP Service] Twilio Error checking verification. Checking fallbacks:', err.message);
      // Fallback check below
    }
  }

  // Fallback Check
  const expectedCodes = ['123456', '123123']; // allow easy developer testing
  const storedCode = fallbackOtpStore[destination] || (activeOtpStore[destination] && activeOtpStore[destination].expiresAt > Date.now() ? activeOtpStore[destination].code : null);
  
  if (code === storedCode || expectedCodes.includes(code)) {
    delete fallbackOtpStore[destination];
    return true;
  }

  return false;
}

module.exports = {
  sendOTP,
  verifyOTP,
  isTwilioConfigured
};
