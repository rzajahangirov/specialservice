package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.InvoiceSequence;
import az.techvibeds.specialservice.repositories.InvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final InvoiceSequenceRepository invoiceSequenceRepository;

    /**
     * Finds the sequence or creates it in a new transaction.
     * This guarantees that the sequence row exists before we try to lock it
     * in the main transaction, avoiding concurrent creation issues.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSequenceExists(Company company, int year) {
        if (invoiceSequenceRepository.readByCompanyAndYear(company, year).isEmpty()) {
            InvoiceSequence newSeq = new InvoiceSequence();
            newSeq.setCompany(company);
            newSeq.setYear(year);
            newSeq.setLastNumber(0L);
            invoiceSequenceRepository.saveAndFlush(newSeq);
        }
    }
}
