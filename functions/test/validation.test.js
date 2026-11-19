"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { validateOpenAiRequest } = require("../src/validation");

const validPayload = () => ({
  model: "gpt-4o",
  messages: [
    { role: "system", content: "You are a coding assistant." },
    { role: "user", content: "Review this function." },
  ],
  temperature: 0.4,
  max_tokens: 2048,
});

test("accepts supported ChatGPT request payloads", () => {
  assert.deepEqual(validateOpenAiRequest(validPayload()), {
    model: "gpt-4o",
    messages: [
      { role: "system", content: "You are a coding assistant." },
      { role: "user", content: "Review this function." },
    ],
    temperature: 0.4,
    max_tokens: 2048,
  });
});

test("rejects models outside the app allowlist", () => {
  const payload = validPayload();
  payload.model = "unsupported-model";
  assert.throws(() => validateOpenAiRequest(payload), /model is not allowed/);
});

test("rejects unknown top-level and message fields", () => {
  const payload = validPayload();
  payload.tools = [{ type: "function" }];
  assert.throws(() => validateOpenAiRequest(payload), /unsupported fields/);

  const messagePayload = validPayload();
  messagePayload.messages[1].tool_calls = [];
  assert.throws(() => validateOpenAiRequest(messagePayload), /unsupported fields/);
});

test("rejects unsupported message roles and oversized prompts", () => {
  const invalidRole = validPayload();
  invalidRole.messages[1].role = "tool";
  assert.throws(() => validateOpenAiRequest(invalidRole), /role is not allowed/);

  const tooLong = validPayload();
  tooLong.messages[1].content = "x".repeat(12001);
  assert.throws(() => validateOpenAiRequest(tooLong), /exceeds the supported size/);
});

test("rejects invalid temperature, token limits, and message counts", () => {
  const badTemperature = validPayload();
  badTemperature.temperature = 2;
  assert.throws(() => validateOpenAiRequest(badTemperature), /Temperature/);

  const badTokens = validPayload();
  badTokens.max_tokens = 9000;
  assert.throws(() => validateOpenAiRequest(badTokens), /max_tokens/);

  const tooMany = validPayload();
  tooMany.messages = Array.from({ length: 19 }, () => ({ role: "user", content: "x" }));
  assert.throws(() => validateOpenAiRequest(tooMany), /Message count/);
});
