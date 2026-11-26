#!/usr/bin/env python3
"""Sign in an existing phone user through the local Firebase Auth Emulator."""

import json
import sys
import time
import urllib.error
import urllib.request

HOST = "http://127.0.0.1:9099"
PROJECT_ID = "demo-societix-local"
API_KEY = "fake-api-key"


def request_json(url, payload=None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"} if data is not None else {},
        method="POST" if data is not None else "GET",
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Auth Emulator returned HTTP {error.code}: {detail}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Cannot reach Auth Emulator at {HOST}: {error.reason}") from error


def get_existing_codes(phone_number):
    codes = request_json(f"{HOST}/emulator/v1/projects/{PROJECT_ID}/verificationCodes")
    return {
        normalize_code(entry.get("sessionCode"))
        for entry in codes.get("verificationCodes", [])
        if entry.get("phoneNumber") == phone_number
    }


def normalize_code(value):
    # The emulator has returned numeric JSON values for SMS codes in some versions, while the
    # signInWithPhoneNumber REST endpoint requires `code` to be a string.
    if isinstance(value, int) and not isinstance(value, bool):
        return str(value)
    return value if isinstance(value, str) else None


def wait_for_new_code(phone_number, previous_codes):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        codes = request_json(f"{HOST}/emulator/v1/projects/{PROJECT_ID}/verificationCodes")
        for entry in reversed(codes.get("verificationCodes", [])):
            code = normalize_code(entry.get("sessionCode"))
            if code and entry.get("phoneNumber") == phone_number and code not in previous_codes:
                return code
        time.sleep(0.5)
    return None


def main():
    phone_number = input("Firebase Auth Emulator phone number (+country code): ").strip()
    if not phone_number.startswith("+"):
        raise RuntimeError("Enter the phone number in E.164 format, including the leading +")

    previous_codes = get_existing_codes(phone_number)
    verification = request_json(
        f"{HOST}/identitytoolkit.googleapis.com/v1/accounts:sendVerificationCode?key={API_KEY}",
        {"phoneNumber": phone_number, "recaptchaToken": "local-test"},
    )
    session_info = verification.get("sessionInfo")
    if not session_info:
        raise RuntimeError("Auth Emulator did not return a verification session")

    print("Waiting for the emulator verification code…", file=sys.stderr)
    code = wait_for_new_code(phone_number, previous_codes)
    if code is None:
        code = input("Enter the verification code shown in the Firebase Emulator log: ").strip()
    if not code:
        raise RuntimeError("A verification code is required")
    result = request_json(
        f"{HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPhoneNumber?key={API_KEY}",
        {"sessionInfo": session_info, "code": code},
    )
    id_token = result.get("idToken")
    if not id_token:
        raise RuntimeError("Auth Emulator sign-in response did not contain an ID token")

    # Keep stdout machine-readable so callers can capture it into an environment variable.
    print(id_token)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, EOFError, KeyboardInterrupt) as error:
        print(f"Sign-in failed: {error}", file=sys.stderr)
        sys.exit(1)
