"use strict";

const ALLOWED_MODELS = new Set(["gpt-4o", "gpt-4o-mini"]);
const ALLOWED_ROLES = new Set(["system", "user", "assistant"]);
const MAX_MESSAGES = 18;
const MAX_MESSAGE_CHARS = 12000;
const MAX_TOTAL_CHARS = 45000;

function validateOpenAiRequest(body) {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    throw new TypeError("Request body must be a JSON object.");
  }

  const allowedKeys = new Set(["model", "messages", "temperature", "max_tokens"]);
  if (Object.keys(body).some((key) => !allowedKeys.has(key))) {
    throw new TypeError("Request contains unsupported fields.");
  }

  if (typeof body.model !== "string" || !ALLOWED_MODELS.has(body.model)) {
    throw new TypeError("Requested OpenAI model is not allowed.");
  }

  if (!Array.isArray(body.messages) || body.messages.length < 1 || body.messages.length > MAX_MESSAGES) {
    throw new TypeError("Message count is outside the supported range.");
  }

  let totalChars = 0;
  const messages = body.messages.map((message) => {
    if (!message || typeof message !== "object" || Array.isArray(message)) {
      throw new TypeError("Each message must be an object.");
    }
    if (Object.keys(message).some((key) => key !== "role" && key !== "content")) {
      throw new TypeError("Message contains unsupported fields.");
    }
    if (typeof message.role !== "string" || !ALLOWED_ROLES.has(message.role)) {
      throw new TypeError("Message role is not allowed.");
    }
    if (typeof message.content !== "string" || message.content.trim().length === 0) {
      throw new TypeError("Message content must be a non-empty string.");
    }
    if (message.content.length > MAX_MESSAGE_CHARS) {
      throw new TypeError("A message exceeds the supported size.");
    }
    totalChars += message.content.length;
    return { role: message.role, content: message.content };
  });

  if (totalChars > MAX_TOTAL_CHARS) {
    throw new TypeError("Combined message content exceeds the supported size.");
  }

  const temperature = body.temperature === undefined ? 0.7 : body.temperature;
  if (typeof temperature !== "number" || !Number.isFinite(temperature) || temperature < 0 || temperature > 1) {
    throw new TypeError("Temperature must be between 0 and 1.");
  }

  const maxTokens = body.max_tokens === undefined ? 2048 : body.max_tokens;
  if (!Number.isInteger(maxTokens) || maxTokens < 1 || maxTokens > 4096) {
    throw new TypeError("max_tokens is outside the supported range.");
  }

  return {
    model: body.model,
    messages,
    temperature,
    max_tokens: maxTokens,
  };
}

module.exports = { validateOpenAiRequest };
