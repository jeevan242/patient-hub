package com.ph.patientservice.service;

import com.ph.patientservice.dto.PatientRequestDTO;
import com.ph.patientservice.dto.PatientResponseDTO;
import com.ph.patientservice.exception.PatientNotFoundException;
import com.ph.patientservice.grpc.BillingServiceGrpcClient;
import com.ph.patientservice.kafka.KafkaProducer;
import com.ph.patientservice.mapper.PatientMapper;
import com.ph.patientservice.model.Patient;
import com.ph.patientservice.repo.PatientRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepo patientRepo;

    private final BillingServiceGrpcClient billingServiceGrpcClient;

    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepo patientRepo, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepo = patientRepo;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepo.findAll();
        return patients.stream().map(PatientMapper::toPatientResponseDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        Patient patient = PatientMapper.toPatient(patientRequestDTO);
        Patient save = patientRepo.save(patient);
        billingServiceGrpcClient.createBillingAccount(patient.getId().toString(), patient.getName(), patient.getEmail());
        kafkaProducer.sendEvent(patient);
        return PatientMapper.toPatientResponseDTO(save);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient patientDb = patientRepo.findById(id).orElseThrow(() -> new PatientNotFoundException("Patient not for for id:" + id));
        patientDb.setName(patientRequestDTO.getName());
        patientDb.setAddress(patientRequestDTO.getAddress());
        patientDb.setEmail(patientRequestDTO.getEmail());
        patientDb.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepo.save(patientDb);
        return PatientMapper.toPatientResponseDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        patientRepo.deleteById(id);
    }
}
