import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";
import { Rate } from "k6/metrics";

const failures = new Rate("observer_request_failures");
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const tokens = (__ENV.STUDENT_TOKENS || "").split(",").filter(Boolean);
const sessions = (__ENV.SESSION_IDS || "").split(",").filter(Boolean);
const questionIds = (__ENV.QUESTION_IDS || "").split(",").filter(Boolean);

export const options = {
  scenarios: {
    integrity_batches: {
      executor: "constant-vus",
      exec: "integrityBatch",
      vus: Number(__ENV.INTEGRITY_VUS || 100),
      duration: __ENV.DURATION || "5m",
    },
    concurrent_autosaves: {
      executor: "constant-arrival-rate",
      exec: "autosave",
      rate: Number(__ENV.AUTOSAVES_PER_SECOND || 100),
      timeUnit: "1s",
      duration: __ENV.DURATION || "5m",
      preAllocatedVUs: Number(__ENV.AUTOSAVE_VUS || 100),
      maxVUs: Number(__ENV.AUTOSAVE_MAX_VUS || 500),
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    observer_request_failures: ["rate<0.01"],
    "http_req_duration{operation:integrity}": ["p(95)<750", "p(99)<1500"],
    "http_req_duration{operation:autosave}": ["p(95)<500", "p(99)<1000"],
  },
};

function credentials() {
  if (!tokens.length || tokens.length !== sessions.length) {
    throw new Error("STUDENT_TOKENS and SESSION_IDS must be non-empty equal-length CSV lists");
  }
  const index = (exec.vu.idInTest - 1) % tokens.length;
  return { token: tokens[index], sessionId: sessions[index] };
}

function headers(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  };
}

function uuid() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16);
    const value = character === "x" ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

export function integrityBatch() {
  const { token, sessionId } = credentials();
  const now = new Date().toISOString();
  const body = JSON.stringify({
    events: [{
      clientEventId: uuid(),
      eventCode: "TAB_BLUR",
      timestamp: now,
      description: "k6 staged integrity event",
      durationMs: 1200,
    }],
  });
  const response = http.post(
    `${baseUrl}/api/student/exam-sessions/${sessionId}/integrity-events`,
    body,
    { ...headers(token), tags: { operation: "integrity" } },
  );
  failures.add(!check(response, { "integrity accepted": (r) => r.status === 200 }));
  sleep(12);
}

export function autosave() {
  const { token, sessionId } = credentials();
  if (!questionIds.length) {
    throw new Error("QUESTION_IDS must contain at least one question ID");
  }
  const questionId = questionIds[exec.scenario.iterationInTest % questionIds.length];
  const selectedOption = ["A", "B", "C", "D"][exec.scenario.iterationInTest % 4];
  const response = http.put(
    `${baseUrl}/api/student/exam-sessions/${sessionId}/answers/${questionId}`,
    JSON.stringify({ selectedOption }),
    { ...headers(token), tags: { operation: "autosave" } },
  );
  failures.add(!check(response, { "autosave accepted": (r) => r.status === 200 }));
}
