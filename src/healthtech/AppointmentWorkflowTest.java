package healthtech;

public final class AppointmentWorkflowTest {
    public static void main(String[] args) throws Exception {
        InfraiClient client = new InfraiClient("test-key");
        ContractSigningService service = new ContractSigningService(client, "health-contracts");
        ContractSigningService.Appointment appointment = new ContractSigningService.Appointment(
                "patient-17", "visit-204", ContractSigningService.AppointmentState.REQUESTED);
        ContractSigningService.SigningResult result = service.signAfterConfirmation(
                appointment, "pdf", "certificate", "private-key");
        if (!"WAITING_FOR_CONFIRMED_APPOINTMENT".equals(result.status())) {
            throw new AssertionError("A requested appointment must wait before signing");
        }
        System.out.println("PASS: requested appointment stays unsigned until confirmation");
    }
}
