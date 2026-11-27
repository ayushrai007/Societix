# Initial platform administrator bootstrap

The first `PLATFORM_ADMIN` is provisioned by a restricted operator action, never a public endpoint.
The bootstrap runner is enabled only when both Firebase Admin credentials and an explicit Firebase
UID are supplied.

1. Complete Firebase Phone Auth for the authorized operator so Firebase has an enabled user with a
   verified Indian phone number. Record the Firebase UID from the trusted operator workflow.
2. Provide Firebase Admin credentials through Application Default Credentials or workload identity.
   Never add a service-account key to the repository.
3. Set `FIREBASE_ADMIN_ENABLED=true`, provide `SOCIETIX_PHONE_ENCRYPTION_KEY`, and set
   `SOCIETIX_PLATFORM_ADMIN_BOOTSTRAP_FIREBASE_UID` to the approved UID for this one startup.
4. Start the backend. It verifies the Firebase user, encrypts and stores the phone contact, creates
   the global platform-admin record, and sets Firebase custom claims `userId`, `roles`, and
   `authorizationVersion` (`tenantId` is omitted for a global admin).
5. Remove `SOCIETIX_PLATFORM_ADMIN_BOOTSTRAP_FIREBASE_UID` immediately after successful startup.
   The runner refuses to bootstrap a different UID after a platform admin is already provisioned.
6. Have the operator force-refresh their Firebase ID token before using platform-admin endpoints.

If the process fails after the database transaction but before Firebase claims are written, rerun
with the same UID. The operation reuses the existing platform-admin identity and retries the claims
write. The operator should not share the bootstrap UID or credentials with application users.
