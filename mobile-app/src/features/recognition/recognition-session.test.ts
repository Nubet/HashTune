import { describe, expect, it, vi } from "vitest";
import { ApiError } from "../../lib/api/client";
import {
  RecognitionSession,
  sampleDurationSeconds,
} from "./recognition-session";
import type { RecognitionPorts } from "./recognition-session";

function ports() {
  return {
    prepare: vi.fn<RecognitionPorts["prepare"]>(async () => undefined),
    record: vi.fn(),
    stop: vi.fn(async () => undefined),
    uri: vi.fn(() => "file:///sample.m4a"),
    discard: vi.fn(async () => undefined),
    recognize: vi.fn<RecognitionPorts["recognize"]>(async () => ({
      status: "NO_MATCH",
    })),
    onRecognized: vi.fn(),
  };
}

describe("microphone recognition session", () => {
  it("caps native recording and sends exactly one finalized sample", async () => {
    const dependencies = ports();
    const session = new RecognitionSession(dependencies);
    await session.start();
    expect(dependencies.record).toHaveBeenCalledWith(sampleDurationSeconds);
    await Promise.all([session.finish(), session.finish()]);
    expect(dependencies.recognize).toHaveBeenCalledOnce();
    expect(dependencies.discard).toHaveBeenCalledOnce();
    expect(session.getSnapshot()).toEqual({
      status: "success",
      result: { status: "NO_MATCH" },
    });
  });

  it("cancels preparation without starting the microphone or uploading", async () => {
    const dependencies = ports();
    let prepared: () => void = () => undefined;
    dependencies.prepare.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          prepared = resolve;
        }),
    );
    const session = new RecognitionSession(dependencies);
    const started = session.start();
    const canceled = session.cancel();
    prepared();
    await Promise.all([started, canceled]);
    expect(dependencies.record).not.toHaveBeenCalled();
    expect(dependencies.recognize).not.toHaveBeenCalled();
    expect(dependencies.discard).toHaveBeenCalledOnce();
    expect(session.getSnapshot().status).toBe("idle");
  });

  it("aborts an in-flight upload and ignores a late response", async () => {
    const dependencies = ports();
    let complete: () => void = () => undefined;
    dependencies.recognize.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          complete = () => resolve({ status: "NO_MATCH" });
        }),
    );
    const session = new RecognitionSession(dependencies);
    await session.start();
    const finished = session.finish();
    await vi.waitFor(() =>
      expect(dependencies.recognize).toHaveBeenCalledOnce(),
    );
    const canceled = session.cancel();
    expect(dependencies.recognize.mock.calls[0]?.[1].aborted).toBe(true);
    complete();
    await Promise.all([finished, canceled]);
    expect(dependencies.onRecognized).not.toHaveBeenCalled();
    expect(session.getSnapshot().status).toBe("idle");
  });

  it("cleans up failed uploads and respects the server cooldown", async () => {
    const dependencies = ports();
    dependencies.recognize.mockRejectedValueOnce(
      new ApiError("Wait", 429, "TOO_MANY_REQUESTS", 30),
    );
    const session = new RecognitionSession(dependencies);
    await session.start();
    await session.finish();
    await session.start();
    expect(dependencies.record).toHaveBeenCalledOnce();
    expect(dependencies.discard).toHaveBeenCalledOnce();
    expect(session.getSnapshot().status).toBe("error");
  });
});
