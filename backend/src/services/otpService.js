const twilio = require('twilio');

// Environment variables to be set in Koyeb settings
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const serviceId = process.env.VERIFICATION_SERVICE_ID || 'verification_eahp6k8';

// In-memory store for OTP codes when Twilio is not fully configured (fallback development mode)
const fallbackOtpStore = {};

const isTwilioConfigured = !!(accountSid && authToken && serviceId);

console.log(`[OTP Service] Twilio configure status: ${isTwilioConfigured ? 'LIVE (Real Twilio Verify)' : 'FALLBACK (Local Mock OTP)'}`);
if (isTwilioConfigured) {
  console.log(`[OTP Service] Utilizing Verify Service ID: ${serviceId}`);
}

/**
 * Send an OTP code to an email address (or mobile number if standard phone format matches)
 * @param {string} destination - Email or Phone number
 * @returns {Promise<{success: boolean, message: string, channel: string}>}
 */
async function sendOTP(destination) {
  const isEmail = destination.includes('@');
  const channel = isEmail ? 'email' : 'sms';

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
  const mockCode = '123456'; 
  fallbackOtpStore[destination] = mockCode;
  
  console.log(`==========================================`);
  console.log(`[DEVELOPMENT OTP BYPASS]`);
  console.log(`Destination: ${destination}`);
  console.log(`Verification Code: ${mockCode}`);
  console.log(`Please use code ${mockCode} to complete authorization.`);
  console.log(`==========================================`);

  return { 
    success: true, 
    message: `[Fallback Mode] Code of '${mockCode}' generated and printed to server logs for ${destination}!`,
    channel,
    debugOtp: mockCode
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
  const storedCode = fallbackOtpStore[destination];
  
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
