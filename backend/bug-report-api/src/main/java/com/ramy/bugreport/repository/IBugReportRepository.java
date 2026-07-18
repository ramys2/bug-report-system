package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.BugReport;

public interface IBugReportRepository extends JpaRepository<BugReport, UUID> {
}
