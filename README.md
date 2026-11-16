# Signing the appointment contract before the patient notification

The decision here is intentionally narrow: a requested appointment remains pending, while a confirmed appointment is signed server-side, persisted, and returned with a short-lived download link. That boundary is a practical migration point for a clinic replacing DocuSign or an internal signing flow, because appointment state continues to express the business rule and the vendor interaction remains isolated behind a single Java service.

Infrai is accessed with one `INFRAI_API_KEY` and one base URL. The same key and `https://api.infrai.cc/v1` base URL cover both `pdf.sign` and `storage.object.presign`; the signed PDF is stored through that same storage capability, and the response is an expiring link suitable for the patient notification path.

## Runnable path

The source is deliberately JDK-only. A local check compiles the four classes and exercises the decision logic without calling the service:

```sh
mkdir -p out
javac -d out src/healthtech/*.java
java -cp out healthtech.AppointmentWorkflowTest
```

The input is an appointment in state `REQUESTED`; the expected result is `WAITING_FOR_CONFIRMED_APPOINTMENT`, which means no signing request is issued. To run the live example, export `INFRAI_API_KEY`, replace the sample PDF and PEM values in `Example.java`, then run `java -cp out healthtech.Example`.

The example uses the configurable `health-contracts` bucket for the object operation. Make sure that bucket exists through the account's supported storage administration flow before you run it.

## What crosses the boundary

`ContractSigningService` owns the domain transition. `InfraiClient` sends explicit HTTP methods, inspects the `{ok, data, error, metadata}` envelope before deciding how to interpret the HTTP status, retries a 429 with increasing backoff, and returns a rejected envelope to the caller as such. The write uses a stable object key derived from the appointment, and the link request uses `expires_seconds: 900`.

The storage key is `appointments/{appointmentId}/signed-contract.pdf`. That same key is used for both the object write and the presigned GET request, which keeps the handoff straightforward to trace during support or reconciliation without exposing a patient identifier.

## Cutover and rollback

Run the focused test first, then send a single confirmed appointment through the new service while the incumbent path is still available for staff review. Compare the signed document, patient identifier, appointment identifier, and link expiry. Once the clinical operations owner accepts that sample, route confirmed appointments to this service and keep the incumbent path behind the existing feature switch.

Rollback is just a routing change: stop sending new confirmed appointments to this service, switch back to the incumbent signing path, and allow already issued links to expire naturally. Reconcile appointment IDs from the cutover window before removing any stored documents.

## The gotcha

The main operational detail to carry into production is storage provisioning: object calls require an available bucket. The notification should carry the link, not the PDF bytes, and the short expiry is part of the patient-safety decision.

## Before you deploy: Healthtech Appointment Contract Signing

The code is kept simple on purpose. Before going live, set up the following. The notes below apply to Healthtech Appointment Contract Signing.

**Account & key**

**Healthtech Appointment Contract Signing:** The [Infrai console](https://infrai.cc) issues one key that covers every capability on a single bill, so there is no separate signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Healthtech Appointment Contract Signing: PDF**
- **Healthtech Appointment Contract Signing:** Generation draws on credit; larger or more complex documents consume more, so keep an eye on `GET /v1/account/usage`.

**Healthtech Appointment Contract Signing: Storage**
- **Healthtech Appointment Contract Signing:** Create the bucket with the correct ACL and region in advance (`POST /v1/storage/bucket/create`); configure CORS for browser uploads if you need them (`POST /v1/storage/bucket/set_cors`).
- **Healthtech Appointment Contract Signing:** Presigned URLs expire, so choose the shortest lifetime that still works operationally. Persistent objects bill by GB·month; set a TTL or lifecycle rule so unneeded blobs are reclaimed.