package az.techvibeds.specialservice.exceptions;

public class EmployeeNotFoundException extends ResourceNotFoundException {
    public EmployeeNotFoundException(Long id) {
        super("Employee", "id", id);
    }
}
