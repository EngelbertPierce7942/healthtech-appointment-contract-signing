package healthtech;

public final class Example {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(apiKey);
        ContractSigningService service = new ContractSigningService(client, "health-contracts");
        var appointment = new ContractSigningService.Appointment(
                "patient-17", "visit-204", ContractSigningService.AppointmentState.CONFIRMED);
        var result = service.signAfterConfirmation(appointment, "BASE64_PDF", "CERT_PEM", "KEY_PEM");
        System.out.println(result.status());
        System.out.println(service.patientNotification(result));
    }
}
