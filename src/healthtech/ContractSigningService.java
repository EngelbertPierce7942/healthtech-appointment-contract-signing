package healthtech;

import java.io.IOException;

public final class ContractSigningService {
    public enum AppointmentState { REQUESTED, CONFIRMED, CANCELLED }

    public record Appointment(String patientId, String appointmentId, AppointmentState state) {}
    public record SigningResult(String status, String downloadResponse) {}

    private final InfraiClient infrai;
    private final String bucket;

    public ContractSigningService(InfraiClient infrai, String bucket) {
        this.infrai = infrai;
        this.bucket = bucket;
    }

    public SigningResult signAfterConfirmation(Appointment appointment, String pdfBase64,
                                                String certPem, String keyPem) throws IOException, InterruptedException {
        if (appointment.state() != AppointmentState.CONFIRMED) {
            return new SigningResult("WAITING_FOR_CONFIRMED_APPOINTMENT", "");
        }
        String objectKey = "appointments/" + appointment.appointmentId() + "/signed-contract.pdf";
        String signed = infrai.pdfSign(pdfBase64, certPem, keyPem);
        infrai.storeSignedDocument(bucket, objectKey, signed);
        String link = infrai.signedDownload(bucket, objectKey);
        return new SigningResult("SIGNED", link);
    }

    public String patientNotification(SigningResult result) {
        if (!"SIGNED".equals(result.status())) return "No document notification sent";
        return "Your appointment contract is signed; use the expiring link from the care team.";
    }
}
