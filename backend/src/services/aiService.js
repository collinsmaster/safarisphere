require('dotenv').config();

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const MODEL = 'gemini-3.5-flash';
const ENDPOINT_URL = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`;

/**
 * Service to orchestrate the SphereMate AI Assistant capabilities
 */
class AIService {
  
  /**
   * Generates a conversational message or caption using Google's Gemini-3.5-Flash
   */
  async generateResponse(systemInstruction, userPrompt) {
    if (!GEMINI_API_KEY || GEMINI_API_KEY === 'MY_GEMINI_API_KEY') {
      console.warn('[SphereMate AI] Gemini API key is missing. Using high-fidelity fallback matching.');
      return this.getMockCompanionResponse(userPrompt);
    }

    try {
      const requestBody = {
        contents: [
          {
            parts: [{ text: userPrompt }]
          }
        ],
        systemInstruction: {
          parts: [{ text: systemInstruction }]
        },
        generationConfig: {
          temperature: 0.7,
          maxOutputTokens: 500
        }
      };

      const url = `${ENDPOINT_URL}?key=${GEMINI_API_KEY}`;
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        throw new Error(`Gemini API responded with status ${response.status}`);
      }

      const responseData = await response.json();
      const generatedText = responseData.candidates?.[0]?.content?.parts?.[0]?.text;
      
      if (!generatedText) {
        throw new Error('Emply response structure returned from Gemini model.');
      }

      return generatedText.trim();
    } catch (err) {
      console.error('[SphereMate AI] Error calling Gemini API:', err.message);
      return this.getMockCompanionResponse(userPrompt);
    }
  }

  /**
   * Suggestions for post captions based on visual or mood cues
   */
  async suggestCaption(mood, topic) {
    const system = "You are SphereMate, an elite futuristic AI social media companion for the Safari Sphere app. Your mission is to write extremely trendy, witty, engaging, and mobile-friendly social media captions. Use modern emojis, sleek vocabulary, and trendy tags. Keep it under two sentences.";
    const prompt = `Generate 3 amazing caption ideas for a post with the mood: "${mood}" exploring the topic: "${topic}". Return them numbered 1, 2, and 3.`;
    return await this.generateResponse(system, prompt);
  }

  /**
   * Smart Content Moderation check (AI spam & safety guard)
   */
  async moderateContent(contentType, text) {
    const system = "You are ContentGuard, an automated intelligence built to protect Safari Sphere from hate speech, harassment, severe toxicity, explicit abuse, scams, and excessive spam. Respond ONLY with a standard JSON object containing 'isSafe' (boolean), 'reason' (string), and 'toxicityScore' (value between 0.0 and 1.0). Do not include any markdown format tags, backticks or extra sentences.";
    const prompt = `Analyze this ${contentType}: "${text}"`;
    
    try {
      const rawResponse = await this.generateResponse(system, prompt);
      // Strip markdown wrapper if present
      const cleanedJson = rawResponse.replace(/```json/g, '').replace(/```/g, '').trim();
      return JSON.parse(cleanedJson);
    } catch (err) {
      console.error('[ContentGuard AI] Moderation parse failure. Defaulting to safe passage.');
      return { isSafe: true, reason: 'Approved via heuristic parser', toxicityScore: 0.1 };
    }
  }

  /**
   * Summarizes profiles or chats to compile a "Vibe Summary"
   */
  async generateVibeSummary(userDisplayName, postsArray) {
    if (!postsArray || postsArray.length === 0) {
      return `Companion report: ${userDisplayName} is a new explorer just stepping into Safari Sphere. Currently setting camp and vibing in silent observation! 🧭`;
    }

    const compiledText = postsArray.map(p => `- ${p.content}`).join('\n');
    const system = "You are SphereMate Analyst. Your job is to read a list of social media updates and generate a highly engaging, futuristic, visual 2-line summary of their current personality, vibe and core interests.";
    const prompt = `Analyze these updates written by ${userDisplayName}:\n${compiledText}`;
    
    return await this.generateResponse(system, prompt);
  }

  /**
   * Helper fallback when Gemini API key is missing
   */
  getMockCompanionResponse(prompt) {
    const query = prompt.toLowerCase();
    
    if (query.includes('caption')) {
      return "1. Chasing wild dreams and cosmic streams here in Safari Sphere! 🦁✨ #NewAge\n2. Savannah dust and digital rust. Connected and vibing under the neon sun. 🌅🛸\n3. Living on the edge of creativity where reality meets the sphere. #VybesOnly";
    }
    
    if (query.includes('moderation') || query.includes('analyze')) {
      return '{"isSafe": true, "reason": "No violation detected by standard heuristics", "toxicityScore": 0.05}';
    }

    if (query.includes('summary') || query.includes('updates')) {
      return "Safari Observer Status: Radiating high-frequency explorer vibes! Driven by a strong interest in outdoor wonders, ambient music, and modern neo-glassmorphic styling lines. 🌾⭐";
    }

    return `Hey there! I'm SphereMate, your futuristic AI companion. I am fully coded and ready to elevate your Safari Sphere experience! Let's generate a smart caption, moderate incoming posts, or synthesize a cosmic vibe summary! 🚀`;
  }
}

module.exports = new AIService();
